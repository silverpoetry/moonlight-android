package com.limelight.ui.stream;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class StreamSessionPresentationControllerTest {
    private static final int INCONCLUSIVE = -7;

    @Test
    public void stageFailureOwnsDiagnosticsAndSuppressesDuplicates() {
        RecordingHost host = new RecordingHost();
        host.surfaceValid = true;
        ManualDiagnostics diagnostics = new ManualDiagnostics(true);
        StreamSessionPresentationController controller = create(
                host,
                diagnostics);

        controller.onStageStarting("video");
        controller.onStageFailed("video", 3, -4);
        controller.onStageFailed("audio", 8, -9);

        assertEquals(1, diagnostics.requestCount);
        assertEquals(2, host.dismissCount);
        assertEquals(
                List.of("connecting:starting video", "message:decoder"),
                host.events);

        diagnostics.complete(5, 1);
        assertEquals(
                "dialog:connection error title:" +
                        "connection error video (error -4)\n\n" +
                        "check ports\nports:5\n\nblocked",
                host.events.get(2));
    }

    @Test
    public void rejectedDiagnosticsPresentsInconclusiveFallbackImmediately() {
        RecordingHost host = new RecordingHost();
        StreamSessionPresentationController controller = create(
                host,
                new ManualDiagnostics(false));

        controller.onStageFailed("audio", 4, 6);

        assertEquals(
                List.of(
                        "dialog:connection error title:" +
                                "connection error audio (error 6)\n\n" +
                                "check ports\nports:4"),
                host.events);
    }

    @Test
    public void gracefulTerminationStopsSessionWithoutDiagnostics() {
        RecordingHost host = new RecordingHost();
        ManualDiagnostics diagnostics = new ManualDiagnostics(true);
        StreamSessionPresentationController controller = create(
                host,
                diagnostics);

        controller.onConnectionTerminated(
                MoonBridge.ML_ERROR_GRACEFUL_TERMINATION);

        assertEquals(
                List.of("stop-controller", "stop-connection", "finish"),
                host.events);
        assertEquals(0, diagnostics.requestCount);
    }

    @Test
    public void terminationDiagnosesOnceAndUsesResolvedPortFlags() {
        RecordingHost host = new RecordingHost();
        ManualDiagnostics diagnostics = new ManualDiagnostics(true);
        StreamSessionPresentationController controller = create(
                host,
                diagnostics);

        controller.onConnectionTerminated(
                MoonBridge.ML_ERROR_NO_VIDEO_FRAME);
        controller.onConnectionTerminated(99);

        assertEquals(1, diagnostics.requestCount);
        assertEquals(77, diagnostics.requestedPortFlags);
        assertEquals(
                List.of(
                        "stop-controller",
                        "stop-connection",
                        "stop-controller"),
                host.events);

        diagnostics.complete(9, 0);
        assertEquals(
                "dialog:termination title:no frame\n\n" +
                        "check ports\nports:9",
                host.events.get(3));
    }

    @Test
    public void connectionWarningsRespectSettingsAndBitratePolicy() {
        RecordingHost host = new RecordingHost();
        StreamSessionPresentationController controller = create(
                host,
                new ManualDiagnostics(true));

        host.bitrateKbps = 5000;
        controller.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR);
        host.bitrateKbps = 5001;
        controller.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR);
        controller.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_OKAY);
        host.warningsDisabled = true;
        controller.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR);

        assertEquals(
                List.of("warning:POOR", "warning:SLOW", "warning:NONE"),
                host.events);
    }

    @Test
    public void transientMessagesOnlyFollowWarningPolicy() {
        RecordingHost host = new RecordingHost();
        StreamSessionPresentationController controller = create(
                host,
                new ManualDiagnostics(true));

        host.warningsDisabled = true;
        controller.onMessage("transient", true);
        controller.onMessage("persistent", false);
        host.warningsDisabled = false;
        controller.onMessage("visible", true);

        assertEquals(
                List.of("message:persistent", "message:visible"),
                host.events);
    }

    @Test
    public void ownerSuppressionAndDestructionRejectLateDiagnostics() {
        RecordingHost host = new RecordingHost();
        ManualDiagnostics diagnostics = new ManualDiagnostics(true);
        StreamSessionPresentationController controller = create(
                host,
                diagnostics);

        controller.onStageFailed("audio", 4, 6);
        controller.suppressFailurePresentation();
        diagnostics.complete(4, 0);
        controller.destroy();
        controller.destroy();
        controller.onConnectionStarted();

        assertEquals(1, host.dismissCount);
        assertTrue(host.events.isEmpty());
        assertEquals(1, diagnostics.destroyCount);
    }

    @Test
    public void connectedAndPayloadCallbacksRemainNarrowHostOperations() {
        RecordingHost host = new RecordingHost();
        StreamSessionPresentationController controller = create(
                host,
                new ManualDiagnostics(true));

        controller.onConnectionStarted();
        controller.onHdrModeChanged(true, new byte[] {1});
        controller.onNativeCursor(
                true,
                true,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                new byte[] {2});

        assertEquals(1, host.dismissCount);
        assertEquals(
                List.of("connected", "hdr:true", "cursor:true:2:3"),
                host.events);
    }

    private static StreamSessionPresentationController create(
            RecordingHost host,
            ManualDiagnostics diagnostics) {
        return new StreamSessionPresentationController(
                host,
                diagnostics,
                new StreamConnectionMessages(
                        new TestLabels(),
                        flags -> "ports:" + flags,
                        INCONCLUSIVE),
                ignored -> 77,
                INCONCLUSIVE);
    }

    private static final class ManualDiagnostics
            implements StreamSessionPresentationController.Diagnostics {
        private final boolean accepts;
        private Callback callback;
        private int requestCount;
        private int requestedPortFlags;
        private int destroyCount;

        private ManualDiagnostics(boolean accepts) {
            this.accepts = accepts;
        }

        @Override
        public boolean request(int portFlags, Callback callback) {
            requestCount++;
            requestedPortFlags = portFlags;
            if (accepts) {
                this.callback = callback;
            }
            return accepts;
        }

        @Override
        public void destroy() {
            destroyCount++;
            callback = null;
        }

        void complete(int portFlags, int probeResult) {
            Callback pending = callback;
            callback = null;
            if (pending != null) {
                pending.onResult(portFlags, probeResult);
            }
        }
    }

    private static final class RecordingHost
            implements StreamSessionPresentationController.Host {
        private final List<String> events = new ArrayList<>();
        private boolean canPresent = true;
        private boolean surfaceValid;
        private boolean warningsDisabled;
        private int bitrateKbps;
        private int dismissCount;

        @Override
        public boolean canPresentSessionUi() {
            return canPresent;
        }

        @Override
        public void updateConnectingMessage(String message) {
            events.add("connecting:" + message);
        }

        @Override
        public void dismissConnectingIndicator() {
            dismissCount++;
        }

        @Override
        public boolean isRenderSurfaceValid() {
            return surfaceValid;
        }

        @Override
        public void showLongMessage(String message) {
            events.add("message:" + message);
        }

        @Override
        public void showFailureDialog(String title, String message) {
            events.add("dialog:" + title + ":" + message);
        }

        @Override
        public void stopControllerInput() {
            events.add("stop-controller");
        }

        @Override
        public void stopConnection() {
            events.add("stop-connection");
        }

        @Override
        public void finishGracefully() {
            events.add("finish");
        }

        @Override
        public boolean areConnectionWarningsDisabled() {
            return warningsDisabled;
        }

        @Override
        public int getBitrateKbps() {
            return bitrateKbps;
        }

        @Override
        public void setConnectionWarning(
                StreamSessionPresentationController.ConnectionWarning
                        warning) {
            events.add("warning:" + warning);
        }

        @Override
        public void onSessionConnected() {
            events.add("connected");
        }

        @Override
        public void onHdrModeChanged(
                boolean enabled,
                byte[] hdrMetadata) {
            events.add("hdr:" + enabled);
        }

        @Override
        public void onNativeCursor(
                boolean visible,
                boolean shapeChanged,
                int format,
                int x,
                int y,
                int width,
                int height,
                int hotspotX,
                int hotspotY,
                int shapeId,
                int scaleX,
                int scaleY,
                byte[] imageData) {
            events.add("cursor:" + visible + ":" + x + ":" + y);
        }
    }

    private static final class TestLabels
            implements StreamConnectionMessages.Labels {
        @Override
        public String connectionStarting() {
            return "starting";
        }

        @Override
        public String connectionError() {
            return "connection error";
        }

        @Override
        public String connectionErrorTitle() {
            return "connection error title";
        }

        @Override
        public String checkPorts() {
            return "check ports";
        }

        @Override
        public String blockedNetwork() {
            return "blocked";
        }

        @Override
        public String connectionTerminatedTitle() {
            return "termination title";
        }

        @Override
        public String noVideoReceived() {
            return "no video";
        }

        @Override
        public String noFrameReceived() {
            return "no frame";
        }

        @Override
        public String earlyTermination() {
            return "early";
        }

        @Override
        public String frameConversion() {
            return "conversion";
        }

        @Override
        public String connectionTerminated() {
            return "terminated";
        }

        @Override
        public String errorCodePrefix() {
            return "code";
        }

        @Override
        public String videoDecoderInitializationFailed() {
            return "decoder";
        }
    }
}
