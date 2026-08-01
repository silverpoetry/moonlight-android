package com.limelight.computers.model;

import java.util.Locale;
import java.util.Objects;

/** One immutable connection endpoint with explicit provenance. */
public final class HostEndpoint {
    public enum Kind {
        LOCAL_IPV4,
        LOCAL_IPV6,
        REMOTE,
        MANUAL
    }

    public static final int MAXIMUM_ADDRESS_LENGTH = 1024;

    private final Kind kind;
    private final String address;
    private final int port;

    public HostEndpoint(Kind kind, String address, int port) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.address = normalizeAddress(address);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid endpoint port");
        }
        this.port = port;
    }

    private static String normalizeAddress(String value) {
        String normalized = Objects.requireNonNull(value, "address").trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() ||
                normalized.length() > MAXIMUM_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Invalid endpoint address");
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (Character.isWhitespace(normalized.charAt(index))) {
                throw new IllegalArgumentException(
                        "Endpoint address cannot contain whitespace");
            }
        }
        return normalized;
    }

    public Kind getKind() {
        return kind;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostEndpoint)) {
            return false;
        }
        HostEndpoint endpoint = (HostEndpoint) other;
        return kind == endpoint.kind &&
                port == endpoint.port &&
                address.equals(endpoint.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, address, port);
    }

    @Override
    public String toString() {
        String formattedAddress = address.contains(":") ?
                "[" + address + "]" : address;
        return kind + ":" + formattedAddress + ":" + port;
    }
}
