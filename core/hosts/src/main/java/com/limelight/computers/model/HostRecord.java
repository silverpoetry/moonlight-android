package com.limelight.computers.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable persistent host metadata. Credentials are intentionally absent. */
public final class HostRecord {
    private static final String EMPTY_MAC = "00:00:00:00:00:00";

    private final HostIdentity identity;
    private final Map<HostEndpoint.Kind, HostEndpoint> endpoints;
    private final String macAddress;

    public HostRecord(
            HostIdentity identity,
            List<HostEndpoint> endpoints,
            String macAddress) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.endpoints = indexEndpoints(endpoints);
        this.macAddress = normalizeMacAddress(macAddress);
    }

    private static Map<HostEndpoint.Kind, HostEndpoint> indexEndpoints(
            List<HostEndpoint> endpoints) {
        EnumMap<HostEndpoint.Kind, HostEndpoint> indexed =
                new EnumMap<>(HostEndpoint.Kind.class);
        for (HostEndpoint endpoint : Objects.requireNonNull(
                endpoints, "endpoints")) {
            HostEndpoint checked = Objects.requireNonNull(
                    endpoint, "endpoint");
            if (indexed.put(checked.getKind(), checked) != null) {
                throw new IllegalArgumentException(
                        "Duplicate endpoint kind: " + checked.getKind());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static String normalizeMacAddress(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (EMPTY_MAC.equals(normalized)) {
            return null;
        }
        if (!normalized.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}")) {
            throw new IllegalArgumentException("Invalid MAC address");
        }
        return normalized;
    }

    public HostIdentity getIdentity() {
        return identity;
    }

    public List<HostEndpoint> getEndpoints() {
        return Collections.unmodifiableList(
                new ArrayList<>(endpoints.values()));
    }

    public HostEndpoint getEndpoint(HostEndpoint.Kind kind) {
        return endpoints.get(Objects.requireNonNull(kind, "kind"));
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HostRecord)) {
            return false;
        }
        HostRecord record = (HostRecord) other;
        return identity.equals(record.identity) &&
                endpoints.equals(record.endpoints) &&
                Objects.equals(macAddress, record.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, endpoints, macAddress);
    }
}
