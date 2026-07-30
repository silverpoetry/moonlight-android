package com.limelight.ui.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StreamLaunchReporterTest {
    @Test
    public void reportIsAcceptedAndExecutedOnlyOnce() {
        ManualExecutor executor = new ManualExecutor();
        AtomicInteger reports = new AtomicInteger();
        StreamLaunchReporter reporter =
                new StreamLaunchReporter(executor, reports::incrementAndGet);

        assertTrue(reporter.reportOnce());
        assertFalse(reporter.reportOnce());
        assertEquals(0, reports.get());

        executor.runNext();

        assertEquals(1, reports.get());
        assertFalse(reporter.reportOnce());
    }

    @Test
    public void destroyCancelsQueuedReportAndRejectsNewWork() {
        ManualExecutor executor = new ManualExecutor();
        AtomicInteger reports = new AtomicInteger();
        StreamLaunchReporter reporter =
                new StreamLaunchReporter(executor, reports::incrementAndGet);

        assertTrue(reporter.reportOnce());
        reporter.destroy();
        reporter.destroy();
        executor.runNext();

        assertEquals(0, reports.get());
        assertTrue(executor.isShutdown());
        assertFalse(reporter.reportOnce());
    }

    @Test
    public void reportFailureIsContainedAndStillCountsAsRequested() {
        ManualExecutor executor = new ManualExecutor();
        AtomicInteger attempts = new AtomicInteger();
        StreamLaunchReporter reporter =
                new StreamLaunchReporter(executor, () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("expected");
                });

        assertTrue(reporter.reportOnce());
        executor.runNext();

        assertEquals(1, attempts.get());
        assertFalse(reporter.reportOnce());
    }

    @Test
    public void schedulingFailureAllowsRetry() {
        ManualExecutor executor = new ManualExecutor();
        executor.rejectNext = true;
        AtomicInteger reports = new AtomicInteger();
        StreamLaunchReporter reporter =
                new StreamLaunchReporter(executor, reports::incrementAndGet);

        assertFalse(reporter.reportOnce());
        assertTrue(reporter.reportOnce());
        executor.runNext();

        assertEquals(1, reports.get());
    }

    private static final class ManualExecutor
            extends AbstractExecutorService {
        private final List<Runnable> tasks = new ArrayList<>();
        private boolean shutdown;
        private boolean rejectNext;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> queued = new ArrayList<>(tasks);
            tasks.clear();
            return queued;
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
            if (rejectNext) {
                rejectNext = false;
                throw new IllegalStateException("expected rejection");
            }
            if (shutdown) {
                throw new IllegalStateException("executor is shut down");
            }
            tasks.add(command);
        }

        void runNext() {
            if (!tasks.isEmpty()) {
                tasks.remove(0).run();
            }
        }
    }
}
