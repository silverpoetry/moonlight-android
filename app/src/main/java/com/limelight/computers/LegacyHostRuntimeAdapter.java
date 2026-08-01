package com.limelight.computers;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostObservation;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeObservation;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Converts immutable runtime state only at the remaining NvHTTP DTO edge. */
public final class LegacyHostRuntimeAdapter {
    private LegacyHostRuntimeAdapter() {
    }

    public static HostRuntimeSnapshot fromPersistedHost(
            PersistedHost host) {
        PersistedHost source = Objects.requireNonNull(host, "host");
        return new HostRuntimeSnapshot(
                source,
                new HostConnectionState(
                        source.getRecord().getIdentity().getId(),
                        HostConnectionState.Reachability.UNKNOWN,
                        HostConnectionState.PairingStatus.UNKNOWN,
                        null,
                        0,
                        0),
                null,
                false);
    }

    public static HostRuntimeSnapshot fromComputerDetails(
            ComputerDetails details,
            String userAlias) {
        ComputerDetails source = Objects.requireNonNull(details, "details");
        HostRecord record = LegacyHostDetailsAdapter.toHostRecord(
                source,
                userAlias);
        return new HostRuntimeSnapshot(
                new PersistedHost(record, source.serverCert),
                new HostConnectionState(
                        record.getIdentity().getId(),
                        toReachability(source.state),
                        toPairingStatus(source.pairState),
                        findActiveEndpoint(record, source.activeAddress),
                        source.httpsPort,
                        source.runningGameId),
                source.rawAppList,
                source.nvidiaServer);
    }

    public static HostRuntimeObservation toObservation(
            ComputerDetails details,
            HostEndpoint activeEndpoint) {
        ComputerDetails source = Objects.requireNonNull(details, "details");
        HostRecord record = LegacyHostDetailsAdapter.toHostRecord(source);
        return createObservation(source, record, activeEndpoint);
    }

    public static HostRuntimeObservation toObservation(
            ComputerDetails details) {
        ComputerDetails source = Objects.requireNonNull(details, "details");
        HostRecord record = LegacyHostDetailsAdapter.toHostRecord(source);
        return createObservation(
                source,
                record,
                findActiveEndpoint(record, source.activeAddress));
    }

    private static HostRuntimeObservation createObservation(
            ComputerDetails source,
            HostRecord record,
            HostEndpoint activeEndpoint) {
        HostIdentity identity = record.getIdentity();
        List<HostEndpoint> observedEndpoints = new ArrayList<>(4);
        for (HostEndpoint endpoint : record.getEndpoints()) {
            if (endpoint.getKind() == HostEndpoint.Kind.LOCAL_IPV4 &&
                    endpoint.getAddress().startsWith("127.")) {
                continue;
            }
            observedEndpoints.add(endpoint);
        }
        return new HostRuntimeObservation(
                new HostObservation(
                        identity.getId(),
                        identity.getAdvertisedName(),
                        observedEndpoints,
                        record.getMacAddress()),
                new HostConnectionState(
                        identity.getId(),
                        toReachability(source.state),
                        toPairingStatus(source.pairState),
                        activeEndpoint,
                        source.httpsPort,
                        source.runningGameId),
                source.externalPort,
                source.rawAppList,
                source.nvidiaServer);
    }

    public static ComputerDetails toComputerDetails(
            HostRuntimeSnapshot snapshot) {
        HostRuntimeSnapshot source = Objects.requireNonNull(
                snapshot,
                "snapshot");
        ComputerDetails details = LegacyHostDetailsAdapter.toComputerDetails(
                source.getPersistedHost());
        HostConnectionState connection = source.getConnectionState();
        details.state = toLegacyState(connection.getReachability());
        details.pairState = toLegacyPairState(
                connection.getPairingStatus());
        details.activeAddress = toAddressTuple(
                connection.getActiveEndpoint());
        details.httpsPort = connection.getHttpsPort();
        details.runningGameId = connection.getRunningAppId();
        details.rawAppList = source.getRawAppList();
        details.nvidiaServer = source.isNvidiaServer();
        return details;
    }

    private static HostEndpoint findActiveEndpoint(
            HostRecord record,
            ComputerDetails.AddressTuple activeAddress) {
        if (activeAddress == null) {
            return null;
        }
        for (HostEndpoint endpoint : record.getEndpoints()) {
            if (endpoint.getPort() == activeAddress.port &&
                    endpoint.getAddress().equalsIgnoreCase(
                            activeAddress.address)) {
                return endpoint;
            }
        }
        return null;
    }

    private static HostConnectionState.Reachability toReachability(
            ComputerDetails.State state) {
        if (state == ComputerDetails.State.ONLINE) {
            return HostConnectionState.Reachability.ONLINE;
        }
        if (state == ComputerDetails.State.OFFLINE) {
            return HostConnectionState.Reachability.OFFLINE;
        }
        return HostConnectionState.Reachability.UNKNOWN;
    }

    private static ComputerDetails.State toLegacyState(
            HostConnectionState.Reachability reachability) {
        switch (reachability) {
            case ONLINE:
                return ComputerDetails.State.ONLINE;
            case OFFLINE:
                return ComputerDetails.State.OFFLINE;
            case UNKNOWN:
                return ComputerDetails.State.UNKNOWN;
            default:
                throw new AssertionError("Unhandled reachability");
        }
    }

    private static HostConnectionState.PairingStatus toPairingStatus(
            PairingManager.PairState state) {
        if (state == PairingManager.PairState.PAIRED) {
            return HostConnectionState.PairingStatus.PAIRED;
        }
        if (state == PairingManager.PairState.NOT_PAIRED) {
            return HostConnectionState.PairingStatus.NOT_PAIRED;
        }
        return HostConnectionState.PairingStatus.UNKNOWN;
    }

    private static PairingManager.PairState toLegacyPairState(
            HostConnectionState.PairingStatus status) {
        switch (status) {
            case PAIRED:
                return PairingManager.PairState.PAIRED;
            case NOT_PAIRED:
                return PairingManager.PairState.NOT_PAIRED;
            case UNKNOWN:
                return null;
            default:
                throw new AssertionError("Unhandled pairing status");
        }
    }

    private static ComputerDetails.AddressTuple toAddressTuple(
            HostEndpoint endpoint) {
        return endpoint == null
                ? null
                : new ComputerDetails.AddressTuple(
                        endpoint.getAddress(),
                        endpoint.getPort());
    }
}
