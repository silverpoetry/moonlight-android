package com.limelight.computers.session;

import com.limelight.LimeLog;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * One-shot owner for a host quit that must outlive the stream screen.
 *
 * <p>The controller contains no Android owner and deliberately has no cancel
 * operation: once the local stream has closed, the host request must finish
 * even if the Activity is destroyed. Its private scheduler is released after
 * completion or admission failure.</p>
 */
public final class DeferredHostQuitController {
    public enum RequestStatus {
        ACCEPTED,
        ALREADY_REQUESTED,
        UNAVAILABLE
    }

    public interface Callback {
        void onCompleted(Result result);
    }

    public static final class Result {
        private final HostQuitUseCase.Outcome outcome;
        private final Exception failure;

        private Result(
                HostQuitUseCase.Outcome outcome,
                Exception failure) {
            this.outcome = outcome;
            this.failure = failure;
        }

        public HostQuitUseCase.Outcome getOutcome() {
            return outcome;
        }

        public Exception getFailure() {
            return failure;
        }

        public boolean isSuccessful() {
            return failure == null;
        }
    }

    interface Scheduler {
        void schedule(Runnable command, long delayMillis);

        void close();
    }

    private static final ThreadFactory THREAD_FACTORY = command -> {
        Thread thread = new Thread(command, "DeferredHostQuit");
        thread.setDaemon(true);
        return thread;
    };

    private final Object lock = new Object();
    private final Scheduler scheduler;
    private final HostQuitUseCase useCase;
    private boolean requested;

    public static DeferredHostQuitController create() {
        ScheduledThreadPoolExecutor executor =
                new ScheduledThreadPoolExecutor(
                        1,
                        THREAD_FACTORY,
                        new ThreadPoolExecutor.AbortPolicy());
        executor.setRemoveOnCancelPolicy(true);
        return new DeferredHostQuitController(
                new Scheduler() {
                    @Override
                    public void schedule(
                            Runnable command,
                            long delayMillis) {
                        executor.schedule(
                                command,
                                delayMillis,
                                TimeUnit.MILLISECONDS);
                    }

                    @Override
                    public void close() {
                        executor.shutdown();
                    }
                },
                new HostQuitUseCase());
    }

    DeferredHostQuitController(
            Scheduler scheduler,
            HostQuitUseCase useCase) {
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "scheduler");
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public RequestStatus request(
            HostQuitUseCase.Backend backend,
            long delayMillis,
            Callback callback) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(callback, "callback");

        synchronized (lock) {
            if (requested) {
                return RequestStatus.ALREADY_REQUESTED;
            }
            requested = true;
        }

        try {
            scheduler.schedule(
                    () -> execute(backend, callback),
                    Math.max(0L, delayMillis));
            return RequestStatus.ACCEPTED;
        }
        catch (RejectedExecutionException error) {
            closeAfterAdmissionFailure(error);
            return RequestStatus.UNAVAILABLE;
        }
        catch (RuntimeException error) {
            closeAfterAdmissionFailure(error);
            return RequestStatus.UNAVAILABLE;
        }
    }

    private void execute(
            HostQuitUseCase.Backend backend,
            Callback callback) {
        Result result;
        try {
            result = new Result(useCase.execute(backend), null);
        }
        catch (Exception failure) {
            result = new Result(null, failure);
        }

        try {
            callback.onCompleted(result);
        }
        catch (RuntimeException error) {
            LimeLog.severe(
                    "Deferred host quit callback failed: " +
                            error.getClass().getSimpleName());
        }
        finally {
            scheduler.close();
        }
    }

    private void closeAfterAdmissionFailure(RuntimeException error) {
        LimeLog.severe(
                "Unable to schedule deferred host quit: " +
                        error.getClass().getSimpleName());
        scheduler.close();
    }
}
