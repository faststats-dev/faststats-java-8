package dev.faststats;

import java.util.UUID;

public final class MockContext extends SimpleContext {
    private MockContext() throws IllegalArgumentException {
        super(new MockConfig(UUID.randomUUID()), "core:test", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
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

    public static final class Factory extends FastStatsContextFactory<MockContext, Factory> {
        @Override
        protected MockContext createContext() {
            return new MockContext();
        }
    }
}
