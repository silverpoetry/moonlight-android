package com.limelight.computers;

/**
 * Owns the serialized boundary for admitting a previously unknown host.
 *
 * <p>Admission performs identity discovery before a stable host ID exists, so
 * it cannot yet use a per-host lock. This gate gives every discovery source
 * one interruptible lane until identity has been resolved. Closing the owner
 * rejects queued work and interrupts the operation currently holding the
 * lane.</p>
 */
final class HostAdmissionGate implements AutoCloseable {
    final class Lease implements AutoCloseable {
        private boolean released;

        private Lease() {
        }

        @Override
        public void close() {
            synchronized (HostAdmissionGate.this) {
                if (released) {
                    return;
                }
                released = true;
                active = false;
                ownerThread = null;
                HostAdmissionGate.this.notifyAll();
            }
        }
    }

    private boolean active;
    private boolean closed;
    private Thread ownerThread;

    synchronized Lease acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        while (active && !closed) {
            wait();
        }
        if (closed) {
            throw new IllegalStateException(
                    "Host admission owner is closed");
        }
        active = true;
        ownerThread = Thread.currentThread();
        return new Lease();
    }

    @Override
    public void close() {
        Thread threadToInterrupt;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            threadToInterrupt = ownerThread;
            notifyAll();
        }
        if (threadToInterrupt != null &&
                threadToInterrupt != Thread.currentThread()) {
            threadToInterrupt.interrupt();
        }
    }
}
