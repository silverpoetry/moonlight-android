package com.limelight.ui.stream;

import com.limelight.settings.stream.StreamDecoderSettings;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamDisplayRefreshPolicyTest {
    @Test
    public void explicitPacingModesAlwaysAllowReduction() {
        assertTrue(mayReduce(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                false));
        assertTrue(mayReduce(
                StreamDecoderSettings.FramePacing.MAXIMUM_SMOOTHNESS,
                false));
    }

    @Test
    public void balancedModeUsesItsReductionSetting() {
        assertFalse(mayReduce(
                StreamDecoderSettings.FramePacing.BALANCED,
                false));
        assertTrue(mayReduce(
                StreamDecoderSettings.FramePacing.BALANCED,
                true));
    }

    @Test
    public void minimumLatencyNeverAllowsReduction() {
        assertFalse(mayReduce(
                StreamDecoderSettings.FramePacing.MINIMUM_LATENCY,
                true));
    }

    @Test
    public void selectedPhoneFamiliesLeaveRefreshRateToSystem() {
        assertTrue(systemManaged(false, "Xiaomi", "unknown"));
        assertTrue(systemManaged(false, "unknown", "Redmi"));
        assertTrue(systemManaged(false, "Unknown", "POCO"));
    }

    @Test
    public void televisionsAndOtherDevicesUseAppSelection() {
        assertFalse(systemManaged(true, "Xiaomi", "Redmi"));
        assertFalse(systemManaged(false, "Google", "Pixel"));
        assertFalse(systemManaged(false, null, null));
    }

    private static boolean mayReduce(
            StreamDecoderSettings.FramePacing framePacing,
            boolean balancedReductionEnabled) {
        return StreamDisplayRefreshPolicy.mayReduceRefreshRate(
                framePacing,
                balancedReductionEnabled);
    }

    private static boolean systemManaged(
            boolean television,
            String manufacturer,
            String brand) {
        return StreamDisplayRefreshPolicy
                .shouldLetSystemManageRefreshRate(
                        television,
                        manufacturer,
                        brand);
    }
}
