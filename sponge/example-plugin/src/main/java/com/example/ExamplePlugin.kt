package com.example

import com.google.inject.Inject
import dev.faststats.ErrorTracker
import dev.faststats.data.Metric
import dev.faststats.sponge.SpongeContext
import org.spongepowered.api.Server
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.util.concurrent.atomic.AtomicInteger

@Plugin("example")
class ExamplePlugin {
    @Inject
    private lateinit var contextBuilder: SpongeContext.Builder

    private val gameCount = AtomicInteger()
    private var context: SpongeContext? = null

    @Listener
    fun onServerStart(event: StartedEngineEvent<Server>) {
        val context = contextBuilder
            .token("YOUR_TOKEN_HERE")
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
        this.context = context
        context.ready() // start metrics and errors submission
    }

    @Listener
    fun onServerStop(event: StoppingEngineEvent<Server>) {
        context?.shutdown() // safely shut down configured services
    }

    fun startGame() {
        gameCount.incrementAndGet()
    }

    companion object {
        val ERROR_TRACKER: ErrorTracker = ErrorTracker.contextAware()
    }
}
