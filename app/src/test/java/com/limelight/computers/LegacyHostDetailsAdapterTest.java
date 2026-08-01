package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.ComputerDetails;

import org.junit.Test;

import java.security.cert.X509Certificate;

public final class LegacyHostDetailsAdapterTest {
    @Test
    public void repositoryRoundTripPreservesEveryPersistentField() {
        ComputerDetails source = details();
        X509Certificate certificate =
                new TestX509Certificate("pinned");
        source.serverCert = certificate;

        PersistedHost host = LegacyHostDetailsAdapter
                .toPersistedHost(source);
        ComputerDetails restored = LegacyHostDetailsAdapter
                .toComputerDetails(host);

        assertEquals("host-id", restored.uuid);
        assertEquals("Host", restored.name);
        assertEquals(source.localAddress, restored.localAddress);
        assertEquals(source.remoteAddress, restored.remoteAddress);
        assertEquals(source.manualAddress, restored.manualAddress);
        assertEquals(source.ipv6Address, restored.ipv6Address);
        assertEquals(source.macAddress, restored.macAddress);
        assertSame(certificate, restored.serverCert);
        assertEquals(48000, restored.externalPort);
        assertEquals(ComputerDetails.State.UNKNOWN, restored.state);
        assertNull(restored.activeAddress);
        assertNull(restored.rawAppList);
    }

    @Test
    public void immutableRecordCannotBeChangedThroughSourceMutation() {
        ComputerDetails source = details();
        HostRecord record = LegacyHostDetailsAdapter.toHostRecord(
                source);

        source.name = "Changed";
        source.localAddress = tuple("192.0.2.99", 47989);

        assertEquals(
                "Host",
                record.getIdentity().getAdvertisedName());
        assertEquals(
                "192.0.2.1",
                record.getEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV4)
                        .getAddress());
    }

    @Test
    public void repositoryOwnedAliasSurvivesLegacyDtoConversion() {
        HostRecord record = LegacyHostDetailsAdapter.toHostRecord(
                details(),
                "Living room");

        assertEquals(
                "Living room",
                record.getIdentity().getUserAlias());
        assertEquals(
                "Living room",
                record.getIdentity().getDisplayName());
        assertEquals(
                "Host",
                record.getIdentity().getAdvertisedName());
    }

    private static ComputerDetails details() {
        ComputerDetails details = new ComputerDetails();
        details.uuid = "Host-ID";
        details.name = "Host";
        details.localAddress = tuple("192.0.2.1", 47989);
        details.remoteAddress = tuple("remote.test", 48000);
        details.manualAddress = tuple("manual.test", 47989);
        details.ipv6Address = tuple("2001:db8::1", 47989);
        details.macAddress = "00:11:22:33:44:55";
        details.state = ComputerDetails.State.ONLINE;
        details.activeAddress = details.localAddress;
        details.rawAppList = "<AppList/>";
        return details;
    }

    private static ComputerDetails.AddressTuple tuple(
            String address,
            int port) {
        return new ComputerDetails.AddressTuple(address, port);
    }
}
