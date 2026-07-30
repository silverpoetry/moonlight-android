package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamFailureDiagnosticsTest {
    @Test
    public void resultMovesFromWorkerToCallbackExecutor() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbackExecutor = new ManualExecutor();
        AtomicInteger delivered = new AtomicInteger();
        StreamFailureDiagnostics diagnostics =
                new StreamFailureDiagnostics(
                        worker,
                        callbackExecutor,
                        portFlags -> portFlags + 10);

        assertTrue(diagnostics.request(
                7,
                result -> delivered.set(
                        result.getProbeResultOr(-1))));
        assertEquals(0, delivered.get());

        worker.runNext();
        assertEquals(0, delivered.get());

        callbackExecutor.runNext();
        assertEquals(17, delivered.get());
    }

    @Test
    public void newerRequestInvalidatesOlderUnpublishedResult() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbackExecutor = new ManualExecutor();
        AtomicInteger delivered = new AtomicInteger();
        StreamFailureDiagnostics diagnostics =
                new StreamFailureDiagnostics(
                        worker,
                        callbackExecutor,
                        portFlags -> portFlags);

        diagnostics.request(1, result -> delivered.addAndGet(100));
        diagnostics.request(2, result -> delivered.addAndGet(
                result.getProbeResultOr(-1)));

        worker.runAll();
        callbackExecutor.runAll();

        assertEquals(2, delivered.get());
    }

    @Test
    public void destroyCancelsWorkAndQueuedPublication() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbackExecutor = new ManualExecutor();
        AtomicInteger delivered = new AtomicInteger();
        StreamFailureDiagnostics diagnostics =
                new StreamFailureDiagnostics(
                        worker,
                        callbackExecutor,
                        portFlags -> portFlags);

        diagnostics.request(3, result -> delivered.incrementAndGet());
        worker.runNext();
        diagnostics.destroy();
        callbackExecutor.runAll();

        assertEquals(0, delivered.get());
        assertTrue(worker.isShutdown());
        assertFalse(diagnostics.request(
                4,
                result -> delivered.incrementAndGet()));
    }

    @Test
    public void probeFailurePublishesFallbackCapableResult() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbackExecutor = new ManualExecutor();
        AtomicInteger delivered = new AtomicInteger();
        StreamFailureDiagnostics diagnostics =
                new StreamFailureDiagnostics(
                        worker,
                        callbackExecutor,
                        portFlags -> {
                            throw new IllegalStateException("probe");
                        });

        diagnostics.request(5, result -> {
            assertTrue(result.didProbeFail());
            assertEquals(5, result.getPortFlags());
            delivered.set(result.getProbeResultOr(42));
        });
        worker.runNext();
        callbackExecutor.runNext();

        assertEquals(42, delivered.get());
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
            List<Runnable> pending =
                    new ArrayList<>(commands);
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
