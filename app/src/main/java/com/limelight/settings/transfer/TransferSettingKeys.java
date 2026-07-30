package com.limelight.settings.transfer;

import com.limelight.settings.SettingKey;

/**
 * Persisted clipboard and transfer settings.
 */
public final class TransferSettingKeys {
    public static final SettingKey<Boolean> CLIPBOARD_SYNC =
            SettingKey.booleanKey(
                    "checkbox_clipboard_sync",
                    false);

    public static final SettingKey<Boolean>
            LEGACY_CLIPBOARD_IMAGE_SYNC =
            SettingKey.booleanKey(
                    "checkbox_clipboard_image_sync",
                    false);

    private TransferSettingKeys() {
    }
}
