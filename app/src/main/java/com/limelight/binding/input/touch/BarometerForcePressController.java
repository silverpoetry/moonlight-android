package com.limelight.binding.input.touch;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.limelight.LimeLog;

import java.util.Locale;

/**
 * Owns the pressure sensor lifecycle and maps Android pointer IDs into the
 * platform-independent force-press detector.
 */
public final class BarometerForcePressController
        implements SensorEventListener {
    public interface Listener {
        /**
         * @return true if the current one- or two-pointer gesture accepted the force press
         */
        boolean onForcePressDown(int pointerId, int pointerCount);

        void onForcePressUp(int pointerId, boolean cancelled);
    }

    private final SensorManager sensorManager;
    private final Sensor pressureSensor;
    private final Listener listener;
    private final BarometerForcePressDetector detector =
            new BarometerForcePressDetector();

    private boolean enabled;
    private boolean resumed;
    private boolean registered;

    public BarometerForcePressController(Context context, Listener listener) {
        sensorManager = (SensorManager) context.getSystemService(
                Context.SENSOR_SERVICE);
        pressureSensor = sensorManager == null
                ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE, false);
        this.listener = listener;
    }

    public boolean isAvailable() {
        return pressureSensor != null;
    }

    public void setThresholdHpa(float thresholdHpa) {
        boolean changed = Float.compare(
                detector.getThresholdHpa(),
                thresholdHpa) != 0;
        detector.setThresholdHpa(thresholdHpa);
        if (enabled && changed) {
            logConfiguration("updated");
        }
    }

    public void setMinimumTouchDurationMs(long minimumTouchDurationMs) {
        boolean changed = detector.getMinimumTouchDurationMs() !=
                minimumTouchDurationMs;
        detector.setMinimumTouchDurationMs(minimumTouchDurationMs);
        if (enabled && changed) {
            logConfiguration("updated");
        }
    }

    public void setEnabled(boolean enabled) {
        boolean newEnabled = enabled && isAvailable();
        if (this.enabled == newEnabled) {
            return;
        }

        if (!newEnabled) {
            cancelTouchSession();
        }
        this.enabled = newEnabled;
        updateRegistration();
        if (newEnabled) {
            logConfiguration("enabled");
        }
    }

    public void start() {
        resumed = true;
        updateRegistration();
    }

    public void stop() {
        resumed = false;
        updateRegistration();
        cancelTouchSession();
    }

    public void onTouchEvent(MotionEvent event) {
        if (!enabled || event == null) {
            return;
        }

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        if (action != MotionEvent.ACTION_CANCEL &&
                event.getToolType(actionIndex) !=
                        MotionEvent.TOOL_TYPE_FINGER) {
            return;
        }
        switch (action) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            detector.onPointerDown(
                    event.getPointerId(actionIndex),
                    event.getEventTime());
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            releasePointer(
                    event.getPointerId(actionIndex),
                    event.getEventTime());
            break;

        case MotionEvent.ACTION_CANCEL:
            cancelTouchSession();
            break;

        default:
            break;
        }
    }

    public void cancelTouchSession() {
        int forcePointerId = detector.getForcePointerId();
        boolean wasForcePressed = detector.isForcePressed();
        detector.cancelTouchSession();
        if (wasForcePressed) {
            listener.onForcePressUp(forcePointerId, true);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!enabled ||
                event.sensor.getType() != Sensor.TYPE_PRESSURE ||
                event.values.length == 0) {
            return;
        }

        detector.onPressureSample(
                event.values[0],
                SystemClock.uptimeMillis());
        if (!detector.wasForceTriggeredOnLastSample()) {
            return;
        }

        int pointerId = detector.getForcePointerId();
        if (!listener.onForcePressDown(
                pointerId,
                detector.getActivePointerCount())) {
            detector.blockCurrentTouchSession();
            return;
        }

        LimeLog.info(String.format(
                Locale.US,
                "Barometer force press: pointer=%d raw=%.4f baseline=%.4f " +
                        "delta=%.4f threshold=%.4f",
                pointerId,
                detector.getCurrentPressureHpa(),
                detector.getSessionBaselineHpa(),
                detector.getCurrentDeltaHpa(),
                detector.getThresholdHpa()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void releasePointer(int pointerId, long eventTimeMs) {
        int forcePointerId = detector.getForcePointerId();
        boolean wasForcePressed = detector.isForcePressed();
        detector.onPointerUp(pointerId, eventTimeMs);
        if (wasForcePressed && !detector.isForcePressed()) {
            listener.onForcePressUp(forcePointerId, false);
        }
    }

    private void updateRegistration() {
        boolean shouldRegister = enabled && resumed && pressureSensor != null;
        if (registered == shouldRegister) {
            return;
        }

        if (shouldRegister) {
            registered = sensorManager.registerListener(
                    this,
                    pressureSensor,
                    SensorManager.SENSOR_DELAY_FASTEST,
                    0);
            if (!registered) {
                LimeLog.warning("Failed to register pressure sensor");
            }
        }
        else {
            sensorManager.unregisterListener(this);
            registered = false;
        }
    }

    private void logConfiguration(String state) {
        LimeLog.info(String.format(
                Locale.US,
                "Barometer force press %s: threshold=%.4f " +
                        "minimumTouchDurationMs=%d registered=%s",
                state,
                detector.getThresholdHpa(),
                detector.getMinimumTouchDurationMs(),
                registered));
    }
}
