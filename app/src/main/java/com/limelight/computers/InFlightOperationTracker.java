package com.limelight.computers;

/**
 * Tracks a bounded set of in-flight operations and provides an interruptible
 * idle barrier. Each operation owns exactly one idempotent lease.
 */
final class InFlightOperationTracker {
    final class Lease implements AutoCloseable {
        private boolean closed;

        private Lease() {
        }

        @Override
        public void close() {
            synchronized (InFlightOperationTracker.this) {
                if (closed) {
                    return;
                }
                closed = true;
                activeCount--;
                if (activeCount == 0) {
                    InFlightOperationTracker.this.notifyAll();
                }
            }
        }
    }

    private int activeCount;

    synchronized Lease begin() {
        if (activeCount == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "In-flight operation count overflow");
        }
        activeCount++;
        return new Lease();
    }

    synchronized void awaitIdle() throws InterruptedException {
        while (activeCount != 0) {
            wait();
        }
    }

    synchronized int getActiveCount() {
        return activeCount;
    }
}
