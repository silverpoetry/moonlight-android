package com.limelight.utils.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Runs one task at a time while retaining at most the newest pending task.
 *
 * <p>This executor is intended for state synchronization where an in-flight
 * operation may finish, but queued work for an older state is obsolete as
 * soon as a newer state arrives. Its single pending slot makes that policy
 * explicit and prevents an unavailable or slow endpoint from creating an
 * unbounded backlog.</p>
 */
public final class LatestTaskExecutor implements Executor, AutoCloseable {
    private static final int PENDING_CAPACITY = 1;

    private final ThreadPoolExecutor executor;

    public LatestTaskExecutor(String threadName) {
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
                new ArrayBlockingQueue<>(PENDING_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, checkedName);
                    thread.setDaemon(true);
                    return thread;
                },
                new ReplacePendingTaskPolicy());
    }

    @Override
    public synchronized void execute(Runnable task) {
        execute(task, () -> { });
    }

    /**
     * Enqueues work and invokes {@code onSuperseded} if it is replaced before
     * execution or discarded during shutdown. The callback must be fast and
     * must not throw.
     */
    public synchronized void execute(
            Runnable task,
            Runnable onSuperseded) {
        executor.execute(new PendingTask(
                Objects.requireNonNull(task, "task"),
                Objects.requireNonNull(
                        onSuperseded,
                        "onSuperseded")));
    }

    @Override
    public synchronized void close() {
        List<Runnable> discarded = executor.shutdownNow();
        for (Runnable task : discarded) {
            discard(task);
        }
    }

    private static final class ReplacePendingTaskPolicy
            implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(
                Runnable task,
                ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                discard(task);
                throw new RejectedExecutionException(
                        "Latest-task executor is closed");
            }

            discard(executor.getQueue().poll());
            if (!executor.getQueue().offer(task)) {
                discard(task);
                throw new RejectedExecutionException(
                        "Unable to replace pending task");
            }
        }
    }

    private static void discard(Runnable task) {
        if (task instanceof PendingTask) {
            ((PendingTask) task).onSuperseded();
        }
    }

    private static final class PendingTask implements Runnable {
        private final Runnable task;
        private final Runnable supersededCallback;

        PendingTask(Runnable task, Runnable supersededCallback) {
            this.task = task;
            this.supersededCallback = supersededCallback;
        }

        @Override
        public void run() {
            task.run();
        }

        void onSuperseded() {
            supersededCallback.run();
        }
    }
}
