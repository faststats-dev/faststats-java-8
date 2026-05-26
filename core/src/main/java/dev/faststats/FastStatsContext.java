package dev.faststats;

import org.jetbrains.annotations.Contract;

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
    @Contract(value = "-> new", pure = true)
    Metrics.Factory metricsFactory();

    /**
     * Creates a new feature flag service factory bound to this context.
     *
     * @return a new feature flag service factory
     * @since 0.24.0
     */
    @Contract(value = "-> new", pure = true)
    FeatureFlagService.Factory featureFlagServiceFactory();

    /**
     * Create and attach a new context-aware error tracker.
     * <p>
     * This tracker will automatically track errors that occur in the same class loader as the tracker itself.
     * <p>
     * You can still manually track errors using {@code #trackError}.
     *
     * @return the error tracker
     * @see #unawareErrorTracker()
     * @see ErrorTracker#attachErrorContext(ClassLoader)
     * @see ErrorTracker#trackError(String, boolean)
     * @see ErrorTracker#trackError(Throwable, boolean)
     * @since 0.24.0
     */
    @Contract(value = " -> new")
    ErrorTracker awareErrorTracker();

    /**
     * Create a new context-unaware error tracker.
     * <p>
     * This tracker will not automatically track any errors.
     * <p>
     * You have to manually track errors using {@code #trackError}.
     *
     * @return the error tracker
     * @see #awareErrorTracker()
     * @see ErrorTracker#trackError(String)
     * @see ErrorTracker#trackError(Throwable)
     * @since 0.24.0
     */
    @Contract(value = " -> new", pure = true)
    ErrorTracker unawareErrorTracker();

    // todo: add docs
    @Contract(pure = true)
    SdkInfo getSdkInfo();
}
