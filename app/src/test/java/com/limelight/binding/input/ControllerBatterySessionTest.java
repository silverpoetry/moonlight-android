package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ControllerBatterySessionTest {
    private static final long INTERVAL_MS = 120_000;

    @Test
    public void enableReportsImmediatelyThenAtInterval() {
        RecordingScheduler scheduler = new RecordingScheduler();
        Counter reports = new Counter();
        ControllerBatterySession session = create(scheduler, reports);

        session.setEnabled(true);
        assertEquals(0, scheduler.lastDelayMs);
        scheduler.runPending();

        assertEquals(1, reports.value);
        assertEquals(INTERVAL_MS, scheduler.lastDelayMs);
        scheduler.runPending();
        assertEquals(2, reports.value);
    }

    @Test
    public void repeatedEnableDoesNotCreateDuplicateSchedule() {
        RecordingScheduler scheduler = new RecordingScheduler();
        ControllerBatterySession session = create(
                scheduler,
                new Counter());

        session.setEnabled(true);
        session.setEnabled(true);

        assertEquals(1, scheduler.scheduleCount);
        assertEquals(1, scheduler.pending.size());
    }

    @Test
    public void disableCancelsPendingAndRejectsStaleCallback() {
        RecordingScheduler scheduler = new RecordingScheduler();
        Counter reports = new Counter();
        ControllerBatterySession session = create(scheduler, reports);
        session.setEnabled(true);
        Runnable stale = scheduler.pending.get(0);

        session.setEnabled(false);
        stale.run();

        assertEquals(1, scheduler.cancelCount);
        assertEquals(0, reports.value);
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void disableDuringReportPreventsRequeue() {
        RecordingScheduler scheduler = new RecordingScheduler();
        ControllerBatterySession[] holder = new ControllerBatterySession[1];
        holder[0] = new ControllerBatterySession(
                scheduler,
                () -> holder[0].setEnabled(false),
                INTERVAL_MS);

        holder[0].setEnabled(true);
        scheduler.runPending();

        assertTrue(scheduler.pending.isEmpty());
        assertEquals(1, scheduler.scheduleCount);
    }

    @Test
    public void reenableRejectsPreviousGeneration() {
        RecordingScheduler scheduler = new RecordingScheduler();
        Counter reports = new Counter();
        ControllerBatterySession session = create(scheduler, reports);
        session.setEnabled(true);
        Runnable stale = scheduler.pending.get(0);

        session.setEnabled(false);
        session.setEnabled(true);
        stale.run();
        scheduler.runPending();

        assertEquals(1, reports.value);
    }

    @Test
    public void destroyIsIdempotentAndTerminal() {
        RecordingScheduler scheduler = new RecordingScheduler();
        Counter reports = new Counter();
        ControllerBatterySession session = create(scheduler, reports);
        session.setEnabled(true);
        Runnable stale = scheduler.pending.get(0);

        session.destroy();
        session.destroy();
        session.setEnabled(true);
        stale.run();

        assertEquals(1, scheduler.cancelCount);
        assertEquals(0, reports.value);
        assertTrue(scheduler.pending.isEmpty());
    }

    private static ControllerBatterySession create(
            RecordingScheduler scheduler,
            Counter reports) {
        return new ControllerBatterySession(
                scheduler,
                () -> reports.value++,
                INTERVAL_MS);
    }

    private static final class Counter {
        int value;
    }

    private static final class RecordingScheduler
            implements ControllerBatterySession.Scheduler {
        final List<Runnable> pending = new ArrayList<>();
        int scheduleCount;
        int cancelCount;
        long lastDelayMs = -1;

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            scheduleCount++;
            lastDelayMs = delayMs;
            pending.add(runnable);
        }

        @Override
        public void cancel(Runnable runnable) {
            cancelCount++;
            pending.remove(runnable);
        }

        void runPending() {
            Runnable runnable = pending.remove(0);
            runnable.run();
        }
    }
}
