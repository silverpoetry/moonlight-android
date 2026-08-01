package com.limelight.nvstream.filetransfer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import com.limelight.transfer.FileManifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Enumerates Android share URIs into validated transfer-source metadata. */
final class DesktopFileUploadSourceEnumerator {
    private DesktopFileUploadSourceEnumerator() {
    }

    static List<DesktopFileUploadSource> enumerate(
            Context context,
            List<Uri> sourceUris) throws IOException {
        Objects.requireNonNull(context, "context");
        if (sourceUris == null || sourceUris.isEmpty()) {
            throw new IOException("No files were shared");
        }

        List<DesktopFileUploadSource> entries = new ArrayList<>();
        Set<String> topLevelNames = new HashSet<>();
        for (Uri uri : sourceUris) {
            if (uri == null) {
                continue;
            }
            if ("file".equalsIgnoreCase(uri.getScheme()) &&
                    uri.getPath() != null) {
                appendTopLevelDocument(
                        context,
                        DocumentFile.fromFile(new File(uri.getPath())),
                        topLevelNames,
                        entries);
            }
            else if ("content".equalsIgnoreCase(uri.getScheme()) &&
                    (isTreeUri(uri) ||
                            DocumentsContract.isDocumentUri(context, uri))) {
                appendTopLevelDocument(
                        context,
                        isTreeUri(uri)
                                ? DocumentFile.fromTreeUri(context, uri)
                                : DocumentFile.fromSingleUri(context, uri),
                        topLevelNames,
                        entries);
            }
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                appendContentUri(
                        context,
                        uri,
                        topLevelNames,
                        entries);
            }
            else {
                throw new IOException("Unsupported shared item URI");
            }
        }
        if (entries.isEmpty()) {
            throw new IOException("No files were shared");
        }
        return entries;
    }

    private static boolean isTreeUri(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return segments.size() >= 2 &&
                "tree".equals(segments.get(0));
    }

    private static void appendTopLevelDocument(
            Context context,
            DocumentFile document,
            Set<String> topLevelNames,
            List<DesktopFileUploadSource> entries) throws IOException {
        if (document == null ||
                !document.exists() ||
                (!document.isFile() && !document.isDirectory())) {
            throw new IOException("A shared item is unavailable");
        }

        String original = FileManifest.sanitizeName(document.getName());
        String rootName = uniqueName(
                original,
                document.isDirectory(),
                topLevelNames);
        appendDocument(context, document, rootName, entries);
    }

    private static void appendContentUri(
            Context context,
            Uri uri,
            Set<String> topLevelNames,
            List<DesktopFileUploadSource> entries) throws IOException {
        if (entries.size() >= FileManifest.MAX_ENTRIES) {
            throw new IOException("Too many shared files");
        }
        SharedContentInfo content = querySharedContent(context, uri);
        String rootName = uniqueName(
                content.name,
                false,
                topLevelNames);
        entries.add(new DesktopFileUploadSource(
                new FileManifest.Entry(
                        FileManifest.TYPE_REGULAR,
                        rootName,
                        content.size,
                        0),
                uri));
    }

    private static String uniqueName(
            String original,
            boolean directory,
            Set<String> names) {
        String name = original;
        int suffix = 2;
        while (!names.add(name.toLowerCase(Locale.ROOT))) {
            name = FileManifest.appendCollisionSuffix(
                    original,
                    suffix++,
                    directory);
        }
        return name;
    }

    private static SharedContentInfo querySharedContent(
            Context context,
            Uri uri) throws IOException {
        String name = null;
        long size = -1;
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] {
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE
                },
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndex(
                        OpenableColumns.SIZE);
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) {
                    name = cursor.getString(nameColumn);
                }
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    size = cursor.getLong(sizeColumn);
                }
            }
        }
        catch (SecurityException error) {
            throw new IOException(
                    "Shared item permission was not granted",
                    error);
        }
        catch (RuntimeException ignored) {
            // Some providers support streams but not metadata queries.
        }

        if (name == null || name.isEmpty()) {
            name = uri.getLastPathSegment();
        }
        if (name == null || name.isEmpty()) {
            throw new IOException(
                    "Unable to determine shared file name");
        }
        if (size < 0) {
            try (ParcelFileDescriptor descriptor = context
                    .getContentResolver()
                    .openFileDescriptor(uri, "r")) {
                if (descriptor != null) {
                    size = descriptor.getStatSize();
                }
            }
            catch (SecurityException error) {
                throw new IOException(
                        "Shared item permission was not granted",
                        error);
            }
            catch (RuntimeException error) {
                throw new IOException(
                        "Unable to inspect shared item",
                        error);
            }
        }
        if (size < 0 || size > FileManifest.MAX_FILE_BYTES) {
            throw new IOException(
                    "Unable to determine shared file size");
        }
        return new SharedContentInfo(
                FileManifest.sanitizeName(name),
                size);
    }

    private static void appendDocument(
            Context context,
            DocumentFile document,
            String path,
            List<DesktopFileUploadSource> entries) throws IOException {
        if (entries.size() >= FileManifest.MAX_ENTRIES) {
            throw new IOException("Too many shared files");
        }
        if (document.isDirectory()) {
            entries.add(new DesktopFileUploadSource(
                    new FileManifest.Entry(
                            FileManifest.TYPE_DIRECTORY,
                            path,
                            0,
                            Math.max(0, document.lastModified())),
                    null));
            DocumentFile[] children = document.listFiles();
            Arrays.sort(children, (first, second) ->
                    String.valueOf(first.getName()).compareToIgnoreCase(
                            String.valueOf(second.getName())));
            Set<String> siblingNames = new HashSet<>();
            for (DocumentFile child : children) {
                if (!child.isFile() && !child.isDirectory()) {
                    throw new IOException(
                            "Unsupported shared item type");
                }
                String original = FileManifest.sanitizeName(
                        child.getName());
                String childName = uniqueName(
                        original,
                        child.isDirectory(),
                        siblingNames);
                appendDocument(
                        context,
                        child,
                        path + '/' + childName,
                        entries);
            }
            return;
        }

        long size = querySize(context, document);
        if (size < 0 || size > FileManifest.MAX_FILE_BYTES) {
            throw new IOException(
                    "Unable to determine shared file size");
        }
        entries.add(new DesktopFileUploadSource(
                new FileManifest.Entry(
                        FileManifest.TYPE_REGULAR,
                        path,
                        size,
                        Math.max(0, document.lastModified())),
                document.getUri()));
    }

    private static long querySize(
            Context context,
            DocumentFile document) {
        try (Cursor cursor = context.getContentResolver().query(
                document.getUri(),
                new String[] {OpenableColumns.SIZE},
                null,
                null,
                null)) {
            if (cursor != null &&
                    cursor.moveToFirst() &&
                    !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
        }
        catch (RuntimeException ignored) {
            // Fall through to DocumentFile.length().
        }
        long size = document.length();
        return size >= 0 ? size : -1;
    }

    private static final class SharedContentInfo {
        private final String name;
        private final long size;

        private SharedContentInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }
}
