package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamDisplaySettingsTest {
    @Test
    public void invalidPersistedEnumsUseProductDefaults() {
        assertEquals(
                StreamDisplaySettings.Gravity.DEFAULT,
                StreamDisplaySettings.Gravity.fromStorageValue("broken"));
        assertEquals(
                StreamDisplaySettings.FsrTarget.UNKNOWN,
                StreamDisplaySettings.FsrTarget.fromStorageValue("broken"));
        assertEquals(
                StreamDisplaySettings.FsrSharpness.STANDARD,
                StreamDisplaySettings.FsrSharpness.fromStorageValue("broken"));
        assertEquals(
                StreamDisplaySettings.FsrHdrOutput.SDR,
                StreamDisplaySettings.FsrHdrOutput.fromStorageValue("broken"));
    }

    @Test
    public void externalDisplayDisablesFsrWithoutChangingStoredTarget() {
        StreamDisplaySettings settings = createSettings(
                true,
                StreamDisplaySettings.FsrTarget.OUTPUT_4K,
                true);

        assertFalse(settings.isFsrEnabled());
        assertEquals(
                StreamDisplaySettings.FsrTarget.OUTPUT_4K,
                settings.getFsrTarget());
    }

    @Test
    public void nativeHdrRequiresBothStreamHdrAndNativeOutput() {
        assertTrue(createSettings(
                false,
                StreamDisplaySettings.FsrTarget.OUTPUT_2K,
                true).isNativeHdrOutputEnabled());

        StreamDisplaySettings settings = new StreamDisplaySettings(
                1920,
                1080,
                false,
                false,
                false,
                false,
                true,
                StreamDisplaySettings.Gravity.DEFAULT,
                StreamDisplaySettings.FsrTarget.OUTPUT_2K,
                StreamDisplaySettings.FsrSharpness.STANDARD,
                StreamDisplaySettings.FsrHdrOutput.SDR);
        assertFalse(settings.isNativeHdrOutputEnabled());
    }

    private static StreamDisplaySettings createSettings(
            boolean externalDisplay,
            StreamDisplaySettings.FsrTarget fsrTarget,
            boolean hdrEnabled) {
        return new StreamDisplaySettings(
                1920,
                1080,
                false,
                false,
                false,
                externalDisplay,
                hdrEnabled,
                StreamDisplaySettings.Gravity.DEFAULT,
                fsrTarget,
                StreamDisplaySettings.FsrSharpness.STANDARD,
                StreamDisplaySettings.FsrHdrOutput.NATIVE);
    }
}
