package com.limelight;

import android.app.Application;

import com.limelight.stream.launch.PendingStreamReconnectStore;

/** Narrow process composition root for cross-Activity stream handoff state. */
public final class MoonlightApplication extends Application {
    private final PendingStreamReconnectStore pendingStreamReconnectStore =
            new PendingStreamReconnectStore();

    public PendingStreamReconnectStore getPendingStreamReconnectStore() {
        return pendingStreamReconnectStore;
    }
}
