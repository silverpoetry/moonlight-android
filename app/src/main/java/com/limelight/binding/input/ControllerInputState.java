package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/**
 * Mutable protocol-visible state for one physical or virtual controller input
 * source.
 *
 * <p>The state belongs to a single controller context. Slot aggregation reads
 * it, but no other component may mutate its individual fields.</p>
 */
final class ControllerInputState {
    private int inputMap;
    private byte leftTrigger;
    private byte rightTrigger;
    private short leftStickX;
    private short leftStickY;
    private short rightStickX;
    private short rightStickY;
    private boolean leftTriggerAxisUsed;
    private boolean rightTriggerAxisUsed;
    private boolean horizontalHatUsed;
    private boolean verticalHatUsed;

    int getInputMap() {
        return inputMap;
    }

    void setInputMap(int inputMap) {
        this.inputMap = inputMap;
    }

    void setButtonMask(int mask, boolean pressed) {
        if (pressed) {
            inputMap |= mask;
        }
        else {
            inputMap &= ~mask;
        }
    }

    byte getLeftTrigger() {
        return leftTrigger;
    }

    byte getRightTrigger() {
        return rightTrigger;
    }

    short getLeftStickX() {
        return leftStickX;
    }

    short getLeftStickY() {
        return leftStickY;
    }

    short getRightStickX() {
        return rightStickX;
    }

    short getRightStickY() {
        return rightStickY;
    }

    void setLeftStick(short x, short y) {
        leftStickX = x;
        leftStickY = y;
    }

    void setRightStick(short x, short y) {
        rightStickX = x;
        rightStickY = y;
    }

    void setTriggers(byte left, byte right) {
        leftTrigger = left;
        rightTrigger = right;
    }

    boolean isLeftTriggerAxisUsed() {
        return leftTriggerAxisUsed;
    }

    boolean isRightTriggerAxisUsed() {
        return rightTriggerAxisUsed;
    }

    void updateTriggerAxes(
            float left,
            float right,
            boolean idleIsNegative,
            float deadzone) {
        // Android initially reports zero even for axes whose true idle value
        // is negative. Do not normalize either axis until it has moved.
        if (left != 0) {
            leftTriggerAxisUsed = true;
        }
        if (right != 0) {
            rightTriggerAxisUsed = true;
        }
        if (idleIsNegative) {
            if (leftTriggerAxisUsed) {
                left = (left + 1) / 2;
            }
            if (rightTriggerAxisUsed) {
                right = (right + 1) / 2;
            }
        }

        if (left <= deadzone) {
            left = 0;
        }
        if (right <= deadzone) {
            right = 0;
        }

        setTriggers(
                (byte) (left * 0xff),
                (byte) (right * 0xff));
    }

    boolean setDigitalTrigger(boolean left, boolean pressed) {
        if (left) {
            if (leftTriggerAxisUsed) {
                return false;
            }
            leftTrigger = pressed ? (byte) 0xff : 0;
        }
        else {
            if (rightTriggerAxisUsed) {
                return false;
            }
            rightTrigger = pressed ? (byte) 0xff : 0;
        }
        return true;
    }

    boolean isHorizontalHatUsed() {
        return horizontalHatUsed;
    }

    boolean isVerticalHatUsed() {
        return verticalHatUsed;
    }

    void updateHat(float horizontal, float vertical) {
        inputMap &= ~(ControllerPacket.LEFT_FLAG |
                ControllerPacket.RIGHT_FLAG);
        if (horizontal < -0.5f) {
            inputMap |= ControllerPacket.LEFT_FLAG;
            horizontalHatUsed = true;
        }
        else if (horizontal > 0.5f) {
            inputMap |= ControllerPacket.RIGHT_FLAG;
            horizontalHatUsed = true;
        }

        inputMap &= ~(ControllerPacket.UP_FLAG |
                ControllerPacket.DOWN_FLAG);
        if (vertical < -0.5f) {
            inputMap |= ControllerPacket.UP_FLAG;
            verticalHatUsed = true;
        }
        else if (vertical > 0.5f) {
            inputMap |= ControllerPacket.DOWN_FLAG;
            verticalHatUsed = true;
        }
    }

    void replace(
            int inputMap,
            byte leftTrigger,
            byte rightTrigger,
            short leftStickX,
            short leftStickY,
            short rightStickX,
            short rightStickY) {
        this.inputMap = inputMap;
        this.leftTrigger = leftTrigger;
        this.rightTrigger = rightTrigger;
        this.leftStickX = leftStickX;
        this.leftStickY = leftStickY;
        this.rightStickX = rightStickX;
        this.rightStickY = rightStickY;
    }
}
