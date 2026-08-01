package com.limelight.stream.launch;

import java.util.Arrays;
import java.util.Objects;

/** Immutable data required to hand a stream session to the Game Activity. */
public final class StreamLaunchRequest {
    private final String hostAddress;
    private final int hostPort;
    private final int httpsPort;
    private final String appName;
    private final int appId;
    private final boolean appSupportsHdr;
    private final String clientId;
    private final String hostId;
    private final String hostName;
    private final byte[] serverCertificate;

    public StreamLaunchRequest(
            String hostAddress,
            int hostPort,
            int httpsPort,
            String appName,
            int appId,
            boolean appSupportsHdr,
            String clientId,
            String hostId,
            String hostName,
            byte[] serverCertificate) {
        this.hostAddress = requireText(hostAddress, "hostAddress");
        this.hostPort = requirePort(hostPort, "hostPort");
        this.httpsPort = requireOptionalPort(httpsPort, "httpsPort");
        this.appName = appName == null ? "app" : appName;
        if (appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        this.appId = appId;
        this.appSupportsHdr = appSupportsHdr;
        this.clientId = requireText(clientId, "clientId");
        this.hostId = hostId;
        this.hostName = hostName;
        this.serverCertificate = serverCertificate == null
                ? null
                : serverCertificate.clone();
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getHostPort() {
        return hostPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public String getAppName() {
        return appName;
    }

    public int getAppId() {
        return appId;
    }

    public boolean supportsHdr() {
        return appSupportsHdr;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public byte[] getServerCertificate() {
        return serverCertificate == null
                ? null
                : serverCertificate.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StreamLaunchRequest)) {
            return false;
        }
        StreamLaunchRequest that = (StreamLaunchRequest) other;
        return hostPort == that.hostPort &&
                httpsPort == that.httpsPort &&
                appId == that.appId &&
                appSupportsHdr == that.appSupportsHdr &&
                hostAddress.equals(that.hostAddress) &&
                appName.equals(that.appName) &&
                clientId.equals(that.clientId) &&
                Objects.equals(hostId, that.hostId) &&
                Objects.equals(hostName, that.hostName) &&
                Arrays.equals(
                        serverCertificate,
                        that.serverCertificate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                hostAddress,
                hostPort,
                httpsPort,
                appName,
                appId,
                appSupportsHdr,
                clientId,
                hostId,
                hostName);
        return 31 * result + Arrays.hashCode(serverCertificate);
    }

    private static String requireText(
            String value,
            String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static int requirePort(int value, String name) {
        if (value < 1 || value > 65_535) {
            throw new IllegalArgumentException(
                    name + " is outside the valid port range");
        }
        return value;
    }

    private static int requireOptionalPort(int value, String name) {
        if (value < 0 || value > 65_535) {
            throw new IllegalArgumentException(
                    name + " is outside the valid port range");
        }
        return value;
    }
}
