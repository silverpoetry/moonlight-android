package com.limelight.preferences;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Vibrator;

import androidx.core.content.ContextCompat;

/** Android adapter that converts platform details into semantic capabilities. */
final class AndroidSettingsDeviceCapabilities {
    private static final String FEATURE_FIRE_OS =
            "com.amazon.software.fireos";
    private static final String FEATURE_NVIDIA_SHIELD =
            "com.nvidia.feature.shield";

    private AndroidSettingsDeviceCapabilities() {
    }

    static SettingsDeviceCapabilities collect(Context context) {
        PackageManager packageManager = context.getPackageManager();
        SensorManager sensorManager = ContextCompat.getSystemService(
                context,
                SensorManager.class);
        Vibrator vibrator = ContextCompat.getSystemService(
                context,
                Vibrator.class);
        boolean vibratorAvailable =
                vibrator != null && vibrator.hasVibrator();

        return SettingsDeviceCapabilities.builder()
                .touchscreenAvailable(packageManager.hasSystemFeature(
                        PackageManager.FEATURE_TOUCHSCREEN))
                .absoluteMouseModeAvailable(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                !packageManager.hasSystemFeature(
                                        FEATURE_NVIDIA_SHIELD))
                .barometerAvailable(
                        sensorManager != null &&
                                sensorManager.getDefaultSensor(
                                        Sensor.TYPE_PRESSURE,
                                        false) != null)
                .controllerMotionSensorsAvailable(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                .deviceMotionSensorsAvailable(
                        packageManager.hasSystemFeature(
                                PackageManager
                                        .FEATURE_SENSOR_ACCELEROMETER) ||
                                packageManager.hasSystemFeature(
                                        PackageManager
                                                .FEATURE_SENSOR_GYROSCOPE))
                .usbHostAvailable(packageManager.hasSystemFeature(
                        PackageManager.FEATURE_USB_HOST))
                .pictureInPictureAvailable(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                packageManager.hasSystemFeature(
                                        PackageManager
                                                .FEATURE_PICTURE_IN_PICTURE) &&
                                !packageManager.hasSystemFeature(
                                        FEATURE_FIRE_OS))
                .vibratorAvailable(vibratorAvailable)
                .vibrationAmplitudeControlAvailable(
                        vibratorAvailable &&
                                Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.O &&
                                vibrator.hasAmplitudeControl())
                .build();
    }
}
