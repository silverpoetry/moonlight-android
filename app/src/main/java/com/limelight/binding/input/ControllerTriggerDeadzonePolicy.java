package com.limelight.binding.input;

/** Resolves the initial trigger deadzone sampled from Android motion ranges. */
final class ControllerTriggerDeadzonePolicy {
    private static final float DEFAULT_DEADZONE = 0.13f;
    private static final float MAX_ACCEPTED_DEADZONE = 0.30f;

    private ControllerTriggerDeadzonePolicy() {
    }

    static float resolve(
            float leftFlat,
            float rightFlat,
            boolean correctionDisabled) {
        float sampled = Math.max(
                Math.abs(leftFlat),
                Math.abs(rightFlat));
        if (correctionDisabled) {
            return sampled;
        }
        return sampled < DEFAULT_DEADZONE ||
                sampled > MAX_ACCEPTED_DEADZONE
                ? DEFAULT_DEADZONE
                : sampled;
    }
}
