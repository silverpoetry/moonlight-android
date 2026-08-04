package com.limelight.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.limelight.LimeLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Imports private state from the final legacy package before repositories open it. */
public final class LegacyDataImporter {
    private static final String TARGET_PACKAGE = "com.silverpoetry.moonlight";
    private static final Uri LEGACY_EXPORT_URI = Uri.parse(
            "content://com.limelight.unofficialA.migration/export");
    private static final String MARKER = ".legacy-package-imported";
    private static final String PREFERENCES_MARKER =
            ".legacy-default-preferences-imported";
    private static final String LEGACY_DEFAULT_PREFERENCES =
            "com.limelight.unofficialA_preferences";
    private static final String TARGET_DEFAULT_PREFERENCES =
            TARGET_PACKAGE + "_preferences";

    private LegacyDataImporter() {
    }

    public static void importIfAvailable(Context context) {
        if (!TARGET_PACKAGE.equals(context.getPackageName())) {
            return;
        }
        importDefaultPreferencesIfAvailable(context);
        File marker = new File(context.getFilesDir(), MARKER);
        if (marker.isFile()) {
            return;
        }
        if (context.getPackageManager().resolveContentProvider(
                LEGACY_EXPORT_URI.getAuthority(), 0) == null) {
            return;
        }

        try (InputStream stream = context.getContentResolver()
                .openInputStream(LEGACY_EXPORT_URI)) {
            if (stream == null) {
                return;
            }
            int importedFiles = importArchive(context, stream);
            importDefaultPreferencesIfAvailable(context);
            if (importedFiles > 0 && marker.createNewFile()) {
                LimeLog.info("Imported legacy package data: " +
                        importedFiles + " files");
            }
        }
        catch (IOException | SecurityException e) {
            LimeLog.warning("Unable to import legacy package data", e);
        }
    }

    /**
     * Moves Android's package-derived default preference file into the new
     * package namespace. This runs before any settings repository is opened.
     */
    private static void importDefaultPreferencesIfAvailable(
            Context context) {
        File marker = new File(
                context.getFilesDir(),
                PREFERENCES_MARKER);
        File sourceFile = new File(
                new File(
                        context.getApplicationInfo().dataDir,
                        "shared_prefs"),
                LEGACY_DEFAULT_PREFERENCES + ".xml");
        if (marker.isFile() || !sourceFile.isFile()) {
            return;
        }

        SharedPreferences source = context.getSharedPreferences(
                LEGACY_DEFAULT_PREFERENCES,
                Context.MODE_PRIVATE);
        Map<String, ?> values = source.getAll();
        if (values.isEmpty()) {
            return;
        }

        SharedPreferences.Editor target = context
                .getSharedPreferences(
                        TARGET_DEFAULT_PREFERENCES,
                        Context.MODE_PRIVATE)
                .edit()
                .clear();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            putPreference(target, entry.getKey(), entry.getValue());
        }
        if (target.commit()) {
            try {
                marker.createNewFile();
            }
            catch (IOException e) {
                LimeLog.warning(
                        "Unable to mark legacy preference migration",
                        e);
            }
        }
    }

    private static void putPreference(
            SharedPreferences.Editor editor,
            String key,
            Object value) {
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        }
        else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        }
        else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        }
        else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        }
        else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        else if (value instanceof Set<?>) {
            Set<String> strings = new LinkedHashSet<>();
            for (Object element : (Set<?>) value) {
                if (!(element instanceof String)) {
                    LimeLog.warning(
                            "Skipping malformed legacy string set: " + key);
                    return;
                }
                strings.add((String) element);
            }
            editor.putStringSet(key, strings);
        }
        else {
            LimeLog.warning(
                    "Skipping unsupported legacy preference: " + key);
        }
    }

    private static int importArchive(
            Context context,
            InputStream stream) throws IOException {
        File dataRoot = new File(
                context.getApplicationInfo().dataDir)
                .getCanonicalFile();
        String dataRootPrefix = dataRoot.getPath() + File.separator;
        int importedFiles = 0;
        byte[] buffer = new byte[32 * 1024];
        try (ZipInputStream archive = new ZipInputStream(
                new BufferedInputStream(stream))) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                File target = new File(dataRoot, entry.getName())
                        .getCanonicalFile();
                if (!target.getPath().startsWith(dataRootPrefix)) {
                    throw new IOException(
                            "Migration archive escaped app data directory");
                }
                if (entry.isDirectory()) {
                    ensureDirectory(target);
                    continue;
                }
                File parent = target.getParentFile();
                ensureDirectory(parent);
                File temporary = File.createTempFile(
                        ".migration-", ".tmp", parent);
                try (BufferedOutputStream output =
                             new BufferedOutputStream(
                                     new FileOutputStream(temporary))) {
                    int read;
                    while ((read = archive.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                if (target.exists() && !target.delete()) {
                    throw new IOException(
                            "Unable to replace " + target.getName());
                }
                if (!temporary.renameTo(target)) {
                    throw new IOException(
                            "Unable to publish " + target.getName());
                }
                if (entry.getTime() > 0) {
                    target.setLastModified(entry.getTime());
                }
                importedFiles++;
            }
        }
        return importedFiles;
    }

    private static void ensureDirectory(File directory)
            throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException(
                    "Unable to create migration directory");
        }
    }
}
