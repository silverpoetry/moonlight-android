package com.limelight.platform.files;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Stages app-private files behind the smallest FileProvider surface required
 * for read-only sharing.
 *
 * <p>Persistent files are never exposed directly. Each outbound share gets a
 * private cache session, while clipboard images remain confined to their own
 * cache directory. The FileProvider XML mirrors these two directories.</p>
 */
public final class AndroidPrivateFileShare {
    public static final String CLIPBOARD_CACHE_DIRECTORY = "clipboard";
    public static final String OUTBOUND_CACHE_DIRECTORY = "outbound-shares";

    private static final long OUTBOUND_RETENTION_MILLIS =
            24L * 60L * 60L * 1000L;

    private AndroidPrivateFileShare() {
    }

    /** Copies an owned private file into a unique, read-only share session. */
    public static Uri stageReadOnly(Context context, File source)
            throws IOException {
        Context applicationContext = applicationContext(context);
        File canonicalSource = requireOwnedFile(applicationContext, source);
        File outboundRoot = requireDirectory(
                new File(
                        applicationContext.getCacheDir(),
                        OUTBOUND_CACHE_DIRECTORY));
        cleanupExpiredSessions(outboundRoot, System.currentTimeMillis());

        File session = new File(
                outboundRoot,
                UUID.randomUUID().toString());
        if (!session.mkdir()) {
            throw new IOException(
                    "Unable to create outbound share session");
        }

        File stagedFile = new File(session, canonicalSource.getName());
        try {
            copyAtomically(canonicalSource, stagedFile);
            return uriFor(applicationContext, stagedFile);
        }
        catch (IOException | RuntimeException error) {
            deleteRecursively(session);
            throw error;
        }
    }

    /** Exposes only a regular file inside the dedicated clipboard cache. */
    public static Uri exposeClipboardFile(Context context, File file)
            throws IOException {
        Context applicationContext = applicationContext(context);
        File clipboardRoot = requireDirectory(
                new File(
                        applicationContext.getCacheDir(),
                        CLIPBOARD_CACHE_DIRECTORY));
        File canonicalFile = Objects
                .requireNonNull(file, "file")
                .getCanonicalFile();
        if (!isStrictDescendant(canonicalFile, clipboardRoot) ||
                !canonicalFile.isFile()) {
            throw new FileNotFoundException(
                    "Clipboard share is outside its private cache");
        }
        return uriFor(applicationContext, canonicalFile);
    }

    private static Context applicationContext(Context context) {
        Context requiredContext = Objects.requireNonNull(context, "context");
        Context candidate = requiredContext.getApplicationContext();
        return candidate != null ? candidate : requiredContext;
    }

    private static File requireOwnedFile(Context context, File source)
            throws IOException {
        File canonicalSource = Objects
                .requireNonNull(source, "source")
                .getCanonicalFile();
        File filesRoot = context.getFilesDir().getCanonicalFile();
        File cacheRoot = context.getCacheDir().getCanonicalFile();
        if ((!isStrictDescendant(canonicalSource, filesRoot) &&
                !isStrictDescendant(canonicalSource, cacheRoot)) ||
                !canonicalSource.isFile()) {
            throw new FileNotFoundException(
                    "Outbound share is not an owned private file");
        }
        return canonicalSource;
    }

    private static File requireDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException(
                    "Unable to create private share directory");
        }
        return directory.getCanonicalFile();
    }

    private static boolean isStrictDescendant(File file, File directory)
            throws IOException {
        String filePath = file.getCanonicalPath();
        String directoryPath = directory.getCanonicalPath();
        return filePath.startsWith(directoryPath + File.separator);
    }

    private static void copyAtomically(File source, File destination)
            throws IOException {
        AtomicFile atomicFile = new AtomicFile(destination);
        FileOutputStream output = null;
        try (InputStream input = new FileInputStream(source)) {
            output = atomicFile.startWrite();
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
            atomicFile.finishWrite(output);
        }
        catch (IOException | RuntimeException error) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw error;
        }
    }

    private static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file);
    }

    private static void cleanupExpiredSessions(File root, long now) {
        File[] sessions = root.listFiles();
        if (sessions == null) {
            return;
        }
        long cutoff = now - OUTBOUND_RETENTION_MILLIS;
        for (File session : sessions) {
            if (session.lastModified() < cutoff) {
                deleteRecursively(session);
            }
        }
    }

    private static void deleteRecursively(File target) {
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (target.exists() && !target.delete()) {
            // Cache cleanup is best effort and retried on the next share.
        }
    }
}
