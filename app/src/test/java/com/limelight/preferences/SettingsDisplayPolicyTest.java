package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.stream.StreamResolutionCodec;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public final class SettingsDisplayPolicyTest {
    @Test
    public void policyBuildsOrderedDeduplicatedNativeOptions() {
        SettingsDisplayCapabilities capabilities =
                SettingsDisplayCapabilities.builder()
                        .addNativeResolution(2200, 1800, false)
                        .addNativeResolution(2200, 1800, false)
                        .addNativeResolution(3840, 2160, true)
                        .maximumSupportedPresetWidth(1280)
                        .maximumRefreshRate(60)
                        .hdrState(SettingsDisplayCapabilities.HdrState
                                .BLOCKED_BY_FIRMWARE)
                        .build();

        SettingsDisplayPolicy.Result result =
                SettingsDisplayPolicy.evaluate(
                        capabilities,
                        Collections.singleton("1000x1000"),
                        false);

        List<SettingsDisplayPolicy.ResolutionOption> options =
                result.getResolutionOptions();
        assertEquals(4, options.size());
        assertOption(
                options.get(0),
                "1000x1000",
                false,
                SettingsDisplayPolicy.OrientationLabel.NONE);
        assertTrue(options.get(0).isCustom());
        assertOption(
                options.get(1),
                "1800x2200",
                false,
                SettingsDisplayPolicy.OrientationLabel.PORTRAIT);
        assertOption(
                options.get(2),
                "2200x1800",
                false,
                SettingsDisplayPolicy.OrientationLabel.LANDSCAPE);
        assertOption(
                options.get(3),
                "3840x2160",
                true,
                SettingsDisplayPolicy.OrientationLabel.NONE);
        assertEquals(3, result.getResolutionRemovals().size());
        assertRemoval(
                result.getResolutionRemovals().get(0),
                StreamResolutionCodec.RESOLUTION_4K,
                StreamResolutionCodec.RESOLUTION_1440P);
        assertRemoval(
                result.getResolutionRemovals().get(2),
                StreamResolutionCodec.RESOLUTION_1080P,
                StreamResolutionCodec.RESOLUTION_720P);
        assertEquals(2, result.getFrameRateRemovals().size());
        assertEquals(60, result.getNativeFrameRate());
        assertEquals(
                SettingsDisplayCapabilities.HdrState
                        .BLOCKED_BY_FIRMWARE,
                result.getHdrState());
        assertFalse(result.hasInvalidCustomResolution());
    }

    @Test
    public void unlockFrameRatesPreservesEveryConfiguredValue() {
        SettingsDisplayPolicy.Result result =
                SettingsDisplayPolicy.evaluate(
                        SettingsDisplayCapabilities.builder()
                                .maximumRefreshRate(30)
                                .build(),
                        Collections.singleton("1920x1080"),
                        true);

        assertTrue(result.getFrameRateRemovals().isEmpty());
        assertEquals(30, result.getNativeFrameRate());
    }

    @Test
    public void unknownMaximumWidthDoesNotRemoveResolutionPresets() {
        SettingsDisplayPolicy.Result result =
                SettingsDisplayPolicy.evaluate(
                        SettingsDisplayCapabilities.builder().build(),
                        null,
                        false);

        assertTrue(result.getResolutionRemovals().isEmpty());
    }

    @Test
    public void invalidCustomResolutionIsIgnoredAndReported() {
        SettingsDisplayPolicy.Result result =
                SettingsDisplayPolicy.evaluate(
                        SettingsDisplayCapabilities.builder().build(),
                        Collections.singleton("1920-by-1080"),
                        false);

        assertTrue(result.hasInvalidCustomResolution());
        assertTrue(result.getResolutionOptions().isEmpty());
    }

    @Test
    public void frameRateThresholdsMatchDisplayTolerance() {
        SettingsDisplayPolicy.Result atNinetyThreshold =
                SettingsDisplayPolicy.evaluate(
                        SettingsDisplayCapabilities.builder()
                                .maximumRefreshRate(88)
                                .build(),
                        null,
                        false);
        SettingsDisplayPolicy.Result atOneTwentyThreshold =
                SettingsDisplayPolicy.evaluate(
                        SettingsDisplayCapabilities.builder()
                                .maximumRefreshRate(118)
                                .build(),
                        null,
                        false);

        assertEquals(1,
                atNinetyThreshold.getFrameRateRemovals().size());
        assertTrue(atOneTwentyThreshold
                .getFrameRateRemovals()
                .isEmpty());
    }

    private static void assertOption(
            SettingsDisplayPolicy.ResolutionOption option,
            String value,
            boolean fullscreen,
            SettingsDisplayPolicy.OrientationLabel orientation) {
        assertEquals(value, option.getValue());
        assertEquals(fullscreen, option.isFullscreen());
        assertEquals(orientation, option.getOrientationLabel());
    }

    private static void assertRemoval(
            SettingsDisplayPolicy.ValueRemoval removal,
            String value,
            String fallback) {
        assertEquals(value, removal.getValue());
        assertEquals(fallback, removal.getFallbackValue());
    }
}
