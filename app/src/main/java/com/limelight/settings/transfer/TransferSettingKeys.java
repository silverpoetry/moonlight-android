package com.limelight.settings.transfer;

import com.limelight.settings.SettingKey;

/**
 * Persisted clipboard and transfer settings.
 */
public final class TransferSettingKeys {
    public static final int MAX_DIRECTORY_URI_LENGTH = 8_192;
    public static final SettingKey<Boolean> CLIPBOARD_SYNC =
            SettingKey.booleanKey(
                    "checkbox_clipboard_sync",
                    false);
    public static final SettingKey<String>
            CLIPBOARD_FILE_DIRECTORY_URI =
            SettingKey.boundedStringKey(
                    "clipboard_file_save_directory",
                    "",
                    MAX_DIRECTORY_URI_LENGTH);

    public static final SettingKey<Boolean>
            LEGACY_CLIPBOARD_IMAGE_SYNC =
            SettingKey.booleanKey(
                    "checkbox_clipboard_image_sync",
                    false);

    private TransferSettingKeys() {
    }
}
