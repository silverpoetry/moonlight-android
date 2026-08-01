package com.limelight.computers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class HostPollingClientLifecycleTest {
    @Test
    public void synchronousCallbacksBelongToInProgressStart() {
        HostPollingClientLifecycle lifecycle =
                new HostPollingClientLifecycle();
        lifecycle.activate();

        HostPollingClientLifecycle.StartToken token =
                lifecycle.beginStart();

        assertNotNull(token);
        assertTrue(lifecycle.owns(token));
        assertTrue(lifecycle.completeStart(token, () -> { }));
        assertTrue(lifecycle.owns(token));
        assertNull(lifecycle.beginStart());
    }

    @Test
    public void pauseDuringStartRejectsLateCompletion() {
        HostPollingClientLifecycle lifecycle =
                new HostPollingClientLifecycle();
        AtomicInteger closes = new AtomicInteger();
        lifecycle.activate();
        HostPollingClientLifecycle.StartToken token =
                lifecycle.beginStart();

        assertNull(lifecycle.deactivate());
        assertFalse(lifecycle.owns(token));
        assertFalse(lifecycle.completeStart(
                token,
                closes::incrementAndGet));
        assertTrue(closes.get() == 1);
    }

    @Test
    public void staleConnectionCannotReplaceNewSubscription() {
        HostPollingClientLifecycle lifecycle =
                new HostPollingClientLifecycle();
        AtomicInteger staleCloses = new AtomicInteger();
        lifecycle.activate();
        HostPollingClientLifecycle.StartToken stale =
                lifecycle.beginStart();
        lifecycle.onConnectionLost();
        HostPollingClientLifecycle.StartToken current =
                lifecycle.beginStart();

        assertFalse(lifecycle.completeStart(
                stale,
                staleCloses::incrementAndGet));
        assertTrue(lifecycle.completeStart(current, () -> { }));
        assertFalse(lifecycle.owns(stale));
        assertTrue(lifecycle.owns(current));
        assertTrue(staleCloses.get() == 1);
    }

    @Test
    public void disconnectClosesOnceAndAllowsForegroundRetry() {
        HostPollingClientLifecycle lifecycle =
                new HostPollingClientLifecycle();
        AtomicInteger closes = new AtomicInteger();
        lifecycle.activate();
        HostPollingClientLifecycle.StartToken first =
                lifecycle.beginStart();
        assertTrue(lifecycle.completeStart(
                first,
                closes::incrementAndGet));

        run(lifecycle.onConnectionLost());
        assertNull(lifecycle.onConnectionLost());
        assertNotNull(lifecycle.beginStart());
        assertTrue(closes.get() == 1);
    }

    @Test
    public void destroyPermanentlyRejectsNewStarts() {
        HostPollingClientLifecycle lifecycle =
                new HostPollingClientLifecycle();
        lifecycle.activate();
        HostPollingClientLifecycle.StartToken token =
                lifecycle.beginStart();
        assertTrue(lifecycle.completeStart(token, () -> { }));

        run(lifecycle.destroy());
        lifecycle.activate();

        assertNull(lifecycle.beginStart());
        assertFalse(lifecycle.owns(token));
    }

    private static void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
