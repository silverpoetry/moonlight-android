package com.limelight.ui.stream;

/** Immutable decoder facts sampled once before transport configuration. */
public final class StreamDecoderCapabilities {
    private final boolean hevc;
    private final boolean hevcMain10Hdr10;
    private final boolean av1;
    private final boolean av1Main10;
    private final int preferredColorSpace;
    private final int preferredColorRange;

    public StreamDecoderCapabilities(
            boolean hevc,
            boolean hevcMain10Hdr10,
            boolean av1,
            boolean av1Main10,
            int preferredColorSpace,
            int preferredColorRange) {
        if (hevcMain10Hdr10 && !hevc) {
            throw new IllegalArgumentException(
                    "HEVC Main10 requires an HEVC decoder");
        }
        if (av1Main10 && !av1) {
            throw new IllegalArgumentException(
                    "AV1 Main10 requires an AV1 decoder");
        }
        this.hevc = hevc;
        this.hevcMain10Hdr10 = hevcMain10Hdr10;
        this.av1 = av1;
        this.av1Main10 = av1Main10;
        this.preferredColorSpace = preferredColorSpace;
        this.preferredColorRange = preferredColorRange;
    }

    public boolean isHevcSupported() {
        return hevc;
    }

    public boolean isHevcMain10Hdr10Supported() {
        return hevcMain10Hdr10;
    }

    public boolean isAv1Supported() {
        return av1;
    }

    public boolean isAv1Main10Supported() {
        return av1Main10;
    }

    public int getPreferredColorSpace() {
        return preferredColorSpace;
    }

    public int getPreferredColorRange() {
        return preferredColorRange;
    }
}
