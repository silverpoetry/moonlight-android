package com.limelight.utils;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Objects;

/** Android-version compatibility for short local UI vibration pulses. */
public final class AndroidVibratorCompat {
    private AndroidVibratorCompat() {
    }

    public static void vibrateOneShot(Vibrator vibrator, long durationMs) {
        Objects.requireNonNull(vibrator, "vibrator");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrateBeforeOreo(vibrator, durationMs);
        }
    }

    /** API 23-25 compatibility path; VibrationEffect starts at API 26. */
    @SuppressWarnings("deprecation")
    private static void vibrateBeforeOreo(
            Vibrator vibrator,
            long durationMs) {
        vibrator.vibrate(durationMs);
    }
}
