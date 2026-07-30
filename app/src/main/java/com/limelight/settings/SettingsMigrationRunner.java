package com.limelight.settings;

import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.stream.StreamDecoderSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;

import java.util.Objects;

/**
 * Runs ordered, idempotent migrations for default application preferences.
 */
public final class SettingsMigrationRunner {
    private SettingsMigrationRunner() {
    }

    public static void migrate(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        int storedVersion = repository.get(SettingsSchema.VERSION);
        boolean hasLateLegacyValues =
                containsLegacyValues(repository);
        if (storedVersion >= SettingsSchema.CURRENT_VERSION &&
                !hasLateLegacyValues) {
            return;
        }

        SettingsRepository.Editor editor = repository.edit();
        if (storedVersion < 1 || hasLateLegacyValues) {
            migrateToVersion1(repository, editor);
        }
        if (storedVersion < SettingsSchema.CURRENT_VERSION) {
            editor.put(
                    SettingsSchema.VERSION,
                    SettingsSchema.CURRENT_VERSION);
        }
        editor.commit();
    }

    private static boolean containsLegacyValues(
            SettingsRepository repository) {
        return repository.contains(
                StreamAudioSettingKeys
                        .LEGACY_ENABLE_51_SURROUND) ||
                repository.contains(
                        StreamDecoderSettingKeys
                                .LEGACY_DISABLE_FRAME_DROP) ||
                repository.contains(
                        TransferSettingKeys
                                .LEGACY_CLIPBOARD_IMAGE_SYNC);
    }

    private static void migrateToVersion1(
            SettingsRepository repository,
            SettingsRepository.Editor editor) {
        if (repository.contains(
                StreamAudioSettingKeys
                        .LEGACY_ENABLE_51_SURROUND)) {
            if (repository.get(
                    StreamAudioSettingKeys
                            .LEGACY_ENABLE_51_SURROUND)) {
                editor.put(
                        StreamAudioSettingKeys
                                .CHANNEL_CONFIGURATION,
                        "51");
            }
            editor.remove(
                    StreamAudioSettingKeys
                            .LEGACY_ENABLE_51_SURROUND);
        }

        if (repository.contains(
                StreamDecoderSettingKeys
                        .LEGACY_DISABLE_FRAME_DROP)) {
            boolean neverDropFrames = repository.get(
                    StreamDecoderSettingKeys
                            .LEGACY_DISABLE_FRAME_DROP);
            editor.put(
                    StreamDecoderSettingKeys.FRAME_PACING,
                    neverDropFrames
                            ? StreamDecoderSettingKeys
                                    .FRAME_PACING_BALANCED
                            : StreamDecoderSettingKeys
                                    .FRAME_PACING_MINIMUM_LATENCY);
            editor.remove(
                    StreamDecoderSettingKeys
                            .LEGACY_DISABLE_FRAME_DROP);
        }

        if (repository.contains(
                TransferSettingKeys
                        .LEGACY_CLIPBOARD_IMAGE_SYNC)) {
            boolean clipboardSyncEnabled =
                    repository.get(
                            TransferSettingKeys.CLIPBOARD_SYNC) ||
                            repository.get(
                                    TransferSettingKeys
                                            .LEGACY_CLIPBOARD_IMAGE_SYNC);
            editor.put(
                    TransferSettingKeys.CLIPBOARD_SYNC,
                    clipboardSyncEnabled);
            editor.remove(
                    TransferSettingKeys
                            .LEGACY_CLIPBOARD_IMAGE_SYNC);
        }
    }
}
