package com.limelight.binding.input.driver;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class DualSenseAdaptiveTriggerCommandTest {
    @Test
    public void resistanceModeIsEncodedSymmetrically() {
        byte[] command = DualSenseAdaptiveTriggerCommand.create(
                1,
                230,
                10,
                40,
                100);

        assertHeader(command);
        assertEffect(command, 11, 1, 40, 230, 0);
        assertEffect(command, 22, 1, 40, 230, 0);
    }

    @Test
    public void triggerModeUsesStartEndAndStrength() {
        byte[] command = DualSenseAdaptiveTriggerCommand.create(
                2,
                200,
                10,
                30,
                150);

        assertEffect(command, 11, 2, 30, 150, 200);
        assertEffect(command, 22, 2, 30, 150, 200);
    }

    @Test
    public void automaticModeUsesFrequencyStrengthAndStart() {
        byte[] command = DualSenseAdaptiveTriggerCommand.create(
                6,
                255,
                12,
                20,
                100);

        assertEffect(command, 11, 6, 12, 255, 20);
        assertEffect(command, 22, 6, 12, 255, 20);
    }

    @Test
    public void unsupportedModeDisablesBothTriggers() {
        byte[] command = DualSenseAdaptiveTriggerCommand.create(
                99,
                255,
                255,
                255,
                255);

        assertEffect(command, 11, 0, 0, 0, 0);
        assertEffect(command, 22, 0, 0, 0, 0);
    }

    @Test
    public void numericParametersAreClampedToProtocolBytes() {
        byte[] command = DualSenseAdaptiveTriggerCommand.create(
                2,
                300,
                10,
                -1,
                500);

        assertEffect(command, 11, 2, 0, 255, 255);
    }

    private static void assertHeader(byte[] command) {
        assertEquals(48, command.length);
        assertEquals(0x02, Byte.toUnsignedInt(command[0]));
        assertEquals(0x0c, Byte.toUnsignedInt(command[1]));
        assertEquals(0xf7, Byte.toUnsignedInt(command[2]));
        assertEquals(0x10, Byte.toUnsignedInt(command[10]));
        assertArrayEquals(
                new byte[] {0x02, 0x00, 0x02, 0x00},
                new byte[] {
                        command[40],
                        command[41],
                        command[42],
                        command[43]
                });
        assertArrayEquals(
                new byte[] {0x78, 0x78, (byte) 0xef},
                new byte[] {
                        command[45],
                        command[46],
                        command[47]
                });
    }

    private static void assertEffect(
            byte[] command,
            int offset,
            int first,
            int second,
            int third,
            int fourth) {
        assertArrayEquals(
                new byte[] {
                        (byte) first,
                        (byte) second,
                        (byte) third,
                        (byte) fourth
                },
                new byte[] {
                        command[offset],
                        command[offset + 1],
                        command[offset + 2],
                        command[offset + 3]
                });
    }
}
