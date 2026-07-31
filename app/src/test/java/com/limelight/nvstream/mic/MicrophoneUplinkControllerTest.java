package com.limelight.nvstream.mic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class MicrophoneUplinkControllerTest {
    @Test
    public void unsupportedFactoryNeverCreatesSession() {
        FakeFactory factory = new FakeFactory();
        factory.supported = false;
        MicrophoneUplinkController controller =
                new MicrophoneUplinkController(factory);

        assertEquals(
                MicrophoneUplinkState.UNAVAILABLE,
                controller.getState());
        assertFalse(controller.start());
        assertEquals(0, factory.createCount);
        assertEquals(
                MicrophoneUplinkState.UNAVAILABLE,
                controller.getState());
    }

    @Test
    public void successfulSessionHasIdempotentStartAndStop() {
        FakeFactory factory = new FakeFactory();
        FakeSession session = new FakeSession();
        factory.nextSession = session;
        MicrophoneUplinkController controller =
                new MicrophoneUplinkController(factory);

        assertTrue(controller.start());
        assertTrue(controller.start());
        assertTrue(controller.isActive());
        assertEquals(1, factory.createCount);
        assertEquals(1, session.startCount);

        assertTrue(controller.stop());
        assertTrue(controller.stop());
        assertEquals(1, session.stopCount);
        assertEquals(
                MicrophoneUplinkState.OFF,
                controller.getState());
    }

    @Test
    public void failedStartCleansUpAndCanRetry() {
        FakeFactory factory = new FakeFactory();
        FakeSession failed = new FakeSession();
        failed.startResult = false;
        failed.lastError = "capture failed";
        factory.nextSession = failed;
        MicrophoneUplinkController controller =
                new MicrophoneUplinkController(factory);

        assertFalse(controller.start());
        assertEquals(1, failed.stopCount);
        assertEquals(
                MicrophoneUplinkState.ERROR,
                controller.getState());
        assertEquals("capture failed", controller.getLastMessage());

        FakeSession replacement = new FakeSession();
        factory.nextSession = replacement;
        assertTrue(controller.start());
        assertSame(replacement, factory.lastCreated);
        assertEquals(
                MicrophoneUplinkState.ON,
                controller.getState());
    }

    @Test
    public void asynchronousCaptureFailureBecomesError() {
        FakeFactory factory = new FakeFactory();
        FakeSession session = new FakeSession();
        factory.nextSession = session;
        MicrophoneUplinkController controller =
                new MicrophoneUplinkController(factory);
        assertTrue(controller.start());

        session.running = false;
        session.lastError = "read failed";

        assertFalse(controller.isActive());
        assertEquals(
                MicrophoneUplinkState.ERROR,
                controller.getState());
        assertEquals("read failed", controller.getLastMessage());
    }

    @Test
    public void failedStopRetainsSessionForExplicitRetry() {
        FakeFactory factory = new FakeFactory();
        FakeSession session = new FakeSession();
        factory.nextSession = session;
        MicrophoneUplinkController controller =
                new MicrophoneUplinkController(factory);
        assertTrue(controller.start());

        session.stopResult = false;
        session.lastError = "thread still running";
        assertFalse(controller.stop());
        assertEquals(
                MicrophoneUplinkState.ERROR,
                controller.getState());

        session.stopResult = true;
        assertTrue(controller.stop());
        assertEquals(2, session.stopCount);
        assertEquals(
                MicrophoneUplinkState.OFF,
                controller.getState());
    }

    private static final class FakeFactory
            implements MicrophoneUplinkSessionFactory {
        private boolean supported = true;
        private FakeSession nextSession;
        private MicrophoneUplinkSession lastCreated;
        private int createCount;

        @Override
        public boolean isSupported() {
            return supported;
        }

        @Override
        public MicrophoneUplinkSession create() {
            createCount++;
            lastCreated = nextSession;
            return nextSession;
        }
    }

    private static final class FakeSession
            implements MicrophoneUplinkSession {
        private boolean startResult = true;
        private boolean stopResult = true;
        private boolean running;
        private String lastError = "failed";
        private int startCount;
        private int stopCount;

        @Override
        public boolean start() {
            startCount++;
            running = startResult;
            return startResult;
        }

        @Override
        public boolean stop() {
            stopCount++;
            if (stopResult) {
                running = false;
            }
            return stopResult;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public String getLastErrorMessage() {
            return lastError;
        }
    }
}
