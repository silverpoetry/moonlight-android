package com.limelight.ui.hosts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.limelight.computers.pairing.HostPairingUseCase;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class HostPairingControllerTest {
    @Test
    public void resultMovesFromOwnedWorkerToCallbackExecutor() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        AtomicReference<HostPairingController.Result> result =
                new AtomicReference<>();

        assertEquals(
                HostPairingController.RequestStatus.ACCEPTED,
                controller.request(
                        cancellation -> HostPairingUseCase.Outcome.PAIRED,
                        result::set));
        assertNull(result.get());
        worker.runNext();
        assertNull(result.get());
        callbacks.runNext();

        assertTrue(result.get().isSuccessful());
        assertEquals(
                HostPairingUseCase.Outcome.PAIRED,
                result.get().getOutcome());
    }

    @Test
    public void duplicateIsRejectedBeforeSecondOperationRuns() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        AtomicInteger operations = new AtomicInteger();

        assertEquals(
                HostPairingController.RequestStatus.ACCEPTED,
                controller.request(
                        cancellation -> {
                            operations.incrementAndGet();
                            return HostPairingUseCase.Outcome.FAILED;
                        },
                        result -> {}));
        assertEquals(
                HostPairingController.RequestStatus.ALREADY_RUNNING,
                controller.request(
                        cancellation -> {
                            operations.incrementAndGet();
                            return HostPairingUseCase.Outcome.PAIRED;
                        },
                        result -> {}));

        worker.runAll();
        callbacks.runAll();
        assertEquals(1, operations.get());
    }

    @Test
    public void destroySuppressesPublicationAfterWorkCompletes() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        AtomicInteger deliveries = new AtomicInteger();

        controller.request(
                cancellation -> HostPairingUseCase.Outcome.PAIRED,
                result -> deliveries.incrementAndGet());
        worker.runNext();
        controller.destroy();
        callbacks.runAll();

        assertEquals(0, deliveries.get());
        assertTrue(worker.isShutdown());
        assertEquals(
                HostPairingController.RequestStatus.DESTROYED,
                controller.request(
                        cancellation -> HostPairingUseCase.Outcome.PAIRED,
                        result -> deliveries.incrementAndGet()));
    }

    @Test
    public void destroyBeforeWorkerStartsPreventsOperation() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        AtomicInteger operations = new AtomicInteger();
        AtomicInteger deliveries = new AtomicInteger();

        controller.request(
                cancellation -> {
                    operations.incrementAndGet();
                    return HostPairingUseCase.Outcome.PAIRED;
                },
                result -> deliveries.incrementAndGet());
        controller.destroy();
        worker.runAll();
        callbacks.runAll();

        assertEquals(0, operations.get());
        assertEquals(0, deliveries.get());
    }

    @Test
    public void destroyDuringOperationDoesNotInterruptCompletion() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger deliveries = new AtomicInteger();

        controller.request(
                cancellation -> {
                    controller.destroy();
                    assertTrue(cancellation.isCanceled());
                    completed.incrementAndGet();
                    return HostPairingUseCase.Outcome.PAIRED;
                },
                result -> deliveries.incrementAndGet());
        worker.runAll();
        callbacks.runAll();

        assertEquals(1, completed.get());
        assertEquals(0, deliveries.get());
    }

    @Test
    public void operationFailureIsPublishedAsData() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        HostPairingController controller =
                new HostPairingController(worker, callbacks);
        IOException failure = new IOException("network");
        AtomicReference<HostPairingController.Result> result =
                new AtomicReference<>();

        controller.request(
                cancellation -> {
                    throw failure;
                },
                result::set);
        worker.runNext();
        callbacks.runNext();

        assertSame(failure, result.get().getFailure());
        assertFalse(result.get().isSuccessful());
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
        public boolean awaitTermination(
                long timeout,
                TimeUnit unit) {
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
