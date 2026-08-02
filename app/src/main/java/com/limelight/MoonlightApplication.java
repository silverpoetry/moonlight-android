package com.limelight;

import android.app.Application;

import com.limelight.migration.LegacyDataImporter;
import com.limelight.stream.launch.PendingStreamReconnectStore;

/** Narrow process composition root for cross-Activity stream handoff state. */
public final class MoonlightApplication extends Application {
    private final PendingStreamReconnectStore pendingStreamReconnectStore =
            new PendingStreamReconnectStore();

    @Override
    public void onCreate() {
        super.onCreate();
        LegacyDataImporter.importIfAvailable(this);
    }

    public PendingStreamReconnectStore getPendingStreamReconnectStore() {
        return pendingStreamReconnectStore;
    }
}
