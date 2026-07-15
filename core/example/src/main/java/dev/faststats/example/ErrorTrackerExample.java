package dev.faststats.example;

import dev.faststats.Attributes;
import dev.faststats.ErrorTracker;
import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;

public final class ErrorTrackerExample {
    // Context-aware: automatically tracks uncaught errors from the same class loader
    public static final ErrorTracker CONTEXT_AWARE = ErrorTracker.contextAware()
            // Filter expected noise before it is submitted
            .ignoreError(InvocationTargetException.class, "Expected .* but got .*")
            .ignoreError(AccessDeniedException.class);

    // Context-unaware: only tracks errors passed to trackError() manually
    public static final ErrorTracker CONTEXT_UNAWARE = ErrorTracker.contextUnaware()
            // Replace sensitive values in error messages before submission
            .anonymize("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "[email hidden]")
            .anonymize("Bearer [A-Za-z0-9._~+/=-]+", "Bearer [token hidden]")
            .anonymize("AKIA[0-9A-Z]{16}", "[aws-key hidden]")
            .anonymize("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[uuid hidden]")
            .anonymize("([?&](?:api_?key|token|secret)=)[^&\\s]+", "$1[redacted]");

    public static final FastStatsContext CONTEXT = getContextFactory()
            .errorTrackerService(CONTEXT_AWARE) // Set the global/internal error tracker
            .create();

    static {
        // Attributes on the global error tracker are attached to all reports
        CONTEXT_AWARE.getAttributes()
                .put("environment", "production")
                .put("component", "global-error-handler");

        // Tracker-wide attributes are attached to every report submitted by this tracker
        CONTEXT_UNAWARE.getAttributes()
                .put("component", "manual-error-handler");

        // Register an additional tracker for submission
        CONTEXT.errorTrackerService()
                .orElseThrow(() -> new NoSuchElementException("Error tracker service is not configured"))
                .registerErrorTracker(CONTEXT_UNAWARE);
    }

    public static void manualTracking() {
        try {
            throw new RuntimeException("Something went wrong!");
        } catch (final Exception e) {
            CONTEXT_UNAWARE.trackError(e)
                    // Add additional attributes for more context
                    .attributes(Attributes.empty()
                            .put("operation", "manualTracking")
                            .put("severity", "warning")
                            .put("retryable", false))
                    // Define whether the error was properly handled
                    .handled(false);
        }
    }

    private static SimpleContext.Factory<?, ?> getContextFactory() {
        return null;
    }
}
