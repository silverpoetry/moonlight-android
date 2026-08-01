package com.limelight.stream.launch.android;

import android.content.Context;
import android.content.Intent;

import com.limelight.AppView;
import com.limelight.ShortcutTrampoline;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;

import java.util.Objects;

/** Owns launcher and TV intents for host/app shortcuts. */
public final class AndroidShortcutIntentFactory {
    private AndroidShortcutIntentFactory() {
    }

    public static Intent createHostIntent(
            Context context,
            ComputerDetails computer) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(computer, "computer");
        Intent intent = new Intent(context, ShortcutTrampoline.class);
        intent.putExtra(AppView.NAME_EXTRA, computer.name);
        intent.putExtra(AppView.UUID_EXTRA, computer.uuid);
        intent.setAction(Intent.ACTION_DEFAULT);
        return intent;
    }

    public static Intent createAppIntent(
            Context context,
            ComputerDetails computer,
            NvApp app) {
        Intent intent = createHostIntent(context, computer);
        Objects.requireNonNull(app, "app");
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_NAME,
                app.getAppName());
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_ID,
                Integer.toString(app.getAppId()));
        intent.putExtra(
                AndroidStreamLaunchContract.EXTRA_APP_HDR,
                app.isHdrSupported());
        return intent;
    }
}
