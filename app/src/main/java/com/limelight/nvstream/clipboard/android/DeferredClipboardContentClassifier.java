package com.limelight.nvstream.clipboard.android;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;

/** Identifies clipboard entries whose bytes are supplied only on explicit paste. */
public final class DeferredClipboardContentClassifier {
    private static final String XIAOMI_CONTINUITY_LABEL = "universalClipData";

    private DeferredClipboardContentClassifier() {
    }

    public static boolean isRemoteDeferred(
            ClipDescription description,
            Uri uri) {
        if (description == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PersistableBundle extras = description.getExtras();
            if (extras != null && extras.getBoolean(
                    ClipDescription.EXTRA_IS_REMOTE_DEVICE,
                    false)) {
                return true;
            }
        }

        CharSequence label = description.getLabel();
        return label != null && XIAOMI_CONTINUITY_LABEL.contentEquals(label);
    }
}
