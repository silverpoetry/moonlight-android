package com.limelight.ui.stream;

import android.os.Handler;

import androidx.annotation.MainThread;

import java.util.Objects;

/**
 * Applies the Activity-scoped UI side effects for one stream session.
 */
public final class StreamSessionUiEffects {
    interface Cancellable {
        void cancel();
    }

    interface DelayedTaskScheduler {
        Cancellable schedule(Runnable task, long delayMs);
    }

    public interface Host {
        @MainThread
        void setKeepScreenOn(boolean keepScreenOn);

        @MainThread
        void notifyStreamConnecting();

        @MainThread
        void notifyStreamConnected();

        @MainThread
        void notifyStreamEnded();

        @MainThread
        void setInputGrabbed(boolean grabbed);
    }

    private enum State {
        IDLE,
        CONNECTING,
        CONNECTED,
        ENDED
    }

    private static final long INPUT_GRAB_DELAY_MS = 500;

    private final Host host;
    private final DelayedTaskScheduler scheduler;
    private State state = State.IDLE;
    private Cancellable pendingInputGrab;

    @MainThread
    public StreamSessionUiEffects(
            Host host,
            Handler mainHandler) {
        this(
                host,
                createScheduler(mainHandler));
    }

    StreamSessionUiEffects(
            Host host,
            DelayedTaskScheduler scheduler) {
        this.host = Objects.requireNonNull(host, "host");
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "scheduler");
    }

    @MainThread
    public void onConnecting() {
        if (state != State.IDLE) {
            return;
        }
        state = State.CONNECTING;
        host.notifyStreamConnecting();
    }

    @MainThread
    public void onConnected() {
        if (state != State.CONNECTING) {
            return;
        }
        state = State.CONNECTED;
        pendingInputGrab = scheduler.schedule(
                this::applyDelayedInputGrab,
                INPUT_GRAB_DELAY_MS);
        host.setKeepScreenOn(true);
        host.notifyStreamConnected();
    }

    @MainThread
    public void onEnded() {
        if (state == State.ENDED) {
            return;
        }
        if (state == State.IDLE) {
            state = State.ENDED;
            return;
        }
        state = State.ENDED;
        cancelPendingInputGrab();
        host.setInputGrabbed(false);
        host.setKeepScreenOn(false);
        host.notifyStreamEnded();
    }

    @MainThread
    public void destroy() {
        onEnded();
    }

    private void applyDelayedInputGrab() {
        pendingInputGrab = null;
        if (state == State.CONNECTED) {
            host.setInputGrabbed(true);
        }
    }

    private void cancelPendingInputGrab() {
        if (pendingInputGrab == null) {
            return;
        }
        pendingInputGrab.cancel();
        pendingInputGrab = null;
    }

    private static DelayedTaskScheduler createScheduler(
            Handler mainHandler) {
        Objects.requireNonNull(mainHandler, "mainHandler");
        return (task, delayMs) -> {
            mainHandler.postDelayed(task, delayMs);
            return () -> mainHandler.removeCallbacks(task);
        };
    }
}
