package com.limelight.stream.launch.android;

import android.app.Activity;

import com.limelight.R;
import com.limelight.utils.SpinnerDialog;

import java.util.Objects;

/**
 * Owns the one connection indicator for an Android stream launch.
 *
 * <p>The launching Activity owns the dialog for the complete preparation.
 * The stream Activity receives only its token and closes the matching dialog
 * after the visible decoder Surface has been attached.</p>
 */
public final class AndroidStreamLaunchProgress {
    private final Object lock = new Object();
    private Session activeSession;

    public AndroidStreamLaunchProgress() {
    }

    public Session begin(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        Session session = new Session(activity);
        Session previous;
        synchronized (lock) {
            previous = activeSession;
            activeSession = session;
        }
        if (previous != null) {
            previous.dismiss();
        }
        return session;
    }

    public Session takeActive(String sessionToken) {
        synchronized (lock) {
            if (activeSession == null ||
                    !activeSession.matches(sessionToken)) {
                return null;
            }
            return activeSession;
        }
    }

    public void finish(Session session) {
        if (session == null) {
            return;
        }
        boolean shouldDismiss;
        synchronized (lock) {
            shouldDismiss = activeSession == session;
            if (shouldDismiss) {
                activeSession = null;
            }
        }
        if (shouldDismiss) {
            session.dismiss();
        }
    }

    public void finishFor(Activity activity) {
        Session session;
        synchronized (lock) {
            session = activeSession;
            if (session == null || session.activity != activity) {
                return;
            }
            activeSession = null;
        }
        session.dismiss();
    }

    public static final class Session {
        private final Activity activity;
        private final SpinnerDialog indicator;
        private volatile String sessionToken;

        private Session(Activity activity) {
            this.activity = activity;
            indicator = SpinnerDialog.displayDialog(
                    activity,
                    activity.getString(R.string.conn_establishing_title),
                    activity.getString(R.string.conn_establishing_msg),
                    true);
        }

        public void updateMessage(String message) {
            indicator.setMessage(message);
        }

        public void setFinishOnCancelEnabled(boolean enabled) {
            indicator.setFinishOnCancelEnabled(enabled);
        }

        public void bindSessionToken(String sessionToken) {
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "sessionToken is required");
            }
            this.sessionToken = sessionToken;
        }

        private boolean matches(String candidate) {
            return sessionToken != null &&
                    sessionToken.equals(candidate);
        }

        private void dismiss() {
            indicator.dismiss();
        }
    }
}
