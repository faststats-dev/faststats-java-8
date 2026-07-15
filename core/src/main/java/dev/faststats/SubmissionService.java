package dev.faststats;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.faststats.internal.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class SubmissionService {
    protected static final Duration TIMEOUT = Duration.ofSeconds(3);

    protected final Logger logger;
    protected final SimpleContext context;

    SubmissionService(final SimpleContext context) {
        this.context = context;
        this.logger = context.getLoggerFactory().getLogger(getClass());
    }

    protected abstract String serverType();

    protected URI getServerUrl(final String propertyName, final String defaultUrl) {
        final String property = System.getProperty(propertyName);
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
            final byte[] compressed = compress(data.toString());
            logger.info("Sending %s to: %s (%s bytes)\n%s", submissionName, url, compressed.length, data);

            final Response response = send(url, compressed, "application/octet-stream");

            if (isSuccessful(response)) {
                final boolean warnings = hasWarnings(response.body());
                final Logger.LogLevel level = warnings ? Logger.LogLevel.WARN : Logger.LogLevel.INFO;
                logger.debug(level, "%s submitted successfully with status code: %s (%s)", null,
                        capitalize(submissionName), response.statusCode(), response.body());
                return true;
            }
            logUnsuccessfulResponse(response);
        } catch (final SocketTimeoutException t) {
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
            final JsonElement json = JsonParser.parseString(body);
            return json.isJsonObject() && json.getAsJsonObject().has("warnings");
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private static String capitalize(final String value) {
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    protected static boolean isSuccessful(final Response response) {
        final int statusCode = response.statusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    protected void logUnsuccessfulResponse(final Response response) {
        final int statusCode = response.statusCode();
        final String body = response.body();

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
        try (final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             final GZIPOutputStream output = new GZIPOutputStream(byteOutput)) {
            output.write(data.getBytes(UTF_8));
            output.finish();
            return byteOutput.toByteArray();
        }
    }

    protected Response sendJson(final URI url, final String body) throws IOException {
        return send(url, body.getBytes(UTF_8), "application/json");
    }

    private Response send(final URI url, final byte[] body, final String contentType) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setConnectTimeout((int) TIMEOUT.toMillis());
        connection.setReadTimeout((int) TIMEOUT.toMillis());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Authorization", "Bearer " + context.getToken());
        connection.setRequestProperty("User-Agent", context.getSdkInfo().getUserAgent());
        if ("application/octet-stream".equals(contentType)) {
            connection.setRequestProperty("Content-Encoding", "gzip");
        }
        try (final OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }

        final int statusCode = connection.getResponseCode();
        final InputStream input = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        final String responseBody = input == null ? "" : readFully(input);
        connection.disconnect();
        return new Response(statusCode, responseBody);
    }

    private static String readFully(final InputStream input) throws IOException {
        try (final InputStream in = input;
             final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), UTF_8);
        }
    }

    protected static final class Response {
        private final int statusCode;
        private final String body;

        Response(final int statusCode, final String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        int statusCode() {
            return statusCode;
        }

        String body() {
            return body;
        }
    }
}
