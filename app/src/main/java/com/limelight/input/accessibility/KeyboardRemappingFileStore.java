package com.limelight.input.accessibility;

import android.content.Context;
import android.util.AtomicFile;
import android.view.KeyEvent;

import com.limelight.LimeLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns the bounded, atomic storage contract for accessibility key remapping.
 */
public final class KeyboardRemappingFileStore {
    public static final String FILE_NAME = "keyboard_remapping.json";
    static final String LEGACY_FILE_NAME = "axi_switch_keyboard.json";
    public static final long MAXIMUM_BYTES = 4L * 1024L * 1024L;
    private static final int MAXIMUM_MAPPINGS = 4096;

    private KeyboardRemappingFileStore() {
    }

    /**
     * Resolves the canonical destination, migrating the historical file once.
     */
    public static File resolve(Context context) throws IOException {
        Context requiredContext = Objects.requireNonNull(context, "context");
        File filesDirectory = requiredContext.getFilesDir();
        File canonicalFile = new File(filesDirectory, FILE_NAME);
        File legacyFile = new File(filesDirectory, LEGACY_FILE_NAME);
        if (canonicalFile.isFile()) {
            removeLegacyFile(legacyFile);
            return canonicalFile;
        }
        if (!legacyFile.isFile()) {
            return canonicalFile;
        }

        copyAtomically(legacyFile, canonicalFile);
        removeLegacyFile(legacyFile);
        return canonicalFile;
    }

    /** Returns an immutable snapshot suitable for input-thread reads. */
    public static Map<Integer, Integer> load(Context context)
            throws IOException {
        File file = resolve(context);
        if (!file.isFile()) {
            return Collections.emptyMap();
        }
        try (InputStream input = new FileInputStream(file)) {
            return parse(readUtf8(input));
        }
    }

    public static boolean isManagedFileName(String name) {
        return name != null &&
                (name.equals(FILE_NAME) ||
                        name.equals(LEGACY_FILE_NAME) ||
                        name.startsWith(FILE_NAME + "."));
    }

    static Map<Integer, Integer> parse(String json) throws IOException {
        try {
            JSONArray mappings =
                    new JSONObject(json).getJSONArray("data");
            if (mappings.length() > MAXIMUM_MAPPINGS) {
                throw new IOException(
                        "Accessibility key mapping contains too many entries");
            }
            LinkedHashMap<Integer, Integer> result =
                    new LinkedHashMap<>();
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                int scanCode = mapping.getInt("scancode");
                int keyCode = mapping.getInt("code");
                if (scanCode < 0 || keyCode < 0 ||
                        keyCode > KeyEvent.getMaxKeyCode()) {
                    throw new IOException(
                            "Accessibility key mapping contains an invalid code");
                }
                if (!result.containsKey(scanCode)) {
                    result.put(scanCode, keyCode);
                }
            }
            return Collections.unmodifiableMap(result);
        }
        catch (JSONException error) {
            throw new IOException(
                    "Invalid accessibility key mapping document",
                    error);
        }
    }

    private static void copyAtomically(File source, File destination)
            throws IOException {
        AtomicFile atomicFile = new AtomicFile(destination);
        FileOutputStream output = null;
        try (InputStream input = new FileInputStream(source)) {
            output = atomicFile.startWrite();
            copyBounded(input, output);
            atomicFile.finishWrite(output);
        }
        catch (IOException | RuntimeException error) {
            if (output != null) {
                atomicFile.failWrite(output);
            }
            throw error;
        }
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copyBounded(input, output);
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static void copyBounded(
            InputStream input,
            java.io.OutputStream output) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long totalBytes = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            totalBytes += count;
            if (totalBytes > MAXIMUM_BYTES) {
                throw new IOException(
                        "Accessibility key mapping exceeds size limit");
            }
            output.write(buffer, 0, count);
        }
        output.flush();
    }

    private static void removeLegacyFile(File legacyFile) {
        if (legacyFile.exists() && !legacyFile.delete()) {
            LimeLog.warning(
                    "Unable to remove migrated accessibility key mapping");
        }
    }
}
