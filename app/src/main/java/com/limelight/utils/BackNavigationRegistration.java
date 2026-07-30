package com.limelight.utils;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Lifecycle handle for a custom system-back action.
 *
 * Activities that need non-default back behavior register the same action with the
 * platform dispatcher on Android 13+ and keep their {@code onBackPressed()} fallback
 * for older releases. Calling {@link #unregister()} more than once is safe.
 */
public final class BackNavigationRegistration {
    private final Activity activity;
    private final Dialog dialog;
    private Object platformCallback;

    private BackNavigationRegistration(Activity activity,
                                       Dialog dialog,
                                       Object platformCallback) {
        this.activity = activity;
        this.dialog = dialog;
        this.platformCallback = platformCallback;
    }

    @Nullable
    public static BackNavigationRegistration register(Activity activity, Runnable action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null;
        }
        return new BackNavigationRegistration(
                activity, null, Api33.register(activity, action));
    }

    @Nullable
    public static BackNavigationRegistration register(Dialog dialog, Runnable action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null;
        }
        return new BackNavigationRegistration(
                null, dialog, Api33.register(dialog, action));
    }

    public void unregister() {
        if (platformCallback == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity != null) {
                Api33.unregister(activity, platformCallback);
            }
            else {
                Api33.unregister(dialog, platformCallback);
            }
        }
        platformCallback = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static final class Api33 {
        private Api33() {
        }

        static Object register(Activity activity, Runnable action) {
            OnBackInvokedCallback callback = action::run;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        static Object register(Dialog dialog, Runnable action) {
            OnBackInvokedCallback callback = action::run;
            dialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        static void unregister(Activity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (OnBackInvokedCallback) callback);
        }

        static void unregister(Dialog dialog, Object callback) {
            dialog.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (OnBackInvokedCallback) callback);
        }
    }
}
