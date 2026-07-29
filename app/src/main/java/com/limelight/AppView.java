package com.limelight;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.grid.AppGridAdapter;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.ui.gamemenu.GameDisplayFragment;
import com.limelight.utils.AutoReconnectHelper;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import org.xmlpull.v1.XmlPullParserException;

public class AppView extends Activity implements AdapterFragmentCallbacks {
    private AppGridAdapter appGridAdapter;
    private String uuidString;
    private ShortcutHelper shortcutHelper;

    private ComputerDetails computer;
    private ComputerManagerService.ApplistPoller poller;
    private SpinnerDialog blockingLoadSpinner;
    private String lastRawApplist;
    private int lastRunningAppId;
    private boolean suspendGridUpdates;
    private boolean inForeground;
    private boolean showHiddenApps;
    private HashSet<Integer> hiddenAppIds = new HashSet<>();
    private android.app.AlertDialog pendingAppMenuDialog;

    public final static String HIDDEN_APPS_PREF_FILENAME = "HiddenApps";

    public final static String NAME_EXTRA = "Name";
    public final static String UUID_EXTRA = "UUID";
    public final static String NEW_PAIR_EXTRA = "NewPair";
    public final static String SHOW_HIDDEN_APPS_EXTRA = "ShowHiddenApps";

    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    localBinder.waitForReady();

                    // Get the computer object
                    computer = localBinder.getComputer(uuidString);
                    if (computer == null) {
                        finish();
                        return;
                    }

                    // Add a launcher shortcut for this PC (forced, since this is user interaction)
                    shortcutHelper.createAppViewShortcut(computer, true, getIntent().getBooleanExtra(NEW_PAIR_EXTRA, false));
                    shortcutHelper.reportComputerShortcutUsed(computer);

                    try {
                        appGridAdapter = new AppGridAdapter(AppView.this,
                                PreferenceConfiguration.readPreferences(AppView.this),
                                computer, localBinder.getUniqueId(),
                                showHiddenApps);
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                        return;
                    }

                    appGridAdapter.updateHiddenApps(hiddenAppIds, true);

                    // Now make the binder visible. We must do this after appGridAdapter
                    // is set to prevent us from reaching updateUiWithServerinfo() and
                    // touching the appGridAdapter prior to initialization.
                    managerBinder = localBinder;

                    // Load the app grid with cached data (if possible).
                    // This must be done _before_ startComputerUpdates()
                    // so the initial serverinfo response can update the running
                    // icon.
                    populateAppGridWithCache();

