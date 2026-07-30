package com.limelight.settings.stream;

import java.util.Objects;

/**
 * Immutable, consumer-specific settings required by the Android video decoder
 * and its performance statistics.
 */
public final class StreamDecoderSettings {
    public enum VideoFormat {
        AUTO,
        FORCE_AV1,
        FORCE_HEVC,
        FORCE_H264
    }

    public enum FramePacing {
        MINIMUM_LATENCY,
        BALANCED,
        CAP_FPS,
        MAXIMUM_SMOOTHNESS
    }

    private final int width;
    private final int height;
    private final int fps;
    private final int bitrateKbps;
    private final VideoFormat videoFormat;
    private final FramePacing framePacing;
    private final boolean fullRange;
    private final boolean lowLatencyExperimentEnabled;
    private final boolean performanceOverlayEnabled;
    private final int audioChannelCount;

    public StreamDecoderSettings(
            int width,
            int height,
            int fps,
            int bitrateKbps,
            VideoFormat videoFormat,
            FramePacing framePacing,
            boolean fullRange,
            boolean lowLatencyExperimentEnabled,
            boolean performanceOverlayEnabled,
            int audioChannelCount) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Decoder dimensions must be positive");
        }
        if (fps <= 0) {
            throw new IllegalArgumentException(
                    "Decoder frame rate must be positive");
        }
        if (bitrateKbps < 0) {
            throw new IllegalArgumentException(
                    "Decoder bitrate cannot be negative");
        }
        if (audioChannelCount <= 0) {
            throw new IllegalArgumentException(
                    "Audio channel count must be positive");
        }
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrateKbps = bitrateKbps;
        this.videoFormat = Objects.requireNonNull(
                videoFormat,
                "videoFormat");
        this.framePacing = Objects.requireNonNull(
                framePacing,
                "framePacing");
        this.fullRange = fullRange;
        this.lowLatencyExperimentEnabled =
                lowLatencyExperimentEnabled;
        this.performanceOverlayEnabled =
                performanceOverlayEnabled;
        this.audioChannelCount = audioChannelCount;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFps() {
        return fps;
    }

    public int getBitrateKbps() {
        return bitrateKbps;
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public FramePacing getFramePacing() {
        return framePacing;
    }

    public boolean isFullRange() {
        return fullRange;
    }

    public boolean isLowLatencyExperimentEnabled() {
        return lowLatencyExperimentEnabled;
    }

    public boolean isPerformanceOverlayEnabled() {
        return performanceOverlayEnabled;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }
}
