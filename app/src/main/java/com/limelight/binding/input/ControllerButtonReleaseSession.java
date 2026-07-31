package com.limelight.binding.input;

import com.limelight.settings.controller.ControllerSettings;

import java.util.Arrays;
import java.util.Objects;

/**
 * Enforces a minimum host-visible duration for physical controller buttons
 * without blocking Android's input thread.
 *
 * <p>Each target has one fixed slot. The session schedules only the earliest
 * pending deadline and scans the bounded target table when it wakes. Normal
 * button input allocates nothing and never enters a lock or queue.</p>
 */
final class ControllerButtonReleaseSession {
    interface Scheduler {
        long now();

        void schedule(Runnable runnable, long delayMs);

        void cancel(Runnable runnable);
    }

    interface Listener {
        void onButtonRelease(
                ControllerDigitalButtonMapping.Target target,
                ControllerSettings settings,
                long eventTime);
    }

    static final long MINIMUM_BUTTON_DOWN_TIME_MS = 25;

    private static final ControllerDigitalButtonMapping.Target[] TARGETS =
            ControllerDigitalButtonMapping.Target.values();

    private final Scheduler scheduler;
    private final Listener listener;
    private final boolean[] pressed = new boolean[TARGETS.length];
    private final long[] downTimes = new long[TARGETS.length];
    private final boolean[] pending = new boolean[TARGETS.length];
    private final long[] dueTimes = new long[TARGETS.length];
    private final long[] sequences = new long[TARGETS.length];
    private final long[] eventTimes = new long[TARGETS.length];
    private final ControllerSettings[] settings =
            new ControllerSettings[TARGETS.length];
    private final Runnable scheduledDrain =
            new Runnable() {
                @Override
                public void run() {
                    scheduled = false;
                    drainDueReleases();
                }
            };

    private long nextSequence;
    private boolean scheduled;
    private boolean destroyed;

