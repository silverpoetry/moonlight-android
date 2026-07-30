package com.limelight.ui.stream;

import androidx.annotation.MainThread;

import java.util.Objects;

/**
 * Applies the Activity-scoped UI side effects for one stream session.
 */
public final class StreamSessionUiEffects {
    public interface Host {
        @MainThread
        void setKeepScreenOn(boolean keepScreenOn);

        @MainThread
        void notifyStreamConnecting();

        @MainThread
        void notifyStreamConnected();

        @MainThread
        void notifyStreamEnded();
    }

    private enum State {
        IDLE,
        CONNECTING,
        CONNECTED,
        ENDED
    }

    private final Host host;
    private State state = State.IDLE;

    @MainThread
    public StreamSessionUiEffects(Host host) {
        this.host = Objects.requireNonNull(host, "host");
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
        host.setKeepScreenOn(false);
        host.notifyStreamEnded();
    }

    @MainThread
    public void destroy() {
        onEnded();
    }
}
