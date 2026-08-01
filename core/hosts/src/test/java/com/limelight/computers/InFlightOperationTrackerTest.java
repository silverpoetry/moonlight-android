package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InFlightOperationTrackerTest {
    @Test
    public void idleBarrierWaitsForEveryLease() throws Exception {
        InFlightOperationTracker tracker =
                new InFlightOperationTracker();
        InFlightOperationTracker.Lease first = tracker.begin();
        InFlightOperationTracker.Lease second = tracker.begin();
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicBoolean idleReached = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            waiting.countDown();
            try {
                tracker.awaitIdle();
                idleReached.set(true);
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        first.close();
        assertFalse(idleReached.get());
        second.close();
        waiter.join(1000L);

        assertFalse(waiter.isAlive());
        assertTrue(idleReached.get());
        assertEquals(0, tracker.getActiveCount());
    }

    @Test
    public void leaseCloseIsIdempotent() {
        InFlightOperationTracker tracker =
                new InFlightOperationTracker();
        InFlightOperationTracker.Lease lease = tracker.begin();

        lease.close();
        lease.close();

        assertEquals(0, tracker.getActiveCount());
    }

    @Test
    public void interruptedBarrierPreservesOutstandingLease()
            throws Exception {
        InFlightOperationTracker tracker =
                new InFlightOperationTracker();
        InFlightOperationTracker.Lease lease = tracker.begin();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                tracker.awaitIdle();
            }
            catch (InterruptedException error) {
                interrupted.set(true);
            }
        });

        waiter.start();
        waiter.interrupt();
        waiter.join(1000L);

        assertTrue(interrupted.get());
        assertEquals(1, tracker.getActiveCount());
        lease.close();
    }
}
