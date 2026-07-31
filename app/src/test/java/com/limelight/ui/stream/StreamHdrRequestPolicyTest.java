package com.limelight.ui.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class StreamHdrRequestPolicyTest {
    @Test
    public void ignoredCapabilityForcesHdrWithoutWarning() {
        StreamHdrRequestPolicy.Decision decision = decide(
                false,
                true,
                false,
                false,
                false);

        assertTrue(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning.NONE,
                decision.getWarning());
    }

    @Test
    public void disabledSettingDoesNotProbeFailuresToTheUser() {
        StreamHdrRequestPolicy.Decision decision = decide(
                false,
                false,
                false,
                true,
                false);

        assertFalse(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning.NONE,
                decision.getWarning());
    }

    @Test
    public void blockedFirmwareDisablesHdrWithoutWarning() {
        StreamHdrRequestPolicy.Decision decision = decide(
                true,
                false,
                true,
                false,
                true);

        assertFalse(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning.NONE,
                decision.getWarning());
    }

    @Test
    public void oldAndroidVersionProducesVersionWarning() {
        StreamHdrRequestPolicy.Decision decision = decide(
                true,
                false,
                false,
                true,
                false);

        assertFalse(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning
                        .ANDROID_VERSION_UNSUPPORTED,
                decision.getWarning());
    }

    @Test
    public void missingHdr10DisplayProducesDisplayWarning() {
        StreamHdrRequestPolicy.Decision decision = decide(
                true,
                false,
                true,
                true,
                false);

        assertFalse(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning
                        .DISPLAY_HDR10_UNSUPPORTED,
                decision.getWarning());
    }

    @Test
    public void supportedDeviceRequestsHdrWithoutWarning() {
        StreamHdrRequestPolicy.Decision decision = decide(
                true,
                false,
                true,
                true,
                true);

        assertTrue(decision.isHdrRequested());
        assertEquals(
                StreamHdrRequestPolicy.Warning.NONE,
                decision.getWarning());
    }

    @Test
    public void impossibleCapabilitySnapshotIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StreamHdrRequestPolicy.DeviceCapabilities(
                        false,
                        true,
                        true));
    }

    private static StreamHdrRequestPolicy.Decision decide(
            boolean hdrEnabled,
            boolean ignoreHdrCapability,
            boolean canQueryHdrCapabilities,
            boolean hdrStreamingAllowed,
            boolean displaySupportsHdr10) {
        return StreamHdrRequestPolicy.decide(
                hdrEnabled,
                ignoreHdrCapability,
                new StreamHdrRequestPolicy.DeviceCapabilities(
                        canQueryHdrCapabilities,
                        hdrStreamingAllowed,
                        displaySupportsHdr10));
    }
}
