package dev.faststats.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A metric.
 *
 * @param <T> the metric data type
 * @since 0.24.0
 */
public interface Metric<T> {
    /**
     * Get the source id.
     *
     * @return the source id
     * @since 0.24.0
     */
    @SourceId
    @Contract(pure = true)
    String getId();

    /**
     * Compute the metric data.
     *
     * @return an optional containing the metric data
     * @throws Exception if unable to compute the metric data
     * @implSpec The implementation must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     * @deprecated This method only adds unnecessary mental overhead. Use {@link #getData()} instead.
     */
    @Contract(pure = true)
    @ApiStatus.OverrideOnly
    @Deprecated(since = "0.28.0", forRemoval = true)
    default Optional<? extends T> compute() throws Exception {
        return Optional.empty();
    }

    /**
     * Get the metric data as a JSON element.
     *
     * @return an optional containing the metric data as {@link JsonElement}
     * @throws Exception if unable to get the metric data
     * @implSpec The implementation must be thread-safe and pure (i.e. not modify any shared state).
     * @since 0.24.0
     */
    @Contract(pure = true)
    @ApiStatus.OverrideOnly
    Optional<JsonElement> getData() throws Exception;

    /**
     * Create a JsonObject metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the JsonObject metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.28.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<JsonObject> object(@SourceId final String id, final Callable<@Nullable JsonObject> callable) throws IllegalArgumentException {
        return new SimpleMetric.Json<>(id, callable);
    }

    /**
     * Create a JsonArray metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the JsonArray metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.28.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<JsonArray> array(@SourceId final String id, final Callable<@Nullable JsonArray> callable) throws IllegalArgumentException {
        return new SimpleMetric.Json<>(id, callable);
    }

    /**
     * Create a JsonPromitive metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the JsonPrimitive metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.28.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<JsonPrimitive> primitive(@SourceId final String id, final Callable<@Nullable JsonPrimitive> callable) throws IllegalArgumentException {
        return new SimpleMetric.Json<>(id, callable);
    }

    /**
     * Create a string array metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the string array metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<String[]> stringArray(@SourceId final String id, final Callable<String @Nullable []> callable) throws IllegalArgumentException {
        return new SimpleMetric.Array<>(id, callable);
    }

    /**
     * Create a boolean array metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the boolean array metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Boolean[]> booleanArray(@SourceId final String id, final Callable<Boolean @Nullable []> callable) throws IllegalArgumentException {
        return new SimpleMetric.Array<>(id, callable);
    }

    /**
     * Create a number array metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the number array metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Number[]> numberArray(@SourceId final String id, final Callable<Number @Nullable []> callable) throws IllegalArgumentException {
        return new SimpleMetric.Array<>(id, callable);
    }

    /**
     * Create a string map metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the string map metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Map<String, ? extends String>> stringMap(@SourceId final String id, final Callable<? extends @Nullable Map<String, String>> callable) throws IllegalArgumentException {
        return new SimpleMetric.Map<>(id, callable);
    }

    /**
     * Create a boolean map metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the boolean map metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Map<String, ? extends Boolean>> booleanMap(@SourceId final String id, final Callable<? extends @Nullable Map<String, Boolean>> callable) throws IllegalArgumentException {
        return new SimpleMetric.Map<>(id, callable);
    }

    /**
     * Create a number map metric.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the number map metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Map<String, ? extends Number>> numberMap(@SourceId final String id, final Callable<? extends @Nullable Map<String, ? extends Number>> callable) throws IllegalArgumentException {
        return new SimpleMetric.Map<>(id, callable);
    }

    /**
     * Create a metric for a boolean value.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the boolean metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Boolean> bool(@SourceId final String id, final Callable<@Nullable Boolean> callable) throws IllegalArgumentException {
        return new SimpleMetric.Primitive<>(id, callable);
    }

    /**
     * Create a metric for a string value.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the string metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<String> string(@SourceId final String id, final Callable<@Nullable String> callable) throws IllegalArgumentException {
        return new SimpleMetric.Primitive<>(id, callable);
    }

    /**
     * Create a metric for a number value.
     *
     * @param id       the source id
     * @param callable the metric data callable
     * @return the number metric
     * @throws IllegalArgumentException if the source id is invalid
     * @apiNote The callable must be thread-safe and pure (i.e. not modify any shared state).
     * @see #getData()
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Metric<Number> number(@SourceId final String id, final Callable<@Nullable Number> callable) throws IllegalArgumentException {
        return new SimpleMetric.Primitive<>(id, callable);
    }
}
