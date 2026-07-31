package com.limelight.binding.input;

import android.view.InputDevice;

import androidx.annotation.Nullable;

import java.util.Objects;

/** Immutable capabilities sampled when an Android input device is attached. */
final class AndroidControllerDeviceProfile {
    private final int deviceId;
    private final int vendorId;
    private final int productId;
    private final String name;
    private final boolean external;
    private final boolean hasPaddles;
    private final boolean hasShareButton;
    private final boolean hasModeButton;
    private final boolean hasSelectButton;
    private final InputDevice.MotionRange touchpadXRange;
    private final InputDevice.MotionRange touchpadYRange;
    private final InputDevice.MotionRange touchpadPressureRange;
    private final ControllerAxisProfile axisProfile;
    private final float leftStickDeadzoneRadius;
    private final float rightStickDeadzoneRadius;
    private final float triggerDeadzone;
    private final ControllerButtonMapper buttonMapper;
    private final boolean backIsStart;
    private final boolean modeIsSelect;

    private AndroidControllerDeviceProfile(Builder builder) {
        deviceId = builder.deviceId;
        vendorId = builder.vendorId;
        productId = builder.productId;
        name = builder.name;
        external = builder.external;
        hasPaddles = builder.hasPaddles;
        hasShareButton = builder.hasShareButton;
        hasModeButton = builder.hasModeButton;
        hasSelectButton = builder.hasSelectButton;
        touchpadXRange = builder.touchpadXRange;
        touchpadYRange = builder.touchpadYRange;
        touchpadPressureRange = builder.touchpadPressureRange;
        axisProfile = builder.axisProfile;
        leftStickDeadzoneRadius = builder.leftStickDeadzoneRadius;
        rightStickDeadzoneRadius = builder.rightStickDeadzoneRadius;
        triggerDeadzone = builder.triggerDeadzone;
        buttonMapper = builder.buttonMapper;
        backIsStart = builder.backIsStart;
        modeIsSelect = builder.modeIsSelect;
    }

    static Builder builder(
            int deviceId,
            int vendorId,
            int productId,
            String name,
            ControllerAxisProfile axisProfile,
            ControllerButtonMapper buttonMapper) {
        return new Builder(
                deviceId,
                vendorId,
                productId,
                name,
                axisProfile,
                buttonMapper);
    }

    int getDeviceId() {
        return deviceId;
    }

    int getVendorId() {
        return vendorId;
    }

    int getProductId() {
        return productId;
    }

    String getName() {
        return name;
    }

    boolean isExternal() {
        return external;
    }

    boolean hasPaddles() {
        return hasPaddles;
    }

    boolean hasShareButton() {
        return hasShareButton;
    }

    boolean hasModeButton() {
        return hasModeButton;
    }

    boolean hasSelectButton() {
        return hasSelectButton;
    }

    @Nullable
    InputDevice.MotionRange getTouchpadXRange() {
        return touchpadXRange;
    }

    @Nullable
    InputDevice.MotionRange getTouchpadYRange() {
        return touchpadYRange;
    }

    @Nullable
    InputDevice.MotionRange getTouchpadPressureRange() {
        return touchpadPressureRange;
    }

    ControllerAxisProfile getAxisProfile() {
        return axisProfile;
    }

    float getLeftStickDeadzoneRadius() {
        return leftStickDeadzoneRadius;
    }

    float getRightStickDeadzoneRadius() {
        return rightStickDeadzoneRadius;
    }

    float getTriggerDeadzone() {
        return triggerDeadzone;
    }

    ControllerButtonMapper getButtonMapper() {
        return buttonMapper;
    }

    ControllerButtonMappingState createButtonMappingState() {
        return new ControllerButtonMappingState(
                backIsStart,
                modeIsSelect);
    }

    static final class Builder {
        private final int deviceId;
        private final int vendorId;
        private final int productId;
        private final String name;
        private final ControllerAxisProfile axisProfile;
        private final ControllerButtonMapper buttonMapper;

        private boolean external;
        private boolean hasPaddles;
        private boolean hasShareButton;
        private boolean hasModeButton;
        private boolean hasSelectButton;
        private InputDevice.MotionRange touchpadXRange;
        private InputDevice.MotionRange touchpadYRange;
        private InputDevice.MotionRange touchpadPressureRange;
        private float leftStickDeadzoneRadius;
        private float rightStickDeadzoneRadius;
        private float triggerDeadzone;
        private boolean backIsStart;
        private boolean modeIsSelect;

        private Builder(
                int deviceId,
                int vendorId,
                int productId,
                String name,
                ControllerAxisProfile axisProfile,
                ControllerButtonMapper buttonMapper) {
            this.deviceId = deviceId;
            this.vendorId = vendorId;
            this.productId = productId;
            this.name = Objects.requireNonNull(name, "name");
            this.axisProfile = Objects.requireNonNull(
                    axisProfile,
                    "axisProfile");
            this.buttonMapper = Objects.requireNonNull(
                    buttonMapper,
                    "buttonMapper");
        }

        Builder external(boolean value) {
            external = value;
            return this;
        }

        Builder capabilities(
                boolean paddles,
                boolean shareButton,
                boolean modeButton,
                boolean selectButton) {
            hasPaddles = paddles;
            hasShareButton = shareButton;
            hasModeButton = modeButton;
            hasSelectButton = selectButton;
            return this;
        }

        Builder touchpadRanges(
                @Nullable InputDevice.MotionRange xRange,
                @Nullable InputDevice.MotionRange yRange,
                @Nullable InputDevice.MotionRange pressureRange) {
            touchpadXRange = xRange;
            touchpadYRange = yRange;
            touchpadPressureRange = pressureRange;
            return this;
        }

        Builder deadzones(
                float leftStick,
                float rightStick,
                float trigger) {
            leftStickDeadzoneRadius = leftStick;
            rightStickDeadzoneRadius = rightStick;
            triggerDeadzone = trigger;
            return this;
        }

        Builder buttonFallbacks(
                boolean mapBackToStart,
                boolean mapModeToSelect) {
            backIsStart = mapBackToStart;
            modeIsSelect = mapModeToSelect;
            return this;
        }

        AndroidControllerDeviceProfile build() {
            return new AndroidControllerDeviceProfile(this);
        }
    }
}
