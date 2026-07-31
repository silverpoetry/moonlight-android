package com.limelight.shortcuts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Stable identity policy for built-in, imported, and user shortcuts.
 */
public final class GameMenuShortcutIds {
    public static final String BUILT_IN_PREFIX =
            "shortcut:builtin:";
    public static final String IMPORTED_PREFIX =
            "shortcut:imported:";
    public static final String CUSTOM_PREFIX =
            "shortcut:custom:";

    private GameMenuShortcutIds() {
    }

    public static String builtIn(String name) {
        return BUILT_IN_PREFIX + requireSuffix(name);
    }

    public static String imported(
            String name, short[] keyCodes) {
        return IMPORTED_PREFIX +
                sha256(name + '\n' + Arrays.toString(keyCodes));
    }

    public static String custom(String storageId) {
        String suffix = requireSuffix(storageId);
        if (suffix.startsWith(CUSTOM_PREFIX)) {
            if (!isCustom(suffix)) {
                throw new IllegalArgumentException(
                        "Custom shortcut ID suffix must not be empty");
            }
            return suffix;
        }
        return CUSTOM_PREFIX + suffix;
    }

    public static String newCustom() {
        return CUSTOM_PREFIX + UUID.randomUUID();
    }

    public static boolean isCustom(String id) {
        return id != null &&
                id.length() > CUSTOM_PREFIX.length() &&
                id.startsWith(CUSTOM_PREFIX);
    }

    public static boolean isImported(String id) {
        return id != null &&
                id.length() > IMPORTED_PREFIX.length() &&
                id.startsWith(IMPORTED_PREFIX);
    }

    private static String requireSuffix(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Shortcut ID suffix must not be empty");
        }
        return value.trim();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result =
                    new StringBuilder(digest.length * 2);
            for (byte valueByte : digest) {
                int unsignedByte = valueByte & 0xff;
                if (unsignedByte < 0x10) {
                    result.append('0');
                }
                result.append(Integer.toHexString(unsignedByte));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException error) {
            throw new AssertionError(
                    "SHA-256 is unavailable", error);
        }
    }
}
