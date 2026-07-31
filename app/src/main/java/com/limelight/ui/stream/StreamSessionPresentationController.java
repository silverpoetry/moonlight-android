package com.limelight.ui.stream;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/**
 * Owns presentation policy for one stream session.
 *
 * <p>The callback router guarantees main-thread serialization. This owner
 * guarantees that at most one terminal failure is presented, keeps bounded
 * diagnostics inside the session lifecycle, and translates transport events
 * into narrow platform presentation operations.</p>
 */
public final class StreamSessionPresentationController
        implements StreamSessionCallbackRouter.UiHost {
    public enum ConnectionWarning {
        NONE,
        POOR,
        SLOW
    }

    public interface Diagnostics {
        interface Callback {
            void onResult(int portFlags, int probeResult);
        }

        boolean request(int portFlags, Callback callback);

        void destroy();
    }

    public interface TerminationPortResolver {
        int resolve(int errorCode);
    }

    public interface Host {
        boolean canPresentSessionUi();

        void updateConnectingMessage(String message);

        void dismissConnectingIndicator();

        boolean isRenderSurfaceValid();

        void showLongMessage(String message);

        void showFailureDialog(String title, String message);

        void stopControllerInput();

        void stopConnection();

        void finishGracefully();

        boolean areConnectionWarningsDisabled();

        int getBitrateKbps();

        void setConnectionWarning(ConnectionWarning warning);

        void onSessionConnected();

        void onHdrModeChanged(boolean enabled, byte[] hdrMetadata);

        void onNativeCursor(
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
                byte[] imageData);
    }

    private final Host host;
    private final Diagnostics diagnostics;
    private final StreamConnectionMessages messages;
    private final TerminationPortResolver terminationPortResolver;
    private final int inconclusiveProbeResult;

    private boolean failurePresented;
    private boolean failurePresentationSuppressed;
    private boolean destroyed;

    public StreamSessionPresentationController(
            Host host,
            Diagnostics diagnostics,
            StreamConnectionMessages messages,
            TerminationPortResolver terminationPortResolver,
            int inconclusiveProbeResult) {
        this.host = Objects.requireNonNull(host, "host");
        this.diagnostics = Objects.requireNonNull(
                diagnostics,
                "diagnostics");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.terminationPortResolver = Objects.requireNonNull(
                terminationPortResolver,
                "terminationPortResolver");
        this.inconclusiveProbeResult = inconclusiveProbeResult;
    }

    @MainThread
    @Override
    public void onStageStarting(String stage) {
        if (!destroyed) {
            host.updateConnectingMessage(messages.stageStarting(stage));
        }
    }

    @MainThread
    @Override
    public void onStageFailed(
            String stage,
            int portFlags,
            int errorCode) {
        if (!canPresent()) {
            return;
        }

        host.dismissConnectingIndicator();
        if (failurePresented) {
            return;
        }
        failurePresented = true;
        LimeLog.severe(stage + " failed: " + errorCode);

        if (stage.contains("video") && host.isRenderSurfaceValid()) {
            host.showLongMessage(
                    messages.videoDecoderInitializationFailed());
        }

        if (!diagnostics.request(
                portFlags,
                (diagnosedFlags, probeResult) -> presentStageFailure(
                        stage,
                        errorCode,
                        diagnosedFlags,
                        probeResult))) {
            presentStageFailure(
                    stage,
                    errorCode,
                    portFlags,
                    inconclusiveProbeResult);
        }
    }

    @MainThread
    @Override
    public void onConnectionStarted() {
        if (destroyed) {
            return;
        }
        host.dismissConnectingIndicator();
        host.onSessionConnected();
    }

    @MainThread
    @Override
    public void onConnectionTerminated(int errorCode) {
        int portFlags = terminationPortResolver.resolve(errorCode);
        if (!canPresent()) {
            return;
        }

        host.stopControllerInput();
        if (failurePresented) {
            return;
        }

        failurePresented = true;
        LimeLog.severe("Connection terminated: " + errorCode);
        host.stopConnection();

        if (errorCode == MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
            host.finishGracefully();
            return;
        }

        if (!diagnostics.request(
                portFlags,
                (diagnosedFlags, probeResult) ->
                        presentTerminationFailure(
                                errorCode,
                                diagnosedFlags,
                                probeResult))) {
            presentTerminationFailure(
                    errorCode,
                    portFlags,
                    inconclusiveProbeResult);
        }
    }

    @MainThread
    @Override
    public void onConnectionStatusUpdate(int connectionStatus) {
        if (destroyed || host.areConnectionWarningsDisabled()) {
            return;
        }

        if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
            host.setConnectionWarning(host.getBitrateKbps() > 5000
                    ? ConnectionWarning.SLOW
                    : ConnectionWarning.POOR);
        }
        else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
            host.setConnectionWarning(ConnectionWarning.NONE);
        }
    }

    @MainThread
    @Override
    public void onMessage(String message, boolean transientMessage) {
        if (!destroyed &&
                (!transientMessage ||
                        !host.areConnectionWarningsDisabled())) {
            host.showLongMessage(message);
        }
    }

    @MainThread
    @Override
    public void onHdrModeChanged(boolean enabled, byte[] hdrMetadata) {
        if (!destroyed) {
            host.onHdrModeChanged(enabled, hdrMetadata);
        }
    }

    @MainThread
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
        if (!destroyed) {
            host.onNativeCursor(
                    visible,
                    shapeChanged,
                    format,
                    x,
                    y,
                    width,
                    height,
                    hotspotX,
                    hotspotY,
                    shapeId,
                    scaleX,
                    scaleY,
                    imageData);
        }
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        diagnostics.destroy();
    }

    /** Prevents an owner-initiated stop from presenting as a failure. */
    @MainThread
    public void suppressFailurePresentation() {
        if (!destroyed) {
            failurePresented = true;
            failurePresentationSuppressed = true;
        }
    }

    private boolean canPresent() {
        return !destroyed && host.canPresentSessionUi();
    }

    private void presentStageFailure(
            String stage,
            int errorCode,
            int portFlags,
            int probeResult) {
        if (!canPresent() || failurePresentationSuppressed) {
            return;
        }
        host.showFailureDialog(
                messages.connectionErrorTitle(),
                messages.stageFailure(
                        stage,
                        errorCode,
                        portFlags,
                        probeResult));
    }

    private void presentTerminationFailure(
            int errorCode,
            int portFlags,
            int probeResult) {
        if (!canPresent() || failurePresentationSuppressed) {
            return;
        }
        host.showFailureDialog(
                messages.connectionTerminatedTitle(),
                messages.terminationFailure(
                        errorCode,
                        portFlags,
                        probeResult));
    }
}
