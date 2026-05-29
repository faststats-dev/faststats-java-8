package dev.faststats;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// fixme: thread safety
// todo: cleanup
// todo: introduce a factory pattern to shorten the constructor?
@ApiStatus.Internal
public non-sealed abstract class SimpleContext implements FastStatsContext {
    private final ErrorTrackingSink errorTrackingSink = new ErrorTrackingSink(this);

    private final Config config;
    private final @Token String token;
    private final SdkInfo sdkInfo;

    /**
     * Creates a new context that stores the shared configuration and token for all FastStats services.
     *
     * @param config the shared configuration
     * @param name   the name of the SDK
     * @param token  the FastStats project token
     * @throws IllegalArgumentException if the token is invalid
     * @throws IllegalArgumentException if the SDK information is invalid
     * @throws IllegalStateException    if the SDK information is incomplete or missing
     * @throws UncheckedIOException     if an IO error occurs
     * @since 0.24.0
     */
    protected SimpleContext(final Config config, final String name, @Token final String token) throws IllegalArgumentException {
        this.sdkInfo = constructSdkInfo(name);
        if (!token.matches(Token.PATTERN)) {
            throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
        }
        this.config = config;
        this.token = token;
    }

    private SdkInfo constructSdkInfo(final String name) throws UncheckedIOException, IllegalStateException, IllegalArgumentException {
        try (final var stream = getClass().getResourceAsStream("/META-INF/faststats.properties")) {
            if (stream == null) throw new IllegalStateException("Resource '/META-INF/faststats.properties' not found");

            final var properties = new Properties();
            properties.load(stream);

            final var version = properties.getProperty("version", null);
            if (version == null) throw new IllegalStateException("Missing 'version' in faststats.properties");

            final var buildId = properties.getProperty("build-id", null);

            return new SimpleSdkInfo(name, version, buildId);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read faststats.properties from META-INF", e);
        }
    }

    @Contract(pure = true)
    public abstract String getProjectName();

    @Override
    @Contract(pure = true)
    public final Config getConfig() {
        return config;
    }

    @Override
    @Contract(pure = true)
    public final @Token String getToken() {
        return token;
    }

    @Override
    @Contract(value = " -> new", pure = true)
    public final FeatureFlagService.Factory featureFlagServiceFactory() {
        return new SimpleFeatureFlagService.Factory(config, token);
    }

    @Override
    @Contract(pure = true)
    public final Optional<ErrorTracker> errorTracker() {
        return Optional.ofNullable(internalErrorTracker);
    }

    @Override
    public final SimpleContext registerErrorTracker(final ErrorTracker tracker) {
        errorTrackingSink.errorTrackers.add((SimpleErrorTracker) tracker);
        errorTrackingSink.startErrorSubmission();
        return this;
    }

    @Override
    @Contract(pure = true)
    public SdkInfo getSdkInfo() {
        return sdkInfo;
    }

    ErrorTrackingSink errorTrackingSink() {
        return errorTrackingSink;
    }

    void trackInternalError(final Throwable error, final boolean handled) {
        if (internalErrorTracker == null) return;
        internalErrorTracker.trackError(error, handled);
    }
}
