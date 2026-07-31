package com.limelight.ui.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ExternalDisplaySelectionPolicyTest {
    @Test
    public void selectsFirstNonDefaultDisplayInPlatformOrder() {
        assertEquals(
                7,
                ExternalDisplaySelectionPolicy.select(
                        0,
                        new int[] {0, 7, 9}));
    }

    @Test
    public void returnsNoDisplayWhenOnlyDefaultIsAvailable() {
        assertEquals(
                ExternalDisplaySelectionPolicy.NO_DISPLAY,
                ExternalDisplaySelectionPolicy.select(
                        0,
                        new int[] {0}));
        assertEquals(
                ExternalDisplaySelectionPolicy.NO_DISPLAY,
                ExternalDisplaySelectionPolicy.select(0, null));
    }
}
