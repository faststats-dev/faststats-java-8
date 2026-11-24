package dev.faststats.config;

import dev.faststats.Config;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public final class SimpleConfig implements Config {
    private static final int CONFIG_VERSION = 3;
    private static final String CONSENT_SCREEN_SHOWN = "consent-screen-shown";

    private static final String COMMENT = " FastStats (https://faststats.dev) collects pseudonymous usage statistics and errors.\n"
            + "# This helps developers understand how their projects are used in the real world.\n"
            + "#\n"
            + "# No IP addresses, player data, or personal information is collected.\n"
            + "# The server ID below is randomly generated and can be regenerated at any time.\n"
            + "#\n"
            + "# Enabling metrics has no noticeable performance impact.\n"
            + "# Keeping FastStats enabled is recommended.\n"
            + "# To disable all FastStats features, set 'enabled=false'.\n"
            + "# To disable only metrics submission, set 'submitMetrics=false'.\n"
            + "# To disable only additional metrics, set 'submitAdditionalMetrics=false'.\n"
            + "# To disable only error tracking, set 'submitErrors=false'.\n"
            + "#\n"
            + "# If you suspect a developer is collecting personal data or bypassing any opt-out option,\n"
            + "# please report it at: https://faststats.dev/abuse\n"
            + "#\n"
            + "# For more information, visit: https://faststats.dev/info\n";
    private static final String ONBOARDING_MESSAGE = "This plugin uses FastStats to collect pseudonymous usage statistics and errors.\n"
            + "No personal or identifying information is ever collected.\n"
            + "To opt out, set 'enabled=false' in the metrics configuration file.\n"
            + "Learn more at: https://faststats.dev/info\n"
            + "\n"
            + "Since this is your first start with FastStats, submission will not start\n"
            + "until after a restart, to allow you to opt out if you prefer.";

    private final Path file;
    private final UUID serverId;
    private final boolean debug;
    private final boolean firstRun;
    private final boolean client;
    private final boolean additionalMetrics;
    private final boolean enabled;
    private final boolean errorTracking;
    private final boolean submitMetrics;
    private final boolean consentScreenShown;

    public SimpleConfig(
            final Path file,
            final UUID serverId,
            final boolean enabled,
            final boolean additionalMetrics,
            final boolean debug,
            final boolean submitMetrics,
            final boolean errorTracking,
            final boolean firstRun,
            final boolean client,
            final boolean consentScreenShown
    ) {
        this.file = file;
        this.serverId = serverId;
        this.enabled = enabled;
        this.additionalMetrics = additionalMetrics;
        this.debug = debug;
        this.submitMetrics = submitMetrics;
        this.errorTracking = errorTracking;
        this.firstRun = firstRun;
        this.client = client;
        this.consentScreenShown = consentScreenShown;
    }

    @Override
    public UUID serverId() {
        return serverId;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean additionalMetrics() {
        return additionalMetrics;
    }

    @Override
    public boolean debug() {
        return debug;
    }

    @Override
    public boolean submitMetrics() {
        return submitMetrics;
    }

    @Override
    public boolean errorTracking() {
        return errorTracking;
    }

    public boolean firstRun() {
        return firstRun;
    }

    public boolean consentScreenShown() {
        return consentScreenShown;
    }

    /**
     * Creates a copy of this config that reflects the consent given on the consent screen.
     * <p>
     * FastStats is enabled if either metrics submission or error tracking was allowed,
     * and additional metrics require metrics submission.
     *
     * @param submitMetrics     whether metrics submission was allowed
     * @param additionalMetrics whether additional metrics were allowed
     * @param errorTracking     whether error tracking was allowed
     * @return the new config, marked as having shown the consent screen
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public SimpleConfig withConsent(final boolean submitMetrics, final boolean additionalMetrics, final boolean errorTracking) {
        return new SimpleConfig(
                file,
                serverId,
                submitMetrics || errorTracking,
                submitMetrics && additionalMetrics,
                debug,
                submitMetrics,
                errorTracking,
                firstRun,
                client,
                true
        );
    }

    /**
     * @return {@code false} if FastStats was disabled using {@code -Dfaststats.enabled=false}
     */
    public static boolean enabledFlag() {
        return Boolean.parseBoolean(System.getProperty("faststats.enabled", "true"));
    }

    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file, final LoggerFactory factory) throws RuntimeException {
        return read(file, factory, false);
    }

    /**
     * Reads the config from the given file.
     *
     * @param client whether the config belongs to a client, only clients store client specific options
     */
    @Contract(mutates = "io")
    public static SimpleConfig read(final Path file, final LoggerFactory factory, final boolean client) throws RuntimeException {
        final Logger logger = factory.getLogger(SimpleConfig.class);

        final boolean debugFlag = Boolean.getBoolean("faststats.debug");
        final boolean enabledFlag = enabledFlag();

        final Properties properties = readOrEmpty(file);
        final boolean firstRun = properties == null;
        final AtomicBoolean saveConfig = new AtomicBoolean(firstRun);

        final UUID serverId = parse(properties, saveConfig, "serverId", UUID::randomUUID, value -> {
            final String corrected = value.length() > 36 ? value.substring(0, 36) : value;
            final UUID uuid = UUID.fromString(corrected);
            if (!value.equals(uuid.toString())) saveConfig.set(true);
            return uuid;
        }, logger);
        final Integer configVersion = parse(properties, saveConfig, "configVersion", null, Integer::parseInt, logger);
        final boolean enabled = parse(properties, saveConfig, "enabled", () -> true, Boolean::parseBoolean, logger);
        final boolean submitMetrics = parse(properties, saveConfig, "submitMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean errorTracking = parse(properties, saveConfig, "submitErrors", () -> true, Boolean::parseBoolean, logger);
        final boolean additionalMetrics = parse(properties, saveConfig, "submitAdditionalMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean debug = parse(properties, saveConfig, "debug", () -> false, Boolean::parseBoolean, logger);
        final boolean consentScreenShown = client && parse(properties, saveConfig, CONSENT_SCREEN_SHOWN, () -> false, Boolean::parseBoolean, logger);

        if (configVersion == null || configVersion < CONFIG_VERSION) saveConfig.set(true);
        else if (configVersion > CONFIG_VERSION) saveConfig.set(false);

        if (saveConfig.get()) try {
            if (configVersion != null && configVersion < CONFIG_VERSION)
                logger.info("Updating config version from %s to %s", configVersion, CONFIG_VERSION);
            Files.createDirectories(file.getParent());
            try (final java.io.BufferedWriter writer = Files.newBufferedWriter(file, UTF_8)) {
                final Properties store = new Properties();

                store.setProperty("enabled", Boolean.toString(enabled));
                store.setProperty("submitMetrics", Boolean.toString(submitMetrics));
                store.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics));
                store.setProperty("submitErrors", Boolean.toString(errorTracking));

                store.setProperty("serverId", serverId.toString());

                store.setProperty("debug", Boolean.toString(debug));
                if (client) store.setProperty(CONSENT_SCREEN_SHOWN, Boolean.toString(consentScreenShown));
                store.setProperty("configVersion", Integer.toString(CONFIG_VERSION));

                store.store(writer, COMMENT);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }

        return new SimpleConfig(
                file,
                serverId,
                enabled && enabledFlag,
                enabled && enabledFlag && additionalMetrics,
                debug || debugFlag,
                enabled && enabledFlag && submitMetrics,
                enabled && enabledFlag && errorTracking,
                firstRun,
                client,
                consentScreenShown
        );
    }

    @Contract(mutates = "io")
    public synchronized void persist() throws RuntimeException {
        Properties properties = readOrEmpty(file);
        if (properties == null) {
            // the file was deleted after it was read, recreate it
            properties = new Properties();
            properties.setProperty("serverId", serverId.toString());
            properties.setProperty("debug", Boolean.toString(debug));
            properties.setProperty("configVersion", Integer.toString(CONFIG_VERSION));
        }

        properties.setProperty("enabled", Boolean.toString(enabled()));
        properties.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics()));
        properties.setProperty("submitErrors", Boolean.toString(errorTracking()));
        properties.setProperty("submitMetrics", Boolean.toString(submitMetrics()));
        if (client) properties.setProperty(CONSENT_SCREEN_SHOWN, Boolean.toString(consentScreenShown()));
        else properties.remove(CONSENT_SCREEN_SHOWN);

        try {
            Files.createDirectories(file.getParent());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create metrics config directory", e);
        }
        try (final java.io.BufferedWriter writer = Files.newBufferedWriter(file, UTF_8)) {
            properties.store(writer, COMMENT);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }
    }

    // fixme: this code sucks ass
    @Contract(value = "_, _, _, !null, _, _-> !null")
    private static <T> @Nullable T parse(
            @Nullable final Properties properties,
            final AtomicBoolean saveConfig,
            final String key,
            @Nullable final Supplier<T> defaultValue,
            final Function<String, T> parser,
            final Logger logger
    ) {
        if (properties == null) {
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
        final String property = properties.getProperty(key);
        if (property == null) {
            logger.warn("Missing configuration property: %s", key);
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
        try {
            return parser.apply(property.trim());
        } catch (final Exception e) {
            logger.error("Failed to read property '%s' from config", e, key);
            saveConfig.set(true);
            return defaultValue != null ? defaultValue.get() : null;
        }
    }

    private static @Nullable Properties readOrEmpty(final Path file) throws RuntimeException {
        if (!Files.isRegularFile(file)) return null;
        try (final java.io.BufferedReader reader = Files.newBufferedReader(file, UTF_8)) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read metrics config", e);
        }
    }

    public boolean preSubmissionStart(final LoggerFactory loggerFactory) {
        if (Boolean.getBoolean("faststats.first-run")) return false;

        if (firstRun()) {
            int separatorLength = 0;
            final String[] split = ONBOARDING_MESSAGE.split("\n");
            for (final String s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final Logger logger = loggerFactory.getLogger(getClass());
            logger.print(Logger.LogLevel.INFO, null, repeat('-', separatorLength));
            for (final String s : split) logger.print(Logger.LogLevel.INFO, null, s);
            logger.print(Logger.LogLevel.INFO, null, repeat('-', separatorLength));

            System.setProperty("faststats.first-run", "true");
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SimpleConfig{" +
                "serverId=" + serverId +
                ", debug=" + debug +
                ", firstRun=" + firstRun +
                ", client=" + client +
                ", consentScreenShown=" + consentScreenShown +
                ", additionalMetrics=" + additionalMetrics +
                ", enabled=" + enabled +
                ", errorTracking=" + errorTracking +
                ", submitMetrics=" + submitMetrics +
                '}';
    }

    private static String repeat(final char c, final int count) {
        final StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
