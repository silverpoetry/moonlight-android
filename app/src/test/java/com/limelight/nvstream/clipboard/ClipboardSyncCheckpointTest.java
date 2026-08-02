package com.limelight.nvstream.clipboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ClipboardSyncCheckpointTest {
    private static final String FINGERPRINT =
            "0123456789abcdef0123456789abcdef" +
                    "0123456789abcdef0123456789abcdef";

    @Test
    public void representsUninitializedAndInitializedState() {
        ClipboardSyncCheckpoint empty =
                ClipboardSyncCheckpoint.uninitialized();
        ClipboardSyncCheckpoint initialized =
                ClipboardSyncCheckpoint.initialized(FINGERPRINT);

        assertFalse(empty.isInitialized());
        assertNull(empty.getLastHandledFingerprint());
        assertTrue(initialized.isInitialized());
        assertEquals(
                FINGERPRINT,
                initialized.getLastHandledFingerprint());
    }

    @Test
    public void validatesCanonicalSha256Hex() {
        assertTrue(ClipboardSyncCheckpoint.isValidFingerprint(FINGERPRINT));
        assertFalse(ClipboardSyncCheckpoint.isValidFingerprint(null));
        assertFalse(ClipboardSyncCheckpoint.isValidFingerprint("abc"));
        assertFalse(ClipboardSyncCheckpoint.isValidFingerprint(
                FINGERPRINT.toUpperCase()));
        assertFalse(ClipboardSyncCheckpoint.isValidFingerprint(
                FINGERPRINT.substring(0, 63) + "g"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidInitializedState() {
        ClipboardSyncCheckpoint.initialized("invalid");
    }
}
