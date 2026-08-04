package com.limelight.settings.audio;

import com.limelight.settings.audio.StreamAudioSettings
        .ChannelConfiguration;

/**
 * Canonical conversion between persisted audio schema values and domain
 * enums.
 *
 * <p>Keeping this conversion beside the schema prevents loaders and typed
 * update intents from silently assigning different meanings to the same
 * stored value.</p>
 */
final class StreamAudioSettingsCodec {
    private static final String CHANNEL_SURROUND_5_1 = "51";
    private static final String CHANNEL_SURROUND_7_1 = "71";
    private StreamAudioSettingsCodec() {
    }

    static ChannelConfiguration decodeChannelConfiguration(
            String value) {
        if (CHANNEL_SURROUND_7_1.equals(value)) {
            return ChannelConfiguration.SURROUND_7_1;
        }
        if (CHANNEL_SURROUND_5_1.equals(value)) {
            return ChannelConfiguration.SURROUND_5_1;
        }
        return ChannelConfiguration.STEREO;
    }

}
