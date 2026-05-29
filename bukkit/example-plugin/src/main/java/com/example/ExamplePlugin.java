package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.data.Metric;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

public final class ExamplePlugin extends JavaPlugin {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.aware();
    private final AtomicInteger gameCount = new AtomicInteger();
    private final BukkitContext context = new BukkitContext(this, "YOUR_TOKEN_HERE")
            .globalErrorTracker(ERROR_TRACKER);

    private final BukkitMetrics metrics = context.metricsFactory()
            // Custom metrics require a corresponding data source in your project settings
            .addMetric(Metric.number("game_count", gameCount::get))
            .addMetric(Metric.string("server_version", () -> "1.0.0"))

            // #onFlush is invoked after successful metrics submission
            // This is useful for cleaning up cached data
            .onFlush(() -> gameCount.set(0)) // reset game count on flush

            .create();

    @Override
    public void onEnable() {
        metrics.ready(); // register additional error handlers
    }

    @Override
    public void onDisable() {
        metrics.shutdown(); // safely shut down metrics submission
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
