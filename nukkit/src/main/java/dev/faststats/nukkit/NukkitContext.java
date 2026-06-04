package dev.faststats.nukkit;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.TaskHandler;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Nukkit FastStats context.
 *
 * @since 0.24.0
 */
public final class NukkitContext extends SimpleContext {
    private final Set<TaskHandler> tasks = new CopyOnWriteArraySet<>();
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

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        final var scheduledTask = plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(
                plugin, task, toTicks(initialDelay, unit), toTicks(period, unit), true
        );
        tasks.add(scheduledTask);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(TaskHandler::cancel);
        tasks.clear();
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

    private static int toTicks(final long time, final TimeUnit unit) {
        return Math.max(1, (int) (unit.toMillis(time) / 50L));
    }
}
