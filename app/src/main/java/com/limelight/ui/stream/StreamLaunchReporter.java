package com.limelight.ui.stream;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import com.limelight.LimeLog;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

/**
 * Owns the bounded background work used to publish launch side effects for one
 * stream session.
 */
public final class StreamLaunchReporter {
    public interface ReportAction {
        @WorkerThread
        void run();
    }

    private static final ThreadFactory THREAD_FACTORY = command -> {
        Thread thread = new Thread(command, "StreamLaunchReporter");
        thread.setDaemon(true);
        return thread;
    };

    private final Object lock = new Object();
    private final ExecutorService workerExecutor;
    private final ReportAction reportAction;

    private FutureTask<Void> pendingTask;
    private boolean reportRequested;
    private boolean destroyed;

    @AnyThread
    public static StreamLaunchReporter create(
            ReportAction reportAction) {
        return new StreamLaunchReporter(
                Executors.newSingleThreadExecutor(THREAD_FACTORY),
                reportAction);
    }

    StreamLaunchReporter(
            ExecutorService workerExecutor,
            ReportAction reportAction) {
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "workerExecutor");
        this.reportAction = Objects.requireNonNull(
                reportAction,
                "reportAction");
    }

    /**
     * Schedules the report at most once for this session.
     *
     * @return {@code true} only when a new report was accepted
     */
    @AnyThread
    public boolean reportOnce() {
        final FutureTask<Void> newTask;
        synchronized (lock) {
            if (destroyed || reportRequested) {
                return false;
            }
            reportRequested = true;
            newTask = new FutureTask<>(() -> {
                runReport();
                return null;
            });
            pendingTask = newTask;
        }

        try {
            workerExecutor.execute(newTask);
            return true;
        } catch (RuntimeException error) {
            synchronized (lock) {
                if (pendingTask == newTask) {
                    pendingTask = null;
                    reportRequested = false;
                }
            }
            LimeLog.severe(
                    "Unable to schedule stream launch report: " + error);
            return false;
        }
    }

    @AnyThread
    public void destroy() {
        final FutureTask<Void> taskToCancel;
        synchronized (lock) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            taskToCancel = pendingTask;
            pendingTask = null;
        }

        if (taskToCancel != null) {
            taskToCancel.cancel(true);
        }
        workerExecutor.shutdownNow();
    }

    @WorkerThread
    private void runReport() {
        try {
            reportAction.run();
        } catch (RuntimeException | Error error) {
            LimeLog.warning(
                    "Stream launch report failed: " + error);
        } finally {
            synchronized (lock) {
                pendingTask = null;
            }
        }
    }
}
