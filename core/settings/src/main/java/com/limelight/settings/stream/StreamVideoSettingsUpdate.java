package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamVideoSettings.ScreenOnPolicy;
import com.limelight.settings.stream.StreamVideoSettings.VirtualDisplayMode;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One type-safe stream-video settings intent.
 */
public final class StreamVideoSettingsUpdate {
    private interface Applier {
        StreamVideoSettings apply(StreamVideoSettings settings);
    }

    private interface Persister {
        void persist(
                SettingsRepository repository,
                SettingsRepository.Editor editor);
    }

    private final Applier applier;
    private final Persister persister;

    private StreamVideoSettingsUpdate(
            Applier applier,
            Persister persister) {
        this.applier = Objects.requireNonNull(applier, "applier");
        this.persister =
                Objects.requireNonNull(persister, "persister");
    }

    public static StreamVideoSettingsUpdate videoFormat(
            VideoFormat format) {
        VideoFormat normalized =
                Objects.requireNonNull(format, "format");
        String stored = StreamVideoSettingsCodec
                .encodeVideoFormat(normalized);
        return new StreamVideoSettingsUpdate(
                settings -> settings.toBuilder()
                        .setVideoFormat(normalized)
                        .build(),
                (repository, editor) -> editor.put(
                        StreamVideoSettingKeys.VIDEO_FORMAT,
                        stored));
    }

    public static StreamVideoSettingsUpdate hdrEnabled(
            boolean enabled) {
        return single(
                StreamVideoSettingKeys.HDR_ENABLED,
                enabled,
                StreamVideoSettings.Builder::setHdrEnabled);
    }

    public static StreamVideoSettingsUpdate
            hdrHighBrightness(boolean enabled) {
        return single(
                StreamVideoSettingKeys.HDR_HIGH_BRIGHTNESS,
                enabled,
                StreamVideoSettings.Builder
                        ::setHdrHighBrightness);
    }

    public static StreamVideoSettingsUpdate
            ignoreHdrCapability(boolean ignore) {
        return single(
                StreamVideoSettingKeys.IGNORE_HDR_CAPABILITY,
                ignore,
                StreamVideoSettings.Builder
                        ::setIgnoreHdrCapability);
    }

    public static StreamVideoSettingsUpdate
            lowLatencyExperimentEnabled(boolean enabled) {
        return single(
                StreamVideoSettingKeys
                        .LOW_LATENCY_EXPERIMENT,
                enabled,
                StreamVideoSettings.Builder
                        ::setLowLatencyExperimentEnabled);
    }

    public static StreamVideoSettingsUpdate
            enforceDisplayMode(boolean enforce) {
        return single(
                StreamVideoSettingKeys.ENFORCE_DISPLAY_MODE,
                enforce,
                StreamVideoSettings.Builder
                        ::setEnforceDisplayMode);
    }

    public static StreamVideoSettingsUpdate screenOnPolicy(
            ScreenOnPolicy policy) {
        ScreenOnPolicy normalized =
                Objects.requireNonNull(policy, "policy");
        int stored = StreamVideoSettingKeys.SCREEN_ON_POLICY
                .normalizeValue(normalized.getStorageValue());
        return new StreamVideoSettingsUpdate(
                settings -> settings.toBuilder()
                        .setScreenOnPolicy(
                                ScreenOnPolicy
                                        .fromStorageValue(stored))
                        .build(),
                (repository, editor) -> editor.put(
                        StreamVideoSettingKeys.SCREEN_ON_POLICY,
                        stored));
    }

    public static StreamVideoSettingsUpdate virtualDisplayMode(
            VirtualDisplayMode mode) {
        VirtualDisplayMode normalized =
                Objects.requireNonNull(mode, "mode");
        int stored = StreamVideoSettingKeys
                .VIRTUAL_DISPLAY_MODE
                .normalizeValue(normalized.getStorageValue());
        return new StreamVideoSettingsUpdate(
                settings -> settings.toBuilder()
                        .setVirtualDisplayMode(
                                VirtualDisplayMode
                                        .fromStorageValue(stored))
                        .build(),
                (repository, editor) -> editor.put(
                        StreamVideoSettingKeys
                                .VIRTUAL_DISPLAY_MODE,
                        stored));
    }

