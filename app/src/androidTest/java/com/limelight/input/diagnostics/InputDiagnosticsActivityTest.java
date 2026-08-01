package com.limelight.input.diagnostics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.Instrumentation;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class InputDiagnosticsActivityTest {
    @Test
    public void launchesAndPublishesLocalDiagnostics() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(
                instrumentation.getTargetContext(),
                InputDiagnosticsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InputDiagnosticsActivity activity =
                (InputDiagnosticsActivity)
                        instrumentation.startActivitySync(intent);

        try {
            instrumentation.waitForIdleSync();
            TextView devices = activity.findViewById(
                    R.id.inputDiagnosticsDevices);
            TextView sensors = activity.findViewById(
                    R.id.inputDiagnosticsSensors);
            Button controllerVibration = activity.findViewById(
                    R.id.testControllerVibration);

            assertNotNull(devices);
            assertNotNull(sensors);
            assertNotNull(controllerVibration);
            assertFalse(devices.getText().toString().isEmpty());
            assertFalse(sensors.getText().toString().isEmpty());
        }
        finally {
            activity.finish();
        }
    }
}
