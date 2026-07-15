package dev.faststats.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

abstract class SimpleMetric<T> implements Metric<T> {
    private final @SourceId String id;
    protected final Callable<? extends @Nullable T> callable;

    private SimpleMetric(@SourceId final String id, final Callable<? extends @Nullable T> callable) throws IllegalArgumentException {
        if (!id.matches(SourceId.PATTERN)) {
            throw new IllegalArgumentException("Invalid source id '" + id + "', must match '" + SourceId.PATTERN + "'");
        }
        this.id = id;
        this.callable = callable;
    }

    @Override
    public final @SourceId String getId() {
        return id;
    }

    @Override
    public final Optional<T> compute() throws Exception {
        return Optional.ofNullable(callable.call());
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final SimpleMetric<?> that = (SimpleMetric<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SimpleMetric{" +
                "id='" + id + '\'' +
                '}';
    }

    static final class Json<T extends JsonElement> extends SimpleMetric<T> {
        public Json(
                @SourceId final String id,
                final Callable<? extends @Nullable T> callable
        ) throws IllegalArgumentException {
            super(id, callable);
        }

        @Override
        public Optional<JsonElement> getData() throws Exception {
            return Optional.ofNullable(callable.call());
        }
    }

    static final class Array<T> extends SimpleMetric<T[]> {
        public Array(
                @SourceId final String id,
                final Callable<? extends T @Nullable []> callable
        ) throws IllegalArgumentException {
            super(id, callable);
        }

        @Override
        public Optional<JsonElement> getData() throws Exception {
            final var data = callable.call();
            if (data == null) return Optional.empty();

            final var elements = new JsonArray(data.length);
            for (final var d : data) {
                if (d instanceof final Boolean b) elements.add(b);
                else if (d instanceof final Number n) elements.add(n);
                else elements.add(d.toString());
            }
            return Optional.of(elements);
        }
    }

    static final class Map<T> extends SimpleMetric<java.util.Map<String, ? extends T>> {
        public Map(
                @SourceId final String id,
                final Callable<? extends java.util.@Nullable Map<String, ? extends T>> callable
        ) throws IllegalArgumentException {
            super(id, callable);
        }

        @Override
        public Optional<JsonElement> getData() throws Exception {
            final var data = callable.call();
            if (data == null) return Optional.empty();

            final var object = new JsonObject();
            data.forEach((key, value) -> {
                if (value instanceof final Boolean bool) object.addProperty(key, bool);
                else if (value instanceof final Number number) object.addProperty(key, number);
                else object.addProperty(key, value.toString());
            });
            return Optional.of(object);
        }
    }

    static final class Primitive<T> extends SimpleMetric<T> {
        public Primitive(
                @SourceId final String id,
                final Callable<? extends @Nullable T> callable
        ) throws IllegalArgumentException {
            super(id, callable);
        }

        @Override
        public Optional<JsonElement> getData() throws Exception {
            final var data = callable.call();
            if (data == null) return Optional.empty();

            if (data instanceof final Boolean bool)
                return Optional.of(new JsonPrimitive(bool));
            if (data instanceof final Number number)
                return Optional.of(new JsonPrimitive(number));
            return Optional.of(new JsonPrimitive(data.toString()));
        }
    }
}
