package com.limelight.binding.video;

public class PerfOverlayStats {
    public int width;
    public int height;
    public int targetBitrateKbps;
    public int targetFps;
    public int networkLatencyMs;
    public int networkLatencyVarianceMs;
    /** True when the control-stream ENet RTT has been established. */
    public boolean networkLatencyAvailable;

    public float totalFps;
    public float receivedFps;
    public float renderedFps;
    public float packetLossPercent;
    public float decodeTimeMs;
    public boolean decoderLatencyAvailable;
    public float hostProcessingLatencyMs;
    public float networkRateKbps;
    public float videoRateKbps;
    public float audioRateKbps;

    public long videoBytes;
    public long audioBytes;
    public long totalNetworkBytes;

    public boolean hdr;
    public String codecName;
    public String decoderName;
}
