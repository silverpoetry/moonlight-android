package com.limelight.stream.launch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PendingStreamReconnectStoreTest {
    @Test
    public void compareAndClearCannotEraseReplacement() {
        PendingStreamReconnectStore store =
                new PendingStreamReconnectStore();
        PendingStreamReconnect first = reconnect("first");
        PendingStreamReconnect replacement = reconnect("replacement");
        store.save(first);
        assertSame(first, store.get());

        store.save(replacement);

        assertFalse(store.clearIfCurrent(first));
        assertSame(replacement, store.get());
        assertTrue(store.clearIfCurrent(replacement));
        assertNull(store.get());
    }

    @Test
    public void explicitClearRemovesPendingHandoff() {
        PendingStreamReconnectStore store =
                new PendingStreamReconnectStore();
        store.save(reconnect("host"));

        store.clear();

        assertNull(store.get());
    }

    private static PendingStreamReconnect reconnect(String hostId) {
        return new PendingStreamReconnect(
                hostId,
                "Desktop",
                1,
                false);
    }
}
