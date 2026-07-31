package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.utils.Vector2d;

/**
 * Stateful gamepad-to-desktop input mapping used by controller mouse mode.
 */
final class ControllerMouseEmulationTranslator {
    interface Output {
        void sendMouseButton(byte button, boolean down);

        void sendKey(int keyCode, byte action);

        void sendChord(short[] keyCodes);

        void sendMouseMove(short deltaX, short deltaY);

        void sendHighResolutionScroll(
                short verticalAmount,
                short horizontalAmount);

        void sendDiscreteScroll(byte amount);
    }

    private static final short[] SHOW_KEYBOARD_CHORD = {
            KeyboardTranslator.VK_LWIN,
            KeyboardTranslator.VK_LCONTROL,
            KeyboardTranslator.VK_O
    };
    private static final short[] SHOW_DESKTOP_CHORD = {
            KeyboardTranslator.VK_LWIN,
            KeyboardTranslator.VK_D
    };

    private static final float STICK_NORMALIZATION_SCALE =
            1.0f / 32766.0f;
    private static final float BASE_MOUSE_SPEED = 4.0f;
    private static final float PERCENT_SCALE = 0.01f;

    private final Vector2d translatedStick = new Vector2d();
    private int previousInputMap;

    void restoreFrom(
            ControllerMouseEmulationTranslator previousTranslator) {
        previousInputMap =
                previousTranslator.previousInputMap;
    }

    void translate(int inputMap, Output output) {
        int changedMask = inputMap ^ previousInputMap;
        previousInputMap = inputMap;

        sendMouseButtonChange(
                inputMap,
                changedMask,
                ControllerPacket.A_FLAG,
                MouseButtonPacket.BUTTON_LEFT,
                output);
        sendMouseButtonChange(
                inputMap,
                changedMask,
                ControllerPacket.B_FLAG,
                MouseButtonPacket.BUTTON_RIGHT,
                output);

        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.UP_FLAG,
                KeyboardTranslator.VK_UP,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.DOWN_FLAG,
                KeyboardTranslator.VK_DOWN,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.RIGHT_FLAG,
                KeyboardTranslator.VK_RIGHT,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.LEFT_FLAG,
                KeyboardTranslator.VK_LEFT,
                output);

        sendChordOnPress(
                inputMap,
                changedMask,
                ControllerPacket.LS_CLK_FLAG,
                SHOW_KEYBOARD_CHORD,
                output);
        sendChordOnPress(
                inputMap,
                changedMask,
                ControllerPacket.RS_CLK_FLAG,
                SHOW_DESKTOP_CHORD,
                output);

        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.X_FLAG,
                KeyboardTranslator.VK_ESCAPE,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.Y_FLAG,
                KeyboardTranslator.VK_RETURN,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.SPECIAL_BUTTON_FLAG,
                KeyboardTranslator.VK_LWIN,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.LB_FLAG,
                KeyboardTranslator.VK_LMENU,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.RB_FLAG,
                KeyboardTranslator.VK_TAB,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.PLAY_FLAG,
                KeyboardTranslator.VK_BACK_SPACE,
                output);
        sendKeyChange(
                inputMap,
                changedMask,
                ControllerPacket.BACK_FLAG,
                KeyboardTranslator.VK_SPACE,
                output);
    }

    void translateMotion(
            short leftStickX,
            short leftStickY,
            short rightStickX,
            short rightStickY,
            int leftTrigger,
            int rightTrigger,
            int sensitivityPercent,
            ControllerSettings.AnalogStickForScrolling scrollStick,
            Output output) {
        if (scrollStick ==
                ControllerSettings.AnalogStickForScrolling.RIGHT) {
            sendMouseMove(
                    leftStickX,
                    leftStickY,
                    sensitivityPercent,
                    output);
            sendMouseScroll(
                    rightStickX,
                    rightStickY,
                    sensitivityPercent,
                    output);
        }
        else if (scrollStick ==
                ControllerSettings.AnalogStickForScrolling.LEFT) {
            sendMouseMove(
                    rightStickX,
                    rightStickY,
                    sensitivityPercent,
                    output);
            sendMouseScroll(
                    leftStickX,
                    leftStickY,
                    sensitivityPercent,
                    output);
        }
        else {
            sendMouseMove(
                    leftStickX,
                    leftStickY,
                    sensitivityPercent,
                    output);
            sendMouseMove(
                    rightStickX,
                    rightStickY,
                    sensitivityPercent,
                    output);
        }

        // Trigger scrolling intentionally repeats once per scheduled report
        // while held, matching the existing controller mouse behavior.
        if (leftTrigger > 0) {
            output.sendDiscreteScroll((byte) 1);
        }
        if (rightTrigger > 0) {
            output.sendDiscreteScroll((byte) -1);
        }
    }

    private void sendMouseMove(
            short stickX,
            short stickY,
            int sensitivityPercent,
            Output output) {
        if (!translateStick(
                stickX,
                stickY,
                sensitivityPercent)) {
            return;
        }
        output.sendMouseMove(
                (short) translatedStick.getX(),
                (short) -translatedStick.getY());
    }

    private void sendMouseScroll(
            short stickX,
            short stickY,
            int sensitivityPercent,
            Output output) {
        if (!translateStick(
                stickX,
                stickY,
                sensitivityPercent)) {
            return;
        }
        output.sendHighResolutionScroll(
                (short) translatedStick.getY(),
                (short) translatedStick.getX());
    }

    private boolean translateStick(
            short stickX,
            short stickY,
            int sensitivityPercent) {
        translatedStick.initialize(stickX, stickY);
        translatedStick.scalarMultiply(
                STICK_NORMALIZATION_SCALE);
        translatedStick.scalarMultiply(
                BASE_MOUSE_SPEED *
                        sensitivityPercent *
                        PERCENT_SCALE);
        if (translatedStick.getMagnitude() > 0) {
            // Preserve the established cubic response curve.
            translatedStick.scalarMultiply(
                    Math.pow(
                            translatedStick.getMagnitude(),
                            2));
        }
        return translatedStick.getMagnitude() >= 1;
    }

    private static void sendMouseButtonChange(
            int inputMap,
            int changedMask,
            int buttonFlag,
            byte mouseButton,
            Output output) {
        if ((changedMask & buttonFlag) == 0) {
            return;
        }
        output.sendMouseButton(
                mouseButton,
                (inputMap & buttonFlag) != 0);
    }

    private static void sendKeyChange(
            int inputMap,
            int changedMask,
            int buttonFlag,
            int keyCode,
            Output output) {
        if ((changedMask & buttonFlag) == 0) {
            return;
        }
        output.sendKey(
                keyCode,
                (inputMap & buttonFlag) != 0
                        ? KeyboardPacket.KEY_DOWN
                        : KeyboardPacket.KEY_UP);
    }

    private static void sendChordOnPress(
            int inputMap,
            int changedMask,
            int buttonFlag,
            short[] chord,
            Output output) {
        if ((changedMask & buttonFlag) != 0 &&
                (inputMap & buttonFlag) != 0) {
            output.sendChord(chord);
        }
    }
}
