package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/**
 * Owns the requested report rates and registration lifecycle for one controller's motion sensors.
 */
final class ControllerMotionSession {
    interface Scheduler {
        void schedule(Runnable runnable, long delayMs);

        void cancel(Runnable runnable);
    }

    interface Registrations {
        /**
         * Replaces the registration for {@code motionType} and returns whether one was active.
         */
        boolean replace(short controllerNumber, byte motionType, short reportRateHz);

        void sendNeutralGyroscope(short controllerNumber);
    }

    static final int MAX_REPORT_RATE_HZ = 200;
    static final long REENABLE_DELAY_MS = 1_000;

    private final Scheduler scheduler;
    private final Registrations registrations;
    private short accelerometerControllerNumber;
    private short accelerometerReportRateHz;
    private short gyroscopeControllerNumber;
    private short gyroscopeReportRateHz;
    private Runnable pendingReenable;
    private long reenableGeneration;
    private boolean destroyed;

    ControllerMotionSession(
            Scheduler scheduler,
            Registrations registrations) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    synchronized void setReportRate(
            short controllerNumber,
            byte motionType,
            short requestedReportRateHz) {
        if (destroyed || !isSupportedMotionType(motionType)) {
            return;
        }

        short reportRateHz = limitReportRate(requestedReportRateHz);
        cancelPendingReenable();
        storeRequest(controllerNumber, motionType, reportRateHz);
        registrations.replace(controllerNumber, motionType, reportRateHz);
    }

    synchronized void disable() {
        if (destroyed) {
            return;
        }

        cancelPendingReenable();
        boolean gyroscopeWasActive =
                registrations.replace(
                        gyroscopeControllerNumber,
                        MoonBridge.LI_MOTION_TYPE_GYRO,
                        (short) 0);
        registrations.replace(
                accelerometerControllerNumber,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 0);
        if (gyroscopeWasActive) {
            registrations.sendNeutralGyroscope(
                    gyroscopeControllerNumber);
        }
    }

    synchronized void enableAfterDeviceSettles() {
        if (destroyed || !hasRequestedReports()) {
            return;
        }

        cancelPendingReenable();
        final long expectedGeneration = reenableGeneration;
        pendingReenable =
                new Runnable() {
                    @Override
                    public void run() {
                        runReenable(expectedGeneration);
                    }
                };
        scheduler.schedule(
                pendingReenable,
                REENABLE_DELAY_MS);
    }

    synchronized DesiredState snapshotDesiredState() {
        return new DesiredState(
                accelerometerControllerNumber,
                accelerometerReportRateHz,
                gyroscopeControllerNumber,
                gyroscopeReportRateHz);
    }

    synchronized void restoreDesiredState(DesiredState state) {
        Objects.requireNonNull(state, "state");
        if (destroyed) {
            return;
        }
        accelerometerControllerNumber = state.accelerometerControllerNumber;
        accelerometerReportRateHz = state.accelerometerReportRateHz;
        gyroscopeControllerNumber = state.gyroscopeControllerNumber;
        gyroscopeReportRateHz = state.gyroscopeReportRateHz;
    }

    synchronized void destroy() {
        if (destroyed) {
            return;
        }

        destroyed = true;
        cancelPendingReenable();
        registrations.replace(
                gyroscopeControllerNumber,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 0);
        registrations.replace(
                accelerometerControllerNumber,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 0);
    }

    private void restoreRequestedRegistrations() {
        if (accelerometerReportRateHz != 0) {
            registrations.replace(
                    accelerometerControllerNumber,
                    MoonBridge.LI_MOTION_TYPE_ACCEL,
                    accelerometerReportRateHz);
        }
        if (gyroscopeReportRateHz != 0) {
            registrations.replace(
                    gyroscopeControllerNumber,
                    MoonBridge.LI_MOTION_TYPE_GYRO,
                    gyroscopeReportRateHz);
        }
    }

    private synchronized void runReenable(long expectedGeneration) {
        if (destroyed || expectedGeneration != reenableGeneration) {
            return;
        }
        pendingReenable = null;
        restoreRequestedRegistrations();
    }

    private void cancelPendingReenable() {
        reenableGeneration++;
        if (pendingReenable != null) {
            scheduler.cancel(pendingReenable);
            pendingReenable = null;
        }
    }

    private void storeRequest(
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        if (motionType == MoonBridge.LI_MOTION_TYPE_ACCEL) {
            accelerometerControllerNumber = controllerNumber;
            accelerometerReportRateHz = reportRateHz;
        }
        else {
            gyroscopeControllerNumber = controllerNumber;
            gyroscopeReportRateHz = reportRateHz;
        }
    }

    private boolean hasRequestedReports() {
        return accelerometerReportRateHz != 0 ||
                gyroscopeReportRateHz != 0;
    }

    private static boolean isSupportedMotionType(byte motionType) {
        return motionType == MoonBridge.LI_MOTION_TYPE_ACCEL ||
                motionType == MoonBridge.LI_MOTION_TYPE_GYRO;
    }

    private static short limitReportRate(short reportRateHz) {
        return (short) Math.max(
                0,
                Math.min(MAX_REPORT_RATE_HZ, reportRateHz));
    }

    static final class DesiredState {
        private final short accelerometerControllerNumber;
        private final short accelerometerReportRateHz;
        private final short gyroscopeControllerNumber;
        private final short gyroscopeReportRateHz;

        private DesiredState(
                short accelerometerControllerNumber,
                short accelerometerReportRateHz,
                short gyroscopeControllerNumber,
                short gyroscopeReportRateHz) {
            this.accelerometerControllerNumber = accelerometerControllerNumber;
            this.accelerometerReportRateHz = accelerometerReportRateHz;
            this.gyroscopeControllerNumber = gyroscopeControllerNumber;
            this.gyroscopeReportRateHz = gyroscopeReportRateHz;
        }
    }
}
