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
                SettingsScreenIds.ACTION_CONFIGURATION_IMPORT,
                SettingsDocumentAction.IMPORT_CONFIGURATION);
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
                SettingsScreenIds.ACTION_CONFIGURATION_EXPORT,
                SettingsDocumentAction.EXPORT_CONFIGURATION);
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
