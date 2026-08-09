package com.limelight.stream.launch;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class PendingClipboardFilePullStoreTest {
    @Test
    public void handoffIsScopedToHostAndApp() {
        PendingClipboardFilePullStore store =
                new PendingClipboardFilePullStore();
        PendingClipboardFilePull pending =
                new PendingClipboardFilePull(
                        "host-a",
                        17,
                        "content://downloads/tree/primary%3AMoonlight");

        store.save(pending);

        assertSame(pending, store.getFor("host-a", 17));
        assertTrue(store.hasFor("host-a", 17));
        assertNull(store.getFor("host-b", 17));
        assertNull(store.getFor("host-a", 18));
        assertFalse(store.hasFor("host-b", 17));
    }

    @Test
    public void onlyCurrentHandoffCanBeConsumed() {
        PendingClipboardFilePullStore store =
                new PendingClipboardFilePullStore();
        PendingClipboardFilePull replaced =
                new PendingClipboardFilePull(
                        "host-a", 17, "content://old");
        PendingClipboardFilePull current =
                new PendingClipboardFilePull(
                        "host-a", 17, "content://current");

        store.save(replaced);
        store.save(current);

        assertFalse(store.clearIfCurrent(replaced));
        assertSame(current, store.getFor("host-a", 17));
        assertTrue(store.clearIfCurrent(current));
        assertNull(store.getFor("host-a", 17));
    }
}
