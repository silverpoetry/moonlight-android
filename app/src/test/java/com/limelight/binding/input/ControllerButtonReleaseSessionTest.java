package com.limelight.binding.input;

import com.limelight.settings.controller.ControllerSettings;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ControllerButtonReleaseSessionTest {
    private static final ControllerSettings SETTINGS =
            ControllerSettings.builder().build();

    @Test
    public void releaseAfterMinimumDurationRemainsSynchronous() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 25;

        assertFalse(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                100));
        assertNull(fixture.scheduler.scheduledRunnable);
        assertTrue(fixture.releases.isEmpty());
    }

    @Test
    public void quickReleaseRunsAtHostVisibleMinimumDuration() {
        Fixture fixture = new Fixture();
        fixture.scheduler.now = 100;
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 105;

        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                200));
        fixture.scheduler.advanceTo(124);
        assertTrue(fixture.releases.isEmpty());

        fixture.scheduler.advanceTo(125);
        assertReleases(fixture, "A:200");
    }

    @Test
    public void repeatDownDoesNotRestartMinimumDuration() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 20;
        fixture.session.recordButtonDown(targetA(), 1);
        fixture.scheduler.now = 25;

        assertFalse(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                100));
    }

    @Test
    public void duplicateButtonUpDoesNotExtendPendingDeadline() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 5;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                10));

        fixture.scheduler.now = 20;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                11));
        fixture.scheduler.advanceTo(25);

        assertReleases(fixture, "A:10");
    }

    @Test
    public void abandonedButtonCancelsPendingRelease() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 5;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                10));

        fixture.session.abandonButton(targetA());
        fixture.scheduler.advanceTo(100);

        assertTrue(fixture.releases.isEmpty());
        assertNull(fixture.scheduler.scheduledRunnable);
    }

    @Test
    public void newPressFlushesPendingReleaseBeforeNextDown() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 5;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                10));

        fixture.scheduler.now = 10;
        fixture.session.flushPendingRelease(targetA());
        fixture.session.recordButtonDown(targetA(), 0);
        assertReleases(fixture, "A:10");

        fixture.scheduler.now = 15;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                20));
        fixture.scheduler.advanceTo(35);
        assertReleases(fixture, "A:10", "A:20");
    }

    @Test
    public void multipleTargetsReleaseByDeadlineThenArrivalOrder() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 1;
        fixture.session.recordButtonDown(targetB(), 0);
        fixture.scheduler.now = 5;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                10));
        assertTrue(fixture.session.deferButtonUp(
                targetB(),
                SETTINGS,
                11));

        fixture.scheduler.advanceTo(25);
        assertReleases(fixture, "A:10");
        fixture.scheduler.advanceTo(26);
        assertReleases(fixture, "A:10", "B:11");
    }

    @Test
    public void deviceRecreationTransfersPendingDeadlineOnce() {
        FakeScheduler scheduler = new FakeScheduler();
        List<String> oldReleases = new ArrayList<>();
        List<String> newReleases = new ArrayList<>();
        ControllerButtonReleaseSession previous = session(
                scheduler,
                oldReleases);
        ControllerButtonReleaseSession replacement = session(
                scheduler,
                newReleases);
        previous.recordButtonDown(targetA(), 0);
        scheduler.now = 5;
        assertTrue(previous.deferButtonUp(
                targetA(),
                SETTINGS,
                20));

        scheduler.now = 10;
        replacement.restoreFrom(previous);
        previous.destroy();
        scheduler.advanceTo(25);

        assertTrue(oldReleases.isEmpty());
        assertTrue(newReleases.equals(
                Arrays.asList("A:20")));
    }

    @Test
    public void destructionCancelsLateRelease() {
        Fixture fixture = new Fixture();
        fixture.session.recordButtonDown(targetA(), 0);
        fixture.scheduler.now = 5;
        assertTrue(fixture.session.deferButtonUp(
                targetA(),
                SETTINGS,
                10));

        fixture.session.destroy();
        fixture.scheduler.advanceTo(100);

        assertTrue(fixture.releases.isEmpty());
        assertNull(fixture.scheduler.scheduledRunnable);
    }

    private static ControllerButtonReleaseSession session(
            FakeScheduler scheduler,
            List<String> releases) {
        return new ControllerButtonReleaseSession(
                scheduler,
                (target, settings, eventTime) ->
                        releases.add(
                                target.name() + ":" + eventTime));
    }

    private static ControllerDigitalButtonMapping.Target targetA() {
        return ControllerDigitalButtonMapping.Target.A;
    }

    private static ControllerDigitalButtonMapping.Target targetB() {
        return ControllerDigitalButtonMapping.Target.B;
    }

    private static void assertReleases(
            Fixture fixture,
            String... releases) {
        assertTrue(fixture.releases.equals(
                Arrays.asList(releases)));
    }

    private static final class Fixture {
        final FakeScheduler scheduler = new FakeScheduler();
        final List<String> releases = new ArrayList<>();
        final ControllerButtonReleaseSession session =
                session(scheduler, releases);
    }

    private static final class FakeScheduler
            implements ControllerButtonReleaseSession.Scheduler {
        long now;
        long scheduledAt;
        Runnable scheduledRunnable;

        @Override
        public long now() {
            return now;
        }

        @Override
        public void schedule(
                Runnable runnable,
                long delayMs) {
            scheduledRunnable = runnable;
            scheduledAt = now + delayMs;
        }

        @Override
        public void cancel(Runnable runnable) {
            if (scheduledRunnable == runnable) {
                scheduledRunnable = null;
            }
        }

        void advanceTo(long targetTime) {
            now = targetTime;
            while (scheduledRunnable != null &&
                    scheduledAt <= now) {
                Runnable runnable = scheduledRunnable;
                scheduledRunnable = null;
                runnable.run();
            }
        }
    }
}
