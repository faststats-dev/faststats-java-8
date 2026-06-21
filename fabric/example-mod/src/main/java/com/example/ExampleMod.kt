package com.example

import dev.faststats.ErrorTracker
import dev.faststats.data.Metric
import dev.faststats.fabric.FabricContext
import net.fabricmc.api.ModInitializer
import java.util.concurrent.atomic.AtomicInteger

class ExampleMod : ModInitializer {
    private val gameCount = AtomicInteger()

    private val context = FabricContext.Factory(
        "example-mod", // your mod id as defined in fabric.mod.json
        "YOUR_TOKEN_HERE",
    )
        // .metrics(Metrics.Factory::create) // Define a minimal metrics instance without any custom metrics
        .metrics { factory ->
            factory
                // Custom metrics require a corresponding data source in your project settings
                .addMetric(Metric.number("game_count") { gameCount.get() })
                .addMetric(Metric.string("server_version") { "1.0.0" })

                // #onFlush is invoked after successful metrics submission
                // This is useful for cleaning up cached data
                .onFlush { gameCount.set(0) } // reset game count on flush

                .create()
        }
        .errorTrackerService(ERROR_TRACKER)
        .create()

    override fun onInitialize() {
        // your actual logic
    }

    fun startGame() {
        gameCount.incrementAndGet()
    }

    companion object {
        val ERROR_TRACKER: ErrorTracker = ErrorTracker.contextAware()
    }
}
