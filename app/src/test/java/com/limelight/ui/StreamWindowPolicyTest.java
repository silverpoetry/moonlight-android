package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamWindowPolicyTest {
    @Test
    public void explicitFullscreenPoliciesUseEntireDisplay() {
        assertTrue(StreamWindowPolicy.shouldUseEntireDisplay(
                true, false, false, false));
        assertTrue(StreamWindowPolicy.shouldUseEntireDisplay(
                false, true, false, false));
    }

    @Test
    public void nativeResolutionRequiresPhysicalModeMatch() {
        assertFalse(StreamWindowPolicy.shouldUseEntireDisplay(
                false, false, true, false));
        assertTrue(StreamWindowPolicy.shouldUseEntireDisplay(
                false, false, true, true));
    }

    @Test
    public void presetResolutionDoesNotInferFullscreenFromModeMatch() {
        assertFalse(StreamWindowPolicy.shouldUseEntireDisplay(
                false, false, false, true));
    }

    @Test
    public void physicalResolutionMatchesEitherDisplayOrientation() {
        assertTrue(StreamWindowPolicy.matchesPhysicalResolution(
                2400, 1080, 2400, 1080));
        assertTrue(StreamWindowPolicy.matchesPhysicalResolution(
                1080, 2400, 2400, 1080));
        assertFalse(StreamWindowPolicy.matchesPhysicalResolution(
                1920, 1080, 2400, 1080));
    }

    @Test
    public void invalidResolutionNeverMatches() {
        assertFalse(StreamWindowPolicy.matchesPhysicalResolution(
                0, 1080, 2400, 1080));
        assertFalse(StreamWindowPolicy.matchesPhysicalResolution(
                2400, 1080, -1, 1080));
    }
}
