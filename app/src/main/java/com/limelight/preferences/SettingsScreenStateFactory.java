package com.limelight.preferences;

import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Freezes mutable registry metadata into a read-only render snapshot. */
final class SettingsScreenStateFactory {
    private static final String[] FEATURED_SETTING_IDS = new String[] {
            StreamResolutionSettingKeys.RESOLUTION.getName(),
            StreamResolutionSettingKeys.FPS.getName(),
            StreamVideoSettingKeys.BITRATE_KBPS.getName(),
            InputSettingKeys.TOUCH_MODE.getName(),
            TransferSettingKeys.CLIPBOARD_SYNC.getName(),
    };

    private SettingsScreenStateFactory() {
    }

    static SettingsScreenState create(
            List<SettingsSection> sourceSections,
            SettingsValueReader values,
            CharSequence openActionLabel) {
        Objects.requireNonNull(sourceSections, "sourceSections");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(openActionLabel, "openActionLabel");

        ArrayList<SettingsScreenState.Section> sections =
                new ArrayList<>();
        LinkedHashMap<String, SettingsScreenState.Row> rowsById =
                new LinkedHashMap<>();
        for (SettingsSection sourceSection : sourceSections) {
            if (!sourceSection.visible) {
                continue;
            }
            ArrayList<SettingsScreenState.Row> rows =
                    new ArrayList<>();
            for (SettingsItem item : sourceSection.items) {
                if (!item.visible) {
                    continue;
                }
                SettingsScreenState.Row row = createRow(
                        item,
                        values,
                        openActionLabel);
                if (rowsById.put(row.getId(), row) != null) {
                    throw new IllegalStateException(
                            "Duplicate settings row ID: " + row.getId());
                }
                rows.add(row);
            }
            if (!rows.isEmpty()) {
                sections.add(new SettingsScreenState.Section(
                        sourceSection.key,
                        sourceSection.title,
                        sourceSection.iconRes,
                        rows));
            }
        }

        ArrayList<SettingsScreenState.Row> featuredRows =
                new ArrayList<>();
        for (String id : FEATURED_SETTING_IDS) {
            SettingsScreenState.Row row = rowsById.get(id);
            if (row != null) {
                featuredRows.add(row);
            }
        }
        return new SettingsScreenState(
                sections,
                featuredRows,
                rowsById);
    }

    private static SettingsScreenState.Row createRow(
            SettingsItem item,
            SettingsValueReader values,
            CharSequence openActionLabel) {
        boolean switchControl =
                item.type == SettingsItem.Type.SWITCH;
        return new SettingsScreenState.Row(
                item.key,
                item.title,
                item.summary,
                valueText(item, values, openActionLabel),
                item.iconRes,
                item.isEnabled(values),
                switchControl,
                switchControl && values.getBoolean(item));
    }

    private static CharSequence valueText(
            SettingsItem item,
            SettingsValueReader values,
            CharSequence openActionLabel) {
        switch (item.type) {
            case SWITCH:
                return null;
            case LIST:
            case INTEGER_LIST:
                return item.getSelectedEntry(values);
            case SLIDER:
                return item.formatSliderValue(
                        item.round(values.getInt(item)));
            case TEXT:
                return values.getText(item);
            case ACTION:
            case WEB:
                return openActionLabel;
            default:
                throw new AssertionError(
                        "Unhandled settings item type: " + item.type);
        }
    }
}
