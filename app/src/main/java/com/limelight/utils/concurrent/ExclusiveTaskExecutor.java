package com.limelight.utils.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executes at most one task and rejects submissions while that task is busy.
 *
 * <p>There is deliberately no pending queue. Owners that expose a single
 * lifecycle operation already reject duplicate requests at their state
 * boundary; this executor makes the same invariant true at the scheduling
 * boundary so a missed state check cannot create hidden deferred work.</p>
 */
public final class ExclusiveTaskExecutor extends AbstractExecutorService
        implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    public ExclusiveTaskExecutor(String threadName) {
        String checkedName = Objects.requireNonNull(
                threadName,
                "threadName");
        if (checkedName.isEmpty()) {
            throw new IllegalArgumentException(
                    "threadName must not be empty");
        }

        executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable, checkedName);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task"));
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(
            long timeout,
            TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
