package com.limelight.ui.stream;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import com.limelight.LimeLog;
import com.limelight.utils.concurrent.LatestTaskExecutor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Owns the bounded background work used to diagnose one stream failure.
 *
 * <p>Only the newest request may publish a result. Destruction cancels pending
 * work and invalidates callback tasks that were already queued.</p>
 */
public final class StreamFailureDiagnostics {
    public interface Probe {
        @WorkerThread
        int test(int portFlags);
    }

    public interface Callback {
        @AnyThread
        void onResult(Result result);
    }

    public static final class Result {
        private final int portFlags;
        private final int probeResult;
        private final Throwable failure;

        private Result(
                int portFlags,
                int probeResult,
                Throwable failure) {
            this.portFlags = portFlags;
            this.probeResult = probeResult;
            this.failure = failure;
        }

        @AnyThread
        public int getPortFlags() {
            return portFlags;
        }

        @AnyThread
        public int getProbeResultOr(int fallback) {
            return failure == null ? probeResult : fallback;
        }

        @AnyThread
        public boolean didProbeFail() {
            return failure != null;
        }
    }

    private final Object lock = new Object();
    private final ExecutorService workerExecutor;
    private final Executor callbackExecutor;
    private final Probe probe;

    private Future<?> pendingTask;
    private long generation;
    private boolean destroyed;

    @AnyThread
    public static StreamFailureDiagnostics create(
            Probe probe,
            Executor callbackExecutor) {
        return new StreamFailureDiagnostics(
                new LatestTaskExecutor(
                        "StreamFailureDiagnostics"),
                callbackExecutor,
                probe);
    }

    StreamFailureDiagnostics(
            ExecutorService workerExecutor,
            Executor callbackExecutor,
            Probe probe) {
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor");
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    /**
     * Replaces any older unpublished diagnosis with this request.
     *
     * @return {@code false} when the owner is already destroyed or the worker
     *         cannot accept the request
     */
    @AnyThread
    public boolean request(int portFlags, Callback callback) {
        Objects.requireNonNull(callback, "callback");

        final long requestGeneration;
        final Future<?> previousTask;
        final FutureTask<Void> newTask;
        synchronized (lock) {
            if (destroyed) {
                return false;
            }
            generation++;
            requestGeneration = generation;
            previousTask = pendingTask;
            newTask = new FutureTask<>(
                    () -> {
                        runProbe(
                                requestGeneration,
                                portFlags,
                                callback);
                        return null;
                    });
            pendingTask = newTask;
        }

        if (previousTask != null) {
            previousTask.cancel(true);
        }

        try {
            workerExecutor.execute(newTask);
            return true;
        } catch (RuntimeException error) {
            synchronized (lock) {
                if (pendingTask == newTask) {
                    pendingTask = null;
                }
            }
            LimeLog.severe(
                    "Unable to schedule stream failure diagnostics: " +
                            error);
            return false;
        }
    }

    @AnyThread
    public void destroy() {
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            generation++;
            if (pendingTask != null) {
                pendingTask.cancel(true);
                pendingTask = null;
            }
        }

        List<Runnable> discardedTasks = workerExecutor.shutdownNow();
        if (!discardedTasks.isEmpty()) {
            LimeLog.info(
                    "Discarded " + discardedTasks.size() +
                            " pending stream diagnostic task(s)");
        }
    }

    private void runProbe(
            long requestGeneration,
            int portFlags,
            Callback callback) {
        Result result;
        try {
            result = new Result(
                    portFlags,
                    probe.test(portFlags),
                    null);
        } catch (RuntimeException | Error error) {
            result = new Result(portFlags, 0, error);
            LimeLog.warning(
                    "Stream failure diagnostics failed: " + error);
        }

        final Result completedResult = result;
        try {
            callbackExecutor.execute(() -> deliver(
                    requestGeneration,
                    callback,
                    completedResult));
        } catch (RuntimeException error) {
            LimeLog.severe(
                    "Unable to publish stream failure diagnostics: " +
                            error);
        }
    }

    private void deliver(
            long requestGeneration,
            Callback callback,
            Result result) {
        synchronized (lock) {
            if (destroyed || generation != requestGeneration) {
                return;
            }
            pendingTask = null;
        }
        callback.onResult(result);
    }
}
