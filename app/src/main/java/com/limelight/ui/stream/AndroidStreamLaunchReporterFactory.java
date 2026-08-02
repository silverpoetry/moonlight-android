package com.limelight.ui.stream;

import android.content.Context;

import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.nvstream.http.NvApp;
import com.limelight.utils.ShortcutHelper;

import java.util.Objects;

/** Creates a session-scoped reporter from an immutable launch snapshot. */
public final class AndroidStreamLaunchReporterFactory {
    private AndroidStreamLaunchReporterFactory() {
    }

    public static StreamLaunchReporter create(
            Context context,
            String computerName,
            String computerUuid,
            NvApp launchedApp,
            boolean reportGameLaunch) {
        Context applicationContext = Objects.requireNonNull(
                        context,
                        "context")
                .getApplicationContext();
        HostIdentity hostSnapshot = new HostIdentity(
                HostId.of(computerUuid),
                computerName,
                null);
        NvApp appSnapshot = snapshotApp(launchedApp);
        ShortcutHelper shortcutHelper =
                new ShortcutHelper(applicationContext);

        return StreamLaunchReporter.create(() -> {
            shortcutHelper.reportComputerShortcutUsed(
                    hostSnapshot);
            if (reportGameLaunch) {
                shortcutHelper.reportGameLaunched(
                        hostSnapshot,
                        appSnapshot);
            }
        });
    }

    private static NvApp snapshotApp(NvApp launchedApp) {
        NvApp app = Objects.requireNonNull(
                launchedApp,
                "launchedApp");
        NvApp snapshot = new NvApp(
                app.getAppName(),
                app.getAppId(),
                app.isHdrSupported());
        snapshot.setCustomImagePath(app.getCustomImagePath());
        return snapshot;
    }
}
