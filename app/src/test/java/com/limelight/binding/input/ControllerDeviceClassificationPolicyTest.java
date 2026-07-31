package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerDeviceClassificationPolicyTest {
    @Test
    public void absentDeviceUsesDefaultControllerContext() {
        assertTrue(classify(true, false, false, false, true));
    }

    @Test
    public void reportedGamepadAlwaysUsesControllerHandling() {
        assertTrue(classify(false, true, false, false, true));
    }

    @Test
    public void android11VirtualDeviceMirrorsAttachedGamepadInventory() {
        assertTrue(classify(false, false, true, true, true));
        assertFalse(classify(false, false, true, false, true));
    }

    @Test
    public void nonAlphabeticFallbackPreservesUnusualControllers() {
        assertTrue(classify(false, false, false, false, false));
        assertFalse(classify(false, false, false, true, true));
    }

    private static boolean classify(
            boolean deviceAbsent,
            boolean reportsGamepadInput,
            boolean isAndroid11VirtualDevice,
            boolean hasAttachedGamepad,
            boolean isAlphabeticKeyboard) {
        return ControllerDeviceClassificationPolicy.isGameController(
                deviceAbsent,
                reportsGamepadInput,
                isAndroid11VirtualDevice,
                hasAttachedGamepad,
                isAlphabeticKeyboard);
    }
}
