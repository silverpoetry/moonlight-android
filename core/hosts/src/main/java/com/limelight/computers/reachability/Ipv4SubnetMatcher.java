package com.limelight.computers.reachability;

import java.util.Objects;

/** Pure network-prefix comparison for two IPv4 addresses. */
public final class Ipv4SubnetMatcher {
    private static final int IPV4_BYTE_COUNT = 4;

    private Ipv4SubnetMatcher() {
    }

    public static boolean isSameSubnet(
            byte[] firstAddress,
            byte[] secondAddress,
            int prefixLength) {
        Objects.requireNonNull(firstAddress, "firstAddress");
        Objects.requireNonNull(secondAddress, "secondAddress");
        if (firstAddress.length != IPV4_BYTE_COUNT ||
                secondAddress.length != IPV4_BYTE_COUNT) {
            throw new IllegalArgumentException(
                    "Subnet matching requires IPv4 addresses");
        }
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 prefix length");
        }

        int completeBytes = prefixLength / 8;
        for (int index = 0; index < completeBytes; index++) {
            if (firstAddress[index] != secondAddress[index]) {
                return false;
            }
        }

        int remainingBits = prefixLength % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (firstAddress[completeBytes] & mask) ==
                (secondAddress[completeBytes] & mask);
    }
}
