package com.limelight.computers.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class ManualHostEndpointParserTest {
    private static final int DEFAULT_PORT = 47989;

    @Test
    public void parsesHostWithDefaultAndExplicitPort() {
        HostEndpoint defaultEndpoint =
                ManualHostEndpointParser.parse(
                        " Example.COM ",
                        DEFAULT_PORT);
        HostEndpoint explicitEndpoint =
                ManualHostEndpointParser.parse(
                        "example.com:51337",
                        DEFAULT_PORT);

        assertEquals("example.com", defaultEndpoint.getAddress());
        assertEquals(DEFAULT_PORT, defaultEndpoint.getPort());
        assertEquals(51337, explicitEndpoint.getPort());
    }

    @Test
    public void parsesBracketedAndUnbracketedIpv6() {
        HostEndpoint bracketed = ManualHostEndpointParser.parse(
                "[2001:db8::1]:48000",
                DEFAULT_PORT);
        HostEndpoint unbracketed = ManualHostEndpointParser.parse(
                "2001:db8::1",
                DEFAULT_PORT);

        assertEquals("2001:db8::1", bracketed.getAddress());
        assertEquals(48000, bracketed.getPort());
        assertEquals("2001:db8::1", unbracketed.getAddress());
        assertEquals(DEFAULT_PORT, unbracketed.getPort());
    }

    @Test
    public void rejectsEmptyMalformedAndInvalidPort() {
        assertNull(ManualHostEndpointParser.parse(
                "  ",
                DEFAULT_PORT));
        assertNull(ManualHostEndpointParser.parse(
                "host name",
                DEFAULT_PORT));
        assertNull(ManualHostEndpointParser.parse(
                "example.com:70000",
                DEFAULT_PORT));
        assertNull(ManualHostEndpointParser.parse(
                "user@example.com",
                DEFAULT_PORT));
        assertNull(ManualHostEndpointParser.parse(
                "example.com/path",
                DEFAULT_PORT));
    }
}
