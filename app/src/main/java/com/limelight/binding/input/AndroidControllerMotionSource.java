package com.limelight.binding.input;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.InputDevice;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/** Binds a controller's Android motion-sensor manager when safely available. */
final class AndroidControllerMotionSource {
    private AndroidControllerMotionSource() {
    }

    @Nullable
    static SensorManager findAvailableManager(
            InputDevice device,
            boolean motionSensorsEnabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                !ControllerMotionSensorPolicy
                .shouldProbeInputDeviceSensors(
                        Build.VERSION.SDK_INT,
                        device.getVendorId(),
                        motionSensorsEnabled)) {
            return null;
        }
        return findOnAndroidTwelveOrLater(device);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Nullable
    private static SensorManager findOnAndroidTwelveOrLater(
            InputDevice device) {
        SensorManager manager = device.getSensorManager();
        return manager.getDefaultSensor(
                        Sensor.TYPE_ACCELEROMETER) != null ||
                manager.getDefaultSensor(
                        Sensor.TYPE_GYROSCOPE) != null
                ? manager
                : null;
    }
}
