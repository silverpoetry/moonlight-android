package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;

/**
 * Stateful gamepad-to-desktop input mapping used by controller mouse mode.
 */
final class ControllerMouseEmulationTranslator {
    interface Output {
        void sendMouseButton(byte button, boolean down);

        void sendKey(int keyCode, byte action);

        void sendChord(short[] keyCodes);
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

    private int previousInputMap;

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
