package com.limelight.ui.hosts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HostServiceBindingControllerTest {
    private final ManualExecutorService worker =
            new ManualExecutorService();
    private final ManualExecutor callbacks = new ManualExecutor();
    private final HostServiceBindingController controller =
            new HostServiceBindingController(worker, callbacks);

    @After
    public void tearDown() {
        controller.destroy();
    }

    @Test
    public void latestConnectionSupersedesQueuedPredecessor() {
        AtomicInteger firstCallbacks = new AtomicInteger();
        AtomicInteger secondCallbacks = new AtomicInteger();

        assertEquals(
                HostServiceBindingController.ConnectStatus.ACCEPTED,
                controller.connect(
                        cancellation -> "first",
                        result -> firstCallbacks.incrementAndGet()));
        assertEquals(
                HostServiceBindingController.ConnectStatus.ACCEPTED,
                controller.connect(
                        cancellation -> "second",
                        result -> secondCallbacks.incrementAndGet()));

        worker.runAll();
        callbacks.runAll();

        assertEquals(0, firstCallbacks.get());
        assertEquals(1, secondCallbacks.get());
    }

    @Test
    public void disconnectInvalidatesQueuedCallback() {
        AtomicInteger callbacksSeen = new AtomicInteger();
        controller.connect(
                cancellation -> "ready",
                result -> callbacksSeen.incrementAndGet());

        worker.runAll();
        controller.disconnect();
        callbacks.runAll();

        assertEquals(0, callbacksSeen.get());
    }

    @Test
    public void disconnectDiscardsCompletedUndeliveredValue() {
        AtomicInteger callbacksSeen = new AtomicInteger();
        AtomicInteger valuesDiscarded = new AtomicInteger();
        controller.connect(
                cancellation -> "ready",
                result -> callbacksSeen.incrementAndGet(),
                value -> valuesDiscarded.incrementAndGet());

        worker.runAll();
        controller.disconnect();
        callbacks.runAll();

        assertEquals(0, callbacksSeen.get());
        assertEquals(1, valuesDiscarded.get());
    }

    @Test
    public void replacementSignalObservesCancellation() {
        AtomicBoolean canceled = new AtomicBoolean();
        controller.connect(
                signal -> {
                    controller.connect(
                            replacementSignal -> "replacement",
                            result -> { });
                    canceled.set(signal.isCanceled());
                    return "stale";
                },
                result -> { });

        worker.runAll();
        callbacks.runAll();

        assertTrue(canceled.get());
        Thread.interrupted();
    }

    @Test
    public void destroyRejectsNewConnection() {
        controller.destroy();

        assertEquals(
                HostServiceBindingController.ConnectStatus.DESTROYED,
                controller.connect(
                        cancellation -> "ready",
                        result -> { }));
        assertTrue(worker.isShutdown());
    }

    @Test
    public void unavailableExecutorDoesNotLeaveControllerBusy() {
        ManualExecutorService rejectedWorker =
                new ManualExecutorService();
        rejectedWorker.shutdown();
        HostServiceBindingController rejectedController =
                new HostServiceBindingController(
                        rejectedWorker,
                        Runnable::run);
        try {
            assertEquals(
                    HostServiceBindingController.ConnectStatus.UNAVAILABLE,
                    rejectedController.connect(
                            cancellation -> "ready",
                            result -> { }));
        }
        finally {
            rejectedController.destroy();
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> commands = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            commands.add(command);
        }

        void runAll() {
            Runnable command;
            while ((command = commands.poll()) != null) {
                command.run();
            }
        }
    }

    private static final class ManualExecutorService
            extends AbstractExecutorService {
        private final Queue<Runnable> commands = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            java.util.List<Runnable> remaining =
                    new java.util.ArrayList<>(commands);
            commands.clear();
            return remaining;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && commands.isEmpty();
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
                throw new RejectedExecutionException("shutdown");
            }
            commands.add(command);
        }

        void runAll() {
            Runnable command;
            while ((command = commands.poll()) != null) {
                command.run();
            }
        }
    }
}
