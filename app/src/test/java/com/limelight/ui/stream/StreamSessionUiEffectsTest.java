package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class StreamSessionUiEffectsTest {
    @Test
    public void successfulSessionHasOneOrderedEffectTrace() {
        RecordingHost host = new RecordingHost();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host);

        effects.onConnecting();
        effects.onConnecting();
        effects.onConnected();
        effects.onConnected();
        effects.onEnded();
        effects.onEnded();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "keep:true",
                        "connected",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    @Test
    public void failedStartEndsConnectingStateWithoutKeepingScreenOn() {
        RecordingHost host = new RecordingHost();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host);

        effects.onConnecting();
        effects.onEnded();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    @Test
    public void destroyBeforeStartHasNoSideEffects() {
        RecordingHost host = new RecordingHost();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host);

        effects.destroy();
        effects.onConnecting();

        assertEquals(0, host.trace.size());
    }

    @Test
    public void connectedCannotArriveBeforeConnectingOrAfterEnd() {
        RecordingHost host = new RecordingHost();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host);

        effects.onConnected();
        effects.onConnecting();
        effects.onEnded();
        effects.onConnected();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    private static final class RecordingHost
            implements StreamSessionUiEffects.Host {
        final List<String> trace = new ArrayList<>();

        @Override
        public void setKeepScreenOn(boolean keepScreenOn) {
            trace.add("keep:" + keepScreenOn);
        }

        @Override
        public void notifyStreamConnecting() {
            trace.add("connecting");
        }

        @Override
        public void notifyStreamConnected() {
            trace.add("connected");
        }

        @Override
        public void notifyStreamEnded() {
            trace.add("ended");
        }
    }
}
