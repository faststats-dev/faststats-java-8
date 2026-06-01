package dev.faststats.minestom;

import dev.faststats.ErrorTracker;
import dev.faststats.ErrorTrackerService;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

/**
 * Minestom FastStats context.
 *
 * @since 0.24.0
 */
public final class MinestomContext extends SimpleContext {
    MinestomContext(final Factory factory, @Token final String token) {
        super(factory, SimpleConfig.read(Path.of("faststats", "config.properties")), "minestom", token);
        initializeServices(factory);
    }

    @Override
    public void ready() {
        super.ready();
        errorTrackerService().map(ErrorTrackerService::globalErrorTracker).ifPresent(errorTracker -> {
            final var handler = MinecraftServer.getExceptionManager().getExceptionHandler();
            MinecraftServer.getExceptionManager().setExceptionHandler(error -> {
                handler.handleException(error);
                if (!ErrorTracker.isSameLoader(getClass().getClassLoader(), error)) return;
                errorTracker.trackError(error);
            });
        });
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected MinestomMetrics.Factory metricsFactory() {
        return new MinestomMetricsImpl.Factory(this);
    }

    @Override
    public String getProjectName() {
        return MinecraftServer.getBrandName();
    }

    public static final class Factory extends SimpleContext.Factory<MinestomContext, Factory> {
        private final @Token String token;

        public Factory(@Token final String token) {
            this.token = token;
        }

        @Override
        public MinestomContext create() {
            return new MinestomContext(this, token);
        }
    }
}
