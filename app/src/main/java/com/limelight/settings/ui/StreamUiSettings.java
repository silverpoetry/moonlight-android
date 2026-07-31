package com.limelight.settings.ui;

import java.util.Objects;

/**
 * Immutable policy for overlays and floating in-stream controls.
 */
public final class StreamUiSettings {
    public enum FloatingAction {
        GAME_MENU(0),
        SOFT_KEYBOARD(1),
        FULL_KEYBOARD(2);

        private final int storageValue;

        FloatingAction(int storageValue) {
            this.storageValue = storageValue;
        }

        public int getStorageValue() {
            return storageValue;
        }

        public static FloatingAction fromStorageValue(int value) {
            for (FloatingAction action : values()) {
                if (action.storageValue == value) {
                    return action;
                }
            }
            return GAME_MENU;
        }
    }

    private final boolean floatingControlEnabled;
    private final FloatingAction floatingAction;
    private final boolean rememberFloatingPosition;
    private final float floatingPositionX;
    private final float floatingPositionY;
    private final boolean floatingPositionNearestLeft;
    private final boolean performanceOverlayEnabled;
    private final boolean compactPerformanceOverlay;
    private final boolean compactPerformanceDetails;
    private final boolean compactPerformanceInteractive;
    private final boolean rumbleOverlayEnabled;
    private final int compactPerformanceScalePercent;
    private final int compactPerformanceMarginTopDp;
    private final boolean hideBuiltInShortcuts;
    private final boolean pictureInPictureEnabled;
    private final boolean connectionWarningsDisabled;
    private final boolean latencyToastEnabled;
    private final boolean gameModeIntegrationDisabled;

