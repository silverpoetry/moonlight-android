package com.limelight.computers.reachability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.limelight.computers.model.HostEndpoint;

import org.junit.Test;

import java.util.Arrays;

public final class HostReachabilityPlanTest {
    private final HostEndpoint local = endpoint(
            HostEndpoint.Kind.LOCAL_IPV4,
            "192.168.1.2",
            47989);
    private final HostEndpoint manual = endpoint(
            HostEndpoint.Kind.MANUAL,
            "manual.example",
            47989);
    private final HostEndpoint remote = endpoint(
            HostEndpoint.Kind.REMOTE,
            "remote.example",
            47989);
    private final HostEndpoint ipv6 = endpoint(
            HostEndpoint.Kind.LOCAL_IPV6,
            "fd00::2",
            47989);

    @Test
    public void localNetworkOrderPreservesLegacyPriority() {
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                manual,
                remote,
                ipv6,
                false);

        assertEquals(
                Arrays.asList(local, manual, remote, ipv6),
                plan.getEndpoints());
    }

    @Test
    public void wrongSubnetOrderPrefersExternalCandidates() {
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                manual,
                remote,
                ipv6,
                true);

        assertEquals(
                Arrays.asList(manual, remote, ipv6, local),
                plan.getEndpoints());
    }

    @Test
    public void duplicatePhysicalTargetKeepsFirstProvenance() {
        HostEndpoint duplicateManual = endpoint(
                HostEndpoint.Kind.MANUAL,
                "192.168.1.2",
                47989);

        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                duplicateManual,
                null,
                null,
                false);

        assertEquals(Arrays.asList(local), plan.getEndpoints());
    }

    @Test
    public void endpointListIsImmutable() {
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                null,
                null,
                null,
                false);

        assertThrows(
                UnsupportedOperationException.class,
                () -> plan.getEndpoints().clear());
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address,
            int port) {
        return new HostEndpoint(kind, address, port);
    }
}