    /**
     * Persists the fields represented by the display configuration's explicit
     * Apply action as one transaction. Immediate radio settings are
     * intentionally not overwritten by a stale draft.
     */
    public static StreamVideoSettingsUpdate displayConfiguration(
            StreamVideoSettings draft) {
        StreamVideoSettings normalized =
                Objects.requireNonNull(draft, "draft");
        String resolution = normalized.getWidth() +
                "x" + normalized.getHeight();
        String fps = Integer.toString(normalized.getFps());
        return new StreamVideoSettingsUpdate(
                settings -> settings.toBuilder()
                        .setDimensions(
                                normalized.getWidth(),
                                normalized.getHeight())
                        .setFps(normalized.getFps())
                        .setBitrateKbps(
                                normalized.getBitrateKbps())
                        .setPortrait(normalized.isPortrait())
                        .setExternalDisplay(
                                normalized.isExternalDisplay())
                        .build(),
                (repository, editor) -> {
                    editor
                        .put(
                                StreamResolutionSettingKeys
                                        .RESOLUTION,
                                resolution)
                        .put(
                                StreamResolutionSettingKeys
                                        .SELECTION,
                                StreamResolutionCodec
                                        .SELECTION_CUSTOM_OR_NATIVE)
                        .put(
                                StreamResolutionSettingKeys.FPS,
                                fps)
                        .put(
                                StreamVideoSettingKeys.BITRATE_KBPS,
                                normalized.getBitrateKbps())
                        .put(
                                StreamVideoSettingKeys
                                        .EXTERNAL_DISPLAY,
                                normalized.isExternalDisplay())
                        .put(
                                StreamVideoSettingKeys.PORTRAIT,
                                normalized.isPortrait());
                    addCustomResolutionIfRequired(
                            repository,
                            editor,
                            resolution);
                });
    }

    public StreamVideoSettings applyTo(
            StreamVideoSettings settings) {
        return applier.apply(
                Objects.requireNonNull(settings, "settings"));
    }

    public void persist(SettingsRepository repository) {
        SettingsRepository.Editor editor =
                Objects.requireNonNull(repository, "repository")
                        .edit();
        persister.persist(repository, editor);
        editor.apply();
    }

    private interface ValueApplier<T> {
        StreamVideoSettings.Builder apply(
                StreamVideoSettings.Builder builder,
                T value);
    }

    private static <T> StreamVideoSettingsUpdate single(
            SettingKey<T> key,
            T value,
            ValueApplier<T> applier) {
        T normalized = key.normalizeValue(value);
        return new StreamVideoSettingsUpdate(
                settings -> applier.apply(
                                settings.toBuilder(),
                                normalized)
                        .build(),
                (repository, editor) -> editor.put(key, normalized));
    }

    private static void addCustomResolutionIfRequired(
            SettingsRepository repository,
            SettingsRepository.Editor editor,
            String value) {
        if (StreamResolutionCodec.isStandardResolutionPreset(value) ||
                CustomResolution.parse(value) == null) {
            return;
        }
        Set<String> customResolutions = new LinkedHashSet<>(
                repository.get(
                        StreamResolutionSettingKeys.CUSTOM_RESOLUTIONS));
        if (customResolutions.size() >=
                StreamResolutionSettingKeys.MAX_CUSTOM_RESOLUTIONS &&
                !customResolutions.contains(value)) {
            return;
        }
        customResolutions.add(value);
        editor.put(
                StreamResolutionSettingKeys.CUSTOM_RESOLUTIONS,
                customResolutions);
    }
}
