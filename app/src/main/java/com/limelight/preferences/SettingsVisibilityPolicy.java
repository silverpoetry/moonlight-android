package com.limelight.preferences;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.controller.ControllerSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.ui.StreamUiSettingKeys;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pure device-capability policy for settings section and row visibility. */
final class SettingsVisibilityPolicy {
    private SettingsVisibilityPolicy() {
    }

    static Result evaluate(
            SettingsDeviceCapabilities capabilities,
            boolean barometerForcePressEnabled) {
        LinkedHashSet<String> hiddenSections = new LinkedHashSet<>();
        LinkedHashSet<String> hiddenItems = new LinkedHashSet<>();

        if (!capabilities.isTouchscreenAvailable()) {
            hiddenSections.add(SettingsScreenIds.SECTION_VIRTUAL_CONTROLS);
        }
        if (!capabilities.isAbsoluteMouseModeAvailable()) {
            hiddenItems.add(InputSettingKeys.ABSOLUTE_MOUSE_MODE.getName());
        }
        if (!capabilities.isBarometerAvailable()) {
            hiddenItems.add(
                    InputSettingKeys.BAROMETER_FORCE_PRESS.getName());
        }
        if (!capabilities.isBarometerAvailable() ||
                !barometerForcePressEnabled) {
            hiddenItems.add(
                    InputSettingKeys
                            .BAROMETER_FORCE_PRESS_THRESHOLD
                            .getName());
            hiddenItems.add(
                    InputSettingKeys
                            .BAROMETER_FORCE_PRESS_MINIMUM_DURATION
                            .getName());
        }
        if (!capabilities.areControllerMotionSensorsAvailable()) {
            hiddenItems.add(
                    ControllerSettingKeys.MOTION_SENSORS.getName());
        }
        if (!capabilities.areDeviceMotionSensorsAvailable()) {
            hiddenItems.add(
                    ControllerSettingKeys
                            .MOTION_SENSORS_FALLBACK_TO_DEVICE
                            .getName());
        }
        if (!capabilities.isUsbHostAvailable()) {
            hiddenItems.add(
                    ControllerSettingKeys.CLAIM_ALL_USB_DEVICES.getName());
            hiddenItems.add(ControllerSettingKeys.USB_DRIVER.getName());
        }
        if (!capabilities.isPictureInPictureAvailable()) {
            hiddenItems.add(
                    StreamUiSettingKeys.PICTURE_IN_PICTURE.getName());
        }
        if (!capabilities.isVibratorAvailable()) {
            hiddenItems.add(
                    ControllerSettingKeys.FALLBACK_DEVICE_RUMBLE.getName());
            hiddenItems.add(
                    ControllerSettingKeys
                            .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT
                            .getName());
            hiddenItems.add(StreamAudioSettingKeys.AUDIO_HAPTICS.getName());
            hiddenItems.add(
                    StreamAudioSettingKeys
                            .AUDIO_HAPTICS_STRENGTH_PERCENT
                            .getName());
            hiddenItems.add(
                    StreamAudioSettingKeys
                            .AUDIO_HAPTICS_VOICE_FILTER
                            .getName());
            hiddenItems.add(
                    ControllerSettingKeys.ONSCREEN_RUMBLE.getName());
        }
        else if (!capabilities
                .isVibrationAmplitudeControlAvailable()) {
            hiddenItems.add(
                    ControllerSettingKeys
                            .FALLBACK_DEVICE_RUMBLE_STRENGTH_PERCENT
                            .getName());
        }

        return new Result(hiddenSections, hiddenItems);
    }

    static final class Result {
        private final Set<String> hiddenSectionIds;
        private final Set<String> hiddenItemIds;

        private Result(
                Set<String> hiddenSectionIds,
                Set<String> hiddenItemIds) {
            this.hiddenSectionIds = Collections.unmodifiableSet(
                    new LinkedHashSet<>(hiddenSectionIds));
            this.hiddenItemIds = Collections.unmodifiableSet(
                    new LinkedHashSet<>(hiddenItemIds));
        }

        Set<String> getHiddenSectionIds() {
            return hiddenSectionIds;
        }

        Set<String> getHiddenItemIds() {
            return hiddenItemIds;
        }
    }
}
