package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DecoderStatisticsTrackerTest {
    @Test
    public void windowBoundaryUsesMonotonicTimestamp() {
        DecoderStatisticsTracker tracker =
                new DecoderStatisticsTracker();
        tracker.startWindow(5_000);

        assertFalse(tracker.isWindowElapsed(5_999));
        assertTrue(tracker.isWindowElapsed(6_000));
    }

    @Test
    public void recentSnapshotCombinesPreviousAndActiveWindows() {
        DecoderStatisticsTracker tracker =
                new DecoderStatisticsTracker();
        tracker.startWindow(1_000);
        tracker.recordDecodeUnit(
                100,
                (char) 10,
                4,
                true);
        tracker.recordRenderedFrame();
        tracker.rotateWindow(2_000);
        tracker.recordDecodeUnit(
                200,
                (char) 20,
                6,
                true);

        VideoStats snapshot =
                tracker.snapshotRecentWindows();
        VideoStatsFps fps = snapshot.getFps(3_000);

        assertEquals(2, snapshot.totalFramesReceived);
        assertEquals(1, snapshot.totalFramesRendered);
        assertEquals(300, snapshot.videoBytes);
        assertEquals(10, snapshot.minHostProcessingLatency);
        assertEquals(20, snapshot.maxHostProcessingLatency);
        assertEquals(1f, fps.receivedFps, 0);
        assertEquals(0.5f, fps.renderedFps, 0);
    }

    @Test
    public void lossAndLatencyCountersRollIntoCumulativeStats() {
        DecoderStatisticsTracker tracker =
                new DecoderStatisticsTracker();
        tracker.startWindow(1_000);
        tracker.recordFramesLost(2);
        tracker.recordDecodeUnit(
                400,
                (char) 0,
                5,
                true);
        tracker.recordDecoderLatency(7, true);
        tracker.rotateWindow(2_000);

        assertEquals(
                12,
                tracker.getAverageEndToEndLatencyMs());
        assertEquals(
                7,
                tracker.getAverageDecoderLatencyMs());
        assertEquals(1, tracker.getCumulativeFramesReceived());
        assertEquals(2, tracker.getCumulativeFramesLost());
        assertEquals(1, tracker.getCumulativeFrameLossEvents());
        assertEquals(
                400,
                tracker
                        .getTotalVideoBytesIncludingActiveWindow());
    }

    @Test
    public void decoderAverageUsesOnlyMatchedDecoderSamples() {
        DecoderStatisticsTracker tracker =
                new DecoderStatisticsTracker();
        tracker.recordDecodeUnit(
                100,
                (char) 0,
                0,
                false);
        tracker.recordDecodeUnit(
                100,
                (char) 0,
                0,
                false);
        tracker.recordDecoderLatency(8, true);

        assertEquals(8, tracker.getAverageDecoderLatencyMs());
    }

    @Test
    public void activeBytesAreIncludedBeforeRotation() {
        DecoderStatisticsTracker tracker =
                new DecoderStatisticsTracker();
        tracker.recordDecodeUnit(
                512,
                (char) 0,
                0,
                false);

        assertEquals(
                512,
                tracker
                        .getTotalVideoBytesIncludingActiveWindow());
        assertEquals(0, tracker.getAverageEndToEndLatencyMs());
        assertEquals(0, tracker.getAverageDecoderLatencyMs());
    }
}
