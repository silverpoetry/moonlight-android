package com.limelight.nvstream;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class ConnectionLeaseManagerTest {
    @Test
    public void leaseCanReleaseOnlyItsOwnPermitOnce() throws Exception {
        ConnectionLeaseManager manager = new ConnectionLeaseManager();

        ConnectionLeaseManager.Lease first = manager.acquire();
        assertNull(manager.tryAcquire(0, TimeUnit.MILLISECONDS));

        first.close();
        first.close();

        ConnectionLeaseManager.Lease second =
                manager.tryAcquire(0, TimeUnit.MILLISECONDS);
        assertNotNull(second);
        assertNull(manager.tryAcquire(0, TimeUnit.MILLISECONDS));

        second.close();
        ConnectionLeaseManager.Lease third =
                manager.tryAcquire(0, TimeUnit.MILLISECONDS);
        assertNotNull(third);
        third.close();
    }
}
