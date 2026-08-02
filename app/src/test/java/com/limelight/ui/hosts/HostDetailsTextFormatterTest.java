package com.limelight.ui.hosts;

import static org.junit.Assert.assertEquals;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;

import org.junit.Test;

import java.util.Arrays;

public final class HostDetailsTextFormatterTest {
    @Test
    public void preservesEstablishedDiagnosticTextContract() {
        HostId hostId = HostId.of("host-id");
        HostEndpoint local = new HostEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.0.2.10",
                47989);
        HostRuntimeSnapshot host = new HostRuntimeSnapshot(
                new PersistedHost(
                        new HostRecord(
                                new HostIdentity(hostId, "Host", null),
                                Arrays.asList(
                                        local,
                                        new HostEndpoint(
                                                HostEndpoint.Kind.REMOTE,
                                                "remote.example.test",
                                                48000),
                                        new HostEndpoint(
                                                HostEndpoint.Kind.LOCAL_IPV6,
                                                "2001:db8::10",
                                                47989)),
                                null),
                        null),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.ONLINE,
                        HostConnectionState.PairingStatus.PAIRED,
                        local,
                        47984,
                        7),
                null,
                false);

        assertEquals(
                "ComputerDetails{state=ONLINE, pairState=PAIRED, " +
                        "hasActiveAddress=true, hasLocalAddress=true, " +
                        "hasRemoteAddress=true, hasManualAddress=false, " +
                        "hasIpv6Address=true, " +
                        "hasPinnedCertificate=false}",
                HostDetailsTextFormatter.format(host));
    }

    @Test
    public void unknownPairingRetainsLegacyNullLabel() {
        HostId hostId = HostId.of("host-id");
        HostRuntimeSnapshot host = new HostRuntimeSnapshot(
                new PersistedHost(
                        new HostRecord(
                                new HostIdentity(hostId, "Host", null),
                                java.util.Collections.emptyList(),
                                null),
                        null),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.UNKNOWN,
                        HostConnectionState.PairingStatus.UNKNOWN,
                        null,
                        0,
                        0),
                null,
                false);

        assertEquals(
                "ComputerDetails{state=UNKNOWN, pairState=null, " +
                        "hasActiveAddress=false, hasLocalAddress=false, " +
                        "hasRemoteAddress=false, hasManualAddress=false, " +
                        "hasIpv6Address=false, " +
                        "hasPinnedCertificate=false}",
                HostDetailsTextFormatter.format(host));
    }
}
