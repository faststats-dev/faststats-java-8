package dev.faststats.fabric;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

final class FabricMetricsServer extends FabricMetrics {
    private @Nullable MinecraftServer server;

    public FabricMetricsServer(final Factory factory, final ModContainer mod) throws IllegalStateException {
        super(factory, mod);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        assert server != null : "Server not initialized";
        metrics.addProperty("online_mode", server.usesAuthentication());
        metrics.addProperty("player_count", server.getPlayerCount());
        appendFabricData(metrics, "Fabric");
    }
}
