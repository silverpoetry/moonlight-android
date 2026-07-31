package com.limelight.preferences;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.limelight.LimeLog;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidStreamDefaults;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;

/** Android adapter for runtime-derived settings-screen values. */
final class AndroidSettingsRuntimeValues {
    private AndroidSettingsRuntimeValues() {
    }

    static SettingsRuntimeValues collect(
            Context context,
            SettingsRepository repository) {
        return new SettingsRuntimeValues(
                AndroidStreamDefaults.getDefaultBitrateKbps(context),
                clipboardDirectoryLabel(context, repository));
    }

    private static String clipboardDirectoryLabel(
            Context context,
            SettingsRepository repository) {
        TransferSettings transfer =
                TransferSettingsLoader.load(repository);
        if (!transfer.hasClipboardFileDirectory()) {
            return null;
        }
        String value = transfer.getClipboardFileDirectoryUri();
        try {
            DocumentFile directory = DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(value));
            String displayName = directory == null
                    ? null
                    : directory.getName();
            return displayName == null || displayName.isEmpty()
                    ? value
                    : displayName;
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Unable to resolve clipboard directory label: " +
                            error.getMessage());
            return value;
        }
    }
}
