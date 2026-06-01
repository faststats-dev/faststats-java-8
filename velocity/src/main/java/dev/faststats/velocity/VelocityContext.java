package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Velocity FastStats context.
 *
 * @since 0.24.0
 */
public final class VelocityContext extends SimpleContext {
    final PluginContainer plugin;
    final ProxyServer server;

    private VelocityContext(
            final Factory factory, final PluginContainer plugin,
            final ProxyServer server,
            @DataDirectory final Path dataDirectory,
            @Token final String token
    ) {
        super(factory, SimpleConfig.read(dataDirectory.resolveSibling("faststats").resolve("config.properties")), "velocity", token);
        this.plugin = plugin;
        this.server = server;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new VelocityMetricsImpl(this);
            }
        };
    }

    @Override
    public String getProjectName() {
        return plugin.getDescription().getId();
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.24.0
     */
    public static class Factory extends SimpleContext.Factory<VelocityContext, Factory> {
        private final PluginContainer plugin;
        private final ProxyServer server;
        private final Path dataDirectory;
        private @Token
        @Nullable String token;

        /**
         * Creates a new Velocity context builder.
         *
         * @param server        the velocity server
         * @param dataDirectory the plugin data directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.24.0
         */
        @Inject
        public Factory(
                final PluginContainer plugin,
                final ProxyServer server,
                @DataDirectory final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.server = server;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Sets the FastStats project token used by the context created from this factory.
         *
         * @param token the FastStats project token
         * @return this factory
         * @throws IllegalArgumentException if the token is invalid
         * @since 0.24.0
         */
        public Factory token(@Token final String token) {
            this.token = token;
            return this;
        }

        @Override
        public VelocityContext create() {
            if (token == null) throw new IllegalStateException("Token not configured");
            return new VelocityContext(this, plugin, server, dataDirectory, token);
        }
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.24.0
     */
    public static final class Builder extends Factory {
        @Inject
        public Builder(
                final PluginContainer plugin,
                final ProxyServer server,
                @DataDirectory final Path dataDirectory
        ) {
            super(plugin, server, dataDirectory);
        }
    }
}
