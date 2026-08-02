package com.limelight.ui.filepush;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.limelight.computers.model.HostEndpoint;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class FilePushControllerTest {
    @Test
    public void hostLoadPublishesImmutableSnapshotOnCallbackExecutor() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        FilePushTarget target = target();
        FilePushController controller = new FilePushController(
                () -> Collections.singletonList(target),
                (ignored, listener) -> { },
                worker,
                callbacks);
        AtomicReference<FilePushController.Result<
                List<FilePushTarget>>> result = new AtomicReference<>();

        assertEquals(
                FilePushController.RequestStatus.ACCEPTED,
                controller.loadHosts(result::set));
        assertNull(result.get());
        worker.runNext();
        assertNull(result.get());
        callbacks.runNext();

        assertTrue(result.get().isSuccessful());
        assertEquals(
                Collections.singletonList(target),
                result.get().getValue());
        try {
            result.get().getValue().clear();
            throw new AssertionError("Expected immutable host snapshot");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void uploadCoalescesProgressAndOwnsOperationUntilCompletion() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        FilePushController controller = new FilePushController(
                Collections::emptyList,
                (target, listener) -> {
                    listener.onProgress(10, 100);
                    listener.onProgress(40, 100);
                },
                worker,
                callbacks);
        AtomicInteger progressCallbacks = new AtomicInteger();
        AtomicLong transferred = new AtomicLong();
        AtomicReference<FilePushController.Result<FilePushTarget>>
                result = new AtomicReference<>();

        assertEquals(
                FilePushController.RequestStatus.ACCEPTED,
                controller.upload(
                        target(),
                        new FilePushController.UploadCallback() {
                            @Override
                            public void onProgress(
                                    long transferredBytes,
                                    long totalBytes) {
                                progressCallbacks.incrementAndGet();
                                transferred.set(transferredBytes);
                            }

                            @Override
                            public void onCompleted(
                                    FilePushController.Result<
                                            FilePushTarget> completed) {
                                result.set(completed);
                            }
                        }));
        assertEquals(
                FilePushController.RequestStatus.ALREADY_RUNNING,
                controller.loadHosts(ignored -> { }));
        worker.runNext();

        assertEquals(2, callbacks.size());
        callbacks.runNext();
        assertEquals(1, progressCallbacks.get());
        assertEquals(40, transferred.get());
        assertTrue(controller.isUploadInProgress());
        callbacks.runNext();

        assertTrue(result.get().isSuccessful());
        assertFalse(controller.isUploadInProgress());
    }

    @Test
    public void destroyCancelsOwnershipAndSuppressesQueuedCallbacks() {
        ManualExecutorService worker = new ManualExecutorService();
        ManualExecutor callbacks = new ManualExecutor();
        FilePushController controller = new FilePushController(
                Collections::emptyList,
                (target, listener) -> { },
                worker,
                callbacks);
        AtomicInteger deliveries = new AtomicInteger();

        controller.loadHosts(result -> deliveries.incrementAndGet());
        worker.runNext();
        controller.destroy();
        callbacks.runAll();

        assertEquals(0, deliveries.get());
        assertEquals(
                FilePushController.RequestStatus.DESTROYED,
                controller.loadHosts(result -> { }));
    }

    private static FilePushTarget target() {
        return new FilePushTarget(
                "Host",
                new HostEndpoint(
                        HostEndpoint.Kind.LOCAL_IPV4,
                        "192.168.1.2",
                        47989),
                new TestCertificate("pinned"));
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        int size() {
            return tasks.size();
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

    private static final class ManualExecutorService
            extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> pending = new java.util.ArrayList<>(tasks);
            tasks.clear();
            return pending;
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
                throw new java.util.concurrent.RejectedExecutionException();
            }
            tasks.add(command);
        }

        void runNext() {
            tasks.remove().run();
        }
    }
}
