package com.limelight.binding.input;

import android.view.InputDevice;

/** Shared Android source-bit capability queries for controller discovery. */
final class AndroidControllerInputCapabilities {
    private AndroidControllerInputCapabilities() {
    }

    static boolean hasGamepadButtons(InputDevice device) {
        return (device.getSources() & InputDevice.SOURCE_GAMEPAD) ==
                InputDevice.SOURCE_GAMEPAD;
    }

    static boolean isGamepad(InputDevice device) {
        return AndroidControllerAxisProbe.hasJoystickAxes(device) ||
                hasGamepadButtons(device);
    }
}
