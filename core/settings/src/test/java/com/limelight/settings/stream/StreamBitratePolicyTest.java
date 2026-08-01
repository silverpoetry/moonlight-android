package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StreamBitratePolicyTest {
    @Test
    public void knownMoonlightDefaultsRemainStable() {
        assertEquals(
                10_000,
                StreamBitratePolicy
                        .calculateDefaultBitrateKbps(
                                1280,
                                720,
                                60));
        assertEquals(
                20_000,
                StreamBitratePolicy
                        .calculateDefaultBitrateKbps(
                                1920,
                                1080,
                                60));
        assertEquals(
                113_000,
                StreamBitratePolicy
                        .calculateDefaultBitrateKbps(
                                3840,
                                2160,
                                120));
    }

    @Test
    public void extremeCustomResolutionCannotOverflow() {
        assertEquals(
                40_000,
                StreamBitratePolicy
                        .calculateDefaultBitrateKbps(
                                99_999,
                                99_999,
                                30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDimensionsAreRejected() {
        StreamBitratePolicy.calculateDefaultBitrateKbps(
                0,
                1080,
                60);
    }
}
