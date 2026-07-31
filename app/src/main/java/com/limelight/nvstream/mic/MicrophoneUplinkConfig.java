package com.limelight.nvstream.mic;

/**
 * Immutable capture and encoder contract for microphone protocol version 1.
 *
 * <p>These values are protocol invariants, not user preferences. Supporting a
 * different format requires explicit protocol negotiation and a new factory,
 * rather than silently changing capture parameters.</p>
 */
public final class MicrophoneUplinkConfig {
    public static final int PROTOCOL_V1_SAMPLE_RATE_HZ = 48_000;
    public static final int PROTOCOL_V1_CHANNEL_COUNT = 1;
    public static final int PROTOCOL_V1_SAMPLES_PER_FRAME = 960;
    public static final int PROTOCOL_V1_OPUS_BITRATE_BPS = 40_000;
    public static final int PROTOCOL_V1_CAPTURE_BUFFER_FRAMES = 4;
    public static final int PCM_BYTES_PER_SAMPLE = 2;

    private final int sampleRateHz;
    private final int channelCount;
    private final int samplesPerFrame;
    private final int opusBitrateBps;
    private final int captureBufferFrames;

    private MicrophoneUplinkConfig(
            int sampleRateHz,
            int channelCount,
            int samplesPerFrame,
            int opusBitrateBps,
            int captureBufferFrames) {
        requireProtocolV1(
                sampleRateHz,
                channelCount,
                samplesPerFrame,
                opusBitrateBps,
                captureBufferFrames);
        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;
        this.samplesPerFrame = samplesPerFrame;
        this.opusBitrateBps = opusBitrateBps;
        this.captureBufferFrames = captureBufferFrames;
    }

    public static MicrophoneUplinkConfig protocolV1() {
        return new MicrophoneUplinkConfig(
                PROTOCOL_V1_SAMPLE_RATE_HZ,
                PROTOCOL_V1_CHANNEL_COUNT,
                PROTOCOL_V1_SAMPLES_PER_FRAME,
                PROTOCOL_V1_OPUS_BITRATE_BPS,
                PROTOCOL_V1_CAPTURE_BUFFER_FRAMES);
    }

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSamplesPerFrame() {
        return samplesPerFrame;
    }

    public int getOpusBitrateBps() {
        return opusBitrateBps;
    }

    public int getCaptureBufferFrames() {
        return captureBufferFrames;
    }

    public int getFrameDurationMillis() {
        return samplesPerFrame * 1_000 / sampleRateHz;
    }

    public int getCaptureBufferSizeBytes() {
        return samplesPerFrame *
                channelCount *
                PCM_BYTES_PER_SAMPLE *
                captureBufferFrames;
    }

    static MicrophoneUplinkConfig protocolV1FromNegotiatedValues(
            int sampleRateHz,
            int channelCount,
            int samplesPerFrame,
            int opusBitrateBps,
            int captureBufferFrames) {
        return new MicrophoneUplinkConfig(
                sampleRateHz,
                channelCount,
                samplesPerFrame,
                opusBitrateBps,
                captureBufferFrames);
    }

    private static void requireProtocolV1(
            int sampleRateHz,
            int channelCount,
            int samplesPerFrame,
            int opusBitrateBps,
            int captureBufferFrames) {
        if (sampleRateHz != PROTOCOL_V1_SAMPLE_RATE_HZ ||
                channelCount != PROTOCOL_V1_CHANNEL_COUNT ||
                samplesPerFrame !=
                        PROTOCOL_V1_SAMPLES_PER_FRAME ||
                opusBitrateBps !=
                        PROTOCOL_V1_OPUS_BITRATE_BPS ||
                captureBufferFrames !=
                        PROTOCOL_V1_CAPTURE_BUFFER_FRAMES) {
            throw new IllegalArgumentException(
                    "Unsupported microphone uplink format");
        }
    }
}
