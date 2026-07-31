package com.limelight.preferences;

/** Immutable runtime-derived values used to initialize settings metadata. */
final class SettingsRuntimeValues {
    private final int defaultBitrateKbps;
    private final String clipboardDirectoryLabel;

    SettingsRuntimeValues(
            int defaultBitrateKbps,
            String clipboardDirectoryLabel) {
        if (defaultBitrateKbps <= 0) {
            throw new IllegalArgumentException(
                    "Default bitrate must be positive");
        }
        this.defaultBitrateKbps = defaultBitrateKbps;
        this.clipboardDirectoryLabel = clipboardDirectoryLabel;
    }

    int getDefaultBitrateKbps() {
        return defaultBitrateKbps;
    }

    String getClipboardDirectoryLabel() {
        return clipboardDirectoryLabel;
    }
}
