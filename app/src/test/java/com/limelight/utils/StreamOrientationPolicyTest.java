package com.limelight.utils;

import org.junit.Test;

import static com.limelight.utils.StreamOrientationPolicy.Mode.FOLLOW_USER;
import static com.limelight.utils.StreamOrientationPolicy.Mode.FOLLOW_USER_ALL_ROTATIONS;
import static com.limelight.utils.StreamOrientationPolicy.Mode.SENSOR_PORTRAIT;
import static com.limelight.utils.StreamOrientationPolicy.Mode.USER_LANDSCAPE;
import static com.limelight.utils.StreamOrientationPolicy.Mode.USER_PORTRAIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamOrientationPolicyTest {
    @Test
    public void adaptiveWindowAlwaysFollowsUser() {
        assertEquals(FOLLOW_USER_ALL_ROTATIONS, resolve(
                true, 700, 900, true, true,
                1080, 1920, true, false));
    }

    @Test
    public void compactRegularWindowPreservesExistingPreferences() {
        assertEquals(SENSOR_PORTRAIT, resolve(
                false, 393, 873, false, false,
                1920, 1080, true, false));
        assertEquals(FOLLOW_USER, resolve(
                false, 393, 873, false, false,
                1920, 1080, false, true));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 393, 873, false, false,
                1920, 1080, false, false));
    }

    @Test
    public void compactSquarishWindowUsesStreamAndControllerIntent() {
        assertEquals(USER_PORTRAIT, resolve(
                false, 700, 750, true, true,
                1080, 1920, false, false));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 700, 750, true, true,
                1920, 1080, false, false));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 700, 750, true, false,
                1920, 1080, false, false));
        assertEquals(FOLLOW_USER_ALL_ROTATIONS, resolve(
                false, 700, 750, false, false,
                1920, 1080, false, false));
    }

    @Test
    public void sbsOnlyRequestsLandscapeForCompactFullscreen() {
        assertEquals(USER_LANDSCAPE,
                StreamOrientationPolicy.resolveSbsMode(false));
        assertEquals(FOLLOW_USER_ALL_ROTATIONS,
                StreamOrientationPolicy.resolveSbsMode(true));
    }

    @Test
    public void squarishClassificationRejectsUnknownDimensions() {
        assertFalse(StreamOrientationPolicy.isSquarish(0, 800));
        assertFalse(StreamOrientationPolicy.isSquarish(800, 0));
        assertTrue(StreamOrientationPolicy.isSquarish(700, 750));
        assertFalse(StreamOrientationPolicy.isSquarish(600, 900));
    }

    private static StreamOrientationPolicy.Mode resolve(
            boolean adaptiveWindow,
            int windowWidthDp,
            int windowHeightDp,
            boolean onScreenControllerEnabled,
            boolean nativeResolution,
            int streamWidth,
            int streamHeight,
            boolean portraitRequested,
            boolean automaticOrientationEnabled) {
        return StreamOrientationPolicy.resolveGameMode(
                adaptiveWindow,
                windowWidthDp,
                windowHeightDp,
                onScreenControllerEnabled,
                nativeResolution,
                streamWidth,
                streamHeight,
                portraitRequested,
                automaticOrientationEnabled);
    }
}
