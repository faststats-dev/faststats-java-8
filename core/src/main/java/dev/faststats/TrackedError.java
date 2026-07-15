package dev.faststats;

import org.jetbrains.annotations.Contract;

/**
 * An error report with tracking metadata.
 *
 * @since 0.24.0
 */
public interface TrackedError {
    /**
     * Returns the tracked error.
     *
     * @return the tracked error
     * @since 0.24.0
     */
    @Contract(pure = true)
    Throwable error();

    /**
     * Returns whether the error was handled.
     *
     * @return whether the error was handled
     * @since 0.24.0
     */
    @Contract(pure = true)
    boolean handled();

    /**
     * Sets whether the error was handled.
     *
     * @param handled whether the error was handled
     * @return this tracked error
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    TrackedError handled(boolean handled);

    /**
     * Returns a copy of the additional error attributes.
     *
     * @return a copy of the additional error attributes
     * @since 0.24.0
     */
    @Contract(value = " -> new", pure = true)
    Attributes attributes();

    /**
     * Sets the additional error attributes.
     *
     * @param attributes the additional error attributes
     * @return this tracked error
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    TrackedError attributes(Attributes attributes);
}
