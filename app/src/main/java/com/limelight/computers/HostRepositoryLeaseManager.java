package com.limelight.computers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the host repository alive while service-owned operations are using it.
 *
 * <p>Acquisition and final release are one atomic state transition, so service
 * destruction cannot close the database between a caller's liveness check and
 * reference increment.</p>
 */
final class HostRepositoryLeaseManager implements AutoCloseable {
    private final Object stateLock = new Object();
    private final Runnable closeAction;
    private int referenceCount = 1;
    private boolean ownerClosed;

    HostRepositoryLeaseManager(Runnable closeAction) {
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    }

    Lease tryAcquire() {
        synchronized (stateLock) {
            if (ownerClosed) {
                return null;
            }
            if (referenceCount == Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "Host repository lease count overflow");
            }
            referenceCount++;
            return new Lease(this);
        }
    }

    @Override
    public void close() {
        synchronized (stateLock) {
            if (ownerClosed) {
                return;
            }
            ownerClosed = true;
        }
        releaseReference();
    }

    private void releaseReference() {
        boolean closeRepository;
        synchronized (stateLock) {
            if (referenceCount == 0) {
                throw new IllegalStateException(
                        "Host repository lease count underflow");
            }
            referenceCount--;
            closeRepository = referenceCount == 0;
        }
        if (closeRepository) {
            closeAction.run();
        }
    }

    static final class Lease implements AutoCloseable {
        private final HostRepositoryLeaseManager owner;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(HostRepositoryLeaseManager owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.releaseReference();
            }
        }
    }
}
