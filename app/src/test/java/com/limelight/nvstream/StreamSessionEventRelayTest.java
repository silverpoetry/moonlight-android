package com.limelight.nvstream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class StreamSessionEventRelayTest {
    @Test
    public void handoffReplaysStartupStateBeforeConnectedEvent() {
        RecordingListener preparation = new RecordingListener();
        StreamSessionEventRelay relay =
                new StreamSessionEventRelay(preparation);

        relay.nativeCursor(
                true, true, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 10, new byte[] {11});
        relay.setHdrMode(true, new byte[] {12});
        relay.connectionStarted();

        RecordingListener stream = new RecordingListener();
        relay.attach(stream);

        assertEquals(
                List.of("hdr", "cursor", "connected"),
                stream.events);
        assertEquals(
                List.of("cursor", "hdr", "connected"),
                preparation.events);
    }

    @Test
    public void runtimeEventsUseOnlyCurrentObserver() {
        RecordingListener preparation = new RecordingListener();
        StreamSessionEventRelay relay =
                new StreamSessionEventRelay(preparation);
        RecordingListener stream = new RecordingListener();
        relay.attach(stream);

        relay.displayMessage("message");
        relay.rumble((short) 0, (short) 1, (short) 2);

        assertEquals(List.of(), preparation.events);
        assertEquals(List.of("message", "rumble"), stream.events);

        relay.detach(stream);
        relay.displayMessage("detached");
        assertEquals(List.of("message", "rumble"), stream.events);
    }

    private static final class RecordingListener
            implements NvConnectionListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public void stageStarting(String stage) {
            events.add("starting");
        }

        @Override
        public void stageComplete(String stage) {
            events.add("complete");
        }

        @Override
        public void stageFailed(String stage, int portFlags, int errorCode) {
            events.add("failed");
        }

        @Override
        public void connectionStarted() {
            events.add("connected");
        }

        @Override
        public void connectionTerminated(int errorCode) {
            events.add("terminated");
        }

        @Override
        public void connectionStatusUpdate(int connectionStatus) {
            events.add("status");
        }

        @Override
        public void displayMessage(String message) {
            events.add("message");
        }

        @Override
        public void displayTransientMessage(String message) {
            events.add("transient");
        }

        @Override
        public void rumble(short controllerNumber, short lowFreqMotor,
                           short highFreqMotor) {
            events.add("rumble");
        }

        @Override
        public void rumbleTriggers(short controllerNumber,
                                   short leftTrigger,
                                   short rightTrigger) {
            events.add("triggers");
        }

        @Override
        public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
            events.add("hdr");
        }

        @Override
        public void setMotionEventState(short controllerNumber,
                                        byte motionType,
                                        short reportRateHz) {
            events.add("motion");
        }

        @Override
        public void setControllerLED(short controllerNumber, byte r,
                                     byte g, byte b) {
            events.add("led");
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
            events.add("cursor");
        }
    }
}
