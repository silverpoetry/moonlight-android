package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DecoderCrashTrackerTest {
    @Test
    public void initialCountIsFrozenAndNormalized() {
        RecordingStore store = new RecordingStore(-2);
        DecoderCrashTracker tracker = new DecoderCrashTracker(store);
        store.crashCount = 9;

        assertEquals(0, tracker.getInitialCrashCount());
    }

    @Test
    public void crashIsCommittedSynchronouslyAtMostOnce() {
        RecordingStore store = new RecordingStore(2);
        DecoderCrashTracker tracker = new DecoderCrashTracker(store);

        tracker.notifyCrash(new RuntimeException("first"));
        tracker.notifyCrash(new RuntimeException("duplicate"));
        tracker.completeCleanly();

        assertEquals(1, store.recordCount);
        assertEquals(3, store.crashCount);
        assertEquals(0, store.clearCount);
    }

    @Test
    public void cleanCompletionClearsHistoricalCrashStateOnce() {
        RecordingStore store = new RecordingStore(2);
        DecoderCrashTracker tracker = new DecoderCrashTracker(store);

        tracker.completeCleanly();
        tracker.completeCleanly();
        tracker.notifyCrash(new RuntimeException("late"));

        assertEquals(1, store.recordCount);
        assertEquals(1, store.clearCount);
        assertEquals(1, store.crashCount);
    }

    @Test
    public void cleanCompletionDoesNotWriteWhenHistoryIsEmpty() {
        RecordingStore store = new RecordingStore(0);
        DecoderCrashTracker tracker = new DecoderCrashTracker(store);

        tracker.completeCleanly();

        assertEquals(0, store.clearCount);
    }

    private static final class RecordingStore
            implements DecoderCrashStore {
        private int crashCount;
        private int recordCount;
        private int clearCount;

        private RecordingStore(int crashCount) {
            this.crashCount = crashCount;
        }

        @Override
        public DecoderCrashState readState() {
            return new DecoderCrashState(crashCount, 0);
        }

        @Override
        public void recordCrashSynchronously() {
            recordCount++;
            crashCount++;
        }

        @Override
        public void clearCrashHistory() {
            clearCount++;
            crashCount = 0;
        }

        @Override
        public void acknowledgeCrashCount(int acknowledgedCrashCount) {
        }
    }
}
