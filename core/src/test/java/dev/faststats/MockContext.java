package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class MockContext extends SimpleContext {
    public MockContext() throws IllegalArgumentException {
        this(null);
    }

    public MockContext(@Nullable final ErrorTracker internalErrorTracker) throws IllegalArgumentException {
        super(new MockConfig(UUID.randomUUID()), "core:test", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", internalErrorTracker);
    }

    @Override
    public Metrics.Factory metricsFactory() {
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
}
