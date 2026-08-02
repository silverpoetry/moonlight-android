package com.limelight.utils;

import androidx.annotation.RequiresApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.limelight.R;
import com.limelight.computers.model.HostIdentity;
import com.limelight.nvstream.http.NvApp;
import com.limelight.stream.launch.android.AndroidShortcutIntentFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ShortcutHelper {

    private final ShortcutManager sm;
    private final Context context;
    private final TvChannelHelper tvChannelHelper;

    public ShortcutHelper(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            sm = context.getSystemService(ShortcutManager.class);
        }
        else {
            sm = null;
        }
        this.tvChannelHelper = new TvChannelHelper(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void reapShortcutsForDynamicAdd() {
        List<ShortcutInfo> dynamicShortcuts = sm.getDynamicShortcuts();
        while (!dynamicShortcuts.isEmpty() && dynamicShortcuts.size() >= sm.getMaxShortcutCountPerActivity()) {
            ShortcutInfo maxRankShortcut = dynamicShortcuts.get(0);
            for (ShortcutInfo scut : dynamicShortcuts) {
                if (maxRankShortcut.getRank() < scut.getRank()) {
                    maxRankShortcut = scut;
                }
            }
            sm.removeDynamicShortcuts(Collections.singletonList(maxRankShortcut.getId()));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private List<ShortcutInfo> getAllShortcuts() {
        LinkedList<ShortcutInfo> list = new LinkedList<>();
        list.addAll(sm.getDynamicShortcuts());
        list.addAll(sm.getPinnedShortcuts());
        return list;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo getInfoForId(String id) {
        List<ShortcutInfo> shortcuts = getAllShortcuts();

        for (ShortcutInfo info : shortcuts) {
            if (info.getId().equals(id)) {
                return info;
            }
        }

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private boolean isExistingDynamicShortcut(String id) {
        for (ShortcutInfo si : sm.getDynamicShortcuts()) {
            if (si.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    public void reportComputerShortcutUsed(HostIdentity host) {
        String hostId = host.getId().getValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            if (getInfoForId(hostId) != null) {
                sm.reportShortcutUsed(hostId);
            }
        }
    }

    public void reportGameLaunched(HostIdentity host, NvApp app) {
        tvChannelHelper.createTvChannel(host);
        tvChannelHelper.addGameToChannel(host, app);
    }

    public void createAppViewShortcut(
            HostIdentity host,
            boolean forceAdd,
            boolean newlyPaired) {
        String hostId = host.getId().getValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutInfo sinfo = new ShortcutInfo.Builder(context, hostId)
                    .setIntent(AndroidShortcutIntentFactory
                            .createHostIntent(context, host))
                    .setShortLabel(host.getAdvertisedName())
                    .setLongLabel(host.getAdvertisedName())
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_pc_scut))
                    .build();

            ShortcutInfo existingSinfo = getInfoForId(hostId);
            if (existingSinfo != null) {
                // Update in place
                sm.updateShortcuts(Collections.singletonList(sinfo));
                sm.enableShortcuts(Collections.singletonList(hostId));
            }

            // Reap shortcuts to make space for this if it's new
            // NOTE: This CAN'T be an else on the above if, because it's
            // possible that we have an existing shortcut but it's not a dynamic one.
            if (!isExistingDynamicShortcut(hostId)) {
                // To avoid a random carousel of shortcuts popping in and out based on polling status,
                // we only add shortcuts if it's not at the limit or the user made a conscious action
                // to interact with this PC.

                if (forceAdd) {
                    // This should free an entry for us to add one below
                    reapShortcutsForDynamicAdd();
                }

                // We still need to check the maximum shortcut count even after reaping,
                // because there's a possibility that it could be zero.
                if (sm.getDynamicShortcuts().size() < sm.getMaxShortcutCountPerActivity()) {
                    // Add a shortcut if there is room
                    sm.addDynamicShortcuts(Collections.singletonList(sinfo));
                }
            }
        }

        if (newlyPaired) {
            // Avoid hammering the channel API for each computer poll because it will throttle us
            tvChannelHelper.createTvChannel(host);
            tvChannelHelper.requestChannelOnHomeScreen(host);
        }
    }

    public void createAppViewShortcutForOnlineHost(HostIdentity host) {
        createAppViewShortcut(host, false, false);
    }

    private String getShortcutIdForGame(HostIdentity host, NvApp app) {
        return host.getId().getValue() + app.getAppId();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean createPinnedGameShortcut(
            HostIdentity host,
            NvApp app,
            Bitmap iconBits) {
        if (sm.isRequestPinShortcutSupported()) {
            Icon appIcon;

            if (iconBits != null) {
                appIcon = Icon.createWithAdaptiveBitmap(iconBits);
            } else {
                appIcon = Icon.createWithResource(context, R.mipmap.ic_pc_scut);
            }

            ShortcutInfo sInfo = new ShortcutInfo.Builder(
                    context,
                    getShortcutIdForGame(host, app))
                .setIntent(AndroidShortcutIntentFactory
                        .createAppIntent(context, host, app))
                .setShortLabel(app.getAppName() + " (" +
                        host.getAdvertisedName() + ")")
                .setIcon(appIcon)
                .build();

            return sm.requestPinShortcut(sInfo, null);
        } else {
            return false;
        }
    }

    public void disableComputerShortcut(
            HostIdentity host,
            CharSequence reason) {
        String hostId = host.getId().getValue();
        tvChannelHelper.deleteChannel(host);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // Delete the computer shortcut itself
            if (getInfoForId(hostId) != null) {
                sm.disableShortcuts(Collections.singletonList(hostId), reason);
            }

            // Delete all associated app shortcuts too
            List<ShortcutInfo> shortcuts = getAllShortcuts();
            LinkedList<String> appShortcutIds = new LinkedList<>();
            for (ShortcutInfo info : shortcuts) {
                if (info.getId().startsWith(hostId)) {
                    appShortcutIds.add(info.getId());
                }
            }
            sm.disableShortcuts(appShortcutIds, reason);
        }
    }

    public void disableAppShortcut(
            HostIdentity host,
            NvApp app,
            CharSequence reason) {
        tvChannelHelper.deleteProgram(host, app);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            String id = getShortcutIdForGame(host, app);
            if (getInfoForId(id) != null) {
                sm.disableShortcuts(Collections.singletonList(id), reason);
            }
        }
    }

    public void enableAppShortcut(HostIdentity host, NvApp app) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            String id = getShortcutIdForGame(host, app);
            if (getInfoForId(id) != null) {
                sm.enableShortcuts(Collections.singletonList(id));
            }
        }
    }
}
