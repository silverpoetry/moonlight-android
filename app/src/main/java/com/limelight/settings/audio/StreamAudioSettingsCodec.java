package com.limelight.settings.audio;

import com.limelight.settings.audio.StreamAudioSettings
        .ChannelConfiguration;
import com.limelight.settings.audio.StreamAudioSettings
        .HapticsOutputTarget;
import com.limelight.settings.audio.StreamAudioSettings.VoiceFilter;

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
    private static final String HAPTICS_TARGET_PHONE = "phone";
    private static final String HAPTICS_TARGET_CONTROLLER =
            "controller";
    private static final String VOICE_FILTER_OFF = "off";
    private static final String VOICE_FILTER_LOW = "low";
    private static final String VOICE_FILTER_MEDIUM = "medium";
    private static final String VOICE_FILTER_HIGH = "high";

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

    static HapticsOutputTarget decodeHapticsOutputTarget(
            String value) {
        return HAPTICS_TARGET_CONTROLLER.equals(value)
                ? HapticsOutputTarget.CONTROLLER
                : HapticsOutputTarget.PHONE;
    }

    static String encodeHapticsOutputTarget(
            HapticsOutputTarget value) {
        return value == HapticsOutputTarget.CONTROLLER
                ? HAPTICS_TARGET_CONTROLLER
                : HAPTICS_TARGET_PHONE;
    }

    static VoiceFilter decodeVoiceFilter(String value) {
        if (VOICE_FILTER_LOW.equals(value)) {
            return VoiceFilter.LOW;
        }
        if (VOICE_FILTER_MEDIUM.equals(value)) {
            return VoiceFilter.MEDIUM;
        }
        if (VOICE_FILTER_HIGH.equals(value)) {
            return VoiceFilter.HIGH;
        }
        return VoiceFilter.OFF;
    }

    static String encodeVoiceFilter(VoiceFilter value) {
        switch (value) {
            case LOW:
                return VOICE_FILTER_LOW;
            case MEDIUM:
                return VOICE_FILTER_MEDIUM;
            case HIGH:
                return VOICE_FILTER_HIGH;
            case OFF:
            default:
                return VOICE_FILTER_OFF;
        }
    }
}
