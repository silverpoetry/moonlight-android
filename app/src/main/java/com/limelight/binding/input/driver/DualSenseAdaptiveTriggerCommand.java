package com.limelight.binding.input.driver;

/** Encodes a symmetric DualSense L2/R2 adaptive-trigger output report. */
final class DualSenseAdaptiveTriggerCommand {
    private static final int REPORT_LENGTH = 48;
    private static final int RIGHT_EFFECT_OFFSET = 11;
    private static final int LEFT_EFFECT_OFFSET = 22;

    private DualSenseAdaptiveTriggerCommand() {
    }

    static byte[] create(
            int mode,
            int strength,
            int frequency,
            int start,
            int end) {
        byte[] effect = encodeEffect(
                mode,
                strength,
                frequency,
                start,
                end);
        byte[] report = new byte[REPORT_LENGTH];
        report[0] = 0x02;
        report[1] = 0x0c;
        report[2] = (byte) 0xf7;
        report[10] = 0x10;
        System.arraycopy(
                effect,
                0,
                report,
                RIGHT_EFFECT_OFFSET,
                effect.length);
        System.arraycopy(
                effect,
                0,
                report,
                LEFT_EFFECT_OFFSET,
                effect.length);
        report[40] = 0x02;
        report[42] = 0x02;
        report[45] = 0x78;
        report[46] = 0x78;
        report[47] = (byte) 0xef;
        return report;
    }

    private static byte[] encodeEffect(
            int mode,
            int strength,
            int frequency,
            int start,
            int end) {
        int boundedStrength = clampByte(strength);
        int boundedFrequency = clampByte(frequency);
        int boundedStart = clampByte(start);
        int boundedEnd = clampByte(end);
        switch (mode) {
            case 1:
                return new byte[] {
                        0x01,
                        (byte) boundedStart,
                        (byte) boundedStrength,
                        0x00
                };
            case 2:
                return new byte[] {
                        0x02,
                        (byte) boundedStart,
                        (byte) boundedEnd,
                        (byte) boundedStrength
                };
            case 6:
                return new byte[] {
                        0x06,
                        (byte) boundedFrequency,
                        (byte) boundedStrength,
                        (byte) boundedStart
                };
            default:
                return new byte[4];
        }
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(0xff, value));
    }
}
