package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControlStreamRttTest {
    @Test
    public void decodesRttAndVarianceFromUnsignedPackedFields() {
        ControlStreamRtt rtt = ControlStreamRtt.fromPacked(
                (42L << 32) | 7L);

        assertTrue(rtt.isAvailable());
        assertEquals(42, rtt.getRoundTripTimeMs());
        assertEquals(7, rtt.getVarianceMs());
    }

    @Test
    public void preservesAnAvailableZeroMillisecondEstimate() {
        ControlStreamRtt rtt = ControlStreamRtt.fromPacked(0L);

        assertTrue(rtt.isAvailable());
        assertEquals(0, rtt.getRoundTripTimeMs());
        assertEquals(0, rtt.getVarianceMs());
    }

    @Test
    public void recognizesNativeUnavailableSentinel() {
        ControlStreamRtt rtt = ControlStreamRtt.fromPacked(-1L);

        assertFalse(rtt.isAvailable());
    }
}
