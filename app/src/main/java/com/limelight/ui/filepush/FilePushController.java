package com.limelight.ui.filepush;

import com.limelight.LimeLog;
import com.limelight.utils.concurrent.ExclusiveTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Lifecycle and execution owner for the desktop-file push screen.
 *
 * <p>Host discovery and upload run on one exclusive worker. Callbacks are
 * generation-gated after destruction, and upload progress is coalesced so a
 * fast producer can enqueue at most one pending UI update. The supplied
 * callback executor must serialize its tasks.</p>
 */
public final class FilePushController {
    public enum RequestStatus {
        ACCEPTED,
        ALREADY_RUNNING,
        DESTROYED,
        UNAVAILABLE
    }

    public interface HostCatalog {
        List<FilePushTarget> loadTargets() throws Exception;
    }

    public interface Uploader {
        void upload(
                FilePushTarget target,
                ProgressListener listener) throws Exception;
    }

    public interface ProgressListener {
        void onProgress(long transferredBytes, long totalBytes);
    }

    public interface Callback<T> {
        void onCompleted(Result<T> result);
    }

    public interface UploadCallback extends Callback<FilePushTarget> {
        void onProgress(long transferredBytes, long totalBytes);
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

    private enum Operation {
        IDLE,
        LOAD_HOSTS,
        UPLOAD,
        DESTROYED
    }

    private final Object stateLock = new Object();
    private final HostCatalog hostCatalog;
    private final Uploader uploader;
    private final ExecutorService workerExecutor;
    private final Executor callbackExecutor;

    private Operation operation = Operation.IDLE;
    private Future<?> activeTask;
    private long generation;
    private boolean progressCallbackScheduled;
    private long latestTransferredBytes;
    private long latestTotalBytes;

    public static FilePushController create(
            HostCatalog hostCatalog,
            Uploader uploader,
            Executor callbackExecutor) {
        return new FilePushController(
                hostCatalog,
                uploader,
                new ExclusiveTaskExecutor("DesktopFilePush"),
                callbackExecutor);
    }

    FilePushController(
            HostCatalog hostCatalog,
            Uploader uploader,
            ExecutorService workerExecutor,
            Executor callbackExecutor) {
        this.hostCatalog = Objects.requireNonNull(
                hostCatalog,
                "hostCatalog");
        this.uploader = Objects.requireNonNull(uploader, "uploader");
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor");
    }

    public RequestStatus loadHosts(
            Callback<List<FilePushTarget>> callback) {
        Objects.requireNonNull(callback, "callback");
        return submit(
                Operation.LOAD_HOSTS,
                generation -> runHostLoad(generation, callback));
    }

    public RequestStatus upload(
            FilePushTarget target,
            UploadCallback callback) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(callback, "callback");
        return submit(
                Operation.UPLOAD,
                generation -> runUpload(
                        generation,
                        target,
                        callback));
    }

    public boolean isUploadInProgress() {
        synchronized (stateLock) {
            return operation == Operation.UPLOAD;
        }
    }

    public void destroy() {
        Future<?> task;
        synchronized (stateLock) {
            if (operation == Operation.DESTROYED) {
                return;
            }
            operation = Operation.DESTROYED;
            generation++;
            progressCallbackScheduled = false;
            task = activeTask;
            activeTask = null;
        }
        if (task != null) {
            task.cancel(true);
        }
        workerExecutor.shutdownNow();
    }

    private RequestStatus submit(
            Operation requestedOperation,
            GenerationTask generationTask) {
        final long taskGeneration;
        final FutureTask<Void> task;
        synchronized (stateLock) {
            if (operation == Operation.DESTROYED) {
                return RequestStatus.DESTROYED;
            }
            if (operation != Operation.IDLE) {
                return RequestStatus.ALREADY_RUNNING;
            }
            if (generation == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "File-push generation overflow");
            }
            taskGeneration = ++generation;
            operation = requestedOperation;
            progressCallbackScheduled = false;
            task = new FutureTask<>(() -> {
                generationTask.run(taskGeneration);
                return null;
            });
            activeTask = task;
        }

