package com.limelight.ui.stream;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.limelight.LimeLog;
import com.limelight.nvstream.mic.MicrophoneUplinkEndpoint;
import com.limelight.nvstream.mic.MicrophoneUplinkState;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

/**
 * Owns permission gating and serialized microphone toggles for one stream.
 *
 * <p>Transport and capture remain in {@link MicrophoneUplinkEndpoint}. This
 * owner prevents repeated menu taps, permission callbacks, and Activity
 * destruction from creating overlapping lifecycle transitions.</p>
 */
public final class StreamMicrophoneController {
    public interface PermissionGateway {
        @MainThread
        boolean isGranted();

        @MainThread
        void requestPermission();
    }

    public interface Feedback {
        @MainThread
        void onUnsupported();

        @MainThread
        void onPermissionDenied();

        @MainThread
        void onOperationFailed(String message);

        @MainThread
        void onStateChanged();
    }

    private enum Operation {
        START,
        STOP
    }

    private static final ThreadFactory THREAD_FACTORY = command -> {
        Thread thread = new Thread(command, "StreamMicrophone");
        thread.setDaemon(true);
        return thread;
    };

    private final Object lock = new Object();
    private final MicrophoneUplinkEndpoint endpoint;
    private final PermissionGateway permissionGateway;
    private final Feedback feedback;
    private final ExecutorService workerExecutor;
    private final Executor mainExecutor;

    private Future<?> pendingOperation;
    private boolean awaitingPermission;
    private boolean pendingStartAfterPermission;
    private boolean destroyed;

    @MainThread
    public static StreamMicrophoneController create(
            MicrophoneUplinkEndpoint endpoint,
            PermissionGateway permissionGateway,
            Feedback feedback,
            Executor mainExecutor) {
        return new StreamMicrophoneController(
                endpoint,
                permissionGateway,
                feedback,
                Executors.newSingleThreadExecutor(THREAD_FACTORY),
                mainExecutor);
    }

    StreamMicrophoneController(
            MicrophoneUplinkEndpoint endpoint,
            PermissionGateway permissionGateway,
            Feedback feedback,
            ExecutorService workerExecutor,
            Executor mainExecutor) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.permissionGateway = Objects.requireNonNull(
                permissionGateway,
                "permissionGateway");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.mainExecutor = Objects.requireNonNull(
                mainExecutor,
                "mainExecutor");
    }

    @AnyThread
    public boolean isActive() {
        synchronized (lock) {
            return !destroyed && endpoint.isMicUplinkActive();
        }
    }

    /**
     * Requests a serialized state transition.
     *
     * @return {@code true} when work or a permission request was accepted
     */
    @MainThread
    public boolean toggle() {
        final Operation operation;
        final boolean requestPermission;
        synchronized (lock) {
            if (destroyed || pendingOperation != null ||
                    awaitingPermission) {
                return false;
            }

            MicrophoneUplinkState state =
                    endpoint.getMicUplinkState();
            if (state == MicrophoneUplinkState.STARTING ||
                    state == MicrophoneUplinkState.STOPPING) {
                return false;
            }

            if (state == MicrophoneUplinkState.ON) {
                operation = Operation.STOP;
                requestPermission = false;
            }
            else {
                if (!endpoint.isMicUplinkSupported()) {
                    operation = null;
                    requestPermission = false;
                }
                else if (!permissionGateway.isGranted()) {
                    awaitingPermission = true;
                    pendingStartAfterPermission = true;
                    operation = null;
                    requestPermission = true;
                }
                else {
                    operation = Operation.START;
                    requestPermission = false;
                }
            }
        }

        if (operation == null && !requestPermission) {
            feedback.onUnsupported();
            return false;
        }
        if (requestPermission) {
            try {
                permissionGateway.requestPermission();
                return true;
            }
            catch (RuntimeException error) {
                synchronized (lock) {
                    awaitingPermission = false;
                    pendingStartAfterPermission = false;
                }
                throw error;
            }
        }
        return schedule(operation);
    }

    @MainThread
    public void onPermissionResult(boolean granted) {
        final boolean shouldStart;
        synchronized (lock) {
            if (destroyed || !awaitingPermission) {
                return;
            }
            awaitingPermission = false;
            shouldStart = granted && pendingStartAfterPermission;
            pendingStartAfterPermission = false;
        }

        if (shouldStart) {
            schedule(Operation.START);
        }
        else if (!granted) {
            feedback.onPermissionDenied();
        }
    }

    @AnyThread
    public void destroy() {
        final Future<?> operationToCancel;
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            awaitingPermission = false;
            pendingStartAfterPermission = false;
            operationToCancel = pendingOperation;
            pendingOperation = null;
        }

        if (operationToCancel != null) {
            operationToCancel.cancel(true);
        }
        workerExecutor.shutdownNow();
    }

    @MainThread
    private boolean schedule(Operation operation) {
        final FutureTask<Void> task = new FutureTask<>(() -> {
            runOperation(operation);
            return null;
        });
        synchronized (lock) {
            if (destroyed || pendingOperation != null) {
                return false;
            }
            pendingOperation = task;
        }

        try {
            workerExecutor.execute(task);
            return true;
        }
        catch (RuntimeException error) {
            synchronized (lock) {
                if (pendingOperation == task) {
                    pendingOperation = null;
                }
            }
            LimeLog.severe(
                    "Unable to schedule microphone transition: " +
                            error);
            return false;
        }
    }

    @WorkerThread
    private void runOperation(Operation operation) {
        final boolean succeeded;
        final String message;
        try {
            if (operation == Operation.START) {
                succeeded = endpoint.startMicUplink();
            }
            else {
                succeeded = endpoint.stopMicUplink();
            }
            message = endpoint.getLastMicUplinkMessage();
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Microphone transition failed unexpectedly: " +
                            error);
            publishOrDiscard(false, error.getMessage());
            return;
        }

        publishOrDiscard(succeeded, message);
    }

    @WorkerThread
    private void publishOrDiscard(
            boolean succeeded,
            String message) {
        try {
            mainExecutor.execute(
                    () -> publishOperationResult(
                            succeeded,
                            message));
        }
        catch (RuntimeException error) {
            synchronized (lock) {
                pendingOperation = null;
            }
            LimeLog.warning(
                    "Unable to publish microphone transition: " +
                            error);
        }
    }

    @MainThread
    private void publishOperationResult(
            boolean succeeded,
            String message) {
        synchronized (lock) {
            if (destroyed || pendingOperation == null) {
                return;
            }
            pendingOperation = null;
        }

        if (!succeeded && message != null && !message.isEmpty()) {
            feedback.onOperationFailed(message);
        }
        feedback.onStateChanged();
    }
}
