package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

final class SimpleSdkInfo implements SdkInfo {
    private final @Nullable String buildId;
    private final String name;
    private final String version;

    SimpleSdkInfo(final String name, final String version, @Nullable final String buildId) throws IllegalArgumentException {
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (version.isBlank()) throw new IllegalArgumentException("version must not be blank");
        this.name = name;
        this.version = version;
        this.buildId = buildId;
    }

    @Override
    public Optional<String> getBuildId() {
        return Optional.ofNullable(buildId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getUserAgent() {
        return "FastStats Metrics " + name + "/" + version;
    }
}
