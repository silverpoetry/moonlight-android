package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DecoderFrameTimingTrackerTest {
    @Test
    public void matchesOutputUsingTheSameMonotonicClockDomain() {
        DecoderFrameTimingTracker tracker =
                new DecoderFrameTimingTracker();

        tracker.recordInputSubmission(12_345, 5_000);

        assertEquals(
                7,
                tracker.consumeDecodeLatencyMs(12_345, 5_007));
        assertEquals(
                DecoderFrameTimingTracker.NO_SAMPLE,
                tracker.consumeDecodeLatencyMs(12_345, 5_008));
    }

    @Test
    public void rejectsOutliersAndClearsPendingSamples() {
        DecoderFrameTimingTracker tracker =
                new DecoderFrameTimingTracker();
        tracker.recordInputSubmission(1, 5_000);
        tracker.recordInputSubmission(2, 5_000);

        assertEquals(
                DecoderFrameTimingTracker.NO_SAMPLE,
                tracker.consumeDecodeLatencyMs(1, 6_000));

        tracker.clear();
        assertEquals(
                DecoderFrameTimingTracker.NO_SAMPLE,
                tracker.consumeDecodeLatencyMs(2, 5_001));
    }

    @Test
    public void expiresUnmatchedFramesWithoutUnboundedGrowth() {
        DecoderFrameTimingTracker tracker =
                new DecoderFrameTimingTracker();
        tracker.recordInputSubmission(1, 1_000);

        // A later submission triggers the periodic expiry sweep.
        tracker.recordInputSubmission(2, 2_501);

        assertEquals(
                DecoderFrameTimingTracker.NO_SAMPLE,
                tracker.consumeDecodeLatencyMs(1, 2_502));
        assertEquals(
                1,
                tracker.consumeDecodeLatencyMs(2, 2_502));
    }
}
