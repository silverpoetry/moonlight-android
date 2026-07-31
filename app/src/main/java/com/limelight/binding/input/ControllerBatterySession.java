package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns periodic battery reporting for one physical controller context.
 */
final class ControllerBatterySession {
    interface Scheduler {
        void schedule(Runnable runnable, long delayMs);

        void cancel(Runnable runnable);
    }

    interface Reporter {
        void report();
    }

    private final Scheduler scheduler;
    private final Reporter reporter;
    private final long intervalMs;

    private long generation;
    private boolean enabled;
    private boolean destroyed;
    private ScheduledTick pendingTick;

    ControllerBatterySession(
            Scheduler scheduler,
            Reporter reporter,
            long intervalMs) {
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "scheduler");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        if (intervalMs <= 0) {
            throw new IllegalArgumentException(
                    "intervalMs must be positive");
        }
        this.intervalMs = intervalMs;
    }

    void setEnabled(boolean enabled) {
        ScheduledTick previous;
        ScheduledTick next = null;
        synchronized (this) {
            if (destroyed || this.enabled == enabled) {
                return;
            }
            this.enabled = enabled;
            generation++;
            previous = pendingTick;
            pendingTick = null;
            if (enabled) {
                next = new ScheduledTick(generation);
                pendingTick = next;
            }
        }

        if (previous != null) {
            scheduler.cancel(previous);
        }
        if (next != null) {
            scheduler.schedule(next, 0);
        }
    }

    void destroy() {
        ScheduledTick previous;
        synchronized (this) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            enabled = false;
            generation++;
            previous = pendingTick;
            pendingTick = null;
        }
        if (previous != null) {
            scheduler.cancel(previous);
        }
    }

    private void runTick(ScheduledTick tick) {
        synchronized (this) {
            if (destroyed || !enabled || pendingTick != tick ||
                    generation != tick.generation) {
                return;
            }
            pendingTick = null;
        }

        reporter.report();

        ScheduledTick next;
        synchronized (this) {
            if (destroyed || !enabled ||
                    generation != tick.generation ||
                    pendingTick != null) {
                return;
            }
            next = new ScheduledTick(generation);
            pendingTick = next;
        }
        scheduler.schedule(next, intervalMs);
    }

    private final class ScheduledTick implements Runnable {
        private final long generation;

        private ScheduledTick(long generation) {
            this.generation = generation;
        }

        @Override
        public void run() {
            runTick(this);
        }
    }
}
