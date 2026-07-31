package com.limelight.settings.transfer;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Builds one validated transfer-policy snapshot from persistent storage.
 */
public final class TransferSettingsLoader {
    private TransferSettingsLoader() {
    }

    public static TransferSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        return new TransferSettings(
                repository.get(
                        TransferSettingKeys.CLIPBOARD_SYNC),
                repository.get(
                        TransferSettingKeys
                                .CLIPBOARD_FILE_DIRECTORY_URI));
    }
}
