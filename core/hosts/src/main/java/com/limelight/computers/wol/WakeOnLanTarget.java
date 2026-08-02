package com.limelight.computers.wol;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable network target for one Wake-on-LAN operation. */
public final class WakeOnLanTarget {
    private final String macAddress;
    private final List<HostEndpoint> endpoints;

    private WakeOnLanTarget(
            String macAddress,
            List<HostEndpoint> endpoints) {
        this.macAddress = macAddress;
        this.endpoints = Collections.unmodifiableList(
                new ArrayList<>(endpoints));
    }

    public static WakeOnLanTarget from(HostRecord record) {
        HostRecord source = Objects.requireNonNull(record, "record");
        if (source.getMacAddress() == null) {
            throw new IllegalArgumentException(
                    "Host has no Wake-on-LAN MAC address");
        }

        List<HostEndpoint> endpoints = new ArrayList<>(4);
        addEndpoint(endpoints, source, HostEndpoint.Kind.LOCAL_IPV4);
        addEndpoint(endpoints, source, HostEndpoint.Kind.REMOTE);
        addEndpoint(endpoints, source, HostEndpoint.Kind.MANUAL);
        addEndpoint(endpoints, source, HostEndpoint.Kind.LOCAL_IPV6);
        return new WakeOnLanTarget(source.getMacAddress(), endpoints);
    }

    private static void addEndpoint(
            List<HostEndpoint> endpoints,
            HostRecord record,
            HostEndpoint.Kind kind) {
        HostEndpoint endpoint = record.getEndpoint(kind);
        if (endpoint != null) {
            endpoints.add(endpoint);
        }
    }

    public String getMacAddress() {
        return macAddress;
    }

    public List<HostEndpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WakeOnLanTarget)) {
            return false;
        }
        WakeOnLanTarget target = (WakeOnLanTarget) other;
        return macAddress.equals(target.macAddress) &&
                endpoints.equals(target.endpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress, endpoints);
    }
}
