package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StreamFramePacingPolicyTest {
    @Test
    public void unrelatedModesRemainUnchanged() {
        assertDecision(
                StreamDecoderSettings.FramePacing.BALANCED,
                120,
                60f,
                120,
                StreamDecoderSettings.FramePacing.BALANCED);
    }

    @Test
    public void capModeTargetsOneFrameBelowCloseRefreshRate() {
        assertDecision(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                120,
                120f,
                119,
                StreamDecoderSettings.FramePacing.CAP_FPS);
    }

    @Test
    public void capModeFallsBackWithoutMutatingRequestedSettings() {
        assertDecision(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                120,
                60f,
                120,
                StreamDecoderSettings.FramePacing.BALANCED);
        assertDecision(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                48,
                48f,
                48,
                StreamDecoderSettings.FramePacing.BALANCED);
    }

    @Test
    public void capModeDoesNotConstrainFasterDisplay() {
        assertDecision(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                60,
                120f,
                60,
                StreamDecoderSettings.FramePacing.CAP_FPS);
    }

    private static void assertDecision(
            StreamDecoderSettings.FramePacing requested,
            int streamFps,
            float displayRefreshRate,
            int expectedFps,
            StreamDecoderSettings.FramePacing expectedMode) {
        StreamFramePacingPolicy.Decision decision =
                StreamFramePacingPolicy.resolve(
                        requested,
                        streamFps,
                        displayRefreshRate);

        assertEquals(expectedFps, decision.getTargetFps());
        assertEquals(expectedMode, decision.getEffectiveMode());
    }
}
