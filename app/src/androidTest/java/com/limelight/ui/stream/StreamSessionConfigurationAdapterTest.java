package com.limelight.ui.stream;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.controller.ControllerSettings;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.transfer.TransferSettings;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class StreamSessionConfigurationAdapterTest {
    @Test
    public void transportAdapterPreservesThePlannedDocument() {
        NvApp app = new NvApp("Desktop", 7, true);
        StreamSessionConfigurationPlanner.Plan plan =
                StreamSessionConfigurationPlanner.plan(
                        new StreamSessionConfigurationPlanner
                                .SettingsSnapshot(
                                new StreamDecoderSettings(
                                        2560,
                                        1440,
                                        120,
                                        40_000,
                                        StreamDecoderSettings.VideoFormat.AUTO,
                                        StreamDecoderSettings.FramePacing
                                                .CAP_FPS,
                                        true,
                                        true,
                                        true,
                                        8),
                                StreamVideoSettings.builder()
                                        .setOptimizeGameSettings(false)
                                        .setVirtualDisplayMode(
                                                StreamVideoSettings
                                                        .VirtualDisplayMode
                                                        .VIRTUAL_ONLY)
                                        .build(),
                                StreamAudioSettings.builder()
                                        .setPlayHostAudio(true)
                                        .setChannelConfiguration(
                                                StreamAudioSettings
                                                        .ChannelConfiguration
                                                        .SURROUND_5_1)
                                        .build(),
                                ControllerSettings.builder()
                                        .setMultiControllerEnabled(true)
                                        .build(),
                                InputSettings.builder()
                                        .setAbsoluteMouseMode(true)
                                        .setAdaptiveInputThrottlingDisabled(
                                                true)
                                        .build(),
                                new TransferSettings(true, "")),
                        new StreamSessionConfigurationPlanner.Environment(
                                app,
                                new StreamSessionConfigurationPlanner
                                        .DecoderCapabilities(
                                        true,
                                        true,
                                        true,
                                        false,
                                        11,
                                        12),
                                6,
                                120f,
                                420,
                                true));

        StreamConfiguration configuration =
                StreamSessionConfigurationAdapter
                        .toTransportConfiguration(
                                plan.getConfigurationDocument());

        assertSame(app, configuration.getApp());
        assertEquals(2560, configuration.getWidth());
        assertEquals(1440, configuration.getHeight());
        assertEquals(120, configuration.getLaunchRefreshRate());
        assertEquals(119, configuration.getRefreshRate());
        assertEquals(40_000, configuration.getBitrate());
        assertFalse(configuration.getSops());
        assertTrue(configuration.getPlayLocalAudio());
        assertEquals(1392, configuration.getMaxPacketSize());
        assertEquals(
                StreamConfiguration.STREAM_CFG_AUTO,
                configuration.getRemote());
        assertEquals(
                MoonBridge.VIDEO_FORMAT_H264 |
                        MoonBridge.VIDEO_FORMAT_H265 |
                        MoonBridge.VIDEO_FORMAT_H265_MAIN10 |
                        MoonBridge.VIDEO_FORMAT_AV1_MAIN8,
                configuration.getSupportedVideoFormats());
        assertEquals(6, configuration.getAttachedGamepadMask());
        assertEquals(12_000, configuration.getClientRefreshRateX100());
        assertEquals(
                MoonBridge.AUDIO_CONFIGURATION_51_SURROUND,
                configuration.getAudioConfiguration());
        assertEquals(11, configuration.getColorSpace());
        assertEquals(12, configuration.getColorRange());
        assertEquals(420, configuration.getPpi());
        assertEquals(2, configuration.getRrazerVD());
        assertFalse(configuration.getPersistGamepadsAfterDisconnect());
        assertTrue(configuration.getNativeCursorEnabled());
        assertTrue(configuration.getClipboardSyncEnabled());
        assertTrue(configuration.getAdaptiveInputThrottlingDisabled());
    }
}
