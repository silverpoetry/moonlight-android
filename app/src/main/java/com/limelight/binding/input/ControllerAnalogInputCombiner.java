package com.limelight.binding.input;

/**
 * Combines analog channels from controller devices sharing a protocol slot.
 */
final class ControllerAnalogInputCombiner {
    private ControllerAnalogInputCombiner() {
    }

    static byte combineTrigger(byte current, byte candidate) {
        return Byte.toUnsignedInt(current) >
                Byte.toUnsignedInt(candidate)
                ? current
                : candidate;
    }

    static short combineAxis(short current, short candidate) {
        return Math.abs((int) current) >
                Math.abs((int) candidate)
                ? current
                : candidate;
    }
}
