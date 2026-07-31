package com.limelight.binding.input;

import java.util.Objects;

/**
 * Owns scheduled reporting for one controller mouse-emulation context.
 */
final class ControllerMouseEmulationSession {
    interface Scheduler {
        void schedule(Runnable runnable, long delayMs);

        void cancel(Runnable runnable);
    }

    static final long REPORT_PERIOD_MS = 50;

    private final Scheduler scheduler;
    private final Runnable reportAction;
    private final Runnable scheduledReport =
            new Runnable() {
                @Override
                public void run() {
                    if (!active || destroyed) {
                        return;
                    }
                    reportAction.run();
                    if (active && !destroyed) {
                        scheduler.schedule(
                                this,
                                REPORT_PERIOD_MS);
                    }
                }
            };

    private boolean active;
    private boolean destroyed;

    ControllerMouseEmulationSession(
            Scheduler scheduler,
            Runnable reportAction) {
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "scheduler");
        this.reportAction = Objects.requireNonNull(
                reportAction,
                "reportAction");
    }

    boolean isActive() {
        return active;
    }

    boolean toggle() {
        setActive(!active);
        return active;
    }

    void setActive(boolean active) {
        scheduler.cancel(scheduledReport);
        if (destroyed) {
            this.active = false;
            return;
        }

        this.active = active;
        if (active) {
            scheduler.schedule(
                    scheduledReport,
                    REPORT_PERIOD_MS);
        }
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        active = false;
        scheduler.cancel(scheduledReport);
    }
}
