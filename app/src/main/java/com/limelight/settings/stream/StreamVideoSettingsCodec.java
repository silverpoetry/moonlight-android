package com.limelight.settings.stream;

import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamDisplaySettings.FsrHdrOutput;
import com.limelight.settings.stream.StreamDisplaySettings.FsrSharpness;
import com.limelight.settings.stream.StreamDisplaySettings.FsrTarget;

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

    static String encodeFsrTarget(FsrTarget value) {
        switch (value) {
            case OUTPUT_2K:
                return "2k";
            case OUTPUT_4K:
                return "4k";
            case NATIVE_HEIGHT:
                return "native_height";
            case OFF:
                return "off";
            case UNKNOWN:
            default:
                throw new IllegalArgumentException(
                        "Unknown FSR target cannot be selected");
        }
    }

    static String encodeFsrSharpness(FsrSharpness value) {
        switch (value) {
            case SOFT:
                return "soft";
            case STRONG:
                return "strong";
            case MAXIMUM:
                return "max";
            case STANDARD:
            default:
                return "standard";
        }
    }

    static String encodeFsrHdrOutput(FsrHdrOutput value) {
        return value == FsrHdrOutput.NATIVE
                ? "native"
                : "sdr";
    }
}
