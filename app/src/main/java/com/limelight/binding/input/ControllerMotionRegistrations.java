package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/**
 * Owns the active accelerometer and gyroscope registration resources for one
 * controller context.
 */
final class ControllerMotionRegistrations<M, S, L>
        implements ControllerMotionSession.Registrations {
    enum SensorKind {
        ACCELEROMETER,
        GYROSCOPE
    }

    interface Backend<M, S, L> {
        S findSensor(M manager, SensorKind kind);

        L createListener(
                short controllerNumber,
                byte motionType,
                boolean needsDeviceOrientationCorrection);

        boolean register(
                M manager,
                L listener,
                S sensor,
                int samplingPeriodUs);

        void unregister(M manager, L listener);

        boolean needsDeviceOrientationCorrection(M manager);

        void sendNeutralGyroscope(short controllerNumber);
    }

    private static final class ActiveRegistration<M, L> {
        private final M manager;
        private final L listener;

        private ActiveRegistration(M manager, L listener) {
            this.manager = manager;
            this.listener = listener;
        }
    }

    private final Backend<M, S, L> backend;
    private M manager;
    private ActiveRegistration<M, L> accelerometer;
    private ActiveRegistration<M, L> gyroscope;

    ControllerMotionRegistrations(Backend<M, S, L> backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    synchronized void setManager(M manager) {
        this.manager = manager;
    }

    synchronized boolean hasManager() {
        return manager != null;
    }

    synchronized boolean usesManager(M expectedManager) {
        return manager == expectedManager;
    }

    synchronized boolean hasSensor(SensorKind kind) {
        return manager != null && backend.findSensor(manager, kind) != null;
    }

    @Override
    public synchronized boolean replace(
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        SensorKind kind = kindForMotionType(motionType);
        if (kind == null) {
            return false;
        }

        ActiveRegistration<M, L> previous = takeRegistration(kind);
        boolean wasActive = previous != null;
        if (previous != null) {
            backend.unregister(previous.manager, previous.listener);
        }

        M registrationManager = manager;
        if (reportRateHz <= 0 || registrationManager == null) {
            return wasActive;
        }

        S sensor = backend.findSensor(registrationManager, kind);
        if (sensor == null) {
            return wasActive;
        }

        L listener = backend.createListener(
                controllerNumber,
                motionType,
                backend.needsDeviceOrientationCorrection(
                        registrationManager));
        if (!backend.register(
                registrationManager,
                listener,
                sensor,
                1_000_000 / reportRateHz)) {
            return wasActive;
        }

        storeRegistration(
                kind,
                new ActiveRegistration<>(
                        registrationManager,
                        listener));
        return wasActive;
    }

    @Override
    public void sendNeutralGyroscope(short controllerNumber) {
        backend.sendNeutralGyroscope(controllerNumber);
    }

    private ActiveRegistration<M, L> takeRegistration(
            SensorKind kind) {
        ActiveRegistration<M, L> registration;
        if (kind == SensorKind.ACCELEROMETER) {
            registration = accelerometer;
            accelerometer = null;
        }
        else {
            registration = gyroscope;
            gyroscope = null;
        }
        return registration;
    }

    private void storeRegistration(
            SensorKind kind,
            ActiveRegistration<M, L> registration) {
        if (kind == SensorKind.ACCELEROMETER) {
            accelerometer = registration;
        }
        else {
            gyroscope = registration;
        }
    }

    private static SensorKind kindForMotionType(byte motionType) {
        if (motionType == MoonBridge.LI_MOTION_TYPE_ACCEL) {
            return SensorKind.ACCELEROMETER;
        }
        if (motionType == MoonBridge.LI_MOTION_TYPE_GYRO) {
            return SensorKind.GYROSCOPE;
        }
        return null;
    }
}
