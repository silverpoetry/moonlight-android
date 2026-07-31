package com.limelight.settings.stream;

import com.limelight.settings.SettingKey;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamVideoSettings.ScreenOnPolicy;
import com.limelight.settings.stream.StreamVideoSettings.VirtualDisplayMode;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamVideoSettingsUpdateTest {
    @Test
    public void scalarUpdateChangesOnlyItsOwnedField() {
        StreamVideoSettings original = representativeSettings();
        StreamVideoSettingsUpdate update =
                StreamVideoSettingsUpdate.hdrEnabled(false);

        StreamVideoSettings updated = update.applyTo(original);

        assertFalse(updated.isHdrEnabled());
        assertEquals(
                original.getWidth(),
                updated.getWidth());
        assertEquals(
                original.getVideoFormat(),
                updated.getVideoFormat());
        assertEquals(
                original.getScreenOnPolicy(),
                updated.getScreenOnPolicy());
    }

    @Test
    public void displayApplyIsOneOwnedTransaction() {
        FakeRepository repository = new FakeRepository();
        StreamVideoSettings current = representativeSettings();
        StreamVideoSettings draft = current.toBuilder()
                .setDimensions(3440, 1440)
                .setFps(165)
                .setBitrateKbps(120_000)
                .setPortrait(true)
                .setExternalDisplay(true)
                .setVideoFormat(VideoFormat.FORCE_H264)
                .setHdrEnabled(false)
                .build();
        StreamVideoSettingsUpdate update =
                StreamVideoSettingsUpdate
                        .displayConfiguration(draft);

        StreamVideoSettings updated = update.applyTo(current);
        update.persist(repository);

        assertEquals(3440, updated.getWidth());
        assertEquals(1440, updated.getHeight());
        assertEquals(165, updated.getFps());
        assertEquals(120_000, updated.getBitrateKbps());
        assertTrue(updated.isPortrait());
        assertTrue(updated.isExternalDisplay());
        assertEquals(
                current.getVideoFormat(),
                updated.getVideoFormat());
        assertEquals(
                current.isHdrEnabled(),
                updated.isHdrEnabled());
        assertEquals(
                current.getScreenOnPolicy(),
                updated.getScreenOnPolicy());
        assertEquals(
                current.getVirtualDisplayMode(),
                updated.getVirtualDisplayMode());

        assertEquals(
                "3440x1440",
                repository.values.get(
                        StreamResolutionSettingKeys.RESOLUTION
                                .getName()));
        assertEquals(
                StreamResolutionCodec
                        .SELECTION_CUSTOM_OR_NATIVE,
                repository.values.get(
                        StreamResolutionSettingKeys.SELECTION
                                .getName()));
        assertEquals(
                "165",
                repository.values.get(
                        StreamResolutionSettingKeys.FPS.getName()));
        assertEquals(
                120_000,
                repository.values.get(
                        StreamVideoSettingKeys.BITRATE_KBPS
                                .getName()));
        assertEquals(
                "3440x1440",
                repository.values.get(
                        StreamVideoSettingKeys.CUSTOM_RESOLUTION_TEXT
                                .getName()));
        assertEquals(7, repository.values.size());
        assertEquals(1, repository.applyCount);
    }

    @Test
    public void enumUpdatesUseCanonicalStorageValues() {
        FakeRepository repository = new FakeRepository();
        StreamVideoSettings original = representativeSettings();
        StreamVideoSettingsUpdate videoFormat =
                StreamVideoSettingsUpdate.videoFormat(
                        VideoFormat.FORCE_AV1);
        StreamVideoSettingsUpdate screenPolicy =
                StreamVideoSettingsUpdate.screenOnPolicy(
                        ScreenOnPolicy.CURRENT_SESSION);
        StreamVideoSettingsUpdate virtualDisplay =
                StreamVideoSettingsUpdate.virtualDisplayMode(
                        VirtualDisplayMode.VIRTUAL_ONLY);

        StreamVideoSettings updated =
                videoFormat.applyTo(original);
        updated = screenPolicy.applyTo(updated);
        updated = virtualDisplay.applyTo(updated);
        videoFormat.persist(repository);
        screenPolicy.persist(repository);
        virtualDisplay.persist(repository);

        assertEquals(
                VideoFormat.FORCE_AV1,
                updated.getVideoFormat());
        assertEquals(
                ScreenOnPolicy.CURRENT_SESSION,
                updated.getScreenOnPolicy());
        assertEquals(
                VirtualDisplayMode.VIRTUAL_ONLY,
                updated.getVirtualDisplayMode());
        assertEquals(
                "forceav1",
                repository.values.get(
                        StreamVideoSettingKeys.VIDEO_FORMAT
                                .getName()));
        assertEquals(
                1,
                repository.values.get(
                        StreamVideoSettingKeys.SCREEN_ON_POLICY
                                .getName()));
        assertEquals(
                2,
                repository.values.get(
                        StreamVideoSettingKeys.VIRTUAL_DISPLAY_MODE
                                .getName()));
        assertEquals(3, repository.applyCount);
    }

    private static StreamVideoSettings representativeSettings() {
        return StreamVideoSettings.builder()
                .setDimensions(2560, 1440)
                .setFps(120)
                .setBitrateKbps(80_000)
                .setVideoFormat(VideoFormat.FORCE_HEVC)
                .setHdrEnabled(true)
                .setHdrHighBrightness(true)
                .setIgnoreHdrCapability(true)
                .setLowLatencyExperimentEnabled(false)
                .setFpsUnlocked(true)
                .setPortrait(false)
                .setExternalDisplay(false)
                .setVirtualDisplayMode(
                        VirtualDisplayMode.EXTENDED)
                .setEnforceDisplayMode(true)
                .setScreenOnPolicy(ScreenOnPolicy.ALWAYS)
                .build();
    }

    private static final class FakeRepository
            implements SettingsRepository {
        private final Map<String, Object> values =
                new HashMap<>();
        private int applyCount;

        @Override
        public boolean contains(SettingKey<?> key) {
            return values.containsKey(key.getName());
        }

        @Override
        public <T> T get(SettingKey<T> key) {
            return key.normalizeStoredValue(
                    values.get(key.getName()));
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public <T> Editor put(
                        SettingKey<T> key,
                        T value) {
                    values.put(
                            key.getName(),
                            key.normalizeValue(value));
                    return this;
                }

                @Override
                public Editor remove(SettingKey<?> key) {
                    values.remove(key.getName());
                    return this;
                }

                @Override
                public void apply() {
                    applyCount++;
                }

                @Override
                public boolean commit() {
                    applyCount++;
                    return true;
                }
            };
        }
    }
}
