package com.limelight.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamLayoutGeometryTest {
    @Test
    public void landscapeStreamFitsPillarboxedContainer() {
        assertEquals(
                new StreamLayoutGeometry.Size(1920, 1080),
                StreamLayoutGeometry.fitWithin(
                        2400,
                        1080,
                        16.0 / 9.0));
    }

    @Test
    public void portraitStreamFitsLetterboxedContainer() {
        assertEquals(
                new StreamLayoutGeometry.Size(607, 1080),
                StreamLayoutGeometry.fitWithin(
                        2400,
                        1080,
                        9.0 / 16.0));
    }

    @Test
    public void exactAspectUsesEntireContainer() {
        assertEquals(
                new StreamLayoutGeometry.Size(1920, 1080),
                StreamLayoutGeometry.fitWithin(
                        1920,
                        1080,
                        16.0 / 9.0));
    }

    @Test
    public void legacyDisplayCompatibilityPreservesToleranceSemantics() {
        assertTrue(StreamLayoutGeometry.hasCompatibleAspectRatio(
                2560,
                1440,
                1920,
                1080,
                0.001));
        assertFalse(StreamLayoutGeometry.hasCompatibleAspectRatio(
                2400,
                1080,
                1920,
                1080,
                0.001));
        assertFalse(StreamLayoutGeometry.hasCompatibleAspectRatio(
                0,
                1080,
                1920,
                1080,
                0.001));
    }

    @Test
    public void fixedOutputHonorsMinimumWidthAndEvenDimensions() {
        assertEquals(
                new StreamLayoutGeometry.Size(2560, 1440),
                StreamLayoutGeometry.getEvenOutputSize(
                        1920,
                        1080,
                        1440,
                        2560));
        assertEquals(
                new StreamLayoutGeometry.Size(2880, 1440),
                StreamLayoutGeometry.getEvenOutputSize(
                        2001,
                        1000,
                        1440,
                        2560));
    }

    @Test
    public void invalidSourceUsesStableSixteenByNineFallback() {
        assertEquals(
                new StreamLayoutGeometry.Size(3840, 2160),
                StreamLayoutGeometry.getEvenOutputSize(
                        0,
                        0,
                        2160,
                        3840));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAspectRatioIsRejected() {
        StreamLayoutGeometry.fitWithin(1920, 1080, 0);
    }
}
