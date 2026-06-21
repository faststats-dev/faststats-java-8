package com.example

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import dev.faststats.ErrorTracker
import dev.faststats.data.Metric
import dev.faststats.velocity.VelocityContext
import java.util.concurrent.atomic.AtomicInteger

@Plugin(
    id = "example",
    name = "Example Plugin",
    version = "1.0.0",
    url = "https://example.com",
    authors = ["Your Name"],
)
class ExamplePlugin @Inject constructor(contextBuilder: VelocityContext.Builder) {
    private val gameCount = AtomicInteger()

    private val context = contextBuilder
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

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        context.ready() // start metrics and errors submission
    }

    @Subscribe
    fun onProxyStop(event: ProxyShutdownEvent) {
        context.shutdown() // safely shut down configured services
    }

    fun startGame() {
        gameCount.incrementAndGet()
    }

    companion object {
        val ERROR_TRACKER: ErrorTracker = ErrorTracker.contextAware()
    }
}
