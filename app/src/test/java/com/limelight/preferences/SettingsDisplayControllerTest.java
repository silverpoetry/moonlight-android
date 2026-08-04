package com.limelight.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SettingsDisplayControllerTest {
    @Test
    public void controllerAppliesOnePlanAndRepairsStoredFallbacks() {
        FakeRepository repository = new FakeRepository();
        repository.putInitial(
                StreamResolutionSettingKeys.CUSTOM_RESOLUTIONS,
                Collections.singleton("2000x1000"));
        repository.putInitial(StreamVideoSettingKeys.UNLOCK_FPS, false);
        repository.putInitial(
                StreamResolutionSettingKeys.RESOLUTION,
                StreamResolutionCodec.RESOLUTION_4K);
        repository.putInitial(StreamResolutionSettingKeys.FPS, "120");
        repository.putInitial(StreamVideoSettingKeys.HDR_ENABLED, true);

        SettingsItem resolution = listItem(
                StreamResolutionSettingKeys.RESOLUTION,
                new String[] {
                        StreamResolutionCodec.RESOLUTION_720P,
                        StreamResolutionCodec.RESOLUTION_1080P,
                        StreamResolutionCodec.RESOLUTION_1440P,
                        StreamResolutionCodec.RESOLUTION_4K,
                });
        SettingsItem frameRate = listItem(
                StreamResolutionSettingKeys.FPS,
                new String[] {"30", "60", "90", "120"});
        SettingsItem hdr = new SettingsItem();
        hdr.key = StreamVideoSettingKeys.HDR_ENABLED.getName();
        hdr.settingKey = StreamVideoSettingKeys.HDR_ENABLED;
        hdr.type = SettingsItem.Type.SWITCH;
        ArrayList<SettingsSection> sections = new ArrayList<>();
        SettingsSection section = new SettingsSection("video", "Video", 0);
        section.items.add(resolution);
        section.items.add(frameRate);
        section.items.add(hdr);
        sections.add(section);

        SettingsDisplayController.Result result =
                new SettingsDisplayController(
                        new SettingsStore(repository),
                        new SettingsScreenModel(sections),
                        new FakeText())
                        .apply(SettingsDisplayCapabilities.builder()
                                .addNativeResolution(
                                        2560,
                                        1600,
                                        false)
                                .maximumSupportedPresetWidth(1280)
                                .maximumRefreshRate(75)
                                .hdrState(SettingsDisplayCapabilities
                                        .HdrState.BLOCKED_BY_FIRMWARE)
                                .build());

        assertEquals("75", result.getNativeFrameRateValue());
        assertEquals(
                StreamResolutionCodec.RESOLUTION_720P,
                repository.get(
                        StreamResolutionSettingKeys.RESOLUTION));
        assertEquals(
                StreamResolutionCodec.SELECTION_PRESET,
                repository.get(
                        StreamResolutionSettingKeys.SELECTION));
        assertEquals("60", repository.get(
                StreamResolutionSettingKeys.FPS));
        assertFalse(repository.get(
                StreamVideoSettingKeys.HDR_ENABLED));
        assertFalse(hdr.enabled);
        assertEquals("hdr-required", hdr.summary);
        assertTrue(containsValue(resolution, "2000x1000"));
        assertTrue(containsValue(resolution, "2560x1600"));
        assertTrue(containsValue(frameRate, "75"));
        assertFalse(containsValue(frameRate, "90"));
        assertFalse(containsValue(frameRate, "120"));
    }

    @Test
    public void unavailableHdrHidesItsRow() {
        FakeRepository repository = new FakeRepository();
        SettingsItem hdr = new SettingsItem();
        hdr.key = StreamVideoSettingKeys.HDR_ENABLED.getName();
        hdr.settingKey = StreamVideoSettingKeys.HDR_ENABLED;
        hdr.type = SettingsItem.Type.SWITCH;
        SettingsSection section = new SettingsSection("video", "Video", 0);
        section.items.add(hdr);
        ArrayList<SettingsSection> sections = new ArrayList<>();
        sections.add(section);

        new SettingsDisplayController(
                new SettingsStore(repository),
                new SettingsScreenModel(sections),
                new FakeText())
                .apply(SettingsDisplayCapabilities.builder().build());

        assertFalse(hdr.visible);
    }

    private static SettingsItem listItem(
            SettingKey<String> key,
            String[] values) {
        SettingsItem item = new SettingsItem();
        item.key = key.getName();
        item.settingKey = key;
        item.type = SettingsItem.Type.LIST;
        item.entries = new CharSequence[values.length];
        item.entryValues = new CharSequence[values.length];
        for (int index = 0; index < values.length; index++) {
            item.entries[index] = values[index];
            item.entryValues[index] = values[index];
        }
        return item;
    }

    private static boolean containsValue(
            SettingsItem item,
            String value) {
        for (CharSequence candidate : item.entryValues) {
            if (value.equals(candidate.toString())) {
                return true;
            }
        }
        return false;
    }

    private static final class FakeText implements SettingsDisplayText {
        @Override
        public CharSequence customResolutionName(
                SettingsDisplayPolicy.ResolutionOption option) {
            return "custom:" + option.getValue();
        }

        @Override
        public CharSequence nativeResolutionName(
                SettingsDisplayPolicy.ResolutionOption option) {
            return "resolution:" + option.getValue();
        }

        @Override
        public CharSequence nativeFrameRateName(String frameRateValue) {
            return "fps:" + frameRateValue;
        }

        @Override
        public CharSequence hdrFirmwareRequired() {
            return "hdr-required";
        }
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values = new HashMap<>();

        <T> void putInitial(SettingKey<T> key, T value) {
            values.put(key.getName(), key.normalizeValue(value));
        }

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return contains(key)
                    ? key.normalizeStoredValue(values.get(key.getName()))
                    : key.getDefaultValue();
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> changes = new HashMap<>();

            @Override
            public <T> Editor put(SettingKey<T> key, T value) {
                changes.put(key.getName(), key.normalizeValue(value));
                return this;
            }

            @Override
            public Editor remove(SettingKey<?> key) {
                changes.put(key.getName(), null);
                return this;
            }

            @Override
            public void apply() {
                commit();
            }

            @Override
            public boolean commit() {
                for (Map.Entry<String, Object> change :
                        changes.entrySet()) {
                    if (change.getValue() == null) {
                        values.remove(change.getKey());
                    }
                    else {
                        values.put(change.getKey(), change.getValue());
                    }
                }
                return true;
            }
        }
    }
}
