package com.limelight.ui.decoder;

import android.app.Activity;

import com.limelight.R;
import com.limelight.binding.video.DecoderCrashNotificationPolicy;
import com.limelight.binding.video.DecoderCrashState;
import com.limelight.binding.video.DecoderCrashStore;
import com.limelight.settings.android.AndroidStreamDefaults;
import com.limelight.utils.Dialog;

import java.util.Objects;

/** Presents decoder-crash recovery once for each unacknowledged state. */
public final class AndroidDecoderCrashNotificationController {
    private final Activity activity;
    private final DecoderCrashStore crashStore;

    public AndroidDecoderCrashNotificationController(
            Activity activity,
            DecoderCrashStore crashStore) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.crashStore = Objects.requireNonNull(
                crashStore, "crashStore");
    }

    public void showIfNeeded() {
        DecoderCrashState state = crashStore.readState();
        DecoderCrashNotificationPolicy.Action action =
                DecoderCrashNotificationPolicy.evaluate(state);
        if (action == DecoderCrashNotificationPolicy.Action.NONE) {
            return;
        }

        int titleResource;
        int messageResource;
        if (action ==
                DecoderCrashNotificationPolicy.Action.RESET_SETTINGS) {
            AndroidStreamDefaults.resetAfterDecoderCrashes(activity);
            titleResource = R.string.title_decoding_reset;
            messageResource = R.string.message_decoding_reset;
        }
        else {
            titleResource = R.string.title_decoding_error;
            messageResource = R.string.message_decoding_error;
        }

        int crashCount = state.getCrashCount();
        Dialog.displayDialog(
                activity,
                activity.getString(titleResource),
                activity.getString(messageResource),
                () -> crashStore.acknowledgeCrashCount(crashCount));
    }
}
