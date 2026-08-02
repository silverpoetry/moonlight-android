package com.limelight.virtualcontrols.layout.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;

import androidx.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.limelight.LimeLog;
import com.limelight.platform.files.AndroidPrivateFileShare;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutDocument;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutReadResult;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Android internal-storage adapter for virtual-control layouts.
 *
 * <p>Writes use a canonical JSON file and {@link AtomicFile}, so a process
 * death cannot publish a partial layout. A validated historical document is
 * migrated once on first access. All I/O is expected to run off
 * latency-sensitive input callbacks.</p>
 */
public final class AndroidVirtualControlLayoutRepository
        implements VirtualControlLayoutRepository {
    private final Context applicationContext;
    private final File filesDirectory;

    public AndroidVirtualControlLayoutRepository(Context context) {
        Context requiredContext =
                Objects.requireNonNull(context, "context");
        Context candidate =
                requiredContext.getApplicationContext();
        applicationContext =
                candidate != null ? candidate : requiredContext;
        filesDirectory = applicationContext.getFilesDir();
    }

    @Override
    public VirtualControlLayoutReadResult load(
            VirtualControlLayoutKey key) throws IOException {
        File file = ensureCanonicalFile(key);
        if (!file.isFile()) {
            return VirtualControlLayoutReadResult.missing();
        }
        VirtualControlLayoutDocument document = readDocument(file);
        removeLegacyFile(key);
        return VirtualControlLayoutReadResult.found(document);
    }

    @Override
    public void save(
            VirtualControlLayoutKey key,
            VirtualControlLayoutDocument document) throws IOException {
        Objects.requireNonNull(document, "document");
        writeDocument(resolveCanonicalFile(key), document);
        removeLegacyFile(key);
    }

    private static void writeDocument(
            File file,
            VirtualControlLayoutDocument document) throws IOException {
        byte[] bytes = document.getJson().getBytes(StandardCharsets.UTF_8);
        AtomicFile atomicFile = new AtomicFile(file);
        FileOutputStream output = null;
        try {
            output = atomicFile.startWrite();
            output.write(bytes);
            output.flush();
            atomicFile.finishWrite(output);
        }
        catch (IOException error) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw error;
        }
    }

    public void importFrom(
            ContentResolver resolver,
            Uri source,
            VirtualControlLayoutKey destination) throws IOException {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(source, "source");
        try (InputStream input = resolver.openInputStream(source)) {
            if (input == null) {
                throw new FileNotFoundException(
                        "Document provider returned no input stream");
            }
            save(
                    destination,
                    parseImportedDocument(readUtf8(input)));
        }
    }

    @Nullable
    public Uri getShareUri(
            VirtualControlLayoutKey key) throws IOException {
        File file = ensureCanonicalFile(key);
        if (!file.isFile()) {
            return null;
        }
        return AndroidPrivateFileShare.stageReadOnly(
                applicationContext,
                file);
    }

    static String canonicalFileName(VirtualControlLayoutKey key) {
        return "virtual_control_" +
                key.getKind().name().toLowerCase(Locale.ROOT) +
                "_" + key.getProfileId() +
                (key.getOrientation() ==
                        VirtualControlLayoutOrientation.PORTRAIT
                        ? "_portrait"
                        : "_landscape") +
                ".json";
    }

    static String legacyFileName(VirtualControlLayoutKey key) {
        String orientationSuffix =
                key.getOrientation() ==
                        VirtualControlLayoutOrientation.PORTRAIT
                        ? "_1"
                        : "";
        return "axi_" +
                key.getProfileId() +
                orientationSuffix +
                ".txt";
    }

    static VirtualControlLayoutDocument parseImportedDocument(
            String json) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                throw new IOException(
                        "Virtual-control layout root must be an array");
            }
            return VirtualControlLayoutDocument.fromJson(json);
        }
        catch (JsonParseException | IllegalArgumentException error) {
            throw new IOException(
                    "Invalid virtual-control layout JSON",
                    error);
        }
    }

    private File ensureCanonicalFile(
            VirtualControlLayoutKey key) throws IOException {
        Objects.requireNonNull(key, "key");
        File canonicalFile = resolveCanonicalFile(key);
        if (canonicalFile.isFile()) {
            return canonicalFile;
        }

        File legacyFile = resolveLegacyFile(key);
        if (!legacyFile.isFile()) {
            return canonicalFile;
        }

        VirtualControlLayoutDocument legacyDocument =
                readDocument(legacyFile);
        writeDocument(canonicalFile, legacyDocument);
        removeLegacyFile(key);
        return canonicalFile;
    }

    private File resolveCanonicalFile(VirtualControlLayoutKey key) {
        Objects.requireNonNull(key, "key");
        return new File(filesDirectory, canonicalFileName(key));
    }

    private File resolveLegacyFile(VirtualControlLayoutKey key) {
        Objects.requireNonNull(key, "key");
        return new File(filesDirectory, legacyFileName(key));
    }

    private void removeLegacyFile(VirtualControlLayoutKey key) {
        File legacyFile = resolveLegacyFile(key);
        if (legacyFile.exists() && !legacyFile.delete()) {
            LimeLog.warning(
                    "Unable to remove migrated virtual-control layout");
        }
    }

    private static VirtualControlLayoutDocument readDocument(File file)
            throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return VirtualControlLayoutDocument.fromJson(readUtf8(input));
        }
    }

    private static String readUtf8(InputStream input)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalBytes = 0;
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes >
                    VirtualControlLayoutDocument.MAX_UTF8_BYTES) {
                throw new IOException(
                        "Virtual-control layout exceeds " +
                                VirtualControlLayoutDocument
                                        .MAX_UTF8_BYTES +
                                " bytes");
            }
            output.write(buffer, 0, bytesRead);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
