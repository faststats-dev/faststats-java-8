package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * Shared FastStats context.
 * <p>
 * Platform-specific contexts should extend this class to provide a shared
 * configuration, token, and metrics factory for their environment.
 *
 * @since 0.24.0
 */
public sealed interface FastStatsContext permits SimpleContext {
    /**
     * Get the metrics configuration shared by services created from this context.
     *
     * @return the shared configuration
     * @since 0.24.0
     */
    @Contract(pure = true)
    Config getConfig();

    /**
     * Get the token shared by services created from this context.
     *
     * @return the shared token
     * @since 0.24.0
     */
    @Token
    @Contract(pure = true)
    String getToken();

    /**
     * Creates a new platform metrics factory bound to this context.
     *
     * @return a new platform metrics factory
     * @since 0.24.0
     */
    // todo: if the context is replaced with a factory pattern make the metrics instance context wide so only one can exist and querying the metrics instance is done ON the context
    @Contract(value = "-> new", pure = true)
    Metrics.Factory metricsFactory();

    /**
     * Creates a new feature flag service factory bound to this context.
     *
     * @return a new feature flag service factory
     * @since 0.24.0
     */
    // todo: if the context is replaced with a factory pattern make the feature flag service instance context wide so only one can exist and querying the service instance is done ON the context
    @Contract(value = "-> new", pure = true)
    FeatureFlagService.Factory featureFlagServiceFactory();

    /**
     * Get the registered internal/global error tracker, if one was configured.
     *
     * @return the internal/global error tracker
     * @since 0.24.0
     */
    @Contract(pure = true)
    Optional<ErrorTracker> errorTracker();

    // todo: only one global error tracker can exist, let it be defined on the context factory
    FastStatsContext globalErrorTracker(ErrorTracker errorTracker);

    FastStatsContext registerErrorTracker(ErrorTracker errorTracker);

    /**
     * Get the SDK information shared by services created from this context.
     *
     * @return the shared SDK information
     * @since 0.24.0
     */
    @Contract(pure = true)
    SdkInfo getSdkInfo();
}
