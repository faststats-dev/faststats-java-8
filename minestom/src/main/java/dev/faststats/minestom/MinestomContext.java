package dev.faststats.minestom;

import dev.faststats.FastStatsContextFactory;
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
    MinestomContext(@Token final String token) {
        super(SimpleConfig.read(Path.of("faststats", "config.properties")), "minestom", token);
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

    public static final class Factory extends FastStatsContextFactory<MinestomContext, Factory> {
        private final @Token String token;

        public Factory(@Token final String token) {
            this.token = token;
        }

        @Override
        protected MinestomContext createContext() {
            return new MinestomContext(token);
        }
    }
}
