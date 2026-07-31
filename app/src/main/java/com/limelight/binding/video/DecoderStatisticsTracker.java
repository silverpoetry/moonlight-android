package com.limelight.binding.video;

/**
 * Owns decoder statistics windows and cumulative counters.
 *
 * <p>Methods intentionally remain lock-free because callers are the existing
 * realtime input and renderer threads. This class adds no scheduling,
 * allocation, or synchronization to per-frame updates.</p>
 */
final class DecoderStatisticsTracker {
    private static final long WINDOW_DURATION_MS = 1_000;

    private final VideoStats activeWindow = new VideoStats();
    private final VideoStats previousWindow = new VideoStats();
    private final VideoStats cumulative = new VideoStats();

    void startWindow(long nowMs) {
        activeWindow.measurementStartTimestamp = nowMs;
    }

    boolean isWindowElapsed(long nowMs) {
        return nowMs >=
                activeWindow.measurementStartTimestamp +
                        WINDOW_DURATION_MS;
    }

    void recordFramesLost(int count) {
        activeWindow.framesLost += count;
        activeWindow.totalFrames += count;
        activeWindow.frameLossEvents++;
    }

    void recordRenderedFrame() {
        activeWindow.totalFramesRendered++;
    }

    void recordDecoderLatency(
            long latencyMs,
            boolean includeInEndToEndLatency) {
        activeWindow.decoderTimeMs += latencyMs;
        if (includeInEndToEndLatency) {
            activeWindow.totalTimeMs += latencyMs;
        }
    }

    void recordFrameRenderLatency(long latencyMs) {
        activeWindow.totalTimeMs += latencyMs;
    }

    void recordDecodeUnit(
            int decodeUnitLength,
            char hostProcessingLatency,
            long receiveToEnqueueLatencyMs,
            boolean includeReceiveToEnqueueLatency) {
        if (hostProcessingLatency != 0) {
            if (activeWindow.minHostProcessingLatency != 0) {
                activeWindow.minHostProcessingLatency =
                        (char) Math.min(
                                activeWindow
                                        .minHostProcessingLatency,
                                hostProcessingLatency);
            }
            else {
                activeWindow.minHostProcessingLatency =
                        hostProcessingLatency;
            }
            activeWindow.framesWithHostProcessingLatency++;
        }
        activeWindow.maxHostProcessingLatency =
                (char) Math.max(
                        activeWindow.maxHostProcessingLatency,
                        hostProcessingLatency);
        activeWindow.totalHostProcessingLatency +=
                hostProcessingLatency;
        activeWindow.totalFramesReceived++;
        activeWindow.totalFrames++;
        activeWindow.videoBytes += decodeUnitLength;

        if (includeReceiveToEnqueueLatency) {
            activeWindow.totalTimeMs +=
                    receiveToEnqueueLatencyMs;
        }
    }

    VideoStats snapshotRecentWindows() {
        VideoStats snapshot = new VideoStats();
        snapshot.add(previousWindow);
        snapshot.add(activeWindow);
        return snapshot;
    }

    void rotateWindow(long nowMs) {
        cumulative.add(activeWindow);
        previousWindow.copy(activeWindow);
        activeWindow.clear();
        activeWindow.measurementStartTimestamp = nowMs;
    }

    long getTotalVideoBytesIncludingActiveWindow() {
        return cumulative.videoBytes +
                activeWindow.videoBytes;
    }

    int getAverageEndToEndLatencyMs() {
        if (cumulative.totalFramesReceived == 0) {
            return 0;
        }
        return (int) (cumulative.totalTimeMs /
                cumulative.totalFramesReceived);
    }

    int getAverageDecoderLatencyMs() {
        if (cumulative.totalFramesReceived == 0) {
            return 0;
        }
        return (int) (cumulative.decoderTimeMs /
                cumulative.totalFramesReceived);
    }

    int getCumulativeFramesReceived() {
        return cumulative.totalFramesReceived;
    }

    int getCumulativeFramesRendered() {
        return cumulative.totalFramesRendered;
    }

    int getCumulativeFramesLost() {
        return cumulative.framesLost;
    }

    int getCumulativeFrameLossEvents() {
        return cumulative.frameLossEvents;
    }
}
