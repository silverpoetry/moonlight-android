package com.limelight.settings.stream;

import org.junit.Test;

public class StreamDecoderSettingsTest {
    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidStreamDimensions() {
        createSettings(0, 1080, 60, 20000, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidFrameRate() {
        createSettings(1920, 1080, 0, 20000, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidAudioChannelCount() {
        createSettings(1920, 1080, 60, 20000, 0);
    }

    private static StreamDecoderSettings createSettings(
            int width,
            int height,
            int fps,
            int bitrate,
            int audioChannels) {
        return new StreamDecoderSettings(
                width,
                height,
                fps,
                bitrate,
                StreamDecoderSettings.VideoFormat.AUTO,
                StreamDecoderSettings.FramePacing.MINIMUM_LATENCY,
                false,
                false,
                false,
                audioChannels);
    }
}
