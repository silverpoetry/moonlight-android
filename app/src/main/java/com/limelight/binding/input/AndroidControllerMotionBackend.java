package com.limelight.binding.input;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Objects;

/**
 * Android platform adapter for controller motion-sensor registrations.
 */
final class AndroidControllerMotionBackend implements
        ControllerMotionRegistrations.Backend<
                SensorManager,
                Sensor,
                SensorEventListener> {
    interface ListenerFactory {
        SensorEventListener create(
                short controllerNumber,
                byte motionType,
                boolean needsDeviceOrientationCorrection);
    }

    interface NeutralGyroscopeSink {
        void send(short controllerNumber);
    }

    private final SensorManager deviceSensorManager;
    private final ListenerFactory listenerFactory;
    private final NeutralGyroscopeSink neutralGyroscopeSink;

    AndroidControllerMotionBackend(
            SensorManager deviceSensorManager,
            ListenerFactory listenerFactory,
            NeutralGyroscopeSink neutralGyroscopeSink) {
        this.deviceSensorManager = Objects.requireNonNull(
                deviceSensorManager,
                "deviceSensorManager");
        this.listenerFactory = Objects.requireNonNull(
                listenerFactory,
                "listenerFactory");
        this.neutralGyroscopeSink = Objects.requireNonNull(
                neutralGyroscopeSink,
                "neutralGyroscopeSink");
    }

    @Override
    public Sensor findSensor(
            SensorManager manager,
            ControllerMotionRegistrations.SensorKind kind) {
        return manager.getDefaultSensor(
                kind == ControllerMotionRegistrations.SensorKind.ACCELEROMETER
                        ? Sensor.TYPE_ACCELEROMETER
                        : Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public SensorEventListener createListener(
            short controllerNumber,
            byte motionType,
            boolean needsDeviceOrientationCorrection) {
        return listenerFactory.create(
                controllerNumber,
                motionType,
                needsDeviceOrientationCorrection);
    }

    @Override
    public boolean register(
            SensorManager manager,
            SensorEventListener listener,
            Sensor sensor,
            int samplingPeriodUs) {
        return manager.registerListener(
                listener,
                sensor,
                samplingPeriodUs);
    }

    @Override
    public void unregister(
            SensorManager manager,
            SensorEventListener listener) {
        manager.unregisterListener(listener);
    }

    @Override
    public boolean needsDeviceOrientationCorrection(
            SensorManager manager) {
        return manager == deviceSensorManager;
    }

    @Override
    public void sendNeutralGyroscope(short controllerNumber) {
        neutralGyroscopeSink.send(controllerNumber);
    }
}
