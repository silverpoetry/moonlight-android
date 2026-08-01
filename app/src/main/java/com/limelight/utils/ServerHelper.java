package com.limelight.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.limelight.AppView;
import com.limelight.Game;
import com.limelight.R;
import com.limelight.ShortcutTrampoline;
import com.limelight.computers.ComputerManagerService;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;

public class ServerHelper {
    private static final String RECENT_SESSION_PREF_FILENAME = "RecentSessions";
    private static final String RECENT_SESSION_APP_ID_SUFFIX = ".appId";
    private static final String RECENT_SESSION_APP_NAME_SUFFIX = ".appName";
    private static final String RECENT_SESSION_APP_HDR_SUFFIX = ".appHdr";

    public static ComputerDetails.AddressTuple getCurrentAddressFromComputer(ComputerDetails computer) throws IOException {
        if (computer.activeAddress == null) {
            throw new IOException("No active address for "+computer.name);
        }
        return computer.activeAddress;
    }

    public static Intent createPcShortcutIntent(Context parent, ComputerDetails computer) {
        Intent i = new Intent(parent, ShortcutTrampoline.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);
        i.putExtra(AppView.UUID_EXTRA, computer.uuid);
        i.setAction(Intent.ACTION_DEFAULT);
        return i;
    }

    public static Intent createAppShortcutIntent(Context parent, ComputerDetails computer, NvApp app) {
        Intent i = new Intent(parent, ShortcutTrampoline.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);
        i.putExtra(AppView.UUID_EXTRA, computer.uuid);
        i.putExtra(Game.EXTRA_APP_NAME, app.getAppName());
        i.putExtra(Game.EXTRA_APP_ID, ""+app.getAppId());
        i.putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported());
        i.setAction(Intent.ACTION_DEFAULT);
        return i;
    }

    public static Intent createStartIntent(Activity parent, NvApp app, ComputerDetails computer,
                                            ComputerManagerService.ComputerManagerBinder managerBinder) {

        Intent intent = new Intent(parent, Game.class);
        intent.putExtra(Game.EXTRA_HOST, computer.activeAddress.address);
        intent.putExtra(Game.EXTRA_PORT, computer.activeAddress.port);
        intent.putExtra(Game.EXTRA_HTTPS_PORT, computer.httpsPort);
        intent.putExtra(Game.EXTRA_APP_NAME, app.getAppName());
        intent.putExtra(Game.EXTRA_APP_ID, app.getAppId());
        intent.putExtra(Game.EXTRA_APP_HDR, app.isHdrSupported());
        intent.putExtra(Game.EXTRA_UNIQUEID, managerBinder.getUniqueId());
        intent.putExtra(Game.EXTRA_PC_UUID, computer.uuid);
        intent.putExtra(Game.EXTRA_PC_NAME, computer.name);
        try {
            if (computer.serverCert != null) {
                intent.putExtra(Game.EXTRA_SERVER_CERT, computer.serverCert.getEncoded());
            }
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return intent;
    }

    private static String getRecentSessionKey(ComputerDetails computer, String suffix) {
        return computer.uuid + suffix;
    }

    private static void rememberRecentSession(Activity parent, NvApp app, ComputerDetails computer) {
        if (computer.uuid == null || app.getAppId() == 0) {
            return;
        }

        parent.getSharedPreferences(RECENT_SESSION_PREF_FILENAME, Activity.MODE_PRIVATE)
                .edit()
                .putInt(getRecentSessionKey(computer, RECENT_SESSION_APP_ID_SUFFIX), app.getAppId())
                .putString(getRecentSessionKey(computer, RECENT_SESSION_APP_NAME_SUFFIX), app.getAppName())
                .putBoolean(getRecentSessionKey(computer, RECENT_SESSION_APP_HDR_SUFFIX), app.isHdrSupported())
                .apply();
    }

    public static NvApp getRecentSession(Activity parent, ComputerDetails computer) {
        if (computer.uuid == null) {
            return null;
        }

        int appId = parent.getSharedPreferences(RECENT_SESSION_PREF_FILENAME, Activity.MODE_PRIVATE)
                .getInt(getRecentSessionKey(computer, RECENT_SESSION_APP_ID_SUFFIX), 0);
        if (appId == 0) {
            return null;
        }

        String appName = parent.getSharedPreferences(RECENT_SESSION_PREF_FILENAME, Activity.MODE_PRIVATE)
                .getString(getRecentSessionKey(computer, RECENT_SESSION_APP_NAME_SUFFIX), "app");
        boolean appHdr = parent.getSharedPreferences(RECENT_SESSION_PREF_FILENAME, Activity.MODE_PRIVATE)
                .getBoolean(getRecentSessionKey(computer, RECENT_SESSION_APP_HDR_SUFFIX), false);
        return new NvApp(appName, appId, appHdr);
    }

    public static void doStart(Activity parent, NvApp app, ComputerDetails computer,
                                ComputerManagerService.ComputerManagerBinder managerBinder) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            UiToast.makeText(parent, parent.getResources().getString(R.string.pair_pc_offline), UiToast.LENGTH_SHORT).show();
            return;
        }
        rememberRecentSession(parent, app, computer);
        parent.startActivity(createStartIntent(parent, app, computer, managerBinder));
    }

}
