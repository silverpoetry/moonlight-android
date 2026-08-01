package com.limelight.ui.hosts;

import android.content.Context;

import com.limelight.R;
import com.limelight.computers.session.HostQuitUseCase;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.Objects;

/** Localized presentation mapping for host-session termination results. */
public final class HostQuitMessageResolver {
    private HostQuitMessageResolver() {
    }

    public static CharSequence resolve(
            Context context,
            String appName,
            HostQuitUseCase.Outcome outcome) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(outcome, "outcome");
        switch (outcome) {
            case QUIT:
                return appendAppName(
                        context.getText(R.string.applist_quit_success),
                        appName);
            case REJECTED:
                return appendAppName(
                        context.getText(R.string.applist_quit_fail),
                        appName);
            case NOT_SESSION_OWNER:
                return context.getText(
                        R.string.applist_quit_not_session_owner);
            default:
                throw new AssertionError(
                        "Unhandled host quit outcome: " + outcome);
        }
    }

    public static CharSequence resolveFailure(
            Context context,
            String appName,
            Exception failure) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(failure, "failure");
        if (failure instanceof UnknownHostException) {
            return context.getText(R.string.error_unknown_host);
        }
        if (failure instanceof FileNotFoundException) {
            return context.getText(R.string.error_404);
        }
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? appendAppName(
                        context.getText(R.string.applist_quit_fail),
                        appName)
                : message;
    }

    private static CharSequence appendAppName(
            CharSequence message,
            String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            return message;
        }
        return message + " " + appName;
    }
}
