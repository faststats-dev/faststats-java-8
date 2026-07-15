package dev.faststats.sponge;

import dev.faststats.Config;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApiStatus.Internal
public final class SpongeConfig implements Config {
    private static final int CONFIG_VERSION = 2;

    private static final String COMMENT =
            " FastStats (https://faststats.dev) collects anonymous usage statistics and errors.\n" +
                    "# This helps developers understand how their projects are used in the real world.\n" +
                    "#\n" +
                    "# No IP addresses, player data, or personal information is collected.\n" +
                    "# The server ID below is randomly generated and can be regenerated at any time.\n" +
                    "#\n" +
                    "# Enabling metrics has no noticeable performance impact.\n" +
                    "# Enabling metrics is highly recommended, you can do so in the Sponge metrics.config,\n" +
                    "# by setting the \"global-state\" property to \"TRUE\".\n" +
                    "# To disable only metrics submission, set 'submitMetrics=false'.\n" +
                    "# To disable additional metrics, set 'submitAdditionalMetrics=false'.\n" +
                    "# To disable error tracking, set 'submitErrors=false'.\n" +
                    "#\n" +
                    "# If you suspect a developer is collecting personal data or bypassing the Sponge config,\n" +
                    "# please report it at: https://faststats.dev/abuse\n" +
                    "#\n" +
                    "# For more information, visit: https://faststats.dev/info\n";
    private static final String ONBOARDING_MESSAGE =
            "This plugin uses FastStats to collect anonymous usage statistics and errors.\n" +
                    "No personal or identifying information is ever collected.\n" +
                    "It is recommended to enable metrics by setting 'global-state=TRUE' in the sponge metrics config.\n" +
                    "Learn more at: https://faststats.dev/info\n" +
                    "\n" +
                    "Since this is your first start with FastStats, submission will not start\n" +
                    "until you restart the server to allow you to opt out if you prefer.\n";

    private final UUID serverId;
    private final boolean enabled;
    private final boolean additionalMetrics;
    private final boolean debug;
    private final boolean submitMetrics;
    private final boolean errorTracking;
    private final boolean firstRun;

    public SpongeConfig(final UUID serverId, final boolean enabled, final boolean additionalMetrics,
                        final boolean debug, final boolean submitMetrics, final boolean errorTracking,
                        final boolean firstRun) {
        this.serverId = serverId;
        this.enabled = enabled;
        this.additionalMetrics = additionalMetrics;
        this.debug = debug;
        this.submitMetrics = submitMetrics;
        this.errorTracking = errorTracking;
        this.firstRun = firstRun;
    }

    @Contract(mutates = "io")
    public static SpongeConfig read(final PluginContainer plugin, final Path file, final LoggerFactory loggerFactory) throws RuntimeException {
        final Logger logger = loggerFactory.getLogger(SpongeConfig.class);
        final boolean debugFlag = Boolean.getBoolean("faststats.debug");

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
        final boolean submitMetrics = parse(properties, saveConfig, "submitMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean errorTracking = parse(properties, saveConfig, "submitErrors", () -> true, Boolean::parseBoolean, logger);
        final boolean additionalMetrics = parse(properties, saveConfig, "submitAdditionalMetrics", () -> true, Boolean::parseBoolean, logger);
        final boolean debug = parse(properties, saveConfig, "debug", () -> true, Boolean::parseBoolean, logger);

        if (configVersion == null || configVersion < CONFIG_VERSION) saveConfig.set(true);
        else if (configVersion > CONFIG_VERSION) saveConfig.set(false);

        if (saveConfig.get()) try {
            if (configVersion != null && configVersion < CONFIG_VERSION)
                logger.info("Updating config version from %s to %s", configVersion, CONFIG_VERSION);
            Files.createDirectories(file.getParent());
            try (final OutputStream out = Files.newOutputStream(file);
                 final OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8)) {
                final Properties store = new Properties();

                store.setProperty("submitMetrics", Boolean.toString(submitMetrics));
                store.setProperty("submitAdditionalMetrics", Boolean.toString(additionalMetrics));
                store.setProperty("submitErrors", Boolean.toString(errorTracking));

                store.setProperty("serverId", serverId.toString());

                store.setProperty("debug", Boolean.toString(debug));
                store.setProperty("configVersion", Integer.toString(CONFIG_VERSION));

                store.store(writer, COMMENT);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save metrics config", e);
        }

        final boolean enabled = Sponge.metricsConfigManager().effectiveCollectionState(plugin).asBoolean();
        return new SpongeConfig(
                serverId,
                enabled,
                enabled && additionalMetrics,
                debug || debugFlag,
                enabled && submitMetrics,
                enabled && errorTracking,
                firstRun
        );
    }

    @Contract(value = "_, _, _, !null, _, _ -> !null")
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
        try (final Reader reader = Files.newBufferedReader(file, UTF_8)) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read metrics config", e);
        }
    }

    public boolean preSubmissionStart(final SpongeContext context) {
        if (Boolean.getBoolean("faststats.first-run")) return false;

        if (firstRun()) {
            int separatorLength = 0;
            final String[] split = ONBOARDING_MESSAGE.split("\n");
            for (final String s : split) if (s.length() > separatorLength) separatorLength = s.length();

            final Logger logger = context.getLoggerFactory().getLogger(getClass());
            logger.print(Logger.LogLevel.INFO, null, repeat('-', separatorLength));
            for (final String s : split) logger.print(Logger.LogLevel.INFO, null, s);
            logger.print(Logger.LogLevel.INFO, null, repeat('-', separatorLength));

            System.setProperty("faststats.first-run", "true");
            return false;
        }
        return true;
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

    private static String repeat(final char c, final int count) {
        final StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
