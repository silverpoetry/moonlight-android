package com.limelight.settings.controller;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * One type-safe physical-controller settings intent.
 */
public final class ControllerSettingsUpdate<T> {
    private interface Applier<T> {
        void apply(ControllerSettings.Builder builder, T value);
    }

    private final SettingKey<T> key;
    private final T value;
    private final Applier<T> applier;

    private ControllerSettingsUpdate(
            SettingKey<T> key,
            T value,
            Applier<T> applier) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = key.normalizeValue(value);
        this.applier = Objects.requireNonNull(applier, "applier");
    }

    public static ControllerSettingsUpdate<Integer>
            mouseSensitivityPercent(int value) {
        return new ControllerSettingsUpdate<>(
                ControllerSettingKeys.MOUSE_SENSITIVITY_PERCENT,
                value,
                ControllerSettings.Builder
                        ::setMouseSensitivityPercent);
    }

    public ControllerSettings applyTo(
            ControllerSettings settings) {
        ControllerSettings.Builder builder =
                Objects.requireNonNull(settings, "settings")
                        .toBuilder();
        applier.apply(builder, value);
        return builder.build();
    }

    public void persist(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository")
                .edit()
                .put(key, value)
                .apply();
    }
}
