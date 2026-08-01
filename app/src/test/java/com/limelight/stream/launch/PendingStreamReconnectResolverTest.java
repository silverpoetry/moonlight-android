package com.limelight.stream.launch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PendingStreamReconnectResolverTest {
    private final PendingStreamReconnectResolver resolver =
            new PendingStreamReconnectResolver();

    @Test
    public void hostFilterIsCaseInsensitive() {
        assertEquals(
                PendingStreamReconnectResolver.Outcome.READY,
                resolver.resolve(
                        reconnect("Host", 4),
                        "host",
                        true,
                        0).getOutcome());
    }

    @Test
    public void mismatchedHostDoesNotConsumePendingStream() {
        assertEquals(
                PendingStreamReconnectResolver.Outcome.HOST_MISMATCH,
                resolver.resolve(
                        reconnect("first", 4),
                        "second",
                        true,
                        0).getOutcome());
    }

    @Test
    public void missingAppUsesCurrentlyRunningApp() {
        PendingStreamReconnectResolver.Resolution resolution =
                resolver.resolve(
                        reconnect("host", 0),
                        null,
                        true,
                        19);

        assertEquals(
                PendingStreamReconnectResolver.Outcome.READY,
                resolution.getOutcome());
        assertEquals(19, resolution.getAppId());
    }

    @Test
    public void missingPendingAndUnavailableHostAreDistinct() {
        assertEquals(
                PendingStreamReconnectResolver.Outcome.NO_PENDING,
                resolver.resolve(null, null, false, 0).getOutcome());
        assertEquals(
                PendingStreamReconnectResolver.Outcome.HOST_UNAVAILABLE,
                resolver.resolve(
                        reconnect("host", 4),
                        null,
                        false,
                        0).getOutcome());
    }

    private static PendingStreamReconnect reconnect(
            String hostId,
            int appId) {
        return new PendingStreamReconnect(
                hostId,
                "Desktop",
                appId,
                false);
    }
}
