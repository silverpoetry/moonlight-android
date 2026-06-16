package com.limelight.utils;

import android.app.Activity;
import android.content.Intent;

import com.limelight.Game;
import com.limelight.computers.ComputerManagerService;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;

public final class AutoReconnectHelper {
    private static PendingStream pendingStream;

    private AutoReconnectHelper() {
    }

    public static synchronized void savePendingStream(Intent intent) {
        if (intent == null) {
            return;
        }

        String pcUuid = intent.getStringExtra(Game.EXTRA_PC_UUID);
        int appId = intent.getIntExtra(Game.EXTRA_APP_ID, StreamConfiguration.INVALID_APP_ID);
        if (pcUuid == null || appId == StreamConfiguration.INVALID_APP_ID) {
            return;
        }

        pendingStream = new PendingStream(
                pcUuid,
                intent.getStringExtra(Game.EXTRA_APP_NAME),
                appId,
                intent.getBooleanExtra(Game.EXTRA_APP_HDR, false));
    }

    public static synchronized boolean maybeResumeStream(Activity activity,
                                                         ComputerManagerService.ComputerManagerBinder managerBinder,
                                                         String currentPcUuid) {
        if (activity == null || managerBinder == null || pendingStream == null) {
            return false;
        }

        if (currentPcUuid != null && !currentPcUuid.equalsIgnoreCase(pendingStream.pcUuid)) {
            return false;
        }

        ComputerDetails computer = managerBinder.getComputer(pendingStream.pcUuid);
        if (computer == null ||
                computer.state != ComputerDetails.State.ONLINE ||
                computer.activeAddress == null) {
            return false;
        }

        int appId = pendingStream.appId;
        if (appId == StreamConfiguration.INVALID_APP_ID) {
            appId = computer.runningGameId;
        }
        if (appId == 0 || appId == StreamConfiguration.INVALID_APP_ID) {
            return false;
        }

        NvApp app = new NvApp(
                pendingStream.appName != null ? pendingStream.appName : "app",
                appId,
                pendingStream.appSupportsHdr);

        pendingStream = null;
        activity.startActivity(ServerHelper.createStartIntent(activity, app, computer, managerBinder));
        return true;
    }

    private static final class PendingStream {
        private final String pcUuid;
        private final String appName;
        private final int appId;
        private final boolean appSupportsHdr;

        private PendingStream(String pcUuid, String appName, int appId, boolean appSupportsHdr) {
            this.pcUuid = pcUuid;
            this.appName = appName;
            this.appId = appId;
            this.appSupportsHdr = appSupportsHdr;
        }
    }
}
