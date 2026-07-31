package com.limelight.binding.video;

import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.stream.StreamDecoderSettings;

/**
 * Platform-independent policy for codec acceptance and stream color defaults.
 */
final class DecoderSelectionPolicy {
    enum NonWhitelistedHevcDecision {
        ACCEPT_FORCED,
        ACCEPT_HDR_REQUIRED,
        ACCEPT_RESOLUTION_REQUIRED,
        CHECK_PERFORMANCE
    }

    private static final int REC_709_DEFAULT_API_LEVEL = 26;
    private static final int MAX_AVC_DIMENSION = 4096;

    private DecoderSelectionPolicy() {
    }

    static boolean shouldAttemptHevc(
            StreamDecoderSettings.VideoFormat format) {
        return format !=
                StreamDecoderSettings.VideoFormat.FORCE_H264;
    }

    static NonWhitelistedHevcDecision
            evaluateNonWhitelistedHevc(
            StreamDecoderSettings.VideoFormat format,
            boolean requestedHdr,
            int width,
            int height) {
        if (format ==
                StreamDecoderSettings.VideoFormat.FORCE_H264) {
            throw new IllegalArgumentException(
                    "HEVC is disabled by the selected format");
        }
        if (format ==
                StreamDecoderSettings.VideoFormat.FORCE_HEVC) {
            return NonWhitelistedHevcDecision.ACCEPT_FORCED;
        }
        if (requestedHdr) {
            return NonWhitelistedHevcDecision
                    .ACCEPT_HDR_REQUIRED;
        }
        if (width > MAX_AVC_DIMENSION ||
                height > MAX_AVC_DIMENSION) {
            return NonWhitelistedHevcDecision
                    .ACCEPT_RESOLUTION_REQUIRED;
        }
        return NonWhitelistedHevcDecision.CHECK_PERFORMANCE;
    }

    static boolean shouldAttemptAv1(
            StreamDecoderSettings.VideoFormat format) {
        return format ==
                StreamDecoderSettings.VideoFormat.FORCE_AV1;
    }

    static int getPreferredColorSpace(
            int sdkInt,
            boolean hasHevcDecoder,
            boolean hasAv1Decoder) {
        return sdkInt >= REC_709_DEFAULT_API_LEVEL ||
                hasHevcDecoder ||
                hasAv1Decoder
                ? MoonBridge.COLORSPACE_REC_709
                : MoonBridge.COLORSPACE_REC_601;
    }

    static int getPreferredColorRange(boolean fullRange) {
        return fullRange
                ? MoonBridge.COLOR_RANGE_FULL
                : MoonBridge.COLOR_RANGE_LIMITED;
    }
}
