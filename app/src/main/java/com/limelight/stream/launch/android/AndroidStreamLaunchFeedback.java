package com.limelight.stream.launch.android;

import android.app.Activity;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.utils.UiToast;

import java.util.Objects;

/** Shared UI mapping for terminal stream-launch failures. */
public final class AndroidStreamLaunchFeedback {
    private AndroidStreamLaunchFeedback() {
    }

    public static void showIfNeeded(
            Activity activity,
            AndroidStreamLauncher.Result result) {
        Objects.requireNonNull(activity, "activity");
        if (result == null ||
                result.getOutcome() ==
                        AndroidStreamLauncher.Outcome.STARTED ||
                result.getOutcome() ==
                        AndroidStreamLauncher.Outcome.ALREADY_STARTING) {
            return;
        }
        int message = result.getOutcome() ==
                AndroidStreamLauncher.Outcome.HOST_UNAVAILABLE
                ? R.string.pair_pc_offline
                : R.string.conn_error_msg;
        UiToast.makeText(
                activity,
                message,
                UiToast.LENGTH_LONG).show();
        if (result.getFailure() != null) {
            LimeLog.warning(
                    "Stream launch failed: " +
                            result.getFailure()
                                    .getClass()
                                    .getSimpleName());
        }
    }
}
