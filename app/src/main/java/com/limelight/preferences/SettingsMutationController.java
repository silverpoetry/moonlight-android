package com.limelight.preferences;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.CustomResolution;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Single use-case boundary for settings writes and their presentation effects.
 */
final class SettingsMutationController {
    private static final long SWITCH_ANIMATION_DELAY_MS = 180;
    private static final long FRAME_RATE_RELOAD_DELAY_MS = 500;

    private final SettingsStore store;

    SettingsMutationController(SettingsStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    ChangeResult changeBoolean(
            SettingsItem item,
            boolean value,
            boolean allowSwitchAnimation) {
        Objects.requireNonNull(item, "item");
        store.putBoolean(item, value);
        return ChangeResult.accepted(
                effectAfterChange(item, allowSwitchAnimation),
                false);
    }

    ChangeResult changeInteger(
            SettingsItem item,
            int value) {
        Objects.requireNonNull(item, "item");
        store.putInt(item, value);
        return ChangeResult.accepted(
                effectAfterChange(item, false),
                false);
    }

    ChangeResult changeList(
            SettingsItem item,
            String value,
            String nativeFrameRateValue) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(value, "value");
        SettingsRepository.Editor editor =
                store.repository.edit();
        if (StreamResolutionSettingKeys.RESOLUTION
                .getName()
                .equals(item.key)) {
            editor.put(
                    StreamResolutionSettingKeys.SELECTION,
                    StreamResolutionCodec.isStandardResolutionPreset(value)
                            ? StreamResolutionCodec.SELECTION_PRESET
                            : StreamResolutionCodec
                                    .SELECTION_CUSTOM_OR_NATIVE);
        }
        editor.put(item.stringKey(), value).apply();
        return ChangeResult.accepted(
                effectAfterChange(item, false),
                StreamResolutionSettingKeys.FPS
                                .getName()
                                .equals(item.key) &&
                        value.equals(nativeFrameRateValue));
    }

