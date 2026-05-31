package com.example;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.velocity.VelocityContext;

import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "example", name = "Example Plugin", version = "1.0.0",
        url = "https://example.com", authors = {"Your Name"})
public class ExamplePlugin {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private final AtomicInteger gameCount = new AtomicInteger();

    private final VelocityContext context;

    @Inject
    public ExamplePlugin(final VelocityContext.Builder contextBuilder) {
        this.context = contextBuilder
                .token("YOUR_TOKEN_HERE")
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
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        context.ready(); // register additional error handlers
    }

    @Subscribe
    public void onProxyStop(final ProxyShutdownEvent event) {
        context.shutdown(); // safely shut down configured services
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
