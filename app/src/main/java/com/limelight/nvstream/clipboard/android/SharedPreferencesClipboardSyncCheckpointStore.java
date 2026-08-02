package com.limelight.nvstream.clipboard.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.nvstream.clipboard.ClipboardSyncCheckpoint;
import com.limelight.nvstream.clipboard.ClipboardSyncCheckpointStore;

import java.util.Objects;

/** Android persistence adapter for clipboard loop-suppression state. */
public final class SharedPreferencesClipboardSyncCheckpointStore
        implements ClipboardSyncCheckpointStore {
    static final String PREFERENCES_NAME = "clipboard_sync_state";
    static final String LAST_HANDLED_KEY = "last_handled_key";

    private final SharedPreferences preferences;

    public SharedPreferencesClipboardSyncCheckpointStore(Context context) {
        Objects.requireNonNull(context, "context");
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public ClipboardSyncCheckpoint read() {
        if (!preferences.contains(LAST_HANDLED_KEY)) {
            return ClipboardSyncCheckpoint.uninitialized();
        }
        String fingerprint;
        try {
            fingerprint = preferences.getString(LAST_HANDLED_KEY, null);
        }
        catch (ClassCastException error) {
            return ClipboardSyncCheckpoint.uninitialized();
        }
        if (!ClipboardSyncCheckpoint.isValidFingerprint(fingerprint)) {
            return ClipboardSyncCheckpoint.uninitialized();
        }
        return ClipboardSyncCheckpoint.initialized(fingerprint);
    }

    @Override
    public void write(ClipboardSyncCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (!checkpoint.isInitialized()) {
            throw new IllegalArgumentException(
                    "Only initialized checkpoints can be persisted");
        }
        preferences.edit()
                .putString(
                        LAST_HANDLED_KEY,
                        checkpoint.getLastHandledFingerprint())
                .apply();
    }
}
