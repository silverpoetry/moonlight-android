package com.limelight.binding.input;

import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/** Adapts Android controller touchpad events to the Moonlight protocol. */
final class AndroidControllerTouchpadAdapter {
    interface Target {
        short getControllerNumber();

        ControllerInputState getControllerInputState();

        InputDevice.MotionRange getTouchpadXRange();

        InputDevice.MotionRange getTouchpadYRange();

        InputDevice.MotionRange getTouchpadPressureRange();

        void sendControllerInput();
    }

    interface Output {
        int sendControllerTouch(
                byte controllerNumber,
                byte touchType,
                int pointerId,
                float x,
                float y,
                float pressure);
    }

    private final Output output;

    AndroidControllerTouchpadAdapter(Output output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    boolean tryHandle(
            MotionEvent event,
            Target target,
            boolean touchpadAsMouse) {
        int source = event.getSource();
        if (source != InputDevice.SOURCE_TOUCHPAD &&
                source != InputDevice.SOURCE_MOUSE) {
            return false;
        }

        if (source == InputDevice.SOURCE_MOUSE) {
            updateMouseSourceButton(
                    target,
                    event.getActionMasked());
            return !touchpadAsMouse;
        }

        ControllerTouchpadEventPolicy.Dispatch dispatch =
                ControllerTouchpadEventPolicy.resolve(
                        event.getActionMasked(),
                        event.getFlags(),
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.M
                                ? event.getActionButton()
                                : 0,
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.M);
        if (dispatch ==
                ControllerTouchpadEventPolicy.Dispatch.UNHANDLED) {
            return false;
        }
        if (dispatch ==
                ControllerTouchpadEventPolicy.Dispatch.BUTTON_DOWN ||
                dispatch ==
                        ControllerTouchpadEventPolicy.Dispatch.BUTTON_UP) {
            setTouchpadButton(
                    target,
                    dispatch ==
                            ControllerTouchpadEventPolicy.Dispatch.BUTTON_DOWN);
            // The clickpad remains a controller button even when contact
            // motion is delegated to Android's mouse path.
            return !touchpadAsMouse;
        }

        if (touchpadAsMouse) {
            return false;
        }

        InputDevice.MotionRange xRange = target.getTouchpadXRange();
        InputDevice.MotionRange yRange = target.getTouchpadYRange();
        if (xRange == null || yRange == null) {
            return false;
        }

        switch (dispatch) {
            case MOVE_ALL_CONTACTS:
                for (int pointerIndex = 0;
                     pointerIndex < event.getPointerCount();
                     pointerIndex++) {
                    if (!sendContact(
                            target,
                            event,
                            MoonBridge.LI_TOUCH_EVENT_MOVE,
                            pointerIndex)) {
                        return false;
                    }
                }
                return true;
            case CANCEL_ALL_CONTACTS:
                return output.sendControllerTouch(
                        (byte) target.getControllerNumber(),
                        MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                        0,
                        0,
                        0,
                        0) != MoonBridge.LI_ERR_UNSUPPORTED;
            case CONTACT_DOWN:
                return sendContact(
                        target,
                        event,
                        MoonBridge.LI_TOUCH_EVENT_DOWN,
                        event.getActionIndex());
            case CONTACT_UP:
                return sendContact(
                        target,
                        event,
                        MoonBridge.LI_TOUCH_EVENT_UP,
                        event.getActionIndex());
            case CONTACT_CANCEL:
                return sendContact(
                        target,
                        event,
                        MoonBridge.LI_TOUCH_EVENT_CANCEL,
                        event.getActionIndex());
            default:
                throw new AssertionError(
                        "Unhandled touchpad dispatch: " + dispatch);
        }
    }

    private static void updateMouseSourceButton(
            Target target,
            int actionMasked) {
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                setTouchpadButton(target, true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setTouchpadButton(target, false);
                break;
            default:
                break;
        }
    }

    private static void setTouchpadButton(
            Target target,
            boolean pressed) {
        target.getControllerInputState().setButtonMask(
                ControllerPacket.TOUCHPAD_FLAG,
                pressed);
        target.sendControllerInput();
    }

    private boolean sendContact(
            Target target,
            MotionEvent event,
            byte touchType,
            int pointerIndex) {
        InputDevice.MotionRange pressureRange =
                target.getTouchpadPressureRange();
        float pressure = pressureRange == null
                ? 0
                : normalize(
                        event.getPressure(pointerIndex),
                        pressureRange);
        return output.sendControllerTouch(
                (byte) target.getControllerNumber(),
                touchType,
                event.getPointerId(pointerIndex),
                normalize(
                        event.getX(pointerIndex),
                        target.getTouchpadXRange()),
                normalize(
                        event.getY(pointerIndex),
                        target.getTouchpadYRange()),
                pressure) != MoonBridge.LI_ERR_UNSUPPORTED;
    }

    private static float normalize(
            float value,
            InputDevice.MotionRange range) {
        return ControllerTouchpadNormalizer.normalize(
                value,
                range.getMin(),
                range.getRange());
    }
}
