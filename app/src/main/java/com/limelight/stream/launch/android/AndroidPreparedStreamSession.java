package com.limelight.stream.launch.android;

import android.os.Handler;
import android.view.Surface;

import androidx.annotation.MainThread;

import com.limelight.binding.video.DecoderCrashTracker;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.binding.video.PerfOverlayRelay;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.SessionState;
import com.limelight.nvstream.StreamSessionController;
import com.limelight.nvstream.StreamSessionEventRelay;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.controller.ControllerSettingsState;
import com.limelight.settings.input.InputSettingsState;
import com.limelight.settings.stream.StreamDecoderSettings;
import com.limelight.settings.stream.StreamDisplaySettings;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsState;
import com.limelight.stream.launch.StreamLaunchRequest;
import com.limelight.ui.stream.StreamHdrRequestPolicy;
import com.limelight.ui.stream.StreamMediaResourceOwner;
import com.limelight.ui.stream.StreamSessionConfigurationPlanner;
import com.limelight.ui.stream.StreamWifiLockController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One prepared stream whose transport and decoder outlive either UI page.
 *
 * <p>The session starts on an off-screen Surface. Ownership is transferred
 * exactly once to the stream Activity, which attaches its real Surface and
 * callback sinks without restarting the transport.</p>
 */
public final class AndroidPreparedStreamSession implements AutoCloseable {
    public interface Listener {
        void onProgress(String message);

        void onReady(AndroidPreparedStreamSession session);

        void onFailure(
                AndroidPreparedStreamSession session,
                Failure failure);
    }

    public static final class Failure {
        private final String stage;
        private final int portFlags;
        private final int errorCode;
        private final String detail;

        private Failure(
                String stage,
                int portFlags,
                int errorCode,
                String detail) {
            this.stage = stage;
            this.portFlags = portFlags;
            this.errorCode = errorCode;
            this.detail = detail;
        }

        public String getStage() {
            return stage;
        }

        public int getPortFlags() {
            return portFlags;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getDetail() {
            return detail;
        }

        static Failure handoffTimedOut() {
            return new Failure("handoff", 0, 0, null);
        }
    }

    public enum State {
        CREATED,
        PREPARING,
        READY,
        ATTACHED,
        CLOSED,
        FAILED
    }

    private final Object lock = new Object();
    private final String token;
    private final StreamLaunchRequest request;
    private final Handler mainHandler;
    private final Listener listener;
    private final SettingsRepository settingsRepository;
    private final StreamVideoSettings videoSettings;
    private final StreamVideoSettingsState videoSettingsState;
    private final StreamAudioSettingsState audioSettingsState;
    private final StreamUiSettingsState uiSettingsState;
    private final StreamDisplaySettings displaySettings;
    private final StreamDecoderSettings decoderSettings;
    private final TransferSettings transferSettings;
    private final InputSettingsState inputSettingsState;
    private final ControllerSettingsState controllerSettingsState;
    private final VirtualControlSettingsState virtualControlSettingsState;
    private final StreamDecoderSettings.FramePacing effectiveFramePacing;
    private final List<StreamSessionConfigurationPlanner.Warning> warnings;
    private final StreamHdrRequestPolicy.Warning hdrWarning;
    private final NvConnection connection;
    private final StreamMediaResourceOwner mediaResourceOwner;
    private final DecoderCrashTracker decoderCrashTracker;
    private final AndroidStreamStagingSurface stagingSurface;
    private final PerfOverlayRelay performanceRelay;
    private final StreamWifiLockController wifiLockController;
    private final StreamSessionEventRelay eventRelay;
    private final StreamSessionController sessionController;

    private State state = State.CREATED;
    private String latestMessage;

