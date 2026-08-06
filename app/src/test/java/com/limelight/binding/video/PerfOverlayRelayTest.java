package com.limelight.binding.video;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class PerfOverlayRelayTest {
    @Test
    public void attachReplaysAnImmutableSnapshotAndThenForwardsUpdates() {
        PerfOverlayRelay relay = new PerfOverlayRelay();
        PerfOverlayStats initial = new PerfOverlayStats();
        initial.renderedFps = 60f;
        relay.onPerfUpdate(initial);
        initial.renderedFps = 1f;

        List<Float> received = new ArrayList<>();
        PerfOverlayListener observer =
                stats -> received.add(stats.renderedFps);
        relay.attach(observer);

        PerfOverlayStats next = new PerfOverlayStats();
        next.renderedFps = 120f;
        relay.onPerfUpdate(next);
        relay.detach(observer);
        next.renderedFps = 30f;
        relay.onPerfUpdate(next);

        assertEquals(List.of(60f, 120f), received);
    }
}
