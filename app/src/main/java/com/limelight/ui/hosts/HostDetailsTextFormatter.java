package com.limelight.ui.hosts;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRuntimeSnapshot;

import java.util.Objects;

/** Formats immutable host state for the diagnostic details dialog. */
public final class HostDetailsTextFormatter {
    private HostDetailsTextFormatter() {
    }

    public static String format(HostRuntimeSnapshot host) {
        HostRuntimeSnapshot source = Objects.requireNonNull(host, "host");
        HostConnectionState state = source.getConnectionState();
        return "ComputerDetails{" +
                "state=" + state.getReachability() +
                ", pairState=" + legacyPairingLabel(
                        state.getPairingStatus()) +
                ", hasActiveAddress=" +
                        (state.getActiveEndpoint() != null) +
                ", hasLocalAddress=" + hasEndpoint(
                        source,
                        HostEndpoint.Kind.LOCAL_IPV4) +
                ", hasRemoteAddress=" + hasEndpoint(
                        source,
                        HostEndpoint.Kind.REMOTE) +
                ", hasManualAddress=" + hasEndpoint(
                        source,
                        HostEndpoint.Kind.MANUAL) +
                ", hasIpv6Address=" + hasEndpoint(
                        source,
                        HostEndpoint.Kind.LOCAL_IPV6) +
                ", hasPinnedCertificate=" +
                        (source.getPersistedHost()
                                .getPinnedCertificate() != null) +
                '}';
    }

    private static boolean hasEndpoint(
            HostRuntimeSnapshot host,
            HostEndpoint.Kind kind) {
        return host.getRecord().getEndpoint(kind) != null;
    }

    private static String legacyPairingLabel(
            HostConnectionState.PairingStatus status) {
        return status == HostConnectionState.PairingStatus.UNKNOWN
                ? "null"
                : status.name();
    }
}
