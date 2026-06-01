package dev.faststats;

import java.util.UUID;

public final class MockContext extends SimpleContext {
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
