package com.limelight.stream.launch.android;

import android.content.Context;
import android.content.Intent;

import com.limelight.AppView;
import com.limelight.ShortcutTrampoline;
import com.limelight.computers.model.HostIdentity;
import com.limelight.nvstream.http.NvApp;

import java.util.Objects;

/** Owns launcher and TV intents for host/app shortcuts. */
public final class AndroidShortcutIntentFactory {
    private AndroidShortcutIntentFactory() {
    }

    public static Intent createHostIntent(
            Context context,
            HostIdentity host) {
        Objects.requireNonNull(context, "context");
        HostIdentity target = Objects.requireNonNull(host, "host");
        Intent intent = new Intent(context, ShortcutTrampoline.class);
        intent.putExtra(
                AppView.NAME_EXTRA,
                target.getAdvertisedName());
        intent.putExtra(
                AppView.UUID_EXTRA,
                target.getId().getValue());
        intent.setAction(Intent.ACTION_DEFAULT);
        return intent;
    }

    public static Intent createAppIntent(
            Context context,
            HostIdentity host,
            NvApp app) {
        Intent intent = createHostIntent(context, host);
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
