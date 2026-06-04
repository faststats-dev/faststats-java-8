package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.fabric.FabricContext;
import net.fabricmc.api.ModInitializer;

import java.util.concurrent.atomic.AtomicInteger;

public class ExampleMod implements ModInitializer {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final AtomicInteger gameCount = new AtomicInteger();

    private final FabricContext context = new FabricContext.Factory(
            "example-mod", // your mod id as defined in fabric.mod.json
            "YOUR_TOKEN_HERE"
    )
            // .metrics(Metrics.Factory::create) // Define a minimal metrics instance without any custom metrics
            .metrics(factory -> factory
                    // Custom metrics require a corresponding data source in your project settings
                    .addMetric(Metric.number("game_count", gameCount::get))
                    .addMetric(Metric.string("server_version", () -> "1.0.0"))

                    // #onFlush is invoked after successful metrics submission
                    // This is useful for cleaning up cached data
                    .onFlush(() -> gameCount.set(0)) // reset game count on flush

                    .create())
            .errorTrackerService(ERROR_TRACKER)
            .create();

    @Override
    public void onInitialize() {
        // your actual logic
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
