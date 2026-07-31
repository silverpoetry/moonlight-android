package com.limelight.binding.input;

/**
 * Selects how a controller context obtains its protocol player slot.
 *
 * <p>This policy contains no Android device access or allocator mutation. The
 * caller remains responsible for applying the returned strategy exactly once
 * to the context's {@link ControllerSlotLease}.</p>
 */
final class ControllerAssignmentPolicy {
    enum Strategy {
        FIXED_PLAYER_ONE,
        RESERVE_NEXT,
        FIND_ASSOCIATED_JOYSTICK
    }

    private ControllerAssignmentPolicy() {
    }

    static Strategy forInputDevice(
            boolean external,
            boolean hasJoystickAxes,
            boolean multiControllerEnabled) {
        if (!external) {
            return Strategy.FIXED_PLAYER_ONE;
        }
        if (multiControllerEnabled && hasJoystickAxes) {
            return Strategy.RESERVE_NEXT;
        }
        if (!hasJoystickAxes) {
            return Strategy.FIND_ASSOCIATED_JOYSTICK;
        }
        return Strategy.FIXED_PLAYER_ONE;
    }

    static Strategy forUsbController(
            boolean multiControllerEnabled) {
        return multiControllerEnabled
                ? Strategy.RESERVE_NEXT
                : Strategy.FIXED_PLAYER_ONE;
    }
}
