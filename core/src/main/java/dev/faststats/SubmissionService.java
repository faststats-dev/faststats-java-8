package dev.faststats;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.faststats.internal.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class SubmissionService {
    protected static final Duration TIMEOUT = Duration.ofSeconds(3);
    protected static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    protected final Logger logger;
    protected final SimpleContext context;

    SubmissionService(final SimpleContext context) {
        this.context = context;
        this.logger = context.getLoggerFactory().getLogger(getClass());
    }

    protected abstract String serverType();

    protected URI getServerUrl(final String propertyName, final String defaultUrl) {
        final var property = System.getProperty(propertyName);
        if (property != null) try {
            return new URI(property);
        } catch (final URISyntaxException e) {
            logger.error("Failed to parse server url from %s: %s", e, propertyName, property);
        }
        return URI.create(defaultUrl);
    }

    protected boolean submit(
            final URI url,
            final JsonElement data,
            final String submissionName
    ) {
        try {
            final var compressed = compress(data.toString());
            logger.info("Sending %s to: %s (%s bytes)\n%s", submissionName, url, compressed.length, data);

            final var response = HTTP_CLIENT.send(
                    createSubmissionRequest(url, compressed),
                    HttpResponse.BodyHandlers.ofString(UTF_8)
            );

            if (isSuccessful(response)) {
                final var warnings = hasWarnings(response.body());
                final var level = warnings ? Logger.LogLevel.WARN : Logger.LogLevel.INFO;
                logger.debug(level, "%s submitted successfully with status code: %s (%s)", null,
                        capitalize(submissionName), response.statusCode(), response.body());
                return true;
            }
            logUnsuccessfulResponse(response);
        } catch (final HttpConnectTimeoutException t) {
            logger.error("%s submission timed out after 3 seconds: %s", null, capitalize(serverType()), url);
        } catch (final ConnectException t) {
            logger.error("Failed to connect to %s server: %s", null, serverType(), url);
        } catch (final Throwable t) {
            logger.error("Failed to submit %s", t, submissionName);
        }
        return false;
    }

    private boolean hasWarnings(final String body) {
        try {
            final var json = JsonParser.parseString(body);
            return json.isJsonObject() && json.getAsJsonObject().has("warnings");
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private static String capitalize(final String value) {
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    protected static boolean isSuccessful(final HttpResponse<?> response) {
        final var statusCode = response.statusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    protected void logUnsuccessfulResponse(final HttpResponse<?> response) {
        final var statusCode = response.statusCode();
        final var body = response.body();

        if (statusCode >= 300 && statusCode < 400) {
            logger.warn("Received redirect response from %s server: %s (%s)", serverType(), statusCode, body);
        } else if (statusCode >= 400 && statusCode < 500) {
            logger.error("Submitted invalid request to %s server: %s (%s)", null, serverType(), statusCode, body);
        } else if (statusCode >= 500 && statusCode < 600) {
            logger.error("Received server error response from %s server: %s (%s)", null, serverType(), statusCode, body);
        } else {
            logger.warn("Received unexpected response from %s server: %s (%s)", serverType(), statusCode, body);
        }
    }

    private static byte[] compress(final String data) throws IOException {
        try (final var byteOutput = new ByteArrayOutputStream();
             final var output = new GZIPOutputStream(byteOutput)) {
            output.write(data.getBytes(UTF_8));
            output.finish();
            return byteOutput.toByteArray();
        }
    }

    private HttpRequest createSubmissionRequest(final URI url, final byte[] compressed) {
        return HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                .header("Content-Encoding", "gzip")
                .header("Content-Type", "application/octet-stream")
                .header("Authorization", "Bearer " + context.getToken())
                .header("User-Agent", context.getSdkInfo().getUserAgent())
                .timeout(TIMEOUT)
                .uri(url)
                .build();
    }
}
