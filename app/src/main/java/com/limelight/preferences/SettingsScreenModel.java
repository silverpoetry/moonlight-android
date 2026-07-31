package com.limelight.preferences;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Mutable settings-screen state with one owner for structural operations.
 * Android rendering observes the same section list but does not implement
 * lookup, dependency, visibility, or selection policy itself.
 */
final class SettingsScreenModel {
    private final ArrayList<SettingsSection> sections;

    SettingsScreenModel(ArrayList<SettingsSection> sections) {
        this.sections = Objects.requireNonNull(sections, "sections");
    }

    SettingsItem findItem(String key) {
        if (key == null) {
            return null;
        }
        for (SettingsSection section : sections) {
            for (SettingsItem item : section.items) {
                if (key.equals(item.key)) {
                    return item;
                }
            }
        }
        return null;
    }

    void hideItem(String key) {
        SettingsItem item = findItem(key);
        if (item != null) {
            item.visible = false;
        }
    }

    void hideSection(String key) {
        if (key == null) {
            return;
        }
        for (SettingsSection section : sections) {
            if (key.equals(section.key)) {
                section.visible = false;
            }
        }
    }

    void applyVisibility(SettingsVisibilityPolicy.Result visibility) {
        for (String sectionId : visibility.getHiddenSectionIds()) {
            hideSection(sectionId);
        }
        for (String itemId : visibility.getHiddenItemIds()) {
            hideItem(itemId);
        }
    }

    void linkDependencyDefaults() {
        for (SettingsSection section : sections) {
            for (SettingsItem item : section.items) {
                if (!isEmpty(item.dependency)) {
                    SettingsItem dependency = findItem(item.dependency);
                    if (dependency == null) {
                        throw new IllegalStateException(
                                "Unknown settings dependency " +
                                        item.dependency +
                                        " for " + item.key);
                    }
                    item.dependencyItemRef = dependency;
                }
            }
        }
    }

    void removeEmptySections() {
        ArrayList<SettingsSection> visibleSections = new ArrayList<>();
        for (SettingsSection section : sections) {
            if (section.visible && !section.visibleItems().isEmpty()) {
                visibleSections.add(section);
            }
        }
        sections.clear();
        sections.addAll(visibleSections);
    }

    int clampSelectedSection(int selected) {
        if (sections.isEmpty() || selected < 0) {
            return -1;
        }
        return Math.min(selected, sections.size() - 1);
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }
}
