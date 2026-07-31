package com.limelight.binding.input;

import android.content.Context;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.view.InputDevice;

import com.limelight.LimeLog;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.settings.controller.ControllerSettings;

import java.util.Objects;

/** Android and USB adapter for the controller inventory at stream startup. */
public final class AndroidControllerInventory {
    private final InputManager inputManager;
    private final UsbManager usbManager;

    AndroidControllerInventory(
            InputManager inputManager,
            UsbManager usbManager) {
        this.inputManager = Objects.requireNonNull(
                inputManager,
                "inputManager");
        this.usbManager = usbManager;
    }

    public static AndroidControllerInventory from(Context context) {
        Objects.requireNonNull(context, "context");
        return new AndroidControllerInventory(
                (InputManager) context.getSystemService(
                        Context.INPUT_SERVICE),
                (UsbManager) context.getSystemService(
                        Context.USB_SERVICE));
    }

    public short getInitialControllerMask(ControllerSettings settings) {
        Objects.requireNonNull(settings, "settings");

        int attachedControllerCount = countInputDevices();
        if (settings.isUsbDriverEnabled()) {
            attachedControllerCount += countUsbDevices();
        }

        if (settings.isOnscreenControllerEnabled()) {
            LimeLog.info("Counting OSC gamepad");
        }
        LimeLog.info(
                "Enumerated " + attachedControllerCount +
                        " gamepads");
        return ControllerInventoryMask.fromAttachedDevices(
                attachedControllerCount,
                settings.isOnscreenControllerEnabled());
    }

    boolean isGameControllerDevice(
            InputDevice device,
            int sdkInt) {
        boolean deviceAbsent = device == null;
        boolean reportsGamepadInput = !deviceAbsent &&
                AndroidControllerInputCapabilities.isGamepad(device);
        boolean isAndroid11VirtualDevice = !deviceAbsent &&
                sdkInt == Build.VERSION_CODES.R &&
                device.getId() == -1;
        boolean hasAttachedGamepad =
                isAndroid11VirtualDevice && hasAttachedGamepad();
        boolean isAlphabeticKeyboard = !deviceAbsent &&
                device.getKeyboardType() ==
                        InputDevice.KEYBOARD_TYPE_ALPHABETIC;
        return ControllerDeviceClassificationPolicy.isGameController(
                deviceAbsent,
                reportsGamepadInput,
                isAndroid11VirtualDevice,
                hasAttachedGamepad,
                isAlphabeticKeyboard);
    }

    private boolean hasAttachedGamepad() {
        for (int deviceId : inputManager.getInputDeviceIds()) {
            InputDevice device = inputManager.getInputDevice(deviceId);
            if (device != null &&
                    AndroidControllerInputCapabilities.isGamepad(device)) {
                return true;
            }
        }
        return false;
    }

    private int countInputDevices() {
        int count = 0;
        for (int deviceId : inputManager.getInputDeviceIds()) {
            InputDevice device = inputManager.getInputDevice(deviceId);
            if (device == null ||
                    !AndroidControllerAxisProbe.hasJoystickAxes(device)) {
                continue;
            }

            LimeLog.info(
                    "Counting InputDevice: " + device.getName());
            count++;
        }
        return count;
    }

    private int countUsbDevices() {
        if (usbManager == null) {
            return 0;
        }

        int count = 0;
        for (UsbDevice device :
                usbManager.getDeviceList().values()) {
            // Do not reserve a second slot for a USB device already exposed
            // through Android's InputDevice API.
            if (!UsbDriverService.shouldClaimDevice(device, false) ||
                    UsbDriverService.isRecognizedInputDevice(device)) {
                continue;
            }

            LimeLog.info(
                    "Counting UsbDevice: " +
                            device.getDeviceName());
            count++;
        }
        return count;
    }
}
