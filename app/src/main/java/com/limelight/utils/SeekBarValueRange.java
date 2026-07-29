package com.limelight.utils;

/**
 * Maps a logical value range onto the zero-based progress range supported by
 * {@link android.widget.SeekBar} on every Android version supported by the app.
 */
public final class SeekBarValueRange {
    private final int minimum;
    private final int maximum;

    public SeekBarValueRange(int minimum, int maximum) {
        if (maximum < minimum) {
            throw new IllegalArgumentException(
                    "maximum must be greater than or equal to minimum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public int getProgressMaximum() {
        return maximum - minimum;
    }

    public int valueToProgress(int value) {
        return clamp(value, minimum, maximum) - minimum;
    }

    public int progressToValue(int progress) {
        return clamp(progress, 0, getProgressMaximum()) + minimum;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
