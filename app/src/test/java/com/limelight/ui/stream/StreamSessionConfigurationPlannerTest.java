package com.limelight.ui.stream;

import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.transfer.TransferSettings;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class StreamSessionConfigurationPlannerTest {
    @Test
    public void planTranslatesAllSettingsAndCapabilities() {
        NvApp app = new NvApp("Desktop", 7, true);
        StreamDecoderSettings decoder = decoder(
                StreamDecoderSettings.VideoFormat.AUTO,
                StreamDecoderSettings.FramePacing.CAP_FPS);
        StreamVideoSettings video = StreamVideoSettings.builder()
                .setOptimizeGameSettings(false)
                .setVirtualDisplayMode(
                        StreamVideoSettings.VirtualDisplayMode.EXTENDED)
                .build();
        StreamAudioSettings audio = StreamAudioSettings.builder()
                .setPlayHostAudio(true)
                .setChannelConfiguration(
                        StreamAudioSettings.ChannelConfiguration
                                .SURROUND_7_1)
                .build();
        ControllerSettings controller = ControllerSettings.builder()
                .setMultiControllerEnabled(true)
                .build();
        InputSettings input = InputSettings.builder()
                .setAbsoluteMouseMode(true)
                .setAdaptiveInputThrottlingDisabled(true)
                .build();

        StreamSessionConfigurationPlanner.Plan plan =
                StreamSessionConfigurationPlanner.plan(
                        new StreamSessionConfigurationPlanner
                                .SettingsSnapshot(
                                decoder,
                                video,
                                audio,
                                controller,
                                input,
                                new TransferSettings(true, "")),
                        new StreamSessionConfigurationPlanner.Environment(
                                app,
                                capabilities(
                                        true,
                                        true,
                                        true,
                                        true),
                                0b110,
                                120f,
                                420,
                                true));

        StreamSessionConfigurationPlanner.ConfigurationDocument config =
                plan.getConfigurationDocument();
        assertSame(app, config.getApp());
        assertEquals(2560, config.getWidth());
        assertEquals(1440, config.getHeight());
        assertEquals(120, config.getLaunchRefreshRate());
        assertEquals(119, config.getRefreshRate());
        assertEquals(40_000, config.getBitrateKbps());
        assertFalse(config.shouldOptimizeGameSettings());
        assertTrue(config.shouldPlayHostAudio());
        assertEquals(1392, config.getMaxPacketSize());
        assertEquals(
                StreamSessionConfigurationPlanner.NetworkMode.AUTO,
                config.getNetworkMode());
        assertEquals(
                MoonBridge.VIDEO_FORMAT_H264 |
                        MoonBridge.VIDEO_FORMAT_H265 |
                        MoonBridge.VIDEO_FORMAT_H265_MAIN10 |
                        MoonBridge.VIDEO_FORMAT_AV1_MAIN8 |
                        MoonBridge.VIDEO_FORMAT_AV1_MAIN10,
                config.getSupportedVideoFormats());
        assertEquals(0b110, config.getAttachedGamepadMask());
        assertFalse(config.shouldPersistGamepadsAfterDisconnect());
        assertEquals(12_000, config.getClientRefreshRateX100());
        assertEquals(
                StreamAudioSettings.ChannelConfiguration.SURROUND_7_1,
                config.getAudioConfiguration());
        assertEquals(11, config.getColorSpace());
        assertEquals(12, config.getColorRange());
        assertEquals(420, config.getPixelsPerInch());
        assertEquals(1, config.getVirtualDisplayMode());
        assertTrue(config.isNativeCursorEnabled());
        assertTrue(config.isClipboardSyncEnabled());
        assertTrue(config.isAdaptiveInputThrottlingDisabled());
        assertEquals(
                StreamDecoderSettings.FramePacing.CAP_FPS,
                plan.getEffectiveFramePacing());
        assertTrue(plan.isHdrEnabled());
        assertTrue(plan.getWarnings().isEmpty());
    }

    @Test
    public void unavailableForcedDecoderAndHdrProduceOrderedWarnings() {
        StreamSessionConfigurationPlanner.Plan plan = plan(
                decoder(
                        StreamDecoderSettings.VideoFormat.FORCE_HEVC,
                        StreamDecoderSettings.FramePacing.MINIMUM_LATENCY),
                ControllerSettings.builder().build(),
                InputSettings.builder().build(),
                StreamAudioSettings.builder().build(),
                capabilities(false, false, false, false),
                0,
                true);

        assertFalse(plan.isHdrEnabled());
        assertEquals(
                List.of(
                        StreamSessionConfigurationPlanner.Warning
                                .HDR_DECODER_UNSUPPORTED,
                        StreamSessionConfigurationPlanner.Warning
                                .FORCED_HEVC_DECODER_UNAVAILABLE),
                plan.getWarnings());
        assertEquals(
                MoonBridge.VIDEO_FORMAT_H264,
                plan.getConfigurationDocument()
                        .getSupportedVideoFormats());
        assertThrows(
                UnsupportedOperationException.class,
                () -> plan.getWarnings().add(
                        StreamSessionConfigurationPlanner.Warning
                                .FORCED_AV1_DECODER_UNAVAILABLE));
    }

    @Test
    public void gamepadMaskAndPersistenceFollowControllerPolicy() {
        StreamSessionConfigurationPlanner.Plan singleController = plan(
                decoder(
                        StreamDecoderSettings.VideoFormat.AUTO,
                        StreamDecoderSettings.FramePacing.BALANCED),
                ControllerSettings.builder()
                        .setMultiControllerEnabled(false)
                        .build(),
                InputSettings.builder().build(),
                StreamAudioSettings.builder().build(),
                capabilities(true, false, false, false),
                0b1110,
                false);
        assertEquals(
                1,
                singleController.getConfigurationDocument()
                        .getAttachedGamepadMask());
        assertTrue(singleController.getConfigurationDocument()
                .shouldPersistGamepadsAfterDisconnect());

        StreamSessionConfigurationPlanner.Plan onscreenController = plan(
                decoder(
                        StreamDecoderSettings.VideoFormat.AUTO,
                        StreamDecoderSettings.FramePacing.BALANCED),
                ControllerSettings.builder()
                        .setMultiControllerEnabled(true)
                        .setOnscreenControllerEnabled(true)
                        .build(),
                InputSettings.builder().build(),
                StreamAudioSettings.builder().build(),
                capabilities(true, false, false, false),
                0b0100,
                false);
        assertEquals(
                0b0101,
                onscreenController.getConfigurationDocument()
                        .getAttachedGamepadMask());
        assertFalse(onscreenController.getConfigurationDocument()
                .shouldPersistGamepadsAfterDisconnect());
    }

    @Test
    public void audioChannelConfigurationsMapToProtocolDocuments() {
        assertEquals(
                StreamAudioSettings.ChannelConfiguration.STEREO,
                planWithAudio(
                        StreamAudioSettings.ChannelConfiguration.STEREO)
                        .getConfigurationDocument()
                        .getAudioConfiguration());
        assertEquals(
                StreamAudioSettings.ChannelConfiguration.SURROUND_5_1,
                planWithAudio(
                        StreamAudioSettings.ChannelConfiguration
                                .SURROUND_5_1)
                        .getConfigurationDocument()
                        .getAudioConfiguration());
        assertEquals(
                StreamAudioSettings.ChannelConfiguration.SURROUND_7_1,
                planWithAudio(
                        StreamAudioSettings.ChannelConfiguration
                                .SURROUND_7_1)
                        .getConfigurationDocument()
                        .getAudioConfiguration());
    }

    @Test
    public void inconsistentMain10CapabilitiesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> capabilities(false, true, false, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> capabilities(false, false, false, true));
    }

    private static StreamSessionConfigurationPlanner.Plan planWithAudio(
            StreamAudioSettings.ChannelConfiguration configuration) {
        return plan(
                decoder(
                        StreamDecoderSettings.VideoFormat.AUTO,
                        StreamDecoderSettings.FramePacing.BALANCED),
                ControllerSettings.builder().build(),
                InputSettings.builder().build(),
                StreamAudioSettings.builder()
                        .setChannelConfiguration(configuration)
                        .build(),
                capabilities(true, false, false, false),
                0,
                false);
    }

    private static StreamSessionConfigurationPlanner.Plan plan(
            StreamDecoderSettings decoder,
            ControllerSettings controller,
            InputSettings input,
            StreamAudioSettings audio,
            StreamSessionConfigurationPlanner.DecoderCapabilities
                    capabilities,
            int gamepadMask,
            boolean hdrRequested) {
        return StreamSessionConfigurationPlanner.plan(
                new StreamSessionConfigurationPlanner.SettingsSnapshot(
                        decoder,
                        StreamVideoSettings.builder().build(),
                        audio,
                        controller,
                        input,
                        new TransferSettings(false, "")),
                new StreamSessionConfigurationPlanner.Environment(
                        new NvApp("Desktop", 1, true),
                        capabilities,
                        gamepadMask,
                        60f,
                        350,
                        hdrRequested));
    }

    private static StreamDecoderSettings decoder(
            StreamDecoderSettings.VideoFormat format,
            StreamDecoderSettings.FramePacing pacing) {
        return new StreamDecoderSettings(
                2560,
                1440,
                120,
                40_000,
                format,
                pacing,
                true,
                true,
                true,
                2);
    }

    private static StreamSessionConfigurationPlanner.DecoderCapabilities
            capabilities(
                    boolean hevc,
                    boolean hevcMain10,
                    boolean av1,
                    boolean av1Main10) {
        return new StreamSessionConfigurationPlanner.DecoderCapabilities(
                hevc,
                hevcMain10,
                av1,
                av1Main10,
                11,
                12);
    }
}
