package com.limelight.preferences;

import com.limelight.LimeLog;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import java.util.ArrayList;
import java.util.Objects;

/** Applies pure display capability policy to settings-screen metadata. */
final class SettingsDisplayController {
    private final SettingsStore store;
    private final SettingsScreenModel screenModel;
    private final SettingsDisplayText text;

    SettingsDisplayController(
            SettingsStore store,
            SettingsScreenModel screenModel,
            SettingsDisplayText text) {
        this.store = Objects.requireNonNull(store, "store");
        this.screenModel = Objects.requireNonNull(
                screenModel,
                "screenModel");
        this.text = Objects.requireNonNull(text, "text");
    }

    Result apply(SettingsDisplayCapabilities capabilities) {
        String customResolution = store.get(
                StreamVideoSettingKeys.CUSTOM_RESOLUTION_TEXT);
        boolean unlockFrameRates = store.repository.get(
                StreamVideoSettingKeys.UNLOCK_FPS);
        SettingsDisplayPolicy.Result policy =
                SettingsDisplayPolicy.evaluate(
                        capabilities,
                        customResolution,
                        unlockFrameRates);
        if (policy.hasInvalidCustomResolution()) {
            LimeLog.warning(
                    "Ignoring invalid custom resolution setting");
        }

        applyNativeResolutions(policy);
        for (SettingsDisplayPolicy.ValueRemoval removal :
                policy.getResolutionRemovals()) {
            removeValue(
                    StreamResolutionSettingKeys.RESOLUTION.getName(),
                    removal);
        }
        for (SettingsDisplayPolicy.ValueRemoval removal :
                policy.getFrameRateRemovals()) {
            removeValue(
                    StreamResolutionSettingKeys.FPS.getName(),
                    removal);
        }
        String nativeFrameRateValue = applyNativeFrameRate(
                policy.getNativeFrameRate());
        applyHdrState(policy.getHdrState());
        return new Result(nativeFrameRateValue);
    }

    private void applyNativeResolutions(
            SettingsDisplayPolicy.Result policy) {
        SettingsItem item = screenModel.findItem(
                StreamResolutionSettingKeys.RESOLUTION.getName());
        if (item == null) {
            return;
        }
        for (SettingsDisplayPolicy.ResolutionOption option :
                policy.getNativeResolutions()) {
            if (containsValue(item, option.getValue())) {
                continue;
            }

            item.appendEntry(
                    text.nativeResolutionName(option),
                    option.getValue());
        }
    }

    private String applyNativeFrameRate(int frameRate) {
        if (frameRate == 0) {
            return null;
        }
        SettingsItem item = screenModel.findItem(
                StreamResolutionSettingKeys.FPS.getName());
        if (item == null) {
            return null;
        }
        String value = Integer.toString(frameRate);
        if (containsValue(item, value)) {
            return null;
        }

        item.appendEntry(text.nativeFrameRateName(value), value);
        return value;
    }

    private void applyHdrState(
            SettingsDisplayCapabilities.HdrState hdrState) {
        SettingsItem item = screenModel.findItem(
                StreamVideoSettingKeys.HDR_ENABLED.getName());
        if (item == null) {
            return;
        }
        switch (hdrState) {
            case UNAVAILABLE:
                screenModel.hideItem(
                        StreamVideoSettingKeys.HDR_ENABLED.getName());
                break;
            case AVAILABLE:
                break;
            case BLOCKED_BY_FIRMWARE:
                item.enabled = false;
                store.put(StreamVideoSettingKeys.HDR_ENABLED, false);
                item.summary = text.hdrFirmwareRequired();
                break;
            default:
                throw new AssertionError("Unhandled HDR capability state");
        }
    }

    private void removeValue(
            String itemKey,
            SettingsDisplayPolicy.ValueRemoval removal) {
        SettingsItem item = screenModel.findItem(itemKey);
        if (item == null || item.entryValues.length == 0) {
            return;
        }

        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        for (int index = 0; index < item.entryValues.length; index++) {
            if (!removal.getValue().equalsIgnoreCase(
                    item.entryValues[index].toString())) {
                entries.add(item.entries[index]);
                values.add(item.entryValues[index]);
            }
        }
        item.entries = entries.toArray(new CharSequence[0]);
        item.entryValues = values.toArray(new CharSequence[0]);

        if (!removal.getValue().equalsIgnoreCase(
                store.getString(item))) {
            return;
        }
        if (StreamResolutionSettingKeys.RESOLUTION
                .getName()
                .equals(itemKey)) {
            store.put(
                    StreamResolutionSettingKeys.SELECTION,
                    StreamResolutionCodec.isStandardResolutionPreset(
                            removal.getFallbackValue())
                            ? StreamResolutionCodec.SELECTION_PRESET
                            : StreamResolutionCodec
                                    .SELECTION_CUSTOM_OR_NATIVE);
        }
        store.putString(item, removal.getFallbackValue());
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

    static final class Result {
        private final String nativeFrameRateValue;

        private Result(String nativeFrameRateValue) {
            this.nativeFrameRateValue = nativeFrameRateValue;
        }

        String getNativeFrameRateValue() {
            return nativeFrameRateValue;
        }
    }
}
