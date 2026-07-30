package com.limelight.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WindowInsetsPolicyTest {
    private static final WindowInsetsPolicy.EdgeInsets SYSTEM_BARS =
            new WindowInsetsPolicy.EdgeInsets(8, 24, 12, 36);
    private static final WindowInsetsPolicy.EdgeInsets DISPLAY_CUTOUT =
            new WindowInsetsPolicy.EdgeInsets(40, 10, 0, 4);

    @Test
    public void fullscreenOptInUsesEntireDisplay() {
        assertEquals(WindowInsetsPolicy.NONE,
                WindowInsetsPolicy.resolveStreamInsets(
                        false, true, SYSTEM_BARS, DISPLAY_CUTOUT));
    }

    @Test
    public void fullscreenWithoutOptInProtectsDisplayCutoutOnly() {
        assertEquals(DISPLAY_CUTOUT,
                WindowInsetsPolicy.resolveStreamInsets(
                        false, false, SYSTEM_BARS, DISPLAY_CUTOUT));
    }

    @Test
    public void multiWindowOptInStillProtectsVisibleSystemBars() {
        assertEquals(SYSTEM_BARS,
                WindowInsetsPolicy.resolveStreamInsets(
                        true, true, SYSTEM_BARS, DISPLAY_CUTOUT));
    }

    @Test
    public void multiWindowWithoutOptInUsesMaximumSafeInsetsPerEdge() {
        assertEquals(new WindowInsetsPolicy.EdgeInsets(40, 24, 12, 36),
                WindowInsetsPolicy.resolveStreamInsets(
                        true, false, SYSTEM_BARS, DISPLAY_CUTOUT));
    }
}
