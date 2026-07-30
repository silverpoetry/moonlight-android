package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class StreamSessionCallbackRouterTest {
    @Test
    public void presentationCallbacksAreSerializedByUiDispatcher() {
        ManualUiDispatcher dispatcher = new ManualUiDispatcher();
        RecordingUiHost uiHost = new RecordingUiHost();
        RecordingFeedbackHost feedbackHost =
                new RecordingFeedbackHost();
        StreamSessionCallbackRouter router =
                new StreamSessionCallbackRouter(
                        uiHost,
                        feedbackHost,
                        dispatcher);

        router.stageStarting("video");
        router.stageFailed("audio", 2, 3);
        router.connectionStarted();
        router.connectionStatusUpdate(1);
        router.displayMessage("message");
        router.displayTransientMessage("warning");
        router.connectionTerminated(2);

        assertEquals(0, uiHost.events.size());
        dispatcher.runAll();

        assertEquals(
                List.of(
                        "starting:video",
                        "failed:audio:2:3",
                        "started",
                        "status:1",
                        "message:false:message",
                        "message:true:warning",
                        "terminated:2"),
                uiHost.events);
    }

    @Test
    public void feedbackCallbacksStayOnImmediatePath() {
        ManualUiDispatcher dispatcher = new ManualUiDispatcher();
        RecordingUiHost uiHost = new RecordingUiHost();
        RecordingFeedbackHost feedbackHost =
                new RecordingFeedbackHost();
        StreamSessionCallbackRouter router =
                new StreamSessionCallbackRouter(
                        uiHost,
                        feedbackHost,
                        dispatcher);

        router.rumble((short) 1, (short) 2, (short) 3);
        router.rumbleTriggers((short) 4, (short) 5, (short) 6);
        router.setMotionEventState((short) 7, (byte) 8, (short) 9);
        router.setControllerLED(
                (short) 10,
                (byte) 11,
                (byte) 12,
                (byte) 13);

        assertEquals(
                List.of(
                        "rumble:1:2:3",
                        "triggers:4:5:6",
                        "motion:7:8:9",
                        "led:10:11:12:13"),
                feedbackHost.events);
        assertEquals(0, dispatcher.tasks.size());
    }

    @Test
    public void destructionClearsQueuedUiAndRejectsLateFeedback() {
        ManualUiDispatcher dispatcher = new ManualUiDispatcher();
        RecordingUiHost uiHost = new RecordingUiHost();
        RecordingFeedbackHost feedbackHost =
                new RecordingFeedbackHost();
        StreamSessionCallbackRouter router =
                new StreamSessionCallbackRouter(
                        uiHost,
                        feedbackHost,
                        dispatcher);

        router.connectionStarted();
        router.destroy();
        router.destroy();
        router.connectionTerminated(1);
        router.rumble((short) 1, (short) 2, (short) 3);
        dispatcher.runAll();

        assertEquals(1, dispatcher.clearCount);
        assertEquals(0, uiHost.events.size());
        assertEquals(0, feedbackHost.events.size());
    }

    @Test
    public void mutablePayloadsAreSnapshottedBeforeUiDispatch() {
        ManualUiDispatcher dispatcher = new ManualUiDispatcher();
        RecordingUiHost uiHost = new RecordingUiHost();
        StreamSessionCallbackRouter router =
                new StreamSessionCallbackRouter(
                        uiHost,
                        new RecordingFeedbackHost(),
                        dispatcher);
        byte[] metadata = new byte[] {1, 2};
        byte[] cursor = new byte[] {3, 4};

        router.setHdrMode(true, metadata);
        router.nativeCursor(
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
                cursor);
        metadata[0] = 9;
        cursor[0] = 9;
        dispatcher.runAll();

        assertArrayEquals(new byte[] {1, 2}, uiHost.hdrMetadata);
        assertArrayEquals(new byte[] {3, 4}, uiHost.cursorImage);
    }

    @Test
    public void completedStageHasNoDownstreamSideEffect() {
        ManualUiDispatcher dispatcher = new ManualUiDispatcher();
        RecordingUiHost uiHost = new RecordingUiHost();
        StreamSessionCallbackRouter router =
                new StreamSessionCallbackRouter(
                        uiHost,
                        new RecordingFeedbackHost(),
                        dispatcher);

        router.stageComplete("video");
        dispatcher.runAll();

        assertEquals(0, uiHost.events.size());
    }

    private static final class ManualUiDispatcher
            implements StreamSessionCallbackRouter.UiDispatcher {
        private final List<Runnable> tasks = new ArrayList<>();
        private int clearCount;

        @Override
        public boolean dispatch(Runnable task) {
            tasks.add(task);
            return true;
        }

        @Override
        public void clear() {
            clearCount++;
            tasks.clear();
        }

        void runAll() {
            List<Runnable> queued = new ArrayList<>(tasks);
            tasks.clear();
            queued.forEach(Runnable::run);
        }
    }

    private static final class RecordingUiHost
            implements StreamSessionCallbackRouter.UiHost {
        private final List<String> events = new ArrayList<>();
        private byte[] hdrMetadata;
        private byte[] cursorImage;

        @Override
        public void onStageStarting(String stage) {
            events.add("starting:" + stage);
        }

        @Override
        public void onStageFailed(
                String stage,
                int portFlags,
                int errorCode) {
            events.add(
                    "failed:" + stage + ":" +
                            portFlags + ":" + errorCode);
        }

        @Override
        public void onConnectionStarted() {
            events.add("started");
        }

        @Override
        public void onConnectionTerminated(int errorCode) {
            events.add("terminated:" + errorCode);
        }

        @Override
        public void onConnectionStatusUpdate(int connectionStatus) {
            events.add("status:" + connectionStatus);
        }

        @Override
        public void onMessage(
                String message,
                boolean transientMessage) {
            events.add(
                    "message:" + transientMessage + ":" + message);
        }

        @Override
        public void onHdrModeChanged(
                boolean enabled,
                byte[] hdrMetadata) {
            this.hdrMetadata = hdrMetadata;
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
            cursorImage = imageData;
        }
    }

    private static final class RecordingFeedbackHost
            implements StreamSessionCallbackRouter.FeedbackHost {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onRumble(
                short controllerNumber,
                short lowFreqMotor,
                short highFreqMotor) {
            events.add(
                    "rumble:" + controllerNumber + ":" +
                            lowFreqMotor + ":" + highFreqMotor);
        }

        @Override
        public void onRumbleTriggers(
                short controllerNumber,
                short leftTrigger,
                short rightTrigger) {
            events.add(
                    "triggers:" + controllerNumber + ":" +
                            leftTrigger + ":" + rightTrigger);
        }

        @Override
        public void onMotionEventState(
                short controllerNumber,
                byte motionType,
                short reportRateHz) {
            events.add(
                    "motion:" + controllerNumber + ":" +
                            motionType + ":" + reportRateHz);
        }

        @Override
        public void onControllerLed(
                short controllerNumber,
                byte red,
                byte green,
                byte blue) {
            events.add(
                    "led:" + controllerNumber + ":" +
                            red + ":" + green + ":" + blue);
        }
    }
}
