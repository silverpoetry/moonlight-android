package com.limelight.preferences;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsScreenIds;

import java.util.Arrays;
import java.util.Locale;

/** Mutable screen metadata decoded from the settings XML adapter. */
final class SettingsItem {
    enum Type {
        SWITCH,
        LIST,
        INTEGER_LIST,
        SLIDER,
        TEXT,
        ACTION,
        WEB
    }

    String key;
    SettingKey<?> settingKey;
    Type type;
    CharSequence title;
    CharSequence summary;
    String dependency;
    SettingsItem dependencyItemRef;
    String url;
    boolean visible = true;
    boolean enabled = true;
    Integer displayDefaultInteger;
    int min;
    int max;
    int step;
    int keyStep;
    int divisor;
    int decimalPlaces;
    CharSequence suffix;
    CharSequence dialogMessage;
    CharSequence[] entries = new CharSequence[0];
    CharSequence[] entryValues = new CharSequence[0];
    int iconRes;

    boolean isEnabled(SettingsValueReader values) {
        if (!enabled) {
            return false;
        }
        if (isEmpty(dependency)) {
            return true;
        }
        if (dependencyItemRef == null) {
            throw new IllegalStateException(
                    "Unresolved dependency for " + key +
                            ": " + dependency);
        }
        if (dependencyItemRef.type == Type.LIST ||
                dependencyItemRef.type == Type.INTEGER_LIST) {
            String value = dependencyItemRef.type == Type.INTEGER_LIST
                    ? Integer.toString(
                            values.getInt(dependencyItemRef))
                    : values.getString(dependencyItemRef);
            return !isEmpty(value) &&
                    !"off".equals(value) &&
                    !"false".equals(value) &&
                    !"0".equals(value);
        }
        return values.getBoolean(dependencyItemRef);
    }

    CharSequence getSelectedEntry(SettingsValueReader values) {
        String selected = type == Type.INTEGER_LIST
                ? Integer.toString(values.getInt(this))
                : values.getString(this);
        for (int index = 0; index < entryValues.length; index++) {
            if (selected.equals(entryValues[index].toString())) {
                return entries[index];
            }
        }
        return selected;
    }

    String formatSliderValue(int value) {
        String text;
        if (divisor != 1) {
            text = String.format(
                    (Locale) null,
                    "%." + decimalPlaces + "f",
                    value / (float) divisor);
        }
        else {
            text = Integer.toString(value);
        }
        return isEmpty(suffix)
                ? text
                : text + (suffix.length() > 1 ? " " : "") + suffix;
    }

    int round(int value) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        if (step <= 1) {
            return value;
        }
        return ((value + (step - 1)) / step) * step;
    }

    void appendEntry(CharSequence entry, CharSequence value) {
        entries = Arrays.copyOf(entries, entries.length + 1);
        entryValues = Arrays.copyOf(
                entryValues,
                entryValues.length + 1);
        entries[entries.length - 1] = entry;
        entryValues[entryValues.length - 1] = value;
    }

    boolean isCustomBitrateEditor() {
        return SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS.equals(key);
    }

    @SuppressWarnings("unchecked")
    SettingKey<Boolean> booleanKey() {
        return (SettingKey<Boolean>) requireStorageType(
                SettingKey.StorageType.BOOLEAN);
    }

    @SuppressWarnings("unchecked")
    SettingKey<Integer> integerKey() {
        return (SettingKey<Integer>) requireStorageType(
                SettingKey.StorageType.INTEGER);
    }

    @SuppressWarnings("unchecked")
    SettingKey<String> stringKey() {
        return (SettingKey<String>) requireStorageType(
                SettingKey.StorageType.STRING);
    }

    private SettingKey<?> requireStorageType(
            SettingKey.StorageType expected) {
        if (settingKey == null ||
                settingKey.getStorageType() != expected) {
            throw new IllegalStateException(
                    "Setting " + key +
                            " requires " + expected +
                            " storage");
        }
        return settingKey;
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }
}
