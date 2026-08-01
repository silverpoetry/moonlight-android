package com.limelight.nvstream.filetransfer;

import android.net.Uri;

import com.limelight.transfer.FileManifest;

import java.util.Objects;

/** Binds one Android source handle to its platform-neutral wire entry. */
final class DesktopFileUploadSource {
    private final FileManifest.Entry manifestEntry;
    private final Uri sourceUri;

    DesktopFileUploadSource(
            FileManifest.Entry manifestEntry,
            Uri sourceUri) {
        this.manifestEntry = Objects.requireNonNull(
                manifestEntry,
                "manifestEntry");
        this.sourceUri = sourceUri;
    }

    FileManifest.Entry getManifestEntry() {
        return manifestEntry;
    }

    Uri getSourceUri() {
        return sourceUri;
    }
}
