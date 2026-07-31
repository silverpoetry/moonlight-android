package com.limelight.binding.input;

import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;

/** Immutable Android key-code to protocol digital-input mapping. */
final class ControllerDigitalButtonMapping {
    enum Target {
        UNHANDLED(0),
        SPECIAL(ControllerPacket.SPECIAL_BUTTON_FLAG),
        PLAY(ControllerPacket.PLAY_FLAG),
        BACK(ControllerPacket.BACK_FLAG),
        DPAD_LEFT(ControllerPacket.LEFT_FLAG),
        DPAD_RIGHT(ControllerPacket.RIGHT_FLAG),
        DPAD_UP(ControllerPacket.UP_FLAG),
        DPAD_DOWN(ControllerPacket.DOWN_FLAG),
        DPAD_UP_LEFT(
                ControllerPacket.UP_FLAG |
                        ControllerPacket.LEFT_FLAG),
        DPAD_UP_RIGHT(
                ControllerPacket.UP_FLAG |
                        ControllerPacket.RIGHT_FLAG),
        DPAD_DOWN_LEFT(
                ControllerPacket.DOWN_FLAG |
                        ControllerPacket.LEFT_FLAG),
        DPAD_DOWN_RIGHT(
                ControllerPacket.DOWN_FLAG |
                        ControllerPacket.RIGHT_FLAG),
        A(ControllerPacket.A_FLAG),
        B(ControllerPacket.B_FLAG),
        X(ControllerPacket.X_FLAG),
        Y(ControllerPacket.Y_FLAG),
        LEFT_BUMPER(ControllerPacket.LB_FLAG),
        RIGHT_BUMPER(ControllerPacket.RB_FLAG),
        LEFT_STICK(ControllerPacket.LS_CLK_FLAG),
        RIGHT_STICK(ControllerPacket.RS_CLK_FLAG),
        MISC(ControllerPacket.MISC_FLAG),
        TOUCHPAD(ControllerPacket.TOUCHPAD_FLAG),
        LEFT_TRIGGER(0),
        RIGHT_TRIGGER(0),
        PADDLE_1(ControllerPacket.PADDLE1_FLAG),
        PADDLE_2(ControllerPacket.PADDLE2_FLAG),
        PADDLE_3(ControllerPacket.PADDLE3_FLAG),
        PADDLE_4(ControllerPacket.PADDLE4_FLAG);

        private final int inputMask;

        Target(int inputMask) {
            this.inputMask = inputMask;
        }

        int getInputMask() {
            return inputMask;
        }
    }

    private ControllerDigitalButtonMapping() {
    }

    static Target resolve(
            int keyCode,
            int scanCode,
            boolean hasPaddles) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_MODE:
                return Target.SPECIAL;
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_MENU:
                return Target.PLAY;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                return Target.BACK;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return Target.DPAD_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return Target.DPAD_RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return Target.DPAD_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return Target.DPAD_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP_LEFT:
                return Target.DPAD_UP_LEFT;
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
                return Target.DPAD_UP_RIGHT;
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                return Target.DPAD_DOWN_LEFT;
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                return Target.DPAD_DOWN_RIGHT;
            case KeyEvent.KEYCODE_BUTTON_B:
                return Target.B;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                return Target.A;
            case KeyEvent.KEYCODE_BUTTON_X:
                return Target.X;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return Target.Y;
            case KeyEvent.KEYCODE_BUTTON_L1:
                return Target.LEFT_BUMPER;
            case KeyEvent.KEYCODE_BUTTON_R1:
                return Target.RIGHT_BUMPER;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                return Target.LEFT_STICK;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                return Target.RIGHT_STICK;
            case KeyEvent.KEYCODE_MEDIA_RECORD:
                return Target.MISC;
            case KeyEvent.KEYCODE_BUTTON_1:
                return Target.TOUCHPAD;
            case KeyEvent.KEYCODE_BUTTON_L2:
                return Target.LEFT_TRIGGER;
            case KeyEvent.KEYCODE_BUTTON_R2:
                return Target.RIGHT_TRIGGER;
            case KeyEvent.KEYCODE_UNKNOWN:
                return hasPaddles
                        ? resolvePaddle(scanCode)
                        : Target.UNHANDLED;
            default:
                return Target.UNHANDLED;
        }
    }

    static boolean isSuppressedByHat(
            Target target,
            boolean horizontalHatUsed,
            boolean verticalHatUsed) {
        switch (target) {
            case DPAD_LEFT:
            case DPAD_RIGHT:
                return horizontalHatUsed;
            case DPAD_UP:
            case DPAD_DOWN:
                return verticalHatUsed;
            case DPAD_UP_LEFT:
            case DPAD_UP_RIGHT:
            case DPAD_DOWN_LEFT:
            case DPAD_DOWN_RIGHT:
                return horizontalHatUsed && verticalHatUsed;
            default:
                return false;
        }
    }

    private static Target resolvePaddle(int scanCode) {
        switch (scanCode) {
            case 0x2c4:
                return Target.PADDLE_1;
            case 0x2c5:
                return Target.PADDLE_2;
            case 0x2c6:
                return Target.PADDLE_3;
            case 0x2c7:
                return Target.PADDLE_4;
            default:
                return Target.UNHANDLED;
        }
    }
}
