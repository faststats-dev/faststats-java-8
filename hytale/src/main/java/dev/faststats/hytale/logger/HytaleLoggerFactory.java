package dev.faststats.hytale.logger;

import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;

public final class HytaleLoggerFactory implements LoggerFactory {
    @Override
    public Logger getLogger(final String name) {
        return new HytaleLogger(name);
    }
}
