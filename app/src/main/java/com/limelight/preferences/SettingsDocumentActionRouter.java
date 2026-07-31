package com.limelight.preferences;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.transfer.TransferSettingKeys;

/** Pure mapping from stable settings row IDs to document actions. */
final class SettingsDocumentActionRouter {
    private SettingsDocumentActionRouter() {
    }

    static SettingsDocumentAction resolve(String key) {
        if (SettingsScreenIds.ACTION_VIRTUAL_KEYBOARD_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_VIRTUAL_KEYBOARD;
        }
        if (SettingsScreenIds.ACTION_VIRTUAL_GAMEPAD_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_VIRTUAL_GAMEPAD;
        }
        if (SettingsScreenIds.ACTION_BACKUP_HOSTS_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_HOSTS;
        }
        if (SettingsScreenIds.ACTION_BACKUP_CERTIFICATE_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_CERTIFICATE;
        }
        if (SettingsScreenIds.ACTION_BACKUP_PRIVATE_KEY_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_PRIVATE_KEY;
        }
        if (SettingsScreenIds.ACTION_ACCESSIBILITY_CONFIG_IMPORT
                .equals(key)) {
            return SettingsDocumentAction
                    .IMPORT_ACCESSIBILITY_CONFIGURATION;
        }
        if (SettingsScreenIds.ACTION_APP_BACKGROUND_SELECT
                .equals(key)) {
            return SettingsDocumentAction.SELECT_BACKGROUND;
        }
        if (TransferSettingKeys.CLIPBOARD_FILE_DIRECTORY_URI
                .getName()
                .equals(key)) {
            return SettingsDocumentAction.SELECT_CLIPBOARD_DIRECTORY;
        }
        if (SettingsScreenIds.ACTION_VIRTUAL_KEYBOARD_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_VIRTUAL_KEYBOARD;
        }
        if (SettingsScreenIds.ACTION_VIRTUAL_GAMEPAD_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_VIRTUAL_GAMEPAD;
        }
        if (SettingsScreenIds.ACTION_BACKUP_HOSTS_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_HOSTS;
        }
        if (SettingsScreenIds.ACTION_BACKUP_CERTIFICATE_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_CERTIFICATE;
        }
        if (SettingsScreenIds.ACTION_BACKUP_PRIVATE_KEY_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_PRIVATE_KEY;
        }
        return null;
    }
}
