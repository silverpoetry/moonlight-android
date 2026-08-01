package com.limelight.computers.discovery;

import com.limelight.computers.model.HostEndpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Endpoint-only discovery result. Stable identity is resolved by probing. */
public final class HostDiscoveryCandidate {
    private final List<HostEndpoint> endpoints;

    public HostDiscoveryCandidate(List<HostEndpoint> endpoints) {
        Objects.requireNonNull(endpoints, "endpoints");
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "A discovery candidate requires an endpoint");
        }
        EnumSet<HostEndpoint.Kind> kinds = EnumSet.noneOf(
                HostEndpoint.Kind.class);
        List<HostEndpoint> copy = new ArrayList<>(endpoints.size());
        for (HostEndpoint endpoint : endpoints) {
            HostEndpoint checked = Objects.requireNonNull(
                    endpoint,
                    "endpoint");
            if (checked.getKind() != HostEndpoint.Kind.LOCAL_IPV4 &&
                    checked.getKind() != HostEndpoint.Kind.LOCAL_IPV6) {
                throw new IllegalArgumentException(
                        "Discovery candidates may contain only local endpoints");
            }
            if (!kinds.add(checked.getKind())) {
                throw new IllegalArgumentException(
                        "Duplicate discovery endpoint kind");
            }
            copy.add(checked);
        }
        this.endpoints = Collections.unmodifiableList(copy);
    }

    public List<HostEndpoint> getEndpoints() {
        return endpoints;
    }

    public HostEndpoint getEndpoint(HostEndpoint.Kind kind) {
        for (HostEndpoint endpoint : endpoints) {
            if (endpoint.getKind() == kind) {
                return endpoint;
            }
        }
        return null;
    }
}
