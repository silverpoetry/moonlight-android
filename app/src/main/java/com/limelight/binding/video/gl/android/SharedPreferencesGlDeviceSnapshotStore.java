package com.limelight.binding.video.gl.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.binding.video.gl.GlDeviceSnapshot;
import com.limelight.binding.video.gl.GlDeviceSnapshotStore;

import java.util.Map;
import java.util.Objects;

/** Android persistence adapter for the build-scoped GL renderer cache. */
public final class SharedPreferencesGlDeviceSnapshotStore
        implements GlDeviceSnapshotStore {
    // These exact names preserve the historical cache without a migration.
    static final String PREFERENCES_NAME = "GlPreferences";
    static final String BUILD_FINGERPRINT_KEY = "Fingerprint";
    static final String RENDERER_KEY = "Renderer";

    private final SharedPreferences preferences;

    public SharedPreferencesGlDeviceSnapshotStore(Context context) {
        Objects.requireNonNull(context, "context");
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE);
    }

    @Override
    public GlDeviceSnapshot read() {
        Map<String, ?> stored = preferences.getAll();
        Object buildFingerprint = stored.get(BUILD_FINGERPRINT_KEY);
        Object renderer = stored.get(RENDERER_KEY);
        if (!(buildFingerprint instanceof String) ||
                !(renderer instanceof String)) {
            return GlDeviceSnapshot.unavailable();
        }
        return GlDeviceSnapshot.fromUntrusted(
                (String) buildFingerprint,
                (String) renderer);
    }

    @Override
    public void replace(GlDeviceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        SharedPreferences.Editor editor = preferences.edit();
        if (snapshot.isAvailable()) {
            editor.putString(
                            BUILD_FINGERPRINT_KEY,
                            snapshot.getBuildFingerprint())
                    .putString(RENDERER_KEY, snapshot.getRenderer());
        }
        else {
            editor.remove(BUILD_FINGERPRINT_KEY)
                    .remove(RENDERER_KEY);
        }
        editor.apply();
    }
}
