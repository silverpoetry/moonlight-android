package com.limelight.nvstream.mdns;

import org.junit.Test;

import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class JmDnsCompatibilityTest {
    @Test
    public void serviceInfoRetainsNvStreamIdentityAndAddressFamilies() {
        ServiceInfo info = ServiceInfo.create(
                "_nvstream._tcp.local.", "test-host", 47989, "");

        assertEquals("test-host", info.getName());
        assertEquals(47989, info.getPort());
        assertEquals(0, info.getInet4Addresses().length);
        assertEquals(0, info.getInet6Addresses().length);
    }

    @Test
    public void androidTopologyDelegateRemainsInstalled() throws Exception {
        // Initializing the agent installs the Android-specific topology delegate that
        // deliberately tolerates OEM interfaces which misreport multicast capability.
        Class.forName(JmDNSDiscoveryAgent.class.getName());

        assertTrue(NetworkTopologyDiscovery.Factory.getInstance()
                instanceof JmDNSDiscoveryAgent.MyNetworkTopologyDiscovery);
    }
}
