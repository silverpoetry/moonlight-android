package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerRumbleAmplitudesTest {
    @Test
    public void dualMotorOrderCanBeFlipped() {
        short low = (short) 0x8000;
        short high = (short) 0x4000;

        assertArrayEquals(
                new int[]{0x40, 0x80},
                ControllerRumbleAmplitudes.dual(
                        low,
                        high,
                        false));
        assertArrayEquals(
                new int[]{0x80, 0x40},
                ControllerRumbleAmplitudes.dual(
                        low,
                        high,
                        true));
    }

    @Test
    public void quadMotorOrderPreservesTriggerChannels() {
        assertArrayEquals(
                new int[]{2, 1, 3, 4},
                ControllerRumbleAmplitudes.quad(
                        (short) 0x0100,
                        (short) 0x0200,
                        (short) 0x0300,
                        (short) 0x0400,
                        false));
    }

    @Test
    public void singleMotorMixIsWeightedAndCapped() {
        assertEquals(
                0,
                ControllerRumbleAmplitudes.single(
                        (short) 0,
                        (short) 0x0100));
        assertEquals(
                255,
                ControllerRumbleAmplitudes.single(
                        (short) 0xffff,
                        (short) 0xffff));
    }

    @Test
    public void fallbackScalingTreatsProtocolMotorAsUnsigned() {
        assertEquals(
                (short) 32767,
                ControllerRumbleAmplitudes
                        .scaleProtocolMotor(
                                (short) 0xffff,
                                50));
        assertEquals(
                (short) 0xfffe,
                ControllerRumbleAmplitudes
                        .scaleProtocolMotor(
                                (short) 0xffff,
                                200));
    }

    @Test
    public void zeroDetectionChecksEveryChannel() {
        assertTrue(
                ControllerRumbleAmplitudes.areAllZero(
                        new int[]{0, 0, 0, 0}));
        assertFalse(
                ControllerRumbleAmplitudes.areAllZero(
                        new int[]{0, 0, 1, 0}));
    }
}
