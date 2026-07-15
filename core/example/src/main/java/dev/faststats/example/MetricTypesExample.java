package dev.faststats.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.faststats.data.Metric;

public final class MetricTypesExample {
    // Single value metrics
    public static final Metric<Number> PLAYER_COUNT = Metric.number("player_count", () -> 42);
    public static final Metric<String> SERVER_VERSION = Metric.string("server_version", () -> "1.0.0");
    public static final Metric<Boolean> MAINTENANCE_MODE = Metric.bool("maintenance_mode", () -> false);

    // Array metrics
    public static final Metric<String[]> INSTALLED_PLUGINS = Metric.stringArray("installed_plugins", () -> new String[]{"WorldEdit", "Essentials"});
    public static final Metric<String[]> WORLDS = Metric.stringArray("worlds", () -> new String[]{"city", "farmworld", "farmworld_nether", "famrworld_end"});

    // Json metrics
    public static final Metric<JsonObject> JSON_OBJECT = Metric.object("nested_example", () -> {
        var object = new JsonObject();
        object.addProperty("scope", "example");
        return object;
    });
    public static final Metric<JsonArray> JSON_ARRAY = Metric.array("examples", () -> {
        var array = new JsonArray();
        array.add("example");
        return array;
    });
    public static final Metric<JsonPrimitive> JSON_NUMBER = Metric.primitive("example", () -> new JsonPrimitive("example"));
}
