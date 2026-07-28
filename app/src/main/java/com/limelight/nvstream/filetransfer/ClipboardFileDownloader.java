package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.limelight.nvstream.http.NvHTTP;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClipboardFileDownloader {
    public interface Listener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    private ClipboardFileDownloader() {
    }

    public static int download(Context context, NvHTTP http, Uri destinationTree,
                               String transferId, long originId,
                               Listener listener) throws IOException {
        DocumentFile root = DocumentFile.fromTreeUri(context, destinationTree);
        if (root == null || !root.isDirectory() || !root.canWrite()) {
            throw new IOException("Selected directory is no longer writable");
        }

        byte[] encoded = http.downloadClipboardFileManifest(
                transferId, originId);
        FileManifest manifest = FileManifest.decode(encoded);
        Map<String, DocumentFile> documents = new HashMap<>();
        List<DocumentFile> createdTopLevel = new ArrayList<>();
        long transferred = 0;

        try {
            for (int index = 0; index < manifest.entries.size(); index++) {
                FileManifest.Entry entry = manifest.entries.get(index);
                int separator = entry.path.lastIndexOf('/');
                DocumentFile parent;
                String displayName;
                if (separator < 0) {
                    parent = root;
                    displayName = uniqueName(parent, entry.path,
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
                    createdTopLevel.add(document);
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
                        byte[] chunk = http.downloadClipboardFileChunk(
                                transferId, originId, index, offset, length);
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
            return createdTopLevel.size();
        } catch (Throwable error) {
            for (int index = createdTopLevel.size() - 1; index >= 0; index--) {
                try {
                    createdTopLevel.get(index).delete();
                } catch (Throwable ignored) {
                }
            }
            if (error instanceof IOException) {
                throw (IOException)error;
            }
            throw new IOException("File download failed", error);
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
}
