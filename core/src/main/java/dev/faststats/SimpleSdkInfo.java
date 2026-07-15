package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.ServiceLoader;

final class SimpleSdkInfo implements SdkInfo {
    private static final UserAgentProvider userAgentProvider = loadUserAgentProvider();

    private final @Nullable String buildId;
    private final String name;
    private final String userAgent;
    private final String version;

    SimpleSdkInfo(final String name, final String version, @Nullable final String buildId) throws IllegalArgumentException {
        if (name.trim().isEmpty()) throw new IllegalArgumentException("name must not be blank");
        if (version.trim().isEmpty()) throw new IllegalArgumentException("version must not be blank");
        this.name = name;
        this.version = version;
        this.buildId = buildId;
        this.userAgent = userAgentProvider.getUserAgent(this);
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
        return userAgent;
    }

    static final class SimpleUserAgentProvider implements UserAgentProvider {
        @Override
        public String getUserAgent(final SdkInfo sdkInfo) {
            return "FastStats Metrics " + sdkInfo.getName() + "/" + sdkInfo.getVersion();
        }
    }

    private static UserAgentProvider loadUserAgentProvider() {
        for (final UserAgentProvider provider : ServiceLoader.load(UserAgentProvider.class)) {
            return provider;
        }
        return new SimpleUserAgentProvider();
    }
}
