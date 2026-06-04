package com.example;

import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.faststats.minestom.MinestomContext;
import net.minestom.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicInteger;

public final class ExampleServer {
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware();
    private static final AtomicInteger gameCount = new AtomicInteger();

    private static final MinestomContext context = new MinestomContext.Factory("YOUR_TOKEN_HERE")
            .errorTrackerService(ERROR_TRACKER)
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

    public static void main(final String[] args) {
        final var server = MinecraftServer.init();

        server.start("0.0.0.0", 25565);
        MinecraftServer.getSchedulerManager().buildShutdownTask(ExampleServer::shutdown);
        
        context.ready(); // register additional error handlers and start metrics submission
    }

    public static void shutdown() {
        context.shutdown(); // safely shut down configured services
    }

    public static void startGame() {
        gameCount.incrementAndGet();
    }
}
