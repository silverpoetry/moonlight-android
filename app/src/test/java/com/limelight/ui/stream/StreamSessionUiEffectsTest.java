package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.assertEquals;

public final class StreamSessionUiEffectsTest {
    @Test
    public void successfulSessionHasOneOrderedEffectTrace() {
        RecordingHost host = new RecordingHost();
        ManualScheduler scheduler = new ManualScheduler();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host, scheduler);

        effects.onConnecting();
        effects.onConnecting();
        effects.onConnected();
        effects.onConnected();
        scheduler.runNext();
        effects.onEnded();
        effects.onEnded();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "keep:true",
                        "connected",
                        "grab:true",
                        "grab:false",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    @Test
    public void failedStartEndsConnectingStateWithoutKeepingScreenOn() {
        RecordingHost host = new RecordingHost();
        ManualScheduler scheduler = new ManualScheduler();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host, scheduler);

        effects.onConnecting();
        effects.onEnded();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "grab:false",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    @Test
    public void destroyBeforeStartHasNoSideEffects() {
        RecordingHost host = new RecordingHost();
        ManualScheduler scheduler = new ManualScheduler();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host, scheduler);

        effects.destroy();
        effects.onConnecting();

        assertEquals(0, host.trace.size());
    }

    @Test
    public void connectedCannotArriveBeforeConnectingOrAfterEnd() {
        RecordingHost host = new RecordingHost();
        ManualScheduler scheduler = new ManualScheduler();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host, scheduler);

        effects.onConnected();
        effects.onConnecting();
        effects.onEnded();
        effects.onConnected();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "grab:false",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    @Test
    public void endingBeforeDelayCancelsInputGrab() {
        RecordingHost host = new RecordingHost();
        ManualScheduler scheduler = new ManualScheduler();
        StreamSessionUiEffects effects =
                new StreamSessionUiEffects(host, scheduler);

        effects.onConnecting();
        effects.onConnected();
        effects.onEnded();
        scheduler.runAll();

        assertEquals(
                Arrays.asList(
                        "connecting",
                        "keep:true",
                        "connected",
                        "grab:false",
                        "keep:false",
                        "ended"),
                host.trace);
    }

    private static final class RecordingHost
            implements StreamSessionUiEffects.Host {
        final List<String> trace = new ArrayList<>();

        @Override
        public void setKeepScreenOn(boolean keepScreenOn) {
            trace.add("keep:" + keepScreenOn);
        }

        @Override
        public void notifyStreamConnecting() {
            trace.add("connecting");
        }

        @Override
        public void notifyStreamConnected() {
            trace.add("connected");
        }

        @Override
        public void notifyStreamEnded() {
            trace.add("ended");
        }

        @Override
        public void setInputGrabbed(boolean grabbed) {
            trace.add("grab:" + grabbed);
        }
    }

    private static final class ManualScheduler
            implements StreamSessionUiEffects.DelayedTaskScheduler {
        private final Queue<ScheduledTask> tasks =
                new ArrayDeque<>();

        @Override
        public StreamSessionUiEffects.Cancellable schedule(
                Runnable task,
                long delayMs) {
            ScheduledTask scheduledTask =
                    new ScheduledTask(task);
            tasks.add(scheduledTask);
            return scheduledTask;
        }

        void runNext() {
            tasks.remove().run();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                runNext();
            }
        }
    }

    private static final class ScheduledTask
            implements StreamSessionUiEffects.Cancellable {
        private final Runnable task;
        private boolean cancelled;

        private ScheduledTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        void run() {
            if (!cancelled) {
                task.run();
            }
        }
    }
}
