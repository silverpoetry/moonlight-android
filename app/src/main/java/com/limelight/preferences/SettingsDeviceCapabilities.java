package com.limelight.preferences;

/** Immutable semantic capabilities used to shape the settings screen. */
final class SettingsDeviceCapabilities {
    private final boolean touchscreenAvailable;
    private final boolean absoluteMouseModeAvailable;
    private final boolean barometerAvailable;
    private final boolean controllerMotionSensorsAvailable;
    private final boolean deviceMotionSensorsAvailable;
    private final boolean usbHostAvailable;
    private final boolean pictureInPictureAvailable;
    private final boolean vibratorAvailable;
    private final boolean vibrationAmplitudeControlAvailable;
    private final boolean xiaomiRefreshRateOverrideAvailable;

    private SettingsDeviceCapabilities(Builder builder) {
        touchscreenAvailable = builder.touchscreenAvailable;
        absoluteMouseModeAvailable = builder.absoluteMouseModeAvailable;
        barometerAvailable = builder.barometerAvailable;
        controllerMotionSensorsAvailable =
                builder.controllerMotionSensorsAvailable;
        deviceMotionSensorsAvailable =
                builder.deviceMotionSensorsAvailable;
        usbHostAvailable = builder.usbHostAvailable;
        pictureInPictureAvailable =
                builder.pictureInPictureAvailable;
        vibratorAvailable = builder.vibratorAvailable;
        vibrationAmplitudeControlAvailable =
                builder.vibrationAmplitudeControlAvailable;
        xiaomiRefreshRateOverrideAvailable =
                builder.xiaomiRefreshRateOverrideAvailable;
    }

    static Builder builder() {
        return new Builder();
    }

    boolean isTouchscreenAvailable() {
        return touchscreenAvailable;
    }

    boolean isAbsoluteMouseModeAvailable() {
        return absoluteMouseModeAvailable;
    }

    boolean isBarometerAvailable() {
        return barometerAvailable;
    }

    boolean areControllerMotionSensorsAvailable() {
        return controllerMotionSensorsAvailable;
    }

    boolean areDeviceMotionSensorsAvailable() {
        return deviceMotionSensorsAvailable;
    }

    boolean isUsbHostAvailable() {
        return usbHostAvailable;
    }

    boolean isPictureInPictureAvailable() {
        return pictureInPictureAvailable;
    }

    boolean isVibratorAvailable() {
        return vibratorAvailable;
    }

    boolean isVibrationAmplitudeControlAvailable() {
        return vibrationAmplitudeControlAvailable;
    }

    boolean isXiaomiRefreshRateOverrideAvailable() {
        return xiaomiRefreshRateOverrideAvailable;
    }

    static final class Builder {
        private boolean touchscreenAvailable;
        private boolean absoluteMouseModeAvailable;
        private boolean barometerAvailable;
        private boolean controllerMotionSensorsAvailable;
        private boolean deviceMotionSensorsAvailable;
        private boolean usbHostAvailable;
        private boolean pictureInPictureAvailable;
        private boolean vibratorAvailable;
        private boolean vibrationAmplitudeControlAvailable;
        private boolean xiaomiRefreshRateOverrideAvailable;

        Builder touchscreenAvailable(boolean value) {
            touchscreenAvailable = value;
            return this;
        }

        Builder absoluteMouseModeAvailable(boolean value) {
            absoluteMouseModeAvailable = value;
            return this;
        }

        Builder barometerAvailable(boolean value) {
            barometerAvailable = value;
            return this;
        }

        Builder controllerMotionSensorsAvailable(boolean value) {
            controllerMotionSensorsAvailable = value;
            return this;
        }

        Builder deviceMotionSensorsAvailable(boolean value) {
            deviceMotionSensorsAvailable = value;
            return this;
        }

        Builder usbHostAvailable(boolean value) {
            usbHostAvailable = value;
            return this;
        }

        Builder pictureInPictureAvailable(boolean value) {
            pictureInPictureAvailable = value;
            return this;
        }

        Builder vibratorAvailable(boolean value) {
            vibratorAvailable = value;
            return this;
        }

        Builder vibrationAmplitudeControlAvailable(boolean value) {
            vibrationAmplitudeControlAvailable = value;
            return this;
        }

        Builder xiaomiRefreshRateOverrideAvailable(boolean value) {
            xiaomiRefreshRateOverrideAvailable = value;
            return this;
        }

        SettingsDeviceCapabilities build() {
            return new SettingsDeviceCapabilities(this);
        }
    }
}