                    // Start updates
                    startComputerUpdates();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tryAutoReconnect();
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || isChangingConfigurations()) {
                                return;
                            }

                            // Despite my best efforts to catch all conditions that could
                            // cause the activity to be destroyed when we try to commit
                            // I haven't been able to, so we have this try-catch block.
                            try {
                                getFragmentManager().beginTransaction()
                                        .replace(R.id.appFragmentContainer, new AdapterFragment())
                                        .commitAllowingStateLoss();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // If appGridAdapter is initialized, let it know about the configuration change.
        // If not, it will pick it up when it initializes.
        if (appGridAdapter != null) {
            // Update the app grid adapter to create grid items with the correct layout
            appGridAdapter.updateLayoutWithPreferences(this, PreferenceConfiguration.readPreferences(this));

            try {
                // Reinflate the app grid itself to pick up the layout change
                getFragmentManager().beginTransaction()
                        .replace(R.id.appFragmentContainer, new AdapterFragment())
                        .commitAllowingStateLoss();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        if(dialogFragment!=null) {
            dialogFragment.dismiss();
        }
    }

    private void startComputerUpdates() {
        // Don't start polling if we're not bound or in the foreground
        if (managerBinder == null || !inForeground) {
            return;
        }

        managerBinder.startPolling(new ComputerManagerListener() {
            @Override
            public void notifyComputerUpdated(final ComputerDetails details) {
                // Do nothing if updates are suspended
                if (suspendGridUpdates) {
                    return;
                }

                // Don't care about other computers
                if (!details.uuid.equalsIgnoreCase(uuidString)) {
                    return;
                }

                if (details.state == ComputerDetails.State.OFFLINE) {
                    // The PC is unreachable now
                    AppView.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Display a toast to the user and quit the activity
                            UiToast.makeText(AppView.this, getResources().getText(R.string.lost_connection), UiToast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

                    return;
                }

                // Close immediately if the PC is no longer paired
                if (details.state == ComputerDetails.State.ONLINE && details.pairState != PairingManager.PairState.PAIRED) {
                    AppView.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Disable shortcuts referencing this PC for now
                            shortcutHelper.disableComputerShortcut(details,
                                    getResources().getString(R.string.scut_not_paired));

                            // Display a toast to the user and quit the activity
                            UiToast.makeText(AppView.this, getResources().getText(R.string.scut_not_paired), UiToast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

                    return;
                }

                AppView.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tryAutoReconnect();
                    }
                });

                // App list is the same or empty
                if (details.rawAppList == null || details.rawAppList.equals(lastRawApplist)) {

                    // Let's check if the running app ID changed
                    if (details.runningGameId != lastRunningAppId) {
                        // Update the currently running game using the app ID
                        lastRunningAppId = details.runningGameId;
                        updateUiWithServerinfo(details);
                    }

                    return;
                }

                lastRunningAppId = details.runningGameId;
                lastRawApplist = details.rawAppList;

                try {
                    updateUiWithAppList(NvHTTP.getAppListByReader(new StringReader(details.rawAppList)));
                    updateUiWithServerinfo(details);

                    if (blockingLoadSpinner != null) {
                        blockingLoadSpinner.dismiss();
                        blockingLoadSpinner = null;
                    }
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (poller == null) {
            poller = managerBinder.createAppListPoller(computer);
        }
        poller.start();
    }

    private void stopComputerUpdates() {
        if (poller != null) {
            poller.stop();
        }

        if (managerBinder != null) {
            managerBinder.stopPolling();
        }

        if (appGridAdapter != null) {
            appGridAdapter.cancelQueuedOperations();
        }
    }

    private GameDisplayFragment dialogFragment;

    private PreferenceConfiguration pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;

        shortcutHelper = new ShortcutHelper(this);

        UiHelper.setLocale(this);

        setContentView(R.layout.activity_app_view_new);

        // Allow floating expanded PiP overlays while browsing apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        UiHelper.notifyNewRootViewImmersive(this);

        showHiddenApps = getIntent().getBooleanExtra(SHOW_HIDDEN_APPS_EXTRA, false);
        uuidString = getIntent().getStringExtra(UUID_EXTRA);

        SharedPreferences hiddenAppsPrefs = getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE);
        for (String hiddenAppIdStr : hiddenAppsPrefs.getStringSet(uuidString, new HashSet<String>())) {
            hiddenAppIds.add(Integer.parseInt(hiddenAppIdStr));
        }

        String computerName = getIntent().getStringExtra(NAME_EXTRA);

        TextView label = findViewById(R.id.appListText);
        setTitle(computerName);
        label.setText(computerName);

        ImageView imageView=findViewById(R.id.iv_root_view);

        pref=PreferenceConfiguration.readPreferences(this);

        if(pref.enableScreenBg&&Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            String fileName= PreferenceManager.getDefaultSharedPreferences(this).getString("screen_bg_file_name","axi_screen_bg.png");
            File imageFile=new File(getFilesDir().getAbsolutePath(),fileName);
            if(imageFile.exists()){
                try{
                    Glide.with(this)
                            .load(imageFile)
                            .skipMemoryCache(true)
                            .diskCacheStrategy( DiskCacheStrategy.ALL )
                            .into(imageView);
                    imageView.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S&&pref.enableScreenObscure) {
                        findViewById(R.id.iv_root_view).setRenderEffect(RenderEffect.createBlurEffect(25, 25, Shader.TileMode.CLAMP));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                imageView.setVisibility(View.GONE);
            }
        }else{
            imageView.setVisibility(View.GONE);
        }

        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialogFragment!=null){
                    dialogFragment.dismiss();
                    dialogFragment=null;
                }
                dialogFragment=new GameDisplayFragment();
                dialogFragment.setWidth(UiHelper.dpToPx(AppView.this,364));
                dialogFragment.setTitle(R.string.game_menu_display_title);
                dialogFragment.setShowLock(false);
                dialogFragment.setPrefConfig(pref);
                dialogFragment.show(getFragmentManager());
            }
        });

        // Bind to the computer manager service
        bindService(new Intent(this, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);
    }

    private void updateHiddenApps(boolean hideImmediately) {
        HashSet<String> hiddenAppIdStringSet = new HashSet<>();

        for (Integer hiddenAppId : hiddenAppIds) {
            hiddenAppIdStringSet.add(hiddenAppId.toString());
        }

        getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .putStringSet(uuidString, hiddenAppIdStringSet)
                .apply();

        appGridAdapter.updateHiddenApps(hiddenAppIds, hideImmediately);
    }

    private void populateAppGridWithCache() {
        try {
            // Try to load from cache
            lastRawApplist = CacheHelper.readInputStreamToString(CacheHelper.openCacheFileForInput(getCacheDir(), "applist", uuidString));
            List<NvApp> applist = NvHTTP.getAppListByReader(new StringReader(lastRawApplist));
            updateUiWithAppList(applist);
            LimeLog.info("Loaded applist from cache");
        } catch (IOException | XmlPullParserException e) {
            if (lastRawApplist != null) {
                LimeLog.warning("Saved applist corrupted: "+lastRawApplist);
                e.printStackTrace();
            }
            LimeLog.info("Loading applist from the network");
            // We'll need to load from the network
            loadAppsBlocking();
        }
    }

    private void loadAppsBlocking() {
        blockingLoadSpinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.applist_refresh_title),
                getResources().getString(R.string.applist_refresh_msg), true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (managerBinder != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(this);

        inForeground = true;
        startComputerUpdates();
        tryAutoReconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        inForeground = false;
        stopComputerUpdates();
    }

    private void tryAutoReconnect() {
        if (!inForeground || managerBinder == null) {
            return;
        }

        AutoReconnectHelper.maybeResumeStream(this, managerBinder, uuidString);
    }

    private void showAppOptionsDialog(final AppObject app, final View targetView) {
        if (app == null) {
            return;
        }

        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_host_options, null, false);
        final TextView titleView = dialogView.findViewById(R.id.tv_host_menu_title);
        final TextView statusView = dialogView.findViewById(R.id.tv_host_menu_status);
        final LinearLayout actionList = dialogView.findViewById(R.id.layout_host_menu_actions);
        final TextView cancelButton = dialogView.findViewById(R.id.btn_host_menu_cancel);

        titleView.setText(app.app.getAppName());
        statusView.setText(getResources().getString(app.isRunning
                ? R.string.applist_menu_status_running
                : R.string.applist_menu_status_available));

        final ArrayList<MenuAction> actions = buildAppMenuActions(app, targetView);
        for (int i = 0; i < actions.size(); i++) {
            View item = createAppOptionView(actionList, actions.get(i));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                itemParams.topMargin = UiHelper.dpToPx(this, 6);
            }
            actionList.addView(item, itemParams);
        }

        pendingAppMenuDialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        pendingAppMenuDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface dialog) {
                pendingAppMenuDialog = null;
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingAppMenuDialog != null) {
                    pendingAppMenuDialog.dismiss();
                }
            }
        });
        pendingAppMenuDialog.show();
        if (pendingAppMenuDialog.getWindow() != null) {
            pendingAppMenuDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private View createAppOptionView(
            LinearLayout parent, final MenuAction action) {
        View item = getLayoutInflater().inflate(
                R.layout.item_host_option, parent, false);
        TextView label = item.findViewById(R.id.tv_host_option);
        ImageView icon = item.findViewById(R.id.iv_host_option_icon);

        label.setText(action.labelResId);
        if (action.iconResId != 0) {
            icon.setImageResource(action.iconResId);
        }
        else {
            icon.setVisibility(View.GONE);
        }
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingAppMenuDialog != null) {
                    pendingAppMenuDialog.dismiss();
                }
                if (action.runnable != null) {
                    action.runnable.run();
                }
            }
        });
        return item;
    }

    private ArrayList<MenuAction> buildAppMenuActions(final AppObject app, final View targetView) {
        ArrayList<MenuAction> actions = new ArrayList<>();

        if (lastRunningAppId != 0) {
            if (lastRunningAppId == app.app.getAppId()) {
                actions.add(new MenuAction(R.string.applist_menu_resume, R.drawable.ic_play, new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doStart(AppView.this, app.app, computer, managerBinder);
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_restart, R.drawable.ic_axi_reboot, new Runnable() {
                    @Override
                    public void run() {
                        restartCurrentApp(app);
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_quit, R.drawable.ic_axi_exit, new Runnable() {
                    @Override
                    public void run() {
                        quitCurrentApp(app, null);
                    }
                }));
            }
            else {
                actions.add(new MenuAction(R.string.applist_menu_quit_and_start, R.drawable.ic_axi_reboot, new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doStart(AppView.this, app.app, computer, managerBinder);
                    }
                }));
            }
        }

        if (lastRunningAppId != app.app.getAppId() || app.isHidden) {
            actions.add(new MenuAction(app.isHidden ? R.string.applist_menu_show_app : R.string.applist_menu_hide_app,
                    app.isHidden ? R.drawable.ic_axi_desktop : R.drawable.ic_axi_unlink,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (app.isHidden) {
                                hiddenAppIds.remove(app.app.getAppId());
                            }
                            else {
                                hiddenAppIds.add(app.app.getAppId());
                            }
                            updateHiddenApps(false);
                        }
                    }));
        }

        actions.add(new MenuAction(R.string.applist_menu_details, R.drawable.ic_axi_app_about, new Runnable() {
            @Override
            public void run() {
                Dialog.displayDialog(AppView.this, getResources().getString(R.string.title_details),
                        app.app.toString(), false);
            }
        }));

        if (canCreatePinnedShortcut(targetView)) {
            actions.add(new MenuAction(R.string.applist_menu_scut, R.drawable.ic_axi_app_add, new Runnable() {
                @Override
                public void run() {
                    Bitmap appBits = getAppBitmap(targetView);
                    if (!shortcutHelper.createPinnedGameShortcut(computer, app.app, appBits)) {
                        UiToast.makeText(AppView.this, getResources().getString(R.string.unable_to_pin_shortcut),
                                UiToast.LENGTH_LONG).show();
                    }
                }
            }));
        }

        return actions;
    }

    private void quitCurrentApp(final AppObject app, final Runnable onComplete) {
        suspendGridUpdates = true;
        ServerHelper.doQuit(AppView.this, computer, app.app, managerBinder, new Runnable() {
            @Override
            public void run() {
                suspendGridUpdates = false;
                if (poller != null) {
                    poller.pollNow();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void restartCurrentApp(final AppObject app) {
        quitCurrentApp(app, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doStart(AppView.this, app.app, computer, managerBinder);
                    }
                });
            }
        });
    }

    private boolean canCreatePinnedShortcut(View targetView) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getAppBitmap(targetView) != null;
    }

    private Bitmap getAppBitmap(View targetView) {
        if (targetView == null) {
            return null;
        }

        ImageView appImageView = targetView.findViewById(R.id.grid_image);
        if (appImageView == null) {
            return null;
        }

        if (appImageView.getDrawable() instanceof BitmapDrawable) {
            BitmapDrawable drawable = (BitmapDrawable) appImageView.getDrawable();
            return drawable.getBitmap();
        }
        else if (appImageView.getDrawable() instanceof GlideBitmapDrawable) {
            GlideBitmapDrawable drawable = (GlideBitmapDrawable) appImageView.getDrawable();
            return drawable.getBitmap();
        }

        return null;
    }

    private void updateUiWithServerinfo(final ComputerDetails details) {
        AppView.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;

                    // Look through our current app list to tag the running app
                for (int i = 0; i < appGridAdapter.getCount(); i++) {
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                    // There can only be one or zero apps running.
                    if (existingApp.isRunning &&
                            existingApp.app.getAppId() == details.runningGameId) {
                        // This app was running and still is, so we're done now
                        return;
                    }
                    else if (existingApp.app.getAppId() == details.runningGameId) {
                        // This app wasn't running but now is
                        existingApp.isRunning = true;
                        updated = true;
                    }
                    else if (existingApp.isRunning) {
                        // This app was running but now isn't
                        existingApp.isRunning = false;
                        updated = true;
                    }
                    else {
                        // This app wasn't running and still isn't
                    }
                }

                if (updated) {
                    appGridAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void updateUiWithAppList(final List<NvApp> appList) {
        AppView.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;

                // First handle app updates and additions
                for (NvApp app : appList) {
                    boolean foundExistingApp = false;

                    // Try to update an existing app in the list first
                    for (int i = 0; i < appGridAdapter.getCount(); i++) {
                        AppObject existingApp = (AppObject) appGridAdapter.getItem(i);
                        if (existingApp.app.getAppId() == app.getAppId()) {
                            // Found the app; update its properties
                            if (!existingApp.app.getAppName().equals(app.getAppName())) {
                                existingApp.app.setAppName(app.getAppName());
                                updated = true;
                            }

                            foundExistingApp = true;
                            break;
                        }
                    }

                    if (!foundExistingApp) {
                        // This app must be new
                        appGridAdapter.addApp(new AppObject(app));

                        // We could have a leftover shortcut from last time this PC was paired
                        // or if this app was removed then added again. Enable those shortcuts
                        // again if present.
                        shortcutHelper.enableAppShortcut(computer, app);

                        updated = true;
                    }
                }

                // Next handle app removals
                int i = 0;
                while (i < appGridAdapter.getCount()) {
                    boolean foundExistingApp = false;
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                    // Check if this app is in the latest list
                    for (NvApp app : appList) {
                        if (existingApp.app.getAppId() == app.getAppId()) {
                            foundExistingApp = true;
                            break;
                        }
                    }

                    // This app was removed in the latest app list
                    if (!foundExistingApp) {
                        shortcutHelper.disableAppShortcut(computer, existingApp.app, "App removed from PC");
                        appGridAdapter.removeApp(existingApp);
                        updated = true;

                        // Check this same index again because the item at i+1 is now at i after
                        // the removal
                        continue;
                    }

                    // Move on to the next item
                    i++;
                }

                if (updated) {
                    appGridAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public int getAdapterFragmentLayoutId() {
//        return PreferenceConfiguration.readPreferences(AppView.this).smallIconMode ?
//                    R.layout.app_grid_view_small : R.layout.app_grid_view;
        return R.layout.app_grid_view_new;
    }

    @Override
    public void receiveAbsListView(AbsListView listView) {
        listView.setAdapter(appGridAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long id) {
                AppObject app = (AppObject) appGridAdapter.getItem(pos);
                ServerHelper.doStart(AppView.this, app.app, computer, managerBinder);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AppObject app = (AppObject) appGridAdapter.getItem(position);
                showAppOptionsDialog(app, view);
                return true;
            }
        });
        UiHelper.applyStatusBarPadding(listView);
        listView.requestFocus();
    }

    private static final class MenuAction {
        final int labelResId;
        final int iconResId;
        final Runnable runnable;

        MenuAction(int labelResId, int iconResId, Runnable runnable) {
            this.labelResId = labelResId;
            this.iconResId = iconResId;
            this.runnable = runnable;
        }
    }

    public static class AppObject {
        public final NvApp app;
        public boolean isRunning;
        public boolean isHidden;

        public AppObject(NvApp app) {
            if (app == null) {
                throw new IllegalArgumentException("app must not be null");
            }
            this.app = app;
        }

        @Override
        public String toString() {
            return app.getAppName();
        }
    }

}
