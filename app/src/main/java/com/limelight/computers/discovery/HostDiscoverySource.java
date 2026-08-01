package com.limelight.computers.discovery;

/** Lifecycle contract for endpoint discovery implementations. */
public interface HostDiscoverySource extends AutoCloseable {
    interface Listener {
        void onHostDiscovered(HostDiscoveryCandidate candidate);

        void onDiscoveryFailure();
    }

    void awaitReady() throws InterruptedException;

    void start(int queryPeriodMs);

    void stop();

    @Override
    void close();
}