    ChangeResult changeText(
            SettingsItem item,
            String value) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(value, "value");
        if (!item.isCustomBitrateEditor()) {
            store.putString(item, value);
            return ChangeResult.accepted(
                    effectAfterChange(item, false),
                    false);
        }
        if (value.isEmpty()) {
            return ChangeResult.invalidBitrate();
        }
        try {
            BigDecimal bitrateMbps = new BigDecimal(value);
            if (bitrateMbps.signum() < 0 ||
                    bitrateMbps.compareTo(
                            BigDecimal.valueOf(9999)) > 0) {
                return ChangeResult.invalidBitrate();
            }
            int bitrateKbps = bitrateMbps
                    .movePointRight(3)
                    .intValueExact();
            store.put(
                    StreamVideoSettingKeys.BITRATE_KBPS,
                    bitrateKbps);
            return ChangeResult.accepted(
                    effectAfterChange(item, false),
                    false);
        }
        catch (NumberFormatException | ArithmeticException error) {
            return ChangeResult.invalidBitrate();
        }
    }

    ChangeResult addCustomResolution(
            SettingsItem resolutionItem,
            String rawValue) {
        Objects.requireNonNull(resolutionItem, "resolutionItem");
        CustomResolution resolution = CustomResolution.parse(rawValue);
        if (resolution == null) {
            return ChangeResult.rejected(
                    ValidationError.INVALID_CUSTOM_RESOLUTION);
        }
        String value = resolution.toStorageValue();
        if (containsValue(resolutionItem, value)) {
            return ChangeResult.rejected(
                    ValidationError.DUPLICATE_CUSTOM_RESOLUTION);
        }

        Set<String> customResolutions = new LinkedHashSet<>(
                store.get(StreamResolutionSettingKeys
                        .CUSTOM_RESOLUTIONS));
        if (customResolutions.contains(value)) {
            return ChangeResult.rejected(
                    ValidationError.DUPLICATE_CUSTOM_RESOLUTION);
        }
        if (customResolutions.size() >=
                StreamResolutionSettingKeys.MAX_CUSTOM_RESOLUTIONS) {
            return ChangeResult.rejected(
                    ValidationError.CUSTOM_RESOLUTION_LIMIT);
        }
        customResolutions.add(value);
        store.repository.edit()
                .put(
                        StreamResolutionSettingKeys.CUSTOM_RESOLUTIONS,
                        customResolutions)
                .put(StreamResolutionSettingKeys.RESOLUTION, value)
                .put(
                        StreamResolutionSettingKeys.SELECTION,
                        StreamResolutionCodec
                                .SELECTION_CUSTOM_OR_NATIVE)
                .apply();
        return ChangeResult.accepted(
                ChangeEffect.reload(0),
                false);
    }

    ChangeResult removeCustomResolution(String rawValue) {
        CustomResolution resolution = CustomResolution.parse(rawValue);
        if (resolution == null) {
            return ChangeResult.rejected(
                    ValidationError.INVALID_CUSTOM_RESOLUTION);
        }
        String value = resolution.toStorageValue();
        Set<String> customResolutions = new LinkedHashSet<>(
                store.get(StreamResolutionSettingKeys
                        .CUSTOM_RESOLUTIONS));
        if (!customResolutions.remove(value)) {
            return ChangeResult.accepted(
                    ChangeEffect.refresh(0),
                    false);
        }

        SettingsRepository.Editor editor = store.repository.edit()
                .put(
                        StreamResolutionSettingKeys.CUSTOM_RESOLUTIONS,
                        customResolutions);
        if (value.equals(store.get(
                StreamResolutionSettingKeys.RESOLUTION))) {
            editor.put(
                            StreamResolutionSettingKeys.RESOLUTION,
                            StreamResolutionCodec.DEFAULT_RESOLUTION)
                    .put(
                            StreamResolutionSettingKeys.SELECTION,
                            StreamResolutionCodec.SELECTION_PRESET);
        }
        editor.apply();
        return ChangeResult.accepted(
                ChangeEffect.reload(0),
                false);
    }

    private static boolean containsValue(
            SettingsItem item,
            String value) {
        for (CharSequence candidate : item.entryValues) {
            if (value.equals(candidate.toString())) {
                return true;
            }
        }
        return false;
    }

    private ChangeEffect effectAfterChange(
            SettingsItem item,
            boolean allowSwitchAnimation) {
        Objects.requireNonNull(item, "item");
        if (AppPresentationSettingKeys.THEME_MODE
                .getName()
                .equals(item.key) ||
                AppPresentationSettingKeys.LANGUAGE
                        .getName()
                        .equals(item.key)) {
            return ChangeEffect.recreate(allowSwitchAnimation
                    ? SWITCH_ANIMATION_DELAY_MS
                    : 0);
        }
        if (InputSettingKeys.BAROMETER_FORCE_PRESS
                .getName()
                .equals(item.key)) {
            return ChangeEffect.reload(allowSwitchAnimation
                    ? SWITCH_ANIMATION_DELAY_MS
                    : 0);
        }
        if (StreamVideoSettingKeys.UNLOCK_FPS
                .getName()
                .equals(item.key)) {
            return ChangeEffect.reload(
                    FRAME_RATE_RELOAD_DELAY_MS);
        }
        return ChangeEffect.refresh(allowSwitchAnimation
                ? SWITCH_ANIMATION_DELAY_MS
                : 0);
    }

    enum ValidationError {
        NONE,
        INVALID_BITRATE,
        INVALID_CUSTOM_RESOLUTION,
        DUPLICATE_CUSTOM_RESOLUTION,
        CUSTOM_RESOLUTION_LIMIT,
    }

    static final class ChangeResult {
        private final ValidationError validationError;
        private final ChangeEffect effect;
        private final boolean showNativeFrameRateWarning;

        private ChangeResult(
                ValidationError validationError,
                ChangeEffect effect,
                boolean showNativeFrameRateWarning) {
            this.validationError = validationError;
            this.effect = effect;
            this.showNativeFrameRateWarning = showNativeFrameRateWarning;
        }

        static ChangeResult accepted(
                ChangeEffect effect,
                boolean showNativeFrameRateWarning) {
            return new ChangeResult(
                    ValidationError.NONE,
                    Objects.requireNonNull(effect, "effect"),
                    showNativeFrameRateWarning);
        }

        static ChangeResult invalidBitrate() {
            return rejected(ValidationError.INVALID_BITRATE);
        }

        static ChangeResult rejected(ValidationError error) {
            if (error == ValidationError.NONE) {
                throw new IllegalArgumentException(
                        "A rejected change requires an error");
            }
            return new ChangeResult(error, null, false);
        }

        boolean isAccepted() {
            return validationError == ValidationError.NONE;
        }

        ValidationError getValidationError() {
            return validationError;
        }

        ChangeEffect getEffect() {
            if (effect == null) {
                throw new IllegalStateException(
                        "Rejected change has no effect");
            }
            return effect;
        }

        boolean shouldShowNativeFrameRateWarning() {
            return showNativeFrameRateWarning;
        }
    }

    static final class ChangeEffect {
        enum Type {
            REFRESH,
            RELOAD,
            RECREATE
        }

        private final Type type;
        private final long delayMs;

        private ChangeEffect(Type type, long delayMs) {
            this.type = type;
            this.delayMs = delayMs;
        }

        static ChangeEffect refresh(long delayMs) {
            return new ChangeEffect(Type.REFRESH, delayMs);
        }

        static ChangeEffect reload(long delayMs) {
            return new ChangeEffect(Type.RELOAD, delayMs);
        }

        static ChangeEffect recreate(long delayMs) {
            return new ChangeEffect(Type.RECREATE, delayMs);
        }

        Type getType() {
            return type;
        }

        long getDelayMs() {
            return delayMs;
        }
    }
}
