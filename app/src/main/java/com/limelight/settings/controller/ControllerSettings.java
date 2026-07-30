package com.limelight.settings.controller;

/**
 * Immutable physical-controller policy for one stream session.
 */
public final class ControllerSettings {
    public enum AnalogStickForScrolling {
        NONE,
        RIGHT,
        LEFT
    }

    private final int stickDeadzonePercent;
    private final boolean multiControllerEnabled;
    private final boolean usbDriverEnabled;
    private final boolean onscreenControllerEnabled;
    private final boolean onlyL3R3Enabled;
    private final boolean triggerDeadzoneDisabled;
    private final boolean deviceRumbleEnabled;
    private final boolean motionSensorsEnabled;
    private final boolean motionSensorsFallbackToDevice;
    private final boolean joyConFixEnabled;
    private final boolean touchpadAsMouse;
    private final int mouseSensitivityPercent;
    private final boolean rumbleMotorsFlipped;
    private final boolean forceStrongVibrations;
    private final boolean forceStrongVibrationsStopPulse;
    private final boolean onscreenRumbleEnabled;
    private final boolean fallbackDeviceRumbleEnabled;
    private final int fallbackDeviceRumbleStrengthPercent;
    private final boolean forceGyroEnabled;
    private final boolean forceGyroRequiresLeftTrigger;
    private final boolean forceGyroAxesSwapped;
    private final int forceGyroSensitivityPercent;
    private final boolean virtualControllerMotionEnabled;
    private final boolean faceButtonsFlipped;
    private final boolean mouseEmulationEnabled;
    private final int mouseEmulationButton;
    private final boolean mouseEmulationOpensGameMenu;
    private final boolean usbGyroscopeReportingEnabled;
    private final AnalogStickForScrolling analogStickForScrolling;
    private final boolean batteryReportingEnabled;
    private final boolean controllerAudioHapticsEnabled;
    private final boolean audioHapticsTargetController;
    private final boolean keepControllerRumbleWithAudioHaptics;

