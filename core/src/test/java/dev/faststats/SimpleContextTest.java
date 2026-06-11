package dev.faststats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SimpleContextTest {
    @Test
    public void contextWithoutAttachedServicesThrows() {
        final var error = assertThrows(IllegalStateException.class, () -> new MockContext.Factory().create());
        assertEquals("Context created without any service attached, was this intentional?", error.getMessage());
    }

    @Test
    public void contextWithAttachedServicesAndDisabledFeaturesDoesNotThrow() {
        assertDoesNotThrow(() -> new MockContext.Factory()
                .allFeaturesDisabled()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .create()
        );
    }
}
