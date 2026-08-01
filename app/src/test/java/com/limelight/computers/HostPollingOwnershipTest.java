package com.limelight.computers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class HostPollingOwnershipTest {
    @Test
    public void staleOwnerCannotReleaseReplacement() {
        HostPollingOwnership<String> ownership =
                new HostPollingOwnership<>();
        HostPollingOwnership.Token oldToken = ownership.replace("old");
        HostPollingOwnership.Token currentToken =
                ownership.replace("current");

        assertFalse(ownership.owns(oldToken));
        assertFalse(ownership.release(oldToken));
        assertNull(ownership.getListener(oldToken));
        assertEquals("current", ownership.getListener());
        assertEquals("current", ownership.getListener(currentToken));
        assertTrue(ownership.owns(currentToken));
    }

    @Test
    public void currentOwnerReleaseIsIdempotent() {
        HostPollingOwnership<String> ownership =
                new HostPollingOwnership<>();
        HostPollingOwnership.Token token = ownership.replace("listener");

        assertTrue(ownership.release(token));
        assertFalse(ownership.release(token));
        assertNull(ownership.getListener());
    }

    @Test
    public void clearInvalidatesAnyActiveOwner() {
        HostPollingOwnership<String> ownership =
                new HostPollingOwnership<>();
        HostPollingOwnership.Token token = ownership.replace("listener");

        assertTrue(ownership.clear());
        assertFalse(ownership.owns(token));
        assertFalse(ownership.clear());
        assertNull(ownership.getListener());
    }
}
