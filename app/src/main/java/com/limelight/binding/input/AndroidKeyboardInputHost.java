package com.limelight.binding.input;

import android.app.Activity;
import android.os.Handler;

import com.limelight.binding.input.capture.AndroidStreamInputCaptureController;

import java.util.Objects;

/** Android actions requested by the keyboard input state machine. */
public final class AndroidKeyboardInputHost
        implements KeyboardInputController.Host {
    interface GrabState {
        boolean isGrabbed();
    }

    interface DelayedActionScheduler {
        void schedule(Runnable action, long delayMs);
    }

    private static final long TOGGLE_GRAB_DELAY_MS = 250L;

    private final GrabState grabState;
    private final DelayedActionScheduler scheduler;
    private final Runnable nonBackKeyDown;
    private final Runnable toggleInputGrab;
    private final Runnable quit;
    private final Runnable toggleCursorVisibility;

    public AndroidKeyboardInputHost(
            Activity activity,
            AndroidStreamInputCaptureController inputCaptureController,
            Runnable nonBackKeyDown,
            Runnable toggleInputGrab,
            Runnable toggleCursorVisibility) {
        Activity owner = Objects.requireNonNull(activity, "activity");
        AndroidStreamInputCaptureController captureController =
                Objects.requireNonNull(
                        inputCaptureController,
                        "inputCaptureController");
        this.grabState = captureController::isInputGrabbed;
        this.scheduler = (action, delayMs) -> {
            Handler handler = owner.getWindow()
                    .getDecorView()
                    .getHandler();
            if (handler != null) {
                handler.postDelayed(action, delayMs);
            }
        };
        this.nonBackKeyDown = Objects.requireNonNull(
                nonBackKeyDown,
                "nonBackKeyDown");
        this.toggleInputGrab = Objects.requireNonNull(
                toggleInputGrab,
                "toggleInputGrab");
        this.quit = owner::finish;
        this.toggleCursorVisibility = Objects.requireNonNull(
                toggleCursorVisibility,
                "toggleCursorVisibility");
    }

    AndroidKeyboardInputHost(
            GrabState grabState,
            DelayedActionScheduler scheduler,
            Runnable nonBackKeyDown,
            Runnable toggleInputGrab,
            Runnable quit,
            Runnable toggleCursorVisibility) {
        this.grabState = Objects.requireNonNull(grabState, "grabState");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.nonBackKeyDown = Objects.requireNonNull(
                nonBackKeyDown,
                "nonBackKeyDown");
        this.toggleInputGrab = Objects.requireNonNull(
                toggleInputGrab,
                "toggleInputGrab");
        this.quit = Objects.requireNonNull(quit, "quit");
        this.toggleCursorVisibility = Objects.requireNonNull(
                toggleCursorVisibility,
                "toggleCursorVisibility");
    }

    @Override
    public boolean isInputGrabbed() {
        return grabState.isGrabbed();
    }

    @Override
    public void onNonBackKeyDown() {
        nonBackKeyDown.run();
    }

    @Override
    public void requestToggleInputGrab() {
        scheduler.schedule(toggleInputGrab, TOGGLE_GRAB_DELAY_MS);
    }

    @Override
    public void requestQuit() {
        quit.run();
    }

    @Override
    public void requestToggleCursorVisibility() {
        toggleCursorVisibility.run();
    }
}
