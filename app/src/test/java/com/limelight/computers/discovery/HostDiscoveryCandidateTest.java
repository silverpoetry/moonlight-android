package com.limelight.computers.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.limelight.computers.model.HostEndpoint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HostDiscoveryCandidateTest {
    @Test
    public void acceptsAndDefensivelyCopiesLocalEndpoints() {
        List<HostEndpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.0.2.1"));
        HostDiscoveryCandidate candidate =
                new HostDiscoveryCandidate(endpoints);
        endpoints.clear();

        assertEquals(1, candidate.getEndpoints().size());
        assertEquals(
                "192.0.2.1",
                candidate.getEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV4).getAddress());
        assertThrows(
                UnsupportedOperationException.class,
                () -> candidate.getEndpoints().clear());
    }

    @Test
    public void rejectsEmptyRemoteAndDuplicateCandidates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostDiscoveryCandidate(
                        Collections.emptyList()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostDiscoveryCandidate(
                        Collections.singletonList(endpoint(
                                HostEndpoint.Kind.REMOTE,
                                "remote.test"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HostDiscoveryCandidate(Arrays.asList(
                        endpoint(
                                HostEndpoint.Kind.LOCAL_IPV4,
                                "192.0.2.1"),
                        endpoint(
                                HostEndpoint.Kind.LOCAL_IPV4,
                                "192.0.2.2"))));
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
        return new HostEndpoint(kind, address, 47989);
    }
}
