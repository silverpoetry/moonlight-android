package com.limelight.binding.input;

/**
 * Immutable Android-compatible battery status and normalized capacity sample.
 */
final class ControllerBatterySample {
    private final int status;
    private final float capacity;

    ControllerBatterySample(int status, float capacity) {
        this.status = status;
        this.capacity = capacity;
    }

    int getStatus() {
        return status;
    }

    float getCapacity() {
        return capacity;
    }
}
