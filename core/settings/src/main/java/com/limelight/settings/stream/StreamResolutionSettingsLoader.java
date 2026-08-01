package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;

import java.util.Objects;

/**
 * Loads and atomically repairs the resolution settings aggregate.
 */
public final class StreamResolutionSettingsLoader {
    private StreamResolutionSettingsLoader() {
    }

    public static StreamResolutionCodec.Result load(
            SettingsRepository repository,
            StreamResolutionCodec.DisplayAspect displayAspect) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(displayAspect, "displayAspect");

        LegacyResolution legacy = readLegacyResolution(repository);
        boolean selectionMissing = !repository.contains(
                StreamResolutionSettingKeys.SELECTION);
        StreamResolutionCodec.Result result;
        if (legacy != null) {
            result = StreamResolutionCodec.decode(
                    legacy.resolution,
                    StreamResolutionCodec.SELECTION_PRESET,
                    StreamResolutionCodec.ASPECT_RATIO_16_9,
                    legacy.fps,
                    displayAspect);
        }
        else {
            result = StreamResolutionCodec.decode(
                    repository.get(
                            StreamResolutionSettingKeys.RESOLUTION),
                    readSelection(repository),
                    repository.get(
                            StreamResolutionSettingKeys.ASPECT_RATIO),
                    repository.get(StreamResolutionSettingKeys.FPS),
                    displayAspect);
        }

        if (legacy != null ||
                selectionMissing ||
                result.isRepairRequired()) {
            SettingsRepository.Editor editor = repository.edit()
                    .remove(
                            StreamResolutionSettingKeys
                                    .LEGACY_RESOLUTION_AND_FPS)
                    .put(
                            StreamResolutionSettingKeys.RESOLUTION,
                            result.getCanonicalResolution())
                    .put(
                            StreamResolutionSettingKeys.SELECTION,
                            result.getCanonicalSelection())
                    .put(
                            StreamResolutionSettingKeys.ASPECT_RATIO,
                            result.getCanonicalAspectRatio())
                    .put(
                            StreamResolutionSettingKeys.FPS,
                            result.getCanonicalFps());
            editor.commit();
        }

        return result;
    }

    private static String readSelection(
            SettingsRepository repository) {
        if (repository.contains(
                StreamResolutionSettingKeys.SELECTION)) {
            return repository.get(
                    StreamResolutionSettingKeys.SELECTION);
        }

        String resolution = repository.get(
                StreamResolutionSettingKeys.RESOLUTION);
        return StreamResolutionCodec.isStandardResolutionPreset(
                resolution)
                ? StreamResolutionCodec.SELECTION_PRESET
                : StreamResolutionCodec.SELECTION_CUSTOM_OR_NATIVE;
    }

    private static LegacyResolution readLegacyResolution(
            SettingsRepository repository) {
        if (!repository.contains(
                StreamResolutionSettingKeys
                        .LEGACY_RESOLUTION_AND_FPS)) {
            return null;
        }

        switch (repository.get(
                StreamResolutionSettingKeys
                        .LEGACY_RESOLUTION_AND_FPS)) {
            case "360p30":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_360P,
                        "30");
            case "360p60":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_360P,
                        "60");
            case "720p30":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_720P,
                        "30");
            case "720p60":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_720P,
                        "60");
            case "1080p30":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_1080P,
                        "30");
            case "1080p60":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_1080P,
                        "60");
            case "4K30":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_4K,
                        "30");
            case "4K60":
                return new LegacyResolution(
                        StreamResolutionCodec.RESOLUTION_4K,
                        "60");
            default:
                return new LegacyResolution(
                        StreamResolutionCodec.DEFAULT_RESOLUTION,
                        StreamResolutionCodec.DEFAULT_FPS);
        }
    }

    private static final class LegacyResolution {
        final String resolution;
        final String fps;

        LegacyResolution(String resolution, String fps) {
            this.resolution = resolution;
            this.fps = fps;
        }
    }
}
