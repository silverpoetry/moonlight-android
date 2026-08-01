package com.limelight.stream.launch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StreamLaunchRequestTest {
    @Test
    public void certificateIsDefensivelyCopied() {
        byte[] certificate = new byte[] {1, 2, 3};
        StreamLaunchRequest request = request(certificate);
        certificate[0] = 9;
        byte[] returned = request.getServerCertificate();
        returned[1] = 9;

        assertArrayEquals(
                new byte[] {1, 2, 3},
                request.getServerCertificate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHostPort() {
        new StreamLaunchRequest(
                "host",
                0,
                47984,
                "app",
                1,
                false,
                "client",
                "host-id",
                "host-name",
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidAppId() {
        new StreamLaunchRequest(
                "host",
                47989,
                47984,
                "app",
                0,
                false,
                "client",
                "host-id",
                "host-name",
                null);
    }

    @Test
    public void nullAppNameUsesProtocolFallback() {
        StreamLaunchRequest request = new StreamLaunchRequest(
                "host",
                47989,
                47984,
                null,
                1,
                false,
                "client",
                null,
                null,
                null);

        assertEquals("app", request.getAppName());
    }

    @Test
    public void unknownHttpsPortIsPreserved() {
        StreamLaunchRequest request = new StreamLaunchRequest(
                "host",
                47989,
                0,
                "app",
                1,
                false,
                "client",
                null,
                null,
                null);

        assertEquals(0, request.getHttpsPort());
    }

    private static StreamLaunchRequest request(byte[] certificate) {
        return new StreamLaunchRequest(
                "host",
                47989,
                47984,
                "Desktop",
                1,
                true,
                "client",
                "host-id",
                "host-name",
                certificate);
    }
}
