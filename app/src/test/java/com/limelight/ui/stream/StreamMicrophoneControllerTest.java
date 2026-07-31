package com.limelight.ui.stream;

import com.limelight.nvstream.mic.MicrophoneUplinkEndpoint;
import com.limelight.nvstream.mic.MicrophoneUplinkState;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamMicrophoneControllerTest {
    @Test
    public void permissionGrantResumesExactlyOnePendingStart() {
        Fixture fixture = new Fixture();
        fixture.permission.granted = false;

        assertTrue(fixture.controller.toggle());
        assertFalse(fixture.controller.toggle());
        assertEquals(1, fixture.permission.requestCount);
        assertEquals(0, fixture.endpoint.startCount);

        fixture.controller.onPermissionResult(true);
        fixture.controller.onPermissionResult(true);
        fixture.worker.runNext();

        assertEquals(1, fixture.endpoint.startCount);
        assertEquals(1, fixture.feedback.stateChangeCount);
        assertTrue(fixture.controller.isActive());
    }

    @Test
    public void permissionDenialClearsPendingStartAndReportsIt() {
        Fixture fixture = new Fixture();
        fixture.permission.granted = false;

        assertTrue(fixture.controller.toggle());
        fixture.controller.onPermissionResult(false);

        assertEquals(1, fixture.feedback.permissionDeniedCount);
        assertEquals(0, fixture.endpoint.startCount);
        assertFalse(fixture.controller.isActive());

        assertTrue(fixture.controller.toggle());
        assertEquals(2, fixture.permission.requestCount);
    }

    @Test
    public void unsupportedEndpointDoesNotRequestPermissionOrWork() {
        Fixture fixture = new Fixture();
        fixture.endpoint.supported = false;

        assertFalse(fixture.controller.toggle());

        assertEquals(1, fixture.feedback.unsupportedCount);
        assertEquals(0, fixture.permission.requestCount);
        assertEquals(0, fixture.worker.taskCount());
    }

    @Test
    public void toggleSerializesStartAndStopTransitions() {
        Fixture fixture = new Fixture();

        assertTrue(fixture.controller.toggle());
        assertFalse(fixture.controller.toggle());
        fixture.worker.runNext();

        assertEquals(1, fixture.endpoint.startCount);
        assertTrue(fixture.controller.isActive());

        assertTrue(fixture.controller.toggle());
        assertFalse(fixture.controller.toggle());
        fixture.worker.runNext();

        assertEquals(1, fixture.endpoint.stopCount);
        assertFalse(fixture.controller.isActive());
        assertEquals(2, fixture.feedback.stateChangeCount);
    }

    @Test
    public void operationFailurePublishesTransportMessage() {
        Fixture fixture = new Fixture();
        fixture.endpoint.startResult = false;
        fixture.endpoint.lastMessage = "capture failed";

        assertTrue(fixture.controller.toggle());
        fixture.worker.runNext();

        assertEquals(1, fixture.feedback.failureCount);
        assertEquals("capture failed", fixture.feedback.lastFailure);
        assertEquals(1, fixture.feedback.stateChangeCount);
        assertFalse(fixture.controller.isActive());
    }

    @Test
    public void destroyCancelsQueuedWorkAndLatePermissionResult() {
        Fixture fixture = new Fixture();

        assertTrue(fixture.controller.toggle());
        fixture.controller.destroy();
        fixture.controller.destroy();
        fixture.worker.runNext();

        assertTrue(fixture.worker.isShutdown());
        assertEquals(0, fixture.endpoint.startCount);
        assertFalse(fixture.controller.toggle());

        Fixture permissionFixture = new Fixture();
        permissionFixture.permission.granted = false;
        assertTrue(permissionFixture.controller.toggle());
        permissionFixture.controller.destroy();
        permissionFixture.permission.granted = true;
        permissionFixture.controller.onPermissionResult(true);

        assertEquals(0, permissionFixture.endpoint.startCount);
        assertEquals(0, permissionFixture.worker.taskCount());
    }

    private static final class Fixture {
        private final FakeEndpoint endpoint = new FakeEndpoint();
        private final FakePermissionGateway permission =
                new FakePermissionGateway();
        private final FakeFeedback feedback = new FakeFeedback();
        private final ManualExecutorService worker =
                new ManualExecutorService();
        private final StreamMicrophoneController controller =
                new StreamMicrophoneController(
                        endpoint,
                        permission,
                        feedback,
                        worker,
                        Runnable::run);
    }

    private static final class FakeEndpoint
            implements MicrophoneUplinkEndpoint {
        private boolean supported = true;
        private boolean startResult = true;
        private boolean stopResult = true;
        private int startCount;
        private int stopCount;
        private String lastMessage;
        private MicrophoneUplinkState state =
                MicrophoneUplinkState.OFF;

        @Override
        public boolean isMicUplinkSupported() {
            return supported;
        }

        @Override
        public boolean isMicUplinkActive() {
            return state == MicrophoneUplinkState.ON;
        }

        @Override
        public MicrophoneUplinkState getMicUplinkState() {
            return state;
        }

        @Override
        public String getLastMicUplinkMessage() {
            return lastMessage;
        }

        @Override
        public boolean startMicUplink() {
            startCount++;
            state = startResult ?
                    MicrophoneUplinkState.ON :
                    MicrophoneUplinkState.ERROR;
            return startResult;
        }

        @Override
        public boolean stopMicUplink() {
            stopCount++;
            state = stopResult ?
                    MicrophoneUplinkState.OFF :
                    MicrophoneUplinkState.ERROR;
            return stopResult;
        }
    }

    private static final class FakePermissionGateway
            implements StreamMicrophoneController.PermissionGateway {
        private boolean granted = true;
        private int requestCount;

        @Override
        public boolean isGranted() {
            return granted;
        }

        @Override
        public void requestPermission() {
            requestCount++;
        }
    }

    private static final class FakeFeedback
            implements StreamMicrophoneController.Feedback {
        private int unsupportedCount;
        private int permissionDeniedCount;
        private int failureCount;
        private int stateChangeCount;
        private String lastFailure;

        @Override
        public void onUnsupported() {
            unsupportedCount++;
        }

        @Override
        public void onPermissionDenied() {
            permissionDeniedCount++;
        }

        @Override
        public void onOperationFailed(String message) {
            failureCount++;
            lastFailure = message;
        }

        @Override
        public void onStateChanged() {
            stateChangeCount++;
        }
    }

    private static final class ManualExecutorService
            extends AbstractExecutorService {
        private final List<Runnable> tasks = new ArrayList<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> queued = new ArrayList<>(tasks);
            tasks.clear();
            return queued;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(
                long timeout,
                TimeUnit unit) {
            return isTerminated();
        }

        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new IllegalStateException(
                        "executor is shut down");
            }
            tasks.add(command);
        }

        private int taskCount() {
            return tasks.size();
        }

        private void runNext() {
            if (!tasks.isEmpty()) {
                tasks.remove(0).run();
            }
        }
    }
}
