package com.limelight.ui.stream;

import android.app.Activity;

import com.limelight.R;
import com.limelight.utils.SpinnerDialog;

import java.util.Objects;

/** Owns the nullable connecting dialog for one stream Activity. */
public final class AndroidStreamConnectingIndicator {
    private final Activity activity;
    private SpinnerDialog dialog;
    private boolean destroyed;

    public AndroidStreamConnectingIndicator(Activity activity) {
        this.activity = Objects.requireNonNull(activity, "activity");
        dialog = SpinnerDialog.displayDialog(
                activity,
                activity.getString(R.string.conn_establishing_title),
                activity.getString(R.string.conn_establishing_msg),
                true);
    }

    public void updateMessage(String message) {
        if (!destroyed && dialog != null) {
            dialog.setMessage(message);
        }
    }

    public void setFinishOnCancelEnabled(boolean enabled) {
        if (!destroyed && dialog != null) {
            dialog.setFinishOnCancelEnabled(enabled);
        }
    }

    public void dismiss() {
        if (destroyed || dialog == null) {
            return;
        }
        dialog.dismiss();
        dialog = null;
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        dialog = null;
        SpinnerDialog.closeDialogs(activity);
    }
}
