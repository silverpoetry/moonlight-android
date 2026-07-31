package com.limelight.nvstream.mic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class MicrophoneUplinkConfigTest {
    @Test
    public void protocolV1HasExactTransportInvariants() {
        MicrophoneUplinkConfig config =
                MicrophoneUplinkConfig.protocolV1();

        assertEquals(48_000, config.getSampleRateHz());
        assertEquals(1, config.getChannelCount());
        assertEquals(960, config.getSamplesPerFrame());
        assertEquals(40_000, config.getOpusBitrateBps());
        assertEquals(4, config.getCaptureBufferFrames());
        assertEquals(20, config.getFrameDurationMillis());
        assertEquals(7_680, config.getCaptureBufferSizeBytes());
    }

    @Test
    public void unsupportedNegotiatedFormatIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MicrophoneUplinkConfig
                        .protocolV1FromNegotiatedValues(
                                44_100,
                                1,
                                960,
                                40_000,
                                4));
        assertThrows(
                IllegalArgumentException.class,
                () -> MicrophoneUplinkConfig
                        .protocolV1FromNegotiatedValues(
                                48_000,
                                2,
                                960,
                                40_000,
                                4));
    }
}
