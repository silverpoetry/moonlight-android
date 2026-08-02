package com.limelight.settings.virtualcontrols;

import com.limelight.virtualcontrols.layout.VirtualControlLayoutKind;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutProfiles;

import java.util.Objects;

/**
 * Immutable rendering, interaction, and layout-selection policy for the
 * on-screen gamepad, virtual keys, and full keyboard.
 */
public final class VirtualControlSettings {
    private final int controlOpacityPercent;
    private final int keyboardOpacityPercent;
    private final int keyboardHeightDp;
    private final boolean keyboardHapticsEnabled;
    private final boolean showVirtualKeysOnStart;
    private final int normalColor;
    private final int gamepadScalePercent;
    private final boolean stickClickDisabled;
    private final boolean automaticScreenOrientationEnabled;
    private final boolean keyboardCombinationModeEnabled;
    private final String keyboardLayoutId;
    private final String gamepadLayoutId;

    private VirtualControlSettings(Builder builder) {
        controlOpacityPercent = clampPercent(
                builder.controlOpacityPercent);
        keyboardOpacityPercent = clampPercent(
                builder.keyboardOpacityPercent);
        keyboardHeightDp = clamp(
                builder.keyboardHeightDp,
                100,
                400);
        keyboardHapticsEnabled = builder.keyboardHapticsEnabled;
        showVirtualKeysOnStart =
                builder.showVirtualKeysOnStart;
        normalColor = builder.normalColor;
        gamepadScalePercent = clamp(
                builder.gamepadScalePercent,
                20,
                180);
        stickClickDisabled = builder.stickClickDisabled;
        automaticScreenOrientationEnabled =
                builder.automaticScreenOrientationEnabled;
        keyboardCombinationModeEnabled =
                builder.keyboardCombinationModeEnabled;
        keyboardLayoutId = requireLayoutId(
                builder.keyboardLayoutId,
                VirtualControlLayoutKind.KEYBOARD,
                VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID
                        .getDefaultValue());
        gamepadLayoutId = requireLayoutId(
                builder.gamepadLayoutId,
                VirtualControlLayoutKind.GAMEPAD,
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID
                        .getDefaultValue());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public int getControlOpacityPercent() {
        return controlOpacityPercent;
    }

    public int getKeyboardOpacityPercent() {
        return keyboardOpacityPercent;
    }

    public int getKeyboardHeightDp() {
        return keyboardHeightDp;
    }

    public boolean isKeyboardHapticsEnabled() {
        return keyboardHapticsEnabled;
    }

    public boolean shouldShowVirtualKeysOnStart() {
        return showVirtualKeysOnStart;
    }

    public int getNormalColor() {
        return normalColor;
    }

    public int getGamepadScalePercent() {
        return gamepadScalePercent;
    }

    public boolean isStickClickDisabled() {
        return stickClickDisabled;
    }

    public boolean isAutomaticScreenOrientationEnabled() {
        return automaticScreenOrientationEnabled;
    }

    public boolean isKeyboardCombinationModeEnabled() {
        return keyboardCombinationModeEnabled;
    }

    public String getKeyboardLayoutId() {
        return keyboardLayoutId;
    }

    public String getGamepadLayoutId() {
        return gamepadLayoutId;
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String requireLayoutId(
            String value,
            VirtualControlLayoutKind kind,
            String defaultValue) {
        if (!VirtualControlLayoutProfiles.isKnown(kind, value)) {
            return defaultValue;
        }
        return value;
    }

    public static final class Builder {
        private int controlOpacityPercent =
                VirtualControlSettingKeys.DEFAULT_OPACITY_PERCENT;
        private int keyboardOpacityPercent =
                VirtualControlSettingKeys.DEFAULT_OPACITY_PERCENT;
        private int keyboardHeightDp =
                VirtualControlSettingKeys.DEFAULT_KEYBOARD_HEIGHT_DP;
        private boolean keyboardHapticsEnabled;
        private boolean showVirtualKeysOnStart;
        private int normalColor =
                VirtualControlSettingKeys.DEFAULT_NORMAL_COLOR;
        private int gamepadScalePercent =
                VirtualControlSettingKeys.DEFAULT_GAMEPAD_SCALE_PERCENT;
        private boolean stickClickDisabled;
        private boolean automaticScreenOrientationEnabled;
        private boolean keyboardCombinationModeEnabled;
        private String keyboardLayoutId =
                VirtualControlSettingKeys.KEYBOARD_LAYOUT_ID
                        .getDefaultValue();
        private String gamepadLayoutId =
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID
                        .getDefaultValue();

        private Builder() {
        }

        private Builder(VirtualControlSettings settings) {
            controlOpacityPercent = settings.controlOpacityPercent;
            keyboardOpacityPercent = settings.keyboardOpacityPercent;
            keyboardHeightDp = settings.keyboardHeightDp;
            keyboardHapticsEnabled =
                    settings.keyboardHapticsEnabled;
            showVirtualKeysOnStart =
                    settings.showVirtualKeysOnStart;
            normalColor = settings.normalColor;
            gamepadScalePercent = settings.gamepadScalePercent;
            stickClickDisabled = settings.stickClickDisabled;
            automaticScreenOrientationEnabled =
                    settings.automaticScreenOrientationEnabled;
            keyboardCombinationModeEnabled =
                    settings.keyboardCombinationModeEnabled;
            keyboardLayoutId = settings.keyboardLayoutId;
            gamepadLayoutId = settings.gamepadLayoutId;
        }

        public Builder setControlOpacityPercent(int value) {
            controlOpacityPercent = value;
            return this;
        }

        public Builder setKeyboardOpacityPercent(int value) {
            keyboardOpacityPercent = value;
            return this;
        }

        public Builder setKeyboardHeightDp(int value) {
            keyboardHeightDp = value;
            return this;
        }

        public Builder setKeyboardHapticsEnabled(boolean enabled) {
            keyboardHapticsEnabled = enabled;
            return this;
        }

        public Builder setShowVirtualKeysOnStart(boolean show) {
            showVirtualKeysOnStart = show;
            return this;
        }

        public Builder setNormalColor(int value) {
            normalColor = value;
            return this;
        }

        public Builder setGamepadScalePercent(int value) {
            gamepadScalePercent = value;
            return this;
        }

        public Builder setStickClickDisabled(boolean disabled) {
            stickClickDisabled = disabled;
            return this;
        }

        public Builder setAutomaticScreenOrientationEnabled(
                boolean enabled) {
            automaticScreenOrientationEnabled = enabled;
            return this;
        }

        public Builder setKeyboardCombinationModeEnabled(
                boolean enabled) {
            keyboardCombinationModeEnabled = enabled;
            return this;
        }

        public Builder setKeyboardLayoutId(String value) {
            keyboardLayoutId = Objects.requireNonNull(
                    value,
                    "keyboardLayoutId");
            return this;
        }

        public Builder setGamepadLayoutId(String value) {
            gamepadLayoutId = Objects.requireNonNull(
                    value,
                    "gamepadLayoutId");
            return this;
        }

        public VirtualControlSettings build() {
            return new VirtualControlSettings(this);
        }
    }
}
