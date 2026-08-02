package com.limelight.nvstream.clipboard.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.nvstream.clipboard.ClipboardSyncCheckpoint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class SharedPreferencesClipboardSyncCheckpointStoreTest {
    private static final String FINGERPRINT =
            "0123456789abcdef0123456789abcdef" +
                    "0123456789abcdef0123456789abcdef";

    private Context context;
    private SharedPreferences preferences;

    @Before
    public void clearBefore() {
        context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(
                SharedPreferencesClipboardSyncCheckpointStore
                        .PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @After
    public void clearAfter() {
        preferences.edit().clear().commit();
    }

    @Test
    public void roundTripPreservesInitializedCheckpoint() {
        SharedPreferencesClipboardSyncCheckpointStore store =
                new SharedPreferencesClipboardSyncCheckpointStore(context);

        assertFalse(store.read().isInitialized());

        store.write(ClipboardSyncCheckpoint.initialized(FINGERPRINT));
        ClipboardSyncCheckpoint restored =
                new SharedPreferencesClipboardSyncCheckpointStore(context)
                        .read();

        assertTrue(restored.isInitialized());
        assertEquals(
                FINGERPRINT,
                restored.getLastHandledFingerprint());
    }

    @Test
    public void corruptStoredValuesFailClosed() {
        preferences.edit()
                .putInt(
                        SharedPreferencesClipboardSyncCheckpointStore
                                .LAST_HANDLED_KEY,
                        7)
                .commit();
        assertFalse(new SharedPreferencesClipboardSyncCheckpointStore(
                context).read().isInitialized());

        preferences.edit()
                .putString(
                        SharedPreferencesClipboardSyncCheckpointStore
                                .LAST_HANDLED_KEY,
                        "not-a-fingerprint")
                .commit();
        assertFalse(new SharedPreferencesClipboardSyncCheckpointStore(
                context).read().isInitialized());
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToPersistUninitializedCheckpoint() {
        new SharedPreferencesClipboardSyncCheckpointStore(context)
                .write(ClipboardSyncCheckpoint.uninitialized());
    }
}
