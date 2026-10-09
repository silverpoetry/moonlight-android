package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StreamZoomGeometryTest {
    @Test
    public void offCenterPinchKeepsTheSameContentUnderTheFingers() {
        float oldScale = 2f;
        float newScale = 3f;
        float left = 126f;
        float pivot = 500f;
        float oldTranslation = -80f;
        float previousFocus = 550f;
        float focus = 580f;
        float contentCoordinate = pivot +
                (previousFocus - left - pivot - oldTranslation) / oldScale;
        float translation = StreamZoomGeometry.translationAroundFocus(
                oldTranslation, left, pivot, previousFocus, focus, newScale / oldScale);
        float mappedFocus = left + pivot + translation +
                (contentCoordinate - pivot) * newScale;
        assertEquals(focus, mappedFocus, 0.001f);
    }

    @Test
    public void panningCannotMoveTheEnlargedImageOutOfTheViewport() {
        assertEquals(-500f, StreamZoomGeometry.clampTranslation(
                -900, 126, 1000, 500, 2, 126, 1126), 0f);
        assertEquals(500f, StreamZoomGeometry.clampTranslation(
                900, 126, 1000, 500, 2, 126, 1126), 0f);
    }

    @Test
    public void returningToOriginalScaleRestoresOriginalLayout() {
        assertEquals(0f, StreamZoomGeometry.clampTranslation(
                -900, 126, 1000, 500, 1, 126, 1126), 0f);
        assertEquals(1f, StreamZoomGeometry.clampScale(0.4f), 0f);
    }

    @Test
    public void letterboxedAxisRetainsAlignmentUntilItFillsTheViewport() {
        assertEquals(0f, StreamZoomGeometry.clampTranslation(
                200, 200, 600, 300, 1.2f, 0, 1000), 0f);
    }

    @Test
    public void scaleHasFiniteBounds() {
        assertEquals(15f, StreamZoomGeometry.clampScale(40f), 0f);
        assertEquals(1f, StreamZoomGeometry.clampScale(Float.NaN), 0f);
        assertEquals(1f, StreamZoomGeometry.clampScale(Float.POSITIVE_INFINITY), 0f);
    }
}
