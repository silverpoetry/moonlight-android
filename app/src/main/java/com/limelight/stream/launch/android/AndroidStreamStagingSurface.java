package com.limelight.stream.launch.android;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.MainThread;

import java.util.Objects;

/**
 * Off-screen decoder target used only while a stream is being prepared.
 *
 * <p>Images are drained continuously so MediaCodec cannot stall on a full
 * buffer queue before the visible stream Surface is attached.</p>
 */
public final class AndroidStreamStagingSurface implements AutoCloseable {
    private static final int MAX_IMAGES = 3;

    private final HandlerThread drainThread;
    private final ImageReader imageReader;
    private boolean closed;

    @MainThread
    public AndroidStreamStagingSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Staging dimensions must be positive");
        }
        HandlerThread newDrainThread =
                new HandlerThread("StreamStagingSurface");
        newDrainThread.start();
        ImageReader newImageReader;
        try {
            newImageReader = ImageReader.newInstance(
                    width,
                    height,
                    ImageFormat.PRIVATE,
                    MAX_IMAGES);
        }
        catch (RuntimeException | Error failure) {
            newDrainThread.quitSafely();
            throw failure;
        }
        drainThread = newDrainThread;
        imageReader = newImageReader;
        imageReader.setOnImageAvailableListener(
                AndroidStreamStagingSurface::drain,
                new Handler(drainThread.getLooper()));
    }

    @MainThread
    public Surface getSurface() {
        if (closed) {
            throw new IllegalStateException("Staging Surface is closed");
        }
        return imageReader.getSurface();
    }

    @Override
    @MainThread
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        imageReader.setOnImageAvailableListener(null, null);
        imageReader.close();
        drainThread.quitSafely();
    }

    private static void drain(ImageReader reader) {
        Objects.requireNonNull(reader, "reader");
        try {
            Image image;
            while ((image = reader.acquireNextImage()) != null) {
                image.close();
            }
        }
        catch (IllegalStateException ignored) {
            // A queued callback may race with close(); the reader has no
            // remaining resources to drain in that state.
        }
    }
}
