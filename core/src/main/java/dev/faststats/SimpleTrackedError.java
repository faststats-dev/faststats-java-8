package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

final class SimpleTrackedError implements TrackedError {
    private Attributes attributes = Attributes.empty();
    private boolean handled = true;
    private final Throwable error;

    SimpleTrackedError(final Throwable error) {
        this.error = error;
    }

    @Override
    public Throwable error() {
        return error;
    }

    @Override
    public boolean handled() {
        return handled;
    }

    @Override
    public TrackedError handled(final boolean handled) {
        this.handled = handled;
        return this;
    }

    @Override
    public Attributes attributes() {
        return Attributes.copyOf(attributes);
    }

    @Override
    public TrackedError attributes(final Attributes attributes) {
        this.attributes = Attributes.copyOf(attributes);
        return this;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimpleTrackedError that = (SimpleTrackedError) o;
        return handled == that.handled
                && Objects.equals(attributes, that.attributes)
                && deepEquals(error, that.error, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, handled, hash(error, Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    // fixme: hacky shit; it only has to compile and pass tests for now
    private static boolean deepEquals(
            @Nullable final Throwable first,
            @Nullable final Throwable second,
            final Set<Throwable> visited
    ) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        if (first.getClass() != second.getClass()) return false;
        if (!Objects.equals(first.getMessage(), second.getMessage())) return false;
        if (!Arrays.equals(first.getStackTrace(), second.getStackTrace())) return false;
        if (!visited.add(first)) return true;
        return deepEquals(first.getCause(), second.getCause(), visited);
    }

    private static int hash(@Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return 0;
        return Objects.hash(
                error.getClass(),
                error.getMessage(),
                Arrays.hashCode(error.getStackTrace()),
                hash(error.getCause(), visited)
        );
    }
}
