package dev.faststats.example;

import dev.faststats.Attributes;
import dev.faststats.FastStatsContext;
import dev.faststats.FeatureFlag;
import dev.faststats.FeatureFlagService;
import dev.faststats.SimpleContext;

import java.time.Duration;

public final class FeatureFlagExample {
    public static final FastStatsContext CONTEXT = getContextFactory()
            // .featureFlagService(FeatureFlagService.Factory::create) // Define a feature flag service with default settings
            .featureFlagService(factory -> factory
                    .attributes(Attributes.empty() // Define global attributes
                            .put("version", "1.2.3")
                            .put("java_version", System.getProperty("java.version"))
                            .put("java_vendor", System.getProperty("java.vendor")))
                    .ttl(Duration.ofMinutes(10)) // Custom cache TTL for resolved flag values
                    .create())
            .create();

    public static final FeatureFlagService SERVICE = CONTEXT.featureFlagService().orElseThrow();

    // Define flags with default values
    public static final FeatureFlag<Boolean> NEW_COMMANDS = SERVICE.define("new_commands", false);
    public static final FeatureFlag<String> COMPRESSION = SERVICE.define("compression", "zstd");

    public static void usage() {
        // Async: waits for the server value to be fetched
        NEW_COMMANDS.whenReady().thenAccept(enabled -> {
            if (enabled) {
                // register new commands
            }
        });

        // Non-blocking: returns the cached value if present without triggering a fetch
        COMPRESSION.getCached().ifPresent(compression -> {
            switch (compression) {
                case "zstd":
                    // default compression
                    break;
                case "lz4":
                    // experimental compression
                    break;
                default:
                    break;
            }
        });

        // Refresh stale values explicitly when your code decides it is needed
        if (COMPRESSION.isExpired()) {
            COMPRESSION.fetch().thenAccept(string -> {
                // do stuff with the value
            });
        }

        // Opt-in/out (requires allow_specific_opt_in on server)
        NEW_COMMANDS.optIn().thenAccept(updatedValue -> {
            if (updatedValue) {
                // react to the updated server value
            }
        });
        NEW_COMMANDS.optOut().thenAccept(updatedValue -> {
            if (!updatedValue) {
                // react to the updated server value
            }
        });
    }

    private static SimpleContext.Factory<?, ?> getContextFactory() {
        return null;
    }
}
