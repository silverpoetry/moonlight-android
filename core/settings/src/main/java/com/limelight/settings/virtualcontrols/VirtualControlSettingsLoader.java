package com.limelight.settings.virtualcontrols;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Builds one validated virtual-control settings snapshot from storage.
 */
public final class VirtualControlSettingsLoader {
    private VirtualControlSettingsLoader() {
    }

    public static VirtualControlSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        return VirtualControlSettings.builder()
                .setControlOpacityPercent(repository.get(
                        VirtualControlSettingKeys
                                .CONTROL_OPACITY_PERCENT))
                .setKeyboardOpacityPercent(repository.get(
                        VirtualControlSettingKeys
                                .KEYBOARD_OPACITY_PERCENT))
                .setKeyboardHeightDp(repository.get(
                        VirtualControlSettingKeys.KEYBOARD_HEIGHT_DP))
                .setKeyboardHapticsEnabled(repository.get(
                        VirtualControlSettingKeys.KEYBOARD_HAPTICS))
                .setShowVirtualKeysOnStart(repository.get(
                        VirtualControlSettingKeys
                                .SHOW_VIRTUAL_KEYS_ON_START))
                .setGamepadSkin(repository.get(
                        VirtualControlSettingKeys.GAMEPAD_SKIN))
                .setSquareButtonsEnabled(repository.get(
                        VirtualControlSettingKeys.SQUARE_BUTTONS))
                .setGuideButtonVisible(repository.get(
                        VirtualControlSettingKeys.SHOW_GUIDE_BUTTON))
                .setFreeSticksEnabled(repository.get(
                        VirtualControlSettingKeys.FREE_STICKS))
                .setFreeStickOpacityPercent(repository.get(
                        VirtualControlSettingKeys
                                .FREE_STICK_OPACITY_PERCENT))
                .setFixedFreeSticksEnabled(repository.get(
                        VirtualControlSettingKeys.FIXED_FREE_STICKS))
                .setNormalColor(repository.get(
                        VirtualControlSettingKeys.NORMAL_COLOR))
                .setGamepadScalePercent(repository.get(
                        VirtualControlSettingKeys
                                .GAMEPAD_SCALE_PERCENT))
                .setStickClickDisabled(repository.get(
                        VirtualControlSettingKeys
                                .DISABLE_STICK_CLICK))
                .setAutomaticScreenOrientationEnabled(repository.get(
                        VirtualControlSettingKeys
                                .AUTOMATIC_SCREEN_ORIENTATION))
                .setKeyboardCombinationModeEnabled(repository.get(
                        VirtualControlSettingKeys
                                .KEYBOARD_COMBINATION_MODE))
                .setKeyboardLayoutId(repository.get(
                        VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID))
                .setGamepadLayoutId(repository.get(
                        VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID))
                .build();
    }

    public static void save(
            SettingsRepository repository,
            VirtualControlSettings settings) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(settings, "settings");

        repository.edit()
                .put(
                        VirtualControlSettingKeys
                                .CONTROL_OPACITY_PERCENT,
                        settings.getControlOpacityPercent())
                .put(
                        VirtualControlSettingKeys
                                .KEYBOARD_OPACITY_PERCENT,
                        settings.getKeyboardOpacityPercent())
                .put(
                        VirtualControlSettingKeys.KEYBOARD_HEIGHT_DP,
                        settings.getKeyboardHeightDp())
                .put(
                        VirtualControlSettingKeys.KEYBOARD_HAPTICS,
                        settings.isKeyboardHapticsEnabled())
                .put(
                        VirtualControlSettingKeys
                                .SHOW_VIRTUAL_KEYS_ON_START,
                        settings.shouldShowVirtualKeysOnStart())
                .put(
                        VirtualControlSettingKeys.GAMEPAD_SKIN,
                        settings.getGamepadSkin())
                .put(
                        VirtualControlSettingKeys.SQUARE_BUTTONS,
                        settings.areSquareButtonsEnabled())
                .put(
                        VirtualControlSettingKeys.SHOW_GUIDE_BUTTON,
                        settings.isGuideButtonVisible())
                .put(
                        VirtualControlSettingKeys.FREE_STICKS,
                        settings.areFreeSticksEnabled())
                .put(
                        VirtualControlSettingKeys
                                .FREE_STICK_OPACITY_PERCENT,
                        settings.getFreeStickOpacityPercent())
                .put(
                        VirtualControlSettingKeys.FIXED_FREE_STICKS,
                        settings.areFixedFreeSticksEnabled())
                .put(
                        VirtualControlSettingKeys.NORMAL_COLOR,
                        settings.getNormalColor())
                .put(
                        VirtualControlSettingKeys
                                .GAMEPAD_SCALE_PERCENT,
                        settings.getGamepadScalePercent())
                .put(
                        VirtualControlSettingKeys
                                .DISABLE_STICK_CLICK,
                        settings.isStickClickDisabled())
                .put(
                        VirtualControlSettingKeys
                                .AUTOMATIC_SCREEN_ORIENTATION,
                        settings.isAutomaticScreenOrientationEnabled())
                .put(
                        VirtualControlSettingKeys
                                .KEYBOARD_COMBINATION_MODE,
                        settings.isKeyboardCombinationModeEnabled())
                .put(
                        VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID,
                        settings.getKeyboardLayoutId())
                .put(
                        VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID,
                        settings.getGamepadLayoutId())
                .apply();
    }
}
