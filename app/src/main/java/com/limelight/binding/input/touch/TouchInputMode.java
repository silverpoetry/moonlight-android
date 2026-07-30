package com.limelight.binding.input.touch;

/**
 * Stable touchscreen input modes persisted by their historical integer value.
 */
public enum TouchInputMode {
    MULTI_TOUCH(0),
    ABSOLUTE_MOUSE(1),
    NATIVE_TOUCHPAD(2),
    DISABLED(3),
    ABSOLUTE_MOUSE_SWAPPED(4),
    TOUCHPAD_MOVE_ONLY(5),
    TOUCHPAD_MOVE_AND_CLICK(6);

    private final int preferenceValue;

    TouchInputMode(int preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public int getPreferenceValue() {
        return preferenceValue;
    }

    public static TouchInputMode fromPreferenceValue(int value) {
        for (TouchInputMode mode : values()) {
            if (mode.preferenceValue == value) {
                return mode;
            }
        }
        return null;
    }
}
