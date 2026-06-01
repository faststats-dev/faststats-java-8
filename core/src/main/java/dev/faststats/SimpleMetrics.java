package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.data.Metric;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public abstract class SimpleMetrics implements Metrics {
    protected final Logger logger = LoggerFactory.factory().getLogger(getClass());

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ScheduledExecutorService submissionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private @Nullable ScheduledFuture<?> submissionJob = null;

    private final @Nullable Runnable flush;
    private final Set<Metric<?>> metrics;
    private final URI url;

    protected final SimpleContext context;

    @Contract(mutates = "io")
    protected SimpleMetrics(final Factory factory) throws IllegalStateException {
        this(factory, getMetricsServerUrl());
    }

    private static URI getMetricsServerUrl() {
        final var property = System.getProperty("faststats.metrics-server");
        if (property != null) try {
            return new URI(property);
        } catch (final URISyntaxException e) {
            final var logger = LoggerFactory.factory().getLogger(SimpleMetrics.class);
            logger.error("Failed to parse metrics server url: %s", e, property);
        }
        return URI.create("https://metrics.faststats.dev/v1/collect");
    }

    @VisibleForTesting
    protected SimpleMetrics(
            final Factory factory,
            final URI url
    ) {
        this.context = factory.context;
        this.metrics = context.getConfig().additionalMetrics() ? Set.copyOf(factory.metrics) : Set.of();
        final var debug = context.getConfig().debug() || Boolean.getBoolean("faststats.debug");
        this.logger.setFilter(level -> debug || level.equals(Level.CONFIG));
        this.flush = factory.flush;
        this.url = url;
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

    // todo: improve logging to be less cluttered
    @VisibleForTesting
    public boolean submit() {
        final var data = createData().toString();
        final var bytes = data.getBytes(UTF_8);

        logger.info("Uncompressed data: %s", data);

        try (final var byteOutput = new ByteArrayOutputStream();
             final var output = new GZIPOutputStream(byteOutput)) {

            output.write(bytes);
            output.finish();

            final var compressed = byteOutput.toByteArray();
            logger.info("Compressed size: %s bytes", compressed.length);

            final var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                    .header("Content-Encoding", "gzip")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "Bearer " + context.getToken())
                    .header("User-Agent", context.getSdkInfo().getUserAgent())
                    .timeout(Duration.ofSeconds(3))
                    .uri(url)
                    .build();

            logger.info("Sending metrics to: %s", url);
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            final var statusCode = response.statusCode();
            final var body = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Metrics submitted with status code: %s (%s)", statusCode, body);
                if (flush != null) flush.run();
                return true;
            } else if (statusCode >= 300 && statusCode < 400) {
                logger.warn("Received redirect response from metrics server: %s (%s)", statusCode, body);
            } else if (statusCode >= 400 && statusCode < 500) {
                logger.error("Submitted invalid request to metrics server: %s (%s)", null, statusCode, body);
            } else if (statusCode >= 500 && statusCode < 600) {
                logger.error("Received server error response from metrics server: %s (%s)", null, statusCode, body);
            } else {
                logger.warn("Received unexpected response from metrics server: %s (%s)", statusCode, body);
            }
        } catch (final HttpConnectTimeoutException t) {
            logger.error("Metrics submission timed out after 3 seconds: %s", null, url);
        } catch (final ConnectException t) {
            logger.error("Failed to connect to metrics server: %s", null, url);
        } catch (final Throwable t) {
            logger.error("Failed to submit metrics", t);
        }
        return false;
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
