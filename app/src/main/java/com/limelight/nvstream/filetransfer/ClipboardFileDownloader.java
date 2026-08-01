package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import androidx.documentfile.provider.DocumentFile;

import com.limelight.nvstream.http.NvHTTP;
import com.limelight.transfer.FileManifest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClipboardFileDownloader {
    public interface Listener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    private ClipboardFileDownloader() {
    }

    public static int download(Context context, NvHTTP http, Uri destinationTree,
                               String transferId, long originId,
                               Listener listener) throws IOException {
        return download(
                context,
                http,
                destinationTree,
                transferId,
                originId,
                listener,
                null);
    }

    public static int download(Context context, NvHTTP http, Uri destinationTree,
                               String transferId, long originId,
                               Listener listener,
                               CancellationSignal cancellationSignal)
            throws IOException {
        DocumentFile root = DocumentFile.fromTreeUri(context, destinationTree);
        if (root == null || !root.isDirectory() || !root.canWrite()) {
            throw new IOException("Selected directory is no longer writable");
        }

        throwIfCanceled(cancellationSignal);
        byte[] encoded = http.downloadClipboardFileManifest(
                transferId, originId, cancellationSignal);
        FileManifest manifest = FileManifest.decode(encoded);
        Map<String, DocumentFile> documents = new HashMap<>();
        List<TopLevelItem> topLevelItems = new ArrayList<>();
        long transferred = 0;

        try {
            for (int index = 0; index < manifest.entries.size(); index++) {
                throwIfCanceled(cancellationSignal);
                FileManifest.Entry entry = manifest.entries.get(index);
                int separator = entry.path.lastIndexOf('/');
                DocumentFile parent;
                String displayName;
                if (separator < 0) {
                    parent = root;
                    displayName = uniqueStagingName(
                            parent,
                            entry.type == FileManifest.TYPE_DIRECTORY);
                }
                else {
                    parent = documents.get(entry.path.substring(0, separator));
                    displayName = entry.path.substring(separator + 1);
                    if (parent == null || !parent.isDirectory()) {
                        throw new IOException("File manifest parent is unavailable");
                    }
                }

                DocumentFile document;
                if (entry.type == FileManifest.TYPE_DIRECTORY) {
                    document = parent.createDirectory(displayName);
                }
                else {
                    document = parent.createFile("application/octet-stream", displayName);
                }
                if (document == null) {
                    throw new IOException("Unable to create " + displayName);
                }
                if (separator < 0) {
                    topLevelItems.add(new TopLevelItem(
                            document,
                            entry.path,
                            entry.type == FileManifest.TYPE_DIRECTORY));
                }
                documents.put(entry.path, document);

                if (entry.type != FileManifest.TYPE_REGULAR) {
                    continue;
                }

                try (OutputStream output = context.getContentResolver()
                        .openOutputStream(document.getUri(), "w")) {
                    if (output == null) {
                        throw new IOException("Unable to open " + displayName);
                    }
                    long offset = 0;
                    while (offset < entry.size) {
                        int length = (int)Math.min(
                                FileManifest.MAX_CHUNK_BYTES, entry.size - offset);
                        throwIfCanceled(cancellationSignal);
                        byte[] chunk = http.downloadClipboardFileChunk(
                                transferId,
                                originId,
                                index,
                                offset,
                                length,
                                cancellationSignal);
                        throwIfCanceled(cancellationSignal);
                        output.write(chunk);
                        offset += chunk.length;
                        transferred += chunk.length;
                        if (listener != null) {
                            listener.onProgress(transferred, manifest.totalFileBytes);
                        }
                    }
                    output.flush();
                }
            }

            commitTopLevelItems(root, topLevelItems, cancellationSignal);
            return topLevelItems.size();
        } catch (Throwable error) {
            deleteTopLevelItems(topLevelItems);
            if (error instanceof OperationCanceledException) {
                throw (OperationCanceledException)error;
            }
            if (error instanceof IOException) {
                throw (IOException)error;
            }
            throw new IOException("File download failed", error);
        }
    }

    private static void commitTopLevelItems(
            DocumentFile root,
            List<TopLevelItem> topLevelItems,
            CancellationSignal cancellationSignal) throws IOException {
        for (TopLevelItem item : topLevelItems) {
            throwIfCanceled(cancellationSignal);
            String finalName = uniqueName(
                    root,
                    item.requestedName,
                    item.directory);
            if (!item.document.renameTo(finalName)) {
                throw new IOException(
                        "Unable to commit " + item.requestedName);
            }
        }
        throwIfCanceled(cancellationSignal);
    }

    private static void deleteTopLevelItems(
            List<TopLevelItem> topLevelItems) {
        for (int index = topLevelItems.size() - 1; index >= 0; index--) {
            try {
                topLevelItems.get(index).document.delete();
            }
            catch (Throwable ignored) {
            }
        }
    }

    private static String uniqueStagingName(
            DocumentFile parent,
            boolean directory) {
        String requested = ".moonlight-transfer-" + UUID.randomUUID() +
                (directory ? "" : ".part");
        return uniqueName(parent, requested, directory);
    }

    private static void throwIfCanceled(
            CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }
    }

    private static String uniqueName(DocumentFile parent, String requested,
                                     boolean directory) {
        String candidate = requested;
        int suffix = 2;
        while (containsName(parent, candidate)) {
            candidate = FileManifest.appendCollisionSuffix(
                    requested, suffix++, directory);
        }
        return candidate;
    }

    private static boolean containsName(DocumentFile parent, String name) {
        for (DocumentFile child : parent.listFiles()) {
            String childName = child.getName();
            if (childName != null && childName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static final class TopLevelItem {
        final DocumentFile document;
        final String requestedName;
        final boolean directory;

        TopLevelItem(DocumentFile document,
                     String requestedName,
                     boolean directory) {
            this.document = document;
            this.requestedName = requestedName;
            this.directory = directory;
        }
    }
}
