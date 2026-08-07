package com.limelight.preferences;

import com.limelight.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** User-selectable, internally atomic configuration archive components. */
enum ConfigurationArchiveComponent {
    APP_SETTINGS(
            "app_settings",
            R.string.settings_import_component_app_settings,
            R.string.settings_import_component_app_settings_summary,
            "app-settings.json"),
    PAIRING_DATA(
            "pairing_data",
            R.string.settings_import_component_pairing_data,
            R.string.settings_import_component_pairing_data_summary,
            "paired-hosts.db",
            "client.crt",
            "client.key");

    private final String id;
    private final int titleRes;
    private final int summaryRes;
    private final List<String> entryNames;

    ConfigurationArchiveComponent(
            String id,
            int titleRes,
            int summaryRes,
            String... entryNames) {
        this.id = id;
        this.titleRes = titleRes;
        this.summaryRes = summaryRes;
        this.entryNames = Collections.unmodifiableList(
                Arrays.asList(entryNames.clone()));
    }

    String getId() {
        return id;
    }

    int getTitleRes() {
        return titleRes;
    }

    int getSummaryRes() {
        return summaryRes;
    }

    List<String> getEntryNames() {
        return entryNames;
    }

    static ConfigurationArchiveComponent fromId(String id) {
        for (ConfigurationArchiveComponent component : values()) {
            if (component.id.equals(id)) {
                return component;
            }
        }
        return null;
    }
}
