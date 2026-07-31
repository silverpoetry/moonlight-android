package com.limelight.binding.input;

import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Objects;

/** Produces one immutable input-device profile during context construction. */
final class AndroidControllerDeviceProfileProbe {
    private static final int SONY_VENDOR_ID = 0x054c;

    interface IdentityCapability {
        boolean isSupported(int vendorId, int productId);
    }

    private final IdentityCapability paddleCapability;
    private final IdentityCapability shareCapability;

    AndroidControllerDeviceProfileProbe(
            IdentityCapability paddleCapability,
            IdentityCapability shareCapability) {
        this.paddleCapability = Objects.requireNonNull(
                paddleCapability,
                "paddleCapability");
        this.shareCapability = Objects.requireNonNull(
                shareCapability,
                "shareCapability");
    }

    AndroidControllerDeviceProfile probe(
            InputDevice device,
            boolean external,
            boolean ignoreBack,
            float stickDeadzone,
            boolean triggerDeadzoneCorrectionDisabled) {
        Objects.requireNonNull(device, "device");
        String name = Objects.requireNonNull(
                device.getName(),
                "device.name");
        int vendorId = device.getVendorId();
        int productId = device.getProductId();
        boolean hasPaddles = paddleCapability.isSupported(
                vendorId,
                productId);
        boolean hasShareButton = shareCapability.isSupported(
                vendorId,
                productId);

        boolean[] modeSelectAndBack = device.hasKeys(
                KeyEvent.KEYCODE_BUTTON_MODE,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BACK,
                0);
        boolean hasModeButton = modeSelectAndBack[0];
        boolean hasSelectButton =
                modeSelectAndBack[1] || modeSelectAndBack[2];

        boolean standaloneDualShockTouchpad =
                vendorId == SONY_VENDOR_ID &&
                        (name.endsWith(" Touchpad") ||
                                name.startsWith("DualSense")) &&
                        device.getSources() ==
                                (InputDevice.SOURCE_KEYBOARD |
                                        InputDevice.SOURCE_MOUSE);
        InputDevice.MotionRange gasRange =
                AndroidControllerAxisProbe.getMotionRange(
                        device,
                        MotionEvent.AXIS_GAS);
        ControllerAxisProfile axisProfile =
                AndroidControllerAxisProbe.probe(device);
        if (axisProfile.isNonStandardDualShock4()) {
            hasSelectButton = true;
            hasModeButton = true;
        }

        int leftStickX = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getLeftStickX());
        int leftStickY = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getLeftStickY());
        int rightStickX = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getRightStickX());
        int rightStickY = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getRightStickY());
        float leftStickDeadzone =
                leftStickX != -1 && leftStickY != -1
                        ? stickDeadzone
                        : 0.f;
        float rightStickDeadzone =
                rightStickX != -1 && rightStickY != -1
                        ? stickDeadzone
                        : 0.f;

        int leftTrigger = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getLeftTrigger());
        int rightTrigger = AndroidControllerAxisProbe.toAndroidAxis(
                axisProfile.getRightTrigger());
        float triggerDeadzone = 0.f;
        if (leftTrigger != -1 && rightTrigger != -1) {
            InputDevice.MotionRange leftRange = Objects.requireNonNull(
                    AndroidControllerAxisProbe.getMotionRange(
                            device,
                            leftTrigger),
                    "leftTriggerRange");
            InputDevice.MotionRange rightRange = Objects.requireNonNull(
                    AndroidControllerAxisProbe.getMotionRange(
                            device,
                            rightTrigger),
                    "rightTriggerRange");
            triggerDeadzone = ControllerTriggerDeadzonePolicy.resolve(
                    leftRange.getFlat(),
                    rightRange.getFlat(),
                    triggerDeadzoneCorrectionDisabled);
        }

        boolean hasStartOrMenu = false;
        if (name.contains("ASUS Gamepad")) {
            boolean[] startAndMenu = device.hasKeys(
                    KeyEvent.KEYCODE_BUTTON_START,
                    KeyEvent.KEYCODE_MENU,
                    0);
            hasStartOrMenu = startAndMenu[0] || startAndMenu[1];
        }
        ControllerDeviceQuirks quirks = ControllerDeviceQuirks.resolve(
                ControllerDeviceQuirks.Facts.builder(
                                vendorId,
                                productId)
                        .deviceName(name)
                        .hasMode(hasModeButton)
                        .hasSelect(hasSelectButton)
                        .hasStartOrMenu(hasStartOrMenu)
                        .hasGasAxis(gasRange != null)
                        .triggerDeadzone(triggerDeadzone)
                        .build());
        hasModeButton = quirks.hasMode();
        hasSelectButton = quirks.hasSelect();
        triggerDeadzone = quirks.getTriggerDeadzone();

        ControllerButtonMapper buttonMapper =
                ControllerButtonMapper.builder(
                                vendorId,
                                productId,
                                Build.VERSION.SDK_INT)
                        .ignoreBack(ignoreBack)
                        .hasShare(hasShareButton)
                        .dualShockStandaloneTouchpad(
                                standaloneDualShockTouchpad)
                        .linuxStandardFaceButtons(
                                axisProfile
                                        .hasLinuxStandardFaceButtons())
                        .nonStandardDualShock4(
                                axisProfile
                                        .isNonStandardDualShock4())
                        .serval(quirks.isServal())
                        .nonStandardXboxBluetooth(
                                quirks
                                        .isNonStandardXboxBluetooth())
                        .searchIsMode(quirks.isSearchMode())
                        .hasHatAxes(
                                axisProfile.getHatX() !=
                                        ControllerAxisProfile.Axis.NONE ||
                                        axisProfile.getHatY() !=
                                                ControllerAxisProfile.Axis.NONE)
                        .build();

        return AndroidControllerDeviceProfile.builder(
                        device.getId(),
                        vendorId,
                        productId,
                        name,
                        axisProfile,
                        buttonMapper)
                .external(external)
                .capabilities(
                        hasPaddles,
                        hasShareButton,
                        hasModeButton,
                        hasSelectButton)
                .touchpadRanges(
                        device.getMotionRange(
                                MotionEvent.AXIS_X,
                                InputDevice.SOURCE_TOUCHPAD),
                        device.getMotionRange(
                                MotionEvent.AXIS_Y,
                                InputDevice.SOURCE_TOUCHPAD),
                        device.getMotionRange(
                                MotionEvent.AXIS_PRESSURE,
                                InputDevice.SOURCE_TOUCHPAD))
                .deadzones(
                        leftStickDeadzone,
                        rightStickDeadzone,
                        triggerDeadzone)
                .buttonFallbacks(
                        quirks.isBackStart(),
                        quirks.isModeSelect())
                .build();
    }
}
