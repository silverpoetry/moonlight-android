package com.limelight.utils;

import org.junit.Test;

import static com.limelight.utils.StreamOrientationPolicy.Mode.FOLLOW_USER;
import static com.limelight.utils.StreamOrientationPolicy.Mode.FOLLOW_USER_ALL_ROTATIONS;
import static com.limelight.utils.StreamOrientationPolicy.Mode.SENSOR_LANDSCAPE;
import static com.limelight.utils.StreamOrientationPolicy.Mode.SENSOR_PORTRAIT;
import static com.limelight.utils.StreamOrientationPolicy.Mode.USER_LANDSCAPE;
import static com.limelight.utils.StreamOrientationPolicy.Mode.USER_PORTRAIT;
import static com.limelight.utils.StreamOrientationRequest.ManualOrientation.LANDSCAPE;
import static com.limelight.utils.StreamOrientationRequest.ManualOrientation.NONE;
import static com.limelight.utils.StreamOrientationRequest.ManualOrientation.PORTRAIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamOrientationPolicyTest {
    @Test
    public void adaptiveWindowAlwaysFollowsUser() {
        assertEquals(FOLLOW_USER_ALL_ROTATIONS, resolve(
                true, 700, 900, true, true,
                1080, 1920, true, false, NONE));
    }

    @Test
    public void compactRegularWindowPreservesExistingPreferences() {
        assertEquals(SENSOR_PORTRAIT, resolve(
                false, 393, 873, false, false,
                1920, 1080, true, false, NONE));
        assertEquals(FOLLOW_USER, resolve(
                false, 393, 873, false, false,
                1920, 1080, false, true, NONE));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 393, 873, false, false,
                1920, 1080, false, false, NONE));
    }

    @Test
    public void compactSquarishWindowUsesStreamAndControllerIntent() {
        assertEquals(USER_PORTRAIT, resolve(
                false, 700, 750, true, true,
                1080, 1920, false, false, NONE));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 700, 750, true, true,
                1920, 1080, false, false, NONE));
        assertEquals(USER_LANDSCAPE, resolve(
                false, 700, 750, true, false,
                1920, 1080, false, false, NONE));
        assertEquals(FOLLOW_USER_ALL_ROTATIONS, resolve(
                false, 700, 750, false, false,
                1920, 1080, false, false, NONE));
    }

    @Test
    public void manualButtonDirectionWinsWindowAndPreferencePolicy() {
        assertEquals(SENSOR_LANDSCAPE, resolve(
                true, 700, 900, false, false,
                1080, 1920, true, true, LANDSCAPE));
        assertEquals(SENSOR_PORTRAIT, resolve(
                false, 700, 750, true, true,
                1920, 1080, false, false, PORTRAIT));
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
            boolean automaticOrientationEnabled,
            StreamOrientationRequest.ManualOrientation manualOrientation) {
        return StreamOrientationPolicy.resolveGameMode(
                adaptiveWindow,
                windowWidthDp,
                windowHeightDp,
                onScreenControllerEnabled,
                nativeResolution,
                streamWidth,
                streamHeight,
                portraitRequested,
                automaticOrientationEnabled,
                manualOrientation);
    }
}
