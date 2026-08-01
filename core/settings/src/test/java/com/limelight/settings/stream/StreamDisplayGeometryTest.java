package com.limelight.settings.stream;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamDisplayGeometryTest {
    @Test
    public void classifiesDisplayShapeIndependentOfOrientation() {
        assertTrue(StreamDisplayGeometry.isSquarish(1_200, 1_000));
        assertTrue(StreamDisplayGeometry.isSquarish(1_000, 1_200));
        assertFalse(StreamDisplayGeometry.isSquarish(1_600, 1_000));
        assertFalse(StreamDisplayGeometry.isSquarish(0, 1_000));
    }
}
