package com.limelight.preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public final class SettingsRegistryTest {
    @Test
    public void xmlProducesTypedTaskOrientedSections() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        ArrayList<SettingsSection> sections =
                SettingsRegistry.load(context);

        String[] expectedIds = {
                SettingsScreenIds.SECTION_VIDEO_DISPLAY,
                SettingsScreenIds.SECTION_AUDIO,
                SettingsScreenIds.SECTION_TOUCH_MOUSE,
                SettingsScreenIds.SECTION_GAMEPAD,
                SettingsScreenIds.SECTION_VIRTUAL_CONTROLS,
                SettingsScreenIds.SECTION_CLIPBOARD_FILES,
                SettingsScreenIds.SECTION_STREAM_INTERFACE,
                SettingsScreenIds.SECTION_APP_APPEARANCE,
                SettingsScreenIds.SECTION_SYSTEM_ACCESSIBILITY,
                SettingsScreenIds.SECTION_BACKUP_RESTORE,
                SettingsScreenIds.SECTION_ABOUT
        };
        String[] actualIds = new String[sections.size()];
        for (int index = 0; index < sections.size(); index++) {
            SettingsSection section = sections.get(index);
            actualIds[index] = section.key;
            assertNotNull(section.title);
            assertTrue(section.iconRes != 0);
            assertTrue(!section.items.isEmpty());
            for (SettingsItem item : section.items) {
                assertNotNull(item.key);
                assertNotNull(item.type);
                assertTrue(item.iconRes != 0);
                if (item.type != SettingsItem.Type.ACTION &&
                        item.type != SettingsItem.Type.WEB) {
                    assertNotNull(
                            "Missing schema for " + item.key,
                            item.settingKey);
                    assertEquals(
                            item.key.equals(
                                    SettingsScreenIds
                                            .EDITOR_VIDEO_BITRATE_MBPS)
                                    ? StreamVideoSettingKeys
                                            .BITRATE_KBPS
                                            .getName()
                                    : item.key,
                            item.settingKey.getName());
                }
            }
        }
        assertArrayEquals(expectedIds, actualIds);
    }
}
