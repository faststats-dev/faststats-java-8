package dev.faststats.bungee;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Contract;

/**
 * BungeeCord FastStats context.
 *
 * @since 0.24.0
 */
public final class BungeeContext extends SimpleContext {
    final Plugin plugin;

    private BungeeContext(final Factory factory, final Plugin plugin, @Token final String token) {
        super(factory, SimpleConfig.read(plugin.getProxy().getPluginsFolder().toPath().resolve("faststats").resolve("config.properties")), "bungeecord", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new BungeeMetricsImpl(this, plugin);
            }
        };
    }

    @Override
    public String getProjectName() {
        return plugin.getDescription().getName();
    }

    public static final class Factory extends SimpleContext.Factory<BungeeContext, Factory> {
        private final Plugin plugin;
        private final @Token String token;

        public Factory(final Plugin plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        public BungeeContext create() {
            return new BungeeContext(this, plugin, token);
        }
    }
}
