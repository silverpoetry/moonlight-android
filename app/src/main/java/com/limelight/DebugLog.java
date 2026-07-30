package com.limelight;

import android.util.Log;

/**
 * Debug-only facade for code that needs Android's tagged logging API.
 *
 * <p>Release builds intentionally emit no diagnostic log traffic. Keeping the gate in
 * one facade prevents high-rate render, input, or audio paths from bypassing that policy.
 */
public final class DebugLog {
    private static final boolean ENABLED = BuildConfig.DEBUG;

    private DebugLog() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static int debug(String tag, String message) {
        return ENABLED ? Log.d(tag, message) : 0;
    }

    public static int info(String tag, String message) {
        return ENABLED ? Log.i(tag, message) : 0;
    }

    public static int warning(String tag, String message) {
        return ENABLED ? Log.w(tag, message) : 0;
    }

    public static int warning(String tag, String message, Throwable error) {
        return ENABLED ? Log.w(tag, message, error) : 0;
    }

    public static int error(String tag, String message) {
        return ENABLED ? Log.e(tag, message) : 0;
    }

    public static int error(String tag, String message, Throwable error) {
        return ENABLED ? Log.e(tag, message, error) : 0;
    }
}
