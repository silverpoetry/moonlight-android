package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class StreamOverlayVisibilityControllerTest {
    @Test
    public void enteringAndExitingPipAppliesOrderedEffects() {
        RecordingHost host = new RecordingHost();
        StreamOverlayVisibilityController controller =
                new StreamOverlayVisibilityController(host);
        controller.setConnectionWarningVisible(true);
        host.events.clear();

        controller.onPictureInPictureModeChanged(true);
        controller.setConnectionWarningVisible(false);
        controller.onPictureInPictureModeChanged(false);

        assertEquals(
                Arrays.asList(
                        "hideVirtual",
                        "hidePerformance",
                        "warning:false",
                        "disableSensors",
                        "entered",
                        "restorePerformance",
                        "warning:false",
                        "enableSensors",
                        "exited"),
                host.events);
    }

    @Test
    public void duplicateModeCallbacksAreIdempotent() {
        RecordingHost host = new RecordingHost();
        StreamOverlayVisibilityController controller =
                new StreamOverlayVisibilityController(host);

        controller.onPictureInPictureModeChanged(false);
        controller.onPictureInPictureModeChanged(true);
        controller.onPictureInPictureModeChanged(true);

        assertEquals(5, host.events.size());
    }

    @Test
    public void destroyedControllerRejectsLateEffects() {
        RecordingHost host = new RecordingHost();
        StreamOverlayVisibilityController controller =
                new StreamOverlayVisibilityController(host);
        controller.destroy();

        controller.setConnectionWarningVisible(true);
        controller.onPictureInPictureModeChanged(true);

        assertEquals(0, host.events.size());
    }

    private static final class RecordingHost
            implements StreamOverlayVisibilityController.Host {
        private final List<String> events = new ArrayList<>();

        @Override
        public void hideVirtualControls() {
            events.add("hideVirtual");
        }

        @Override
        public void hidePerformanceOverlay() {
            events.add("hidePerformance");
        }

        @Override
        public void restorePerformanceOverlay() {
            events.add("restorePerformance");
        }

        @Override
        public void setConnectionWarningVisible(boolean visible) {
            events.add("warning:" + visible);
        }

        @Override
        public void disableControllerSensors() {
            events.add("disableSensors");
        }

        @Override
        public void enableControllerSensors() {
            events.add("enableSensors");
        }

        @Override
        public void notifyPictureInPictureEntered() {
            events.add("entered");
        }

        @Override
        public void notifyPictureInPictureExited() {
            events.add("exited");
        }
    }
}
