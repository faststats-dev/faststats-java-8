package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.data.Metric;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public abstract class SimpleMetrics extends SubmissionService implements Metrics {
    private static final String COLLECT_PATH = "/v1/collect";

    // todo: merge with context scheduler
    private final ScheduledExecutorService submissionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private @Nullable ScheduledFuture<?> submissionJob = null;

    private final @Nullable Runnable flush;
    private final Set<Metric<?>> metrics;
    private final URI url = getServerUrl(
            "faststats.metrics-server",
            "https://metrics.faststats.dev"
    ).resolve(COLLECT_PATH);

    @Contract(mutates = "io")
    protected SimpleMetrics(final Factory factory) {
        super(factory.context);
        this.metrics = context.getConfig().additionalMetrics() ? Set.copyOf(factory.metrics) : Set.of();
        final var debug = context.getConfig().debug() || Boolean.getBoolean("faststats.debug");
        this.flush = factory.flush;
    }

    protected long getInitialDelay() {
        return TimeUnit.SECONDS.toMillis(Long.getLong("faststats.initial-delay", 30));
    }

    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Async.Schedule
    @MustBeInvokedByOverriders
    protected void startSubmitting() {
        startSubmitting(getInitialDelay(), getPeriod(), TimeUnit.MILLISECONDS);
    }

    protected abstract boolean preSubmissionStart();

    private void startSubmitting(final long initialDelay, final long period, final TimeUnit unit) {
        if (!preSubmissionStart()) return;

        final var enabled = Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));

        if (!context.getConfig().submitMetrics() || !enabled) {
            logger.warn("Metrics disabled, not starting submission");
            return;
        }

        if (isSubmitting()) {
            logger.warn("Metrics already submitting, not starting again");
            return;
        }

        logger.info("Starting metrics submission");
        submissionJob = submissionScheduler.scheduleAtFixedRate(
                this::submit,
                Math.max(0, initialDelay),
                Math.max(1000, period),
                unit
        );
    }

    protected boolean isSubmitting() {
        return submissionJob != null && !submissionJob.isCancelled();
    }

    @VisibleForTesting
    public boolean submit() {
        if (submit(url, createData(), "metrics")) {
            if (flush != null) flush.run();
            return true;
        }
        return false;
    }

    @Override
    protected String serverType() {
        return "metrics";
    }

    private static final String javaVendor = System.getProperty("java.vendor");
    private static final String javaVersion = System.getProperty("java.version");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osName = System.getProperty("os.name");
    private static final String osVersion = System.getProperty("os.version");
    private static final int coreCount = Runtime.getRuntime().availableProcessors();

    private void appendInternalData(final JsonObject metrics) {
        metrics.addProperty("core_count", coreCount);
        metrics.addProperty("java_vendor", javaVendor);
        metrics.addProperty("java_version", javaVersion);
        metrics.addProperty("os_arch", osArch);
        metrics.addProperty("os_name", osName);
        metrics.addProperty("os_version", osVersion);
    }

    private void appendCustomData(final JsonObject metrics) {
        this.metrics.forEach(metric -> {
            try {
                if (metrics.has(metric.getId()))
                    logger.warn("Skipped duplicated metrics entry: %s", metric.getId());
                else metric.getData().ifPresent(element -> metrics.add(metric.getId(), element));
            } catch (final Throwable t) {
                logger.error("Failed to append custom metric data: %s", t, metric.getId());
                context.errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
            }
        });
    }

    public final void appendData(final JsonObject metrics) {
        appendInternalData(metrics);
        appendDefaultData(metrics);
        appendCustomData(metrics);
    }

    private JsonObject createData() {
        final var data = new JsonObject();
        final var metrics = new JsonObject();

        appendData(metrics);

        data.addProperty("project_name", context.getProjectName());
        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.add("data", metrics);

        return data;
    }

    @Contract(mutates = "param1")
    protected abstract void appendDefaultData(JsonObject metrics);

    protected void shutdown() {
        if (submissionJob != null) try {
            logger.info("Shutting down metrics submission");
            submissionJob.cancel(false);
            submit();
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics on shutdown", t);
        } finally {
            submissionJob = null;
            submissionScheduler.shutdown();
        }
    }

    public abstract static class Factory implements Metrics.Factory {
        private @Nullable Runnable flush;
        private final Set<Metric<?>> metrics = new HashSet<>(0);
        protected final SimpleContext context;

        protected Factory(final SimpleContext context) {
            this.context = context;
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            if (!metrics.add(metric)) throw new IllegalArgumentException("Metric already added: " + metric.getId());
            return this;
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            this.flush = flush;
            return this;
        }
    }
}
