package com.limelight.nvstream;

import androidx.annotation.AnyThread;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stable callback endpoint for a stream whose presentation owner changes.
 *
 * <p>The transport binds to this object once. A preparation observer is
 * attached while the session starts, then the stream Activity replaces it.
 * Stateful events that may arrive before handoff are replayed to the new
 * observer in transport order.</p>
 */
public final class StreamSessionEventRelay implements NvConnectionListener {
    private final Object lock = new Object();

    private NvConnectionListener observer;
    private boolean connected;
    private HdrEvent hdrEvent;
    private CursorEvent cursorEvent;

    public StreamSessionEventRelay(NvConnectionListener initialObserver) {
        observer = Objects.requireNonNull(
                initialObserver,
                "initialObserver");
    }

    @AnyThread
    public void attach(NvConnectionListener newObserver) {
        Objects.requireNonNull(newObserver, "newObserver");
        HdrEvent hdrSnapshot;
        CursorEvent cursorSnapshot;
        boolean connectedSnapshot;
        synchronized (lock) {
            observer = newObserver;
            hdrSnapshot = hdrEvent;
            cursorSnapshot = cursorEvent;
            connectedSnapshot = connected;
        }

        // Cursor and HDR may be reported during transport startup. Replay
        // them before connectionStarted() so the presentation owner can cache
        // the initial state before exposing the stream Surface.
        if (hdrSnapshot != null) {
            hdrSnapshot.dispatch(newObserver);
        }
        if (cursorSnapshot != null) {
            cursorSnapshot.dispatch(newObserver);
        }
        if (connectedSnapshot) {
            newObserver.connectionStarted();
        }
    }

    @AnyThread
    public void detach(NvConnectionListener expectedObserver) {
        synchronized (lock) {
            if (observer == expectedObserver) {
                observer = null;
            }
        }
    }

    @Override
    public void stageStarting(String stage) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.stageStarting(stage);
        }
    }

    @Override
    public void stageComplete(String stage) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.stageComplete(stage);
        }
    }

    @Override
    public void stageFailed(String stage, int portFlags, int errorCode) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.stageFailed(stage, portFlags, errorCode);
        }
    }

    @Override
    public void connectionStarted() {
        NvConnectionListener target;
        synchronized (lock) {
            connected = true;
            target = observer;
        }
        if (target != null) {
            target.connectionStarted();
        }
    }

    @Override
    public void connectionTerminated(int errorCode) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.connectionTerminated(errorCode);
        }
    }

    @Override
    public void connectionStatusUpdate(int connectionStatus) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.connectionStatusUpdate(connectionStatus);
        }
    }

    @Override
    public void displayMessage(String message) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.displayMessage(message);
        }
    }

    @Override
    public void displayTransientMessage(String message) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.displayTransientMessage(message);
        }
    }

    @Override
    public void rumble(short controllerNumber, short lowFreqMotor,
                       short highFreqMotor) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.rumble(controllerNumber, lowFreqMotor, highFreqMotor);
        }
    }

    @Override
    public void rumbleTriggers(short controllerNumber, short leftTrigger,
                               short rightTrigger) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.rumbleTriggers(
                    controllerNumber,
                    leftTrigger,
                    rightTrigger);
        }
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        HdrEvent event = new HdrEvent(enabled, copy(hdrMetadata));
        NvConnectionListener target;
        synchronized (lock) {
            hdrEvent = event;
            target = observer;
        }
        if (target != null) {
            event.dispatch(target);
        }
    }

    @Override
    public void setMotionEventState(short controllerNumber, byte motionType,
                                    short reportRateHz) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.setMotionEventState(
                    controllerNumber,
                    motionType,
                    reportRateHz);
        }
    }

    @Override
    public void setControllerLED(short controllerNumber, byte r, byte g,
                                 byte b) {
        NvConnectionListener target = observer();
        if (target != null) {
            target.setControllerLED(controllerNumber, r, g, b);
        }
    }

    @Override
    public void nativeCursor(boolean visible, boolean shapeChanged, int format,
                             int x, int y, int width, int height,
                             int hotspotX, int hotspotY, int shapeId,
                             int scaleX, int scaleY, byte[] imageData) {
        CursorEvent event = new CursorEvent(
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
                copy(imageData));
        NvConnectionListener target;
        synchronized (lock) {
            cursorEvent = event;
            target = observer;
        }
        if (target != null) {
            event.dispatch(target);
        }
    }

    private NvConnectionListener observer() {
        synchronized (lock) {
            return observer;
        }
    }

    private static byte[] copy(byte[] source) {
        return source == null ? null : Arrays.copyOf(source, source.length);
    }

    private static final class HdrEvent {
        private final boolean enabled;
        private final byte[] metadata;

        private HdrEvent(boolean enabled, byte[] metadata) {
            this.enabled = enabled;
            this.metadata = metadata;
        }

        private void dispatch(NvConnectionListener target) {
            target.setHdrMode(enabled, copy(metadata));
        }
    }

    private static final class CursorEvent {
        private final boolean visible;
        private final boolean shapeChanged;
        private final int format;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int hotspotX;
        private final int hotspotY;
        private final int shapeId;
        private final int scaleX;
        private final int scaleY;
        private final byte[] imageData;

        private CursorEvent(
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
            this.visible = visible;
            this.shapeChanged = shapeChanged;
            this.format = format;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.shapeId = shapeId;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.imageData = imageData;
        }

        private void dispatch(NvConnectionListener target) {
            target.nativeCursor(
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
                    copy(imageData));
        }
    }
}
