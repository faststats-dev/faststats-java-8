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
    Attributes getAttributes(); // todo: move into error tracker itself

    /**
     * Registers an additional error tracker for submission with this service.
     * <p>
     * Additional trackers registered here are submitted by the same context, but are not
     * used for internal FastStats errors.
     *
     * @param errorTracker the additional error tracker
     * @return this service
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    ErrorTrackerService registerErrorTracker(ErrorTracker errorTracker);
}
