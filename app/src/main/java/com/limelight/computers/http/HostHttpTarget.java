package com.limelight.computers.http;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRuntimeSnapshot;

import java.security.cert.X509Certificate;
import java.util.Objects;

/** Immutable endpoint and credential snapshot for host HTTP operations. */
public final class HostHttpTarget {
    private final String address;
    private final int port;
    private final int httpsPort;
    private final String uniqueId;
    private final X509Certificate pinnedCertificate;

    public HostHttpTarget(
            String address,
            int port,
            int httpsPort,
            String uniqueId,
            X509Certificate pinnedCertificate) {
        this.address = requireText(address, "address");
        this.port = requirePort(port, "port", false);
        this.httpsPort = requirePort(
                httpsPort,
                "httpsPort",
                true);
        this.uniqueId = requireText(uniqueId, "uniqueId");
        this.pinnedCertificate = pinnedCertificate;
    }

    public static HostHttpTarget from(
            HostRuntimeSnapshot host,
            String uniqueId) {
        HostRuntimeSnapshot source = Objects.requireNonNull(host, "host");
        HostEndpoint endpoint = source.getConnectionState()
                .getActiveEndpoint();
        if (endpoint == null) {
            throw new IllegalArgumentException(
                    "Host has no active endpoint");
        }
        return new HostHttpTarget(
                endpoint.getAddress(),
                endpoint.getPort(),
                source.getConnectionState().getHttpsPort(),
                uniqueId,
                source.getPersistedHost().getPinnedCertificate());
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public X509Certificate getPinnedCertificate() {
        return pinnedCertificate;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostHttpTarget)) {
            return false;
        }
        HostHttpTarget target = (HostHttpTarget) other;
        return address.equals(target.address) &&
                port == target.port &&
                httpsPort == target.httpsPort &&
                uniqueId.equals(target.uniqueId) &&
                Objects.equals(
                        pinnedCertificate,
                        target.pinnedCertificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                address,
                port,
                httpsPort,
                uniqueId,
                pinnedCertificate);
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static int requirePort(
            int value,
            String name,
            boolean optional) {
        int minimum = optional ? 0 : 1;
        if (value < minimum || value > 65_535) {
            throw new IllegalArgumentException(
                    name + " is outside the valid port range");
        }
        return value;
    }
}
