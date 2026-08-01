package com.limelight.stream.launch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RecentStreamSessionTest {
    @Test
    public void nullNameUsesProtocolFallback() {
        RecentStreamSession session =
                new RecentStreamSession(null, 7, true);

        assertEquals("app", session.getAppName());
        assertEquals(7, session.getAppId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAppIdIsRejected() {
        new RecentStreamSession("Desktop", 0, false);
    }
}
