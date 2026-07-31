package com.limelight.preferences;

import android.content.Context;

import com.limelight.R;

import java.util.Objects;

/** Android resource adapter for runtime-derived settings text. */
final class AndroidSettingsRuntimeText implements SettingsRuntimeText {
    private final Context context;

    AndroidSettingsRuntimeText(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public CharSequence clipboardDirectorySummary(
            String directoryLabel) {
        return context.getString(
                R.string.clipboard_file_save_directory_selected,
                directoryLabel);
    }
}
