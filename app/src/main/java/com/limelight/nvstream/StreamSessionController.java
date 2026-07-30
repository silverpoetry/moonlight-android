package com.limelight.nvstream;

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
    private final NvConnectionListener delegate;
    private final Executor stopExecutor;

    private volatile SessionState state = SessionState.CREATED;
    private boolean stopScheduled;

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

    public SessionState getState() {
        return state;
    }

    public boolean start(AudioRenderer audioRenderer,
                         VideoDecoderRenderer videoDecoderRenderer) {
        synchronized (stateLock) {
            if (state != SessionState.CREATED) {
                return false;
            }
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
                    throw error;
                }
            });
            return true;
        } catch (RuntimeException | Error error) {
            synchronized (stateLock) {
                state = SessionState.FAILED;
                stopScheduled = false;
            }
            throw error;
        }
    }

    @Override
    public void stageStarting(String stage) {
        synchronized (stateLock) {
            if (state != SessionState.STARTING) {
                return;
            }
        }
        delegate.stageStarting(stage);
    }

    @Override
    public void stageComplete(String stage) {
        synchronized (stateLock) {
            if (state != SessionState.STARTING) {
                return;
            }
        }
        delegate.stageComplete(stage);
    }

    @Override
    public void stageFailed(String stage, int portFlags, int errorCode) {
        synchronized (stateLock) {
            if (state != SessionState.STARTING) {
                return;
            }
            state = SessionState.FAILED;
        }
        delegate.stageFailed(stage, portFlags, errorCode);
    }

    @Override
    public void connectionStarted() {
        synchronized (stateLock) {
            if (state != SessionState.STARTING) {
                return;
            }
            state = SessionState.STREAMING;
        }
        delegate.connectionStarted();
    }

    @Override
    public void connectionTerminated(int errorCode) {
        synchronized (stateLock) {
            if (state == SessionState.CREATED ||
                    state == SessionState.STOPPED) {
                return;
            }
            if (state == SessionState.STARTING ||
                    state == SessionState.STREAMING) {
                state = SessionState.TERMINATED;
            }
        }
        delegate.connectionTerminated(errorCode);
    }

    @Override
    public void connectionStatusUpdate(int connectionStatus) {
        delegate.connectionStatusUpdate(connectionStatus);
    }

    @Override
    public void displayMessage(String message) {
        delegate.displayMessage(message);
    }

    @Override
    public void displayTransientMessage(String message) {
        delegate.displayTransientMessage(message);
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor,
                       short highFreqMotor) {
        delegate.rumble(controllerNumber, lowFreqMotor, highFreqMotor);
    }

    @Override
    public void rumbleTriggers(short controllerNumber, short leftTrigger,
                               short rightTrigger) {
        delegate.rumbleTriggers(controllerNumber, leftTrigger, rightTrigger);
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        delegate.setHdrMode(enabled, hdrMetadata);
    }

    @Override
    public void setMotionEventState(short controllerNumber, byte motionType,
                                    short reportRateHz) {
        delegate.setMotionEventState(controllerNumber, motionType,
                reportRateHz);
    }

    @Override
    public void setControllerLED(short controllerNumber, byte r, byte g,
                                 byte b) {
        delegate.setControllerLED(controllerNumber, r, g, b);
    }

    @Override
    public void nativeCursor(boolean visible, boolean shapeChanged, int format,
                             int x, int y, int width, int height,
                             int hotspotX, int hotspotY, int shapeId,
                             int scaleX, int scaleY, byte[] imageData) {
        delegate.nativeCursor(visible, shapeChanged, format, x, y,
                width, height, hotspotX, hotspotY, shapeId, scaleX, scaleY,
                imageData);
    }
}
