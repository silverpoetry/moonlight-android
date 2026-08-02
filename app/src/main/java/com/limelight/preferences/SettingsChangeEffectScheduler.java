package com.limelight.preferences;

import android.os.Handler;
import android.os.Looper;

import java.util.Objects;

/** Lifecycle-bound main-thread scheduler for settings refresh and reload. */
final class SettingsChangeEffectScheduler {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reloadAction;
    private final Runnable refreshAction;
    private final Runnable recreateAction;
    private Runnable pendingReload;
    private Runnable pendingRefresh;
    private boolean destroyed;

    SettingsChangeEffectScheduler(
            Runnable reloadAction,
            Runnable refreshAction,
            Runnable recreateAction) {
        this.reloadAction = Objects.requireNonNull(
                reloadAction,
                "reloadAction");
        this.refreshAction = Objects.requireNonNull(
                refreshAction,
                "refreshAction");
        this.recreateAction = Objects.requireNonNull(
                recreateAction,
                "recreateAction");
    }

    void schedule(SettingsMutationController.ChangeEffect effect) {
        Objects.requireNonNull(effect, "effect");
        if (destroyed) {
            return;
        }
        switch (effect.getType()) {
            case RELOAD:
                scheduleReload(effect.getDelayMs());
                break;
            case REFRESH:
                scheduleRefresh(effect.getDelayMs());
                break;
            case RECREATE:
                scheduleRecreate(effect.getDelayMs());
                break;
            default:
                throw new AssertionError("Unhandled settings change effect");
        }
    }

    void destroy() {
        destroyed = true;
        if (pendingReload != null) {
            handler.removeCallbacks(pendingReload);
            pendingReload = null;
        }
        if (pendingRefresh != null) {
            handler.removeCallbacks(pendingRefresh);
            pendingRefresh = null;
        }
    }

    private void scheduleReload(long delayMs) {
        if (pendingReload != null) {
            handler.removeCallbacks(pendingReload);
        }
        pendingReload = new Runnable() {
            @Override
            public void run() {
                if (pendingReload != this || destroyed) {
                    return;
                }
                pendingReload = null;
                if (pendingRefresh != null) {
                    handler.removeCallbacks(pendingRefresh);
                    pendingRefresh = null;
                }
                reloadAction.run();
            }
        };
        dispatch(pendingReload, delayMs);
    }

    private void scheduleRefresh(long delayMs) {
        if (pendingRefresh != null) {
            handler.removeCallbacks(pendingRefresh);
        }
        pendingRefresh = new Runnable() {
            @Override
            public void run() {
                if (pendingRefresh != this || destroyed) {
                    return;
                }
                pendingRefresh = null;
                refreshAction.run();
            }
        };
        dispatch(pendingRefresh, delayMs);
    }

    private void scheduleRecreate(long delayMs) {
        if (pendingReload != null) {
            handler.removeCallbacks(pendingReload);
        }
        if (pendingRefresh != null) {
            handler.removeCallbacks(pendingRefresh);
            pendingRefresh = null;
        }
        pendingReload = new Runnable() {
            @Override
            public void run() {
                if (pendingReload != this || destroyed) {
                    return;
                }
                pendingReload = null;
                recreateAction.run();
            }
        };
        dispatch(pendingReload, delayMs);
    }

    private void dispatch(Runnable action, long delayMs) {
        if (delayMs <= 0) {
            action.run();
        }
        else {
            handler.postDelayed(action, delayMs);
        }
    }
}
