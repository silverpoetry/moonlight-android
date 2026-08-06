package com.limelight.stream.launch.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;

import com.limelight.R;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.ui.stream.AndroidStreamConnectionMessages;
import com.limelight.ui.stream.AndroidStreamFailureDiagnosticsFactory;
import com.limelight.ui.stream.StreamConnectionMessages;
import com.limelight.ui.stream.StreamSessionPresentationController;
import com.limelight.utils.Dialog;

import java.util.Objects;

/** Presents preparation failures on the Activity that owns launch progress. */
public final class AndroidStreamPreparationFailurePresenter {
    private AndroidStreamPreparationFailurePresenter() {
    }

    @MainThread
    public static void present(
            Activity activity,
            AndroidPreparedStreamSession.Failure failure) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(failure, "failure");
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if ("handoff".equals(failure.getStage())) {
            Dialog.displayDialog(
                    activity,
                    activity.getString(R.string.conn_error_title),
                    activity.getString(R.string.conn_error_msg),
                    false);
            return;
        }

        StreamConnectionMessages messages =
                AndroidStreamConnectionMessages.create(activity);
        StreamSessionPresentationController.Diagnostics diagnostics =
                AndroidStreamFailureDiagnosticsFactory.create(
                        new Handler(Looper.getMainLooper()));
        StreamSessionPresentationController.Diagnostics.Callback callback =
                (portFlags, probeResult) -> {
                    String message = messages.stageFailure(
                            failure.getStage(),
                            failure.getErrorCode(),
                            portFlags,
                            probeResult);
                    String detail = failure.getDetail();
                    if (detail != null && !detail.trim().isEmpty()) {
                        message = detail + "\n\n" + message;
                    }
                    if (!activity.isFinishing() &&
                            !activity.isDestroyed()) {
                        Dialog.displayDialog(
                                activity,
                                messages.connectionErrorTitle(),
                                message,
                                false);
                    }
                    diagnostics.destroy();
                };
        if (failure.getPortFlags() == 0 ||
                !diagnostics.request(
                        failure.getPortFlags(),
                        callback)) {
            callback.onResult(
                    failure.getPortFlags(),
                    MoonBridge.ML_TEST_RESULT_INCONCLUSIVE);
        }
    }
}