    AndroidPreparedStreamSession(
            String token,
            StreamLaunchRequest request,
            Handler mainHandler,
            Listener listener,
            SettingsRepository settingsRepository,
            StreamVideoSettings videoSettings,
            StreamVideoSettingsState videoSettingsState,
            StreamAudioSettingsState audioSettingsState,
            StreamUiSettingsState uiSettingsState,
            StreamDisplaySettings displaySettings,
            StreamDecoderSettings decoderSettings,
            TransferSettings transferSettings,
            InputSettingsState inputSettingsState,
            ControllerSettingsState controllerSettingsState,
            VirtualControlSettingsState virtualControlSettingsState,
            StreamDecoderSettings.FramePacing effectiveFramePacing,
            List<StreamSessionConfigurationPlanner.Warning> warnings,
            StreamHdrRequestPolicy.Warning hdrWarning,
            NvConnection connection,
            StreamMediaResourceOwner mediaResourceOwner,
            DecoderCrashTracker decoderCrashTracker,
            AndroidStreamStagingSurface stagingSurface,
            PerfOverlayRelay performanceRelay,
            StreamWifiLockController wifiLockController) {
        this.token = Objects.requireNonNull(token, "token");
        this.request = Objects.requireNonNull(request, "request");
        this.mainHandler = Objects.requireNonNull(mainHandler, "mainHandler");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.settingsRepository = Objects.requireNonNull(
                settingsRepository,
                "settingsRepository");
        this.videoSettings = Objects.requireNonNull(
                videoSettings,
                "videoSettings");
        this.videoSettingsState = Objects.requireNonNull(
                videoSettingsState,
                "videoSettingsState");
        this.audioSettingsState = Objects.requireNonNull(
                audioSettingsState,
                "audioSettingsState");
        this.uiSettingsState = Objects.requireNonNull(
                uiSettingsState,
                "uiSettingsState");
        this.displaySettings = Objects.requireNonNull(
                displaySettings,
                "displaySettings");
        this.decoderSettings = Objects.requireNonNull(
                decoderSettings,
                "decoderSettings");
        this.transferSettings = Objects.requireNonNull(
                transferSettings,
                "transferSettings");
        this.inputSettingsState = Objects.requireNonNull(
                inputSettingsState,
                "inputSettingsState");
        this.controllerSettingsState = Objects.requireNonNull(
                controllerSettingsState,
                "controllerSettingsState");
        this.virtualControlSettingsState = Objects.requireNonNull(
                virtualControlSettingsState,
                "virtualControlSettingsState");
        this.effectiveFramePacing = Objects.requireNonNull(
                effectiveFramePacing,
                "effectiveFramePacing");
        this.warnings = Collections.unmodifiableList(
                new ArrayList<>(warnings));
        this.hdrWarning = Objects.requireNonNull(hdrWarning, "hdrWarning");
        this.connection = Objects.requireNonNull(connection, "connection");
        this.mediaResourceOwner = Objects.requireNonNull(
                mediaResourceOwner,
                "mediaResourceOwner");
        this.decoderCrashTracker = Objects.requireNonNull(
                decoderCrashTracker,
                "decoderCrashTracker");
        this.stagingSurface = Objects.requireNonNull(
                stagingSurface,
                "stagingSurface");
        this.performanceRelay = Objects.requireNonNull(
                performanceRelay,
                "performanceRelay");
        this.wifiLockController = Objects.requireNonNull(
                wifiLockController,
                "wifiLockController");
        eventRelay = new StreamSessionEventRelay(
                new PreparationObserver());
        sessionController = new StreamSessionController(
                connection,
                eventRelay);
    }

    @MainThread
    void start() {
        synchronized (lock) {
            requireState(State.CREATED);
            state = State.PREPARING;
        }
        StreamMediaResourceOwner.StartResources resources =
                mediaResourceOwner.prepareStart(
                        stagingSurface.getSurface());
        try {
            if (!sessionController.start(
                    resources.getAudioRenderer(),
                    resources.getVideoRenderer())) {
                throw new IllegalStateException(
                        "Prepared stream transport rejected startup");
            }
        }
        catch (RuntimeException | Error failure) {
            synchronized (lock) {
                state = State.FAILED;
            }
            mediaResourceOwner.releaseStartResources();
            throw failure;
        }
    }

    @MainThread
    public void attach(
            Surface renderTarget,
            NvConnectionListener streamObserver,
            PerfOverlayListener performanceObserver) {
        Objects.requireNonNull(renderTarget, "renderTarget");
        Objects.requireNonNull(streamObserver, "streamObserver");
        Objects.requireNonNull(
                performanceObserver,
                "performanceObserver");
        synchronized (lock) {
            requireState(State.READY);
            state = State.ATTACHED;
        }
        mediaResourceOwner.setRenderTarget(renderTarget);
        performanceRelay.attach(performanceObserver);
        eventRelay.attach(streamObserver);
        stagingSurface.close();
    }

    @MainThread
    public void detach(
            NvConnectionListener streamObserver,
            PerfOverlayListener performanceObserver) {
        eventRelay.detach(streamObserver);
        performanceRelay.detach(performanceObserver);
    }

