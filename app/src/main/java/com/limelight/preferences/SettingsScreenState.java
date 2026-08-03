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
    private final Map<String, Row> rowsById;

    SettingsScreenState(
            List<Section> sections,
            List<Row> featuredRows,
            Map<String, Row> rowsById) {
        this.sections = immutableCopy(sections);
        this.featuredRows = immutableCopy(featuredRows);
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

        Section(
                String id,
                CharSequence title,
                int iconRes,
                List<Row> rows) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title")
                    .toString();
            this.iconRes = iconRes;
            this.rows = immutableCopy(rows);
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
    }

    static final class Row {
        private final String id;
        private final CharSequence title;
        private final CharSequence summary;
        private final CharSequence valueText;
        private final int iconRes;
        private final boolean enabled;
        private final boolean switchControl;
        private final boolean checked;

        Row(
                String id,
                CharSequence title,
                CharSequence summary,
                CharSequence valueText,
                int iconRes,
                boolean enabled,
                boolean switchControl,
                boolean checked) {
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
            this.switchControl = switchControl;
            this.checked = checked;
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
            return switchControl;
        }

        boolean isChecked() {
            return checked;
        }
    }
}
