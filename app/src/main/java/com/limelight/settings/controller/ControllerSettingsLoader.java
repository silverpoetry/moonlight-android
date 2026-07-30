package com.limelight.settings.controller;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.controller.ControllerSettings
        .AnalogStickForScrolling;

import java.util.Objects;

/**
 * Builds one validated controller-policy snapshot from persistent storage.
 */
public final class ControllerSettingsLoader {
    private ControllerSettingsLoader() {
    }

    public static ControllerSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        return ControllerSettings.builder()
                .setStickDeadzonePercent(repository.get(
                        ControllerSettingKeys.STICK_DEADZONE_PERCENT))
                .setMultiControllerEnabled(repository.get(
                        ControllerSettingKeys.MULTI_CONTROLLER))
                .setUsbDriverEnabled(repository.get(
                        ControllerSettingKeys.USB_DRIVER))
                .setOnscreenControllerEnabled(repository.get(
                        ControllerSettingKeys.ONSCREEN_CONTROLLER))
                .setOnlyL3R3Enabled(repository.get(
                        ControllerSettingKeys.ONLY_L3_R3))
                .setTriggerDeadzoneDisabled(repository.get(
                        ControllerSettingKeys
                                .DISABLE_TRIGGER_DEADZONE))
                .setDeviceRumbleEnabled(repository.get(
                        ControllerSettingKeys.DEVICE_RUMBLE))
                .setMotionSensorsEnabled(repository.get(
                        ControllerSettingKeys.MOTION_SENSORS))
                .setMotionSensorsFallbackToDevice(repository.get(
                        ControllerSettingKeys
                                .MOTION_SENSORS_FALLBACK_TO_DEVICE))
                .setJoyConFixEnabled(repository.get(
                        ControllerSettingKeys.JOY_CON_FIX))
                .setTouchpadAsMouse(repository.get(
                        ControllerSettingKeys.TOUCHPAD_AS_MOUSE))
                .setMouseSensitivityPercent(repository.get(
                        ControllerSettingKeys
                                .MOUSE_SENSITIVITY_PERCENT))
                .setRumbleMotorsFlipped(repository.get(
                        ControllerSettingKeys.FLIP_RUMBLE_MOTORS))
                .setForceStrongVibrations(repository.get(
                        ControllerSettingKeys
                                .FORCE_STRONG_VIBRATIONS))
                .setForceStrongVibrationsStopPulse(repository.get(
                        ControllerSettingKeys
                                .FORCE_STRONG_VIBRATIONS_STOP_PULSE))
                .setOnscreenRumbleEnabled(repository.get(
                        ControllerSettingKeys.ONSCREEN_RUMBLE))
                .setFallbackDeviceRumble(
                        repository.get(
                                ControllerSettingKeys
                                        .FALLBACK_DEVICE_RUMBLE),
                        repository.get(
                                ControllerSettingKeys
                                        .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT))
                .setForceGyro(
                        repository.get(
                                ControllerSettingKeys.FORCE_GYRO),
                        repository.get(
                                ControllerSettingKeys
                                        .FORCE_GYRO_REQUIRES_LEFT_TRIGGER),
                        repository.get(
                                ControllerSettingKeys
                                        .FORCE_GYRO_SWAP_AXES),
                        repository.get(
                                ControllerSettingKeys
                                        .FORCE_GYRO_SENSITIVITY_PERCENT))
                .setVirtualControllerMotionEnabled(repository.get(
                        ControllerSettingKeys
                                .VIRTUAL_CONTROLLER_MOTION))
                .setFaceButtonsFlipped(repository.get(
                        ControllerSettingKeys.FLIP_FACE_BUTTONS))
                .setMouseEmulation(
                        repository.get(
                                ControllerSettingKeys.MOUSE_EMULATION),
                        repository.get(
                                ControllerSettingKeys
                                        .MOUSE_EMULATION_BUTTON),
                        repository.get(
                                ControllerSettingKeys
                                        .MOUSE_EMULATION_OPENS_GAME_MENU))
                .setUsbGyroscopeReportingEnabled(repository.get(
                        ControllerSettingKeys
                                .USB_GYROSCOPE_REPORTING))
                .setAnalogStickForScrolling(parseAnalogStick(
                        repository.get(
                                ControllerSettingKeys
                                        .ANALOG_STICK_FOR_SCROLLING)))
                .setBatteryReportingEnabled(repository.get(
                        ControllerSettingKeys.BATTERY_REPORTING))
                .setControllerAudioHaptics(
                        repository.get(
                                ControllerSettingKeys
                                        .CONTROLLER_AUDIO_HAPTICS),
                        "controller".equals(repository.get(
                                ControllerSettingKeys
                                        .AUDIO_HAPTICS_OUTPUT_TARGET)),
                        repository.get(
                                ControllerSettingKeys
                                        .KEEP_CONTROLLER_RUMBLE_WITH_AUDIO_HAPTICS))
                .build();
    }

    private static AnalogStickForScrolling parseAnalogStick(
            String value) {
        if ("left".equals(value)) {
            return AnalogStickForScrolling.LEFT;
        }
        if ("none".equals(value)) {
            return AnalogStickForScrolling.NONE;
        }
        return AnalogStickForScrolling.RIGHT;
    }
}
