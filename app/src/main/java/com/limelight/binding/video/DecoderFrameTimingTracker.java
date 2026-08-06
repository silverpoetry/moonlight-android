package com.limelight.binding.video;

import java.util.Arrays;

/**
 * Matches MediaCodec input presentation timestamps with their Android-clock
 * submission times.
 *
 * <p>The timestamp supplied to MediaCodec is a presentation timestamp, not an
 * Android uptime timestamp. Measuring it directly against {@code uptimeMillis}
 * therefore produces an invalid result. This tracker keeps the two values
 * paired until the decoder exposes the corresponding output buffer.</p>
 */
final class DecoderFrameTimingTracker {
    static final long NO_SAMPLE = -1L;

    private static final long MAX_SAMPLE_AGE_MS = 1_000L;
    private static final long CLEANUP_INTERVAL_MS = 500L;
    private static final int MAX_PENDING_SAMPLES = 512;

    private final long[] presentationTimestampsUs =
            new long[MAX_PENDING_SAMPLES];
    private final long[] submissionTimesMs =
            new long[MAX_PENDING_SAMPLES];
    private final boolean[] occupied =
            new boolean[MAX_PENDING_SAMPLES];
    private int writeIndex;
    private int consumeStartIndex;
    private long lastCleanupMs;

    synchronized void recordInputSubmission(
            long presentationTimeUs,
            long submissionTimeMs) {
        pruneExpired(submissionTimeMs);

        presentationTimestampsUs[writeIndex] = presentationTimeUs;
        submissionTimesMs[writeIndex] = submissionTimeMs;
        occupied[writeIndex] = true;
        writeIndex = (writeIndex + 1) % MAX_PENDING_SAMPLES;
    }

    synchronized void discardInputSubmission(long presentationTimeUs) {
        int index = find(presentationTimeUs);
        if (index >= 0) {
            occupied[index] = false;
        }
    }

    synchronized long consumeDecodeLatencyMs(
            long presentationTimeUs,
            long outputTimeMs) {
        int index = find(presentationTimeUs);
        if (index < 0) {
            return NO_SAMPLE;
        }

        long latencyMs = outputTimeMs - submissionTimesMs[index];
        occupied[index] = false;
        consumeStartIndex = (index + 1) % MAX_PENDING_SAMPLES;
        return latencyMs >= 0 && latencyMs < MAX_SAMPLE_AGE_MS
                ? latencyMs
                : NO_SAMPLE;
    }

    synchronized void clear() {
        Arrays.fill(occupied, false);
        writeIndex = 0;
        consumeStartIndex = 0;
        lastCleanupMs = 0;
    }

    private void pruneExpired(long nowMs) {
        if (nowMs - lastCleanupMs < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupMs = nowMs;

        long oldestAllowedMs = nowMs - MAX_SAMPLE_AGE_MS;
        for (int index = 0; index < MAX_PENDING_SAMPLES; index++) {
            if (occupied[index] &&
                    submissionTimesMs[index] < oldestAllowedMs) {
                occupied[index] = false;
            }
        }
    }

    private int find(long presentationTimeUs) {
        for (int offset = 0; offset < MAX_PENDING_SAMPLES; offset++) {
            int index = (consumeStartIndex + offset) % MAX_PENDING_SAMPLES;
            if (occupied[index] &&
                    presentationTimestampsUs[index] == presentationTimeUs) {
                return index;
            }
        }
        return -1;
    }
}
