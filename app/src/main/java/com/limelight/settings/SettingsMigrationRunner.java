package com.limelight.settings;

import com.limelight.settings.audio.StreamAudioSettingKeys;
import com.limelight.settings.stream.StreamDecoderSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.ui.GameMenuCardIds;
import com.limelight.settings.ui.GameMenuCardLayout;
import com.limelight.settings.ui.GameMenuCardLayoutCodec;
import com.limelight.settings.ui.GameMenuCardSettingKeys;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        if (storedVersion < 2) {
            migrateToVersion2(repository, editor);
        }
        if (storedVersion < 3 ||
                repository.contains(
                        StreamVideoSettingKeys
                                .LEGACY_BITRATE_MBPS)) {
            migrateToVersion3(repository, editor);
        }
        if (storedVersion < 4 ||
                containsLegacyGameMenuLayout(repository)) {
            migrateToVersion4(repository, editor);
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
                                .LEGACY_CLIPBOARD_IMAGE_SYNC) ||
                repository.contains(
                        StreamVideoSettingKeys
                                .LEGACY_BITRATE_MBPS) ||
                containsLegacyGameMenuLayout(repository);
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

    private static void migrateToVersion2(
            SettingsRepository repository,
            SettingsRepository.Editor editor) {
        if (repository.contains(
                VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID)) {
            editor.put(
                    VirtualControlSettingKeys.GAMEPAD_LAYOUT_ID,
                    repository.get(
                            VirtualControlSettingKeys
                                    .GAMEPAD_LAYOUT_ID));
        }
    }

    private static void migrateToVersion3(
            SettingsRepository repository,
            SettingsRepository.Editor editor) {
        if (!repository.contains(
                StreamVideoSettingKeys.LEGACY_BITRATE_MBPS)) {
            return;
        }
        if (!repository.contains(
                StreamVideoSettingKeys.BITRATE_KBPS)) {
            long legacyKbps =
                    (long) repository.get(
                            StreamVideoSettingKeys
                                    .LEGACY_BITRATE_MBPS) *
                            1000L;
            editor.put(
                    StreamVideoSettingKeys.BITRATE_KBPS,
                    (int) Math.min(
                            StreamVideoSettingKeys
                                    .MAX_BITRATE_KBPS,
                            legacyKbps));
        }
        editor.remove(
                StreamVideoSettingKeys.LEGACY_BITRATE_MBPS);
    }

    private static boolean containsLegacyGameMenuLayout(
            SettingsRepository repository) {
        return repository.contains(
                GameMenuCardSettingKeys.LEGACY_ACTION_ORDER) ||
                repository.contains(
                        GameMenuCardSettingKeys
                                .LEGACY_HIDDEN_ACTION_IDS);
    }

    private static void migrateToVersion4(
            SettingsRepository repository,
            SettingsRepository.Editor editor) {
        boolean hasLegacyOrder = repository.contains(
                GameMenuCardSettingKeys.LEGACY_ACTION_ORDER);
        boolean hasLegacyHidden = repository.contains(
                GameMenuCardSettingKeys
                        .LEGACY_HIDDEN_ACTION_IDS);
        if (!repository.contains(
                GameMenuCardSettingKeys.ORDER_DOCUMENT) &&
                (hasLegacyOrder || hasLegacyHidden)) {
            List<String> migratedOrder = new ArrayList<>();
            if (hasLegacyOrder) {
                String legacyOrder = repository.get(
                        GameMenuCardSettingKeys
                                .LEGACY_ACTION_ORDER);
                if (!legacyOrder.isEmpty()) {
                    for (String legacyId :
                            legacyOrder.split(",")) {
                        if (!legacyId.isEmpty()) {
                            try {
                                migratedOrder.add(
                                        GameMenuCardIds.action(
                                                legacyId));
                            }
                            catch (IllegalArgumentException ignored) {
                                // Invalid legacy references are discarded.
                            }
                            if (migratedOrder.size() >=
                                    GameMenuCardLayout
                                            .MAXIMUM_CARD_COUNT) {
                                break;
                            }
                        }
                    }
                }
            }

            Set<String> migratedHidden =
                    new LinkedHashSet<>();
            if (hasLegacyHidden) {
                for (String legacyId : repository.get(
                        GameMenuCardSettingKeys
                                .LEGACY_HIDDEN_ACTION_IDS)) {
                    if (!legacyId.isEmpty()) {
                        try {
                            migratedHidden.add(
                                    GameMenuCardIds.action(
                                            legacyId));
                        }
                        catch (IllegalArgumentException ignored) {
                            // Invalid legacy references are discarded.
                        }
                    }
                }
            }
            editor.put(
                    GameMenuCardSettingKeys.ORDER_DOCUMENT,
                    GameMenuCardLayoutCodec.encodeOrder(
                            migratedOrder));
            editor.put(
                    GameMenuCardSettingKeys.HIDDEN_CARD_IDS,
                    migratedHidden);
        }

        if (hasLegacyOrder) {
            editor.remove(
                    GameMenuCardSettingKeys
                            .LEGACY_ACTION_ORDER);
        }
        if (hasLegacyHidden) {
            editor.remove(
                    GameMenuCardSettingKeys
                            .LEGACY_HIDDEN_ACTION_IDS);
        }
    }
}
