package dev.faststats.minestom;

import dev.faststats.Metrics;
import dev.faststats.data.Metric;

/**
 * Minestom metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface MinestomMetrics extends Metrics permits MinestomMetricsImpl {
    sealed interface Factory extends Metrics.Factory permits MinestomMetricsImpl.Factory {
        @Override
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        @Override
        Factory onFlush(Runnable flush);

        @Override
        MinestomMetrics create() throws IllegalStateException;
    }
}
