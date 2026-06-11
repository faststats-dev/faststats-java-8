package dev.faststats;

import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * Information that identifies the SDK implementation using FastStats.
 *
 * @since 0.24.0
 */
public sealed interface SdkInfo permits SimpleSdkInfo {
    /**
     * Get the build identifier of the project that implements this SDK.
     * <p>
     * This identifier is used to associate uploaded errors with the correct
     * obfuscation mappings, such as ProGuard or R8 mapping files.
     * It does not identify the FastStats SDK build itself.
     *
     * @return the implementing project's build identifier, if available
     * @since 0.24.0
     */
    Optional<String> getBuildId();

    /**
     * Get the SDK implementation name.
     *
     * @return the SDK name
     * @since 0.24.0
     */
    String getName();

    /**
     * Get the SDK implementation version.
     *
     * @return the SDK version
     * @since 0.24.0
     */
    String getVersion();

    /**
     * Get the user agent sent with FastStats HTTP requests.
     *
     * @return the HTTP user agent
     * @since 0.24.0
     */
    String getUserAgent();

    /**
     * Provides the HTTP user agent for the default {@link SdkInfo}
     * implementation.
     * <p>
     * This service-provider hook is intended for SDKs that depend on
     * FastStats core and need to identify their own distribution in outgoing
     * requests. Implementations are discovered with {@link java.util.ServiceLoader};
     * custom SDKs should provide one to override the core default user agent.
     * <p>
     * The user agent should include enough information to identify the client
     * implementation, including the vendor name, SDK name, and SDK version.
     * It may also include contact information, such as an email address,
     * repository URL, Discord server, or website, so FastStats can reach the
     * implementation owner in case of abuse or operational problems.
     *
     * @since 0.26.0
     */
    interface UserAgentProvider {
        /**
         * Get the user agent for the supplied SDK information.
         *
         * @param sdkInfo the SDK information to build the user agent from
         * @return the HTTP user agent
         * @implNote Calling {@link SdkInfo#getUserAgent()} on the supplied SDK information has undefined behavior.
         * @since 0.26.0
         */
        @Contract(pure = true)
        String getUserAgent(SdkInfo sdkInfo);
    }
}
