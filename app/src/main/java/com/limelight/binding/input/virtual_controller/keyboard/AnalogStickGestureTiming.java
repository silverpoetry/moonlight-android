package com.limelight.binding.input.virtual_controller.keyboard;

final class AnalogStickGestureTiming {
    private AnalogStickGestureTiming() {
    }

    static boolean isDoubleTap(
            long previousEventTimeMs,
            long currentEventTimeMs,
            long timeoutMs) {
        return previousEventTimeMs >= 0
                && currentEventTimeMs >= previousEventTimeMs
                && currentEventTimeMs - previousEventTimeMs <= timeoutMs;
    }
}
