package com.limelight.binding.input;

/**
 * Pure conversion from protocol motor samples to Android amplitudes.
 */
final class ControllerRumbleAmplitudes {
    private static final int MAX_AMPLITUDE = 255;
    private static final int MAX_PROTOCOL_MOTOR =
            Short.MAX_VALUE * 2;

    private ControllerRumbleAmplitudes() {
    }

    static int[] dual(
            short lowFrequencyMotor,
            short highFrequencyMotor,
            boolean flipped) {
        int low = toAndroidAmplitude(
                lowFrequencyMotor);
        int high = toAndroidAmplitude(
                highFrequencyMotor);
        return flipped
                ? new int[]{low, high}
                : new int[]{high, low};
    }

    static int[] quad(
            short lowFrequencyMotor,
            short highFrequencyMotor,
            short leftTriggerMotor,
            short rightTriggerMotor,
            boolean flipped) {
        int[] amplitudes = new int[4];
        int low = toAndroidAmplitude(
                lowFrequencyMotor);
        int high = toAndroidAmplitude(
                highFrequencyMotor);
        amplitudes[0] = flipped ? low : high;
        amplitudes[1] = flipped ? high : low;
        amplitudes[2] = toAndroidAmplitude(
                leftTriggerMotor);
        amplitudes[3] = toAndroidAmplitude(
                rightTriggerMotor);
        return amplitudes;
    }

    static int single(
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        int low = toAndroidAmplitude(
                lowFrequencyMotor);
        int high = toAndroidAmplitude(
                highFrequencyMotor);
        return Math.min(
                MAX_AMPLITUDE,
                (int) (
                        low * 0.80 +
                                high * 0.33));
    }

    static short scaleProtocolMotor(
            short motor,
            int strengthPercent) {
        int scaled =
                ((motor & 0xffff) *
                        strengthPercent) /
                        100;
        return (short) Math.min(
                scaled,
                MAX_PROTOCOL_MOTOR);
    }

    static boolean areAllZero(int[] amplitudes) {
        for (int amplitude : amplitudes) {
            if (amplitude != 0) {
                return false;
            }
        }
        return true;
    }

    private static int toAndroidAmplitude(short motor) {
        return (motor >> 8) & 0xff;
    }
}
