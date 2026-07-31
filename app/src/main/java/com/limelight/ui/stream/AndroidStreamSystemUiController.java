package com.limelight.ui.stream;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import java.util.Objects;

/** Owns the legacy immersive-window lifecycle for one stream Activity. */
public final class AndroidStreamSystemUiController
        implements View.OnSystemUiVisibilityChangeListener {
    public interface BooleanValue {
        boolean get();
    }

    private static final int INITIAL_LAYOUT_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private static final int IMMERSIVE_FLAGS =
            INITIAL_LAYOUT_FLAGS |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private static final long VISIBILITY_RESTORE_DELAY_MS = 2_000L;

    private final Activity activity;
    private final View decorView;
    private final BooleanValue sessionConnected;
    private final Runnable applyScheduledVisibility =
            this::applyCurrentVisibility;

    private boolean destroyed;

    public AndroidStreamSystemUiController(
            Activity activity,
            BooleanValue sessionConnected) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.decorView = activity.getWindow().getDecorView();
        this.sessionConnected = Objects.requireNonNull(
                sessionConnected,
                "sessionConnected");
    }

    public void attachInitialLayout() {
        ensureActive();
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams
                                .FLAG_LAYOUT_IN_SCREEN);
        decorView.setSystemUiVisibility(INITIAL_LAYOUT_FLAGS);
        decorView.setOnSystemUiVisibilityChangeListener(this);
    }

    public void onMultiWindowModeChanged(boolean multiWindow) {
        if (destroyed) {
            return;
        }
        if (multiWindow) {
            activity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        scheduleImmersiveMode(50L);
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

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        boolean fullscreenVisible =
                (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        boolean navigationHidden =
                (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
        if (StreamSystemUiVisibilityPolicy.shouldRestoreImmersiveMode(
                sessionConnected.get(),
                fullscreenVisible,
                navigationHidden)) {
            scheduleImmersiveMode(VISIBILITY_RESTORE_DELAY_MS);
        }
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
        decorView.setOnSystemUiVisibilityChangeListener(null);
    }

    private void applyCurrentVisibility() {
        if (destroyed) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                activity.isInMultiWindowMode()) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        else {
            decorView.setSystemUiVisibility(IMMERSIVE_FLAGS);
        }
    }

    private void ensureActive() {
        if (destroyed) {
            throw new IllegalStateException(
                    "System UI controller is destroyed");
        }
    }
}
