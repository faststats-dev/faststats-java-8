package dev.faststats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleFeatureFlagService extends SubmissionService implements FeatureFlagService {
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final String CHECK_PATH = "/v1/check";
    private static final String OPT_IN_PATH = "/v1/opt-in";
    private static final String OPT_OUT_PATH = "/v1/opt-out";

    private final URI url = getServerUrl("faststats.flags-server", "https://flags.faststats.dev");

    private final SimpleContext context;

    private final Attributes attributes;
    private final Duration ttl;

    private final Map<String, CompletableFuture<?>> fetchesInProgress = new ConcurrentHashMap<>();

    SimpleFeatureFlagService(
            final SimpleContext context,
            final Attributes attributes,
            final Duration ttl
    ) throws IllegalArgumentException {
        super(context);
        if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
        this.attributes = attributes;
        this.context = context;
        this.ttl = ttl;
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> fetch(final SimpleFeatureFlag<T> flag) {
        return (CompletableFuture<T>) fetchesInProgress.computeIfAbsent(flag.getId(), ignored -> createFetch(flag));
    }

    <T> CompletableFuture<T> optIn(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, OPT_IN_PATH);
    }

    <T> CompletableFuture<T> optOut(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, OPT_OUT_PATH);
    }

    private <T> CompletableFuture<T> sendOptRequest(final SimpleFeatureFlag<T> flag, final String path) {
        final JsonObject requestBody = createRequestBody(flag);
        return send(path, requestBody).thenCompose(response -> {
            if (isSuccessful(response)) return fetch(flag);
            logUnsuccessfulResponse(response);
            return failedFuture(new IllegalStateException(
                    String.format("Feature flag opt request failed with status %s (%s)", response.statusCode(), response.body())
            ));
        });
    }

    private <T> CompletableFuture<T> createFetch(final SimpleFeatureFlag<T> flag) {
        final JsonObject requestBody = createRequestBody(flag);
        logger.info("Fetching %s: %s", url.resolve(CHECK_PATH), requestBody.toString());
        return send(CHECK_PATH, requestBody)
                .thenApply(response -> handleFetchResponse(flag, response))
                .whenComplete((ignored, throwable) -> fetchesInProgress.remove(flag.getId()));
    }

    private JsonObject createRequestBody(final SimpleFeatureFlag<?> flag) {
        final JsonObject requestBody = new JsonObject();
        final JsonObject attributes = new JsonObject();

        requestBody.addProperty("identifier", context.getConfig().serverId().toString());
        requestBody.addProperty("key", flag.getId());

        this.attributes.forEachPrimitive(attributes::add);
        if (flag.attributes() != null) flag.attributes().forEachPrimitive(attributes::add);
        if (!attributes.isEmpty()) requestBody.add("attributes", attributes);

        return requestBody;
    }

    private CompletableFuture<Response> send(final String path, final JsonObject requestBody) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendJson(url.resolve(path), requestBody.toString());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> T handleFetchResponse(final SimpleFeatureFlag<T> flag, final Response response) {
        try {
            if (!isSuccessful(response)) {
                logUnsuccessfulResponse(response);
                throw new IllegalStateException(String.format("Unexpected response status: %s (%s)", response.statusCode(), response.body()));
            }

            final JsonElement body = JsonParser.parseString(response.body());
            final T value = getValue(flag, body);
            logger.info("Fetch returned body: %s (value: %s)", body, value);
            flag.setLastFetch(System.currentTimeMillis());
            flag.setValue(value);
            return value;
        } catch (final JsonParseException e) {
            logger.error("Unexpected response body: %s (%s)", e, response.body(), response.statusCode());
            throw new IllegalStateException(String.format("Unexpected response body: %s (%s)", response.body(), response.statusCode()), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(final SimpleFeatureFlag<T> flag, final JsonElement body) {
        if (!(body instanceof JsonObject)) {
            logger.warn("Unexpected JSON response: %s", body);
            throw new IllegalStateException("Unexpected JSON response: " + body);
        }
        final JsonObject object = (JsonObject) body;
        if (!(object.get("value") instanceof JsonPrimitive)) {
            logger.warn("Missing or invalid 'value' in JSON response: %s", body);
            throw new IllegalStateException("Missing or invalid 'value' in JSON response: " + body);
        }
        final JsonPrimitive primitive = (JsonPrimitive) object.get("value");

        switch (flag.getType()) {
            case STRING:
                return (T) primitive.getAsString();
            case NUMBER:
                return (T) getAsNumber(primitive);
            case BOOLEAN:
                return (T) Boolean.valueOf(getAsBoolean(primitive));
            default:
                throw new IllegalStateException("Unknown feature flag type: " + flag.getType());
        }
    }

    private Number getAsNumber(final JsonPrimitive primitive) {
        try {
            if (primitive.isNumber()) return primitive.getAsNumber();
            return new BigDecimal(primitive.getAsString());
        } catch (final NumberFormatException e) {
            logger.warn("Expected a number but got: %s", primitive.getAsString());
            throw new IllegalStateException("Expected a number but got: " + primitive.getAsString(), e);
        }
    }

    private boolean getAsBoolean(final JsonPrimitive primitive) {
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        final String value = primitive.getAsString();
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        logger.warn("Expected a boolean but got: %s", value);
        throw new IllegalStateException("Expected a boolean but got: " + value);
    }

    @Override
    public FeatureFlag<Boolean> define(final String id, final boolean defaultValue) {
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<Boolean> define(final String id, final boolean defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue) {
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue) {
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    private <T> FeatureFlag<T> defineFlag(final String id, final T defaultValue, @Nullable final Attributes attributes) {
        return new SimpleFeatureFlag<>(id, defaultValue, attributes, this);
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public Duration getTTL() {
        return ttl;
    }

    public void shutdown() {
        fetchesInProgress.values().forEach(fetch -> fetch.cancel(true));
        fetchesInProgress.clear();
    }

    @Override
    protected String serverType() {
        return "feature flag";
    }

    static final class Factory implements FeatureFlagService.Factory {
        private final SimpleContext context;
        private @Nullable Attributes attributes;
        private Duration ttl = DEFAULT_TTL;

        Factory(final SimpleContext context) {
            this.context = context;
        }

        @Override
        public FeatureFlagService.Factory attributes(final Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        @Override
        public FeatureFlagService.Factory ttl(final Duration ttl) throws IllegalArgumentException {
            if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
            this.ttl = ttl;
            return this;
        }

        @Override
        public FeatureFlagService create() throws IllegalArgumentException {
            final Attributes attributes = this.attributes != null ? this.attributes : Attributes.empty();
            return new SimpleFeatureFlagService(context, attributes, ttl);
        }
    }

    private static <T> CompletableFuture<T> failedFuture(final Throwable throwable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }
}
