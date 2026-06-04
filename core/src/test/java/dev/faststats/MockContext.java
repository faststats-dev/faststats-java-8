package dev.faststats;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MockContext extends SimpleContext {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();

    private MockContext(final Factory factory) throws IllegalArgumentException {
        super(factory, new MockConfig(UUID.randomUUID()), "test", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        initializeServices(factory);
    }

    @Override
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new MockMetrics(this);
            }
        };
    }

    @Override
    public String getProjectName() {
        return "Mock";
    }

    @Override
    public void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(executor.scheduleAtFixedRate(task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
    }

    private record MockConfig(UUID serverId) implements Config {
        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean submitMetrics() {
            return true;
        }

        @Override
        public boolean errorTracking() {
            return true;
        }

        @Override
        public boolean additionalMetrics() {
            return true;
        }

        @Override
        public boolean debug() {
            return true;
        }
    }

    public static final class Factory extends SimpleContext.Factory<MockContext, Factory> {
        @Override
        public MockContext create() {
            return new MockContext(this);
        }
    }
}
