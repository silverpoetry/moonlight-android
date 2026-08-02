package com.limelight.computers.wol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class WakeOnLanTargetTest {
    @Test
    public void capturesOnlyWakeOnLanDataInLegacySendOrder() {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4, "192.168.1.2");
        HostEndpoint remote = endpoint(
                HostEndpoint.Kind.REMOTE, "remote.example");
        HostEndpoint manual = endpoint(
                HostEndpoint.Kind.MANUAL, "manual.example");
        HostEndpoint ipv6 = endpoint(
                HostEndpoint.Kind.LOCAL_IPV6, "2001:db8::1");
        HostRecord record = new HostRecord(
                identity(),
                Arrays.asList(manual, ipv6, remote, local),
                "00:11:22:33:44:55");

        WakeOnLanTarget target = WakeOnLanTarget.from(record);

        assertEquals("00:11:22:33:44:55", target.getMacAddress());
        assertEquals(
                Arrays.asList(local, remote, manual, ipv6),
                target.getEndpoints());
        assertThrows(
                UnsupportedOperationException.class,
                () -> target.getEndpoints().clear());
    }

    @Test
    public void rejectsHostWithoutMacAddress() {
        HostRecord record = new HostRecord(
                identity(),
                Collections.singletonList(endpoint(
                        HostEndpoint.Kind.LOCAL_IPV4,
                        "192.168.1.2")),
                null);

        assertThrows(
                IllegalArgumentException.class,
                () -> WakeOnLanTarget.from(record));
    }

    private static HostIdentity identity() {
        return new HostIdentity(
                HostId.of("host-id"),
                "Host",
                null);
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
        return new HostEndpoint(kind, address, 47989);
    }
}
