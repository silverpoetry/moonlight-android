package com.limelight.computers.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class DeferredHostQuitControllerTest {
    @Test
    public void executesOnceAfterRequestedDelayAndClosesOwner() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        DeferredHostQuitController controller =
                new DeferredHostQuitController(
                        scheduler,
                        new HostQuitUseCase());
        AtomicReference<DeferredHostQuitController.Result> result =
                new AtomicReference<>();

        assertEquals(
                DeferredHostQuitController.RequestStatus.ACCEPTED,
                controller.request(() -> true, 275L, result::set));

        assertEquals(275L, scheduler.getDelayMillis());
        assertTrue(scheduler.isClosed());
        assertTrue(result.get().isSuccessful());
        assertEquals(
                HostQuitUseCase.Outcome.QUIT,
                result.get().getOutcome());
        assertNull(result.get().getFailure());
    }

    @Test
    public void clampsNegativeDelayAndRejectsDuplicateRequest() {
        HoldingScheduler scheduler = new HoldingScheduler();
        DeferredHostQuitController controller =
                new DeferredHostQuitController(
                        scheduler,
                        new HostQuitUseCase());

        assertEquals(
                DeferredHostQuitController.RequestStatus.ACCEPTED,
                controller.request(() -> true, -1L, result -> { }));
        assertEquals(0L, scheduler.getDelayMillis());
        assertEquals(
                DeferredHostQuitController.RequestStatus
                        .ALREADY_REQUESTED,
                controller.request(() -> true, 0L, result -> { }));
    }

    @Test
    public void preservesBackendFailureAndClosesOwner() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        DeferredHostQuitController controller =
                new DeferredHostQuitController(
                        scheduler,
                        new HostQuitUseCase());
        IOException failure = new IOException("network");
        AtomicReference<DeferredHostQuitController.Result> result =
                new AtomicReference<>();

        controller.request(
                () -> {
                    throw failure;
                },
                0L,
                result::set);

        assertFalse(result.get().isSuccessful());
        assertNull(result.get().getOutcome());
        assertSame(failure, result.get().getFailure());
        assertTrue(scheduler.isClosed());
    }

    @Test
    public void rejectedSchedulingReturnsUnavailableAndClosesOwner() {
        RejectingScheduler scheduler = new RejectingScheduler();
        DeferredHostQuitController controller =
                new DeferredHostQuitController(
                        scheduler,
                        new HostQuitUseCase());
        AtomicInteger callbacks = new AtomicInteger();

        assertEquals(
                DeferredHostQuitController.RequestStatus.UNAVAILABLE,
                controller.request(
                        () -> true,
                        0L,
                        result -> callbacks.incrementAndGet()));
        assertEquals(0, callbacks.get());
        assertTrue(scheduler.isClosed());
    }

    private static class HoldingScheduler
            implements DeferredHostQuitController.Scheduler {
        private long delayMillis = -1L;
        private boolean closed;

        @Override
        public void schedule(Runnable command, long delayMillis) {
            this.delayMillis = delayMillis;
        }

        @Override
        public void close() {
            closed = true;
        }

        long getDelayMillis() {
            return delayMillis;
        }

        boolean isClosed() {
            return closed;
        }
    }

    private static final class ImmediateScheduler
            extends HoldingScheduler {
        @Override
        public void schedule(Runnable command, long delayMillis) {
            super.schedule(command, delayMillis);
            command.run();
        }
    }

    private static final class RejectingScheduler
            extends HoldingScheduler {
        @Override
        public void schedule(Runnable command, long delayMillis) {
            throw new RejectedExecutionException("closed");
        }
    }
}
