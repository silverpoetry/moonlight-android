package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ControllerMotionSessionTest {
    @Test
    public void reportRateIsBoundedAndRegistrationIsReplaced() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeRegistrations registrations = new FakeRegistrations();
        ControllerMotionSession session =
                new ControllerMotionSession(scheduler, registrations);

        session.setReportRate(
                (short) 3,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 500);

        assertEquals(1, registrations.calls.size());
        assertCall(
                registrations.calls.get(0),
                (short) 3,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) ControllerMotionSession.MAX_REPORT_RATE_HZ);
    }

    @Test
    public void disableRetainsRequestsAndSendsNeutralGyroscope() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeRegistrations registrations = new FakeRegistrations();
        ControllerMotionSession session =
                new ControllerMotionSession(scheduler, registrations);
        session.setReportRate(
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 60);
        session.setReportRate(
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 120);

        session.disable();

        assertEquals(4, registrations.calls.size());
        assertCall(
                registrations.calls.get(2),
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 0);
        assertCall(
                registrations.calls.get(3),
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 0);
        assertEquals(1, registrations.neutralGyroscopeCount);
        assertEquals(2, registrations.neutralControllerNumber);

        session.enableAfterDeviceSettles();
        assertEquals(
                ControllerMotionSession.REENABLE_DELAY_MS,
                scheduler.delayMs);
        scheduler.runScheduled();

        assertCall(
                registrations.calls.get(4),
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 60);
        assertCall(
                registrations.calls.get(5),
                (short) 2,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 120);
    }

    @Test
    public void repeatedEnableHasOnePendingCallback() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeRegistrations registrations = new FakeRegistrations();
        ControllerMotionSession session =
                new ControllerMotionSession(
                        scheduler,
                        registrations);
        session.setReportRate(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 100);

        session.disable();
        session.enableAfterDeviceSettles();
        Runnable first = scheduler.scheduled;
        session.enableAfterDeviceSettles();

        assertEquals(2, scheduler.scheduleCount);
        assertEquals(1, scheduler.cancelCount);
        assertTrue(scheduler.scheduled != null);
        assertTrue(first != scheduler.scheduled);

        int callCount = registrations.calls.size();
        first.run();
        assertEquals(callCount, registrations.calls.size());
    }

    @Test
    public void desiredStateMovesWithoutRegisteringUntilEnabled() {
        FakeScheduler oldScheduler = new FakeScheduler();
        ControllerMotionSession oldSession =
                new ControllerMotionSession(
                        oldScheduler,
                        new FakeRegistrations());
        oldSession.setReportRate(
                (short) 4,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 80);
        oldSession.setReportRate(
                (short) 4,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 160);
        ControllerMotionSession.DesiredState state =
                oldSession.snapshotDesiredState();

        FakeScheduler newScheduler = new FakeScheduler();
        FakeRegistrations newRegistrations = new FakeRegistrations();
        ControllerMotionSession newSession =
                new ControllerMotionSession(
                        newScheduler,
                        newRegistrations);
        newSession.restoreDesiredState(state);

        assertTrue(newRegistrations.calls.isEmpty());
        newSession.enableAfterDeviceSettles();
        newScheduler.runScheduled();
        assertEquals(2, newRegistrations.calls.size());
        assertCall(
                newRegistrations.calls.get(0),
                (short) 4,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 80);
        assertCall(
                newRegistrations.calls.get(1),
                (short) 4,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 160);
    }

    @Test
    public void destroyCancelsAndRejectsLateReenable() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeRegistrations registrations = new FakeRegistrations();
        ControllerMotionSession session =
                new ControllerMotionSession(scheduler, registrations);
        session.setReportRate(
                (short) 1,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 100);
        session.disable();
        session.enableAfterDeviceSettles();
        Runnable staleCallback = scheduler.scheduled;

        session.destroy();
        int callCountAfterDestroy = registrations.calls.size();
        staleCallback.run();
        session.setReportRate(
                (short) 1,
                MoonBridge.LI_MOTION_TYPE_GYRO,
                (short) 120);

        assertNull(scheduler.scheduled);
        assertEquals(callCountAfterDestroy, registrations.calls.size());
    }

    @Test
    public void unsupportedAndNegativeRequestsDoNotCreateInvalidRegistrations() {
        FakeRegistrations registrations = new FakeRegistrations();
        ControllerMotionSession session =
                new ControllerMotionSession(
                        new FakeScheduler(),
                        registrations);

        session.setReportRate((short) 0, (byte) 99, (short) 50);
        session.setReportRate(
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) -1);

        assertEquals(1, registrations.calls.size());
        assertCall(
                registrations.calls.get(0),
                (short) 0,
                MoonBridge.LI_MOTION_TYPE_ACCEL,
                (short) 0);
    }

    private static void assertCall(
            RegistrationCall call,
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        assertEquals(controllerNumber, call.controllerNumber);
        assertEquals(motionType, call.motionType);
        assertEquals(reportRateHz, call.reportRateHz);
    }

    private static final class FakeScheduler
            implements ControllerMotionSession.Scheduler {
        private Runnable scheduled;
        private long delayMs;
        private int scheduleCount;
        private int cancelCount;

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            scheduled = runnable;
            this.delayMs = delayMs;
            scheduleCount++;
        }

        @Override
        public void cancel(Runnable runnable) {
            if (scheduled == runnable) {
                scheduled = null;
                cancelCount++;
            }
        }

        void runScheduled() {
            Runnable runnable = scheduled;
            scheduled = null;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    private static final class FakeRegistrations
            implements ControllerMotionSession.Registrations {
        private final List<RegistrationCall> calls =
                new ArrayList<>();
        private boolean accelerometerActive;
        private boolean gyroscopeActive;
        private int neutralGyroscopeCount;
        private short neutralControllerNumber;

        @Override
        public boolean replace(
                short controllerNumber,
                byte motionType,
                short reportRateHz) {
            boolean wasActive;
            if (motionType == MoonBridge.LI_MOTION_TYPE_ACCEL) {
                wasActive = accelerometerActive;
                accelerometerActive = reportRateHz != 0;
            }
            else {
                wasActive = gyroscopeActive;
                gyroscopeActive = reportRateHz != 0;
            }
            calls.add(
                    new RegistrationCall(
                            controllerNumber,
                            motionType,
                            reportRateHz));
            return wasActive;
        }

        @Override
        public void sendNeutralGyroscope(short controllerNumber) {
            neutralGyroscopeCount++;
            neutralControllerNumber = controllerNumber;
        }
    }

    private static final class RegistrationCall {
        private final short controllerNumber;
        private final byte motionType;
        private final short reportRateHz;

        private RegistrationCall(
                short controllerNumber,
                byte motionType,
                short reportRateHz) {
            this.controllerNumber = controllerNumber;
            this.motionType = motionType;
            this.reportRateHz = reportRateHz;
        }
    }
}
