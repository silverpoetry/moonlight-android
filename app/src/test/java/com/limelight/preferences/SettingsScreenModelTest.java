package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.ArrayList;

public final class SettingsScreenModelTest {
    @Test
    public void lookupAndDependencyBindingUseStableIds() {
        SettingsItem parent = item("parent");
        SettingsItem child = item("child");
        child.dependency = parent.key;
        ArrayList<SettingsSection> sections = sections(parent, child);
        SettingsScreenModel model = new SettingsScreenModel(sections);

        model.linkDependencyDefaults();

        assertSame(parent, model.findItem("parent"));
        assertSame(parent, child.dependencyItemRef);
        assertNull(model.findItem(null));
        assertNull(model.findItem("missing"));
    }

    @Test(expected = IllegalStateException.class)
    public void missingDependencyFailsBeforeRendering() {
        SettingsItem child = item("child");
        child.dependency = "missing";
        SettingsScreenModel model = new SettingsScreenModel(
                sections(child));

        model.linkDependencyDefaults();
    }

    @Test
    public void filteringMutatesTheObservedListInPlace() {
        SettingsSection visible = section("visible", item("item"));
        SettingsSection hidden = section("hidden", item("hidden_item"));
        hidden.visible = false;
        SettingsSection empty = section("empty");
        ArrayList<SettingsSection> sections = new ArrayList<>();
        sections.add(visible);
        sections.add(hidden);
        sections.add(empty);
        SettingsScreenModel model = new SettingsScreenModel(sections);

        model.removeEmptySections();

        assertEquals(1, sections.size());
        assertSame(visible, sections.get(0));
        assertEquals(-1, model.clampSelectedSection(-1));
        assertEquals(0, model.clampSelectedSection(8));
    }

    @Test
    public void hideOperationsAreSafeForUnknownIds() {
        SettingsItem item = item("item");
        SettingsSection section = section("section", item);
        SettingsScreenModel model = new SettingsScreenModel(
                sections(section));

        model.hideItem("item");
        model.hideItem("missing");
        model.hideSection("section");
        model.hideSection(null);

        assertFalse(item.visible);
        assertFalse(section.visible);
    }

    private static ArrayList<SettingsSection> sections(
            SettingsItem... items) {
        return sections(section("section", items));
    }

    private static ArrayList<SettingsSection> sections(
            SettingsSection... sections) {
        ArrayList<SettingsSection> result = new ArrayList<>();
        for (SettingsSection section : sections) {
            result.add(section);
        }
        return result;
    }

    private static SettingsSection section(
            String key,
            SettingsItem... items) {
        SettingsSection section = new SettingsSection(key, key, 0);
        for (SettingsItem item : items) {
            section.items.add(item);
        }
        return section;
    }

    private static SettingsItem item(String key) {
        SettingsItem item = new SettingsItem();
        item.key = key;
        item.type = SettingsItem.Type.ACTION;
        return item;
    }
}
