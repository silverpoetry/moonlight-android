package com.limelight.binding.input.capture;

import android.app.Activity;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.binding.input.evdev.EvdevListener;

import java.lang.reflect.Method;
import java.util.Objects;

/** Owns pointer capture, local cursor visibility, and system-key capture. */
public final class AndroidStreamInputCaptureController {
    private final Activity activity;
    private final InputCaptureProvider provider;
    private final StreamInputCaptureState state =
            new StreamInputCaptureState();

    private boolean captureStateApplied;
    private boolean destroyed;

    private AndroidStreamInputCaptureController(
            Activity activity,
            InputCaptureProvider provider) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @MainThread
    public static AndroidStreamInputCaptureController create(
            Activity activity,
            EvdevListener evdevListener) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(evdevListener, "evdevListener");
        return new AndroidStreamInputCaptureController(
                activity,
                InputCaptureManager.getInputCaptureProvider(
                        activity,
                        evdevListener));
    }

    public InputCaptureProvider getProvider() {
        return provider;
    }

    public boolean isInputGrabbed() {
        return !destroyed && state.isInputGrabbed();
    }

    public boolean isLocalCursorVisible() {
        return !destroyed && state.isLocalCursorVisible();
    }

    @MainThread
    public void setInputGrabbed(boolean grabbed) {
        if (destroyed) {
            return;
        }
        applyInputGrab(grabbed);
        state.setInputGrabbed(grabbed);
        captureStateApplied = true;
    }

    @MainThread
    public void toggleInputGrabbed() {
        setInputGrabbed(!state.isInputGrabbed());
    }

    @MainThread
    public void toggleLocalCursorVisibility() {
        if (destroyed) {
            return;
        }
        boolean wasGrabbed = state.isInputGrabbed();
        state.toggleLocalCursorVisibility();
        if (!wasGrabbed) {
            provider.enableCapture();
            setSamsungMetaKeyCaptureEnabled(true);
            captureStateApplied = true;
        }
        applyCursorPreference();
    }

    @MainThread
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!destroyed) {
            provider.onWindowFocusChanged(hasFocus);
        }
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        if (captureStateApplied && state.isInputGrabbed()) {
            provider.disableCapture();
            setSamsungMetaKeyCaptureEnabled(false);
            state.setInputGrabbed(false);
        }
        provider.destroy();
        destroyed = true;
    }

    private void applyInputGrab(boolean grabbed) {
        if (grabbed) {
            provider.enableCapture();
            applyCursorPreference();
        }
        else {
            provider.disableCapture();
        }
        setSamsungMetaKeyCaptureEnabled(grabbed);
    }

    private void applyCursorPreference() {
        if (state.isLocalCursorVisible()) {
            provider.showCursor();
        }
        else {
            provider.hideCursor();
        }
    }

    private void setSamsungMetaKeyCaptureEnabled(boolean enabled) {
        try {
            Class<?> managerClass = Class.forName(
                    "com.samsung.android.view.SemWindowManager");
            Method getInstance = managerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);
            if (manager == null) {
                LimeLog.warning(
                        "SemWindowManager.getInstance() returned null");
                return;
            }
            Method requestMetaKeyEvent = managerClass.getDeclaredMethod(
                    "requestMetaKeyEvent",
                    android.content.ComponentName.class,
                    boolean.class);
            requestMetaKeyEvent.invoke(
                    manager,
                    activity.getComponentName(),
                    enabled);
        }
        catch (ClassNotFoundException ignored) {
            // This API exists only on supported Samsung devices.
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            LimeLog.warning(
                    "Unable to update Samsung meta-key capture: " +
                            exception.getClass().getSimpleName());
        }
    }
}
