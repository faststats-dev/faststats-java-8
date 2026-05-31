package dev.faststats;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Factory for creating a configured FastStats context.
 * <p>
 * Platform implementations may extend this class with constructors that accept
 * platform-specific objects before creating the context.
 *
 * @param <C> the context type created by this factory
 * @param <F> the concrete factory type
 * @since 0.24.0
 */
public abstract class FastStatsContextFactory<C extends SimpleContext, F extends FastStatsContextFactory<C, F>> {
    private @Nullable Function<Metrics.Factory, Metrics> metrics = null;
    private @Nullable Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService;
    private @Nullable ErrorTracker errorTracker;

    /**
     * Configures the global/internal error tracker for the context.
     *
     * @param errorTracker the global/internal error tracker
     * @return this factory
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    public F errorTrackerService(final ErrorTracker errorTracker) {
        this.errorTracker = errorTracker;
        return self();
    }

    /**
     * Configures and creates the single metrics instance for the context.
     *
     * @param metrics a function that receives a new metrics factory and returns the built metrics instance
     * @return this factory
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    public F metrics(final Function<Metrics.Factory, Metrics> metrics) {
        this.metrics = metrics;
        return self();
    }

    /**
     * Configures and creates the single feature flag service instance for the context.
     *
     * @param featureFlagService a function that receives a new service factory and returns the built service instance
     * @return this factory
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    public F featureFlagService(
            final Function<FeatureFlagService.Factory, FeatureFlagService> featureFlagService
    ) {
        this.featureFlagService = featureFlagService;
        return self();
    }

    /**
     * Creates the configured context.
     *
     * @return the configured context
     * @since 0.24.0
     */
    // todo: mutation sucks, move factory into context
    @Contract(value = " -> new", mutates = "io")
    public final C create() {
        final var context = createContext();
        if (metrics != null)
            context.setMetrics(metrics.apply(context.metricsFactory()));
        if (featureFlagService != null)
            context.setFeatureFlagService(featureFlagService.apply(context.featureFlagServiceFactory()));
        if (errorTracker != null)
            context.setErrorTrackerService(errorTracker);
        return context;
    }

    @SuppressWarnings("unchecked")
    private F self() {
        return (F) this;
    }

    /**
     * Creates the platform-specific context instance.
     *
     * @return the platform-specific context
     * @since 0.24.0
     */
    @Contract(value = " -> new", mutates = "io")
    protected abstract C createContext();
}
