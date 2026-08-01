package com.limelight.settings.ui;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * One type-safe in-stream UI settings intent.
 */
public final class StreamUiSettingsUpdate {
    private interface Applier {
        StreamUiSettings apply(StreamUiSettings settings);
    }

    private interface Persister {
        void persist(SettingsRepository.Editor editor);
    }

    private final Applier applier;
    private final Persister persister;

    private StreamUiSettingsUpdate(
            Applier applier,
            Persister persister) {
        this.applier = Objects.requireNonNull(applier, "applier");
        this.persister =
                Objects.requireNonNull(persister, "persister");
    }

    public static StreamUiSettingsUpdate floatingControlEnabled(
            boolean enabled) {
        return single(
                StreamUiSettingKeys.FLOATING_CONTROL_ENABLED,
                enabled,
                StreamUiSettings.Builder
                        ::setFloatingControlEnabled);
    }

    public static StreamUiSettingsUpdate floatingAction(
            StreamUiSettings.FloatingAction action) {
        StreamUiSettings.FloatingAction normalized =
                Objects.requireNonNull(action, "action");
        int value = StreamUiSettingKeys.FLOATING_ACTION
                .normalizeValue(normalized.getStorageValue());
        return new StreamUiSettingsUpdate(
                settings -> settings.toBuilder()
                        .setFloatingAction(
                                StreamUiSettings.FloatingAction
                                        .fromStorageValue(value))
                        .build(),
                editor -> editor.put(
                        StreamUiSettingKeys.FLOATING_ACTION,
                        value));
    }

    public static StreamUiSettingsUpdate rememberFloatingPosition(
            boolean remember) {
        return new StreamUiSettingsUpdate(
                settings -> settings.toBuilder()
                        .setRememberFloatingPosition(remember)
                        .setFloatingPosition(
                                remember
                                        ? settings
                                                .getFloatingPositionX()
                                        : -1f,
                                remember
                                        ? settings
                                                .getFloatingPositionY()
                                        : -1f,
                                settings
                                        .isFloatingPositionNearestLeft())
                        .build(),
                editor -> {
                    editor.put(
                            StreamUiSettingKeys
                                    .REMEMBER_FLOATING_POSITION,
                            remember);
                    if (!remember) {
                        editor.put(
                                        StreamUiSettingKeys
                                                .FLOATING_POSITION_X,
                                        -1f)
                                .put(
                                        StreamUiSettingKeys
                                                .FLOATING_POSITION_Y,
                                        -1f);
                    }
                });
    }

    public static StreamUiSettingsUpdate floatingPosition(
            float x,
            float y,
            boolean nearestLeft) {
        float normalizedX =
                StreamUiSettingKeys.FLOATING_POSITION_X
                        .normalizeValue(x);
        float normalizedY =
                StreamUiSettingKeys.FLOATING_POSITION_Y
                        .normalizeValue(y);
        return new StreamUiSettingsUpdate(
                settings -> settings.toBuilder()
                        .setFloatingPosition(
                                normalizedX,
                                normalizedY,
                                nearestLeft)
                        .build(),
                editor -> editor
                        .put(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_X,
                                normalizedX)
                        .put(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_Y,
                                normalizedY)
                        .put(
                                StreamUiSettingKeys
                                        .FLOATING_POSITION_NEAREST_LEFT,
                                nearestLeft));
    }

    public static StreamUiSettingsUpdate
            compactPerformanceDetails(boolean enabled) {
        return single(
                StreamUiSettingKeys.COMPACT_PERFORMANCE_DETAILS,
                enabled,
                StreamUiSettings.Builder
                        ::setCompactPerformanceDetails);
    }

    public static StreamUiSettingsUpdate
            compactPerformanceInteractive(boolean enabled) {
        return single(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_INTERACTIVE,
                enabled,
                StreamUiSettings.Builder
                        ::setCompactPerformanceInteractive);
    }

    public static StreamUiSettingsUpdate rumbleOverlayEnabled(
            boolean enabled) {
        return single(
                StreamUiSettingKeys.RUMBLE_OVERLAY_ENABLED,
                enabled,
                StreamUiSettings.Builder
                        ::setRumbleOverlayEnabled);
    }

    public static StreamUiSettingsUpdate
            compactPerformanceScalePercent(int percent) {
        return single(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_SCALE_PERCENT,
                percent,
                StreamUiSettings.Builder
                        ::setCompactPerformanceScalePercent);
    }

    public static StreamUiSettingsUpdate
            compactPerformanceMarginTopDp(int marginTopDp) {
        return single(
                StreamUiSettingKeys
                        .COMPACT_PERFORMANCE_MARGIN_TOP_DP,
                marginTopDp,
                StreamUiSettings.Builder
                        ::setCompactPerformanceMarginTopDp);
    }

    public static StreamUiSettingsUpdate hideBuiltInShortcuts(
            boolean hide) {
        return single(
                StreamUiSettingKeys.HIDE_BUILT_IN_SHORTCUTS,
                hide,
                StreamUiSettings.Builder
                        ::setHideBuiltInShortcuts);
    }

    public StreamUiSettings applyTo(StreamUiSettings settings) {
        return applier.apply(
                Objects.requireNonNull(settings, "settings"));
    }

    public void persist(SettingsRepository repository) {
        SettingsRepository.Editor editor =
                Objects.requireNonNull(repository, "repository")
                        .edit();
        persister.persist(editor);
        editor.apply();
    }

    private interface ValueApplier<T> {
        StreamUiSettings.Builder apply(
                StreamUiSettings.Builder builder,
                T value);
    }

    private static <T> StreamUiSettingsUpdate single(
            SettingKey<T> key,
            T value,
            ValueApplier<T> applier) {
        T normalized = key.normalizeValue(value);
        return new StreamUiSettingsUpdate(
                settings -> applier.apply(
                                settings.toBuilder(),
                                normalized)
                        .build(),
                editor -> editor.put(key, normalized));
    }
}
