package com.limelight.computers;

import com.limelight.nvstream.http.ComputerDetails;

import java.util.Objects;

/** Creates detached snapshots at the legacy mutable DTO boundary. */
public final class ComputerDetailsSnapshot {
    private ComputerDetailsSnapshot() {
    }

    public static ComputerDetails copyOf(ComputerDetails source) {
        ComputerDetails checked = Objects.requireNonNull(source, "source");
        ComputerDetails snapshot = new ComputerDetails();
        snapshot.uuid = checked.uuid;
        snapshot.name = checked.name;
        snapshot.localAddress = checked.localAddress;
        snapshot.remoteAddress = checked.remoteAddress;
        snapshot.manualAddress = checked.manualAddress;
        snapshot.ipv6Address = checked.ipv6Address;
        snapshot.macAddress = checked.macAddress;
        snapshot.serverCert = checked.serverCert;
        snapshot.state = checked.state;
        snapshot.activeAddress = checked.activeAddress;
        snapshot.httpsPort = checked.httpsPort;
        snapshot.externalPort = checked.externalPort;
        snapshot.pairState = checked.pairState;
        snapshot.runningGameId = checked.runningGameId;
        snapshot.rawAppList = checked.rawAppList;
        snapshot.nvidiaServer = checked.nvidiaServer;
        return snapshot;
    }
}
