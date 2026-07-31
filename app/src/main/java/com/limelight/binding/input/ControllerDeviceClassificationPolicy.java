package com.limelight.binding.input;

/** Pure policy for routing an Android input device to controller handling. */
final class ControllerDeviceClassificationPolicy {
    private ControllerDeviceClassificationPolicy() {
    }

    static boolean isGameController(
            boolean deviceAbsent,
            boolean reportsGamepadInput,
            boolean isAndroid11VirtualDevice,
            boolean hasAttachedGamepad,
            boolean isAlphabeticKeyboard) {
        if (deviceAbsent || reportsGamepadInput) {
            return true;
        }
        if (isAndroid11VirtualDevice && hasAttachedGamepad) {
            return true;
        }
        return !isAlphabeticKeyboard;
    }
}
