package com.limelight.ui.hosts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class HostUiOperationControllerTest {
    @Test
    public void publishesResultOnCallbackExecutor() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        AtomicReference<String> value = new AtomicReference<>();

        assertEquals(
                HostUiOperationController.RequestStatus.ACCEPTED,
                controller.request(
                        () -> "resolved",
                        result -> value.set(result.getValue())));
        worker.runNext();
        assertNull(value.get());
        callbacks.runNext();

        assertEquals("resolved", value.get());
    }

    @Test
    public void rejectsDuplicateUntilCompletionIsDelivered() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        AtomicInteger executions = new AtomicInteger();

        controller.request(
                executions::incrementAndGet,
                result -> {});
        assertEquals(
                HostUiOperationController.RequestStatus.ALREADY_RUNNING,
                controller.request(
                        executions::incrementAndGet,
                        result -> {}));
        worker.runAll();
        assertEquals(
                HostUiOperationController.RequestStatus.ALREADY_RUNNING,
                controller.request(
                        executions::incrementAndGet,
                        result -> {}));
        callbacks.runAll();

        assertEquals(
                HostUiOperationController.RequestStatus.ACCEPTED,
                controller.request(
                        executions::incrementAndGet,
                        result -> {}));
        worker.runAll();
        callbacks.runAll();
        assertEquals(2, executions.get());
    }

    @Test
    public void cancelInterruptsWorkAndSuppressesQueuedCallback() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        AtomicInteger deliveries = new AtomicInteger();

        controller.request(() -> "done", result ->
                deliveries.incrementAndGet());
        worker.runNext();
        controller.cancelCurrent();
        callbacks.runAll();

        assertEquals(0, deliveries.get());
        assertEquals(
                HostUiOperationController.RequestStatus.ACCEPTED,
                controller.request(
                        () -> "next",
                        result -> deliveries.incrementAndGet()));
    }

    @Test
    public void cancelBeforeWorkerStartsPreventsOperation() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        AtomicInteger executions = new AtomicInteger();

        controller.request(
                executions::incrementAndGet,
                result -> {});
        controller.cancelCurrent();
        worker.runAll();
        callbacks.runAll();

        assertEquals(0, executions.get());
    }

    @Test
    public void destroyInterruptsRunningTaskAndRejectsFutureWork()
            throws Exception {
        java.util.concurrent.ExecutorService worker =
                java.util.concurrent.Executors.newSingleThreadExecutor();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        java.util.concurrent.CountDownLatch started =
                new java.util.concurrent.CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();

        controller.request(() -> {
            started.countDown();
            try {
                Thread.sleep(5000L);
            }
            catch (InterruptedException error) {
                interrupted.set(true);
                throw error;
            }
            return "late";
        }, result -> {});
        assertTrue(started.await(1, TimeUnit.SECONDS));
        controller.destroy();
        worker.awaitTermination(1, TimeUnit.SECONDS);

        assertTrue(interrupted.get());
        assertEquals(
                HostUiOperationController.RequestStatus.DESTROYED,
                controller.request(() -> "never", result -> {}));
        callbacks.runAll();
    }

    @Test
    public void operationFailureIsPublishedAsData() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostUiOperationController controller =
                new HostUiOperationController(worker, callbacks);
        IOException failure = new IOException("network");
        AtomicReference<HostUiOperationController.Result<String>> result =
                new AtomicReference<>();

        controller.request(() -> {
            throw failure;
        }, result::set);
        worker.runAll();
        callbacks.runAll();

        assertFalse(result.get().isSuccessful());
        assertSame(failure, result.get().getFailure());
    }

    @Test
    public void rejectedSchedulingClearsPendingOwnership() {
        ManualExecutorService worker = new ManualExecutorService();
        worker.shutdown();
        HostUiOperationController controller =
                new HostUiOperationController(
                        worker,
                        new ManualExecutor());

        assertEquals(
                HostUiOperationController.RequestStatus.UNAVAILABLE,
                controller.request(() -> "never", result -> {}));
        controller.destroy();
        assertTrue(worker.isShutdown());
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> commands = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            commands.add(command);
        }

        void runNext() {
            commands.remove().run();
        }

        void runAll() {
            while (!commands.isEmpty()) {
                runNext();
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
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> pending = new ArrayList<>(commands);
            commands.clear();
            return pending;
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
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }

        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new IllegalStateException("shutdown");
            }
            commands.add(command);
        }

        void runNext() {
            commands.remove().run();
        }

        void runAll() {
            while (!commands.isEmpty()) {
                runNext();
            }
        }
    }
}
