package com.example;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.hytale.HytaleContext;

import java.util.concurrent.atomic.AtomicInteger;

public class ExamplePlugin extends JavaPlugin {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final AtomicInteger gameCount = new AtomicInteger();

    private final HytaleContext context = new HytaleContext.Factory(this, "YOUR_TOKEN_HERE")
            .errorTrackerService(factory -> factory.globalErrorTracker(ERROR_TRACKER).create())
            // .metrics(Metrics.Factory::create) // Define a minimal metrics instance without any custom metrics
            .metrics(factory -> factory
                    // Custom metrics require a corresponding data source in your project settings
                    .addMetric(Metric.number("game_count", gameCount::get))
                    .addMetric(Metric.string("server_version", () -> "1.0.0"))

                    // #onFlush is invoked after successful metrics submission
                    // This is useful for cleaning up cached data
                    .onFlush(() -> gameCount.set(0)) // reset game count on flush

                    .create())
            .create();

    public ExamplePlugin(final JavaPluginInit init) {
        super(init);
        context.ready(); // register additional error handlers
    }

    @Override
    protected void shutdown() {
        context.shutdown(); // safely shut down configured services
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
