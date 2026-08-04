package com.limelight.settings;

import com.limelight.settings.app.AppPresentationSettingKeys;
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
    private static final SettingKey<Boolean> REMOVED_BACKGROUND_ENABLED =
            SettingKey.booleanKey(
                    "app.appearance.background.enabled",
                    false)
                    .renamedFrom("checkbox_enable_screen_bg");
    private static final SettingKey<Boolean> REMOVED_BACKGROUND_BLUR =
            SettingKey.booleanKey(
                    "app.appearance.background.blur",
                    true)
                    .renamedFrom("checkbox_enable_screen_obscure");
    private static final SettingKey<String> REMOVED_BACKGROUND_FILE =
            SettingKey.boundedStringKey(
                    "app.appearance.background.file",
                    "axi_screen_bg.png",
                    255)
                    .renamedFrom("screen_bg_file_name");
    private static final SettingKey<String> REMOVED_HOST_LIST_LABEL =
            SettingKey.boundedStringKey(
                    "app.appearance.host_list_label",
                    "",
                    256)
                    .renamedFrom("change_screen_label_key");
    private static final SettingKey<Boolean>
            RETIRED_DIRECT_TOUCH_SCALING_ENABLED =
            SettingKey.booleanKey(
                    "input.direct_touch.sensitivity_enabled",
                    false)
                    .renamedFrom("checkbox_enable_touch_sensitivity");
    private static final SettingKey<Integer>
            RETIRED_DIRECT_TOUCH_SCALING_X =
            SettingKey.integerKey(
                    "input.direct_touch.sensitivity_x",
                    100,
                    10,
                    800)
                    .renamedFrom("seekbar_touch_sensitivity_opacity_x");
    private static final SettingKey<Integer>
            RETIRED_DIRECT_TOUCH_SCALING_Y =
            SettingKey.integerKey(
                    "input.direct_touch.sensitivity_y",
                    100,
                    10,
                    800)
                    .renamedFrom("seekbar_touch_sensitivity_opacity_y");
    private static final SettingKey<Boolean>
            RETIRED_DIRECT_TOUCH_GLOBAL_SCALING =
            SettingKey.booleanKey(
                    "input.direct_touch.global_sensitivity",
                    false)
                    .renamedFrom(
                            "checkbox_enable_global_touch_sensitivity");
    private static final SettingKey<Boolean>
            RETIRED_DIRECT_TOUCH_RECENTER =
            SettingKey.booleanKey(
                    "input.direct_touch.recenter_after_rotation",
                    true)
                    .renamedFrom(
                            "checkbox_enable_touch_sensitivity_rotation_auto");
    private static final SettingKey<Boolean>
            RETIRED_AUDIO_REACTIVE_VIBRATION_ENABLED =
            SettingKey.booleanKey(
                    "stream.haptics.audio.enabled",
                    false)
                    .renamedFrom("checkbox_enable_audio_haptics");
    private static final SettingKey<String>
            RETIRED_AUDIO_REACTIVE_VIBRATION_TARGET =
            SettingKey.stringSetKey(
                    "stream.haptics.audio.output_target",
                    "phone",
                    "phone",
                    "controller")
                    .renamedFrom("list_audio_haptics_output_target");
    private static final SettingKey<Integer>
            RETIRED_AUDIO_REACTIVE_VIBRATION_STRENGTH =
            SettingKey.integerKey(
                    "stream.haptics.audio.strength_percent",
                    100,
                    25,
                    200)
                    .renamedFrom("seekbar_audio_haptics_strength");
    private static final SettingKey<String>
            RETIRED_AUDIO_REACTIVE_VIBRATION_FILTER =
            SettingKey.stringSetKey(
                    "stream.haptics.audio.voice_filter",
                    "off",
                    "off",
                    "low",
                    "medium",
                    "high")
                    .renamedFrom("list_audio_haptics_voice_filter");
    private static final SettingKey<Boolean>
            RETIRED_AUDIO_REACTIVE_VIBRATION_RUMBLE_POLICY =
            SettingKey.booleanKey(
                    "stream.haptics.audio.keep_controller_rumble",
                    false)
                    .renamedFrom(
                            "checkbox_audio_haptics_keep_controller_rumble");
    private static final SettingKey<?>[] RETIRED_INPUT_AND_VIBRATION_KEYS = {
            RETIRED_DIRECT_TOUCH_SCALING_ENABLED,
            RETIRED_DIRECT_TOUCH_SCALING_X,
            RETIRED_DIRECT_TOUCH_SCALING_Y,
            RETIRED_DIRECT_TOUCH_GLOBAL_SCALING,
            RETIRED_DIRECT_TOUCH_RECENTER,
            RETIRED_AUDIO_REACTIVE_VIBRATION_ENABLED,
            RETIRED_AUDIO_REACTIVE_VIBRATION_TARGET,
            RETIRED_AUDIO_REACTIVE_VIBRATION_STRENGTH,
            RETIRED_AUDIO_REACTIVE_VIBRATION_FILTER,
            RETIRED_AUDIO_REACTIVE_VIBRATION_RUMBLE_POLICY
    };

    private SettingsMigrationRunner() {
    }

    public static void migrate(SettingsRepository repository) {
        Objects.requireNonNull(repository, "repository");

        int storedVersion = repository.get(SettingsSchema.VERSION);
        boolean hasLateLegacyValues =
                containsLegacyValues(repository);
        boolean hasRenamedValues =
                containsRenamedValues(repository);
        boolean hasRemovedAppearanceValues =
                containsRemovedAppearanceValues(repository);
        boolean hasLegacyThemePreference =
                containsCanonicalOrAlias(
                        repository,
                        AppPresentationSettingKeys.LEGACY_LIGHT_THEME);
        boolean hasRetiredInputOrVibrationValues =
                containsRetiredInputOrVibrationValues(repository);
        if (storedVersion >= SettingsSchema.CURRENT_VERSION &&
                !hasLateLegacyValues &&
                !hasRenamedValues &&
                !hasRemovedAppearanceValues &&
                !hasLegacyThemePreference &&
                !hasRetiredInputOrVibrationValues) {
            return;
        }

        SettingsRepository.Editor editor = repository.edit();
        // Rename aliases first. Later semantic migrations intentionally write
        // canonical values and therefore win when an old multi-key setting
        // must be merged or converted in the same transaction.
        if (storedVersion < 5 || hasRenamedValues) {
            migrateToVersion5(
                    repository,
                    editor,
                    storedVersion < 5);
        }
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
        if (storedVersion < 7 || hasRemovedAppearanceValues) {
            migrateToVersion7(editor);
        }
        if (storedVersion < 8 || hasLegacyThemePreference) {
            migrateToVersion8(
                    repository,
                    editor,
                    hasLegacyThemePreference);
        }
        if (storedVersion < 9 || hasRetiredInputOrVibrationValues) {
            migrateToVersion9(editor);
        }
        if (storedVersion < SettingsSchema.CURRENT_VERSION) {
            editor.put(
                    SettingsSchema.VERSION,
                    SettingsSchema.CURRENT_VERSION);
        }
        editor.commit();
    }

    /** Removes presentation options retired by the Material 3 UI. */
    private static void migrateToVersion7(
            SettingsRepository.Editor editor) {
        removeWithAliases(editor, REMOVED_BACKGROUND_ENABLED);
        removeWithAliases(editor, REMOVED_BACKGROUND_BLUR);
        removeWithAliases(editor, REMOVED_BACKGROUND_FILE);
        removeWithAliases(editor, REMOVED_HOST_LIST_LABEL);
    }

    /** Replaces the old two-state theme toggle with system/light/dark policy. */
    private static void migrateToVersion8(
            SettingsRepository repository,
            SettingsRepository.Editor editor,
            boolean hasLegacyThemePreference) {
        if (hasLegacyThemePreference &&
                !repository.contains(
                        AppPresentationSettingKeys.THEME_MODE)) {
            boolean lightTheme = getCanonicalOrAlias(
                    repository,
                    AppPresentationSettingKeys.LEGACY_LIGHT_THEME);
            editor.put(
                    AppPresentationSettingKeys.THEME_MODE,
                    lightTheme
                            ? AppPresentationSettingKeys.THEME_MODE_LIGHT
                            : AppPresentationSettingKeys.THEME_MODE_DARK);
        }
        removeWithAliases(
                editor,
                AppPresentationSettingKeys.LEGACY_LIGHT_THEME);
    }

    private static boolean containsRemovedAppearanceValues(
            SettingsRepository repository) {
        return containsCanonicalOrAlias(repository, REMOVED_BACKGROUND_ENABLED) ||
                containsCanonicalOrAlias(repository, REMOVED_BACKGROUND_BLUR) ||
                containsCanonicalOrAlias(repository, REMOVED_BACKGROUND_FILE) ||
                containsCanonicalOrAlias(repository, REMOVED_HOST_LIST_LABEL);
    }

    private static boolean containsRetiredInputOrVibrationValues(
            SettingsRepository repository) {
        for (SettingKey<?> key : RETIRED_INPUT_AND_VIBRATION_KEYS) {
            if (containsCanonicalOrAlias(repository, key)) {
                return true;
            }
        }
        return false;
    }

    /** Removes retired input scaling and audio-reactive vibration preferences. */
    private static void migrateToVersion9(
            SettingsRepository.Editor editor) {
        for (SettingKey<?> key : RETIRED_INPUT_AND_VIBRATION_KEYS) {
            removeWithAliases(editor, key);
        }
    }

    private static <T> void removeWithAliases(
            SettingsRepository.Editor editor,
            SettingKey<T> key) {
        editor.remove(key);
        for (String legacyName : key.getLegacyNames()) {
            editor.remove(key.legacyAlias(legacyName));
        }
    }

    private static boolean containsRenamedValues(
            SettingsRepository repository) {
        for (SettingKey<?> key : SettingsKeyCatalog.all()) {
            for (String legacyName : key.getLegacyNames()) {
                if (containsLegacyAlias(
                        repository,
                        key,
                        legacyName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> boolean containsLegacyAlias(
            SettingsRepository repository,
            SettingKey<T> key,
            String legacyName) {
        return repository.contains(key.legacyAlias(legacyName));
    }

    private static void migrateToVersion5(
            SettingsRepository repository,
            SettingsRepository.Editor editor,
            boolean legacyValuesAreAuthoritative) {
        for (SettingKey<?> key : SettingsKeyCatalog.all()) {
            migrateRenamedKey(
                    repository,
                    editor,
                    key,
                    legacyValuesAreAuthoritative);
        }
    }

    private static <T> void migrateRenamedKey(
            SettingsRepository repository,
            SettingsRepository.Editor editor,
            SettingKey<T> key,
            boolean legacyValuesAreAuthoritative) {
        boolean canonicalValueExists = repository.contains(key);
        for (String legacyName : key.getLegacyNames()) {
            SettingKey<T> legacyKey = key.legacyAlias(legacyName);
            if (!repository.contains(legacyKey)) {
                continue;
            }
            if (!canonicalValueExists ||
                    legacyValuesAreAuthoritative) {
                editor.put(key, repository.get(legacyKey));
                canonicalValueExists = true;
            }
            editor.remove(legacyKey);
        }
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
            if (!containsCanonicalOrAlias(
                    repository,
                    StreamAudioSettingKeys
                            .CHANNEL_CONFIGURATION) &&
                    repository.get(
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
            if (!containsCanonicalOrAlias(
                    repository,
                    StreamDecoderSettingKeys.FRAME_PACING)) {
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
            }
            editor.remove(
                    StreamDecoderSettingKeys
                            .LEGACY_DISABLE_FRAME_DROP);
        }

        if (repository.contains(
                TransferSettingKeys
                        .LEGACY_CLIPBOARD_IMAGE_SYNC)) {
            boolean clipboardSyncEnabled;
            if (repository.contains(
                    TransferSettingKeys.CLIPBOARD_SYNC)) {
                clipboardSyncEnabled = repository.get(
                        TransferSettingKeys.CLIPBOARD_SYNC);
            }
            else {
                clipboardSyncEnabled = getBooleanLegacyAlias(
                        repository,
                        TransferSettingKeys.CLIPBOARD_SYNC) ||
                        repository.get(
                                TransferSettingKeys
                                        .LEGACY_CLIPBOARD_IMAGE_SYNC);
            }
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
        boolean hasCurrentBitrate = repository.contains(
                StreamVideoSettingKeys.BITRATE_KBPS);
        for (String legacyName : StreamVideoSettingKeys
                .BITRATE_KBPS.getLegacyNames()) {
            hasCurrentBitrate |= containsLegacyAlias(
                    repository,
                    StreamVideoSettingKeys.BITRATE_KBPS,
                    legacyName);
        }
        if (!hasCurrentBitrate) {
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
        if (!containsCanonicalOrAlias(
                repository,
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

    private static <T> boolean containsCanonicalOrAlias(
            SettingsRepository repository,
            SettingKey<T> key) {
        if (repository.contains(key)) {
            return true;
        }
        for (String legacyName : key.getLegacyNames()) {
            if (containsLegacyAlias(repository, key, legacyName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean getBooleanLegacyAlias(
            SettingsRepository repository,
            SettingKey<Boolean> key) {
        for (String legacyName : key.getLegacyNames()) {
            SettingKey<Boolean> legacyKey =
                    key.legacyAlias(legacyName);
            if (repository.contains(legacyKey)) {
                return repository.get(legacyKey);
            }
        }
        return false;
    }

    private static <T> T getCanonicalOrAlias(
            SettingsRepository repository,
            SettingKey<T> key) {
        if (repository.contains(key)) {
            return repository.get(key);
        }
        for (String legacyName : key.getLegacyNames()) {
            SettingKey<T> legacyKey = key.legacyAlias(legacyName);
            if (repository.contains(legacyKey)) {
                return repository.get(legacyKey);
            }
        }
        return key.getDefaultValue();
    }
}
