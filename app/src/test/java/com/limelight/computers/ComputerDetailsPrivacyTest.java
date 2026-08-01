package com.limelight.computers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.nvstream.http.ComputerDetails;

import org.junit.Test;

public final class ComputerDetailsPrivacyTest {
    @Test
    public void diagnosticStringNeverContainsHostIdentityOrEndpoints() {
        ComputerDetails details = new ComputerDetails();
        details.uuid = "sensitive-host-id";
        details.name = "Sensitive host name";
        details.macAddress = "AA:BB:CC:DD:EE:FF";
        details.localAddress = new ComputerDetails.AddressTuple(
                "192.0.2.123",
                47989);

        String diagnostic = details.toString();

        assertFalse(diagnostic.contains(details.uuid));
        assertFalse(diagnostic.contains(details.name));
        assertFalse(diagnostic.contains(details.macAddress));
        assertFalse(diagnostic.contains(details.localAddress.address));
        assertTrue(diagnostic.contains("hasLocalAddress=true"));
    }
}
