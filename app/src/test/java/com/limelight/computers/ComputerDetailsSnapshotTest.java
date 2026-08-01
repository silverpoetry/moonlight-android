package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;

import org.junit.Test;

public final class ComputerDetailsSnapshotTest {
    @Test
    public void copiesEveryPersistentAndRuntimeField() {
        ComputerDetails source = new ComputerDetails();
        source.uuid = "host-id";
        source.name = "Host";
        source.localAddress = tuple("192.168.1.2", 47989);
        source.remoteAddress = tuple("remote.test", 48000);
        source.manualAddress = tuple("manual.test", 47989);
        source.ipv6Address = tuple("2001:db8::1", 47989);
        source.macAddress = "00:11:22:33:44:55";
        source.serverCert = new TestX509Certificate("pinned");
        source.state = ComputerDetails.State.ONLINE;
        source.activeAddress = source.localAddress;
        source.httpsPort = 47984;
        source.externalPort = 48000;
        source.pairState = PairingManager.PairState.PAIRED;
        source.runningGameId = 42;
        source.rawAppList = "<AppList/>";
        source.nvidiaServer = true;

        ComputerDetails snapshot = ComputerDetailsSnapshot.copyOf(source);

        assertNotSame(source, snapshot);
        assertEquals(source.uuid, snapshot.uuid);
        assertEquals(source.name, snapshot.name);
        assertSame(source.localAddress, snapshot.localAddress);
        assertSame(source.remoteAddress, snapshot.remoteAddress);
        assertSame(source.manualAddress, snapshot.manualAddress);
        assertSame(source.ipv6Address, snapshot.ipv6Address);
        assertEquals(source.macAddress, snapshot.macAddress);
        assertSame(source.serverCert, snapshot.serverCert);
        assertEquals(source.state, snapshot.state);
        assertSame(source.activeAddress, snapshot.activeAddress);
        assertEquals(source.httpsPort, snapshot.httpsPort);
        assertEquals(source.externalPort, snapshot.externalPort);
        assertEquals(source.pairState, snapshot.pairState);
        assertEquals(source.runningGameId, snapshot.runningGameId);
        assertEquals(source.rawAppList, snapshot.rawAppList);
        assertEquals(source.nvidiaServer, snapshot.nvidiaServer);
    }

    @Test
    public void laterSourceMutationCannotReplaceSnapshotScalars() {
        ComputerDetails source = new ComputerDetails();
        source.uuid = "host-id";
        source.name = "Before";
        source.state = ComputerDetails.State.UNKNOWN;
        ComputerDetails snapshot = ComputerDetailsSnapshot.copyOf(source);

        source.name = "After";
        source.state = ComputerDetails.State.OFFLINE;

        assertEquals("Before", snapshot.name);
        assertEquals(ComputerDetails.State.UNKNOWN, snapshot.state);
    }

    private static ComputerDetails.AddressTuple tuple(
            String address,
            int port) {
        return new ComputerDetails.AddressTuple(address, port);
    }
}
