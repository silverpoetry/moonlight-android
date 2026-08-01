package com.limelight.computers.reachability;

import com.limelight.computers.model.HostEndpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, physically deduplicated endpoint probe order. */
public final class HostReachabilityPlan {
    private final List<HostEndpoint> endpoints;

    private HostReachabilityPlan(List<HostEndpoint> endpoints) {
        this.endpoints = Collections.unmodifiableList(endpoints);
    }

    public static HostReachabilityPlan create(
            HostEndpoint local,
            HostEndpoint manual,
            HostEndpoint remote,
            HostEndpoint ipv6,
            boolean preferExternalEndpoints) {
        ArrayList<HostEndpoint> ordered = new ArrayList<>(4);
        if (preferExternalEndpoints) {
            ordered.add(manual);
            ordered.add(remote);
            ordered.add(ipv6);
            ordered.add(local);
        }
        else {
            ordered.add(local);
            ordered.add(manual);
            ordered.add(remote);
            ordered.add(ipv6);
        }

        ArrayList<HostEndpoint> unique = new ArrayList<>(4);
        Set<PhysicalEndpoint> seen = new HashSet<>();
        for (HostEndpoint endpoint : ordered) {
            if (endpoint == null) {
                continue;
            }
            if (seen.add(new PhysicalEndpoint(endpoint))) {
                unique.add(endpoint);
            }
        }
        return new HostReachabilityPlan(unique);
    }

    public List<HostEndpoint> getEndpoints() {
        return endpoints;
    }

    private static final class PhysicalEndpoint {
        private final String address;
        private final int port;

        PhysicalEndpoint(HostEndpoint endpoint) {
            address = endpoint.getAddress();
            port = endpoint.getPort();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PhysicalEndpoint)) {
                return false;
            }
            PhysicalEndpoint endpoint = (PhysicalEndpoint) other;
            return port == endpoint.port &&
                    address.equals(endpoint.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, port);
        }
    }
}
