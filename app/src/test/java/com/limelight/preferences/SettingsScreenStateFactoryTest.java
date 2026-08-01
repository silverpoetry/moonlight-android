package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingKey;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class SettingsScreenStateFactoryTest {
    @Test
    public void snapshotIsImmutableAndIndependentFromRegistryMetadata() {
        FakeValues values = new FakeValues();
        SettingsItem resolution = listItem(
                StreamResolutionSettingKeys.RESOLUTION,
                "Resolution");
        resolution.appendEntry(
                "1080p",
                StreamResolutionCodec.RESOLUTION_1080P);
        values.strings.put(
                resolution.key,
                StreamResolutionCodec.RESOLUTION_1080P);

        SettingsSection source = new SettingsSection(
                "video",
                "Video",
                17);
        source.items.add(resolution);
        ArrayList<SettingsSection> sourceSections =
                new ArrayList<>();
        sourceSections.add(source);

        SettingsScreenState state =
                SettingsScreenStateFactory.create(
                        sourceSections,
                        values,
                        "Open");
        resolution.title = "Changed after freeze";
        source.items.clear();

        assertEquals(1, state.getSections().size());
        assertEquals(1, state.getFeaturedRows().size());
        assertEquals(
                "Resolution",
                state.findRow(resolution.key).getTitle());
        assertEquals(
                "1080p",
                state.findRow(resolution.key).getValueText());
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.getSections().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.getSections().get(0)
                        .getRows()
                        .clear());
    }

    @Test
    public void newSnapshotReflectsValuesWithoutMutatingOldSnapshot() {
        FakeValues values = new FakeValues();
        SettingsItem parent = booleanItem("parent", "Parent");
        SettingsItem child = booleanItem("child", "Child");
        child.dependency = parent.key;
        child.dependencyItemRef = parent;
        SettingsSection section = new SettingsSection(
                "input",
                "Input",
                0);
        section.items.add(parent);
        section.items.add(child);
        ArrayList<SettingsSection> sections = new ArrayList<>();
        sections.add(section);

        SettingsScreenState before =
                SettingsScreenStateFactory.create(
                        sections,
                        values,
                        "Open");
        values.booleans.put(parent.key, true);
        SettingsScreenState after =
                SettingsScreenStateFactory.create(
                        sections,
                        values,
                        "Open");

        assertFalse(before.findRow(child.key).isEnabled());
        assertFalse(before.findRow(parent.key).isChecked());
        assertTrue(after.findRow(child.key).isEnabled());
        assertTrue(after.findRow(parent.key).isChecked());
    }

    @Test
    public void aspectRatioIsAvailableInVideoButNotFeatured() {
        FakeValues values = new FakeValues();
        SettingsItem resolution = listItem(
                StreamResolutionSettingKeys.RESOLUTION,
                "Resolution");
        resolution.appendEntry(
                "1080p",
                StreamResolutionCodec.RESOLUTION_1080P);
        SettingsItem aspectRatio = listItem(
                StreamResolutionSettingKeys.ASPECT_RATIO,
                "Aspect ratio");
        aspectRatio.appendEntry("16:9", "16_9");

        SettingsSection video = new SettingsSection(
                "video",
                "Video",
                17);
        video.items.add(resolution);
        video.items.add(aspectRatio);
        ArrayList<SettingsSection> sections = new ArrayList<>();
        sections.add(video);

        SettingsScreenState state = SettingsScreenStateFactory.create(
                sections,
                values,
                "Open");

        assertEquals(1, state.getFeaturedRows().size());
        assertEquals(
                resolution.key,
                state.getFeaturedRows().get(0).getId());
        assertEquals(
                aspectRatio.key,
                state.findRow(aspectRatio.key).getId());
    }

    private static SettingsItem listItem(
            SettingKey<String> key,
            String title) {
        SettingsItem item = new SettingsItem();
        item.key = key.getName();
        item.settingKey = key;
        item.type = SettingsItem.Type.LIST;
        item.title = title;
        return item;
    }

    private static SettingsItem booleanItem(
            String key,
            String title) {
        SettingsItem item = new SettingsItem();
        item.key = key;
        item.settingKey = SettingKey.booleanKey(key, false);
        item.type = SettingsItem.Type.SWITCH;
        item.title = title;
        return item;
    }

    private static final class FakeValues
            implements SettingsValueReader {
        final Map<String, Boolean> booleans = new HashMap<>();
        final Map<String, String> strings = new HashMap<>();

        @Override
        public boolean getBoolean(SettingsItem item) {
            return booleans.getOrDefault(item.key, false);
        }

        @Override
        public int getInt(SettingsItem item) {
            return 0;
        }

        @Override
        public String getString(SettingsItem item) {
            return strings.getOrDefault(item.key, "");
        }

        @Override
        public String getText(SettingsItem item) {
            return getString(item);
        }
    }
}
