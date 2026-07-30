package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamWifiLockControllerTest {
    private static final int TEST_LOW_LATENCY_MODE = 42;

    @Test
    public void acquiresSupportedLocksOnceAndReleasesInReverseOrder() {
        RecordingFactory factory = new RecordingFactory();
        StreamWifiLockController controller =
                new StreamWifiLockController(
                        factory,
                        TEST_LOW_LATENCY_MODE);

        assertTrue(controller.acquire());
        assertTrue(controller.acquire());
        assertEquals(2, factory.createCount);

        controller.destroy();
        controller.destroy();

        assertEquals(List.of(
                "release:" + TEST_LOW_LATENCY_MODE,
                "release:" + factory.highPerformanceMode),
                factory.releaseOrder);
    }

    @Test
    public void lockFailuresAreIndependent() {
        RecordingFactory factory = new RecordingFactory();
        factory.failFirstCreation = true;
        StreamWifiLockController controller =
                new StreamWifiLockController(
                        factory,
                        TEST_LOW_LATENCY_MODE);

        assertTrue(controller.acquire());
        assertTrue(factory.locks.get(
                TEST_LOW_LATENCY_MODE).held);

        controller.destroy();

        assertEquals(1, factory.releaseOrder.size());
    }

    @Test
    public void unsupportedLowLatencyModeUsesOnlyHighPerformanceLock() {
        RecordingFactory factory = new RecordingFactory();
        StreamWifiLockController controller =
                new StreamWifiLockController(factory, -1);

        assertTrue(controller.acquire());

        assertEquals(1, factory.createCount);
        assertTrue(factory.locks.containsKey(
                factory.highPerformanceMode));
        assertFalse(factory.locks.containsKey(
                TEST_LOW_LATENCY_MODE));
    }

    @Test
    public void destroyBeforeAcquireIsSafeAndTerminal() {
        RecordingFactory factory = new RecordingFactory();
        StreamWifiLockController controller =
                new StreamWifiLockController(
                        factory,
                        TEST_LOW_LATENCY_MODE);

        controller.destroy();

        assertFalse(controller.acquire());
        assertEquals(0, factory.createCount);
        assertTrue(factory.releaseOrder.isEmpty());
    }

    private static final class RecordingFactory
            implements StreamWifiLockController.LockFactory {
        final Map<Integer, RecordingLock> locks =
                new HashMap<>();
        final List<String> releaseOrder =
                new ArrayList<>();
        int createCount;
        int highPerformanceMode;
        boolean failFirstCreation;

        @Override
        public StreamWifiLockController.LockHandle create(
                int mode,
                String tag) {
            createCount++;
            if (createCount == 1) {
                highPerformanceMode = mode;
            }
            if (failFirstCreation && createCount == 1) {
                throw new SecurityException("denied");
            }
            RecordingLock lock =
                    new RecordingLock(mode, releaseOrder);
            locks.put(mode, lock);
            return lock;
        }
    }

    private static final class RecordingLock
            implements StreamWifiLockController.LockHandle {
        private final int mode;
        private final List<String> releaseOrder;
        boolean held;
        boolean referenceCounted = true;

        private RecordingLock(
                int mode,
                List<String> releaseOrder) {
            this.mode = mode;
            this.releaseOrder = releaseOrder;
        }

        @Override
        public void setReferenceCounted(boolean referenceCounted) {
            this.referenceCounted = referenceCounted;
        }

        @Override
        public void acquire() {
            assertFalse(referenceCounted);
            held = true;
        }

        @Override
        public boolean isHeld() {
            return held;
        }

        @Override
        public void release() {
            assertTrue(held);
            held = false;
            releaseOrder.add("release:" + mode);
        }
    }
}
