package com.limelight.binding.video;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public final class AndroidDecoderCrashStoreTest {
    private Context context;

    @Before
    public void clearBefore() {
        context = ApplicationProvider.getApplicationContext();
        clearPreferences();
    }

    @After
    public void clearAfter() {
        clearPreferences();
    }

    @Test
    public void storePreservesCrashAndCleanCompletionSemantics() {
        AndroidDecoderCrashStore store =
                new AndroidDecoderCrashStore(context);
        DecoderCrashTracker firstAttempt =
                new DecoderCrashTracker(store);

        firstAttempt.notifyCrash(new RuntimeException("decoder"));

        assertEquals(1, store.getCrashCount());
        DecoderCrashTracker cleanAttempt =
                new DecoderCrashTracker(
                        new AndroidDecoderCrashStore(context));
        assertEquals(1, cleanAttempt.getInitialCrashCount());

        cleanAttempt.completeCleanly();

        assertEquals(0, store.getCrashCount());
    }

    private void clearPreferences() {
        context.getSharedPreferences(
                        AndroidDecoderCrashStore.PREFERENCES_NAME,
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }
}
