package com.limelight.nvstream.clipboard;

/** Immutable loop-suppression checkpoint for clipboard synchronization. */
public final class ClipboardSyncCheckpoint {
    private static final int SHA256_HEX_LENGTH = 64;
    private static final ClipboardSyncCheckpoint UNINITIALIZED =
            new ClipboardSyncCheckpoint(false, null);

    private final boolean initialized;
    private final String lastHandledFingerprint;

    private ClipboardSyncCheckpoint(
            boolean initialized,
            String lastHandledFingerprint) {
        this.initialized = initialized;
        this.lastHandledFingerprint = lastHandledFingerprint;
    }

    public static ClipboardSyncCheckpoint uninitialized() {
        return UNINITIALIZED;
    }

    public static ClipboardSyncCheckpoint initialized(
            String lastHandledFingerprint) {
        if (!isValidFingerprint(lastHandledFingerprint)) {
            throw new IllegalArgumentException(
                    "lastHandledFingerprint must be lowercase SHA-256 hex");
        }
        return new ClipboardSyncCheckpoint(
                true, lastHandledFingerprint);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getLastHandledFingerprint() {
        return lastHandledFingerprint;
    }

    public static boolean isValidFingerprint(String value) {
        if (value == null || value.length() != SHA256_HEX_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!((character >= '0' && character <= '9') ||
                    (character >= 'a' && character <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
