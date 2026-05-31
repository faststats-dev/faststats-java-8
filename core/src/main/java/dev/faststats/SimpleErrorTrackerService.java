package dev.faststats;

import org.jspecify.annotations.Nullable;

final class SimpleErrorTrackerService implements ErrorTrackerService {
    private final ErrorTrackingSink sink;
    private final SimpleErrorTracker globalErrorTracker;
    private final Attributes attributes;

    SimpleErrorTrackerService(
            final ErrorTrackingSink sink,
            final ErrorTracker globalErrorTracker,
            final Attributes attributes
    ) {
        // todo: don't even let the user provide anything else
        if (!(globalErrorTracker instanceof final SimpleErrorTracker tracker)) {
            throw new IllegalArgumentException("Unsupported error tracker implementation: " + globalErrorTracker.getClass().getName());
        }
        this.sink = sink;
        this.globalErrorTracker = tracker;
        this.attributes = attributes;
        sink.startErrorSubmission();
    }

    @Override
    public ErrorTracker globalErrorTracker() {
        return globalErrorTracker;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public ErrorTrackerService registerErrorTracker(final ErrorTracker errorTracker) {
        // todo: the class is sealed this check will always succeed, cast directly
        if (!(errorTracker instanceof final SimpleErrorTracker tracker)) {
            throw new IllegalArgumentException("Unsupported error tracker implementation: " + errorTracker.getClass().getName());
        }
        sink.errorTrackers.add(tracker);
        sink.startErrorSubmission();
        return this;
    }

    static final class Factory implements ErrorTrackerService.Factory {
        private final ErrorTrackingSink sink;
        private @Nullable ErrorTracker globalErrorTracker;
        private @Nullable Attributes attributes;

        Factory(final ErrorTrackingSink sink) {
            this.sink = sink;
        }

        @Override
        public ErrorTrackerService.Factory globalErrorTracker(final ErrorTracker errorTracker) {
            this.globalErrorTracker = errorTracker;
            return this;
        }

        @Override
        public ErrorTrackerService.Factory attributes(final Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        @Override
        public ErrorTrackerService create() throws IllegalStateException {
            if (globalErrorTracker == null) throw new IllegalStateException("A global error tracker is required");
            final var attributes = this.attributes != null ? this.attributes : Attributes.empty();
            return new SimpleErrorTrackerService(sink, globalErrorTracker, attributes);
        }
    }
}
