package com.limelight.binding.video.gl.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.binding.video.gl.GlDeviceSnapshot;
import com.limelight.binding.video.gl.GlDeviceSnapshotStore;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Lifecycle-bound, one-shot GL renderer probe used during host-screen startup.
 */
public final class AndroidGlRendererProbe {
    public interface Listener {
        void onProbeComplete(GlDeviceSnapshot snapshot);
    }

    private static final long PROBE_TIMEOUT_MILLIS = 5_000;

    private final GlDeviceSnapshotStore store;
    private final String buildFingerprint;
    private final GLSurfaceView surfaceView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean resolved = new AtomicBoolean();
    private final Runnable timeoutAction = () ->
            resolve(GlDeviceSnapshot.unavailable());
    private final Runnable deliveryAction = this::deliverResult;

    private volatile boolean destroyed;
    private volatile Listener listener;
    private volatile GlDeviceSnapshot result =
            GlDeviceSnapshot.unavailable();

    @MainThread
    public AndroidGlRendererProbe(
            Context context,
            GlDeviceSnapshotStore store,
            String buildFingerprint,
            Listener listener) {
        Objects.requireNonNull(context, "context");
        this.store = Objects.requireNonNull(store, "store");
        this.buildFingerprint = Objects.requireNonNull(
                buildFingerprint, "buildFingerprint");
        this.listener = Objects.requireNonNull(listener, "listener");

        surfaceView = new GLSurfaceView(context);
        surfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(
                    GL10 gl,
                    EGLConfig configuration) {
                String renderer = gl == null
                        ? null
                        : gl.glGetString(GL10.GL_RENDERER);
                resolve(GlDeviceSnapshot.fromUntrusted(
                        AndroidGlRendererProbe.this.buildFingerprint,
                        renderer));
            }

            @Override
            public void onSurfaceChanged(
                    GL10 gl,
                    int width,
                    int height) {
            }

            @Override
            public void onDrawFrame(GL10 gl) {
            }
        });
        surfaceView.setRenderMode(
                GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mainHandler.postDelayed(
                timeoutAction,
                PROBE_TIMEOUT_MILLIS);
    }

    public View getView() {
        return surfaceView;
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        listener = null;
        mainHandler.removeCallbacks(timeoutAction);
        mainHandler.removeCallbacks(deliveryAction);
        surfaceView.onPause();
    }

    private void resolve(GlDeviceSnapshot snapshot) {
        if (destroyed || !resolved.compareAndSet(false, true)) {
            return;
        }
        result = Objects.requireNonNull(snapshot, "snapshot");
        try {
            store.replace(snapshot);
        }
        catch (RuntimeException error) {
            LimeLog.warning(
                    "Unable to persist GL renderer cache: " +
                            error.getClass().getSimpleName());
        }
        mainHandler.removeCallbacks(timeoutAction);
        mainHandler.post(deliveryAction);
    }

    @MainThread
    private void deliverResult() {
        if (destroyed) {
            return;
        }
        Listener currentListener = listener;
        listener = null;
        if (currentListener != null) {
            currentListener.onProbeComplete(result);
        }
    }
}
