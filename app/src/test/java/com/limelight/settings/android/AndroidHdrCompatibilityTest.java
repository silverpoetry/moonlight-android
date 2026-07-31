package com.limelight.settings.android;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AndroidHdrCompatibilityTest {
    @Test
    public void identifiesOnlyKnownBrokenShieldFirmware() {
        assertTrue(
                AndroidHdrCompatibility
                        .isKnownBrokenShieldFirmware(
                                "NVIDIA",
                                "NVIDIA/PPR1.180610.011/4079208_2235.1395/release"));
        assertTrue(
                AndroidHdrCompatibility
                        .isKnownBrokenShieldFirmware(
                                "nvidia",
                                "PPR1.180610.011/4079208_2235.1395"));
        assertFalse(
                AndroidHdrCompatibility
                        .isKnownBrokenShieldFirmware(
                                "NVIDIA",
                                "different"));
        assertFalse(
                AndroidHdrCompatibility
                        .isKnownBrokenShieldFirmware(
                                "Google",
                                "PPR1.180610.011/4079208_2235.1395"));
    }
}
