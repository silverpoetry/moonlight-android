package com.limelight.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable presentation snapshot consumed by the settings renderer. */
public final class SettingsScreenState {
    private final List<Section> sections;
    private final List<Row> featuredRows;
    private final List<Group> featuredGroups;
    private final Map<String, Row> rowsById;

    SettingsScreenState(
            List<Section> sections,
            List<Row> featuredRows,
            List<Group> featuredGroups,
            Map<String, Row> rowsById) {
        this.sections = immutableCopy(sections);
        this.featuredRows = immutableCopy(featuredRows);
        this.featuredGroups = immutableCopy(featuredGroups);
        this.rowsById = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(
                        rowsById,
                        "rowsById")));
    }

    List<Section> getSections() {
        return sections;
    }

    List<Row> getFeaturedRows() {
        return featuredRows;
    }

    List<Group> getFeaturedGroups() {
        return featuredGroups;
    }

    Row findRow(String id) {
        return rowsById.get(id);
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(
                        values,
                        "values")));
    }

    static final class Section {
        private final String id;
        private final CharSequence title;
        private final int iconRes;
        private final List<Row> rows;
        private final List<Group> groups;

        Section(
                String id,
                CharSequence title,
                int iconRes,
                List<Row> rows,
                List<Group> groups) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title")
                    .toString();
            this.iconRes = iconRes;
            this.rows = immutableCopy(rows);
            this.groups = immutableCopy(groups);
        }

        String getId() {
            return id;
        }

        CharSequence getTitle() {
            return title;
        }

        int getIconRes() {
            return iconRes;
        }

        List<Row> getRows() {
            return rows;
        }

        List<Group> getGroups() {
            return groups;
        }
    }

    static final class Group {
        private final String id;
        private final int titleRes;
        private final List<Row> rows;

        Group(String id, int titleRes, List<Row> rows) {
            this.id = Objects.requireNonNull(id, "id");
            this.titleRes = titleRes;
            this.rows = immutableCopy(rows);
        }

        String getId() {
            return id;
        }

        int getTitleRes() {
            return titleRes;
        }

        List<Row> getRows() {
            return rows;
        }
    }

    static final class Row {
        static final class Choice {
            private final CharSequence label;
            private final String value;

            Choice(CharSequence label, CharSequence value) {
                this.label = Objects.requireNonNull(label, "label")
                        .toString();
                this.value = Objects.requireNonNull(value, "value")
                        .toString();
            }

            CharSequence getLabel() {
                return label;
            }

            String getValue() {
                return value;
            }
        }

        enum ControlType {
            SWITCH,
            VALUE,
            ACTION
        }

        private final String id;
        private final CharSequence title;
        private final CharSequence summary;
        private final CharSequence valueText;
        private final int iconRes;
        private final boolean enabled;
        private final ControlType controlType;
        private final boolean checked;
        private final List<Choice> inlineChoices;
        private final String selectedChoiceValue;
        private final boolean discreteSlider;
        private final Integer sliderValue;
        private final int sliderMinimum;
        private final int sliderMaximum;
        private final int sliderStep;

        Row(
                String id,
                CharSequence title,
                CharSequence summary,
                CharSequence valueText,
                int iconRes,
                boolean enabled,
                ControlType controlType,
                boolean checked,
                List<Choice> inlineChoices,
                String selectedChoiceValue,
                boolean discreteSlider,
                Integer sliderValue,
                int sliderMinimum,
                int sliderMaximum,
                int sliderStep) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title")
                    .toString();
            this.summary = summary == null
                    ? null
                    : summary.toString();
            this.valueText = valueText == null
                    ? null
                    : valueText.toString();
            this.iconRes = iconRes;
            this.enabled = enabled;
            this.controlType = Objects.requireNonNull(
                    controlType,
                    "controlType");
            this.checked = checked;
            this.inlineChoices = immutableCopy(inlineChoices);
            this.selectedChoiceValue = selectedChoiceValue;
            this.discreteSlider = discreteSlider;
            this.sliderValue = sliderValue;
            this.sliderMinimum = sliderMinimum;
            this.sliderMaximum = sliderMaximum;
            this.sliderStep = sliderStep;
        }

        String getId() {
            return id;
        }

        CharSequence getTitle() {
            return title;
        }

        CharSequence getSummary() {
            return summary;
        }

        CharSequence getValueText() {
            return valueText;
        }

        int getIconRes() {
            return iconRes;
        }

        boolean isEnabled() {
            return enabled;
        }

        boolean hasSwitchControl() {
            return controlType == ControlType.SWITCH;
        }

        boolean hasValueControl() {
            return controlType == ControlType.VALUE;
        }

        boolean hasActionControl() {
            return controlType == ControlType.ACTION;
        }

        boolean isChecked() {
            return checked;
        }

        boolean hasInlineChoices() {
            return !inlineChoices.isEmpty();
        }

        List<Choice> getInlineChoices() {
            return inlineChoices;
        }

        String getSelectedChoiceValue() {
            return selectedChoiceValue;
        }

        boolean hasDiscreteSlider() {
            return discreteSlider && !inlineChoices.isEmpty();
        }

        boolean hasNumericSlider() {
            return sliderValue != null;
        }

        int getSliderValue() {
            if (sliderValue == null) {
                throw new IllegalStateException("Row has no numeric slider: " + id);
            }
            return sliderValue;
        }

        int getSliderMinimum() {
            return sliderMinimum;
        }

        int getSliderMaximum() {
            return sliderMaximum;
        }

        int getSliderStep() {
            return sliderStep;
        }
    }
}
