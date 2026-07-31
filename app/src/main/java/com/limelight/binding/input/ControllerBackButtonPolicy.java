package com.limelight.binding.input;

import java.util.Locale;
import java.util.Objects;

/** Pure policy for deciding whether Android Back belongs to a controller. */
final class ControllerBackButtonPolicy {
    enum DeviceResolution {
        IGNORE_AS_CONTROLLER_INPUT,
        HANDLE_AS_CONTROLLER_INPUT,
        INSPECT_INTERNAL_INVENTORY
    }

    private ControllerBackButtonPolicy() {
    }

    static DeviceResolution resolveDevice(
            String deviceName,
            boolean external,
            boolean hasJoystickAxes,
            boolean hasGamepadButtons) {
        Objects.requireNonNull(deviceName, "deviceName");

        if (deviceName.contains("Razer Serval")) {
            return DeviceResolution.IGNORE_AS_CONTROLLER_INPUT;
        }
        if (!hasJoystickAxes &&
                deviceName.toLowerCase(Locale.ROOT)
                        .contains("remote")) {
            return DeviceResolution.IGNORE_AS_CONTROLLER_INPUT;
        }
        if (!external) {
            return DeviceResolution.INSPECT_INTERNAL_INVENTORY;
        }
        return !hasJoystickAxes && !hasGamepadButtons
                ? DeviceResolution.IGNORE_AS_CONTROLLER_INPUT
                : DeviceResolution.HANDLE_AS_CONTROLLER_INPUT;
    }

    static boolean shouldIgnoreForInternalInventory(
            boolean hasInternalGamepad,
            boolean hasInternalSelectButton) {
        return !hasInternalGamepad || hasInternalSelectButton;
    }
}
