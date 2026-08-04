package com.limelight.preferences;

import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;

import java.util.ArrayList;
import java.util.Collections;
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
                        rows,
                        buildGroups(sourceSection.key, rows)));
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
                buildFeaturedGroups(featuredRows),
                rowsById);
    }

    private static List<SettingsScreenState.Group> buildGroups(
            String sectionId,
            List<SettingsScreenState.Row> rows) {
        return buildGroups(rows, row ->
                SettingsPresentationCatalog.forItem(
                        sectionId,
                        row.getId()));
    }

    private static List<SettingsScreenState.Group> buildFeaturedGroups(
            List<SettingsScreenState.Row> rows) {
        return buildGroups(rows, row ->
                SettingsPresentationCatalog.forFeaturedItem(row.getId()));
    }

    private static List<SettingsScreenState.Group> buildGroups(
            List<SettingsScreenState.Row> rows,
            GroupResolver resolver) {
        LinkedHashMap<String, MutableGroup> grouped =
                new LinkedHashMap<>();
        for (SettingsScreenState.Row row : rows) {
            SettingsPresentationCatalog.Group spec =
                    resolver.resolve(row);
            MutableGroup group = grouped.get(spec.getId());
            if (group == null) {
                group = new MutableGroup(spec);
                grouped.put(spec.getId(), group);
            }
            group.rows.add(row);
        }
        ArrayList<MutableGroup> ordered =
                new ArrayList<>(grouped.values());
        Collections.sort(ordered, (left, right) -> Integer.compare(
                left.spec.getOrder(),
                right.spec.getOrder()));
        ArrayList<SettingsScreenState.Group> result =
                new ArrayList<>(ordered.size());
        for (MutableGroup group : ordered) {
            result.add(new SettingsScreenState.Group(
                    group.spec.getId(),
                    group.spec.getTitleRes(),
                    group.rows));
        }
        return result;
    }

    private interface GroupResolver {
        SettingsPresentationCatalog.Group resolve(
                SettingsScreenState.Row row);
    }

    private static final class MutableGroup {
        private final SettingsPresentationCatalog.Group spec;
        private final ArrayList<SettingsScreenState.Row> rows =
                new ArrayList<>();

        private MutableGroup(
                SettingsPresentationCatalog.Group spec) {
            this.spec = spec;
        }
    }

    private static SettingsScreenState.Row createRow(
            SettingsItem item,
            SettingsValueReader values,
            CharSequence openActionLabel) {
        boolean switchControl =
                item.type == SettingsItem.Type.SWITCH;
        SettingsScreenState.Row.ControlType controlType;
        if (switchControl) {
            controlType = SettingsScreenState.Row.ControlType.SWITCH;
        }
        else if (item.type == SettingsItem.Type.ACTION ||
                item.type == SettingsItem.Type.WEB) {
            controlType = SettingsScreenState.Row.ControlType.ACTION;
        }
        else {
            controlType = SettingsScreenState.Row.ControlType.VALUE;
        }
        SettingsEditorCatalog.Kind editorKind =
                SettingsEditorCatalog.forItem(item);
        boolean inlineChoices =
                (item.type == SettingsItem.Type.LIST ||
                        item.type == SettingsItem.Type.INTEGER_LIST) &&
                (editorKind == SettingsEditorCatalog.Kind.SEGMENTED ||
                        editorKind ==
                                SettingsEditorCatalog.Kind.DISCRETE_SLIDER);
        boolean numericSlider = item.type == SettingsItem.Type.SLIDER;
        return new SettingsScreenState.Row(
                item.key,
                item.title,
                item.summary,
                valueText(item, values, openActionLabel),
                item.iconRes,
                item.isEnabled(values),
                controlType,
                switchControl && values.getBoolean(item),
                inlineChoices
                        ? choices(item)
                        : Collections.emptyList(),
                inlineChoices
                        ? selectedChoiceValue(item, values)
                        : null,
                editorKind == SettingsEditorCatalog.Kind.DISCRETE_SLIDER,
                numericSlider ? values.getInt(item) : null,
                numericSlider ? item.min : 0,
                numericSlider ? item.max : 0,
                numericSlider ? Math.max(1, item.step) : 1);
    }

    private static List<SettingsScreenState.Row.Choice> choices(
            SettingsItem item) {
        ArrayList<SettingsScreenState.Row.Choice> choices =
                new ArrayList<>(item.entries.length);
        int count = Math.min(
                item.entries.length,
                item.entryValues.length);
        for (int index = 0; index < count; index++) {
            choices.add(new SettingsScreenState.Row.Choice(
                    item.entries[index],
                    item.entryValues[index]));
        }
        return choices;
    }

    private static String selectedChoiceValue(
            SettingsItem item,
            SettingsValueReader values) {
        return item.type == SettingsItem.Type.INTEGER_LIST
                ? Integer.toString(values.getInt(item))
                : values.getString(item);
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
