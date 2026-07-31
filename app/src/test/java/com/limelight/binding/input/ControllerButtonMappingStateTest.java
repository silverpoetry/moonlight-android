package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerButtonMappingStateTest {
    @Test
    public void learnedPhysicalButtonsDisableReplacementFallbacks() {
        ControllerButtonMappingState previous =
                new ControllerButtonMappingState(true, true);
        previous.observeStartButton();
        previous.observeSelectButton();
        ControllerButtonMappingState restored =
                new ControllerButtonMappingState(true, true);

        restored.restoreLearnedStateFrom(previous);

        assertFalse(restored.shouldMapBackToStart());
        assertFalse(restored.shouldMapModeToSelect());
    }

    @Test
    public void restorationNeverReenablesFallbackRejectedByNewProfile() {
        ControllerButtonMappingState previous =
                new ControllerButtonMappingState(true, true);
        ControllerButtonMappingState restored =
                new ControllerButtonMappingState(false, false);

        restored.restoreLearnedStateFrom(previous);

        assertFalse(restored.shouldMapBackToStart());
        assertFalse(restored.shouldMapModeToSelect());
        assertTrue(previous.shouldMapBackToStart());
        assertTrue(previous.shouldMapModeToSelect());
    }
}
