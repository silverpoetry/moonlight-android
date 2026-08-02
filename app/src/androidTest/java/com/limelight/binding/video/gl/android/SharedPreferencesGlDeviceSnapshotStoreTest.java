package com.limelight.binding.video.gl.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.binding.video.gl.GlDeviceSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class SharedPreferencesGlDeviceSnapshotStoreTest {
    private static final String FINGERPRINT =
            "vendor/device/product:version/build:user/release-keys";
    private static final String RENDERER = "Adreno (TM) 750";
    private static final String UNRELATED_KEY = "unrelated";

    private Context context;
    private SharedPreferences preferences;

    @Before
    public void clearBefore() {
        context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                SharedPreferencesGlDeviceSnapshotStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @After
    public void clearAfter() {
        preferences.edit().clear().commit();
    }

    @Test
    public void roundTripPreservesHistoricalAtomicPair() {
        SharedPreferencesGlDeviceSnapshotStore store =
                new SharedPreferencesGlDeviceSnapshotStore(context);

        assertFalse(store.read().isAvailable());
        store.replace(GlDeviceSnapshot.available(
                FINGERPRINT,
                RENDERER));

        GlDeviceSnapshot restored =
                new SharedPreferencesGlDeviceSnapshotStore(context)
                        .read();
        assertTrue(restored.isAvailable());
        assertEquals(FINGERPRINT, restored.getBuildFingerprint());
        assertEquals(RENDERER, restored.getRenderer());
        assertEquals(
                FINGERPRINT,
                preferences.getString(
                        SharedPreferencesGlDeviceSnapshotStore
                                .BUILD_FINGERPRINT_KEY,
                        null));
        assertEquals(
                RENDERER,
                preferences.getString(
                        SharedPreferencesGlDeviceSnapshotStore.RENDERER_KEY,
                        null));
    }

    @Test
    public void partialAndWrongTypedPairsFailClosed() {
        preferences.edit()
                .putString(
                        SharedPreferencesGlDeviceSnapshotStore
                                .BUILD_FINGERPRINT_KEY,
                        FINGERPRINT)
                .commit();
        assertFalse(new SharedPreferencesGlDeviceSnapshotStore(
                context).read().isAvailable());

        preferences.edit()
                .putInt(
                        SharedPreferencesGlDeviceSnapshotStore.RENDERER_KEY,
                        750)
                .commit();
        assertFalse(new SharedPreferencesGlDeviceSnapshotStore(
                context).read().isAvailable());
    }

    @Test
    public void oversizedPersistedValuesFailClosed() {
        preferences.edit()
                .putString(
                        SharedPreferencesGlDeviceSnapshotStore
                                .BUILD_FINGERPRINT_KEY,
                        FINGERPRINT)
                .putString(
                        SharedPreferencesGlDeviceSnapshotStore.RENDERER_KEY,
                        repeat('r', 1_025))
                .commit();

        assertFalse(new SharedPreferencesGlDeviceSnapshotStore(
                context).read().isAvailable());
    }

    @Test
    public void unavailableReplacementClearsOnlyTheAtomicPair() {
        preferences.edit()
                .putString(UNRELATED_KEY, "keep")
                .putString(
                        SharedPreferencesGlDeviceSnapshotStore
                                .BUILD_FINGERPRINT_KEY,
                        FINGERPRINT)
                .putString(
                        SharedPreferencesGlDeviceSnapshotStore.RENDERER_KEY,
                        RENDERER)
                .commit();

        new SharedPreferencesGlDeviceSnapshotStore(context)
                .replace(GlDeviceSnapshot.unavailable());

        assertFalse(preferences.contains(
                SharedPreferencesGlDeviceSnapshotStore
                        .BUILD_FINGERPRINT_KEY));
        assertFalse(preferences.contains(
                SharedPreferencesGlDeviceSnapshotStore.RENDERER_KEY));
        assertEquals("keep", preferences.getString(
                UNRELATED_KEY, null));
    }

    private static String repeat(char value, int count) {
        char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
