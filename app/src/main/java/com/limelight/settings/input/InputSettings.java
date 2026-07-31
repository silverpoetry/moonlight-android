package com.limelight.settings.input;

/**
 * Immutable pointer and touchscreen policy for one stream session.
 */
public final class InputSettings {
    private final int touchModePreferenceValue;
    private final boolean mouseNavigationButtonsEnabled;
    private final boolean absoluteMouseMode;
    private final boolean barometerForcePressEnabled;
    private final float barometerForcePressThresholdHpa;
    private final int barometerForcePressMinimumDurationMs;
    private final int softKeyboardGestureFingers;
    private final int touchpadPointerSensitivityX;
    private final int touchpadPointerSensitivityY;
    private final int virtualTouchpadSensitivityX;
    private final int virtualTouchpadSensitivityY;
    private final int externalTouchpadSensitivityX;
    private final int externalTouchpadSensitivityY;
    private final int externalTouchpadScrollAmount;
    private final int mouseWheelScrollAmount;
    private final boolean directTouchSensitivityEnabled;
    private final int directTouchSensitivityX;
    private final int directTouchSensitivityY;
    private final boolean directTouchSensitivityGlobal;
    private final boolean directTouchRecenterEnabled;

    private InputSettings(Builder builder) {
        touchModePreferenceValue =
                normalizeTouchMode(builder.touchModePreferenceValue);
        mouseNavigationButtonsEnabled =
                builder.mouseNavigationButtonsEnabled;
        absoluteMouseMode = builder.absoluteMouseMode;
        barometerForcePressEnabled =
                builder.barometerForcePressEnabled;
        barometerForcePressThresholdHpa = clamp(
                builder.barometerForcePressThresholdHpa,
                InputSettingKeys
                        .MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA /
                        1_000f,
                InputSettingKeys
                        .MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA /
                        1_000f);
        barometerForcePressMinimumDurationMs = clamp(
                builder.barometerForcePressMinimumDurationMs,
                0,
                InputSettingKeys
                        .MAX_FORCE_PRESS_MINIMUM_DURATION_MS);
        softKeyboardGestureFingers = normalizeGestureFingerCount(
                builder.softKeyboardGestureFingers);
        touchpadPointerSensitivityX = clampSensitivity(
                builder.touchpadPointerSensitivityX);
        touchpadPointerSensitivityY = clampSensitivity(
                builder.touchpadPointerSensitivityY);
        virtualTouchpadSensitivityX = clampSensitivity(
                builder.virtualTouchpadSensitivityX);
        virtualTouchpadSensitivityY = clampSensitivity(
                builder.virtualTouchpadSensitivityY);
        externalTouchpadSensitivityX = clampSensitivity(
                builder.externalTouchpadSensitivityX);
        externalTouchpadSensitivityY = clampSensitivity(
                builder.externalTouchpadSensitivityY);
        externalTouchpadScrollAmount = clamp(
                builder.externalTouchpadScrollAmount,
                InputSettingKeys.MIN_SCROLL_AMOUNT,
                InputSettingKeys.MAX_SCROLL_AMOUNT);
        mouseWheelScrollAmount = clamp(
                builder.mouseWheelScrollAmount,
                InputSettingKeys.MIN_SCROLL_AMOUNT,
                InputSettingKeys.MAX_SCROLL_AMOUNT);
        directTouchSensitivityEnabled =
                builder.directTouchSensitivityEnabled;
        directTouchSensitivityX = clamp(
                builder.directTouchSensitivityX,
                InputSettingKeys.MIN_SENSITIVITY_PERCENT,
                InputSettingKeys
                        .MAX_DIRECT_TOUCH_SENSITIVITY_PERCENT);
        directTouchSensitivityY = clamp(
                builder.directTouchSensitivityY,
                InputSettingKeys.MIN_SENSITIVITY_PERCENT,
                InputSettingKeys
                        .MAX_DIRECT_TOUCH_SENSITIVITY_PERCENT);
        directTouchSensitivityGlobal =
                builder.directTouchSensitivityGlobal;
        directTouchRecenterEnabled =
                builder.directTouchRecenterEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public int getTouchModePreferenceValue() {
        return touchModePreferenceValue;
    }

    public boolean areMouseNavigationButtonsEnabled() {
        return mouseNavigationButtonsEnabled;
    }

    public boolean isAbsoluteMouseMode() {
        return absoluteMouseMode;
    }

    public boolean isBarometerForcePressEnabled() {
        return barometerForcePressEnabled;
    }

    public float getBarometerForcePressThresholdHpa() {
        return barometerForcePressThresholdHpa;
    }

    public int getBarometerForcePressMinimumDurationMs() {
        return barometerForcePressMinimumDurationMs;
    }

    public int getSoftKeyboardGestureFingers() {
        return softKeyboardGestureFingers;
    }

    public int getTouchpadPointerSensitivityX() {
        return touchpadPointerSensitivityX;
    }

    public int getTouchpadPointerSensitivityY() {
        return touchpadPointerSensitivityY;
    }

    public int getVirtualTouchpadSensitivityX() {
        return virtualTouchpadSensitivityX;
    }

    public int getVirtualTouchpadSensitivityY() {
        return virtualTouchpadSensitivityY;
    }

    public int getExternalTouchpadSensitivityX() {
        return externalTouchpadSensitivityX;
    }

    public int getExternalTouchpadSensitivityY() {
        return externalTouchpadSensitivityY;
    }

    public int getExternalTouchpadScrollAmount() {
        return externalTouchpadScrollAmount;
    }

    public int getMouseWheelScrollAmount() {
        return mouseWheelScrollAmount;
    }

    public boolean isDirectTouchSensitivityEnabled() {
        return directTouchSensitivityEnabled;
    }

    public int getDirectTouchSensitivityX() {
        return directTouchSensitivityX;
    }

    public int getDirectTouchSensitivityY() {
        return directTouchSensitivityY;
    }

    public boolean isDirectTouchSensitivityGlobal() {
        return directTouchSensitivityGlobal;
    }

    public boolean isDirectTouchRecenterEnabled() {
        return directTouchRecenterEnabled;
    }

    private static int normalizeTouchMode(int value) {
        return value >= 0 && value <= 6 ? value : 0;
    }

    private static int normalizeGestureFingerCount(int value) {
        return value >= 3 && value <= 5 ? value : 0;
    }

    private static int clampSensitivity(int value) {
        return clamp(
                value,
                InputSettingKeys.MIN_SENSITIVITY_PERCENT,
                InputSettingKeys.MAX_SENSITIVITY_PERCENT);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum) {
        if (!Float.isFinite(value)) {
            return InputSettingKeys
                    .DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA /
                    1_000f;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Builder {
        private int touchModePreferenceValue;
        private boolean mouseNavigationButtonsEnabled;
        private boolean absoluteMouseMode;
        private boolean barometerForcePressEnabled;
        private float barometerForcePressThresholdHpa =
                InputSettingKeys
                        .DEFAULT_FORCE_PRESS_THRESHOLD_MILLI_HPA /
                        1_000f;
        private int barometerForcePressMinimumDurationMs =
                InputSettingKeys
                        .DEFAULT_FORCE_PRESS_MINIMUM_DURATION_MS;
        private int softKeyboardGestureFingers;
        private int touchpadPointerSensitivityX =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int touchpadPointerSensitivityY =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int virtualTouchpadSensitivityX =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int virtualTouchpadSensitivityY =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int externalTouchpadSensitivityX =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int externalTouchpadSensitivityY =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int externalTouchpadScrollAmount =
                InputSettingKeys.DEFAULT_SCROLL_AMOUNT;
        private int mouseWheelScrollAmount =
                InputSettingKeys.DEFAULT_SCROLL_AMOUNT;
        private boolean directTouchSensitivityEnabled;
        private int directTouchSensitivityX =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private int directTouchSensitivityY =
                InputSettingKeys.DEFAULT_SENSITIVITY_PERCENT;
        private boolean directTouchSensitivityGlobal;
        private boolean directTouchRecenterEnabled = true;

        private Builder() {
        }

        private Builder(InputSettings settings) {
            touchModePreferenceValue =
                    settings.touchModePreferenceValue;
            mouseNavigationButtonsEnabled =
                    settings.mouseNavigationButtonsEnabled;
            absoluteMouseMode = settings.absoluteMouseMode;
            barometerForcePressEnabled =
                    settings.barometerForcePressEnabled;
            barometerForcePressThresholdHpa =
                    settings.barometerForcePressThresholdHpa;
            barometerForcePressMinimumDurationMs =
                    settings.barometerForcePressMinimumDurationMs;
            softKeyboardGestureFingers =
                    settings.softKeyboardGestureFingers;
            touchpadPointerSensitivityX =
                    settings.touchpadPointerSensitivityX;
            touchpadPointerSensitivityY =
                    settings.touchpadPointerSensitivityY;
            virtualTouchpadSensitivityX =
                    settings.virtualTouchpadSensitivityX;
            virtualTouchpadSensitivityY =
                    settings.virtualTouchpadSensitivityY;
            externalTouchpadSensitivityX =
                    settings.externalTouchpadSensitivityX;
            externalTouchpadSensitivityY =
                    settings.externalTouchpadSensitivityY;
            externalTouchpadScrollAmount =
                    settings.externalTouchpadScrollAmount;
            mouseWheelScrollAmount =
                    settings.mouseWheelScrollAmount;
            directTouchSensitivityEnabled =
                    settings.directTouchSensitivityEnabled;
            directTouchSensitivityX =
                    settings.directTouchSensitivityX;
            directTouchSensitivityY =
                    settings.directTouchSensitivityY;
            directTouchSensitivityGlobal =
                    settings.directTouchSensitivityGlobal;
            directTouchRecenterEnabled =
                    settings.directTouchRecenterEnabled;
        }

        public Builder setTouchModePreferenceValue(int value) {
            touchModePreferenceValue = value;
            return this;
        }

        public Builder setMouseNavigationButtonsEnabled(
                boolean enabled) {
            mouseNavigationButtonsEnabled = enabled;
            return this;
        }

        public Builder setAbsoluteMouseMode(boolean enabled) {
            absoluteMouseMode = enabled;
            return this;
        }

        public Builder setBarometerForcePressEnabled(
                boolean enabled) {
            barometerForcePressEnabled = enabled;
            return this;
        }

        public Builder setBarometerForcePressThresholdHpa(
                float thresholdHpa) {
            barometerForcePressThresholdHpa = thresholdHpa;
            return this;
        }

        public Builder setBarometerForcePressMinimumDurationMs(
                int durationMs) {
            barometerForcePressMinimumDurationMs = durationMs;
            return this;
        }

        public Builder setSoftKeyboardGestureFingers(
                int fingerCount) {
            softKeyboardGestureFingers = fingerCount;
            return this;
        }

        public Builder setTouchpadPointerSensitivity(
                int x,
                int y) {
            touchpadPointerSensitivityX = x;
            touchpadPointerSensitivityY = y;
            return this;
        }

        public Builder setVirtualTouchpadSensitivity(
                int x,
                int y) {
            virtualTouchpadSensitivityX = x;
            virtualTouchpadSensitivityY = y;
            return this;
        }

        public Builder setExternalTouchpadSensitivity(
                int x,
                int y) {
            externalTouchpadSensitivityX = x;
            externalTouchpadSensitivityY = y;
            return this;
        }

        public Builder setExternalTouchpadScrollAmount(
                int amount) {
            externalTouchpadScrollAmount = amount;
            return this;
        }

        public Builder setMouseWheelScrollAmount(int amount) {
            mouseWheelScrollAmount = amount;
            return this;
        }

        public Builder setDirectTouchSensitivityEnabled(
                boolean enabled) {
            directTouchSensitivityEnabled = enabled;
            return this;
        }

        public Builder setDirectTouchSensitivity(
                int x,
                int y) {
            directTouchSensitivityX = x;
            directTouchSensitivityY = y;
            return this;
        }

        public Builder setDirectTouchSensitivityGlobal(
                boolean global) {
            directTouchSensitivityGlobal = global;
            return this;
        }

        public Builder setDirectTouchRecenterEnabled(
                boolean enabled) {
            directTouchRecenterEnabled = enabled;
            return this;
        }

        public InputSettings build() {
            return new InputSettings(this);
        }
    }
}
