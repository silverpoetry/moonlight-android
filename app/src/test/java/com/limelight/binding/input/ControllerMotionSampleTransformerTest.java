package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerMotionSampleTransformerTest {
    private static final float DELTA = 0.0001f;

    @Test
    public void firstZeroSampleIsNotMistakenForDuplicate() {
        ControllerMotionSampleTransformer transformer =
                new ControllerMotionSampleTransformer();

        assertTrue(
                transformer.update(
                        0,
                        0,
                        0,
                        ControllerMotionSampleTransformer.ROTATION_0,
                        false,
                        false));
        assertFalse(
                transformer.update(
                        0,
                        0,
                        0,
                        ControllerMotionSampleTransformer.ROTATION_0,
                        false,
                        false));
    }

    @Test
    public void unchangedRawSampleIsSuppressedBeforeTransformation() {
        ControllerMotionSampleTransformer transformer =
                new ControllerMotionSampleTransformer();
        transformer.update(
                1,
                2,
                3,
                ControllerMotionSampleTransformer.ROTATION_0,
                true,
                false);

        assertFalse(
                transformer.update(
                        1,
                        2,
                        3,
                        ControllerMotionSampleTransformer.ROTATION_270,
                        true,
                        false));
    }

    @Test
    public void deviceCoordinatesMatchAllDisplayRotations() {
        assertTransformed(
                ControllerMotionSampleTransformer.ROTATION_0,
                1,
                3,
                -2);
        assertTransformed(
                ControllerMotionSampleTransformer.ROTATION_90,
                -2,
                3,
                -1);
        assertTransformed(
                ControllerMotionSampleTransformer.ROTATION_180,
                -1,
                3,
                2);
        assertTransformed(
                ControllerMotionSampleTransformer.ROTATION_270,
                2,
                3,
                1);
    }

    @Test
    public void controllerSensorCoordinatesArePassedThrough() {
        ControllerMotionSampleTransformer transformer =
                new ControllerMotionSampleTransformer();

        transformer.update(
                1,
                2,
                3,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                false);

        assertOutput(transformer, 1, 2, 3);
        assertEquals(1, transformer.getRawX(), DELTA);
        assertEquals(2, transformer.getRawY(), DELTA);
        assertEquals(3, transformer.getRawZ(), DELTA);
    }

    @Test
    public void gyroscopeRadiansAreConvertedToDegrees() {
        ControllerMotionSampleTransformer transformer =
                new ControllerMotionSampleTransformer();

        transformer.update(
                (float) Math.PI,
                0,
                0,
                ControllerMotionSampleTransformer.ROTATION_0,
                false,
                true);

        assertEquals(180, transformer.getTransformedX(), 0.001f);
        assertEquals(0, transformer.getTransformedY(), DELTA);
        assertEquals(0, transformer.getTransformedZ(), DELTA);
    }

    private static void assertTransformed(
            int rotation,
            float expectedX,
            float expectedY,
            float expectedZ) {
        ControllerMotionSampleTransformer transformer =
                new ControllerMotionSampleTransformer();
        transformer.update(1, 2, 3, rotation, true, false);
        assertOutput(
                transformer,
                expectedX,
                expectedY,
                expectedZ);
    }

    private static void assertOutput(
            ControllerMotionSampleTransformer transformer,
            float expectedX,
            float expectedY,
            float expectedZ) {
        assertEquals(
                expectedX,
                transformer.getTransformedX(),
                DELTA);
        assertEquals(
                expectedY,
                transformer.getTransformedY(),
                DELTA);
        assertEquals(
                expectedZ,
                transformer.getTransformedZ(),
                DELTA);
    }
}
