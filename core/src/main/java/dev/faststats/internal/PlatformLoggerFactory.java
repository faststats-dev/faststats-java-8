package dev.faststats.internal;

import org.jspecify.annotations.Nullable;

public final class PlatformLoggerFactory extends LoggerFactory {
    @Override
    public Logger getLogger(final Class<?> clazz) {
        return new PlatformLogger(clazz.getName());
    }

    @FunctionalInterface
    public interface Printer {
        void print(Logger.LogLevel level, @Nullable Throwable throwable, String message);
    }

    private final Printer printer;

    public PlatformLoggerFactory(final Printer printer) {
        this.printer = printer;
    }

    final class PlatformLogger implements Logger {
        private final String caller;

        private PlatformLogger(final String caller) {
            this.caller = caller;
        }

        @Override
        public String caller() {
            return caller;
        }

        @Override
        public LoggerFactory factory() {
            return PlatformLoggerFactory.this;
        }

        @Override
        public void print(final LogLevel level, @Nullable final Throwable throwable, final String message) {
            printer.print(level, throwable, message);
        }
    }
}
