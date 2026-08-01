package com.limelight.ui.hosts;

import com.limelight.LimeLog;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

/**
 * Lifecycle owner for the single foreground operation on the manual-host
 * screen.
 *
 * <p>The controller rejects duplicate submissions, interrupts owned work when
 * the screen stops, and generation-gates callbacks already queued for the UI
 * thread. It intentionally knows nothing about Android widgets, host
 * protocols, or localized presentation.</p>
 */
public final class ManualHostOperationController {
    public enum RequestStatus {
        ACCEPTED,
        ALREADY_RUNNING,
        DESTROYED,
        UNAVAILABLE
    }

    public interface Operation<T> {
        T execute() throws Exception;
    }

    public interface Callback<T> {
        void onCompleted(Result<T> result);
    }

    public static final class Result<T> {
        private final T value;
        private final Exception failure;

        private Result(T value, Exception failure) {
            this.value = value;
            this.failure = failure;
        }

        public T getValue() {
            return value;
        }

        public Exception getFailure() {
            return failure;
        }

        public boolean isSuccessful() {
            return failure == null;
        }
    }

    private static final ThreadFactory THREAD_FACTORY = command -> {
        Thread thread = new Thread(command, "ManualHostOperation");
        thread.setDaemon(true);
        return thread;
    };

    private final Object stateLock = new Object();
    private final ExecutorService workerExecutor;
    private final Executor callbackExecutor;

    private Future<?> pendingTask;
    private long generation;
    private boolean destroyed;

    public static ManualHostOperationController create(
            Executor callbackExecutor) {
        return new ManualHostOperationController(
                Executors.newSingleThreadExecutor(THREAD_FACTORY),
                callbackExecutor);
    }

    ManualHostOperationController(
            ExecutorService workerExecutor,
            Executor callbackExecutor) {
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor");
    }

    public <T> RequestStatus request(
            Operation<T> operation,
            Callback<T> callback) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(callback, "callback");

        final long operationGeneration;
        final FutureTask<Void> task;
        synchronized (stateLock) {
            if (destroyed) {
                return RequestStatus.DESTROYED;
            }
            if (pendingTask != null) {
                return RequestStatus.ALREADY_RUNNING;
            }
            if (generation == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "Manual-host operation generation overflow");
            }
            operationGeneration = ++generation;
            task = new FutureTask<>(() -> {
                runOperation(
                        operationGeneration,
                        operation,
                        callback);
                return null;
            });
            pendingTask = task;
        }

        try {
            workerExecutor.execute(task);
            return RequestStatus.ACCEPTED;
        }
        catch (RuntimeException error) {
            synchronized (stateLock) {
                if (pendingTask == task) {
                    pendingTask = null;
                }
            }
            LimeLog.severe(
                    "Unable to schedule manual-host operation: " +
                            error.getClass().getSimpleName());
            return RequestStatus.UNAVAILABLE;
        }
    }

    public void cancelCurrent() {
        Future<?> task;
        synchronized (stateLock) {
            if (destroyed || pendingTask == null) {
                return;
            }
            generation++;
            task = pendingTask;
            pendingTask = null;
        }
        task.cancel(true);
    }

    public void destroy() {
        Future<?> task;
        synchronized (stateLock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            generation++;
            task = pendingTask;
            pendingTask = null;
        }
        if (task != null) {
            task.cancel(true);
        }
        workerExecutor.shutdownNow();
    }

    private <T> void runOperation(
            long operationGeneration,
            Operation<T> operation,
            Callback<T> callback) {
        Result<T> result;
        try {
            result = new Result<>(operation.execute(), null);
        }
        catch (Exception error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            result = new Result<>(null, error);
        }

        Result<T> completedResult = result;
        try {
            callbackExecutor.execute(() -> deliver(
                    operationGeneration,
                    callback,
                    completedResult));
        }
        catch (RuntimeException error) {
            clearIfCurrent(operationGeneration);
            LimeLog.severe(
                    "Unable to publish manual-host result: " +
                            error.getClass().getSimpleName());
        }
    }

    private <T> void deliver(
            long operationGeneration,
            Callback<T> callback,
            Result<T> result) {
        synchronized (stateLock) {
            if (destroyed || generation != operationGeneration) {
                return;
            }
            pendingTask = null;
        }
        callback.onCompleted(result);
    }

    private void clearIfCurrent(long operationGeneration) {
        synchronized (stateLock) {
            if (generation == operationGeneration) {
                pendingTask = null;
            }
        }
    }
}