    private ControllerSettings(Builder builder) {
        stickDeadzonePercent = clamp(
                builder.stickDeadzonePercent,
                0,
                50);
        multiControllerEnabled = builder.multiControllerEnabled;
        usbDriverEnabled = builder.usbDriverEnabled;
        onscreenControllerEnabled =
                builder.onscreenControllerEnabled;
        onlyL3R3Enabled = builder.onlyL3R3Enabled;
        triggerDeadzoneDisabled =
                builder.triggerDeadzoneDisabled;
        deviceRumbleEnabled = builder.deviceRumbleEnabled;
        motionSensorsEnabled = builder.motionSensorsEnabled;
        motionSensorsFallbackToDevice =
                builder.motionSensorsFallbackToDevice;
        joyConFixEnabled = builder.joyConFixEnabled;
        touchpadAsMouse = builder.touchpadAsMouse;
        mouseSensitivityPercent = clamp(
                builder.mouseSensitivityPercent,
                10,
                300);
        rumbleMotorsFlipped = builder.rumbleMotorsFlipped;
        forceStrongVibrations = builder.forceStrongVibrations;
        forceStrongVibrationsStopPulse =
                builder.forceStrongVibrationsStopPulse;
        onscreenRumbleEnabled = builder.onscreenRumbleEnabled;
        fallbackDeviceRumbleEnabled =
                builder.fallbackDeviceRumbleEnabled;
        fallbackDeviceRumbleStrengthPercent = clamp(
                builder.fallbackDeviceRumbleStrengthPercent,
                0,
                200);
        forceGyroEnabled = builder.forceGyroEnabled;
        forceGyroRequiresLeftTrigger =
                builder.forceGyroRequiresLeftTrigger;
        forceGyroAxesSwapped = builder.forceGyroAxesSwapped;
        forceGyroSensitivityPercent = clamp(
                builder.forceGyroSensitivityPercent,
                50,
                200);
        virtualControllerMotionEnabled =
                builder.virtualControllerMotionEnabled;
        faceButtonsFlipped = builder.faceButtonsFlipped;
        mouseEmulationEnabled = builder.mouseEmulationEnabled;
        mouseEmulationButton = normalizeMouseEmulationButton(
                builder.mouseEmulationButton);
        mouseEmulationOpensGameMenu =
                builder.mouseEmulationOpensGameMenu;
        usbGyroscopeReportingEnabled =
                builder.usbGyroscopeReportingEnabled;
        analogStickForScrolling =
                builder.analogStickForScrolling == null
                        ? AnalogStickForScrolling.RIGHT
                        : builder.analogStickForScrolling;
        batteryReportingEnabled = builder.batteryReportingEnabled;
        controllerAudioHapticsEnabled =
                builder.controllerAudioHapticsEnabled;
        audioHapticsTargetController =
                builder.audioHapticsTargetController;
        keepControllerRumbleWithAudioHaptics =
                builder.keepControllerRumbleWithAudioHaptics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getStickDeadzonePercent() {
        return stickDeadzonePercent;
    }

    public boolean isMultiControllerEnabled() {
        return multiControllerEnabled;
    }

    public boolean isUsbDriverEnabled() {
        return usbDriverEnabled;
    }

    public boolean isOnscreenControllerEnabled() {
        return onscreenControllerEnabled;
    }

    public boolean isOnlyL3R3Enabled() {
        return onlyL3R3Enabled;
    }

    public boolean isTriggerDeadzoneDisabled() {
        return triggerDeadzoneDisabled;
    }

    public boolean isDeviceRumbleEnabled() {
        return deviceRumbleEnabled;
    }

    public boolean areMotionSensorsEnabled() {
        return motionSensorsEnabled;
    }

    public boolean isMotionSensorsFallbackToDeviceEnabled() {
        return motionSensorsFallbackToDevice;
    }

    public boolean isJoyConFixEnabled() {
        return joyConFixEnabled;
    }

    public boolean isTouchpadAsMouse() {
        return touchpadAsMouse;
    }

    public int getMouseSensitivityPercent() {
        return mouseSensitivityPercent;
    }

    public boolean areRumbleMotorsFlipped() {
        return rumbleMotorsFlipped;
    }

    public boolean isForceStrongVibrationsEnabled() {
        return forceStrongVibrations;
    }

    public boolean isForceStrongVibrationsStopPulseEnabled() {
        return forceStrongVibrationsStopPulse;
    }

    public boolean isOnscreenRumbleEnabled() {
        return onscreenRumbleEnabled;
    }

    public boolean isFallbackDeviceRumbleEnabled() {
        return fallbackDeviceRumbleEnabled;
    }

    public int getFallbackDeviceRumbleStrengthPercent() {
        return fallbackDeviceRumbleStrengthPercent;
    }

    public boolean isForceGyroEnabled() {
        return forceGyroEnabled;
    }

    public boolean isForceGyroLeftTriggerRequired() {
        return forceGyroRequiresLeftTrigger;
    }

    public boolean areForceGyroAxesSwapped() {
        return forceGyroAxesSwapped;
    }

    public int getForceGyroSensitivityPercent() {
        return forceGyroSensitivityPercent;
    }

    public boolean isVirtualControllerMotionEnabled() {
        return virtualControllerMotionEnabled;
    }

    public boolean areFaceButtonsFlipped() {
        return faceButtonsFlipped;
    }

    public boolean isMouseEmulationEnabled() {
        return mouseEmulationEnabled;
    }

    public int getMouseEmulationButton() {
        return mouseEmulationButton;
    }

    public boolean doesMouseEmulationOpenGameMenu() {
        return mouseEmulationOpensGameMenu;
    }

    public boolean isUsbGyroscopeReportingEnabled() {
        return usbGyroscopeReportingEnabled;
    }

    public AnalogStickForScrolling getAnalogStickForScrolling() {
        return analogStickForScrolling;
    }

    public boolean isBatteryReportingEnabled() {
        return batteryReportingEnabled;
    }

    public boolean isControllerAudioHapticsEnabled() {
        return controllerAudioHapticsEnabled;
    }

    public boolean isAudioHapticsTargetController() {
        return audioHapticsTargetController;
    }

    public boolean shouldKeepControllerRumbleWithAudioHaptics() {
        return keepControllerRumbleWithAudioHaptics;
    }

    private static int normalizeMouseEmulationButton(int value) {
        return value >= 0 && value <= 2 ? value : 0;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Builder {
        private int stickDeadzonePercent = 7;
        private boolean multiControllerEnabled = true;
        private boolean usbDriverEnabled = true;
        private boolean onscreenControllerEnabled;
        private boolean onlyL3R3Enabled;
        private boolean triggerDeadzoneDisabled;
        private boolean deviceRumbleEnabled;
        private boolean motionSensorsEnabled = true;
        private boolean motionSensorsFallbackToDevice;
        private boolean joyConFixEnabled;
        private boolean touchpadAsMouse;
        private int mouseSensitivityPercent = 100;
        private boolean rumbleMotorsFlipped;
        private boolean forceStrongVibrations;
        private boolean forceStrongVibrationsStopPulse;
        private boolean onscreenRumbleEnabled = true;
        private boolean fallbackDeviceRumbleEnabled;
        private int fallbackDeviceRumbleStrengthPercent = 100;
        private boolean forceGyroEnabled;
        private boolean forceGyroRequiresLeftTrigger;
        private boolean forceGyroAxesSwapped = true;
        private int forceGyroSensitivityPercent = 120;
        private boolean virtualControllerMotionEnabled;
        private boolean faceButtonsFlipped;
        private boolean mouseEmulationEnabled = true;
        private int mouseEmulationButton;
        private boolean mouseEmulationOpensGameMenu;
        private boolean usbGyroscopeReportingEnabled = true;
        private AnalogStickForScrolling analogStickForScrolling =
                AnalogStickForScrolling.RIGHT;
        private boolean batteryReportingEnabled = true;
        private boolean controllerAudioHapticsEnabled;
        private boolean audioHapticsTargetController;
        private boolean keepControllerRumbleWithAudioHaptics;

        private Builder() {
        }

        public Builder setStickDeadzonePercent(int value) {
            stickDeadzonePercent = value;
            return this;
        }

        public Builder setMultiControllerEnabled(boolean enabled) {
            multiControllerEnabled = enabled;
            return this;
        }

        public Builder setUsbDriverEnabled(boolean enabled) {
            usbDriverEnabled = enabled;
            return this;
        }

        public Builder setOnscreenControllerEnabled(boolean enabled) {
            onscreenControllerEnabled = enabled;
            return this;
        }

        public Builder setOnlyL3R3Enabled(boolean enabled) {
            onlyL3R3Enabled = enabled;
            return this;
        }

        public Builder setTriggerDeadzoneDisabled(boolean disabled) {
            triggerDeadzoneDisabled = disabled;
            return this;
        }

        public Builder setDeviceRumbleEnabled(boolean enabled) {
            deviceRumbleEnabled = enabled;
            return this;
        }

        public Builder setMotionSensorsEnabled(boolean enabled) {
            motionSensorsEnabled = enabled;
            return this;
        }

        public Builder setMotionSensorsFallbackToDevice(
                boolean enabled) {
            motionSensorsFallbackToDevice = enabled;
            return this;
        }

        public Builder setJoyConFixEnabled(boolean enabled) {
            joyConFixEnabled = enabled;
            return this;
        }

        public Builder setTouchpadAsMouse(boolean enabled) {
            touchpadAsMouse = enabled;
            return this;
        }

        public Builder setMouseSensitivityPercent(int value) {
            mouseSensitivityPercent = value;
            return this;
        }

        public Builder setRumbleMotorsFlipped(boolean flipped) {
            rumbleMotorsFlipped = flipped;
            return this;
        }

        public Builder setForceStrongVibrations(boolean enabled) {
            forceStrongVibrations = enabled;
            return this;
        }

        public Builder setForceStrongVibrationsStopPulse(
                boolean enabled) {
            forceStrongVibrationsStopPulse = enabled;
            return this;
        }

        public Builder setOnscreenRumbleEnabled(boolean enabled) {
            onscreenRumbleEnabled = enabled;
            return this;
        }

        public Builder setFallbackDeviceRumble(
                boolean enabled,
                int strengthPercent) {
            fallbackDeviceRumbleEnabled = enabled;
            fallbackDeviceRumbleStrengthPercent = strengthPercent;
            return this;
        }

        public Builder setForceGyro(
                boolean enabled,
                boolean requireLeftTrigger,
                boolean swapAxes,
                int sensitivityPercent) {
            forceGyroEnabled = enabled;
            forceGyroRequiresLeftTrigger = requireLeftTrigger;
            forceGyroAxesSwapped = swapAxes;
            forceGyroSensitivityPercent = sensitivityPercent;
            return this;
        }

        public Builder setVirtualControllerMotionEnabled(
                boolean enabled) {
            virtualControllerMotionEnabled = enabled;
            return this;
        }

        public Builder setFaceButtonsFlipped(boolean flipped) {
            faceButtonsFlipped = flipped;
            return this;
        }

        public Builder setMouseEmulation(
                boolean enabled,
                int button,
                boolean openGameMenu) {
            mouseEmulationEnabled = enabled;
            mouseEmulationButton = button;
            mouseEmulationOpensGameMenu = openGameMenu;
            return this;
        }

        public Builder setUsbGyroscopeReportingEnabled(
                boolean enabled) {
            usbGyroscopeReportingEnabled = enabled;
            return this;
        }

        public Builder setAnalogStickForScrolling(
                AnalogStickForScrolling value) {
            analogStickForScrolling = value;
            return this;
        }

        public Builder setBatteryReportingEnabled(boolean enabled) {
            batteryReportingEnabled = enabled;
            return this;
        }

        public Builder setControllerAudioHaptics(
                boolean enabled,
                boolean targetController,
                boolean keepControllerRumble) {
            controllerAudioHapticsEnabled = enabled;
            audioHapticsTargetController = targetController;
            keepControllerRumbleWithAudioHaptics =
                    keepControllerRumble;
            return this;
        }

        public ControllerSettings build() {
            return new ControllerSettings(this);
        }
    }
}
