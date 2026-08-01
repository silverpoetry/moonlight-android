package org.cgutman.shieldcontrollerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SceManagerLifecycleTest {
    @Test
    public void failedBindDoesNotAttemptToUnbind() {
        RecordingContext context = new RecordingContext(false);
        SceManager manager = new SceManager(context);

        assertFalse(manager.start());
        manager.stop();

        assertEquals(1, context.bindCount);
        assertEquals(0, context.unbindCount);
    }

    @Test
    public void successfulBindingAndStopAreIdempotent() {
        RecordingContext context = new RecordingContext(true);
        SceManager manager = new SceManager(context);

        assertTrue(manager.start());
        assertTrue(manager.start());
        manager.stop();
        manager.stop();

        assertEquals(1, context.bindCount);
        assertEquals(1, context.unbindCount);
        assertEquals(
                new ComponentName(
                        "com.nvidia.blakepairing",
                        "com.nvidia.blakepairing.AccessoryService"),
                context.boundIntent.getComponent());
    }

    private static final class RecordingContext extends ContextWrapper {
        private final boolean bindResult;
        private int bindCount;
        private int unbindCount;
        private Intent boundIntent;

        private RecordingContext(boolean bindResult) {
            super(InstrumentationRegistry.getInstrumentation()
                    .getTargetContext());
            this.bindResult = bindResult;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public boolean bindService(
                Intent service,
                ServiceConnection connection,
                int flags) {
            bindCount++;
            boundIntent = service;
            return bindResult;
        }

        @Override
        public void unbindService(ServiceConnection connection) {
            unbindCount++;
        }
    }
}
