package com.limelight.settings.stream;

import com.limelight.settings.SettingsRepository;
import com.limelight.settings.stream.StreamResolutionCodec.DisplayAspect;

import java.util.Objects;

/**
 * Builds one validated stream-video launch snapshot.
 */
public final class StreamVideoSettingsLoader {
    private StreamVideoSettingsLoader() {
    }

    public static StreamVideoSettings load(
            SettingsRepository repository,
            DisplayAspect displayAspect) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(displayAspect, "displayAspect");

        StreamResolutionCodec.Result resolution =
                StreamResolutionSettingsLoader.load(
                        repository,
                        displayAspect);
        int bitrate = repository.get(
                StreamVideoSettingKeys.BITRATE_KBPS);
        if (bitrate == 0) {
            bitrate = StreamBitratePolicy
                    .calculateDefaultBitrateKbps(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            resolution.getFps());
        }

        return StreamVideoSettings.builder()
                .setDimensions(
                        resolution.getWidth(),
                        resolution.getHeight())
                .setFps(resolution.getFps())
                .setBitrateKbps(bitrate)
                .setVideoFormat(
                        StreamVideoSettingsCodec
                                .decodeVideoFormat(repository.get(
                                        StreamVideoSettingKeys
                                                .VIDEO_FORMAT)))
                .setHdrEnabled(repository.get(
                        StreamVideoSettingKeys.HDR_ENABLED))
                .setHdrHighBrightness(repository.get(
                        StreamVideoSettingKeys
                                .HDR_HIGH_BRIGHTNESS))
                .setIgnoreHdrCapability(repository.get(
                        StreamVideoSettingKeys
                                .IGNORE_HDR_CAPABILITY))
                .setLowLatencyExperimentEnabled(repository.get(
                        StreamVideoSettingKeys
                                .LOW_LATENCY_EXPERIMENT))
                .setFpsUnlocked(repository.get(
                        StreamVideoSettingKeys.UNLOCK_FPS))
                .setPortrait(repository.get(
                        StreamVideoSettingKeys.PORTRAIT))
                .setExternalDisplay(repository.get(
                        StreamVideoSettingKeys.EXTERNAL_DISPLAY))
                .setVirtualDisplayMode(
                        StreamVideoSettings.VirtualDisplayMode
                                .fromStorageValue(repository.get(
                                        StreamVideoSettingKeys
                                                .VIRTUAL_DISPLAY_MODE)))
                .setEnforceDisplayMode(repository.get(
                        StreamVideoSettingKeys
                                .ENFORCE_DISPLAY_MODE))
                .setScreenOnPolicy(
                        StreamVideoSettings.ScreenOnPolicy
                                .fromStorageValue(repository.get(
                                        StreamVideoSettingKeys
                                                .SCREEN_ON_POLICY)))
                .setPersistedFsrValues(
                        repository.get(
                                StreamDisplaySettingKeys.FSR_TARGET),
                        repository.get(
                                StreamDisplaySettingKeys
                                        .FSR_SHARPNESS),
                        repository.get(
                                StreamDisplaySettingKeys
                                        .FSR_HDR_OUTPUT))
                .build();
    }
}
