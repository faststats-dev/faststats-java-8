package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SimpleErrorTracker implements ErrorTracker {
    // todo: track unchanged exceptions and counts?
    private final Map<JsonObject, Integer> reports = new ConcurrentHashMap<>();

    private final Map<Class<? extends Throwable>, Set<Pattern>> ignoredTypedPatterns = new ConcurrentHashMap<>();
    private final Set<Class<? extends Throwable>> ignoredTypes = new CopyOnWriteArraySet<>();
    private final Set<Pattern> ignoredPatterns = new CopyOnWriteArraySet<>();
    private final List<Map.Entry<Pattern, String>> anonymizationEntries = new CopyOnWriteArrayList<>();

    private volatile @Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent;
    private volatile @Nullable ClassLoader attachedLoader;
    private volatile boolean contextAttached;

    @Override
    public void trackError(final String message) {
        trackError(message, true);
    }

    @Override
    public void trackError(final Throwable error) {
        trackError(error, true);
    }

    @Override
    public void trackError(final String message, final boolean handled) {
        trackError(new RuntimeException(message), handled);
    }

    @Override
    public void trackError(final Throwable error, final boolean handled) {
        try {
            if (isIgnored(error, Collections.newSetFromMap(new IdentityHashMap<>()))) return;
            final var compiled = ErrorHelper.compile(error, null, handled, anonymizationEntries);
            reports.compute(compiled, (key, report) -> {
                return report != null ? report + 1 : 1;
            });
        } catch (final NoClassDefFoundError ignored) {
        }
    }

    private boolean isIgnored(@Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return false;

        if (ignoredTypes.contains(error.getClass())) return true;

        final var message = error.getMessage() != null ? error.getMessage() : "";
        if (ignoredPatterns.stream().map(pattern -> pattern.matcher(message)).anyMatch(Matcher::find)) return true;

        final var patterns = ignoredTypedPatterns.get(error.getClass());
        if (patterns != null && patterns.stream().map(pattern -> pattern.matcher(message)).anyMatch(Matcher::find))
            return true;

        return isIgnored(error.getCause(), visited);
    }

    @Override
    public ErrorTracker ignoreError(final Class<? extends Throwable> type) {
        ignoredTypes.add(type);
        return this;
    }

    @Override
    public ErrorTracker ignoreError(final Pattern pattern) {
        ignoredPatterns.add(pattern);
        return this;
    }

    @Override
    public ErrorTracker ignoreError(final Class<? extends Throwable> type, final Pattern pattern) {
        ignoredTypedPatterns.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>()).add(pattern);
        return this;
    }

    @Override
    public ErrorTracker anonymize(final Pattern pattern, final String replacement) {
        anonymizationEntries.add(Map.entry(pattern, replacement));
        return this;
    }

    @VisibleForTesting
    public JsonArray getData() {
        final var report = new JsonArray(reports.size());
        reports.forEach((entry, count) -> {
            final var copy = entry.deepCopy();
            if (count > 1) copy.addProperty("count", count);
            report.add(copy);
        });
        return report;
    }

    @VisibleForTesting
    public void clear() {
        reports.clear();
    }

    @Override
    public synchronized void attachErrorContext(@Nullable final ClassLoader loader) throws IllegalStateException {
        if (contextAttached) throw new IllegalStateException("Error context already attached");
        contextAttached = true;
        attachedLoader = loader;
        // fixme: single source of truth?
        // SimpleContext.attachErrorTracker(this);
    }

    @Override
    public synchronized void detachErrorContext() {
        if (!contextAttached) return;
        contextAttached = false;
        // fixme: single source of truth?
        // SimpleContext.detachErrorTracker(this);
    }

    @Override
    public boolean isContextAttached() {
        return contextAttached;
    }

    @Override
    public synchronized void setContextErrorHandler(@Nullable final BiConsumer<@Nullable ClassLoader, Throwable> errorEvent) {
        this.errorEvent = errorEvent;
    }

    @Override
    public synchronized Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler() {
        return Optional.ofNullable(errorEvent);
    }
}
