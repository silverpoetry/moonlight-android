package com.limelight.nvstream;

import androidx.annotation.AnyThread;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Owns the lifecycle of one stream connection and exposes one authoritative
 * session state to the UI.
 */
public final class StreamSessionController implements NvConnectionListener {
    private final Object stateLock = new Object();
    private final StreamSessionConnection connection;
    private final Executor stopExecutor;

    private volatile SessionState state = SessionState.CREATED;
    private NvConnectionListener delegate;
    private boolean startRequested;
    private boolean stopScheduled;
    private boolean destroyed;

    public StreamSessionController(NvConnection connection,
                                   NvConnectionListener delegate) {
        this(connection, delegate, command -> {
            Thread thread = new Thread(command, "StreamSessionStop");
            thread.start();
        });
    }

    StreamSessionController(StreamSessionConnection connection,
                            NvConnectionListener delegate,
                            Executor stopExecutor) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.stopExecutor = Objects.requireNonNull(stopExecutor, "stopExecutor");
    }

    @AnyThread
    public SessionState getState() {
        return state;
    }

    @AnyThread
    public boolean canStart() {
        synchronized (stateLock) {
            return !destroyed && state == SessionState.CREATED;
        }
    }

    @AnyThread
    public boolean hasStartBeenRequested() {
        synchronized (stateLock) {
            return startRequested;
        }
    }

    @AnyThread
    public boolean start(AudioRenderer audioRenderer,
                         VideoDecoderRenderer videoDecoderRenderer) {
        synchronized (stateLock) {
            if (destroyed || state != SessionState.CREATED) {
                return false;
            }
            startRequested = true;
            state = SessionState.STARTING;
            try {
                connection.start(audioRenderer, videoDecoderRenderer, this);
                return true;
            } catch (RuntimeException | Error error) {
                state = SessionState.FAILED;
                throw error;
            }
        }
    }

    /**
     * Schedules connection cleanup exactly once.
     *
     * @return {@code true} if this call initiated cleanup
     */
    @AnyThread
    public boolean stop() {
        synchronized (stateLock) {
            if (!state.needsStop() || stopScheduled) {
                return false;
            }
            stopScheduled = true;
            state = SessionState.STOPPING;
        }

        try {
            stopExecutor.execute(() -> {
                try {
                    connection.stop();
                    synchronized (stateLock) {
                        state = SessionState.STOPPED;
                    }
                } catch (RuntimeException | Error error) {
                    synchronized (stateLock) {
                        state = SessionState.FAILED;
                    }
                    LimeLog.severe(
                            "Stream session cleanup failed: " + error);
                }
            });
            return true;
        } catch (RuntimeException | Error error) {
            synchronized (stateLock) {
                state = SessionState.FAILED;
                stopScheduled = false;
            }
            LimeLog.severe(
                    "Unable to schedule stream session cleanup: " + error);
            return false;
        }
    }

    /**
     * Detaches the UI owner and guarantees transport cleanup is requested.
     * Calls made after destruction are idempotent.
     */
    @AnyThread
    public void destroy() {
        synchronized (stateLock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            delegate = null;
        }
        stop();
    }

    @Override
    public void stageStarting(String stage) {
        NvConnectionListener currentDelegate;
        synchronized (stateLock) {
            currentDelegate = getDelegateLocked(
                    SessionState.STARTING);
            if (currentDelegate == null) {
                return;
            }
        }
        currentDelegate.stageStarting(stage);
    }

    @Override
    public void stageComplete(String stage) {
        NvConnectionListener currentDelegate;
        synchronized (stateLock) {
            currentDelegate = getDelegateLocked(
                    SessionState.STARTING);
            if (currentDelegate == null) {
                return;
            }
        }
        currentDelegate.stageComplete(stage);
    }

    @Override
    public void stageFailed(String stage, int portFlags, int errorCode) {
        NvConnectionListener currentDelegate;
        synchronized (stateLock) {
            currentDelegate = getDelegateLocked(
                    SessionState.STARTING);
            if (currentDelegate == null) {
                return;
            }
            state = SessionState.FAILED;
        }
        currentDelegate.stageFailed(stage, portFlags, errorCode);
    }

    @Override
    public void connectionStarted() {
        NvConnectionListener currentDelegate;
        synchronized (stateLock) {
            currentDelegate = getDelegateLocked(
                    SessionState.STARTING);
            if (currentDelegate == null) {
                return;
            }
            state = SessionState.STREAMING;
        }
        currentDelegate.connectionStarted();
    }

    @Override
    public void connectionTerminated(int errorCode) {
        NvConnectionListener currentDelegate;
        synchronized (stateLock) {
            currentDelegate = getDelegateLocked(
                    SessionState.STARTING,
                    SessionState.STREAMING);
            if (currentDelegate == null) {
                return;
            }
            state = SessionState.TERMINATED;
        }
        currentDelegate.connectionTerminated(errorCode);
    }

    @Override
    public void connectionStatusUpdate(int connectionStatus) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.connectionStatusUpdate(connectionStatus);
        }
    }

    @Override
    public void displayMessage(String message) {
        NvConnectionListener currentDelegate =
                getActiveDelegate();
        if (currentDelegate != null) {
            currentDelegate.displayMessage(message);
        }
    }

    @Override
    public void displayTransientMessage(String message) {
        NvConnectionListener currentDelegate =
                getActiveDelegate();
        if (currentDelegate != null) {
            currentDelegate.displayTransientMessage(message);
        }
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor,
                       short highFreqMotor) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.rumble(controllerNumber, lowFreqMotor,
                    highFreqMotor);
        }
    }

    @Override
    public void rumbleTriggers(short controllerNumber, short leftTrigger,
                               short rightTrigger) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.rumbleTriggers(controllerNumber, leftTrigger,
                    rightTrigger);
        }
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.setHdrMode(enabled, hdrMetadata);
        }
    }

    @Override
    public void setMotionEventState(short controllerNumber, byte motionType,
                                    short reportRateHz) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.setMotionEventState(controllerNumber,
                    motionType, reportRateHz);
        }
    }

    @Override
    public void setControllerLED(short controllerNumber, byte r, byte g,
                                 byte b) {
        NvConnectionListener currentDelegate =
                getStreamingDelegate();
        if (currentDelegate != null) {
            currentDelegate.setControllerLED(controllerNumber, r, g, b);
        }
    }

    @Override
    public void nativeCursor(boolean visible, boolean shapeChanged, int format,
                             int x, int y, int width, int height,
                             int hotspotX, int hotspotY, int shapeId,
                             int scaleX, int scaleY, byte[] imageData) {
        // Sunshine sends the initial cursor shape while the transport is still
        // starting. The presentation layer caches it until the stream surface
        // becomes visible, so dropping it here leaves the cursor without a shape
        // until the host later changes it.
        NvConnectionListener currentDelegate =
                getActiveDelegate();
        if (currentDelegate != null) {
            currentDelegate.nativeCursor(visible, shapeChanged, format,
                    x, y, width, height, hotspotX, hotspotY, shapeId,
                    scaleX, scaleY, imageData);
        }
    }

    private NvConnectionListener getActiveDelegate() {
        synchronized (stateLock) {
            return getDelegateLocked(
                    SessionState.STARTING,
                    SessionState.STREAMING);
        }
    }

    private NvConnectionListener getStreamingDelegate() {
        synchronized (stateLock) {
            return getDelegateLocked(SessionState.STREAMING);
        }
    }

    private NvConnectionListener getDelegateLocked(
            SessionState acceptedState) {
        if (destroyed || delegate == null) {
            return null;
        }
        return state == acceptedState ? delegate : null;
    }

    private NvConnectionListener getDelegateLocked(
            SessionState firstAcceptedState,
            SessionState secondAcceptedState) {
        if (destroyed || delegate == null) {
            return null;
        }
        return state == firstAcceptedState ||
                state == secondAcceptedState
                ? delegate
                : null;
    }
}
