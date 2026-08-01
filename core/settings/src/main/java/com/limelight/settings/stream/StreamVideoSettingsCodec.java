package com.limelight.settings.stream;

import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;

/**
 * Canonical conversion between stream-video domain enums and persisted
 * values.
 */
final class StreamVideoSettingsCodec {
    private StreamVideoSettingsCodec() {
    }

    static VideoFormat decodeVideoFormat(String value) {
        if ("forceav1".equals(value)) {
            return VideoFormat.FORCE_AV1;
        }
        if ("forceh265".equals(value)) {
            return VideoFormat.FORCE_HEVC;
        }
        if ("neverh265".equals(value)) {
            return VideoFormat.FORCE_H264;
        }
        return VideoFormat.AUTO;
    }

    static String encodeVideoFormat(VideoFormat value) {
        switch (value) {
            case FORCE_AV1:
                return "forceav1";
            case FORCE_HEVC:
                return "forceh265";
            case FORCE_H264:
                return "neverh265";
            case AUTO:
            default:
                return "auto";
        }
    }
}
