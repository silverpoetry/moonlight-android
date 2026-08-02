package com.limelight.preferences;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.input.InputSettingKeys;
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

    @Test
    public void everySliderRangeIsAcceptedByItsTypedSchema() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();

        for (SettingsSection section : SettingsRegistry.load(context)) {
            for (SettingsItem item : section.items) {
                if (item.type != SettingsItem.Type.SLIDER) {
                    continue;
                }
                assertEquals(
                        "Schema rejects the lower endpoint of " +
                                item.key,
                        item.min,
                        item.integerKey()
                                .normalizeValue(item.min)
                                .intValue());
                assertEquals(
                        "Schema rejects the upper endpoint of " +
                                item.key,
                        item.max,
                        item.integerKey()
                                .normalizeValue(item.max)
                                .intValue());
                assertEquals(
                        "Slider step cannot represent the upper endpoint of " +
                                item.key,
                        item.max,
                        item.round(item.max));
            }
        }
    }

    @Test
    public void forcePressThresholdExposesItsCompleteTypedRange() {
        Context context = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        SettingsItem threshold = findItem(
                SettingsRegistry.load(context),
                InputSettingKeys.BAROMETER_FORCE_PRESS_THRESHOLD
                        .getName());

        assertNotNull(threshold);
        assertEquals(
                InputSettingKeys.MIN_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                threshold.min);
        assertEquals(
                InputSettingKeys.MAX_FORCE_PRESS_THRESHOLD_MILLI_HPA,
                threshold.max);
        assertEquals(1_000, threshold.divisor);
    }

    private static SettingsItem findItem(
            ArrayList<SettingsSection> sections,
            String key) {
        for (SettingsSection section : sections) {
            for (SettingsItem item : section.items) {
                if (key.equals(item.key)) {
                    return item;
                }
            }
        }
        return null;
    }
}
