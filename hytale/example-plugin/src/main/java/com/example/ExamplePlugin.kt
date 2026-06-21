package com.example

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import dev.faststats.ErrorTracker
import dev.faststats.data.Metric
import dev.faststats.hytale.HytaleContext
import java.util.concurrent.atomic.AtomicInteger

class ExamplePlugin(init: JavaPluginInit) : JavaPlugin(init) {
    private val gameCount = AtomicInteger()

    private val context = HytaleContext.Factory(this, "YOUR_TOKEN_HERE")
        .errorTrackerService(ERROR_TRACKER)
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
        .create()

    override fun setup() {
        context.ready() // start metrics and errors submission
    }

    override fun shutdown() {
        context.shutdown() // safely shut down configured services
    }

    fun startGame() {
        gameCount.incrementAndGet()
    }

    companion object {
        val ERROR_TRACKER: ErrorTracker = ErrorTracker.contextAware()
    }
}