    ControllerButtonReleaseSession(
            Scheduler scheduler,
            Listener listener) {
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "scheduler");
        this.listener = Objects.requireNonNull(
                listener,
                "listener");
    }

    void recordButtonDown(
            ControllerDigitalButtonMapping.Target target,
            int repeatCount) {
        if (destroyed || target ==
                ControllerDigitalButtonMapping.Target.UNHANDLED) {
            return;
        }

        int index = target.ordinal();
        if (repeatCount == 0 || !pressed[index]) {
            pressed[index] = true;
            downTimes[index] = scheduler.now();
        }
    }

    boolean deferButtonUp(
            ControllerDigitalButtonMapping.Target target,
            ControllerSettings controllerSettings,
            long eventTime) {
        if (destroyed || target ==
                ControllerDigitalButtonMapping.Target.UNHANDLED) {
            return false;
        }

        int index = target.ordinal();
        if (!pressed[index]) {
            return false;
        }
        if (pending[index]) {
            return true;
        }

        long now = scheduler.now();
        long elapsed = Math.max(0, now - downTimes[index]);
        long remaining =
                MINIMUM_BUTTON_DOWN_TIME_MS - elapsed;
        if (remaining <= 0) {
            pressed[index] = false;
            return false;
        }

        pending[index] = true;
        dueTimes[index] = now + remaining;
        sequences[index] = nextSequence++;
        eventTimes[index] = eventTime;
        settings[index] = Objects.requireNonNull(
                controllerSettings,
                "controllerSettings");
        scheduleEarliestRelease();
        return true;
    }

    void flushPendingRelease(
            ControllerDigitalButtonMapping.Target target) {
        if (destroyed || target ==
                ControllerDigitalButtonMapping.Target.UNHANDLED) {
            return;
        }

        int index = target.ordinal();
        if (!pending[index]) {
            return;
        }

        cancelScheduledDrain();
        release(index);
        scheduleEarliestRelease();
    }

    void abandonButton(
            ControllerDigitalButtonMapping.Target target) {
        if (destroyed || target ==
                ControllerDigitalButtonMapping.Target.UNHANDLED) {
            return;
        }
        int index = target.ordinal();
        pressed[index] = false;
        if (pending[index]) {
            cancelScheduledDrain();
            clearPending(index);
            scheduleEarliestRelease();
        }
    }

    void restoreFrom(
            ControllerButtonReleaseSession previousSession) {
        Objects.requireNonNull(previousSession, "previousSession");
        if (destroyed) {
            return;
        }

        cancelScheduledDrain();
        previousSession.cancelScheduledDrain();
        System.arraycopy(
                previousSession.pressed,
                0,
                pressed,
                0,
                pressed.length);
        System.arraycopy(
                previousSession.downTimes,
                0,
                downTimes,
                0,
                downTimes.length);
        System.arraycopy(
                previousSession.pending,
                0,
                pending,
                0,
                pending.length);
        System.arraycopy(
                previousSession.dueTimes,
                0,
                dueTimes,
                0,
                dueTimes.length);
        System.arraycopy(
                previousSession.sequences,
                0,
                sequences,
                0,
                sequences.length);
        System.arraycopy(
                previousSession.eventTimes,
                0,
                eventTimes,
                0,
                eventTimes.length);
        System.arraycopy(
                previousSession.settings,
                0,
                settings,
                0,
                settings.length);
        nextSequence = previousSession.nextSequence;
        previousSession.clearAllState();
        scheduleEarliestRelease();
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        cancelScheduledDrain();
        clearAllState();
    }

    private void drainDueReleases() {
        if (destroyed) {
            return;
        }

        while (true) {
            int index = findNextDueRelease(scheduler.now());
            if (index < 0) {
                break;
            }
            release(index);
        }
        scheduleEarliestRelease();
    }

    private int findNextDueRelease(long now) {
        int selectedIndex = -1;
        long selectedDueTime = Long.MAX_VALUE;
        long selectedSequence = Long.MAX_VALUE;
        for (int index = 0; index < pending.length; index++) {
            if (!pending[index] || dueTimes[index] > now) {
                continue;
            }
            if (dueTimes[index] < selectedDueTime ||
                    (dueTimes[index] == selectedDueTime &&
                            sequences[index] < selectedSequence)) {
                selectedIndex = index;
                selectedDueTime = dueTimes[index];
                selectedSequence = sequences[index];
            }
        }
        return selectedIndex;
    }

    private void release(int index) {
        ControllerSettings releaseSettings = settings[index];
        long releaseEventTime = eventTimes[index];
        clearPending(index);
        pressed[index] = false;
        listener.onButtonRelease(
                TARGETS[index],
                releaseSettings,
                releaseEventTime);
    }

    private void scheduleEarliestRelease() {
        cancelScheduledDrain();
        if (destroyed) {
            return;
        }

        long earliestDueTime = Long.MAX_VALUE;
        for (int index = 0; index < pending.length; index++) {
            if (pending[index]) {
                earliestDueTime = Math.min(
                        earliestDueTime,
                        dueTimes[index]);
            }
        }
        if (earliestDueTime == Long.MAX_VALUE) {
            return;
        }

        scheduled = true;
        scheduler.schedule(
                scheduledDrain,
                Math.max(0, earliestDueTime - scheduler.now()));
    }

    private void cancelScheduledDrain() {
        if (!scheduled) {
            return;
        }
        scheduled = false;
        scheduler.cancel(scheduledDrain);
    }

    private void clearPending(int index) {
        pending[index] = false;
        dueTimes[index] = 0;
        sequences[index] = 0;
        eventTimes[index] = 0;
        settings[index] = null;
    }

    private void clearAllState() {
        Arrays.fill(pressed, false);
        Arrays.fill(downTimes, 0);
        Arrays.fill(pending, false);
        Arrays.fill(dueTimes, 0);
        Arrays.fill(sequences, 0);
        Arrays.fill(eventTimes, 0);
        Arrays.fill(settings, null);
        nextSequence = 0;
    }
}
