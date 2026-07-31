package com.limelight.binding.input;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

/** Samples Android joystick/gamepad axes into the pure controller profile. */
final class AndroidControllerAxisProbe {
    private static final int SONY_VENDOR_ID = 0x054c;

    private AndroidControllerAxisProbe() {
    }

    static ControllerAxisProfile probe(InputDevice device) {
        boolean hasRxAndRy = hasAxisPair(
                device,
                MotionEvent.AXIS_RX,
                MotionEvent.AXIS_RY);
        String deviceName = device.getName();
        boolean hasSonyButtonC =
                device.getVendorId() == SONY_VENDOR_ID &&
                        deviceName != null &&
                        hasRxAndRy &&
                        device.hasKeys(
                                KeyEvent.KEYCODE_BUTTON_C)[0];
        return ControllerAxisProfile.resolve(
                device.getVendorId(),
                deviceName != null,
                hasSonyButtonC,
                ControllerAxisProfile.Capabilities.builder()
                        .xAndY(hasAxisPair(
                                device,
                                MotionEvent.AXIS_X,
                                MotionEvent.AXIS_Y))
                        .leftTriggerAndRightTrigger(hasAxisPair(
                                device,
                                MotionEvent.AXIS_LTRIGGER,
                                MotionEvent.AXIS_RTRIGGER))
                        .brakeAndGas(hasAxisPair(
                                device,
                                MotionEvent.AXIS_BRAKE,
                                MotionEvent.AXIS_GAS))
                        .brakeAndThrottle(hasAxisPair(
                                device,
                                MotionEvent.AXIS_BRAKE,
                                MotionEvent.AXIS_THROTTLE))
                        .rxAndRy(hasRxAndRy)
                        .zAndRz(hasAxisPair(
                                device,
                                MotionEvent.AXIS_Z,
                                MotionEvent.AXIS_RZ))
                        .hatXAndHatY(hasAxisPair(
                                device,
                                MotionEvent.AXIS_HAT_X,
                                MotionEvent.AXIS_HAT_Y))
                        .build());
    }

    static boolean hasJoystickAxes(InputDevice device) {
        return (device.getSources() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                getMotionRange(device, MotionEvent.AXIS_X) != null &&
                getMotionRange(device, MotionEvent.AXIS_Y) != null;
    }

    static boolean hasAxisPair(
            InputDevice device,
            int firstAxis,
            int secondAxis) {
        return getMotionRange(device, firstAxis) != null &&
                getMotionRange(device, secondAxis) != null;
    }

    @Nullable
    static InputDevice.MotionRange getMotionRange(
            InputDevice device,
            int axis) {
        InputDevice.MotionRange range = device.getMotionRange(
                axis,
                InputDevice.SOURCE_JOYSTICK);
        return range != null
                ? range
                : device.getMotionRange(
                        axis,
                        InputDevice.SOURCE_GAMEPAD);
    }

    static int toAndroidAxis(ControllerAxisProfile.Axis axis) {
        switch (axis) {
            case X:
                return MotionEvent.AXIS_X;
            case Y:
                return MotionEvent.AXIS_Y;
            case Z:
                return MotionEvent.AXIS_Z;
            case RZ:
                return MotionEvent.AXIS_RZ;
            case RX:
                return MotionEvent.AXIS_RX;
            case RY:
                return MotionEvent.AXIS_RY;
            case LEFT_TRIGGER:
                return MotionEvent.AXIS_LTRIGGER;
            case RIGHT_TRIGGER:
                return MotionEvent.AXIS_RTRIGGER;
            case BRAKE:
                return MotionEvent.AXIS_BRAKE;
            case GAS:
                return MotionEvent.AXIS_GAS;
            case THROTTLE:
                return MotionEvent.AXIS_THROTTLE;
            case HAT_X:
                return MotionEvent.AXIS_HAT_X;
            case HAT_Y:
                return MotionEvent.AXIS_HAT_Y;
            case NONE:
            default:
                return -1;
        }
    }
}
