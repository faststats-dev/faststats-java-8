package dev.faststats.nukkit;

import cn.nukkit.plugin.PluginBase;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

/**
 * Nukkit FastStats context.
 *
 * @since 0.24.0
 */
public final class NukkitContext extends SimpleContext {
    final PluginBase plugin;

    private NukkitContext(final Factory factory, final PluginBase plugin, @Token final String token) {
        super(factory, SimpleConfig.read(Path.of(plugin.getServer().getPluginPath(), "faststats", "config.properties")), "nukkit", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new NukkitMetricsImpl(this, ((NukkitContext) context).plugin);
            }
        };
    }

    @Override
    public String getProjectName() {
        return plugin.getName();
    }

    public static final class Factory extends SimpleContext.Factory<NukkitContext, Factory> {
        private final PluginBase plugin;
        private final @Token String token;

        public Factory(final PluginBase plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        public NukkitContext create() {
            return new NukkitContext(this, plugin, token);
        }
    }
}
