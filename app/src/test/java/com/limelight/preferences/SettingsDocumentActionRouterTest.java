package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.transfer.TransferSettingKeys;

import org.junit.Test;

public final class SettingsDocumentActionRouterTest {
    @Test
    public void stableRowIdsResolveToOneAction() {
        assertRoute(
                SettingsScreenIds.ACTION_VIRTUAL_KEYBOARD_IMPORT,
                SettingsDocumentAction.IMPORT_VIRTUAL_KEYBOARD);
        assertRoute(
                SettingsScreenIds.ACTION_VIRTUAL_GAMEPAD_IMPORT,
                SettingsDocumentAction.IMPORT_VIRTUAL_GAMEPAD);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_HOSTS_IMPORT,
                SettingsDocumentAction.IMPORT_HOSTS);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_CERTIFICATE_IMPORT,
                SettingsDocumentAction.IMPORT_CERTIFICATE);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_PRIVATE_KEY_IMPORT,
                SettingsDocumentAction.IMPORT_PRIVATE_KEY);
        assertRoute(
                SettingsScreenIds.ACTION_ACCESSIBILITY_CONFIG_IMPORT,
                SettingsDocumentAction
                        .IMPORT_ACCESSIBILITY_CONFIGURATION);
        assertRoute(
                TransferSettingKeys.CLIPBOARD_FILE_DIRECTORY_URI
                        .getName(),
                SettingsDocumentAction.SELECT_CLIPBOARD_DIRECTORY);
        assertRoute(
                SettingsScreenIds.ACTION_VIRTUAL_KEYBOARD_EXPORT,
                SettingsDocumentAction.EXPORT_VIRTUAL_KEYBOARD);
        assertRoute(
                SettingsScreenIds.ACTION_VIRTUAL_GAMEPAD_EXPORT,
                SettingsDocumentAction.EXPORT_VIRTUAL_GAMEPAD);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_HOSTS_EXPORT,
                SettingsDocumentAction.EXPORT_HOSTS);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_CERTIFICATE_EXPORT,
                SettingsDocumentAction.EXPORT_CERTIFICATE);
        assertRoute(
                SettingsScreenIds.ACTION_BACKUP_PRIVATE_KEY_EXPORT,
                SettingsDocumentAction.EXPORT_PRIVATE_KEY);
    }

    @Test
    public void unrelatedOrMissingIdIsNotClaimed() {
        assertNull(SettingsDocumentActionRouter.resolve(null));
        assertNull(SettingsDocumentActionRouter.resolve("unrelated"));
    }

    private static void assertRoute(
            String key,
            SettingsDocumentAction expected) {
        assertEquals(
                expected,
                SettingsDocumentActionRouter.resolve(key));
    }
}
