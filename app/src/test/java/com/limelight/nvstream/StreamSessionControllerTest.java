package com.limelight.nvstream;

import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamSessionControllerTest {
    @Test
    public void startIsSingleUseAndConnectionCallbackOwnsStreamingState() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, Runnable::run);

        assertEquals(SessionState.CREATED, controller.getState());
        assertTrue(controller.canStart());
        assertFalse(controller.hasStartBeenRequested());
        assertTrue(controller.start(null, null));
        assertEquals(SessionState.STARTING, controller.getState());
        assertFalse(controller.canStart());
        assertTrue(controller.hasStartBeenRequested());
        assertEquals(1, connection.startCount);
        assertFalse(controller.start(null, null));

        connection.listener.connectionStarted();

        assertEquals(SessionState.STREAMING, controller.getState());
        assertEquals(1, listener.connectionStartedCount);
    }

    @Test
    public void stopIsScheduledOnceAndRejectsLateStartedCallback() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);

        assertTrue(controller.stop());
        assertEquals(SessionState.STOPPING, controller.getState());
        assertFalse(controller.stop());
        assertEquals(0, connection.stopCount);

        connection.listener.connectionStarted();
        connection.listener.stageFailed("late", 0, -1);
        assertEquals(0, listener.connectionStartedCount);
        assertEquals(0, listener.stageFailedCount);
        assertEquals(SessionState.STOPPING, controller.getState());

        stopExecutor.runNext();
        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.STOPPED, controller.getState());
    }

    @Test
    public void terminationRequiresExplicitCleanup() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);
        connection.listener.connectionStarted();

        connection.listener.connectionTerminated(42);

        assertEquals(SessionState.TERMINATED, controller.getState());
        assertEquals(1, listener.connectionTerminatedCount);
        assertTrue(controller.stop());

        stopExecutor.runNext();
        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.STOPPED, controller.getState());
    }

    @Test
    public void failedStartStillAllowsConnectionCleanup() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);

        connection.listener.stageFailed("video", 1, -2);

        assertEquals(SessionState.FAILED, controller.getState());
        assertEquals(1, listener.stageFailedCount);
        assertTrue(controller.stop());

        stopExecutor.runNext();
        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.STOPPED, controller.getState());
    }

    @Test
    public void stopBeforeStartStillCleansSingleUseTransport() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);

        assertTrue(controller.stop());
        assertEquals(SessionState.STOPPING, controller.getState());
        assertFalse(controller.hasStartBeenRequested());
        assertFalse(controller.start(null, null));
        assertFalse(controller.stop());

        stopExecutor.runNext();

        assertEquals(0, connection.startCount);
        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.STOPPED, controller.getState());
    }

    @Test
    public void destroyDetachesOwnerAndRequestsCleanupExactlyOnce() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);

        controller.destroy();
        controller.destroy();
        connection.listener.stageStarting("late");
        connection.listener.stageFailed("late", 1, 2);
        connection.listener.displayMessage("late");
        connection.listener.connectionTerminated(3);

        assertEquals(0, listener.stageStartingCount);
        assertEquals(0, listener.stageFailedCount);
        assertEquals(0, listener.displayMessageCount);
        assertEquals(0, listener.connectionTerminatedCount);
        assertEquals(SessionState.STOPPING, controller.getState());

        stopExecutor.runNext();

        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.STOPPED, controller.getState());
    }

    @Test
    public void stopRejectsAllLateRuntimeCallbacks() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);
        connection.listener.connectionStarted();

        assertTrue(controller.stop());
        connection.listener.connectionStatusUpdate(1);
        connection.listener.displayMessage("late");
        connection.listener.displayTransientMessage("late");
        connection.listener.rumble((short) 0, (short) 1, (short) 2);
        connection.listener.rumbleTriggers(
                (short) 0, (short) 1, (short) 2);
        connection.listener.setHdrMode(true, new byte[0]);
        connection.listener.setMotionEventState(
                (short) 0, (byte) 1, (short) 120);
        connection.listener.setControllerLED(
                (short) 0, (byte) 1, (byte) 2, (byte) 3);
        connection.listener.nativeCursor(
                true, false, 0, 1, 2, 3, 4,
                0, 0, 1, 65536, 65536, new byte[0]);
        connection.listener.connectionTerminated(4);

        assertEquals(0, listener.connectionStatusUpdateCount);
        assertEquals(0, listener.displayMessageCount);
        assertEquals(0, listener.displayTransientMessageCount);
        assertEquals(0, listener.rumbleCount);
        assertEquals(0, listener.rumbleTriggersCount);
        assertEquals(0, listener.hdrModeCount);
        assertEquals(0, listener.motionStateCount);
        assertEquals(0, listener.controllerLedCount);
        assertEquals(0, listener.nativeCursorCount);
        assertEquals(0, listener.connectionTerminatedCount);
        assertEquals(SessionState.STOPPING, controller.getState());
    }

    @Test
    public void cleanupFailureIsContainedAndTerminal() {
        FakeConnection connection = new FakeConnection();
        connection.stopFailure =
                new IllegalStateException("cleanup failed");
        RecordingListener listener = new RecordingListener();
        QueuedExecutor stopExecutor = new QueuedExecutor();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, stopExecutor);
        controller.start(null, null);

        assertTrue(controller.stop());
        stopExecutor.runNext();

        assertEquals(1, connection.stopCount);
        assertEquals(SessionState.FAILED, controller.getState());
        assertFalse(controller.stop());
    }

    @Test
    public void callbacksAreForwardedOnlyInTheirOwningState() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, Runnable::run);
        controller.start(null, null);

        connection.listener.stageStarting("video");
        connection.listener.displayMessage("starting");
        connection.listener.connectionStatusUpdate(1);
        connection.listener.rumble((short) 0, (short) 1, (short) 2);

        assertEquals(1, listener.stageStartingCount);
        assertEquals(1, listener.displayMessageCount);
        assertEquals(0, listener.connectionStatusUpdateCount);
        assertEquals(0, listener.rumbleCount);

        connection.listener.connectionStarted();
        connection.listener.stageStarting("late");
        connection.listener.connectionStatusUpdate(1);
        connection.listener.displayTransientMessage("streaming");
        connection.listener.rumble((short) 0, (short) 1, (short) 2);
        connection.listener.setHdrMode(true, new byte[0]);

        assertEquals(1, listener.stageStartingCount);
        assertEquals(1, listener.connectionStatusUpdateCount);
        assertEquals(1, listener.displayTransientMessageCount);
        assertEquals(1, listener.rumbleCount);
        assertEquals(1, listener.hdrModeCount);
    }

    @Test
    public void nativeCursorIsForwardedWhileStartingToPreserveItsInitialShape() {
        FakeConnection connection = new FakeConnection();
        RecordingListener listener = new RecordingListener();
        StreamSessionController controller = new StreamSessionController(
                connection, listener, Runnable::run);
        controller.start(null, null);

        connection.listener.nativeCursor(
                true, true, 1, 2, 3, 4, 5,
                0, 0, 1, 65536, 65536, new byte[] {1});

        assertEquals(SessionState.STARTING, controller.getState());
        assertEquals(1, listener.nativeCursorCount);

        connection.listener.connectionStarted();
        connection.listener.nativeCursor(
                true, false, 1, 2, 3, 0, 0,
                0, 0, 1, 65536, 65536, new byte[0]);

        assertEquals(2, listener.nativeCursorCount);
    }

    private static final class FakeConnection
            implements StreamSessionConnection {
        int startCount;
        int stopCount;
        NvConnectionListener listener;
        RuntimeException stopFailure;

        @Override
        public void start(AudioRenderer audioRenderer,
                          VideoDecoderRenderer videoDecoderRenderer,
                          NvConnectionListener connectionListener) {
            startCount++;
            listener = connectionListener;
        }

        @Override
        public void stop() {
            stopCount++;
            if (stopFailure != null) {
                throw stopFailure;
            }
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final List<Runnable> commands = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            commands.add(command);
        }

        void runNext() {
            commands.remove(0).run();
        }
    }

    private static final class RecordingListener
            implements NvConnectionListener {
        int stageStartingCount;
        int stageFailedCount;
        int connectionStartedCount;
        int connectionTerminatedCount;
        int connectionStatusUpdateCount;
        int displayMessageCount;
        int displayTransientMessageCount;
        int rumbleCount;
        int rumbleTriggersCount;
        int hdrModeCount;
        int motionStateCount;
        int controllerLedCount;
        int nativeCursorCount;

        @Override
        public void stageStarting(String stage) {
            stageStartingCount++;
        }

        @Override
        public void stageComplete(String stage) {
        }

        @Override
        public void stageFailed(String stage, int portFlags, int errorCode) {
            stageFailedCount++;
        }

        @Override
        public void connectionStarted() {
            connectionStartedCount++;
        }

        @Override
        public void connectionTerminated(int errorCode) {
            connectionTerminatedCount++;
        }

        @Override
        public void connectionStatusUpdate(int connectionStatus) {
            connectionStatusUpdateCount++;
        }

        @Override
        public void displayMessage(String message) {
            displayMessageCount++;
        }

        @Override
        public void displayTransientMessage(String message) {
            displayTransientMessageCount++;
        }

        @Override
        public void rumble(short controllerNumber, short lowFreqMotor,
                           short highFreqMotor) {
            rumbleCount++;
        }

        @Override
        public void rumbleTriggers(short controllerNumber,
                                   short leftTrigger,
                                   short rightTrigger) {
            rumbleTriggersCount++;
        }

        @Override
        public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
            hdrModeCount++;
        }

        @Override
        public void setMotionEventState(short controllerNumber,
                                        byte motionType,
                                        short reportRateHz) {
            motionStateCount++;
        }

        @Override
        public void setControllerLED(short controllerNumber, byte r,
                                     byte g, byte b) {
            controllerLedCount++;
        }

        @Override
        public void nativeCursor(boolean visible, boolean shapeChanged,
                                 int format, int x, int y, int width,
                                 int height, int hotspotX, int hotspotY,
                                 int shapeId, int scaleX, int scaleY,
                                 byte[] imageData) {
            nativeCursorCount++;
        }
    }
}
