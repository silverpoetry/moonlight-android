package com.limelight.ui.clipboard;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;

import com.limelight.nvstream.filetransfer.ClipboardFileDownloadResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Creates a read-only Android share document from one download result. */
final class ClipboardFileShareIntentFactory {
    private ClipboardFileShareIntentFactory() {
    }

    static Intent create(ClipboardFileDownloadResult result) {
        Objects.requireNonNull(result, "result");
        List<ClipboardFileDownloadResult.ShareableFile> files =
                result.getShareableFiles();
        if (files.isEmpty()) {
            throw new IllegalArgumentException(
                    "The download contains no shareable files");
        }

        String mimeType = commonMimeType(files);
        ArrayList<Uri> uris = new ArrayList<>(files.size());
        for (ClipboardFileDownloadResult.ShareableFile file : files) {
            uris.add(file.getUri());
        }

        Intent intent = new Intent(
                uris.size() == 1
                        ? Intent.ACTION_SEND
                        : Intent.ACTION_SEND_MULTIPLE);
        intent.setType(mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uris.size() == 1) {
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        else {
            intent.putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    uris);
        }
        intent.setClipData(createClipData(files, mimeType));
        return intent;
    }

    private static ClipData createClipData(
            List<ClipboardFileDownloadResult.ShareableFile> files,
            String mimeType) {
        ClipboardFileDownloadResult.ShareableFile first = files.get(0);
        ClipData clipData = new ClipData(
                new ClipDescription(
                        first.getDisplayName(),
                        new String[] {mimeType}),
                new ClipData.Item(first.getUri()));
        for (int index = 1; index < files.size(); index++) {
            clipData.addItem(new ClipData.Item(
                    files.get(index).getUri()));
        }
        return clipData;
    }

    private static String commonMimeType(
            List<ClipboardFileDownloadResult.ShareableFile> files) {
        String first = files.get(0).getMimeType().toLowerCase(Locale.ROOT);
        boolean exactMatch = true;
        String commonCategory = mimeCategory(first);
        for (int index = 1; index < files.size(); index++) {
            String current = files.get(index)
                    .getMimeType()
                    .toLowerCase(Locale.ROOT);
            exactMatch &= first.equals(current);
            if (!commonCategory.equals(mimeCategory(current))) {
                commonCategory = "*";
            }
        }
        if (exactMatch) {
            return first;
        }
        return commonCategory.equals("*")
                ? "*/*"
                : commonCategory + "/*";
    }

    private static String mimeCategory(String mimeType) {
        int separator = mimeType.indexOf('/');
        return separator > 0
                ? mimeType.substring(0, separator)
                : "*";
    }
}
