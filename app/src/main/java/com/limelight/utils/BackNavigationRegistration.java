package com.limelight.utils;

import androidx.activity.ComponentActivity;
import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;

/**
 * Lifecycle handle for one custom system-back action.
 *
 * <p>Activities and dialogs register a single AndroidX dispatcher callback
 * that covers legacy back presses and predictive system-back gestures.
 * Calling {@link #unregister()} more than once is safe.</p>
 */
public final class BackNavigationRegistration {
    private OnBackPressedCallback callback;

    private BackNavigationRegistration(OnBackPressedCallback callback) {
        this.callback = callback;
    }

    public static BackNavigationRegistration register(
            ComponentActivity activity,
            Runnable action) {
        OnBackPressedCallback callback = createCallback(action);
        activity.getOnBackPressedDispatcher().addCallback(activity, callback);
        return new BackNavigationRegistration(callback);
    }

    public static BackNavigationRegistration register(
            ComponentDialog dialog,
            Runnable action) {
        OnBackPressedCallback callback = createCallback(action);
        dialog.getOnBackPressedDispatcher().addCallback(callback);
        return new BackNavigationRegistration(callback);
    }

    public void unregister() {
        if (callback == null) {
            return;
        }
        callback.remove();
        callback = null;
    }

    private static OnBackPressedCallback createCallback(Runnable action) {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                action.run();
            }
        };
    }
}
