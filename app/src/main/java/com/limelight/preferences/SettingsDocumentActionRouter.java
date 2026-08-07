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
        if (SettingsScreenIds.ACTION_CONFIGURATION_IMPORT
                .equals(key)) {
            return SettingsDocumentAction.IMPORT_CONFIGURATION;
        }
        if (SettingsScreenIds.ACTION_ACCESSIBILITY_CONFIG_IMPORT
                .equals(key)) {
            return SettingsDocumentAction
                    .IMPORT_ACCESSIBILITY_CONFIGURATION;
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
        if (SettingsScreenIds.ACTION_CONFIGURATION_EXPORT
                .equals(key)) {
            return SettingsDocumentAction.EXPORT_CONFIGURATION;
        }
        return null;
    }
}
