package com.example

import dev.faststats.ErrorTracker
import dev.faststats.data.Metric
import dev.faststats.minestom.MinestomContext
import net.minestom.server.MinecraftServer
import java.util.concurrent.atomic.AtomicInteger

object ExampleServer {
    val ERROR_TRACKER: ErrorTracker = ErrorTracker.contextAware()
    private val gameCount = AtomicInteger()

    private val context = MinestomContext.Factory("YOUR_TOKEN_HERE")
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

    @JvmStatic
    fun main(args: Array<String>) {
        val server = MinecraftServer.init()

        server.start("0.0.0.0", 25565)
        MinecraftServer.getSchedulerManager().buildShutdownTask { shutdown() }

        context.ready() // start metrics and errors submission
    }

    fun shutdown() {
        context.shutdown() // safely shut down configured services
    }

    fun startGame() {
        gameCount.incrementAndGet()
    }
}
