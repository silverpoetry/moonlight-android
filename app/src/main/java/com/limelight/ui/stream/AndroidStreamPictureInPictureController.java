package com.limelight.ui.stream;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.graphics.Rect;
import android.os.Build;
import android.util.Rational;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;

import com.limelight.LimeLog;
import com.limelight.ui.StreamView;
import com.limelight.utils.ViewWindowGeometry;

import java.util.Objects;

/** Owns picture-in-picture parameters, auto-entry state, and API dispatch. */
public final class AndroidStreamPictureInPictureController {
    private final Activity activity;
    private final StreamView streamView;
    private final int streamWidth;
    private final int streamHeight;
    private final String appName;
    private final String pcName;
    private final int[] windowLocationScratch = new int[2];
    private final StreamPictureInPictureState state;

    private boolean legacyAutoEnter;
    private boolean destroyed;

    public AndroidStreamPictureInPictureController(
            Activity activity,
            StreamView streamView,
            int streamWidth,
            int streamHeight,
            boolean enabled,
            String appName,
            String pcName) {
        if (streamWidth <= 0 || streamHeight <= 0) {
            throw new IllegalArgumentException(
                    "Stream dimensions must be positive");
        }
        this.activity = Objects.requireNonNull(activity, "activity");
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        state = new StreamPictureInPictureState(enabled);
        this.appName = appName;
        this.pcName = pcName;
    }

    @MainThread
    public void setSessionConnected(boolean connected) {
        if (destroyed) {
            return;
        }
        state.setConnected(connected);
        applyAutoEnter();
    }

    @MainThread
    public void setEnabled(boolean enabled) {
        if (destroyed) {
            return;
        }
        state.setEnabled(enabled);
        applyAutoEnter();
    }

    @MainThread
    public void acquireAutoEnterSuppression() {
        if (destroyed) {
            return;
        }
        state.acquireSuppression();
        applyAutoEnter();
    }

    @MainThread
    public void releaseAutoEnterSuppression() {
        if (destroyed) {
            return;
        }
        state.releaseSuppression();
        applyAutoEnter();
    }

    @MainThread
    public void onUserLeaveHint() {
        if (destroyed ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                !legacyAutoEnter) {
            return;
        }
        tryEnterPictureInPicture();
    }

    @MainThread
    @RequiresApi(Build.VERSION_CODES.R)
    public boolean onPictureInPictureRequested() {
        if (!destroyed &&
                legacyAutoEnter &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            tryEnterPictureInPicture();
        }
        return true;
    }

    @MainThread
    public void destroy() {
        destroyed = true;
        legacyAutoEnter = false;
        state.setConnected(false);
    }

    @MainThread
    private void applyAutoEnter() {
        boolean autoEnter = state.shouldAutoEnter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.setPictureInPictureParams(
                    buildParams(autoEnter));
        }
        else {
            legacyAutoEnter = autoEnter;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void tryEnterPictureInPicture() {
        try {
            activity.enterPictureInPictureMode(
                    buildParams(false));
        }
        catch (RuntimeException exception) {
            // Several Android 8 Samsung builds throw here during window
            // transitions. PiP is optional, so keep the stream alive.
            LimeLog.warning(
                    "Unable to enter picture-in-picture: " +
                            exception.getClass().getSimpleName());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private PictureInPictureParams buildParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(
                                streamWidth,
                                streamHeight));
        Rect sourceBounds = new Rect();
        if (ViewWindowGeometry.getVisibleBoundsInWindow(
                streamView,
                activity.getWindow().getDecorView(),
                sourceBounds,
                windowLocationScratch)) {
            builder.setSourceRectHint(sourceBounds);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter);
            builder.setSeamlessResizeEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName);
                if (pcName != null) {
                    builder.setSubtitle(pcName);
                }
            }
            else if (pcName != null) {
                builder.setTitle(pcName);
            }
        }
        return builder.build();
    }
}
