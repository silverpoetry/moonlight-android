package com.limelight.input.diagnostics;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;

import com.limelight.BaseActivity;
import com.limelight.R;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

/** Presentation for local input-device diagnostics. */
public final class InputDiagnosticsActivity extends BaseActivity
        implements InputDiagnosticsController.Listener {
    private InputDiagnosticsController controller;
    private TextView deviceSummary;
    private TextView inputEventSummary;
    private TextView sensorSummary;
    private Button controllerVibrationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_diagnostics);
        UiHelper.notifyNewEdgeToEdgeRootView(
                this,
                R.id.inputDiagnosticsHeader,
                R.id.inputDiagnosticsScroll);

        deviceSummary = findViewById(R.id.inputDiagnosticsDevices);
        inputEventSummary = findViewById(
                R.id.inputDiagnosticsEvents);
        sensorSummary = findViewById(R.id.inputDiagnosticsSensors);
        controllerVibrationButton = findViewById(
                R.id.testControllerVibration);

        controller = InputDiagnosticsController.create(this, this);
        findViewById(R.id.inputDiagnosticsBack).setOnClickListener(
                view -> finish());
        findViewById(R.id.refreshInputDevices).setOnClickListener(
                view -> controller.refreshDevices());
        findViewById(R.id.testDeviceVibration).setOnClickListener(
                view -> showVibrationResult(
                        controller.testDeviceVibration(),
                        R.string.input_diagnostics_no_device_vibrator));
        controllerVibrationButton.setOnClickListener(
                view -> showVibrationResult(
                        controller.testControllerVibration(),
                        R.string.input_diagnostics_no_controller_vibrator));
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller.start();
    }

    @Override
    protected void onStop() {
        controller.stop();
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (controller != null) {
            controller.onKeyEvent(event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (controller != null) {
            controller.onKeyEvent(event);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (controller != null) {
            controller.onMotionEvent(event);
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public void onDeviceSummaryChanged(String summary) {
        deviceSummary.setText(summary);
    }

    @Override
    public void onInputEventChanged(String summary) {
        inputEventSummary.setText(summary);
    }

    @Override
    public void onSensorSummaryChanged(String summary) {
        sensorSummary.setText(summary);
    }

    @Override
    public void onControllerVibrationAvailabilityChanged(
            boolean available,
            String deviceName) {
        controllerVibrationButton.setEnabled(available);
        controllerVibrationButton.setText(getString(
                R.string.input_diagnostics_test_controller_vibration_format,
                deviceName));
    }

    private void showVibrationResult(
            boolean successful,
            int unavailableMessage) {
        if (!successful) {
            UiToast.makeText(
                    this,
                    unavailableMessage,
                    UiToast.LENGTH_SHORT).show();
        }
    }
}
