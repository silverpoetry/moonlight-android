package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;

import org.junit.Test;

import java.util.ArrayList;

public final class SettingsRuntimeScreenControllerTest {
    @Test
    public void appliesBitrateDefaultsAndClipboardSummary() {
        SettingsItem bitrate = item(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());
        bitrate.keyStep = 0;
        SettingsItem editor = item(
                SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS);
        SettingsItem directory = item(
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI
                        .getName());
        SettingsScreenModel model = model(
                bitrate,
                editor,
                directory);

        new SettingsRuntimeScreenController(model, new FakeText())
                .apply(new SettingsRuntimeValues(
                        24000,
                        "Moonlight"));

        assertEquals(Integer.valueOf(24000),
                bitrate.displayDefaultInteger);
        assertEquals(50000, bitrate.max);
        assertEquals(1000, bitrate.keyStep);
        assertEquals(Integer.valueOf(24000),
                editor.displayDefaultInteger);
        assertEquals("directory:Moonlight", directory.summary);
    }

    @Test
    public void preservesExplicitKeyStepAndMissingDirectory() {
        SettingsItem bitrate = item(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());
        bitrate.keyStep = 500;
        SettingsItem directory = item(
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI
                        .getName());

        new SettingsRuntimeScreenController(
                model(bitrate, directory),
                new FakeText())
                .apply(new SettingsRuntimeValues(12000, null));

        assertEquals(500, bitrate.keyStep);
        assertNull(directory.summary);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveDefaultBitrate() {
        new SettingsRuntimeValues(0, null);
    }

    private static SettingsScreenModel model(
            SettingsItem... items) {
        SettingsSection section = new SettingsSection(
                "section",
                "Section",
                0);
        for (SettingsItem item : items) {
            section.items.add(item);
        }
        ArrayList<SettingsSection> sections = new ArrayList<>();
        sections.add(section);
        return new SettingsScreenModel(sections);
    }

    private static SettingsItem item(String key) {
        SettingsItem item = new SettingsItem();
        item.key = key;
        item.type = SettingsItem.Type.ACTION;
        return item;
    }

    private static final class FakeText implements SettingsRuntimeText {
        @Override
        public CharSequence clipboardDirectorySummary(
                String directoryLabel) {
            return "directory:" + directoryLabel;
        }
    }
}
