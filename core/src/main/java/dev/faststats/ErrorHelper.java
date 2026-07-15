package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ErrorHelper {
    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int MAX_FRAME_SIZE = 300;
    public static final int MAX_STACK_SIZE = 30;

    private static final Set<String> allowedNames = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("minecraft", "server", "root", "ubuntu")
    ));
    private static final List<Map.Entry<Pattern, String>> defaultAnonymizationEntries = defaultAnonymizationEntries();

    public static JsonObject compile(final TrackedError error, @Nullable final List<String> suppress,
                                     final List<Map.Entry<Pattern, String>> customPatterns,
                                     @Nullable final Attributes attributes) {
        final List<Map.Entry<Pattern, String>> patterns = new ArrayList<>(customPatterns);
        patterns.addAll(defaultAnonymizationEntries);
        return compileAll(error, suppress, patterns, attributes);
    }

    private static JsonObject compileAll(final TrackedError trackedError, @Nullable final List<String> suppress,
                                         final List<Map.Entry<Pattern, String>> customPatterns,
                                         @Nullable final Attributes defaultAttributes) {
        final Throwable error = trackedError.error();
        final JsonObject report = new JsonObject();
        final String message = getAnonymizedMessage(error, customPatterns);

        final JsonArray stacktrace = new JsonArray();
        final String header = message != null
                ? error.getClass().getName() + ": " + message
                : error.getClass().getName();
        stacktrace.add(header);

        final StackTraceElement[] elements = error.getStackTrace();
        final List<String> stack = collapseStackTrace(elements);
        final List<String> list = new ArrayList<>(stack);
        if (suppress != null) list.removeAll(suppress);
        final int traces = Math.min(list.size(), MAX_STACK_SIZE);

        populateTraces(traces, list, elements, stacktrace);
        appendCauseChain(error.getCause(), stack, suppress, stacktrace, customPatterns);

        report.addProperty("error", error.getClass().getName());
        if (message != null) report.addProperty("message", message);

        report.add("stack", stacktrace);
        report.addProperty("handled", trackedError.handled());

        final JsonObject attributes = new JsonObject();
        if (defaultAttributes != null) defaultAttributes.forEachPrimitive(attributes::add);
        trackedError.attributes().forEachPrimitive(attributes::add);
        if (!attributes.isEmpty()) report.add("context", attributes);

        return report;
    }

    // fixme: unmaintainable mess, i already forgot what it does
    private static void appendCauseChain(@Nullable Throwable cause, final List<String> parentStack,
                                         @Nullable final List<String> suppress, final JsonArray stacktrace,
                                         final List<Map.Entry<Pattern, String>> customPatterns) {
        final List<String> toSuppress = new ArrayList<>(parentStack);
        if (suppress != null) toSuppress.addAll(suppress);
        final Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (cause != null && visited.add(cause)) {
            final String causeMessage = getAnonymizedMessage(cause, customPatterns);
            final String header = causeMessage != null
                    ? "Caused by: " + cause.getClass().getName() + ": " + causeMessage
                    : "Caused by: " + cause.getClass().getName();
            stacktrace.add(header);

            final StackTraceElement[] causeElements = cause.getStackTrace();
            final List<String> causeStack = collapseStackTrace(causeElements);
            final List<String> causeList = new ArrayList<>(causeStack);
            causeList.removeAll(toSuppress);
            final int causeTraces = Math.min(causeList.size(), MAX_STACK_SIZE);
            populateTraces(causeTraces, causeList, causeElements, stacktrace);

            cause = cause.getCause();
        }
    }

    private static void populateTraces(final int traces, final List<String> list, final StackTraceElement[] elements,
                                       final JsonArray stacktrace) {
        for (int i = 0; i < traces; i++) {
            final String string = list.get(i);
            if (MAX_FRAME_SIZE < 0 || string.length() <= MAX_FRAME_SIZE) stacktrace.add("  at " + string);
            else stacktrace.add("  at " + string.substring(0, MAX_FRAME_SIZE) + "...");
        }
        if (traces > 0 && traces < list.size()) {
            stacktrace.add("  ... " + (list.size() - traces) + " more");
        } else {
            final int i = elements.length - list.size();
            if (i > 0) stacktrace.add("  ... " + i + " more");
        }
    }

    private static List<String> collapseStackTrace(final StackTraceElement[] trace) {
        final List<String> lines = Arrays.stream(trace)
                .map(StackTraceElement::toString)
                .collect(java.util.stream.Collectors.toList());

        return collapseRepeatingPattern(lines);
    }

    private static List<String> collapseRepeatingPattern(final List<String> lines) {
        final List<String> deduplicated = collapseConsecutiveDuplicates(lines);

        final int n = deduplicated.size();

        for (int cycleLen = 1; cycleLen <= n / 2; cycleLen++) {
            boolean isPattern = true;
            int repetitions = 0;

            for (int i = 0; i < n; i++) {
                if (!deduplicated.get(i).equals(deduplicated.get(i % cycleLen))) {
                    isPattern = false;
                    break;
                }
                if (i > 0 && i % cycleLen == 0) repetitions++;
            }

            if (isPattern && repetitions >= 2) {
                return deduplicated.subList(0, cycleLen);
            }
        }

        return deduplicated;
    }

    private static List<String> collapseConsecutiveDuplicates(final List<String> lines) {
        if (lines.isEmpty()) return lines;

        final List<String> result = new ArrayList<>();
        String previous = null;

        for (final String line : lines) {
            if (line.equals(previous)) continue;
            result.add(line);
            previous = line;
        }

        return result;
    }

    public static boolean isSameLoader(final ClassLoader loader, final Throwable error) {
        return isSameLoader(loader, error, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean isSameLoader(final ClassLoader loader, @Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return false;

        final StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0)
            return isSameLoader(loader, error.getCause(), visited);

        final int firstNonLibraryIndex = findFirstNonLibraryFrameIndex(stackTrace);
        if (firstNonLibraryIndex == -1) return isSameLoader(loader, error.getCause(), visited);

        final int framesToCheck = Math.min(5, stackTrace.length - firstNonLibraryIndex);

        for (int i = 0; i < framesToCheck; i++) {
            final StackTraceElement frame = stackTrace[firstNonLibraryIndex + i];
            if (isLibraryFrame(frame.getClassName())) continue;
            if (!isFromLoader(frame, loader)) return isSameLoader(loader, error.getCause(), visited);
        }

        return true;
    }

    private static int findFirstNonLibraryFrameIndex(final StackTraceElement[] stackTrace) {
        for (int i = 0; i < stackTrace.length; i++) {
            if (!isLibraryFrame(stackTrace[i].getClassName())) return i;
        }
        return -1;
    }

    static boolean isLibraryFrame(final String frame) {
        return frame.startsWith("java.")
                || frame.startsWith("javax.")
                || frame.startsWith("sun.")
                || frame.startsWith("com.sun.")
                || frame.startsWith("jdk.");
    }

    private static boolean isFromLoader(final StackTraceElement frame, final ClassLoader loader) {
        try {
            final Class<?> clazz = Class.forName(frame.getClassName(), false, loader);
            return isSameClassLoader(clazz.getClassLoader(), loader);
        } catch (final Throwable t) {
            return false;
        }
    }

    private static boolean isSameClassLoader(final ClassLoader classLoader, final ClassLoader loader) {
        if (classLoader == loader) return true;
        ClassLoader current = classLoader;
        while (current != null && current != loader) {
            current = current.getParent();
        }
        return loader == current;
    }

    private static @Nullable String getAnonymizedMessage(final Throwable error, final List<Map.Entry<Pattern, String>> customPatterns) {
        final String message = error.getMessage();
        if (message == null) return null;
        String truncated = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH) + "..."
                : message;
        for (final Map.Entry<Pattern, String> entry : customPatterns) {
            truncated = entry.getKey().matcher(truncated).replaceAll(entry.getValue());
        }
        return truncated;
    }

    private static List<Map.Entry<Pattern, String>> defaultAnonymizationEntries() {
        final List<Map.Entry<Pattern, String>> entries = new ArrayList<>();
        entries.add(entry(ipv4Pattern(), "[IP hidden]"));
        entries.add(entry(ipv6Pattern(), "[IP hidden]"));
        entries.add(entry(userHomePathPattern(), "$1$2$3[username hidden]"));
        entries.add(entry(discordWebhookPattern(), "$1[token hidden]"));
        entries.add(entry(jdbcUrlPattern(), "$1[password hidden]$2"));
        usernamePattern().ifPresent(pattern -> entries.add(entry(pattern, "[username hidden]")));
        return entries;
    }

    private static Map.Entry<Pattern, String> entry(final Pattern pattern, final String replacement) {
        return new AbstractMap.SimpleImmutableEntry<>(pattern, replacement);
    }

    private static Pattern discordWebhookPattern() {
        return Pattern.compile("(https://discord\\.com/api/webhooks/\\d+/)[\\w-]+");
    }

    private static Pattern ipv4Pattern() {
        return Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");
    }

    private static Pattern ipv6Pattern() {
        return Pattern.compile("(?i)\\b([0-9a-f]{1,4}:){7}[0-9a-f]{1,4}\\b|" + // Full form
                "(?i)\\b([0-9a-f]{1,4}:){1,7}:\\b|" +                          // Trailing ::
                "(?i)\\b([0-9a-f]{1,4}:){1,6}:[0-9a-f]{1,4}\\b|" +             // :: in middle (1 group after)
                "(?i)\\b([0-9a-f]{1,4}:){1,5}(:[0-9a-f]{1,4}){1,2}\\b|" +      // :: in middle (2 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,4}(:[0-9a-f]{1,4}){1,3}\\b|" +      // :: in middle (3 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,3}(:[0-9a-f]{1,4}){1,4}\\b|" +      // :: in middle (4 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,2}(:[0-9a-f]{1,4}){1,5}\\b|" +      // :: in middle (5 groups after)
                "(?i)\\b[0-9a-f]{1,4}:(:[0-9a-f]{1,4}){1,6}\\b|" +             // :: in middle (6 groups after)
                "(?i)\\b:(:[0-9a-f]{1,4}){1,7}\\b|" +                          // Leading ::
                "(?i)\\b::([0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4}\\b|" +            // :: at start
                "(?i)\\b::\\b");                                               // Just ::
    }

    private static Pattern jdbcUrlPattern() {
        return Pattern.compile("(jdbc:[^:]+://[^:]+:(?:\\d+:)?)[^@]+(@)");
    }

    private static Pattern userHomePathPattern() {
        return Pattern.compile("(/home/)[^/\\s]+" +       // Linux: /home/username
                "|(/Users/)[^/\\s]+" +                    // macOS: /Users/username
                "|((?i)[A-Z]:\\\\Users\\\\)[^\\\\\\s]+"); // Windows: A-Z:\\Users\\username
    }

    private static Optional<Pattern> usernamePattern() {
        return Optional.ofNullable(System.getProperty("user.name"))
                .filter(s -> s.trim().length() > 2)
                .filter(s -> !allowedNames.contains(s.toLowerCase(Locale.ROOT)))
                .map(Pattern::quote)
                .map(s -> Pattern.compile(s, Pattern.CASE_INSENSITIVE));
    }
}
