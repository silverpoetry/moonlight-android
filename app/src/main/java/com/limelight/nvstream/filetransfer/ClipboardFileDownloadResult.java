package com.limelight.nvstream.filetransfer;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable output of one atomically committed clipboard file download. */
public final class ClipboardFileDownloadResult {
    public static final class ShareableFile {
        private final Uri uri;
        private final String displayName;
        private final String mimeType;

        public ShareableFile(
                Uri uri,
                String displayName,
                String mimeType) {
            this.uri = Objects.requireNonNull(uri, "uri");
            this.displayName = requireText(
                    displayName,
                    "displayName");
            this.mimeType = requireText(mimeType, "mimeType");
        }

        public Uri getUri() {
            return uri;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    private final int topLevelItemCount;
    private final List<ShareableFile> shareableFiles;

    public ClipboardFileDownloadResult(
            int topLevelItemCount,
            List<ShareableFile> shareableFiles) {
        if (topLevelItemCount < 0) {
            throw new IllegalArgumentException(
                    "topLevelItemCount must not be negative");
        }
        this.topLevelItemCount = topLevelItemCount;
        List<ShareableFile> copy = new ArrayList<>(
                Objects.requireNonNull(
                        shareableFiles,
                        "shareableFiles").size());
        for (ShareableFile file : shareableFiles) {
            copy.add(Objects.requireNonNull(file, "shareableFile"));
        }
        this.shareableFiles = Collections.unmodifiableList(copy);
    }

    public int getTopLevelItemCount() {
        return topLevelItemCount;
    }

    public List<ShareableFile> getShareableFiles() {
        return shareableFiles;
    }

    public boolean hasShareableFiles() {
        return !shareableFiles.isEmpty();
    }

    private static String requireText(String value, String name) {
        String required = Objects.requireNonNull(value, name).trim();
        if (required.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return required;
    }
}
