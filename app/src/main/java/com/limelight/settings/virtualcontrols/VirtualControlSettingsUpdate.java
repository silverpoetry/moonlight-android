package com.limelight.settings.virtualcontrols;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * One type-safe virtual-control settings intent.
 *
 * <p>Each intent updates one immutable snapshot field and persists exactly one
 * canonical key. The composition root remains the only mutation owner.</p>
 */
public final class VirtualControlSettingsUpdate<T> {
    private interface Applier<T> {
        void apply(
                VirtualControlSettings.Builder builder,
                T value);
    }

    private final SettingKey<T> key;
    private final T value;
    private final Applier<T> applier;

    private VirtualControlSettingsUpdate(
            SettingKey<T> key,
            T value,
            Applier<T> applier) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = key.normalizeValue(value);
        this.applier = Objects.requireNonNull(applier, "applier");
    }

    public static VirtualControlSettingsUpdate<Integer>
            controlOpacityPercent(int value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.CONTROL_OPACITY_PERCENT,
                value,
                VirtualControlSettings.Builder
                        ::setControlOpacityPercent);
    }

    public static VirtualControlSettingsUpdate<Integer>
            keyboardOpacityPercent(int value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.KEYBOARD_OPACITY_PERCENT,
                value,
                VirtualControlSettings.Builder
                        ::setKeyboardOpacityPercent);
    }

    public static VirtualControlSettingsUpdate<Integer>
            keyboardHeightDp(int value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.KEYBOARD_HEIGHT_DP,
                value,
                VirtualControlSettings.Builder::setKeyboardHeightDp);
    }

    public static VirtualControlSettingsUpdate<Boolean>
            keyboardHapticsEnabled(boolean enabled) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.KEYBOARD_HAPTICS,
                enabled,
                VirtualControlSettings.Builder
                        ::setKeyboardHapticsEnabled);
    }

    public static VirtualControlSettingsUpdate<Integer>
            normalColor(int value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.NORMAL_COLOR,
                value,
                VirtualControlSettings.Builder::setNormalColor);
    }

    public static VirtualControlSettingsUpdate<Integer>
            gamepadScalePercent(int value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.GAMEPAD_SCALE_PERCENT,
                value,
                VirtualControlSettings.Builder
                        ::setGamepadScalePercent);
    }

    public static VirtualControlSettingsUpdate<String>
            keyboardLayoutId(String value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID,
                value,
                VirtualControlSettings.Builder::setKeyboardLayoutId);
    }

    public static VirtualControlSettingsUpdate<String>
            gamepadLayoutId(String value) {
        return new VirtualControlSettingsUpdate<>(
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID,
                value,
                VirtualControlSettings.Builder::setGamepadLayoutId);
    }

    public VirtualControlSettings applyTo(
            VirtualControlSettings settings) {
        VirtualControlSettings.Builder builder =
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
