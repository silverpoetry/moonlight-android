package com.limelight.ui.stream;

import android.app.Activity;

import com.limelight.MoonlightApplication;
import com.limelight.stream.launch.android.AndroidStreamLaunchContract;
import com.limelight.stream.launch.android.AndroidStreamLaunchProgress;

import java.util.Objects;

/** Closes the launcher's token-bound progress surface after handoff. */
public final class AndroidStreamConnectingIndicator {
    private final Activity activity;
    private AndroidStreamLaunchProgress.Session launchSession;
    private boolean destroyed;

    public AndroidStreamConnectingIndicator(Activity activity) {
        this.activity = Objects.requireNonNull(activity, "activity");
        AndroidStreamLaunchProgress progress =
                ((MoonlightApplication) activity.getApplication())
                        .getStreamLaunchProgress();
        launchSession = progress.takeActive(
                activity.getIntent().getStringExtra(
                        AndroidStreamLaunchContract
                                .EXTRA_SESSION_TOKEN));
    }

    public void updateMessage(String message) {
        if (destroyed) {
            return;
        }
        if (launchSession != null) {
            launchSession.updateMessage(message);
        }
    }

    public void setFinishOnCancelEnabled(boolean enabled) {
        if (destroyed) {
            return;
        }
        if (launchSession != null) {
            launchSession.setFinishOnCancelEnabled(enabled);
        }
    }

    public boolean isActive() {
        return !destroyed &&
                launchSession != null;
    }

    public void dismiss() {
        if (destroyed) {
            return;
        }
        if (launchSession != null) {
            ((MoonlightApplication) activity.getApplication())
                    .getStreamLaunchProgress()
                    .finish(launchSession);
            launchSession = null;
        }
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        dismiss();
        destroyed = true;
    }
}
