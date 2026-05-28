package dev.faststats;

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
     * <p>
     * The user agent should include enough information to identify the client
     * implementation, including the vendor name, SDK name, and SDK version.
     * It may also include contact information, such as an email address,
     * repository URL, Discord server, or website, so FastStats can reach the
     * implementation owner in case of abuse or operational problems.
     *
     * @return the HTTP user agent
     * @since 0.24.0
     */
    String getUserAgent();
}
