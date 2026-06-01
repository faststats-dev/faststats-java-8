package dev.faststats.minestom;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.config.SimpleConfig;
import dev.faststats.data.Metric;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

final class MinestomMetricsImpl extends SimpleMetrics implements MinestomMetrics {
    @Async.Schedule
    @Contract(mutates = "io")
    private MinestomMetricsImpl(final Factory factory) throws IllegalStateException {
        super(factory);

        startSubmitting();
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) context.getConfig()).preSubmissionStart();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        metrics.addProperty("minecraft_version", MinecraftServer.VERSION_NAME);
        metrics.addProperty("online_mode", !(MinecraftServer.process().auth() instanceof Auth.Offline));
        metrics.addProperty("player_count", MinecraftServer.getConnectionManager().getOnlinePlayerCount());
        metrics.addProperty("server_type", "Minestom");
    }

    public static final class Factory extends SimpleMetrics.Factory implements MinestomMetrics.Factory {
        Factory(final MinestomContext context) {
            super(context);
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
        public MinestomMetrics create() throws IllegalStateException {
            return new MinestomMetricsImpl(this);
        }
    }
}
