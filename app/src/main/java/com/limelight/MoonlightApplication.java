package com.limelight;

import android.app.Application;

import com.limelight.integration.xiaomi.XiaomiRefreshRateOverrideController;
import com.limelight.migration.LegacyDataImporter;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.platform.PlatformIntegrationSettingKeys;
import com.limelight.stream.launch.PendingClipboardFilePullStore;
import com.limelight.stream.launch.PendingStreamReconnectStore;
import com.limelight.stream.launch.android.AndroidStreamLaunchProgress;
import com.limelight.stream.launch.android.AndroidStreamSessionCoordinator;

/** Narrow process composition root for cross-Activity stream handoff state. */
public final class MoonlightApplication extends Application {
    private final PendingClipboardFilePullStore
            pendingClipboardFilePullStore =
            new PendingClipboardFilePullStore();
    private final PendingStreamReconnectStore pendingStreamReconnectStore =
            new PendingStreamReconnectStore();
    private final AndroidStreamLaunchProgress streamLaunchProgress =
            new AndroidStreamLaunchProgress();
    private final AndroidStreamSessionCoordinator
            streamSessionCoordinator =
            new AndroidStreamSessionCoordinator();

    @Override
    public void onCreate() {
        super.onCreate();
        LegacyDataImporter.importIfAvailable(this);
        if (XiaomiRefreshRateOverrideController.isSupportedDevice() &&
                AndroidSettingsRepository.create(this).get(
                        PlatformIntegrationSettingKeys
                                .XIAOMI_REFRESH_RATE_LIMIT_SUPPRESSION)) {
            XiaomiRefreshRateOverrideController.apply(
                    this,
                    true,
                    ignored -> { });
        }
    }

    public PendingStreamReconnectStore getPendingStreamReconnectStore() {
        return pendingStreamReconnectStore;
    }

    public PendingClipboardFilePullStore
            getPendingClipboardFilePullStore() {
        return pendingClipboardFilePullStore;
    }

    public AndroidStreamLaunchProgress getStreamLaunchProgress() {
        return streamLaunchProgress;
    }

    public AndroidStreamSessionCoordinator getStreamSessionCoordinator() {
        return streamSessionCoordinator;
    }
}
