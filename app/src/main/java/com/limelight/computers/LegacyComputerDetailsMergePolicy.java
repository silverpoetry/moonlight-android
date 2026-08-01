package com.limelight.computers;

import com.limelight.nvstream.http.ComputerDetails;

import java.util.Objects;

/**
 * Transitional boundary for merging a completed server probe into the legacy
 * mutable DTO. Credentials are deliberately excluded and have a separate
 * owner; a network observation must never replace a pinned certificate.
 */
public final class LegacyComputerDetailsMergePolicy {
    private static final String EMPTY_MAC_ADDRESS = "00:00:00:00:00:00";

    private LegacyComputerDetailsMergePolicy() {
    }

    public static void mergeObservation(
            ComputerDetails destination,
            ComputerDetails observation) {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(observation, "observation");

        destination.state = observation.state;
        destination.name = observation.name;
        destination.uuid = observation.uuid;

        if (observation.activeAddress != null) {
            destination.activeAddress = observation.activeAddress;
        }
        if (isUsableLocalAddress(observation.localAddress)) {
            destination.localAddress = observation.localAddress;
        }
        mergeRemoteEndpoint(destination, observation);
        if (observation.manualAddress != null) {
            destination.manualAddress = observation.manualAddress;
        }
        if (observation.ipv6Address != null) {
            destination.ipv6Address = observation.ipv6Address;
        }
        if (isUsableMacAddress(observation.macAddress)) {
            destination.macAddress = observation.macAddress;
        }

        destination.externalPort = observation.externalPort;
        destination.httpsPort = observation.httpsPort;
        destination.pairState = observation.pairState;
        destination.runningGameId = observation.runningGameId;
        destination.nvidiaServer = observation.nvidiaServer;
        // Server-info probes do not carry an app list. Preserve the latest
        // app-list worker result unless this observation explicitly has one.
        if (observation.rawAppList != null) {
            destination.rawAppList = observation.rawAppList;
        }
    }

    private static boolean isUsableLocalAddress(
            ComputerDetails.AddressTuple address) {
        return address != null && !address.address.startsWith("127.");
    }

    private static boolean isUsableMacAddress(String macAddress) {
        return macAddress != null &&
                !EMPTY_MAC_ADDRESS.equalsIgnoreCase(macAddress);
    }

    private static void mergeRemoteEndpoint(
            ComputerDetails destination,
            ComputerDetails observation) {
        if (observation.remoteAddress != null) {
            destination.remoteAddress = observation.remoteAddress;
            return;
        }
        if (destination.remoteAddress != null &&
                observation.externalPort > 0) {
            destination.remoteAddress = new ComputerDetails.AddressTuple(
                    destination.remoteAddress.address,
                    observation.externalPort);
        }
    }
}
