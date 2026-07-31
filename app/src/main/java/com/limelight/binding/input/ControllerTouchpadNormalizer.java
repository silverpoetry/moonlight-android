package com.limelight.binding.input;

/** Normalizes a physical touchpad axis into the protocol's unit interval. */
final class ControllerTouchpadNormalizer {
    private ControllerTouchpadNormalizer() {
    }

    static float normalize(
            float value,
            float minimum,
            float range) {
        if (!Float.isFinite(value) ||
                !Float.isFinite(minimum) ||
                !Float.isFinite(range) ||
                range <= 0) {
            return 0;
        }
        float bounded = Math.max(
                minimum,
                Math.min(value, minimum + range));
        return (bounded - minimum) / range;
    }
}
