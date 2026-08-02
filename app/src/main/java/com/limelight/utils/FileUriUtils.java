package com.limelight.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** URI permission operations shared by document-tree result adapters. */
public final class FileUriUtils {
    private FileUriUtils() {
    }

    /**
     * Persists exactly the URI grant modes returned by a document picker.
     *
     * @return {@code false} when the provider returned no read or write grant
     */
    public static boolean persistUriPermission(
            Context context,
            Intent resultData,
            Uri uri) {
        int resultFlags = resultData.getFlags();
        boolean hasReadPermission =
                (resultFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
        boolean hasWritePermission =
                (resultFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;

        if (hasReadPermission && hasWritePermission) {
            context.getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        else if (hasReadPermission) {
            context.getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else if (hasWritePermission) {
            context.getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        else {
            return false;
        }
        return true;
    }
}
