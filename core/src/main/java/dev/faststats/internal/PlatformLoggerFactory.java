package dev.faststats.internal;

import org.jspecify.annotations.Nullable;

public abstract class PlatformLoggerFactory extends LoggerFactory {
    @FunctionalInterface
    public interface Printer {
        void print(Logger.LogLevel level, @Nullable Throwable throwable, String message);
    }

    public static PlatformLoggerFactory create(final Printer printer) {
        return new PlatformLoggerFactory() {
            @Override
            public Logger getLogger(final Class<?> clazz) {
                return new PlatformLogger(clazz.getName());
            }

            @Override
            protected void print(final Logger.LogLevel level, @Nullable final Throwable throwable, final String message) {
                printer.print(level, throwable, message);
            }
        };
    }

    protected abstract void print(Logger.LogLevel level, @Nullable Throwable throwable, String message);

    private final class PlatformLogger implements Logger {
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
            PlatformLoggerFactory.this.print(level, throwable, message);
        }
    }
}
