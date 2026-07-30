package com.limelight.preferences;

import static org.junit.Assert.assertFalse;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StreamSettingsRenderingTest {

    @Test
    public void settingsActivityRendersWithoutCrashing() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(
                instrumentation.getTargetContext(),
                StreamSettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        StreamSettings activity =
                (StreamSettings) instrumentation.startActivitySync(intent);
        try {
            instrumentation.waitForIdleSync();
            assertFalse(activity.isFinishing());
        }
        finally {
            activity.finish();
        }
    }
}
