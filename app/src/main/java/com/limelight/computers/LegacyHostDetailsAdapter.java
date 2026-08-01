package com.limelight.computers;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Explicit compatibility adapter at the remaining mutable NvHTTP DTO edge.
 *
 * <p>Repository and host-domain code use immutable records. Protocol parsing
 * still produces {@link ComputerDetails}; all conversion is centralized here
 * until the NvHTTP response model itself is replaced.</p>
 */
public final class LegacyHostDetailsAdapter {
    private LegacyHostDetailsAdapter() {
    }

    public static HostRecord toHostRecord(ComputerDetails details) {
        return toHostRecord(details, null);
    }

    /**
     * Converts a legacy protocol DTO while retaining repository-owned user
     * presentation that the DTO cannot represent.
     */
    public static HostRecord toHostRecord(
            ComputerDetails details,
            String userAlias) {
        ComputerDetails source = Objects.requireNonNull(
                details,
                "details");
        List<HostEndpoint> endpoints = new ArrayList<>(4);
        addEndpoint(
                endpoints,
                HostEndpoint.Kind.LOCAL_IPV4,
                source.localAddress);
        addEndpoint(
                endpoints,
                HostEndpoint.Kind.REMOTE,
                source.remoteAddress);
        addEndpoint(
                endpoints,
                HostEndpoint.Kind.MANUAL,
                source.manualAddress);
        addEndpoint(
                endpoints,
                HostEndpoint.Kind.LOCAL_IPV6,
                source.ipv6Address);
        return new HostRecord(
                new HostIdentity(
                        HostId.of(source.uuid),
                        source.name,
                        userAlias),
                endpoints,
                source.macAddress);
    }

    public static PersistedHost toPersistedHost(
            ComputerDetails details) {
        ComputerDetails source = Objects.requireNonNull(
                details,
                "details");
        return new PersistedHost(
                toHostRecord(source),
                source.serverCert);
    }

    public static ComputerDetails toComputerDetails(
            PersistedHost host) {
        PersistedHost source = Objects.requireNonNull(host, "host");
        HostRecord record = source.getRecord();
        ComputerDetails details = new ComputerDetails();
        details.uuid = record.getIdentity().getId().getValue();
        details.name = record.getIdentity().getAdvertisedName();
        details.localAddress = toAddressTuple(record.getEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4));
        details.remoteAddress = toAddressTuple(record.getEndpoint(
                HostEndpoint.Kind.REMOTE));
        details.manualAddress = toAddressTuple(record.getEndpoint(
                HostEndpoint.Kind.MANUAL));
        details.ipv6Address = toAddressTuple(record.getEndpoint(
                HostEndpoint.Kind.LOCAL_IPV6));
        details.macAddress = record.getMacAddress();
        details.serverCert = source.getPinnedCertificate();
        details.externalPort = details.remoteAddress == null
                ? NvHTTP.DEFAULT_HTTP_PORT
                : details.remoteAddress.port;
        details.state = ComputerDetails.State.UNKNOWN;
        return details;
    }

    private static void addEndpoint(
            List<HostEndpoint> endpoints,
            HostEndpoint.Kind kind,
            ComputerDetails.AddressTuple tuple) {
        if (tuple != null) {
            endpoints.add(new HostEndpoint(
                    kind,
                    tuple.address,
                    tuple.port));
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
