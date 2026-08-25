package com.limelight.settings.app;

import java.util.Objects;

/**
 * Immutable application presentation snapshot.
 */
public final class AppPresentationSettings {
    private final String language;
    private final boolean smallAppIcons;
    private final String themeMode;
    AppPresentationSettings(
            String language,
            boolean smallAppIcons,
            String themeMode) {
        this.language = Objects.requireNonNull(
                language,
                "language");
        this.smallAppIcons = smallAppIcons;
        this.themeMode = Objects.requireNonNull(
                themeMode,
                "themeMode");
    }

    public String getLanguage() {
        return language;
    }

    public boolean usesSmallAppIcons() {
        return smallAppIcons;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public boolean followsSystemTheme() {
        return AppPresentationSettingKeys.THEME_MODE_SYSTEM.equals(
                themeMode);
    }

    public boolean usesLightTheme() {
        return AppPresentationSettingKeys.THEME_MODE_LIGHT.equals(
                themeMode);
    }

    public boolean usesDarkTheme() {
        return AppPresentationSettingKeys.THEME_MODE_DARK.equals(
                themeMode);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppPresentationSettings)) {
            return false;
        }
        AppPresentationSettings settings =
                (AppPresentationSettings) other;
        return smallAppIcons == settings.smallAppIcons &&
                themeMode.equals(settings.themeMode) &&
                language.equals(settings.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                language,
                smallAppIcons,
                themeMode);
    }
}
