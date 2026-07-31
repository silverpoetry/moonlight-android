package com.limelight.binding.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

/** SharedPreferences persistence adapter for decoder crash recovery. */
public final class AndroidDecoderCrashStore
        implements DecoderCrashTracker.Store {
    public static final String PREFERENCES_NAME = "DecoderTombstone";

    private static final String CRASH_COUNT = "CrashCount";
    private static final String LAST_NOTIFIED_CRASH_COUNT =
            "LastNotifiedCrashCount";

    private final SharedPreferences preferences;

    public AndroidDecoderCrashStore(Context context) {
        Objects.requireNonNull(context, "context");
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public int getCrashCount() {
        return preferences.getInt(CRASH_COUNT, 0);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void recordCrashSynchronously() {
        preferences.edit()
                .putInt(CRASH_COUNT, getCrashCount() + 1)
                .commit();
    }

    @Override
    public void clearCrashHistory() {
        preferences.edit()
                .putInt(CRASH_COUNT, 0)
                .putInt(LAST_NOTIFIED_CRASH_COUNT, 0)
                .apply();
    }
}
