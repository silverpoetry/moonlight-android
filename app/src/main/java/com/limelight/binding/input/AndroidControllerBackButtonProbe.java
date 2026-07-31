package com.limelight.binding.input;

import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyEvent;

import java.util.Objects;

/** Samples Android device facts needed by {@link ControllerBackButtonPolicy}. */
final class AndroidControllerBackButtonProbe {
    private final InputManager inputManager;

    AndroidControllerBackButtonProbe(InputManager inputManager) {
        this.inputManager = Objects.requireNonNull(
                inputManager,
                "inputManager");
    }

    boolean shouldIgnore(InputDevice device, boolean external) {
        Objects.requireNonNull(device, "device");
        ControllerBackButtonPolicy.DeviceResolution resolution =
                ControllerBackButtonPolicy.resolveDevice(
                        device.getName(),
                        external,
                        AndroidControllerAxisProbe
                                .hasJoystickAxes(device),
                        AndroidControllerInputCapabilities
                                .hasGamepadButtons(device));
        switch (resolution) {
            case IGNORE_AS_CONTROLLER_INPUT:
                return true;
            case HANDLE_AS_CONTROLLER_INPUT:
                return false;
            case INSPECT_INTERNAL_INVENTORY:
                return shouldIgnoreFromInternalInventory();
            default:
                throw new AssertionError(
                        "Unhandled Back-button resolution: " +
                                resolution);
        }
    }

    private boolean shouldIgnoreFromInternalInventory() {
        boolean hasInternalGamepad = false;
        boolean hasInternalSelectButton = false;
        for (int id : inputManager.getInputDeviceIds()) {
            InputDevice device = inputManager.getInputDevice(id);
            if (device == null ||
                    AndroidInputDeviceClassifier.isExternal(device)) {
                continue;
            }

            if (device.hasKeys(
                    KeyEvent.KEYCODE_BUTTON_SELECT)[0]) {
                hasInternalSelectButton = true;
            }
            if (AndroidControllerInputCapabilities
                    .hasGamepadButtons(device)) {
                hasInternalGamepad = true;
            }
        }
        return ControllerBackButtonPolicy
                .shouldIgnoreForInternalInventory(
                        hasInternalGamepad,
                        hasInternalSelectButton);
    }
}
