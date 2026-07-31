package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingKey;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class SettingsItemTest {
    @Test
    public void dependencyPolicyHandlesBooleanAndListValues() {
        FakeValues values = new FakeValues();
        SettingsItem dependent = item("dependent", SettingsItem.Type.SWITCH);
        SettingsItem booleanParent = item("boolean", SettingsItem.Type.SWITCH);
        dependent.dependency = booleanParent.key;
        dependent.dependencyItemRef = booleanParent;

        values.booleans.put(booleanParent.key, false);
        assertFalse(dependent.isEnabled(values));
        values.booleans.put(booleanParent.key, true);
        assertTrue(dependent.isEnabled(values));

        SettingsItem listParent = item("list", SettingsItem.Type.LIST);
        dependent.dependency = listParent.key;
        dependent.dependencyItemRef = listParent;
        for (String disabled : new String[] {"", "off", "false", "0"}) {
            values.strings.put(listParent.key, disabled);
            assertFalse(dependent.isEnabled(values));
        }
        values.strings.put(listParent.key, "enabled");
        assertTrue(dependent.isEnabled(values));
    }

    @Test
    public void sliderPolicyClampsRoundsAndFormats() {
        SettingsItem item = item("slider", SettingsItem.Type.SLIDER);
        item.min = 10;
        item.max = 100;
        item.step = 10;
        item.divisor = 10;
        item.decimalPlaces = 1;
        item.suffix = "ms";

        assertEquals(10, item.round(-1));
        assertEquals(20, item.round(11));
        assertEquals(100, item.round(101));
        assertEquals("2.0 ms", item.formatSliderValue(20));
    }

    @Test
    public void storageAccessIsTypeChecked() {
        SettingsItem item = item("count", SettingsItem.Type.SLIDER);
        SettingKey<Integer> integerKey =
                SettingKey.integerKey("count", 5, 0, 10);
        item.settingKey = integerKey;
        assertSame(integerKey, item.integerKey());

        boolean rejected = false;
        try {
            item.booleanKey();
        }
        catch (IllegalStateException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    @Test
    public void sectionReturnsOnlyVisibleItemsInOrder() {
        SettingsSection section =
                new SettingsSection("section", "Section", 1);
        SettingsItem first = item("first", SettingsItem.Type.ACTION);
        SettingsItem hidden = item("hidden", SettingsItem.Type.ACTION);
        hidden.visible = false;
        SettingsItem last = item("last", SettingsItem.Type.ACTION);
        section.items.add(first);
        section.items.add(hidden);
        section.items.add(last);

        assertEquals(2, section.visibleItems().size());
        assertSame(first, section.visibleItems().get(0));
        assertSame(last, section.visibleItems().get(1));
    }

    private static SettingsItem item(
            String key,
            SettingsItem.Type type) {
        SettingsItem item = new SettingsItem();
        item.key = key;
        item.type = type;
        return item;
    }

    private static final class FakeValues
            implements SettingsValueReader {
        final Map<String, Boolean> booleans = new HashMap<>();
        final Map<String, Integer> integers = new HashMap<>();
        final Map<String, String> strings = new HashMap<>();

        @Override
        public boolean getBoolean(SettingsItem item) {
            return booleans.getOrDefault(item.key, false);
        }

        @Override
        public int getInt(SettingsItem item) {
            return integers.getOrDefault(item.key, 0);
        }

        @Override
        public String getString(SettingsItem item) {
            return strings.getOrDefault(item.key, "");
        }
    }
}
