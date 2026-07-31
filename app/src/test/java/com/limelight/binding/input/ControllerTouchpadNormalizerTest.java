package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerTouchpadNormalizerTest {
    private static final float TOLERANCE = 0.0001f;

    @Test
    public void physicalRangeMapsToUnitInterval() {
        assertEquals(0, normalize(10, 10, 20), TOLERANCE);
        assertEquals(0.5f, normalize(20, 10, 20), TOLERANCE);
        assertEquals(1, normalize(30, 10, 20), TOLERANCE);
    }

    @Test
    public void valuesOutsidePhysicalRangeAreClamped() {
        assertEquals(0, normalize(-100, 10, 20), TOLERANCE);
        assertEquals(1, normalize(100, 10, 20), TOLERANCE);
    }

    @Test
    public void invalidRangesProduceSafeNeutralValue() {
        assertEquals(0, normalize(5, 0, 0), TOLERANCE);
        assertEquals(0, normalize(5, 0, -1), TOLERANCE);
        assertEquals(
                0,
                normalize(Float.NaN, 0, 1),
                TOLERANCE);
        assertEquals(
                0,
                normalize(0, Float.POSITIVE_INFINITY, 1),
                TOLERANCE);
    }

    private static float normalize(
            float value,
            float minimum,
            float range) {
        return ControllerTouchpadNormalizer.normalize(
                value,
                minimum,
                range);
    }
}