        try {
            workerExecutor.execute(task);
            return RequestStatus.ACCEPTED;
        }
        catch (RuntimeException failure) {
            clearIfCurrent(taskGeneration);
            LimeLog.severe(
                    "Unable to schedule file-push operation: " +
                            failure.getClass().getSimpleName());
            return RequestStatus.UNAVAILABLE;
        }
    }

    private void runHostLoad(
            long taskGeneration,
            Callback<List<FilePushTarget>> callback) {
        Result<List<FilePushTarget>> result;
        try {
            List<FilePushTarget> loaded = hostCatalog.loadTargets();
            List<FilePushTarget> snapshot = new ArrayList<>(
                    Objects.requireNonNull(loaded, "loadedTargets"));
            for (FilePushTarget target : snapshot) {
                Objects.requireNonNull(target, "target");
            }
            result = success(Collections.unmodifiableList(snapshot));
        }
        catch (Exception failure) {
            result = failure(failure);
        }
        publishCompletion(taskGeneration, callback, result);
    }

    private void runUpload(
            long taskGeneration,
            FilePushTarget target,
            UploadCallback callback) {
        Result<FilePushTarget> result;
        try {
            uploader.upload(
                    target,
                    (transferredBytes, totalBytes) -> publishProgress(
                            taskGeneration,
                            callback,
                            transferredBytes,
                            totalBytes));
            result = success(target);
        }
        catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            result = failure(failure);
        }
        publishCompletion(taskGeneration, callback, result);
    }

    private void publishProgress(
            long taskGeneration,
            UploadCallback callback,
            long transferredBytes,
            long totalBytes) {
        synchronized (stateLock) {
            if (!isCurrent(taskGeneration, Operation.UPLOAD)) {
                return;
            }
            latestTransferredBytes = transferredBytes;
            latestTotalBytes = totalBytes;
            if (progressCallbackScheduled) {
                return;
            }
            progressCallbackScheduled = true;
        }

        try {
            callbackExecutor.execute(() -> deliverProgress(
                    taskGeneration,
                    callback));
        }
        catch (RuntimeException failure) {
            synchronized (stateLock) {
                if (generation == taskGeneration) {
                    progressCallbackScheduled = false;
                }
            }
            LimeLog.severe(
                    "Unable to publish file-push progress: " +
                            failure.getClass().getSimpleName());
        }
    }

    private void deliverProgress(
            long taskGeneration,
            UploadCallback callback) {
        final long transferredBytes;
        final long totalBytes;
        synchronized (stateLock) {
            if (!isCurrent(taskGeneration, Operation.UPLOAD)) {
                return;
            }
            transferredBytes = latestTransferredBytes;
            totalBytes = latestTotalBytes;
            progressCallbackScheduled = false;
        }
        callback.onProgress(transferredBytes, totalBytes);
    }

    private <T> void publishCompletion(
            long taskGeneration,
            Callback<T> callback,
            Result<T> result) {
        try {
            callbackExecutor.execute(() -> deliverCompletion(
                    taskGeneration,
                    callback,
                    result));
        }
        catch (RuntimeException failure) {
            clearIfCurrent(taskGeneration);
            LimeLog.severe(
                    "Unable to publish file-push result: " +
                            failure.getClass().getSimpleName());
        }
    }

    private <T> void deliverCompletion(
            long taskGeneration,
            Callback<T> callback,
            Result<T> result) {
        synchronized (stateLock) {
            if (operation == Operation.DESTROYED ||
                    generation != taskGeneration) {
                return;
            }
            operation = Operation.IDLE;
            activeTask = null;
            progressCallbackScheduled = false;
        }
        callback.onCompleted(result);
    }

    private boolean isCurrent(
            long taskGeneration,
            Operation expectedOperation) {
        return generation == taskGeneration &&
                operation == expectedOperation;
    }

    private void clearIfCurrent(long taskGeneration) {
        synchronized (stateLock) {
            if (generation == taskGeneration &&
                    operation != Operation.DESTROYED) {
                operation = Operation.IDLE;
                activeTask = null;
                progressCallbackScheduled = false;
            }
        }
    }

    private static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    private static <T> Result<T> failure(Exception failure) {
        return new Result<>(
                null,
                Objects.requireNonNull(failure, "failure"));
    }

    private interface GenerationTask {
        void run(long taskGeneration);
    }
}
