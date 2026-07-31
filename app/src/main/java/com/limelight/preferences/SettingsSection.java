package com.limelight.preferences;

import java.util.ArrayList;

/** One task-oriented section in the settings screen. */
final class SettingsSection {
    final String key;
    final CharSequence title;
    final int iconRes;
    final ArrayList<SettingsItem> items = new ArrayList<>();
    boolean visible = true;

    SettingsSection(String key, CharSequence title, int iconRes) {
        this.key = key;
        this.title = title;
        this.iconRes = iconRes;
    }

    ArrayList<SettingsItem> visibleItems() {
        ArrayList<SettingsItem> result = new ArrayList<>();
        for (SettingsItem item : items) {
            if (item.visible) {
                result.add(item);
            }
        }
        return result;
    }
}
