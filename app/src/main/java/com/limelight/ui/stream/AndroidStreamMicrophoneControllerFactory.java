package com.limelight.ui.stream;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.MainThread;

import com.limelight.R;
import com.limelight.nvstream.mic.MicrophoneUplinkEndpoint;
import com.limelight.utils.UiToast;

import java.util.Objects;
import java.util.concurrent.Executor;

/** Composes stream microphone policy with Android permission and UI adapters. */
public final class AndroidStreamMicrophoneControllerFactory {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;

    private AndroidStreamMicrophoneControllerFactory() {
    }

    @MainThread
    public static StreamMicrophoneController create(
            Activity activity,
            MicrophoneUplinkEndpoint endpoint,
            Runnable stateChanged,
            Executor mainExecutor) {
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(stateChanged, "stateChanged");
        Objects.requireNonNull(mainExecutor, "mainExecutor");

        return StreamMicrophoneController.create(
                endpoint,
                new StreamMicrophoneController.PermissionGateway() {
                    @Override
                    public boolean isGranted() {
                        return activity.checkSelfPermission(
                                        Manifest.permission.RECORD_AUDIO) ==
                                        PackageManager.PERMISSION_GRANTED;
                    }

                    @Override
                    public void requestPermission() {
                        activity.requestPermissions(
                                new String[] {
                                    Manifest.permission.RECORD_AUDIO
                                },
                                REQUEST_RECORD_AUDIO_PERMISSION);
                    }
                },
                new StreamMicrophoneController.Feedback() {
                    @Override
                    public void onUnsupported() {
                        showMessage(
                                activity,
                                activity.getString(R.string
                                        .mic_uplink_not_supported),
                                UiToast.LENGTH_LONG);
                    }

                    @Override
                    public void onPermissionDenied() {
                        showMessage(
                                activity,
                                activity.getString(R.string
                                        .mic_uplink_permission_denied),
                                UiToast.LENGTH_LONG);
                    }

                    @Override
                    public void onOperationFailed(String message) {
                        showMessage(
                                activity,
                                message,
                                UiToast.LENGTH_SHORT);
                    }

                    @Override
                    public void onStateChanged() {
                        stateChanged.run();
                    }
                },
                mainExecutor);
    }

    @MainThread
    public static boolean handlePermissionResult(
            StreamMicrophoneController controller,
            int requestCode,
            int[] grantResults) {
        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return false;
        }
        if (controller != null) {
            controller.onPermissionResult(
                    grantResults != null &&
                            grantResults.length > 0 &&
                            grantResults[0] ==
                                    PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    private static void showMessage(
            Activity activity,
            String message,
            int duration) {
        if (message == null || message.isEmpty() ||
                activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        UiToast.makeText(activity, message, duration).show();
    }
}
