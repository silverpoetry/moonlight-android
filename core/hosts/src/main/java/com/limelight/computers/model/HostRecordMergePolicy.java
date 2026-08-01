package com.limelight.computers.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Deterministic persistent-host merge policy for one completed probe. */
public final class HostRecordMergePolicy {
    private HostRecordMergePolicy() {
    }

    public static HostRecord merge(
            HostRecord existing,
            HostObservation observation) {
        HostRecord observed = Objects.requireNonNull(
                observation, "observation").asRecord();
        if (existing == null) {
            return observed;
        }
        if (!existing.getIdentity().getId().equals(
                observed.getIdentity().getId())) {
            throw new IllegalArgumentException(
                    "Cannot merge observations from different hosts");
        }

        Map<HostEndpoint.Kind, HostEndpoint> endpoints =
                new EnumMap<>(HostEndpoint.Kind.class);
        for (HostEndpoint endpoint : existing.getEndpoints()) {
            endpoints.put(endpoint.getKind(), endpoint);
        }
        for (HostEndpoint endpoint : observed.getEndpoints()) {
            endpoints.put(endpoint.getKind(), endpoint);
        }

        HostIdentity identity = new HostIdentity(
                existing.getIdentity().getId(),
                observed.getIdentity().getAdvertisedName(),
                existing.getIdentity().getUserAlias());
        String macAddress = observed.getMacAddress() == null ?
                existing.getMacAddress() : observed.getMacAddress();
        return new HostRecord(
                identity,
                new ArrayList<>(endpoints.values()),
                macAddress);
    }
}
