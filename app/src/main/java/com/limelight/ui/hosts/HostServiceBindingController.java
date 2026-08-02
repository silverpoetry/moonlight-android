package com.limelight.ui.hosts;

import com.limelight.LimeLog;
import com.limelight.utils.concurrent.LatestTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Lifecycle owner for asynchronous host-service binding initialization.
 *
 * <p>Each connection replaces and interrupts its predecessor. Disconnect and
 * destroy invalidate callbacks already queued for the owner thread, so a stale
 * binder can never become visible after a newer connection.</p>
 */
public final class HostServiceBindingController {
    public enum ConnectStatus {
        ACCEPTED,
        DESTROYED,
        UNAVAILABLE
    }

    public interface CancellationSignal {
        boolean isCanceled();
    }

    public interface Initializer<T> {
        T initialize(CancellationSignal cancellationSignal)
                throws Exception;
    }

    public interface Callback<T> {
        void onCompleted(Result<T> result);
    }

    public interface DiscardHandler<T> {
        void discard(T value);
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

    private final Object lock = new Object();
    private final ExecutorService workerExecutor;
    private final Executor callbackExecutor;

    private Future<?> pendingTask;
    private long generation;
    private boolean destroyed;

    public static HostServiceBindingController create(
            Executor callbackExecutor) {
        return new HostServiceBindingController(
                new LatestTaskExecutor("HostServiceBinding"),
                callbackExecutor);
    }

    HostServiceBindingController(
            ExecutorService workerExecutor,
            Executor callbackExecutor) {
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor");
    }

    public <T> ConnectStatus connect(
            Initializer<T> initializer,
            Callback<T> callback) {
        return connect(initializer, callback, value -> { });
    }

    public <T> ConnectStatus connect(
            Initializer<T> initializer,
            Callback<T> callback,
            DiscardHandler<T> discardHandler) {
        Objects.requireNonNull(initializer, "initializer");
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(discardHandler, "discardHandler");

        Future<?> supersededTask;
        long operationGeneration;
        FutureTask<Void> task;
        synchronized (lock) {
            if (destroyed) {
                return ConnectStatus.DESTROYED;
            }
            supersededTask = pendingTask;
            operationGeneration = nextGenerationLocked();
            task = new FutureTask<>(() -> {
                runInitializer(
                        operationGeneration,
                        initializer,
                        callback,
                        discardHandler);
                return null;
            });
            pendingTask = task;
        }

        if (supersededTask != null) {
            supersededTask.cancel(true);
        }
        try {
            workerExecutor.execute(task);
            return ConnectStatus.ACCEPTED;
        }
        catch (RuntimeException error) {
            synchronized (lock) {
                if (pendingTask == task) {
                    pendingTask = null;
                }
            }
            LimeLog.severe(
                    "Unable to schedule host service binding: " +
                            error.getClass().getSimpleName());
            return ConnectStatus.UNAVAILABLE;
        }
    }

    public void disconnect() {
        Future<?> task;
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            nextGenerationLocked();
            task = pendingTask;
            pendingTask = null;
        }
        if (task != null) {
            task.cancel(true);
        }
    }

    public void destroy() {
        Future<?> task;
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            nextGenerationLocked();
            task = pendingTask;
            pendingTask = null;
        }
        if (task != null) {
            task.cancel(true);
        }
        workerExecutor.shutdownNow();
    }

    private <T> void runInitializer(
            long operationGeneration,
            Initializer<T> initializer,
            Callback<T> callback,
            DiscardHandler<T> discardHandler) {
        Result<T> result;
        try {
            result = new Result<>(
                    initializer.initialize(
                            () -> isCanceled(operationGeneration)),
                    null);
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
                    discardHandler,
                    completedResult));
        }
        catch (RuntimeException error) {
            clearIfCurrent(operationGeneration);
            discard(completedResult, discardHandler);
            LimeLog.severe(
                    "Unable to publish host service binding: " +
                            error.getClass().getSimpleName());
        }
    }

    private boolean isCanceled(long operationGeneration) {
        synchronized (lock) {
            return destroyed ||
                    generation != operationGeneration ||
                    Thread.currentThread().isInterrupted();
        }
    }

    private <T> void deliver(
            long operationGeneration,
            Callback<T> callback,
            DiscardHandler<T> discardHandler,
            Result<T> result) {
        boolean current;
        synchronized (lock) {
            current = !destroyed &&
                    generation == operationGeneration;
            if (current) {
                pendingTask = null;
            }
        }
        if (!current) {
            discard(result, discardHandler);
            return;
        }
        callback.onCompleted(result);
    }

    private static <T> void discard(
            Result<T> result,
            DiscardHandler<T> discardHandler) {
        if (result.isSuccessful() && result.getValue() != null) {
            try {
                discardHandler.discard(result.getValue());
            }
            catch (RuntimeException error) {
                LimeLog.warning(
                        "Unable to discard stale host binding result: " +
                                error.getClass().getSimpleName());
            }
        }
    }

    private void clearIfCurrent(long operationGeneration) {
        synchronized (lock) {
            if (generation == operationGeneration) {
                pendingTask = null;
            }
        }
    }

    private long nextGenerationLocked() {
        if (generation == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Host service binding generation overflow");
        }
        return ++generation;
    }
}
