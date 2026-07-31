package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * One type-safe physical-controller settings intent.
 *
 * <p>An intent owns both the immutable state transition and the canonical
 * persistence transaction. Compound policies therefore cannot be partially
 * applied by UI code.</p>
 */
public final class ControllerSettingsUpdate {
    private interface Applier {
        ControllerSettings apply(ControllerSettings settings);
    }

    private interface Persister {
        void persist(SettingsRepository.Editor editor);
    }

    private final Applier applier;
    private final Persister persister;

    private ControllerSettingsUpdate(
            Applier applier,
            Persister persister) {
        this.applier = Objects.requireNonNull(applier, "applier");
        this.persister =
                Objects.requireNonNull(persister, "persister");
    }

    public static ControllerSettingsUpdate
            mouseSensitivityPercent(int value) {
        return single(
                ControllerSettingKeys.MOUSE_SENSITIVITY_PERCENT,
                value,
                ControllerSettings.Builder
                        ::setMouseSensitivityPercent);
    }

    /**
     * The historical "bind all USB" control also enables the USB driver when
     * selected. Clearing it intentionally leaves the driver enabled.
     */
    public static ControllerSettingsUpdate
            claimAllUsbDevices(boolean claimAll) {
        return new ControllerSettingsUpdate(
                settings -> settings.toBuilder()
                        .setUsbDriverEnabled(
                                claimAll ||
                                        settings
                                                .isUsbDriverEnabled())
                        .setClaimAllUsbDevices(claimAll)
                        .build(),
                editor -> {
                    if (claimAll) {
                        editor.put(
                                ControllerSettingKeys.USB_DRIVER,
                                true);
                    }
                    editor.put(
                            ControllerSettingKeys
                                    .CLAIM_ALL_USB_DEVICES,
                            claimAll);
                });
    }

    public static ControllerSettingsUpdate
            rumbleMotorsFlipped(boolean flipped) {
        return single(
                ControllerSettingKeys.FLIP_RUMBLE_MOTORS,
                flipped,
                ControllerSettings.Builder
                        ::setRumbleMotorsFlipped);
    }

    public static ControllerSettingsUpdate
            triggerDeadzoneDisabled(boolean disabled) {
        return single(
                ControllerSettingKeys.DISABLE_TRIGGER_DEADZONE,
                disabled,
                ControllerSettings.Builder
                        ::setTriggerDeadzoneDisabled);
    }

    public static ControllerSettingsUpdate
            deviceRumbleEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.DEVICE_RUMBLE,
                enabled,
                ControllerSettings.Builder
                        ::setDeviceRumbleEnabled);
    }

    public static ControllerSettingsUpdate
            virtualControllerMotionEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.VIRTUAL_CONTROLLER_MOTION,
                enabled,
                ControllerSettings.Builder
                        ::setVirtualControllerMotionEnabled);
    }

    public static ControllerSettingsUpdate
            joyConFixEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.JOY_CON_FIX,
                enabled,
                ControllerSettings.Builder
                        ::setJoyConFixEnabled);
    }

    public static ControllerSettingsUpdate
            batteryReportingEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.BATTERY_REPORTING,
                enabled,
                ControllerSettings.Builder
                        ::setBatteryReportingEnabled);
    }

    public static ControllerSettingsUpdate
            usbGyroscopeReportingEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.USB_GYROSCOPE_REPORTING,
                enabled,
                ControllerSettings.Builder
                        ::setUsbGyroscopeReportingEnabled);
    }

    public static ControllerSettingsUpdate
            triggerRumbleLinkEnabled(boolean enabled) {
        return single(
                ControllerSettingKeys.TRIGGER_RUMBLE_LINK,
                enabled,
                ControllerSettings.Builder
                        ::setTriggerRumbleLinkEnabled);
    }

    public static ControllerSettingsUpdate adaptiveTriggerMode(
            int mode) {
        return single(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_MODE,
                mode,
                ControllerSettings.Builder
                        ::setAdaptiveTriggerMode);
    }

    public static ControllerSettingsUpdate adaptiveTriggerStrength(
            int strength) {
        return single(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_STRENGTH,
                strength,
                ControllerSettings.Builder
                        ::setAdaptiveTriggerStrength);
    }

    public static ControllerSettingsUpdate adaptiveTriggerFrequency(
            int frequency) {
        return single(
                ControllerSettingKeys.ADAPTIVE_TRIGGER_FREQUENCY,
                frequency,
                ControllerSettings.Builder
                        ::setAdaptiveTriggerFrequency);
    }

    public static ControllerSettingsUpdate
            adaptiveTriggerStartPosition(int position) {
        return single(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_START_POSITION,
                position,
                ControllerSettings.Builder
                        ::setAdaptiveTriggerStartPosition);
    }

    public static ControllerSettingsUpdate
            adaptiveTriggerEndPosition(int position) {
        return single(
                ControllerSettingKeys
                        .ADAPTIVE_TRIGGER_END_POSITION,
                position,
                ControllerSettings.Builder
                        ::setAdaptiveTriggerEndPosition);
    }

    public ControllerSettings applyTo(
            ControllerSettings settings) {
        return applier.apply(
                Objects.requireNonNull(settings, "settings"));
    }

    public void persist(SettingsRepository repository) {
        SettingsRepository.Editor editor =
                Objects.requireNonNull(repository, "repository")
                        .edit();
        persister.persist(editor);
        editor.apply();
    }

    private interface ValueApplier<T> {
        ControllerSettings.Builder apply(
                ControllerSettings.Builder builder,
                T value);
    }

    private static <T> ControllerSettingsUpdate single(
            SettingKey<T> key,
            T value,
            ValueApplier<T> applier) {
        T normalized = key.normalizeValue(value);
        return new ControllerSettingsUpdate(
                settings -> applier.apply(
                                settings.toBuilder(),
                                normalized)
                        .build(),
                editor -> editor.put(key, normalized));
    }
}
