package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StreamViewportGeometryTest {
    @Test
    public void pixelEndpointsMapToPixelEndpoints() {
        assertEquals(
                0f,
                StreamViewportGeometry.mapPixelCoordinate(
                        0, 2560, 2400),
                0f);
        assertEquals(
                2399f,
                StreamViewportGeometry.mapPixelCoordinate(
                        2559, 2560, 2400),
                0f);
    }

    @Test
    public void pixelCoordinateIsClampedBeforeMapping() {
        assertEquals(
                0f,
                StreamViewportGeometry.mapPixelCoordinate(
                        -1, 2560, 2400),
                0f);
        assertEquals(
                2399f,
                StreamViewportGeometry.mapPixelCoordinate(
                        3000, 2560, 2400),
                0f);
    }

    @Test
    public void invalidPixelGeometryMapsToOrigin() {
        assertEquals(
                0f,
                StreamViewportGeometry.mapPixelCoordinate(
                        1, 1, 2400),
                0f);
        assertEquals(
                0f,
                StreamViewportGeometry.mapPixelCoordinate(
                        1, 2560, 1),
                0f);
    }

    @Test
    public void dimensionsUseSizeRatioAndCaptureScale() {
        assertEquals(
                0.5f,
                StreamViewportGeometry.mapScaledDimension(
                        65536,
                        2560,
                        1280),
                0f);
        assertEquals(
                1f,
                StreamViewportGeometry.mapScaledDimension(
                        131072,
                        2560,
                        1280),
                0f);
    }

    @Test
    public void missingCaptureScaleMeansIdentity() {
        assertEquals(
                0.5f,
                StreamViewportGeometry.mapScaledDimension(
                        0,
                        2560,
                        1280),
                0f);
        assertEquals(
                1f,
                StreamViewportGeometry.mapScaledDimension(
                        65536,
                        0,
                        1280),
                0f);
    }
}
