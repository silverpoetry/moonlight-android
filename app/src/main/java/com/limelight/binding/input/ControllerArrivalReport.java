package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.jni.MoonBridge;

/**
 * Immutable protocol report derived from controller capabilities.
 */
final class ControllerArrivalReport {
    private final byte reportedType;
    private final int supportedButtonFlags;
    private final short capabilities;
    private final boolean clickpadEmulationRequired;

    private ControllerArrivalReport(Builder builder) {
        int buttonFlags = builder.platformButtonFlags;
        if (builder.hasPaddles) {
            buttonFlags |=
                    ControllerPacket.PADDLE1_FLAG |
                            ControllerPacket.PADDLE2_FLAG |
                            ControllerPacket.PADDLE3_FLAG |
                            ControllerPacket.PADDLE4_FLAG;
        }
        if (builder.hasShareButton) {
            buttonFlags |= ControllerPacket.MISC_FLAG;
        }
        if (builder.hasHorizontalHatAxis) {
            buttonFlags |=
                    ControllerPacket.LEFT_FLAG |
                            ControllerPacket.RIGHT_FLAG;
        }
        if (builder.hasVerticalHatAxis) {
            buttonFlags |=
                    ControllerPacket.UP_FLAG |
                            ControllerPacket.DOWN_FLAG;
        }

        short capabilityFlags = 0;
        if (builder.hasAdvancedInputDeviceApis) {
            if (builder.hasQuadVibrators) {
                capabilityFlags |=
                        MoonBridge.LI_CCAP_RUMBLE |
                                MoonBridge.LI_CCAP_TRIGGER_RUMBLE;
            }
            else if (builder.hasVibratorManager ||
                    builder.hasLegacyVibrator) {
                capabilityFlags |= MoonBridge.LI_CCAP_RUMBLE;
            }
            if (builder.external) {
                capabilityFlags |=
                        MoonBridge.LI_CCAP_BATTERY_STATE;
            }
            if (builder.hasRgbLed &&
                    (builder.hasReliableRgbLedDetection ||
                            builder.detectedType ==
                                    MoonBridge.LI_CTYPE_PS)) {
                capabilityFlags |= MoonBridge.LI_CCAP_RGB_LED;
            }
        }
        if (builder.hasAnalogTriggers) {
            capabilityFlags |=
                    MoonBridge.LI_CCAP_ANALOG_TRIGGERS;
        }
        if (builder.hasAccelerometer) {
            capabilityFlags |= MoonBridge.LI_CCAP_ACCEL;
        }
        if (builder.hasGyroscope) {
            capabilityFlags |= MoonBridge.LI_CCAP_GYRO;
        }
        if (builder.hasLegacyVibrator) {
            capabilityFlags |= MoonBridge.LI_CCAP_RUMBLE;
        }
        if (builder.recognizedByShieldExtensions) {
            capabilityFlags |=
                    MoonBridge.LI_CCAP_RUMBLE |
                            MoonBridge.LI_CCAP_BATTERY_STATE;
        }
        if (builder.hasTouchpad) {
            capabilityFlags |= MoonBridge.LI_CCAP_TOUCHPAD;
            if (builder.hasClickpad) {
                buttonFlags |= ControllerPacket.TOUCHPAD_FLAG;
            }
        }

        clickpadEmulationRequired =
                builder.requiresGenericMotionControllerType;
        reportedType = clickpadEmulationRequired
                ? MoonBridge.LI_CTYPE_UNKNOWN
                : builder.detectedType;
        supportedButtonFlags = buttonFlags;
        capabilities = capabilityFlags;
    }

    static Builder builder(
            byte detectedType,
            int platformButtonFlags) {
        return new Builder(
                detectedType,
                platformButtonFlags);
    }

    byte getReportedType() {
        return reportedType;
    }

    int getSupportedButtonFlags() {
        return supportedButtonFlags;
    }

    short getCapabilities() {
        return capabilities;
    }

    boolean isClickpadEmulationRequired() {
        return clickpadEmulationRequired;
    }

    static final class Builder {
        private final byte detectedType;
        private final int platformButtonFlags;

        private boolean hasPaddles;
        private boolean hasShareButton;
        private boolean hasHorizontalHatAxis;
        private boolean hasVerticalHatAxis;
        private boolean hasAdvancedInputDeviceApis;
        private boolean hasQuadVibrators;
        private boolean hasVibratorManager;
        private boolean hasLegacyVibrator;
        private boolean external;
        private boolean hasRgbLed;
        private boolean hasReliableRgbLedDetection;
        private boolean hasAnalogTriggers;
        private boolean hasAccelerometer;
        private boolean hasGyroscope;
        private boolean requiresGenericMotionControllerType;
        private boolean recognizedByShieldExtensions;
        private boolean hasTouchpad;
        private boolean hasClickpad;

        private Builder(
                byte detectedType,
                int platformButtonFlags) {
            this.detectedType = detectedType;
            this.platformButtonFlags =
                    platformButtonFlags;
        }

        Builder hasPaddles(boolean value) {
            hasPaddles = value;
            return this;
        }

        Builder hasShareButton(boolean value) {
            hasShareButton = value;
            return this;
        }

        Builder hasHorizontalHatAxis(boolean value) {
            hasHorizontalHatAxis = value;
            return this;
        }

        Builder hasVerticalHatAxis(boolean value) {
            hasVerticalHatAxis = value;
            return this;
        }

        Builder hasAdvancedInputDeviceApis(boolean value) {
            hasAdvancedInputDeviceApis = value;
            return this;
        }

        Builder hasQuadVibrators(boolean value) {
            hasQuadVibrators = value;
            return this;
        }

        Builder hasVibratorManager(boolean value) {
            hasVibratorManager = value;
            return this;
        }

        Builder hasLegacyVibrator(boolean value) {
            hasLegacyVibrator = value;
            return this;
        }

        Builder external(boolean value) {
            external = value;
            return this;
        }

        Builder hasRgbLed(boolean value) {
            hasRgbLed = value;
            return this;
        }

        Builder hasReliableRgbLedDetection(boolean value) {
            hasReliableRgbLedDetection = value;
            return this;
        }

        Builder hasAnalogTriggers(boolean value) {
            hasAnalogTriggers = value;
            return this;
        }

        Builder hasAccelerometer(boolean value) {
            hasAccelerometer = value;
            return this;
        }

        Builder hasGyroscope(boolean value) {
            hasGyroscope = value;
            return this;
        }

        Builder requiresGenericMotionControllerType(
                boolean value) {
            requiresGenericMotionControllerType = value;
            return this;
        }

        Builder recognizedByShieldExtensions(boolean value) {
            recognizedByShieldExtensions = value;
            return this;
        }

        Builder hasTouchpad(boolean value) {
            hasTouchpad = value;
            return this;
        }

        Builder hasClickpad(boolean value) {
            hasClickpad = value;
            return this;
        }

        ControllerArrivalReport build() {
            return new ControllerArrivalReport(this);
        }
    }
}
