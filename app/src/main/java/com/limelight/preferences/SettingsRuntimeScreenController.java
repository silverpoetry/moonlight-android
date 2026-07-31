package com.limelight.preferences;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;

import java.util.Objects;

/** Applies runtime-derived values to settings-screen metadata. */
final class SettingsRuntimeScreenController {
    private static final int MAX_BITRATE_KBPS = 50000;
    private static final int DEFAULT_BITRATE_KEY_STEP_KBPS = 1000;

    private final SettingsScreenModel screenModel;
    private final SettingsRuntimeText text;

    SettingsRuntimeScreenController(
            SettingsScreenModel screenModel,
            SettingsRuntimeText text) {
        this.screenModel = Objects.requireNonNull(
                screenModel,
                "screenModel");
        this.text = Objects.requireNonNull(text, "text");
    }

    void apply(SettingsRuntimeValues values) {
        Objects.requireNonNull(values, "values");
        initializeBitrate(values.getDefaultBitrateKbps());
        initializeClipboardDirectory(
                values.getClipboardDirectoryLabel());
    }

    private void initializeBitrate(int defaultBitrateKbps) {
        SettingsItem bitrate = screenModel.findItem(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());
        if (bitrate != null) {
            bitrate.displayDefaultInteger = defaultBitrateKbps;
            bitrate.max = MAX_BITRATE_KBPS;
            if (bitrate.keyStep <= 0) {
                bitrate.keyStep = DEFAULT_BITRATE_KEY_STEP_KBPS;
            }
        }

        SettingsItem customBitrateEditor = screenModel.findItem(
                SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS);
        if (customBitrateEditor != null) {
            customBitrateEditor.displayDefaultInteger =
                    defaultBitrateKbps;
        }
    }

    private void initializeClipboardDirectory(String directoryLabel) {
        if (directoryLabel == null) {
            return;
        }
        SettingsItem item = screenModel.findItem(
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI
                        .getName());
        if (item != null) {
            item.summary = text.clipboardDirectorySummary(
                    directoryLabel);
        }
    }
}
