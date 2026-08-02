package com.limelight.ui.hosts;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import com.limelight.LimeLog;
import com.limelight.computers.pairing.HostPairingUseCase;
import com.limelight.utils.concurrent.ExclusiveTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Lifecycle owner for the single host-pairing operation exposed by PcView.
 *
 * <p>Work is serialized on one owned executor. Duplicate requests are rejected
 * before they can freeze host polling, and destruction invalidates callbacks
 * already queued for the main thread. Cancellation is cooperative until the
 * remote pairing request begins; the use case then completes its credential
 * transaction to keep local and remote state consistent.</p>
 */
public final class HostPairingController {
    public enum RequestStatus {
        ACCEPTED,
        ALREADY_RUNNING,
        DESTROYED,
        UNAVAILABLE
    }

    public interface Operation {
        @WorkerThread
        HostPairingUseCase.Outcome execute(
                HostPairingUseCase.CancellationSignal cancellationSignal)
                throws Exception;
    }

    public interface Callback {
        @AnyThread
        void onCompleted(Result result);
    }

    public static final class Result {
        private final HostPairingUseCase.Outcome outcome;
        private final Exception failure;

        private Result(
                HostPairingUseCase.Outcome outcome,
                Exception failure) {
            this.outcome = outcome;
            this.failure = failure;
        }

        public boolean isSuccessful() {
            return failure == null &&
                    (outcome == HostPairingUseCase.Outcome.PAIRED ||
                            outcome == HostPairingUseCase.Outcome
                                    .ALREADY_PAIRED);
        }

        public HostPairingUseCase.Outcome getOutcome() {
            return outcome;
        }

        public Exception getFailure() {
            return failure;
        }
    }

    private final Object lock = new Object();
    private final ExecutorService workerExecutor;
    private final Executor callbackExecutor;

    private Future<?> pendingTask;
    private long generation;
    private boolean destroyed;

    @AnyThread
    public static HostPairingController create(
            Executor callbackExecutor) {
        return new HostPairingController(
                new ExclusiveTaskExecutor("HostPairing"),
                callbackExecutor);
    }

    HostPairingController(
            ExecutorService workerExecutor,
            Executor callbackExecutor) {
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor");
    }

    @AnyThread
    public RequestStatus request(
            Operation operation,
            Callback callback) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(callback, "callback");

        final long operationGeneration;
        final FutureTask<Void> task;
        synchronized (lock) {
            if (destroyed) {
                return RequestStatus.DESTROYED;
            }
            if (pendingTask != null) {
                return RequestStatus.ALREADY_RUNNING;
            }
            generation++;
            operationGeneration = generation;
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
            synchronized (lock) {
                if (pendingTask == task) {
                    pendingTask = null;
                }
            }
            LimeLog.severe(
                    "Unable to schedule host pairing: " + error);
            return RequestStatus.UNAVAILABLE;
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
                // Do not interrupt a request that may already have crossed
                // the remote pairing boundary. Its credential transaction
                // must finish even though its UI callback is invalidated.
                pendingTask.cancel(false);
                pendingTask = null;
            }
        }
        workerExecutor.shutdown();
    }

    private void runOperation(
            long operationGeneration,
            Operation operation,
            Callback callback) {
        Result result;
        try {
            HostPairingUseCase.Outcome outcome = Objects.requireNonNull(
                    operation.execute(() -> isCanceled(
                            operationGeneration)),
                    "pairingOutcome");
            result = new Result(outcome, null);
        }
        catch (Exception error) {
            result = new Result(null, error);
            LimeLog.warning(
                    "Host pairing operation failed: " +
                            error.getClass().getSimpleName());
        }

        Result completedResult = result;
        try {
            callbackExecutor.execute(() -> deliver(
                    operationGeneration,
                    callback,
                    completedResult));
        }
        catch (RuntimeException error) {
            clearIfCurrent(operationGeneration);
            LimeLog.severe(
                    "Unable to publish host pairing result: " + error);
        }
    }

    private boolean isCanceled(long operationGeneration) {
        synchronized (lock) {
            return destroyed ||
                    generation != operationGeneration ||
                    Thread.currentThread().isInterrupted();
        }
    }

    private void deliver(
            long operationGeneration,
            Callback callback,
            Result result) {
        synchronized (lock) {
            if (destroyed || generation != operationGeneration) {
                return;
            }
            pendingTask = null;
        }
        callback.onCompleted(result);
    }

    private void clearIfCurrent(long operationGeneration) {
        synchronized (lock) {
            if (generation == operationGeneration) {
                pendingTask = null;
            }
        }
    }
}
