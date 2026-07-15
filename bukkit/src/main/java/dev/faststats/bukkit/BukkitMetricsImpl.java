package dev.faststats.bukkit;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.data.Metric;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.function.Supplier;

final class BukkitMetricsImpl extends SimpleMetrics implements BukkitMetrics {
    private final Plugin plugin;

    private final String minecraftVersion;
    private final String pluginVersion;
    private final String serverType;
    private final String serverVersion;

    @SuppressWarnings({"deprecation", "Convert2MethodRef"})
    private BukkitMetricsImpl(final Factory factory, final Plugin plugin) throws IllegalStateException {
        super(factory);

        this.plugin = plugin;
        final Server server = plugin.getServer();

        this.serverVersion = server.getVersion().split("\\(MC: |\\)", 2)[0].trim();
        this.pluginVersion = tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                .orElseGet(() -> plugin.getDescription().getVersion());
        this.minecraftVersion = firstPresent(
                tryOrEmpty(() -> server.getMinecraftVersion()),
                tryOrEmpty(() -> server.getBukkitVersion().split("-", 2)[0]),
                tryOrEmpty(() -> server.getVersion().split("\\(MC: |\\)", 3)[1])
        ).orElse(serverVersion);
        this.serverType = server.getName();
    }

    private boolean checkOnlineMode() {
        final Server server = plugin.getServer();
        return firstPresent(
                tryOrEmpty(() -> server.getServerConfig().isProxyOnlineMode()),
                tryOrEmpty(this::isProxyOnlineMode)
        ).orElseGet(server::getOnlineMode);
    }

    private boolean isProxyOnlineMode() {
        final Server server = plugin.getServer();
        final ConfigurationSection proxies = server.spigot().getPaperConfig().getConfigurationSection("proxies");
        if (proxies == null) return false;

        if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) return true;

        final ConfigurationSection settings = server.spigot().getSpigotConfig().getConfigurationSection("settings");
        if (settings == null) return false;

        return settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("minecraft_version", minecraftVersion);
        metrics.addProperty("online_mode", checkOnlineMode());
        metrics.addProperty("player_count", getPlayerCount());
        metrics.addProperty("plugin_version", pluginVersion);
        metrics.addProperty("server_type", serverType);
        metrics.addProperty("platform_version", serverVersion);
    }

    private int getPlayerCount() {
        try {
            return plugin.getServer().getOnlinePlayers().size();
        } catch (final Throwable t) {
            logger.error("Failed to get player count", t);
            context.errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
            return 0;
        }
    }

    private <T> Optional<T> tryOrEmpty(final Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (final NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    @SafeVarargs
    private static <T> Optional<T> firstPresent(final Optional<T>... options) {
        for (final Optional<T> option : options) {
            if (option.isPresent()) return option;
        }
        return Optional.empty();
    }

    public static final class Factory extends SimpleMetrics.Factory implements BukkitMetrics.Factory {
        private final Plugin plugin;

        Factory(final BukkitContext context, final Plugin plugin) {
            super(context);
            this.plugin = plugin;
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            return (Factory) super.addMetric(metric);
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            return (Factory) super.onFlush(flush);
        }

        @Override
        public BukkitMetrics create() throws IllegalStateException {
            return new BukkitMetricsImpl(this, plugin);
        }
    }
}
