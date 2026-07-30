package com.limelight.nvstream;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Serializes access to a process-global resource using ownership-bearing
 * leases. Closing a lease is idempotent and cannot release a permit owned by
 * another caller.
 */
final class ConnectionLeaseManager {
    private final Semaphore available = new Semaphore(1, true);

    Lease acquire() throws InterruptedException {
        available.acquire();
        return new Lease(this);
    }

    Lease tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return available.tryAcquire(timeout, unit) ? new Lease(this) : null;
    }

    private void release() {
        available.release();
    }

    static final class Lease implements AutoCloseable {
        private final ConnectionLeaseManager owner;
        private boolean closed;

        private Lease(ConnectionLeaseManager owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                closed = true;
            }
            owner.release();
        }
    }
}
