package com.limelight.binding.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControllerMouseEmulationSessionTest {
    @Test
    public void activeSessionReportsAndReschedules() {
        FakeScheduler scheduler = new FakeScheduler();
        CountingReport report = new CountingReport();
        ControllerMouseEmulationSession session =
                new ControllerMouseEmulationSession(
                        scheduler,
                        report);

        session.setActive(true);

        assertTrue(session.isActive());
        assertEquals(1, scheduler.scheduleCount);
        assertEquals(
                ControllerMouseEmulationSession.REPORT_PERIOD_MS,
                scheduler.lastDelayMs);

        scheduler.runScheduled();

        assertEquals(1, report.count);
        assertEquals(2, scheduler.scheduleCount);
    }

    @Test
    public void inactiveSessionRejectsLateScheduledCallback() {
        FakeScheduler scheduler = new FakeScheduler();
        CountingReport report = new CountingReport();
        ControllerMouseEmulationSession session =
                new ControllerMouseEmulationSession(
                        scheduler,
                        report);

        session.setActive(true);
        Runnable staleCallback = scheduler.scheduled;
        session.setActive(false);
        staleCallback.run();

        assertFalse(session.isActive());
        assertEquals(0, report.count);
        assertEquals(1, scheduler.scheduleCount);
    }

    @Test
    public void activeStateCanMoveToReplacementSession() {
        FakeScheduler oldScheduler = new FakeScheduler();
        ControllerMouseEmulationSession oldSession =
                new ControllerMouseEmulationSession(
                        oldScheduler,
                        new CountingReport());
        oldSession.setActive(true);
        boolean restoreActive = oldSession.isActive();

        oldSession.destroy();

        FakeScheduler newScheduler = new FakeScheduler();
        ControllerMouseEmulationSession newSession =
                new ControllerMouseEmulationSession(
                        newScheduler,
                        new CountingReport());
        newSession.setActive(restoreActive);

        assertFalse(oldSession.isActive());
        assertTrue(newSession.isActive());
        assertEquals(1, oldScheduler.cancelCount);
        assertEquals(1, newScheduler.scheduleCount);
    }

    @Test
    public void destroyedSessionCannotRestart() {
        FakeScheduler scheduler = new FakeScheduler();
        CountingReport report = new CountingReport();
        ControllerMouseEmulationSession session =
                new ControllerMouseEmulationSession(
                        scheduler,
                        report);

        session.setActive(true);
        session.destroy();
        session.setActive(true);
        scheduler.runScheduled();

        assertFalse(session.isActive());
        assertEquals(0, report.count);
        assertEquals(1, scheduler.scheduleCount);
    }

    private static final class CountingReport
            implements Runnable {
        private int count;

        @Override
        public void run() {
            count++;
        }
    }

    private static final class FakeScheduler
            implements ControllerMouseEmulationSession.Scheduler {
        private Runnable scheduled;
        private long lastDelayMs;
        private int scheduleCount;
        private int cancelCount;

        @Override
        public void schedule(
                Runnable runnable,
                long delayMs) {
            scheduled = runnable;
            lastDelayMs = delayMs;
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
}
