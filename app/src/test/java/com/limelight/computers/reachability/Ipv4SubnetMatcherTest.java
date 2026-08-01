package com.limelight.computers.reachability;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class Ipv4SubnetMatcherTest {
    @Test
    public void matchesByteAlignedPrefix() {
        assertTrue(Ipv4SubnetMatcher.isSameSubnet(
                address(192, 168, 3, 10),
                address(192, 168, 3, 200),
                24));
        assertFalse(Ipv4SubnetMatcher.isSameSubnet(
                address(192, 168, 3, 10),
                address(192, 168, 4, 10),
                24));
    }

    @Test
    public void matchesMostSignificantBitsForPartialPrefix() {
        assertTrue(Ipv4SubnetMatcher.isSameSubnet(
                address(172, 16, 15, 1),
                address(172, 16, 0, 1),
                20));
        assertFalse(Ipv4SubnetMatcher.isSameSubnet(
                address(172, 16, 16, 1),
                address(172, 16, 0, 1),
                20));
    }

    @Test
    public void zeroPrefixMatchesAnyIpv4Address() {
        assertTrue(Ipv4SubnetMatcher.isSameSubnet(
                address(10, 0, 0, 1),
                address(192, 168, 1, 1),
                0));
    }

    @Test
    public void rejectsInvalidInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4SubnetMatcher.isSameSubnet(
                        new byte[16],
                        new byte[4],
                        24));
        assertThrows(
                IllegalArgumentException.class,
                () -> Ipv4SubnetMatcher.isSameSubnet(
                        new byte[4],
                        new byte[4],
                        33));
    }

    private static byte[] address(int... octets) {
        byte[] address = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            address[index] = (byte) octets[index];
        }
        return address;
    }
}
