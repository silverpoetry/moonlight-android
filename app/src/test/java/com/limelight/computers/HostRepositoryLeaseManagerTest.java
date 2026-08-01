package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class HostRepositoryLeaseManagerTest {
    @Test
    public void ownerCloseWaitsForOutstandingLease() {
        AtomicInteger closeCount = new AtomicInteger();
        HostRepositoryLeaseManager manager =
                new HostRepositoryLeaseManager(closeCount::incrementAndGet);
        HostRepositoryLeaseManager.Lease lease = manager.tryAcquire();
        assertNotNull(lease);

        manager.close();

        assertEquals(0, closeCount.get());
        assertNull(manager.tryAcquire());

        lease.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    public void ownerAndLeaseCloseAreIdempotent() {
        AtomicInteger closeCount = new AtomicInteger();
        HostRepositoryLeaseManager manager =
                new HostRepositoryLeaseManager(closeCount::incrementAndGet);
        HostRepositoryLeaseManager.Lease first = manager.tryAcquire();
        HostRepositoryLeaseManager.Lease second = manager.tryAcquire();
        assertNotNull(first);
        assertNotNull(second);

        first.close();
        first.close();
        manager.close();
        manager.close();
        assertEquals(0, closeCount.get());

        second.close();
        second.close();
        assertEquals(1, closeCount.get());
        assertNull(manager.tryAcquire());
    }

    @Test
    public void ownerWithoutCallersClosesImmediately() {
        AtomicInteger closeCount = new AtomicInteger();
        HostRepositoryLeaseManager manager =
                new HostRepositoryLeaseManager(closeCount::incrementAndGet);

        manager.close();

        assertEquals(1, closeCount.get());
        assertNull(manager.tryAcquire());
    }
}
