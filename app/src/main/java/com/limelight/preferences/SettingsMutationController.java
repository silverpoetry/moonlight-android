package com.limelight.preferences;

import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import java.math.BigDecimal;
import java.util.Objects;

/** Pure settings-write policy and post-change presentation effects. */
final class SettingsMutationController {
    private static final long SWITCH_ANIMATION_DELAY_MS = 180;
    private static final long FRAME_RATE_RELOAD_DELAY_MS = 500;

    private final SettingsStore store;

    SettingsMutationController(SettingsStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    ListChangeResult prepareListChange(
            SettingsItem item,
            String value,
            String nativeFrameRateValue) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(value, "value");
        if (StreamResolutionSettingKeys.RESOLUTION
                .getName()
                .equals(item.key)) {
            store.put(
                    StreamResolutionSettingKeys.SELECTION,
                    StreamResolutionCodec.isStandardResolutionPreset(value)
                            ? StreamResolutionCodec.SELECTION_PRESET
                            : StreamResolutionCodec
                                    .SELECTION_CUSTOM_OR_NATIVE);
        }
        return new ListChangeResult(
                StreamResolutionSettingKeys.FPS
                        .getName()
                        .equals(item.key) &&
                        value.equals(nativeFrameRateValue));
    }

    TextChangeResult commitText(
            SettingsItem item,
            String value) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(value, "value");
        if (!item.isCustomBitrateEditor()) {
            store.putString(item, value);
            return TextChangeResult.ACCEPTED;
        }
        if (value.isEmpty()) {
            return TextChangeResult.INVALID_BITRATE;
        }
        try {
            BigDecimal bitrateMbps = new BigDecimal(value);
            if (bitrateMbps.signum() < 0 ||
                    bitrateMbps.compareTo(
                            BigDecimal.valueOf(9999)) > 0) {
                return TextChangeResult.INVALID_BITRATE;
            }
            int bitrateKbps = bitrateMbps
                    .movePointRight(3)
                    .intValueExact();
            store.put(
                    StreamVideoSettingKeys.BITRATE_KBPS,
                    bitrateKbps);
            return TextChangeResult.ACCEPTED;
        }
        catch (NumberFormatException | ArithmeticException error) {
            return TextChangeResult.INVALID_BITRATE;
        }
    }

    ChangeEffect effectAfterChange(
            SettingsItem item,
            boolean allowSwitchAnimation) {
        Objects.requireNonNull(item, "item");
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

    enum TextChangeResult {
        ACCEPTED,
        INVALID_BITRATE
    }

    static final class ListChangeResult {
        private final boolean showNativeFrameRateWarning;

        private ListChangeResult(boolean showNativeFrameRateWarning) {
            this.showNativeFrameRateWarning = showNativeFrameRateWarning;
        }

        boolean shouldShowNativeFrameRateWarning() {
            return showNativeFrameRateWarning;
        }
    }

    static final class ChangeEffect {
        enum Type {
            REFRESH,
            RELOAD
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

        Type getType() {
            return type;
        }

        long getDelayMs() {
            return delayMs;
        }
    }
}
