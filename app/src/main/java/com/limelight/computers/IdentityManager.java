package com.limelight.computers;

import android.content.Context;
import android.util.AtomicFile;

import com.limelight.LimeLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;

/** Owns the stable, atomically persisted client identity used for pairing. */
public final class IdentityManager {
    private static final String UNIQUE_ID_FILE_NAME = "uniqueid";
    private static final int UNIQUE_ID_BYTES = 8;
    private static final int UNIQUE_ID_CHARACTERS = UNIQUE_ID_BYTES * 2;

    private final String uniqueId;

    public IdentityManager(Context context) {
        Objects.requireNonNull(context, "context");
        AtomicFile identityFile = new AtomicFile(
                new File(context.getFilesDir(), UNIQUE_ID_FILE_NAME));
        String persistedIdentity = loadUniqueId(identityFile);
        if (persistedIdentity != null) {
            uniqueId = persistedIdentity;
            return;
        }

        uniqueId = String.format(
                (Locale) null,
                "%016x",
                new SecureRandom().nextLong());
        persistUniqueId(identityFile, uniqueId);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    private static String loadUniqueId(AtomicFile identityFile) {
        try {
            byte[] encoded = identityFile.readFully();
            if (encoded.length != UNIQUE_ID_CHARACTERS) {
                LimeLog.warning("Persisted client identity has an invalid length");
                return null;
            }
            String candidate = new String(
                    encoded,
                    StandardCharsets.US_ASCII);
            for (int index = 0; index < candidate.length(); index++) {
                char character = candidate.charAt(index);
                if (!((character >= '0' && character <= '9') ||
                        (character >= 'a' && character <= 'f'))) {
                    LimeLog.warning("Persisted client identity has an invalid format");
                    return null;
                }
            }
            return candidate;
        }
        catch (FileNotFoundException error) {
            return null;
        }
        catch (IOException error) {
            LimeLog.warning("Unable to read persisted client identity");
            return null;
        }
    }

    private static void persistUniqueId(
            AtomicFile identityFile,
            String uniqueId) {
        FileOutputStream output = null;
        try {
            output = identityFile.startWrite();
            output.write(uniqueId.getBytes(StandardCharsets.US_ASCII));
            identityFile.finishWrite(output);
        }
        catch (IOException error) {
            if (output != null) {
                identityFile.failWrite(output);
            }
            LimeLog.warning("Unable to persist client identity atomically");
        }
    }
}
