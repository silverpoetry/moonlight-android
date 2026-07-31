package com.limelight.ui.stream;

import android.app.Activity;
import android.widget.TextView;

import androidx.annotation.MainThread;

import com.limelight.R;

import java.util.Objects;

/** Renders connection-quality warnings through the shared overlay owner. */
public final class AndroidStreamConnectionWarningPresenter {
    private final Activity activity;
    private final TextView warningView;
    private final StreamOverlayVisibilityController visibilityController;

    public AndroidStreamConnectionWarningPresenter(
            Activity activity,
            TextView warningView,
            StreamOverlayVisibilityController visibilityController) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.warningView = Objects.requireNonNull(
                warningView,
                "warningView");
        this.visibilityController = Objects.requireNonNull(
                visibilityController,
                "visibilityController");
    }

    @MainThread
    public void setWarning(
            StreamSessionPresentationController.ConnectionWarning warning) {
        StreamSessionPresentationController.ConnectionWarning value =
                Objects.requireNonNull(warning, "warning");
        if (value ==
                StreamSessionPresentationController.ConnectionWarning.NONE) {
            visibilityController.setConnectionWarningVisible(false);
            return;
        }
        warningView.setText(activity.getString(
                value ==
                        StreamSessionPresentationController
                                .ConnectionWarning.SLOW
                        ? R.string.slow_connection_msg
                        : R.string.poor_connection_msg));
        visibilityController.setConnectionWarningVisible(true);
    }
}