    private StreamUiSettings(Builder builder) {
        floatingControlEnabled = builder.floatingControlEnabled;
        floatingAction = Objects.requireNonNull(
                builder.floatingAction,
                "floatingAction");
        rememberFloatingPosition =
                builder.rememberFloatingPosition;
        floatingPositionX =
                StreamUiSettingKeys.FLOATING_POSITION_X
                        .normalizeValue(builder.floatingPositionX);
        floatingPositionY =
                StreamUiSettingKeys.FLOATING_POSITION_Y
                        .normalizeValue(builder.floatingPositionY);
        floatingPositionNearestLeft =
                builder.floatingPositionNearestLeft;
        performanceOverlayEnabled =
                builder.performanceOverlayEnabled;
        compactPerformanceOverlay =
                builder.compactPerformanceOverlay;
        compactPerformanceDetails =
                builder.compactPerformanceDetails;
        compactPerformanceInteractive =
                builder.compactPerformanceInteractive;
        rumbleOverlayEnabled = builder.rumbleOverlayEnabled;
        compactPerformanceScalePercent =
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_SCALE_PERCENT
                        .normalizeValue(
                                builder
                                        .compactPerformanceScalePercent);
        compactPerformanceMarginTopDp =
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_MARGIN_TOP_DP
                        .normalizeValue(
                                builder
                                        .compactPerformanceMarginTopDp);
        hideBuiltInShortcuts = builder.hideBuiltInShortcuts;
        pictureInPictureEnabled =
                builder.pictureInPictureEnabled;
        connectionWarningsDisabled =
                builder.connectionWarningsDisabled;
        latencyToastEnabled = builder.latencyToastEnabled;
        gameModeIntegrationDisabled =
                builder.gameModeIntegrationDisabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public boolean isFloatingControlEnabled() {
        return floatingControlEnabled;
    }

    public FloatingAction getFloatingAction() {
        return floatingAction;
    }

    public boolean shouldRememberFloatingPosition() {
        return rememberFloatingPosition;
    }

    public float getFloatingPositionX() {
        return floatingPositionX;
    }

    public float getFloatingPositionY() {
        return floatingPositionY;
    }

    public boolean isFloatingPositionNearestLeft() {
        return floatingPositionNearestLeft;
    }

    public boolean hasRememberedFloatingPosition() {
        return rememberFloatingPosition &&
                floatingPositionX >= 0f &&
                floatingPositionY >= 0f;
    }

    public boolean isPerformanceOverlayEnabled() {
        return performanceOverlayEnabled;
    }

    public boolean isCompactPerformanceOverlay() {
        return compactPerformanceOverlay;
    }

    public boolean areCompactPerformanceDetailsEnabled() {
        return compactPerformanceDetails;
    }

    public boolean isCompactPerformanceInteractive() {
        return compactPerformanceInteractive;
    }

    public boolean isRumbleOverlayEnabled() {
        return rumbleOverlayEnabled;
    }

    public int getCompactPerformanceScalePercent() {
        return compactPerformanceScalePercent;
    }

    public int getCompactPerformanceMarginTopDp() {
        return compactPerformanceMarginTopDp;
    }

    public boolean shouldHideBuiltInShortcuts() {
        return hideBuiltInShortcuts;
    }

    public boolean isPictureInPictureEnabled() {
        return pictureInPictureEnabled;
    }

    public boolean areConnectionWarningsDisabled() {
        return connectionWarningsDisabled;
    }

    public boolean isLatencyToastEnabled() {
        return latencyToastEnabled;
    }

    public boolean isGameModeIntegrationDisabled() {
        return gameModeIntegrationDisabled;
    }

    public static final class Builder {
        private boolean floatingControlEnabled = true;
        private FloatingAction floatingAction =
                FloatingAction.GAME_MENU;
        private boolean rememberFloatingPosition;
        private float floatingPositionX = -1f;
        private float floatingPositionY = -1f;
        private boolean floatingPositionNearestLeft = true;
        private boolean performanceOverlayEnabled;
        private boolean compactPerformanceOverlay;
        private boolean compactPerformanceDetails = true;
        private boolean compactPerformanceInteractive;
        private boolean rumbleOverlayEnabled;
        private int compactPerformanceScalePercent = 100;
        private int compactPerformanceMarginTopDp = 4;
        private boolean hideBuiltInShortcuts;
        private boolean pictureInPictureEnabled;
        private boolean connectionWarningsDisabled;
        private boolean latencyToastEnabled;
        private boolean gameModeIntegrationDisabled;

        private Builder() {
        }

        private Builder(StreamUiSettings settings) {
            floatingControlEnabled =
                    settings.floatingControlEnabled;
            floatingAction = settings.floatingAction;
            rememberFloatingPosition =
                    settings.rememberFloatingPosition;
            floatingPositionX = settings.floatingPositionX;
            floatingPositionY = settings.floatingPositionY;
            floatingPositionNearestLeft =
                    settings.floatingPositionNearestLeft;
            performanceOverlayEnabled =
                    settings.performanceOverlayEnabled;
            compactPerformanceOverlay =
                    settings.compactPerformanceOverlay;
            compactPerformanceDetails =
                    settings.compactPerformanceDetails;
            compactPerformanceInteractive =
                    settings.compactPerformanceInteractive;
            rumbleOverlayEnabled =
                    settings.rumbleOverlayEnabled;
            compactPerformanceScalePercent =
                    settings.compactPerformanceScalePercent;
            compactPerformanceMarginTopDp =
                    settings.compactPerformanceMarginTopDp;
            hideBuiltInShortcuts =
                    settings.hideBuiltInShortcuts;
            pictureInPictureEnabled =
                    settings.pictureInPictureEnabled;
            connectionWarningsDisabled =
                    settings.connectionWarningsDisabled;
            latencyToastEnabled =
                    settings.latencyToastEnabled;
            gameModeIntegrationDisabled =
                    settings.gameModeIntegrationDisabled;
        }

        public Builder setFloatingControlEnabled(boolean enabled) {
            floatingControlEnabled = enabled;
            return this;
        }

        public Builder setFloatingAction(FloatingAction action) {
            floatingAction = action;
            return this;
        }

        public Builder setRememberFloatingPosition(
                boolean remember) {
            rememberFloatingPosition = remember;
            return this;
        }

        public Builder setFloatingPosition(
                float x,
                float y,
                boolean nearestLeft) {
            floatingPositionX = x;
            floatingPositionY = y;
            floatingPositionNearestLeft = nearestLeft;
            return this;
        }

        public Builder setPerformanceOverlayEnabled(
                boolean enabled) {
            performanceOverlayEnabled = enabled;
            return this;
        }

        public Builder setCompactPerformanceOverlay(
                boolean compact) {
            compactPerformanceOverlay = compact;
            return this;
        }

        public Builder setCompactPerformanceDetails(
                boolean enabled) {
            compactPerformanceDetails = enabled;
            return this;
        }

        public Builder setCompactPerformanceInteractive(
                boolean enabled) {
            compactPerformanceInteractive = enabled;
            return this;
        }

        public Builder setRumbleOverlayEnabled(
                boolean enabled) {
            rumbleOverlayEnabled = enabled;
            return this;
        }

        public Builder setCompactPerformanceScalePercent(
                int percent) {
            compactPerformanceScalePercent = percent;
            return this;
        }

        public Builder setCompactPerformanceMarginTopDp(
                int marginTopDp) {
            compactPerformanceMarginTopDp = marginTopDp;
            return this;
        }

        public Builder setHideBuiltInShortcuts(boolean hide) {
            hideBuiltInShortcuts = hide;
            return this;
        }

        public Builder setPictureInPictureEnabled(
                boolean enabled) {
            pictureInPictureEnabled = enabled;
            return this;
        }

        public Builder setConnectionWarningsDisabled(
                boolean disabled) {
            connectionWarningsDisabled = disabled;
            return this;
        }

        public Builder setLatencyToastEnabled(boolean enabled) {
            latencyToastEnabled = enabled;
            return this;
        }

        public Builder setGameModeIntegrationDisabled(
                boolean disabled) {
            gameModeIntegrationDisabled = disabled;
            return this;
        }

        public StreamUiSettings build() {
            return new StreamUiSettings(this);
        }
    }
}
