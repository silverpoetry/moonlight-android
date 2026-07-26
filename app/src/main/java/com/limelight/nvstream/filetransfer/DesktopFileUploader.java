package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.v4.provider.DocumentFile;

import com.limelight.nvstream.http.NvHTTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DesktopFileUploader {
    public interface Listener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    private DesktopFileUploader() {
    }

    public static void upload(Context context, NvHTTP http, List<Uri> sourceUris,
                              Listener listener) throws IOException {
        List<FileManifest.Entry> entries = enumerate(context, sourceUris);
        FileManifest manifest = FileManifest.validate(entries);
        byte[] encoded = FileManifest.encode(entries);
        String token = randomToken();
        NvHTTP.DesktopFileUploadResult transfer = http.beginDesktopFileUpload(
                encoded, token, UUID.randomUUID().toString());

        long transferred = 0;
        byte[] buffer = new byte[FileManifest.MAX_CHUNK_BYTES];
        for (int index = 0; index < manifest.entries.size(); index++) {
            FileManifest.Entry entry = manifest.entries.get(index);
            if (entry.type != FileManifest.TYPE_REGULAR) {
                continue;
            }
            if (entry.sourceUri == null) {
                throw new IOException("Shared file is no longer available");
            }

            try (InputStream input = context.getContentResolver()
                    .openInputStream(entry.sourceUri)) {
                if (input == null) {
                    throw new IOException("Unable to open " + entry.path);
                }
                long offset = 0;
                while (offset < entry.size) {
                    int requested = (int)Math.min(buffer.length, entry.size - offset);
                    int length = readFully(input, buffer, requested);
                    if (length != requested) {
                        throw new IOException("Shared file changed during transfer");
                    }
                    byte[] chunk = length == buffer.length ?
                            buffer.clone() :
                            Arrays.copyOf(buffer, length);
                    http.uploadDesktopFileChunk(
                            transfer.id, token, index, offset, chunk);
                    offset += length;
                    transferred += length;
                    if (listener != null) {
                        listener.onProgress(transferred, manifest.totalFileBytes);
                    }
                }
                if (input.read() != -1) {
                    throw new IOException("Shared file changed during transfer");
                }
            }
        }
        http.completeDesktopFileUpload(transfer.id, token);
    }

    private static List<FileManifest.Entry> enumerate(Context context,
                                                       List<Uri> sourceUris)
            throws IOException {
        if (sourceUris == null || sourceUris.isEmpty()) {
            throw new IOException("No files were shared");
        }

        List<FileManifest.Entry> entries = new ArrayList<>();
        Set<String> topLevelNames = new HashSet<>();
        for (Uri uri : sourceUris) {
            if (uri == null) {
                continue;
            }
            DocumentFile document;
            if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
                document = DocumentFile.fromFile(new File(uri.getPath()));
            }
            else {
                document = DocumentsContract.isTreeUri(uri) ?
                        DocumentFile.fromTreeUri(context, uri) :
                        DocumentFile.fromSingleUri(context, uri);
            }
            if (document == null || !document.exists() ||
                    (!document.isFile() && !document.isDirectory())) {
                throw new IOException("A shared item is unavailable");
            }

            String original = FileManifest.sanitizeName(document.getName());
            String rootName = original;
            int suffix = 2;
            while (!topLevelNames.add(rootName.toLowerCase(java.util.Locale.ROOT))) {
                rootName = FileManifest.appendCollisionSuffix(
                        original, suffix++, document.isDirectory());
            }
            appendDocument(context, document, rootName, entries);
        }
        if (entries.isEmpty()) {
            throw new IOException("No files were shared");
        }
        return entries;
    }

    private static void appendDocument(Context context, DocumentFile document,
                                       String path,
                                       List<FileManifest.Entry> entries)
            throws IOException {
        if (entries.size() >= FileManifest.MAX_ENTRIES) {
            throw new IOException("Too many shared files");
        }
        if (document.isDirectory()) {
            entries.add(new FileManifest.Entry(
                    FileManifest.TYPE_DIRECTORY,
                    path,
                    0,
                    Math.max(0, document.lastModified()),
                    document.getUri()));
            DocumentFile[] children = document.listFiles();
            Arrays.sort(children, (first, second) -> {
                String firstName = first.getName();
                String secondName = second.getName();
                return String.valueOf(firstName).compareToIgnoreCase(
                        String.valueOf(secondName));
            });
            Set<String> siblingNames = new HashSet<>();
            for (DocumentFile child : children) {
                if (!child.isFile() && !child.isDirectory()) {
                    throw new IOException("Unsupported shared item type");
                }
                String original = FileManifest.sanitizeName(child.getName());
                String childName = original;
                int suffix = 2;
                while (!siblingNames.add(
                        childName.toLowerCase(java.util.Locale.ROOT))) {
                    childName = FileManifest.appendCollisionSuffix(
                            original, suffix++, child.isDirectory());
                }
                appendDocument(context, child, path + '/' + childName, entries);
            }
            return;
        }

        long size = querySize(context, document);
        if (size < 0 || size > FileManifest.MAX_FILE_BYTES) {
            throw new IOException("Unable to determine shared file size");
        }
        entries.add(new FileManifest.Entry(
                FileManifest.TYPE_REGULAR,
                path,
                size,
                Math.max(0, document.lastModified()),
                document.getUri()));
    }

    private static long querySize(Context context, DocumentFile document) {
        try (Cursor cursor = context.getContentResolver().query(
                document.getUri(),
                new String[] {OpenableColumns.SIZE},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
        } catch (Throwable ignored) {
        }
        long size = document.length();
        return size >= 0 ? size : -1;
    }

    private static int readFully(InputStream input, byte[] buffer, int length)
            throws IOException {
        int total = 0;
        while (total < length) {
            int read = input.read(buffer, total, length - total);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                int value = input.read();
                if (value < 0) {
                    break;
                }
                buffer[total++] = (byte)value;
            }
            else {
                total += read;
            }
        }
        return total;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        char[] alphabet = "0123456789abcdef".toCharArray();
        char[] encoded = new char[64];
        for (int index = 0; index < bytes.length; index++) {
            encoded[index * 2] = alphabet[(bytes[index] >>> 4) & 0xF];
            encoded[index * 2 + 1] = alphabet[bytes[index] & 0xF];
        }
        return new String(encoded);
    }
}
