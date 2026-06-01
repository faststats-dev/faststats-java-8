package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

final class SimpleErrorTrackerService implements ErrorTrackerService {
    private final Logger logger = LoggerFactory.factory().getLogger(getClass());
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final SimpleContext context;
    private final URI url = getErrorTrackerServerUrl();
    private final SimpleErrorTracker globalErrorTracker;

    final Set<SimpleErrorTracker> errorTrackers = new CopyOnWriteArraySet<>();
    final Set<ScheduledFuture<?>> submissionJobs = new CopyOnWriteArraySet<>();

    private volatile @Nullable ScheduledExecutorService submissionScheduler;
    private volatile @Nullable ScheduledFuture<?> errorSubmissionJob;

    private static final Object DISPATCHER_LOCK = new Object();
    private static final Set<SimpleErrorTracker> DISPATCHER_TRACKERS = new CopyOnWriteArraySet<>();
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);
    private static Thread.@Nullable UncaughtExceptionHandler originalHandler;

    SimpleErrorTrackerService(final SimpleContext context, final ErrorTracker globalErrorTracker) {
        this.context = context;
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

    // fixme: hacky shit; it only has to compile and pass tests for now
    static void attachErrorTracker(final SimpleErrorTracker tracker) {
        synchronized (DISPATCHER_LOCK) {
            if (DISPATCHER_TRACKERS.isEmpty()) {
                originalHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(SimpleErrorTrackerService::handleUncaughtException);
            }
            DISPATCHER_TRACKERS.add(tracker);
        }
    }

    // fixme: hacky shit; it only has to compile and pass tests for now
    static void detachErrorTracker(final SimpleErrorTracker tracker) {
        synchronized (DISPATCHER_LOCK) {
            DISPATCHER_TRACKERS.remove(tracker);
            if (DISPATCHER_TRACKERS.isEmpty()) {
                Thread.setDefaultUncaughtExceptionHandler(originalHandler);
                originalHandler = null;
            }
        }
    }

    // fixme: hacky shit; it only has to compile and pass tests for now
    private static void handleUncaughtException(final Thread thread, final Throwable error) {
        if (!DISPATCHING.get()) {
            DISPATCHING.set(true);
            try {
                for (final var tracker : DISPATCHER_TRACKERS) {
                    final var loader = tracker.attachedLoader();
                    if (loader != null && !ErrorHelper.isSameLoader(loader, error)) continue;
                    tracker.trackError(error).handled(false);
                    tracker.getContextErrorHandler().ifPresent(handler -> handler.accept(loader, error));
                }
            } finally {
                DISPATCHING.set(false);
            }
        }

        final var handler = originalHandler;
        if (handler != null) handler.uncaughtException(thread, error);
    }

    private static URI getErrorTrackerServerUrl() {
        final var property = System.getProperty("faststats.error-tracker-server");
        if (property != null) try {
            return new URI(property);
        } catch (final URISyntaxException e) {
            final var logger = LoggerFactory.factory().getLogger(SimpleMetrics.class);
            logger.error("Failed to parse error tracker server url: %s", e, property);
        }
        return URI.create("https://metrics.faststats.dev/v1/error");
    }

    // todo: improve logging to be less cluttered; dedupe code
    void submit() {
        if (!context.getConfig().errorTracking()) return;

        final var data = createData();
        if (data == null) return;

        try (final var byteOutput = new ByteArrayOutputStream();
             final var output = new GZIPOutputStream(byteOutput)) {
            output.write(data.toString().getBytes(UTF_8));
            output.finish();

            final var compressed = byteOutput.toByteArray();
            logger.info("Sending errors to: %s", url);
            logger.info("Uncompressed data: %s", data);
            // todo: dedupe this
            final var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                    .header("Content-Encoding", "gzip")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "Bearer " + context.getToken())
                    .header("User-Agent", context.getSdkInfo().getUserAgent())
                    .timeout(Duration.ofSeconds(3))
                    .uri(url)
                    .build();

            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            final var statusCode = response.statusCode();
            final var body = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Errors submitted with status code: %s (%s)", statusCode, body);
                clear();
            } else if (statusCode >= 300 && statusCode < 400) {
                logger.warn("Received redirect response from error server: %s (%s)", statusCode, body);
            } else if (statusCode >= 400 && statusCode < 500) {
                logger.error("Submitted invalid request to error server: %s (%s)", null, statusCode, body);
            } else if (statusCode >= 500 && statusCode < 600) {
                logger.error("Received server error response from error server: %s (%s)", null, statusCode, body);
            } else {
                logger.warn("Received unexpected response from error server: %s (%s)", statusCode, body);
            }
        } catch (final HttpConnectTimeoutException t) {
            logger.error("Error submission timed out after 3 seconds: %s", null, url);
        } catch (final ConnectException t) {
            logger.error("Failed to connect to error server: %s", null, url);
        } catch (final Throwable t) {
            logger.error("Failed to submit errors", t);
        }
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
            submissionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                final var thread = new Thread(runnable, "faststats-submitter");
                thread.setDaemon(true);
                return thread;
            });
            return submissionScheduler;
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
}
