package dev.faststats;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class MockContext extends SimpleContext {
    public MockContext(final UUID serverId, final boolean debug) throws IllegalArgumentException {
        super(new MockConfig(serverId, debug), "core:test", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
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

    private record MockConfig(UUID serverId, boolean debug) implements dev.faststats.Config {
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
    }
}
