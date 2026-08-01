package com.limelight.computers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Performs side-effect-free validation before a database reaches SQLite. */
final class SQLiteDatabaseFileHeader {
    private static final byte[] SQLITE_HEADER =
            "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private SQLiteDatabaseFileHeader() {
    }

    static boolean isValid(File databaseFile) {
        if (databaseFile == null ||
                !databaseFile.isFile() ||
                databaseFile.length() < SQLITE_HEADER.length) {
            return false;
        }

        byte[] header = new byte[SQLITE_HEADER.length];
        try (FileInputStream input = new FileInputStream(databaseFile)) {
            int offset = 0;
            while (offset < header.length) {
                int read = input.read(header, offset, header.length - offset);
                if (read < 0) {
                    return false;
                }
                offset += read;
            }
            return Arrays.equals(header, SQLITE_HEADER);
        }
        catch (IOException error) {
            return false;
        }
    }

    static void requireValidIfPopulated(File databaseFile) {
        if (!databaseFile.exists() || databaseFile.length() == 0) {
            return;
        }
        if (!isValid(databaseFile)) {
            throw new IllegalStateException(
                    "Host database has an invalid SQLite header");
        }
    }
}
