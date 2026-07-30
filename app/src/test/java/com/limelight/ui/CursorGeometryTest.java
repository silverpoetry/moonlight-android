package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CursorGeometryTest {
    @Test
    public void referenceEndpointsMapInsideTargetEndpoints() {
        assertEquals(0f,
                CursorGeometry.mapReferenceCoordinate(0, 2560, 2400),
                0f);
        assertEquals(2399f,
                CursorGeometry.mapReferenceCoordinate(2559, 2560, 2400),
                0f);
    }

    @Test
    public void referenceCoordinateIsClampedBeforeMapping() {
        assertEquals(0f,
                CursorGeometry.mapReferenceCoordinate(-1, 2560, 2400),
                0f);
        assertEquals(2399f,
                CursorGeometry.mapReferenceCoordinate(3000, 2560, 2400),
                0f);
    }

    @Test
    public void invalidOrSinglePixelGeometryMapsToOrigin() {
        assertEquals(0f,
                CursorGeometry.mapReferenceCoordinate(1, 1, 2400),
                0f);
        assertEquals(0f,
                CursorGeometry.mapReferenceCoordinate(1, 2560, 1),
                0f);
    }
}
