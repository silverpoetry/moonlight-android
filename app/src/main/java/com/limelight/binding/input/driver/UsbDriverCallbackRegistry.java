package com.limelight.binding.input.driver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores the single active callback lease for the local USB driver service.
 * A stale owner cannot clear callbacks installed by a newer stream session.
 */
final class UsbDriverCallbackRegistry {
    static final class Callbacks {
        final long leaseId;
        final UsbDriverListener inputListener;
        final UsbDriverService.UsbDriverStateListener stateListener;

        private Callbacks(
                long leaseId,
                UsbDriverListener inputListener,
                UsbDriverService.UsbDriverStateListener stateListener) {
            this.leaseId = leaseId;
            this.inputListener = inputListener;
            this.stateListener = stateListener;
        }
    }

    private final AtomicLong nextLeaseId = new AtomicLong();
    private final AtomicReference<Callbacks> activeCallbacks =
            new AtomicReference<>();

    long acquire(
            UsbDriverListener inputListener,
            UsbDriverService.UsbDriverStateListener stateListener) {
        long leaseId = nextLeaseId.incrementAndGet();
        Callbacks callbacks = new Callbacks(
                leaseId,
                Objects.requireNonNull(inputListener, "inputListener"),
                Objects.requireNonNull(stateListener, "stateListener"));
        activeCallbacks.set(callbacks);
        return leaseId;
    }

    void release(long leaseId) {
        if (leaseId == 0) {
            return;
        }

        while (true) {
            Callbacks current = activeCallbacks.get();
            if (current == null || current.leaseId != leaseId) {
                return;
            }
            if (activeCallbacks.compareAndSet(current, null)) {
                return;
            }
        }
    }

    Callbacks get() {
        return activeCallbacks.get();
    }

    void clear() {
        activeCallbacks.set(null);
    }
}
