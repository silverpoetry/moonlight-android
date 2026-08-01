package com.limelight.computers.model;

import java.util.List;
import java.util.Objects;

/** Probe result that can update host metadata but never carries credentials. */
public final class HostObservation {
    private final HostRecord observedRecord;

    public HostObservation(
            HostId id,
            String advertisedName,
            List<HostEndpoint> endpoints,
            String macAddress) {
        observedRecord = new HostRecord(
                new HostIdentity(
                        Objects.requireNonNull(id, "id"),
                        advertisedName,
                        null),
                endpoints,
                macAddress);
    }

    HostRecord asRecord() {
        return observedRecord;
    }
}
