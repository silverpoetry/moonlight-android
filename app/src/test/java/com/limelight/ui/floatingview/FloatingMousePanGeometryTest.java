package com.limelight.ui.floatingview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class FloatingMousePanGeometryTest {
    @Test public void reverseDragStopsPanningWhileStillInsideEitherEdgeZone() {
        assertEquals(0, FloatingMousePanGeometry.directionalVelocity(450, 2, 450), 0);
        assertEquals(0, FloatingMousePanGeometry.directionalVelocity(-450, -2, -450), 0);
        assertEquals(450, FloatingMousePanGeometry.directionalVelocity(450, -2, 0), 0);
        assertEquals(-450, FloatingMousePanGeometry.directionalVelocity(-450, 2, 0), 0);
    }

    @Test public void stationaryFingerOnlyContinuesAnAlreadyArmedEdge() {
        assertEquals(450, FloatingMousePanGeometry.directionalVelocity(450, 0, 450), 0);
        assertEquals(-450, FloatingMousePanGeometry.directionalVelocity(-450, 0, -450), 0);
        assertEquals(0, FloatingMousePanGeometry.directionalVelocity(450, 0, 0), 0);
        assertEquals(0, FloatingMousePanGeometry.directionalVelocity(-450, 0, 0), 0);
        assertEquals(0, FloatingMousePanGeometry.directionalVelocity(0, -2, 450), 0);
    }

    @Test public void edgeSpeedDependsOnPositionAndRemainsBounded() {
        assertEquals(900, FloatingMousePanGeometry.edgeVelocity(0, 800, 40, 900), 0);
        assertEquals(450, FloatingMousePanGeometry.edgeVelocity(20, 800, 40, 900), 0);
        assertEquals(0, FloatingMousePanGeometry.edgeVelocity(40, 800, 40, 900), 0);
        assertEquals(-900, FloatingMousePanGeometry.edgeVelocity(800, 800, 40, 900), 0);
        assertEquals(-450, FloatingMousePanGeometry.edgeVelocity(780, 800, 40, 900), 0);
        assertEquals(0, FloatingMousePanGeometry.edgeVelocity(0, 0, 40, 900), 0);
        assertEquals(0, FloatingMousePanGeometry.edgeVelocity(10, 20, 40, 900), 0);
    }

    @Test public void unscaledDesktopEdgesReachTheHotspotWithoutMovingTheControlOffscreen() {
        // 1000px viewport, 156px control and an 8px hotspot offset.
        float left = FloatingMousePanGeometry.clamp(5000, 0, 1000, 500, 1, 8, 852);
        float right = FloatingMousePanGeometry.clamp(-5000, 0, 1000, 500, 1, 8, 852);
        assertEquals(8, left, 0);
        assertEquals(852, 999 + right, 0);
    }

    @Test public void zoomedDesktopEdgesStillReachTheHotspot() {
        float left = FloatingMousePanGeometry.clamp(5000, 0, 1000, 500, 2, 8, 852);
        float right = FloatingMousePanGeometry.clamp(-5000, 0, 1000, 500, 2, 8, 852);
        assertEquals(8, -500 + left, 0);
        assertEquals(852, -500 + 999 * 2 + right, 0);
    }

    @Test public void letterboxedContentDoesNotJumpOrPanPastItsEdges() {
        assertEquals(0, FloatingMousePanGeometry.clamp(0, 200, 600, 300, 1, 8, 852), 0);
        assertEquals(0, FloatingMousePanGeometry.clamp(5000, 200, 600, 300, 1, 8, 852), 0);
        assertEquals(0, FloatingMousePanGeometry.clamp(-5000, 200, 600, 300, 1, 8, 852), 0);
    }

    @Test public void bottomEdgeIncludesTheControlsFullHeight() {
        float bottom = FloatingMousePanGeometry.clamp(-5000, 0, 800, 400, 1, 40, 632);
        assertEquals(632, 799 + bottom, 0);
    }
}
