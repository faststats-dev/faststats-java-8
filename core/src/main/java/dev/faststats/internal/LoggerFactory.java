package dev.faststats.internal;

import org.jetbrains.annotations.Contract;

public abstract class LoggerFactory {
    private volatile boolean debug;

    @Contract(value = "_ -> new", pure = true)
    public abstract Logger getLogger(Class<?> clazz);

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
}