    @Override
    @MainThread
    public void close() {
        synchronized (lock) {
            if (state == State.CLOSED) {
                return;
            }
            state = State.CLOSED;
        }
        mediaResourceOwner.prepareVideoForStop();
        sessionController.destroy();
        mediaResourceOwner.releaseStartResources();
        mediaResourceOwner.destroy();
        stagingSurface.close();
        wifiLockController.destroy();
    }

    public String getToken() {
        return token;
    }

    public StreamLaunchRequest getRequest() {
        return request;
    }

    public State getState() {
        synchronized (lock) {
            return state;
        }
    }

    public boolean isReadyForAttach() {
        synchronized (lock) {
            return state == State.READY &&
                    sessionController.getState() == SessionState.STREAMING;
        }
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public StreamVideoSettings getVideoSettings() {
        return videoSettings;
    }

    public StreamVideoSettingsState getVideoSettingsState() {
        return videoSettingsState;
    }

    public StreamAudioSettingsState getAudioSettingsState() {
        return audioSettingsState;
    }

    public StreamUiSettingsState getUiSettingsState() {
        return uiSettingsState;
    }

    public StreamDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    public StreamDecoderSettings getDecoderSettings() {
        return decoderSettings;
    }

    public TransferSettings getTransferSettings() {
        return transferSettings;
    }

    public InputSettingsState getInputSettingsState() {
        return inputSettingsState;
    }

    public ControllerSettingsState getControllerSettingsState() {
        return controllerSettingsState;
    }

    public VirtualControlSettingsState getVirtualControlSettingsState() {
        return virtualControlSettingsState;
    }

    public StreamDecoderSettings.FramePacing getEffectiveFramePacing() {
        return effectiveFramePacing;
    }

    public List<StreamSessionConfigurationPlanner.Warning> getWarnings() {
        return warnings;
    }

    public StreamHdrRequestPolicy.Warning getHdrWarning() {
        return hdrWarning;
    }

    public NvConnection getConnection() {
        return connection;
    }

    public StreamMediaResourceOwner getMediaResourceOwner() {
        return mediaResourceOwner;
    }

    public DecoderCrashTracker getDecoderCrashTracker() {
        return decoderCrashTracker;
    }

    public StreamSessionController getSessionController() {
        return sessionController;
    }

    public StreamWifiLockController getWifiLockController() {
        return wifiLockController;
    }

    private void requireState(State expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Expected " + expected + " but was " + state);
        }
    }

    private final class PreparationObserver
            implements NvConnectionListener {
        @Override
        public void stageStarting(String stage) {
            postProgress(stage);
        }

        @Override
        public void stageComplete(String stage) {
        }

        @Override
        public void stageFailed(
                String stage,
                int portFlags,
                int errorCode) {
            postFailure(stage, portFlags, errorCode);
        }

        @Override
        public void connectionStarted() {
            mainHandler.post(() -> {
                synchronized (lock) {
                    if (state != State.PREPARING) {
                        return;
                    }
                    state = State.READY;
                }
                listener.onReady(AndroidPreparedStreamSession.this);
            });
        }

        @Override
        public void connectionTerminated(int errorCode) {
            postFailure("connection", 0, errorCode);
        }

        @Override
        public void connectionStatusUpdate(int connectionStatus) {
        }

        @Override
        public void displayMessage(String message) {
            synchronized (lock) {
                latestMessage = message;
            }
        }

        @Override
        public void displayTransientMessage(String message) {
            synchronized (lock) {
                latestMessage = message;
            }
        }

        @Override
        public void rumble(short controllerNumber, short lowFreqMotor,
                           short highFreqMotor) {
        }

        @Override
        public void rumbleTriggers(short controllerNumber,
                                   short leftTrigger,
                                   short rightTrigger) {
        }

        @Override
        public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        }

        @Override
        public void setMotionEventState(short controllerNumber,
                                        byte motionType,
                                        short reportRateHz) {
        }

        @Override
        public void setControllerLED(short controllerNumber, byte r,
                                     byte g, byte b) {
        }

        @Override
        public void nativeCursor(
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
        }
    }

    private void postProgress(String stage) {
        mainHandler.post(() -> {
            synchronized (lock) {
                if (state != State.PREPARING) {
                    return;
                }
            }
            listener.onProgress(stage);
        });
    }

    private void postFailure(
            String stage,
            int portFlags,
            int errorCode) {
        mainHandler.post(() -> {
            String detail;
            synchronized (lock) {
                if (state != State.PREPARING && state != State.READY) {
                    return;
                }
                state = State.FAILED;
                detail = latestMessage;
            }
            listener.onFailure(
                    this,
                    new Failure(stage, portFlags, errorCode, detail));
        });
    }
}
