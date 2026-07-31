package com.limelight.ui.stream;

import android.view.Surface;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class StreamRenderSessionHostTest {
    @Test
    public void acceptedStartRetainsResourcesAndConnectingState() {
        RecordingSession session = new RecordingSession();
        RecordingMedia media = new RecordingMedia();
        RecordingUiEffects effects = new RecordingUiEffects();
        StreamRenderSessionHost host = new StreamRenderSessionHost(
                session,
                media,
                effects,
                () -> true,
                () -> { });

        host.startSession(null);

        assertTrue(session.startCalled);
        assertEquals(List.of("connecting"), effects.events);
        assertFalse(media.released);
    }

    @Test
    public void rejectedStartReleasesResourcesAndEndsUiState() {
        RecordingSession session = new RecordingSession();
        session.acceptStart = false;
        RecordingMedia media = new RecordingMedia();
        RecordingUiEffects effects = new RecordingUiEffects();
        StreamRenderSessionHost host = new StreamRenderSessionHost(
                session,
                media,
                effects,
                () -> true,
                () -> { });

        host.startSession(null);

        assertTrue(media.released);
        assertEquals(
                List.of("connecting", "ended"),
                effects.events);
    }

    @Test
    public void failedStartReleasesResourcesBeforeRethrowing() {
        RecordingSession session = new RecordingSession();
        session.startFailure =
                new IllegalStateException("expected");
        RecordingMedia media = new RecordingMedia();
        RecordingUiEffects effects = new RecordingUiEffects();
        StreamRenderSessionHost host = new StreamRenderSessionHost(
                session,
                media,
                effects,
                () -> true,
                () -> { });

        assertThrows(
                IllegalStateException.class,
                () -> host.startSession(null));
        assertTrue(media.released);
        assertEquals(
                List.of("connecting", "ended"),
                effects.events);
    }

    @Test
    public void readinessAndStopOperationsUseCurrentSessionState() {
        RecordingSession session = new RecordingSession();
        RecordingMedia media = new RecordingMedia();
        RecordingUiEffects effects = new RecordingUiEffects();
        boolean[] dependenciesReady = {false};
        boolean[] stopRequested = {false};
        StreamRenderSessionHost host = new StreamRenderSessionHost(
                session,
                media,
                effects,
                () -> dependenciesReady[0],
                () -> stopRequested[0] = true);

        assertFalse(host.canStartSession());
        dependenciesReady[0] = true;
        assertTrue(host.canStartSession());
        session.startRequested = true;
        session.needsStop = true;
        assertTrue(host.hasSessionStarted());
        assertTrue(host.sessionNeedsStop());
        host.prepareVideoForStop();
        host.stopSession();
        assertTrue(media.preparedForStop);
        assertTrue(stopRequested[0]);
    }

    private static final class RecordingSession
            implements StreamRenderSessionHost.SessionPort {
        private boolean acceptStart = true;
        private boolean startCalled;
        private boolean startRequested;
        private boolean needsStop;
        private RuntimeException startFailure;

        @Override
        public boolean canStart() {
            return !startRequested;
        }

        @Override
        public boolean start(
                StreamMediaResourceOwner.StartResources resources) {
            startCalled = true;
            if (startFailure != null) {
                throw startFailure;
            }
            startRequested = acceptStart;
            return acceptStart;
        }

        @Override
        public boolean hasStartBeenRequested() {
            return startRequested;
        }

        @Override
        public boolean needsStop() {
            return needsStop;
        }
    }

    private static final class RecordingMedia
            implements StreamRenderSessionHost.MediaPort {
        private boolean released;
        private boolean preparedForStop;

        @Override
        public StreamMediaResourceOwner.StartResources prepareStart(
                Surface renderTarget) {
            return new StreamMediaResourceOwner.StartResources(
                    null,
                    null);
        }

        @Override
        public void releaseStartResources() {
            released = true;
        }

        @Override
        public void prepareVideoForStop() {
            preparedForStop = true;
        }
    }

    private static final class RecordingUiEffects
            implements StreamRenderSessionHost.UiEffectsPort {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onConnecting() {
            events.add("connecting");
        }

        @Override
        public void onEnded() {
            events.add("ended");
        }
    }
}
