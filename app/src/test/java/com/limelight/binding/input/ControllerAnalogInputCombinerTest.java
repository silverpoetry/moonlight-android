package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerAnalogInputCombinerTest {
    @Test
    public void triggerComparisonUsesUnsignedMagnitude() {
        assertEquals(
                (byte) 0xff,
                ControllerAnalogInputCombiner.combineTrigger(
                        (byte) 0x7f,
                        (byte) 0xff));
        assertEquals(
                (byte) 0xc0,
                ControllerAnalogInputCombiner.combineTrigger(
                        (byte) 0xc0,
                        (byte) 0x80));
    }

    @Test
    public void stickAxisUsesLargestSignedMagnitude() {
        assertEquals(
                -20_000,
                ControllerAnalogInputCombiner.combineAxis(
                        (short) 10_000,
                        (short) -20_000));
        assertEquals(
                Short.MIN_VALUE,
                ControllerAnalogInputCombiner.combineAxis(
                        Short.MAX_VALUE,
                        Short.MIN_VALUE));
    }

    @Test
    public void equalMagnitudeUsesMostRecentCandidate() {
        assertEquals(
                -100,
                ControllerAnalogInputCombiner.combineAxis(
                        (short) 100,
                        (short) -100));
        assertEquals(
                (byte) 10,
                ControllerAnalogInputCombiner.combineTrigger(
                        (byte) 10,
                        (byte) 10));
    }
}
