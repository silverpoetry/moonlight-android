package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerGyroStickTranslatorTest {
    @Test
    public void landscapeUsesNativeAxisOrder() {
        ControllerGyroStickTranslator translator =
                new ControllerGyroStickTranslator();

        translator.update(
                0.5f,
                0.25f,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);

        assertEquals(-4665, translator.getRightStickX());
        assertEquals(-1637, translator.getRightStickY());
    }

    @Test
    public void portraitAndAxisSwapProduceDeterministicOrder() {
        ControllerGyroStickTranslator portrait =
                new ControllerGyroStickTranslator();
        ControllerGyroStickTranslator swappedLandscape =
                new ControllerGyroStickTranslator();

        portrait.update(
                0.5f,
                0.25f,
                ControllerMotionSampleTransformer.ROTATION_0,
                false,
                100);
        swappedLandscape.update(
                0.5f,
                0.25f,
                ControllerMotionSampleTransformer.ROTATION_90,
                true,
                100);

        assertEquals(
                portrait.getRightStickX(),
                swappedLandscape.getRightStickX());
        assertEquals(
                portrait.getRightStickY(),
                swappedLandscape.getRightStickY());
    }

    @Test
    public void zeroSensitivityProducesNoOutput() {
        ControllerGyroStickTranslator translator =
                new ControllerGyroStickTranslator();

        translator.update(
                1,
                -1,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                0);

        assertEquals(0, translator.getRightStickX());
        assertEquals(0, translator.getRightStickY());
    }

    @Test
    public void extremeSamplesAreClampedToProtocolRange() {
        ControllerGyroStickTranslator translator =
                new ControllerGyroStickTranslator();

        translator.update(
                100,
                -100,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                200);

        assertEquals(-Short.MAX_VALUE, translator.getRightStickX());
        assertEquals(Short.MAX_VALUE, translator.getRightStickY());
    }

    @Test
    public void resetClearsFilterAndOutput() {
        ControllerGyroStickTranslator translator =
                new ControllerGyroStickTranslator();
        translator.update(
                1,
                1,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);

        translator.reset();

        assertEquals(0, translator.getRightStickX());
        assertEquals(0, translator.getRightStickY());
    }

    @Test
    public void restorationPreservesFilterHistory() {
        ControllerGyroStickTranslator previous =
                new ControllerGyroStickTranslator();
        previous.update(
                0.5f,
                -0.25f,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);
        ControllerGyroStickTranslator restored =
                new ControllerGyroStickTranslator();
        restored.restoreFrom(previous);

        previous.update(
                0.75f,
                0.1f,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);
        restored.update(
                0.75f,
                0.1f,
                ControllerMotionSampleTransformer.ROTATION_90,
                false,
                100);

        assertEquals(
                previous.getRightStickX(),
                restored.getRightStickX());
        assertEquals(
                previous.getRightStickY(),
                restored.getRightStickY());
    }
}
