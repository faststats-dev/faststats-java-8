package dev.faststats;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AnonymizationTest {
    private static final SimpleErrorTracker TRACKER = (SimpleErrorTracker) ErrorTracker.unaware();
    private final FastStatsContext context = new MockContext(TRACKER);
    private final MockMetrics metrics = (MockMetrics) context.metricsFactory().create();

    private JsonObject getError() {
        return TRACKER.getData().get(0).getAsJsonObject();
    }

    private String getErrorMessage() {
        return getError().get("message").getAsString();
    }

    @Test
    public void ipv4Anonymization() {
        TRACKER.trackError("Connection refused at 192.168.1.100");
        assertEquals("Connection refused at [IP hidden]", getErrorMessage());
    }

    @Test
    public void ipv6Anonymization() {
        TRACKER.trackError("Failed to connect to f833:be65:65da:975b:4896:88f7:6964:44c0");
        assertEquals("Failed to connect to [IP hidden]", getErrorMessage());
    }

    @Test
    public void userHomePathAnonymization() {
        final var username = System.getProperty("user.name", "user");
        TRACKER.trackError("File not found: /home/" + username + "/config.yml");
        assertEquals("File not found: /home/[username hidden]/config.yml", getErrorMessage());
    }

    @Test
    public void windowsUserPathAnonymization() {
        final var username = System.getProperty("user.name", "user");
        TRACKER.trackError("File not found: C:\\Users\\" + username + "\\config.yml");
        assertEquals("File not found: C:\\Users\\[username hidden]\\config.yml", getErrorMessage());
    }

    @Test
    public void macUserPathAnonymization() {
        final var username = System.getProperty("user.name", "user");
        TRACKER.trackError("File not found: /Users/" + username + "/config.yml");
        assertEquals("File not found: /Users/[username hidden]/config.yml", getErrorMessage());
    }

    @Test
    public void usernameAnonymizationIsCaseInsensitive() {
        final var username = System.getProperty("user.name", "user");
        TRACKER.trackError("Error for " + swapCase(username));
        assertEquals("Error for [username hidden]", getErrorMessage());
    }

    @Test
    public void discordWebhookAnonymization() {
        TRACKER.trackError("Webhook failed: https://discord.com/api/webhooks/1234567890987654321/aAaAaAaa0AAaAAaaaAAAAa_0AAAAAAAaaaAaaAaaAAAA0aA00AAA0AAA0aAAaA0a0a0A");
        assertEquals("Webhook failed: https://discord.com/api/webhooks/1234567890987654321/[token hidden]", getErrorMessage());
    }

    @Test
    public void jdbcUrlAnonymization() {
        TRACKER.trackError("Failed: jdbc:mysql://localhost:3306:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://localhost:3306:[password hidden]@mydb", getErrorMessage());
    }

    @Test
    public void jdbcUrlNoPortAnonymization() {
        TRACKER.trackError("Failed: jdbc:mysql://mydb.com:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://mydb.com:[password hidden]@mydb", getErrorMessage());
    }

    @Test
    public void jdbcUrlIpAnonymization() {
        TRACKER.trackError("Failed: jdbc:mysql://127.0.0.1:3306:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://[IP hidden]:3306:[password hidden]@mydb", getErrorMessage());
    }

    @Test
    public void customPatternAnonymizesMessage() {
        TRACKER.anonymize("token=[^&]+", "token=[redacted]");
        TRACKER.trackError("Request failed with token=abc123secret&user=test");
        assertEquals("Request failed with token=[redacted]&user=test", getErrorMessage());
    }

    @Test
    public void customPatternWithCompiledPattern() {
        TRACKER.anonymize(Pattern.compile("Bearer [A-Za-z0-9._~+/=-]+"), "Bearer [redacted]");
        TRACKER.trackError("Auth failed: Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature");
        assertEquals("Auth failed: Bearer [redacted]", getErrorMessage());
    }

    @Test
    public void customPatternWithCaptureGroupReplacement() {
        TRACKER.anonymize("(api_key=)[^&\\s]+", "$1[redacted]");
        TRACKER.trackError("GET /data?api_key=sk_live_12345&format=json failed");
        assertEquals("GET /data?api_key=[redacted]&format=json failed", getErrorMessage());
    }

    @Test
    public void multipleCustomPatterns() {
        TRACKER.anonymize("Bearer [A-Za-z0-9._~+/=-]+", "Bearer [redacted]")
                .anonymize("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[email hidden]");
        TRACKER.trackError("Auth failed for user@example.com with Bearer eyJ0eXAi");
        assertEquals("Auth failed for [email hidden] with Bearer [redacted]", getErrorMessage());
    }

    @Test
    public void customPatternChaining() {
        TRACKER.anonymize("secret-[a-z]+", "[secret hidden]")
                .anonymize("AKIA[0-9A-Z]{16}", "[aws-key hidden]");
        TRACKER.trackError("Credentials: secret-abcdef / AKIA1234567890ABCDEF");
        assertEquals("Credentials: [secret hidden] / [aws-key hidden]", getErrorMessage());
    }

    @Test
    public void customPatternAppliedToCauseChain() {
        final var tracker = TRACKER.anonymize("ssn=\\d{3}-\\d{2}-\\d{4}", "ssn=[redacted]");
        final var cause = new IllegalArgumentException("Validation failed for ssn=123-45-6789");
        tracker.trackError(new RuntimeException("Processing error", cause));
        final var error = getError();
        final var stack = error.getAsJsonArray("stack");
        var causeAnonymized = false;
        for (final var element : stack) {
            final var line = element.getAsString();
            assertFalse(line.contains("123-45-6789"));
            if (line.startsWith("Caused by:") && line.contains("ssn=[redacted]")) causeAnonymized = true;
        }
        assertTrue(causeAnonymized);
    }

    @Test
    public void nullMessageNotAffected() {
        TRACKER.anonymize("anything", "[redacted]");
        TRACKER.trackError(new RuntimeException((String) null));
        assertFalse(getError().has("message"));
    }

    @Test
    public void customAndBuiltInPatternsCombined() {
        TRACKER.anonymize("session=[a-f0-9]+", "session=[redacted]");
        final var username = System.getProperty("user.name", "user");
        TRACKER.trackError("Error for 192.168.1.1 with session=deadbeef01 at /home/" + username + "/app");
        assertEquals("Error for [IP hidden] with session=[redacted] at /home/[username hidden]/app", getErrorMessage());
    }

    @Test
    public void emptyReplacementRemovesMatch() {
        TRACKER.anonymize("\\(internal ref: [^)]+\\)", "");
        TRACKER.trackError("Request failed (internal ref: REF-98765)");
        assertEquals("Request failed ", getErrorMessage());
    }

    @Test
    public void patternDoesNotMatchLeavesMessageUnchanged() {
        TRACKER.anonymize("SECRET_[A-Z]+", "[redacted]");
        TRACKER.trackError("just a normal error");
        assertEquals("just a normal error", getErrorMessage());
    }

    private static String swapCase(final String value) {
        final var builder = new StringBuilder(value.length());
        for (var i = 0; i < value.length(); i++) {
            final var c = value.charAt(i);
            builder.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return builder.toString();
    }
}
