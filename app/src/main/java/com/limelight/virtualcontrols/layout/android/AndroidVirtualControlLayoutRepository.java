package com.limelight.virtualcontrols.layout.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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
import java.util.Objects;

/**
 * Android internal-storage adapter for virtual-control layouts.
 *
 * <p>Historical file names are retained as an implementation detail. Writes
 * use {@link AtomicFile}, so a process death cannot publish a partial layout.
 * All I/O is expected to run off latency-sensitive input callbacks.</p>
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
        File file = resolveFile(key);
        if (!file.isFile()) {
            return VirtualControlLayoutReadResult.missing();
        }
        try (InputStream input = new FileInputStream(file)) {
            return VirtualControlLayoutReadResult.found(
                    VirtualControlLayoutDocument.fromJson(
                            readUtf8(input)));
        }
    }

    @Override
    public void save(
            VirtualControlLayoutKey key,
            VirtualControlLayoutDocument document) throws IOException {
        Objects.requireNonNull(document, "document");
        byte[] bytes =
                document.getJson().getBytes(StandardCharsets.UTF_8);
        AtomicFile atomicFile = new AtomicFile(resolveFile(key));
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
            VirtualControlLayoutKey key) {
        File file = resolveFile(key);
        if (!file.isFile()) {
            return null;
        }
        return FileProvider.getUriForFile(
                applicationContext,
                applicationContext.getPackageName() +
                        ".fileprovider",
                file);
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

    private File resolveFile(VirtualControlLayoutKey key) {
        Objects.requireNonNull(key, "key");
        return new File(filesDirectory, legacyFileName(key));
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
