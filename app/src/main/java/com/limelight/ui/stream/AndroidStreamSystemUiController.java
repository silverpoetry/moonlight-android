package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Objects;

/** Owns edge-to-edge and immersive-system-bar policy for one stream Activity. */
public final class AndroidStreamSystemUiController {
    public interface BooleanValue {
        boolean get();
    }

    private final Activity activity;
    private final View decorView;
    private final WindowInsetsControllerCompat insetsController;
    private final BooleanValue sessionConnected;
    private final Runnable applyScheduledVisibility =
            this::applyCurrentVisibility;

    private boolean destroyed;

    public AndroidStreamSystemUiController(
            Activity activity,
            BooleanValue sessionConnected) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.decorView = activity.getWindow().getDecorView();
        this.insetsController = WindowCompat.getInsetsController(
                activity.getWindow(),
                decorView);
        this.sessionConnected = Objects.requireNonNull(
                sessionConnected,
                "sessionConnected");
    }

    public void attachInitialLayout() {
        ensureActive();
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        applyCurrentVisibility();
    }

    public void onMultiWindowModeChanged(boolean multiWindow) {
        if (destroyed) {
            return;
        }
        scheduleImmersiveMode(50L);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (!destroyed && hasFocus && sessionConnected.get()) {
            scheduleImmersiveMode(250L);
        }
    }

    public void scheduleImmersiveMode(long delayMs) {
        if (destroyed) {
            return;
        }
        Handler handler = decorView.getHandler();
        if (handler == null) {
            return;
        }
        handler.removeCallbacks(applyScheduledVisibility);
        handler.postDelayed(
                applyScheduledVisibility,
                Math.max(0L, delayMs));
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        Handler handler = decorView.getHandler();
        if (handler != null) {
            handler.removeCallbacks(applyScheduledVisibility);
        }
    }

    private void applyCurrentVisibility() {
        if (destroyed) {
            return;
        }
        boolean multiWindow = Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.N &&
                activity.isInMultiWindowMode();
        WindowCompat.setDecorFitsSystemWindows(
                activity.getWindow(),
                multiWindow);
        if (multiWindow) {
            insetsController.show(
                    WindowInsetsCompat.Type.systemBars());
        }
        else {
            insetsController.hide(
                    WindowInsetsCompat.Type.systemBars());
        }
    }

    private void ensureActive() {
        if (destroyed) {
            throw new IllegalStateException(
                    "System UI controller is destroyed");
        }
    }
}
