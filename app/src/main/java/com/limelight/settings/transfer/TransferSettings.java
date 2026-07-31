package com.limelight.settings.transfer;

import java.util.Objects;

/**
 * Immutable clipboard and user-initiated transfer policy.
 */
public final class TransferSettings {
    private final boolean clipboardSyncEnabled;
    private final String clipboardFileDirectoryUri;

    public TransferSettings(
            boolean clipboardSyncEnabled,
            String clipboardFileDirectoryUri) {
        this.clipboardSyncEnabled = clipboardSyncEnabled;
        this.clipboardFileDirectoryUri =
                Objects.requireNonNull(
                        clipboardFileDirectoryUri,
                        "clipboardFileDirectoryUri");
    }

    public boolean isClipboardSyncEnabled() {
        return clipboardSyncEnabled;
    }

    public String getClipboardFileDirectoryUri() {
        return clipboardFileDirectoryUri;
    }

    public boolean hasClipboardFileDirectory() {
        return !clipboardFileDirectoryUri.isEmpty();
    }
}
