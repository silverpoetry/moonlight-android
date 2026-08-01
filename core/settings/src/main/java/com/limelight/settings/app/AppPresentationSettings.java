package com.limelight.settings.app;

import java.util.Objects;

/**
 * Immutable application presentation snapshot.
 */
public final class AppPresentationSettings {
    private final String language;
    private final boolean smallAppIcons;
    private final boolean lightTheme;
    private final boolean backgroundEnabled;
    private final boolean backgroundBlurEnabled;
    private final String backgroundFile;
    private final String hostListLabel;

    AppPresentationSettings(
            String language,
            boolean smallAppIcons,
            boolean lightTheme,
            boolean backgroundEnabled,
            boolean backgroundBlurEnabled,
            String backgroundFile,
            String hostListLabel) {
        this.language = Objects.requireNonNull(
                language,
                "language");
        this.smallAppIcons = smallAppIcons;
        this.lightTheme = lightTheme;
        this.backgroundEnabled = backgroundEnabled;
        this.backgroundBlurEnabled = backgroundBlurEnabled;
        this.backgroundFile = Objects.requireNonNull(
                backgroundFile,
                "backgroundFile");
        this.hostListLabel = Objects.requireNonNull(
                hostListLabel,
                "hostListLabel");
    }

    public String getLanguage() {
        return language;
    }

    public boolean usesSystemLanguage() {
        return AppPresentationSettingKeys.SYSTEM_LANGUAGE.equals(
                language);
    }

    public boolean usesSmallAppIcons() {
        return smallAppIcons;
    }

    public boolean usesLightTheme() {
        return lightTheme;
    }

    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public boolean isBackgroundBlurEnabled() {
        return backgroundBlurEnabled;
    }

    public String getBackgroundFile() {
        return backgroundFile;
    }

    public String getHostListLabel() {
        return hostListLabel;
    }
}
