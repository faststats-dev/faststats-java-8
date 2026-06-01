package dev.faststats.bukkit;

import dev.faststats.Metrics;
import dev.faststats.data.Metric;

/**
 * Bukkit metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface BukkitMetrics extends Metrics permits BukkitMetricsImpl {
    sealed interface Factory extends Metrics.Factory permits BukkitMetricsImpl.Factory {
        @Override
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        @Override
        Factory onFlush(Runnable flush);

        @Override
        BukkitMetrics create() throws IllegalStateException;
    }
}
