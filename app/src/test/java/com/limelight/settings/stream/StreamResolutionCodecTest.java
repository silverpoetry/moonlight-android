package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamResolutionCodecTest {
    private static final StreamResolutionCodec.DisplayAspect DISPLAY_20_9 =
            new StreamResolutionCodec.DisplayAspect(2400, 1080);

    @Test
    public void validPresetRoundTripsWithoutRepair() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "1920x1080",
                        "preset",
                        "16_9",
                        "120",
                        DISPLAY_20_9);

        assertEquals(1920, result.getWidth());
        assertEquals(1080, result.getHeight());
        assertEquals(120, result.getFps());
        assertEquals(
                StreamResolutionCodec.Selection.PRESET,
                result.getSelection());
        assertFalse(result.isRepairRequired());
    }

    @Test
    public void nativeAspectPresetUsesEvenDisplayRatioHeight() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "1920x1080",
                        "preset",
                        "native",
                        "60",
                        DISPLAY_20_9);

        assertEquals(1920, result.getWidth());
        assertEquals(864, result.getHeight());
        assertFalse(result.isRepairRequired());
    }

    @Test
    public void customResolutionDoesNotApplyPresetAspectOverride() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "2000x1000",
                        "custom_or_native",
                        "native",
                        "90",
                        DISPLAY_20_9);

        assertEquals(2000, result.getWidth());
        assertEquals(1000, result.getHeight());
        assertEquals(
                StreamResolutionCodec.Selection.CUSTOM_OR_NATIVE,
                result.getSelection());
    }

    @Test
    public void legacyResolutionIsCanonicalizedIdempotently() {
        StreamResolutionCodec.Result migrated =
                StreamResolutionCodec.decode(
                        "1080p",
                        "custom_or_native",
                        "native",
                        "60",
                        DISPLAY_20_9);

        assertEquals("1920x1080", migrated.getCanonicalResolution());
        assertEquals("preset", migrated.getCanonicalSelection());
        assertEquals("16_9", migrated.getCanonicalAspectRatio());
        assertTrue(migrated.isRepairRequired());

        StreamResolutionCodec.Result reread =
                StreamResolutionCodec.decode(
                        migrated.getCanonicalResolution(),
                        migrated.getCanonicalSelection(),
                        migrated.getCanonicalAspectRatio(),
                        migrated.getCanonicalFps(),
                        DISPLAY_20_9);
        assertFalse(reread.isRepairRequired());
    }

    @Test
    public void corruptResolutionAndFpsUseSafeCanonicalDefaults() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "not-a-resolution",
                        null,
                        null,
                        "NaN",
                        DISPLAY_20_9);

        assertEquals(1280, result.getWidth());
        assertEquals(720, result.getHeight());
        assertEquals(60, result.getFps());
        assertEquals("1280x720", result.getCanonicalResolution());
        assertEquals("60", result.getCanonicalFps());
        assertEquals("preset", result.getCanonicalSelection());
        assertEquals("16_9", result.getCanonicalAspectRatio());
        assertTrue(result.isRepairRequired());
    }

    @Test
    public void whitespaceAndMultiplicationSignAreCanonicalized() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        " 2560×1440 ",
                        "custom_or_native",
                        "16_9",
                        " 90 ",
                        DISPLAY_20_9);

        assertEquals(2560, result.getWidth());
        assertEquals(1440, result.getHeight());
        assertEquals(90, result.getFps());
        assertEquals("2560x1440", result.getCanonicalResolution());
        assertEquals("90", result.getCanonicalFps());
        assertEquals(
                StreamResolutionCodec.Selection.CUSTOM_OR_NATIVE,
                result.getSelection());
        assertTrue(result.isRepairRequired());
    }

    @Test
    public void nonPositiveDimensionsCannotEscapeCodec() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "1920x0",
                        "custom_or_native",
                        "native",
                        "-1",
                        DISPLAY_20_9);

        assertEquals(1280, result.getWidth());
        assertEquals(720, result.getHeight());
        assertEquals(60, result.getFps());
    }

    @Test
    public void invalidDisplayAspectFallsBackToSixteenByNine() {
        StreamResolutionCodec.Result result =
                StreamResolutionCodec.decode(
                        "1920x1080",
                        "preset",
                        "native",
                        "60",
                        new StreamResolutionCodec.DisplayAspect(0, 0));

        assertEquals(1080, result.getHeight());
    }
}
