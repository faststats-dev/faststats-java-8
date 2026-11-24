package dev.faststats;

import com.google.gson.JsonPrimitive;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

final class SimpleAttributes implements Attributes {
    private final ConcurrentHashMap<String, JsonPrimitive> attributes;

    SimpleAttributes(final ConcurrentHashMap<String, JsonPrimitive> attributes) {
        this.attributes = attributes;
    }

    ConcurrentHashMap<String, JsonPrimitive> attributes() {
        return attributes;
    }

    @Override
    public Attributes put(final String key, final String value) {
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes put(final String key, final Number value) {
        if (!Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Value must be finite");
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes put(final String key, final boolean value) {
        attributes.put(key, new JsonPrimitive(value));
        return this;
    }

    @Override
    public Attributes remove(final String key) {
        attributes.remove(key);
        return this;
    }

    @Override
    public boolean containsKey(final String key) {
        return attributes.containsKey(key);
    }

    @Override
    public void forEachPrimitive(final BiConsumer<String, JsonPrimitive> action) {
        attributes.forEach(action);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleAttributes)) return false;
        final SimpleAttributes that = (SimpleAttributes) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }
}
