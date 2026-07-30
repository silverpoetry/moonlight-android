package com.limelight.ui.stream;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.nvstream.NvConnectionListener;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Routes accepted stream callbacks to their explicit execution domains.
 *
 * <p>Presentation callbacks are serialized on the Android main thread.
 * Latency-sensitive controller feedback remains on the connection callback
 * thread. Destruction invalidates presentation work that is already queued.</p>
 */
public final class StreamSessionCallbackRouter
        implements NvConnectionListener {
    interface UiDispatcher {
        boolean dispatch(Runnable task);

        void clear();
    }

    public interface UiHost {
        @MainThread
        void onStageStarting(String stage);

        @MainThread
        void onStageFailed(
                String stage,
                int portFlags,
                int errorCode);

        @MainThread
        void onConnectionStarted();

        @MainThread
        void onConnectionTerminated(int errorCode);

        @MainThread
        void onConnectionStatusUpdate(int connectionStatus);

        @MainThread
        void onMessage(String message, boolean transientMessage);

        @MainThread
        void onHdrModeChanged(boolean enabled, byte[] hdrMetadata);

        @MainThread
        void onNativeCursor(
                boolean visible,
                boolean shapeChanged,
                int format,
                int x,
                int y,
                int width,
                int height,
                int hotspotX,
                int hotspotY,
                int shapeId,
                int scaleX,
                int scaleY,
                byte[] imageData);
    }

    public interface FeedbackHost {
        @AnyThread
        void onRumble(
                short controllerNumber,
                short lowFreqMotor,
                short highFreqMotor);

        @AnyThread
        void onRumbleTriggers(
                short controllerNumber,
                short leftTrigger,
                short rightTrigger);

        @AnyThread
        void onMotionEventState(
                short controllerNumber,
                byte motionType,
                short reportRateHz);

        @AnyThread
        void onControllerLed(
                short controllerNumber,
                byte red,
                byte green,
                byte blue);
    }

    private final UiHost uiHost;
    private final FeedbackHost feedbackHost;
    private final UiDispatcher uiDispatcher;

    private final AtomicBoolean destroyed = new AtomicBoolean();

    @AnyThread
    public StreamSessionCallbackRouter(
            UiHost uiHost,
            FeedbackHost feedbackHost,
            Handler mainHandler) {
        this(
                uiHost,
                feedbackHost,
                createUiDispatcher(mainHandler));
    }

    StreamSessionCallbackRouter(
            UiHost uiHost,
            FeedbackHost feedbackHost,
            UiDispatcher uiDispatcher) {
        this.uiHost = Objects.requireNonNull(uiHost, "uiHost");
        this.feedbackHost = Objects.requireNonNull(
                feedbackHost,
                "feedbackHost");
        this.uiDispatcher = Objects.requireNonNull(
                uiDispatcher,
                "uiDispatcher");
    }

    @AnyThread
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        uiDispatcher.clear();
    }

    @Override
    public void stageStarting(String stage) {
        dispatchUi(() -> uiHost.onStageStarting(stage));
    }

    @Override
    public void stageComplete(String stage) {
        // No presentation or runtime side effect owns this callback.
    }

    @Override
    public void stageFailed(
            String stage,
            int portFlags,
            int errorCode) {
        dispatchUi(() -> uiHost.onStageFailed(
                stage,
                portFlags,
                errorCode));
    }

    @Override
    public void connectionStarted() {
        dispatchUi(uiHost::onConnectionStarted);
    }

    @Override
    public void connectionTerminated(int errorCode) {
        dispatchUi(() -> uiHost.onConnectionTerminated(errorCode));
    }

    @Override
    public void connectionStatusUpdate(int connectionStatus) {
        dispatchUi(() -> uiHost.onConnectionStatusUpdate(
                connectionStatus));
    }

    @Override
    public void displayMessage(String message) {
        dispatchUi(() -> uiHost.onMessage(message, false));
    }

    @Override
    public void displayTransientMessage(String message) {
        dispatchUi(() -> uiHost.onMessage(message, true));
    }

    @Override
    public void rumble(
            short controllerNumber,
            short lowFreqMotor,
            short highFreqMotor) {
        if (!destroyed.get()) {
            feedbackHost.onRumble(
                    controllerNumber,
                    lowFreqMotor,
                    highFreqMotor);
        }
    }

    @Override
    public void rumbleTriggers(
            short controllerNumber,
            short leftTrigger,
            short rightTrigger) {
        if (!destroyed.get()) {
            feedbackHost.onRumbleTriggers(
                    controllerNumber,
                    leftTrigger,
                    rightTrigger);
        }
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        if (destroyed.get()) {
            return;
        }
        byte[] metadataSnapshot = copy(hdrMetadata);
        dispatchUi(() -> uiHost.onHdrModeChanged(
                enabled,
                metadataSnapshot));
    }

    @Override
    public void setMotionEventState(
            short controllerNumber,
            byte motionType,
            short reportRateHz) {
        if (!destroyed.get()) {
            feedbackHost.onMotionEventState(
                    controllerNumber,
                    motionType,
                    reportRateHz);
        }
    }

    @Override
    public void setControllerLED(
            short controllerNumber,
            byte red,
            byte green,
            byte blue) {
        if (!destroyed.get()) {
            feedbackHost.onControllerLed(
                    controllerNumber,
                    red,
                    green,
                    blue);
        }
    }

    @Override
    public void nativeCursor(
            boolean visible,
            boolean shapeChanged,
            int format,
            int x,
            int y,
            int width,
            int height,
            int hotspotX,
            int hotspotY,
            int shapeId,
            int scaleX,
            int scaleY,
            byte[] imageData) {
        if (destroyed.get()) {
            return;
        }
        byte[] imageSnapshot = copy(imageData);
        dispatchUi(() -> uiHost.onNativeCursor(
                visible,
                shapeChanged,
                format,
                x,
                y,
                width,
                height,
                hotspotX,
                hotspotY,
                shapeId,
                scaleX,
                scaleY,
                imageSnapshot));
    }

    private void dispatchUi(Runnable task) {
        if (destroyed.get()) {
            return;
        }
        boolean accepted = uiDispatcher.dispatch(() -> {
            if (!destroyed.get()) {
                task.run();
            }
        });
        if (!accepted && !destroyed.get()) {
            LimeLog.warning(
                    "Main thread rejected a stream session callback");
        }
    }

    private static byte[] copy(byte[] source) {
        return source == null
                ? null
                : Arrays.copyOf(source, source.length);
    }

    private static UiDispatcher createUiDispatcher(
            Handler mainHandler) {
        Objects.requireNonNull(mainHandler, "mainHandler");
        Object callbackToken = new Object();
        return new UiDispatcher() {
            @Override
            public boolean dispatch(Runnable task) {
                if (Looper.myLooper() == mainHandler.getLooper()) {
                    task.run();
                    return true;
                }
                return mainHandler.postAtTime(
                        task,
                        callbackToken,
                        SystemClock.uptimeMillis());
            }

            @Override
            public void clear() {
                mainHandler.removeCallbacksAndMessages(callbackToken);
            }
        };
    }
}
