package com.limelight.computers.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic transition from a completed probe to a runtime snapshot. */
public final class HostRuntimeMergePolicy {
    private HostRuntimeMergePolicy() {
    }

    /**
     * Combines a freshly resolved runtime candidate with the repository-owned
     * alias, unobserved endpoints, and pinned credential for the same host.
     */
    public static HostRuntimeSnapshot rebaseOnPersistedHost(
            HostRuntimeSnapshot candidate,
            PersistedHost persistedHost) {
        HostRuntimeSnapshot observed = Objects.requireNonNull(
                candidate,
                "candidate");
        PersistedHost persisted = Objects.requireNonNull(
                persistedHost,
                "persistedHost");
        HostRecord observedRecord = observed.getRecord();
        HostRecord mergedRecord = HostRecordMergePolicy.merge(
                persisted.getRecord(),
                new HostObservation(
                        observedRecord.getIdentity().getId(),
                        observedRecord.getIdentity().getAdvertisedName(),
                        observedRecord.getEndpoints(),
                        observedRecord.getMacAddress()));
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        mergedRecord,
                        persisted.getPinnedCertificate()),
                observed.getConnectionState(),
                observed.getRawAppList(),
                observed.isNvidiaServer());
    }

    public static HostRuntimeSnapshot merge(
            HostRuntimeSnapshot existing,
            HostRuntimeObservation observation) {
        HostRuntimeSnapshot current = Objects.requireNonNull(
                existing,
                "existing");
        HostRuntimeObservation observed = Objects.requireNonNull(
                observation,
                "observation");
        HostConnectionState observedConnection =
                observed.getConnectionState();
        if (!current.getRecord().getIdentity().getId().equals(
                observedConnection.getHostId())) {
            throw new IllegalArgumentException(
                    "Cannot merge observations from different hosts");
        }

        HostRecord observedRecord =
                observed.getHostObservation().asRecord();
        HostRecord mergedRecord = HostRecordMergePolicy.merge(
                current.getRecord(),
                observed.getHostObservation());
        if (observedRecord.getEndpoint(HostEndpoint.Kind.REMOTE) == null &&
                observed.getReportedExternalPort() > 0) {
            mergedRecord = replaceRemotePort(
                    mergedRecord,
                    observed.getReportedExternalPort());
        }

        HostEndpoint activeEndpoint =
                observedConnection.getActiveEndpoint() == null
                        ? current.getConnectionState().getActiveEndpoint()
                        : observedConnection.getActiveEndpoint();
        HostConnectionState mergedConnection = new HostConnectionState(
                observedConnection.getHostId(),
                observedConnection.getReachability(),
                observedConnection.getPairingStatus(),
                activeEndpoint,
                observedConnection.getHttpsPort(),
                observedConnection.getRunningAppId());
        String appList = observed.getRawAppList() == null
                ? current.getRawAppList()
                : observed.getRawAppList();
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        mergedRecord,
                        current.getPersistedHost()
                                .getPinnedCertificate()),
                mergedConnection,
                appList,
                observed.isNvidiaServer());
    }

    private static HostRecord replaceRemotePort(
            HostRecord record,
            int port) {
        HostEndpoint remote = record.getEndpoint(
                HostEndpoint.Kind.REMOTE);
        if (remote == null || remote.getPort() == port) {
            return record;
        }
        List<HostEndpoint> endpoints = new ArrayList<>(
                record.getEndpoints().size());
        for (HostEndpoint endpoint : record.getEndpoints()) {
            endpoints.add(endpoint.getKind() == HostEndpoint.Kind.REMOTE
                    ? new HostEndpoint(
                            HostEndpoint.Kind.REMOTE,
                            endpoint.getAddress(),
                            port)
                    : endpoint);
        }
        return new HostRecord(
                record.getIdentity(),
                endpoints,
                record.getMacAddress());
    }
}
