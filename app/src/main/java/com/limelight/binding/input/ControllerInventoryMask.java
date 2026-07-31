package com.limelight.binding.input;

/** Builds the protocol's initial controller reservation mask. */
final class ControllerInventoryMask {
    private ControllerInventoryMask() {
    }

    static short fromAttachedDevices(
            int attachedControllerCount,
            boolean onscreenControllerEnabled) {
        int boundedCount = Math.max(
                0,
                Math.min(
                        attachedControllerCount,
                        ControllerSlotAllocator.MAX_SLOTS));
        int mask = boundedCount == 0
                ? 0
                : (1 << boundedCount) - 1;
        if (onscreenControllerEnabled) {
            mask |= 1;
        }
        return (short) mask;
    }
}
