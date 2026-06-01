package dev.faststats.bukkit;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;

/**
 * Bukkit FastStats context.
 *
 * @since 0.24.0
 */
public final class BukkitContext extends SimpleContext {
    final Plugin plugin;

    private BukkitContext(final Factory factory, final Plugin plugin, @Token final String token) {
        super(factory, SimpleConfig.read(getConfigPath(plugin)), "bukkit", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected BukkitMetrics.Factory metricsFactory() {
        return new BukkitMetricsImpl.Factory(this);
    }

    @Override
    public void ready() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
            plugin.getServer().getPluginManager().registerEvents(new PaperEventListener(plugin, this), plugin);
        } catch (final ClassNotFoundException ignored) {
        }
    }

    private static Path getConfigPath(final Plugin plugin) {
        return getPluginsFolder(plugin).resolve("faststats").resolve("config.properties");
    }

    private static Path getPluginsFolder(final Plugin plugin) {
        try {
            return plugin.getServer().getPluginsFolder().toPath();
        } catch (final NoSuchMethodError e) {
            return plugin.getDataFolder().getParentFile().toPath();
        }
    }

    @Override
    public String getProjectName() {
        return plugin.getName();
    }

    public static final class Factory extends SimpleContext.Factory<BukkitContext, Factory> {
        private final Plugin plugin;
        private final @Token String token;

        public Factory(final Plugin plugin, @Token final String token) {
            this.plugin = plugin;
            this.token = token;
        }

        @Override
        public BukkitContext create() {
            return new BukkitContext(this, plugin, token);
        }
    }
}
