package dev.faststats;

import org.jetbrains.annotations.Contract;

/**
 * A service for managing error trackers.
 * <p>
 * Use {@link FastStatsContext#errorTrackerService()} to access the context service instance.
 *
 * @since 0.24.0
 */
public sealed interface ErrorTrackerService permits SimpleErrorTrackerService {
    /**
     * Returns the global/internal error tracker configured for this service.
     *
     * @return the global/internal error tracker
     * @since 0.24.0
     */
    @Contract(pure = true)
    ErrorTracker globalErrorTracker();

    /**
     * Returns the global error context attributes configured for this service.
     *
     * @return the global error context attributes
     * @since 0.24.0
     */
    @Contract(pure = true)
    Attributes getAttributes();

    /**
     * Registers an additional error tracker for submission with this service.
     * <p>
     * The global/internal tracker returned by {@link #globalErrorTracker()} is configured
     * by the service factory. Additional trackers registered here are submitted by
     * the same context, but are not used for internal FastStats errors.
     *
     * @param errorTracker the additional error tracker
     * @return this service
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    ErrorTrackerService registerErrorTracker(ErrorTracker errorTracker);

    /**
     * An error tracker service factory.
     *
     * @since 0.24.0
     */
    // todo: remove factory? there is almost no gain from it and i don't see any reason why there would be configuration required
    sealed interface Factory permits SimpleErrorTrackerService.Factory {
        /**
         * Sets the global/internal error tracker for services created by this factory.
         *
         * @param errorTracker the global/internal error tracker
         * @return the error tracker service factory
         * @since 0.24.0
         */
        @Contract(mutates = "this")
        Factory globalErrorTracker(ErrorTracker errorTracker);

        /**
         * Sets the global error context attributes for services created by this factory.
         *
         * @param attributes the global error context attributes
         * @return the error tracker service factory
         * @since 0.24.0
         */
        @Contract(mutates = "this")
        Factory attributes(Attributes attributes);

        /**
         * Creates a new error tracker service.
         *
         * @return the error tracker service
         * @throws IllegalStateException if no global error tracker was configured
         * @since 0.24.0
         */
        @Contract(value = " -> new", pure = true)
        ErrorTrackerService create() throws IllegalStateException;
    }
}
