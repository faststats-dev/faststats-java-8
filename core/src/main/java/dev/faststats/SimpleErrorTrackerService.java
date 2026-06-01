package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class SimpleErrorTrackerService extends SubmissionService implements ErrorTrackerService {
    private static final Logger logger = LoggerFactory.factory().getLogger(SimpleErrorTrackerService.class);

    private final URI url = getServerUrl(
            "faststats.error-tracker-server",
            "https://metrics.faststats.dev"
    ).resolve("/v1/error");
    private final SimpleErrorTracker globalErrorTracker;

    final Set<SimpleErrorTracker> errorTrackers = new CopyOnWriteArraySet<>();
    final Set<ScheduledFuture<?>> submissionJobs = new CopyOnWriteArraySet<>();

    private volatile @Nullable ScheduledExecutorService submissionScheduler;
    private volatile @Nullable ScheduledFuture<?> errorSubmissionJob;

    private static final Set<SimpleErrorTracker> DISPATCHER_TRACKERS = new CopyOnWriteArraySet<>();
    private static Thread.@Nullable UncaughtExceptionHandler originalHandler;

    SimpleErrorTrackerService(final SimpleContext context, final ErrorTracker globalErrorTracker) {
        super(context);
        logger.setFilter(level -> context.getConfig().debug()); // fixme: awful practice
        this.globalErrorTracker = ((SimpleErrorTracker) globalErrorTracker);
        startErrorSubmission();
    }

    @Override
    public ErrorTracker globalErrorTracker() {
        return globalErrorTracker;
    }

    @Override
    public ErrorTrackerService registerErrorTracker(final ErrorTracker errorTracker) {
        errorTrackers.add(((SimpleErrorTracker) errorTracker));
        startErrorSubmission();
        return this;
    }

    static synchronized void attachErrorTracker(final SimpleErrorTracker tracker) {
        if (DISPATCHER_TRACKERS.isEmpty()) {
            originalHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(SimpleErrorTrackerService::handleUncaughtException);
        }
        DISPATCHER_TRACKERS.add(tracker);
    }

    static synchronized void detachErrorTracker(final SimpleErrorTracker tracker) {
        DISPATCHER_TRACKERS.remove(tracker);
        if (DISPATCHER_TRACKERS.isEmpty()) {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
            originalHandler = null;
        }
    }

    private static void handleUncaughtException(final Thread thread, final Throwable error) {
        for (final var tracker : DISPATCHER_TRACKERS) {
            try {
                final var loader = tracker.attachedLoader();
                if (loader != null && !ErrorHelper.isSameLoader(loader, error)) continue;
                tracker.trackError(error).handled(false);
                tracker.getContextErrorHandler().ifPresent(handler -> handler.accept(loader, error));
            } catch (final Throwable t) {
                logger.error("Failed to dispatch uncaught error to tracker", t);
            }
        }

        final var handler = originalHandler;
        if (handler != null) handler.uncaughtException(thread, error);
    }

    void submit() {
        if (!context.getConfig().errorTracking()) return;

        final var data = createData();
        if (data == null) return;

        if (submit(url, data, "errors")) clear();
    }

    @VisibleForTesting
    public @Nullable JsonObject createData() {
        final var globalErrorTrackerData = globalErrorTracker.getData(false);
        if (errorTrackers.isEmpty() && globalErrorTrackerData.isEmpty()) return null;

        final var data = new JsonObject();
        context.getSdkInfo().getBuildId().ifPresent(id -> data.addProperty("buildId", id));
        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.addProperty("language", "java");
        data.addProperty("project_name", context.getProjectName());
        data.addProperty("sdk_name", context.getSdkInfo().getName());
        data.addProperty("sdk_version", context.getSdkInfo().getVersion());

        final var defaultContext = new JsonObject();
        context.metrics().ifPresent(metrics -> {
            final var simpleMetrics = (SimpleMetrics) metrics;
            simpleMetrics.appendData(defaultContext);
        });
        globalErrorTracker.getAttributes().forEachPrimitive(defaultContext::add);
        data.add("context", defaultContext);

        final var errors = new JsonArray();
        errors.addAll(globalErrorTrackerData);
        errorTrackers.forEach(tracker -> errors.addAll(tracker.getFullData()));
        data.add("errors", errors);
        return data;
    }

    void clear() {
        globalErrorTracker.clear();
        errorTrackers.forEach(SimpleErrorTracker::clear);
    }

    ScheduledFuture<?> scheduleSubmission(
            final Runnable task,
            final long initialDelay,
            final long period,
            final TimeUnit unit
    ) {
        final var scheduler = submissionScheduler();
        final var future = scheduler.scheduleAtFixedRate(task, Math.max(0, initialDelay), Math.max(1000, period), unit);
        submissionJobs.add(future);
        return future;
    }

    void unregisterSubmission(final ScheduledFuture<?> future) {
        future.cancel(false);
        submissionJobs.remove(future);
    }

    boolean isSubmissionSchedulerRunning() {
        final var scheduler = submissionScheduler;
        return scheduler != null && !scheduler.isShutdown();
    }

    private ScheduledExecutorService submissionScheduler() {
        var scheduler = submissionScheduler;
        if (scheduler != null && !scheduler.isShutdown()) return scheduler;
        synchronized (this) {
            scheduler = submissionScheduler;
            if (scheduler != null && !scheduler.isShutdown()) return scheduler;
            // todo: use platform native scheduler if available?
            return submissionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                final var thread = new Thread(runnable, "faststats-submitter");
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    void startErrorSubmission() {
        if (!context.getConfig().errorTracking() || errorSubmissionJob != null) return;
        errorSubmissionJob = scheduleSubmission(
                this::submit,
                TimeUnit.SECONDS.toMillis(Long.getLong("faststats.initial-delay", 30)),
                TimeUnit.MINUTES.toMillis(30),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected String serverType() {
        return "error";
    }
}
