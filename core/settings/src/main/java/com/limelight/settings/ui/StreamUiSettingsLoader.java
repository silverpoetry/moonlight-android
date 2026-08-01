package com.limelight.settings.ui;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Builds one coherent in-stream UI settings snapshot.
 */
public final class StreamUiSettingsLoader {
    private StreamUiSettingsLoader() {
    }

    public static StreamUiSettings load(
            SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");
        return StreamUiSettings.builder()
                .setFloatingControlEnabled(repository.get(
                        StreamUiSettingKeys
                                .FLOATING_CONTROL_ENABLED))
                .setFloatingAction(
                        StreamUiSettings.FloatingAction
                                .fromStorageValue(repository.get(
                                        StreamUiSettingKeys
                                                .FLOATING_ACTION)))
                .setRememberFloatingPosition(repository.get(
                        StreamUiSettingKeys
                                .REMEMBER_FLOATING_POSITION))
                .setFloatingPosition(
                        repository.get(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_X),
                        repository.get(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_Y),
                        repository.get(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_NEAREST_LEFT))
                .setPerformanceOverlayEnabled(repository.get(
                        StreamUiSettingKeys
                                .PERFORMANCE_OVERLAY_ENABLED))
                .setCompactPerformanceOverlay(repository.get(
                        StreamUiSettingKeys
                                .COMPACT_PERFORMANCE_OVERLAY))
                .setCompactPerformanceDetails(repository.get(
                        StreamUiSettingKeys
                                .COMPACT_PERFORMANCE_DETAILS))
                .setCompactPerformanceInteractive(repository.get(
                        StreamUiSettingKeys
                                .COMPACT_PERFORMANCE_INTERACTIVE))
                .setRumbleOverlayEnabled(repository.get(
                        StreamUiSettingKeys
                                .RUMBLE_OVERLAY_ENABLED))
                .setCompactPerformanceScalePercent(repository.get(
                        StreamUiSettingKeys
                                .COMPACT_PERFORMANCE_SCALE_PERCENT))
                .setCompactPerformanceMarginTopDp(repository.get(
                        StreamUiSettingKeys
                                .COMPACT_PERFORMANCE_MARGIN_TOP_DP))
                .setHideBuiltInShortcuts(repository.get(
                        StreamUiSettingKeys
                                .HIDE_BUILT_IN_SHORTCUTS))
                .setPictureInPictureEnabled(repository.get(
                        StreamUiSettingKeys.PICTURE_IN_PICTURE))
                .setConnectionWarningsDisabled(repository.get(
                        StreamUiSettingKeys
                                .CONNECTION_WARNINGS_DISABLED))
                .setLatencyToastEnabled(repository.get(
                        StreamUiSettingKeys.LATENCY_TOAST))
                .setGameModeIntegrationDisabled(repository.get(
                        StreamUiSettingKeys
                                .GAME_MODE_INTEGRATION_DISABLED))
                .build();
    }
}
