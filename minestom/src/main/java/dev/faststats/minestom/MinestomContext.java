package dev.faststats.minestom;

import dev.faststats.ErrorTracker;
import dev.faststats.ErrorTrackerService;
import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Minestom FastStats context.
 *
 * @since 0.24.0
 */
public final class MinestomContext extends SimpleContext {
    private final Set<Task> tasks = new CopyOnWriteArraySet<>();

    MinestomContext(final Factory factory, @Token final String token) {
        super(factory, SimpleConfig.read(Path.of("faststats", "config.properties")), "minestom", token);
        initializeServices(factory);
    }

    @Override
    public void ready() {
        if (!ready) errorTrackerService().map(ErrorTrackerService::globalErrorTracker).ifPresent(errorTracker -> {
            final var handler = MinecraftServer.getExceptionManager().getExceptionHandler();
            MinecraftServer.getExceptionManager().setExceptionHandler(error -> {
                handler.handleException(error);
                if (!ErrorTracker.isSameLoader(getClass().getClassLoader(), error)) return;
                errorTracker.trackError(error);
            });
        });
        super.ready();
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected MinestomMetrics.Factory metricsFactory() {
        return new MinestomMetricsImpl.Factory(this);
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(getProjectName());
    }

    @Override
    public String getProjectName() {
        return MinecraftServer.getBrandName();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        final var scheduleTask = MinecraftServer.getSchedulerManager().scheduleTask(
                task,
                TaskSchedule.duration(initialDelay, unit.toChronoUnit()),
                TaskSchedule.duration(period, unit.toChronoUnit())
        );
        tasks.add(scheduleTask);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(Task::cancel);
        tasks.clear();
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
