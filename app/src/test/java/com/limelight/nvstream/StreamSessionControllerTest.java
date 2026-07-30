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
        assertTrue(controller.start(null, null));
        assertEquals(SessionState.STARTING, controller.getState());
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

    private static final class FakeConnection
            implements StreamSessionConnection {
        int startCount;
        int stopCount;
        NvConnectionListener listener;

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
        int stageFailedCount;
        int connectionStartedCount;
        int connectionTerminatedCount;

        @Override
        public void stageStarting(String stage) {
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
        }

        @Override
        public void displayMessage(String message) {
        }

        @Override
        public void displayTransientMessage(String message) {
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
        public void nativeCursor(boolean visible, boolean shapeChanged,
                                 int format, int x, int y, int width,
                                 int height, int hotspotX, int hotspotY,
                                 int shapeId, int scaleX, int scaleY,
                                 byte[] imageData) {
        }
    }
}
