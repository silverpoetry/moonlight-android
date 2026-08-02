package org.cgutman.shieldcontrollerextensions;

import android.util.Log;

/** Debug-only logging boundary for the isolated SHIELD compatibility module. */
final class ShieldControllerLog {
    private static final String TAG = "ShieldControllerExt";

    private ShieldControllerLog() {
    }

    static void warning(String message, Throwable error) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message, error);
        }
    }
}
