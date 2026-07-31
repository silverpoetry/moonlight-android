package com.limelight.binding.video;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.stream.StreamDecoderSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class DecoderSelectionPolicyTest {
    @Test
    public void forcedH264SkipsHevc() {
        assertFalse(DecoderSelectionPolicy.shouldAttemptHevc(
                StreamDecoderSettings.VideoFormat.FORCE_H264));
        assertTrue(DecoderSelectionPolicy.shouldAttemptHevc(
                StreamDecoderSettings.VideoFormat.AUTO));
        assertThrows(
                IllegalArgumentException.class,
                () -> DecoderSelectionPolicy
                        .evaluateNonWhitelistedHevc(
                                StreamDecoderSettings
                                        .VideoFormat
                                        .FORCE_H264,
                                true,
                                7680,
                                4320));
    }

    @Test
    public void nonWhitelistedHevcRequiresExplicitReason() {
        assertEquals(
                DecoderSelectionPolicy
                        .NonWhitelistedHevcDecision
                        .ACCEPT_FORCED,
                DecoderSelectionPolicy
                        .evaluateNonWhitelistedHevc(
                                StreamDecoderSettings.VideoFormat
                                        .FORCE_HEVC,
                                false,
                                1920,
                                1080));
        assertEquals(
                DecoderSelectionPolicy
                        .NonWhitelistedHevcDecision
                        .ACCEPT_HDR_REQUIRED,
                DecoderSelectionPolicy
                        .evaluateNonWhitelistedHevc(
                                StreamDecoderSettings.VideoFormat
                                        .AUTO,
                                true,
                                1920,
                                1080));
        assertEquals(
                DecoderSelectionPolicy
                        .NonWhitelistedHevcDecision
                        .ACCEPT_RESOLUTION_REQUIRED,
                DecoderSelectionPolicy
                        .evaluateNonWhitelistedHevc(
                                StreamDecoderSettings.VideoFormat
                                        .AUTO,
                                false,
                                4097,
                                2160));
        assertEquals(
                DecoderSelectionPolicy
                        .NonWhitelistedHevcDecision
                        .CHECK_PERFORMANCE,
                DecoderSelectionPolicy
                        .evaluateNonWhitelistedHevc(
                                StreamDecoderSettings.VideoFormat
                                        .AUTO,
                                false,
                                3840,
                                2160));
    }

    @Test
    public void av1IsOnlyAttemptedWhenForced() {
        assertFalse(DecoderSelectionPolicy.shouldAttemptAv1(
                StreamDecoderSettings.VideoFormat.AUTO));
        assertTrue(DecoderSelectionPolicy.shouldAttemptAv1(
                StreamDecoderSettings.VideoFormat.FORCE_AV1));
    }

    @Test
    public void colorSpaceDefaultsFollowPlatformAndCodecAge() {
        assertEquals(
                MoonBridge.COLORSPACE_REC_601,
                DecoderSelectionPolicy.getPreferredColorSpace(
                        25,
                        false,
                        false));
        assertEquals(
                MoonBridge.COLORSPACE_REC_709,
                DecoderSelectionPolicy.getPreferredColorSpace(
                        26,
                        false,
                        false));
        assertEquals(
                MoonBridge.COLORSPACE_REC_709,
                DecoderSelectionPolicy.getPreferredColorSpace(
                        25,
                        true,
                        false));
        assertEquals(
                MoonBridge.COLORSPACE_REC_709,
                DecoderSelectionPolicy.getPreferredColorSpace(
                        25,
                        false,
                        true));
    }

    @Test
    public void colorRangeDirectlyReflectsStreamSetting() {
        assertEquals(
                MoonBridge.COLOR_RANGE_FULL,
                DecoderSelectionPolicy.getPreferredColorRange(
                        true));
        assertEquals(
                MoonBridge.COLOR_RANGE_LIMITED,
                DecoderSelectionPolicy.getPreferredColorRange(
                        false));
    }
}
