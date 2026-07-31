package com.limelight.binding.input;

import java.util.Objects;

/**
 * Immutable controller-axis assignment derived from platform-probed capabilities.
 */
final class ControllerAxisProfile {
    enum Axis {
        NONE,
        X,
        Y,
        Z,
        RZ,
        RX,
        RY,
        LEFT_TRIGGER,
        RIGHT_TRIGGER,
        BRAKE,
        GAS,
        THROTTLE,
        HAT_X,
        HAT_Y
    }

    private static final int SONY_VENDOR_ID = 0x054c;

    private final boolean hasLeftStick;
    private final Axis leftStickX;
    private final Axis leftStickY;
    private final Axis rightStickX;
    private final Axis rightStickY;
    private final Axis leftTrigger;
    private final Axis rightTrigger;
    private final Axis hatX;
    private final Axis hatY;
    private final boolean triggersIdleNegative;
    private final boolean nonStandardDualShock4;
    private final boolean linuxStandardFaceButtons;

    private ControllerAxisProfile(
            boolean hasLeftStick,
            Axis rightStickX,
            Axis rightStickY,
            Axis leftTrigger,
            Axis rightTrigger,
            Axis hatX,
            Axis hatY,
            boolean triggersIdleNegative,
            boolean nonStandardDualShock4,
            boolean linuxStandardFaceButtons) {
        this.hasLeftStick = hasLeftStick;
        this.leftStickX = Axis.X;
        this.leftStickY = Axis.Y;
        this.rightStickX = rightStickX;
        this.rightStickY = rightStickY;
        this.leftTrigger = leftTrigger;
        this.rightTrigger = rightTrigger;
        this.hatX = hatX;
        this.hatY = hatY;
        this.triggersIdleNegative = triggersIdleNegative;
        this.nonStandardDualShock4 = nonStandardDualShock4;
        this.linuxStandardFaceButtons = linuxStandardFaceButtons;
    }

    static ControllerAxisProfile resolve(
            int vendorId,
            boolean hasDeviceName,
            boolean hasButtonC,
            Capabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");

        Axis rightStickX = Axis.NONE;
        Axis rightStickY = Axis.NONE;
        Axis leftTrigger = Axis.NONE;
        Axis rightTrigger = Axis.NONE;
        boolean triggersIdleNegative = false;
        boolean nonStandardDualShock4 = false;
        boolean linuxStandardFaceButtons = false;

        if (capabilities.leftTriggerAndRightTrigger) {
            leftTrigger = Axis.LEFT_TRIGGER;
            rightTrigger = Axis.RIGHT_TRIGGER;
        }
        else if (capabilities.brakeAndGas) {
            leftTrigger = Axis.BRAKE;
            rightTrigger = Axis.GAS;
        }
        else if (capabilities.brakeAndThrottle) {
            leftTrigger = Axis.BRAKE;
            rightTrigger = Axis.THROTTLE;
        }
        else if (capabilities.rxAndRy && hasDeviceName) {
            if (vendorId == SONY_VENDOR_ID) {
                nonStandardDualShock4 = hasButtonC;
                linuxStandardFaceButtons = !hasButtonC;
            }

            if (nonStandardDualShock4) {
                leftTrigger = Axis.RX;
                rightTrigger = Axis.RY;
            }
            else {
                rightStickX = Axis.RX;
                rightStickY = Axis.RY;
                if (capabilities.zAndRz) {
                    leftTrigger = Axis.Z;
                    rightTrigger = Axis.RZ;
                }
            }
            triggersIdleNegative = true;
        }

        if (rightStickX == Axis.NONE &&
                rightStickY == Axis.NONE) {
            if (capabilities.zAndRz) {
                rightStickX = Axis.Z;
                rightStickY = Axis.RZ;
            }
            else if (capabilities.rxAndRy) {
                rightStickX = Axis.RX;
                rightStickY = Axis.RY;
            }
        }

        Axis hatX =
                capabilities.hatXAndHatY
                        ? Axis.HAT_X
                        : Axis.NONE;
        Axis hatY =
                capabilities.hatXAndHatY
                        ? Axis.HAT_Y
                        : Axis.NONE;
        return new ControllerAxisProfile(
                capabilities.xAndY,
                rightStickX,
                rightStickY,
                leftTrigger,
                rightTrigger,
                hatX,
                hatY,
                triggersIdleNegative,
                nonStandardDualShock4,
                linuxStandardFaceButtons);
    }

    boolean hasLeftStick() {
        return hasLeftStick;
    }

    Axis getLeftStickX() {
        return leftStickX;
    }

    Axis getLeftStickY() {
        return leftStickY;
    }

    Axis getRightStickX() {
        return rightStickX;
    }

    Axis getRightStickY() {
        return rightStickY;
    }

    Axis getLeftTrigger() {
        return leftTrigger;
    }

    Axis getRightTrigger() {
        return rightTrigger;
    }

    Axis getHatX() {
        return hatX;
    }

    Axis getHatY() {
        return hatY;
    }

    boolean areTriggersIdleNegative() {
        return triggersIdleNegative;
    }

    boolean isNonStandardDualShock4() {
        return nonStandardDualShock4;
    }

    boolean hasLinuxStandardFaceButtons() {
        return linuxStandardFaceButtons;
    }

    static final class Capabilities {
        private final boolean xAndY;
        private final boolean leftTriggerAndRightTrigger;
        private final boolean brakeAndGas;
        private final boolean brakeAndThrottle;
        private final boolean rxAndRy;
        private final boolean zAndRz;
        private final boolean hatXAndHatY;

        private Capabilities(Builder builder) {
            xAndY = builder.xAndY;
            leftTriggerAndRightTrigger =
                    builder.leftTriggerAndRightTrigger;
            brakeAndGas = builder.brakeAndGas;
            brakeAndThrottle = builder.brakeAndThrottle;
            rxAndRy = builder.rxAndRy;
            zAndRz = builder.zAndRz;
            hatXAndHatY = builder.hatXAndHatY;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private boolean xAndY;
            private boolean leftTriggerAndRightTrigger;
            private boolean brakeAndGas;
            private boolean brakeAndThrottle;
            private boolean rxAndRy;
            private boolean zAndRz;
            private boolean hatXAndHatY;

            Builder xAndY(boolean present) {
                xAndY = present;
                return this;
            }

            Builder leftTriggerAndRightTrigger(
                    boolean present) {
                leftTriggerAndRightTrigger = present;
                return this;
            }

            Builder brakeAndGas(boolean present) {
                brakeAndGas = present;
                return this;
            }

            Builder brakeAndThrottle(boolean present) {
                brakeAndThrottle = present;
                return this;
            }

            Builder rxAndRy(boolean present) {
                rxAndRy = present;
                return this;
            }

            Builder zAndRz(boolean present) {
                zAndRz = present;
                return this;
            }

            Builder hatXAndHatY(boolean present) {
                hatXAndHatY = present;
                return this;
            }

            Capabilities build() {
                return new Capabilities(this);
            }
        }
    }
}
