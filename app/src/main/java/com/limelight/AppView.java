package com.limelight;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.HostPollingClientLifecycle;
import com.limelight.computers.LegacyHostRuntimeAdapter;
import com.limelight.computers.http.android.AndroidNvHttpClientFactory;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.session.HostQuitUseCase;
import com.limelight.computers.session.NvHttpHostQuitBackend;
import com.limelight.grid.AppGridAdapter;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.settings.SettingsMigrationRunner;
import com.limelight.settings.SettingsRepository;
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader;
import com.limelight.settings.android.AndroidDisplayAspectProvider;
import com.limelight.settings.android.SharedPreferencesCustomResolutionRepository;
import com.limelight.settings.android.AndroidSettingsRepository;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsLoader;
import com.limelight.settings.audio.StreamAudioSettingsState;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.app.AppPresentationSettings;
import com.limelight.settings.stream.CustomResolutionRepository;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettingsLoader;
import com.limelight.settings.stream.StreamVideoSettingsState;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.stream.launch.android.AndroidStreamAutoReconnectController;
import com.limelight.stream.launch.android.AndroidStreamLaunchFeedback;
import com.limelight.stream.launch.android.AndroidStreamLauncher;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.ui.gamemenu.GameDisplayFragment;
import com.limelight.ui.gamemenu.GameDisplayHost;
import com.limelight.ui.hosts.ScreenBackgroundPresenter;
import com.limelight.ui.hosts.HostQuitMessageResolver;
import com.limelight.ui.hosts.HostServiceBindingController;
import com.limelight.ui.hosts.HostUiOperationController;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import org.xmlpull.v1.XmlPullParserException;

public class AppView extends BaseActivity implements AdapterFragmentCallbacks,
        GameDisplayHost {
    private AppGridAdapter appGridAdapter;
    private String uuidString;
    private ShortcutHelper shortcutHelper;

    private ComputerDetails computer;
    private volatile ComputerManagerService.ApplistPoller poller;
    private SpinnerDialog blockingLoadSpinner;
    private String lastRawApplist;
    private int lastRunningAppId;
    private volatile boolean suspendGridUpdates;
    private volatile boolean inForeground;
    private boolean showHiddenApps;
    private HashSet<Integer> hiddenAppIds = new HashSet<>();
    private android.app.AlertDialog pendingAppMenuDialog;
    private final HostQuitUseCase hostQuitUseCase =
            new HostQuitUseCase();
    private HostUiOperationController hostOperationController;
    private HostServiceBindingController hostBindingController;
    private AndroidStreamLauncher streamLauncher;
    private AndroidStreamAutoReconnectController
            autoReconnectController;

    public final static String HIDDEN_APPS_PREF_FILENAME = "HiddenApps";

    public final static String NAME_EXTRA = "Name";
    public final static String UUID_EXTRA = "UUID";
    public final static String NEW_PAIR_EXTRA = "NewPair";
    public final static String SHOW_HIDDEN_APPS_EXTRA = "ShowHiddenApps";

    private volatile ComputerManagerService.ComputerManagerBinder managerBinder;
    private final HostPollingClientLifecycle hostPollingLifecycle =
            new HostPollingClientLifecycle();
    private final Object pollingLifecycleLock = new Object();
    private volatile boolean activityDestroyed;
    private boolean managerServiceBound;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);
            HostServiceBindingController controller =
                    hostBindingController;
            if (controller == null) {
                return;
            }
            HostServiceBindingController.ConnectStatus status =
                    controller.connect(
                            cancellation -> initializeAppBinding(
                                    localBinder,
                                    cancellation),
                            AppView.this::onAppBindingInitialized,
                            AppBindingInitialization::discard);
            if (status != HostServiceBindingController.ConnectStatus.ACCEPTED) {
                LimeLog.severe(
                        "Unable to initialize app-list service binding: " +
                                status);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (hostBindingController != null) {
                hostBindingController.disconnect();
            }
            synchronized (pollingLifecycleLock) {
                managerBinder = null;
            }
            runCloseAction(hostPollingLifecycle.onConnectionLost());
        }
    };

    private AppBindingInitialization initializeAppBinding(
            ComputerManagerService.ComputerManagerBinder binder,
            HostServiceBindingController.CancellationSignal
                    cancellation) throws Exception {
        if (!binder.waitForReady() || cancellation.isCanceled()) {
            return null;
        }

        HostRuntimeSnapshot loadedHost = binder.getHost(
                HostId.of(uuidString));
        ComputerDetails loadedComputer = loadedHost == null
                ? null
                : LegacyHostRuntimeAdapter.toComputerDetails(loadedHost);
        if (loadedComputer == null) {
            return AppBindingInitialization.missingHost();
        }

        AppGridAdapter loadedAdapter = new AppGridAdapter(
                this,
                appPresentationSettings.usesSmallAppIcons(),
                loadedComputer,
                binder.getUniqueId(),
                showHiddenApps);
        boolean transferred = false;
        try {
            loadedAdapter.updateHiddenApps(hiddenAppIds, true);
            if (cancellation.isCanceled()) {
                return null;
            }

            String cachedRawAppList = null;
            List<NvApp> cachedApps = null;
            try {
                cachedRawAppList = CacheHelper
                        .readInputStreamToString(
                                CacheHelper.openCacheFileForInput(
                                        getCacheDir(),
                                        "applist",
                                        uuidString));
                cachedApps = NvHTTP.getAppListByReader(
                        new StringReader(cachedRawAppList));
                LimeLog.info("Loaded app list from cache");
            }
            catch (IOException | XmlPullParserException error) {
                LimeLog.info(
                        "Cached app list unavailable: " +
                                error.getClass().getSimpleName());
            }
            if (cancellation.isCanceled()) {
                return null;
            }

            // Shortcut persistence may perform disk I/O, so keep it on the
            // binding worker after the connection has won cancellation.
            shortcutHelper.createAppViewShortcut(
                    loadedComputer,
                    true,
                    getIntent().getBooleanExtra(
                            NEW_PAIR_EXTRA,
                            false));
            shortcutHelper.reportComputerShortcutUsed(
                    loadedComputer);
            if (cancellation.isCanceled()) {
                return null;
            }

            AppBindingInitialization result =
                    AppBindingInitialization.ready(
                            binder,
                            loadedComputer,
                            loadedAdapter,
                            cachedRawAppList,
                            cachedApps);
            transferred = true;
            return result;
        }
        finally {
            if (!transferred) {
                loadedAdapter.cancelQueuedOperations();
            }
        }
    }

    private void onAppBindingInitialized(
            HostServiceBindingController.Result<
                    AppBindingInitialization> result) {
        if (!result.isSuccessful()) {
            LimeLog.severe(
                    "App-list binding initialization failed: " +
                            result.getFailure().getClass().getSimpleName());
            finish();
            return;
        }

        AppBindingInitialization initialization = result.getValue();
        if (initialization == null) {
            return;
        }
        if (!initialization.hasHost()) {
            finish();
            return;
        }
        AppGridAdapter previousAdapter;
        synchronized (pollingLifecycleLock) {
            if (activityDestroyed) {
                initialization.discard();
                return;
            }
            previousAdapter = appGridAdapter;
            computer = initialization.computer;
            appGridAdapter = initialization.adapter;
            managerBinder = initialization.binder;
        }
        if (previousAdapter != null &&
                previousAdapter != initialization.adapter) {
            previousAdapter.cancelQueuedOperations();
        }

        lastRawApplist = initialization.cachedRawAppList;
        lastRunningAppId = 0;
        if (initialization.cachedApps != null) {
            if (blockingLoadSpinner != null) {
                blockingLoadSpinner.dismiss();
                blockingLoadSpinner = null;
            }
            updateUiWithAppList(initialization.cachedApps);
        }
        else {
            LimeLog.info("Loading app list from the network");
            loadAppsBlocking();
        }

        startComputerUpdates();
        tryAutoReconnect();
        if (isFinishing() ||
                isChangingConfigurations() ||
                getSupportFragmentManager().isDestroyed()) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(
                        R.id.appFragmentContainer,
                        new AdapterFragment())
                .commitAllowingStateLoss();
    }

    private static final class AppBindingInitialization {
        private final ComputerManagerService.ComputerManagerBinder binder;
        private final ComputerDetails computer;
        private final AppGridAdapter adapter;
        private final String cachedRawAppList;
        private final List<NvApp> cachedApps;

        private AppBindingInitialization(
                ComputerManagerService.ComputerManagerBinder binder,
                ComputerDetails computer,
                AppGridAdapter adapter,
                String cachedRawAppList,
                List<NvApp> cachedApps) {
            this.binder = binder;
            this.computer = computer;
            this.adapter = adapter;
            this.cachedRawAppList = cachedRawAppList;
            this.cachedApps = cachedApps;
        }

        static AppBindingInitialization missingHost() {
            return new AppBindingInitialization(
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        static AppBindingInitialization ready(
                ComputerManagerService.ComputerManagerBinder binder,
                ComputerDetails computer,
                AppGridAdapter adapter,
                String cachedRawAppList,
                List<NvApp> cachedApps) {
            return new AppBindingInitialization(
                    binder,
                    computer,
                    adapter,
                    cachedRawAppList,
                    cachedApps);
        }

        boolean hasHost() {
            return computer != null;
        }

        void discard() {
            if (adapter != null) {
                adapter.cancelQueuedOperations();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // If appGridAdapter is initialized, let it know about the configuration change.
        // If not, it will pick it up when it initializes.
        if (appGridAdapter != null) {
            // Update the app grid adapter to create grid items with the correct layout
            appPresentationSettings =
                    AndroidAppPresentationSettingsLoader.load(this);
            appGridAdapter.updateLayout(
                    this,
                    appPresentationSettings.usesSmallAppIcons());

            try {
                // Reinflate the app grid itself to pick up the layout change
                getSupportFragmentManager().beginTransaction()
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
        ComputerManagerService.ComputerManagerBinder binder;
        synchronized (pollingLifecycleLock) {
            if (managerBinder == null ||
                    !inForeground) {
                return;
            }
            binder = managerBinder;
        }
        HostPollingClientLifecycle.StartToken startToken =
                hostPollingLifecycle.beginStart();
        if (startToken == null) {
            return;
        }
        synchronized (pollingLifecycleLock) {
            if (managerBinder != binder || !inForeground) {
                hostPollingLifecycle.failStart(startToken);
                return;
            }
        }

        ComputerManagerService.HostPollingSubscription newSubscription;
        try {
            newSubscription = binder.startPolling(
                    new ComputerManagerListener() {
            @Override
            public void notifyComputerUpdated(HostRuntimeSnapshot snapshot) {
                final ComputerDetails details = LegacyHostRuntimeAdapter
                        .toComputerDetails(snapshot);
                // Do nothing if updates are suspended
                if (!hostPollingLifecycle.owns(startToken) ||
                        suspendGridUpdates) {
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
                            if (!hostPollingLifecycle.owns(startToken)) {
                                return;
                            }
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
                            if (!hostPollingLifecycle.owns(startToken)) {
                                return;
                            }
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
                        if (hostPollingLifecycle.owns(startToken)) {
                            tryAutoReconnect();
                        }
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
        }
        catch (RuntimeException | Error error) {
            hostPollingLifecycle.failStart(startToken);
            throw error;
        }

        ComputerManagerService.ApplistPoller newPoller;
        try {
            newPoller = newSubscription.startAppListPolling(
                    HostId.of(computer.uuid));
            if (newPoller == null) {
                newSubscription.close();
                hostPollingLifecycle.failStart(startToken);
                return;
            }
        }
        catch (RuntimeException | Error error) {
            newSubscription.close();
            hostPollingLifecycle.failStart(startToken);
            throw error;
        }
        poller = newPoller;
        Runnable closeAction = () -> closeHostUpdates(
                newSubscription,
                newPoller);
        hostPollingLifecycle.completeStart(startToken, closeAction);
    }

    private void stopComputerUpdates() {
        runCloseAction(hostPollingLifecycle.deactivate());

        if (appGridAdapter != null) {
            appGridAdapter.cancelQueuedOperations();
        }
    }

    private void closeHostUpdates(
            ComputerManagerService.HostPollingSubscription subscription,
            ComputerManagerService.ApplistPoller appListPoller) {
        synchronized (pollingLifecycleLock) {
            if (poller == appListPoller) {
                poller = null;
            }
        }
        // The host subscription owns and closes its app-list worker.
        subscription.close();
    }

    private static void runCloseAction(Runnable closeAction) {
        if (closeAction != null) {
            closeAction.run();
        }
    }

    private GameDisplayFragment dialogFragment;

    private AppPresentationSettings appPresentationSettings;
    private SettingsRepository settingsRepository;
    private StreamVideoSettingsState streamVideoSettingsState;
    private StreamAudioSettingsState streamAudioSettingsState;
    private CustomResolutionRepository
            customResolutionRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hostOperationController = HostUiOperationController.create(
                this::runOnUiThread);
        hostBindingController = HostServiceBindingController.create(
                this::runOnUiThread);
        streamLauncher = new AndroidStreamLauncher(this);
        autoReconnectController =
                new AndroidStreamAutoReconnectController(
                        ((MoonlightApplication) getApplication())
                                .getPendingStreamReconnectStore(),
                        streamLauncher);

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;
        hostPollingLifecycle.activate();

        shortcutHelper = new ShortcutHelper(this);

        setContentView(R.layout.activity_app_view_new);

        // Allow floating expanded PiP overlays while browsing apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        UiHelper.notifyNewEdgeToEdgeRootView(
                this, R.id.rv_top_view, R.id.appFragmentContainer);

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

        appPresentationSettings =
                AndroidAppPresentationSettingsLoader.load(this);
        settingsRepository = AndroidSettingsRepository.create(this);
        SettingsMigrationRunner.migrate(settingsRepository);
        streamVideoSettingsState =
                new StreamVideoSettingsState(
                        StreamVideoSettingsLoader.load(
                                settingsRepository,
                                AndroidDisplayAspectProvider
                                        .get(this)));
        streamAudioSettingsState =
                new StreamAudioSettingsState(
                        StreamAudioSettingsLoader.load(
                                settingsRepository));
        customResolutionRepository =
                new SharedPreferencesCustomResolutionRepository(
                        getSharedPreferences(
                                SharedPreferencesCustomResolutionRepository
                                        .PREFERENCES_NAME,
                                MODE_PRIVATE));

        ScreenBackgroundPresenter.apply(
                this,
                imageView,
                appPresentationSettings);

        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialogFragment!=null){
                    dialogFragment.dismiss();
                    dialogFragment=null;
                }
                dialogFragment =
                        GameDisplayFragment.newInstance(false);
                dialogFragment.setWidth(UiHelper.dpToPx(AppView.this,364));
                dialogFragment.show(getSupportFragmentManager());
            }
                });

        // Bind to the computer manager service
        managerServiceBound = bindService(
                new Intent(this, ComputerManagerService.class),
                serviceConnection,
                Service.BIND_AUTO_CREATE);
    }

    @Override
    public StreamVideoSettings getStreamVideoSettings() {
        return streamVideoSettingsState.get();
    }

    @Override
    public void applyStreamVideoSettingsUpdate(
            StreamVideoSettingsUpdate update) {
        StreamVideoSettings updated =
                update.applyTo(streamVideoSettingsState.get());
        update.persist(settingsRepository);
        streamVideoSettingsState.replace(updated);
    }

    @Override
    public CustomResolutionRepository
            getCustomResolutionRepository() {
        return customResolutionRepository;
    }

    @Override
    public StreamAudioSettings getStreamAudioSettings() {
        return streamAudioSettingsState.get();
    }

    @Override
    public void applyStreamAudioSettingsUpdate(
            StreamAudioSettingsUpdate update) {
        StreamAudioSettings updated =
                update.applyTo(streamAudioSettingsState.get());
        update.persist(settingsRepository);
        streamAudioSettingsState.replace(updated);
    }

    @Override
    public void onDisplayConfigurationApplied() {
        // App-list changes apply to the next stream without navigation.
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

    private void loadAppsBlocking() {
        blockingLoadSpinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.applist_refresh_title),
                getResources().getString(R.string.applist_refresh_msg), true);
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        if (streamLauncher != null) {
            streamLauncher.onOwnerDestroyed();
        }
        runCloseAction(hostPollingLifecycle.destroy());
        if (hostOperationController != null) {
            hostOperationController.destroy();
            hostOperationController = null;
        }
        if (hostBindingController != null) {
            hostBindingController.destroy();
            hostBindingController = null;
        }

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (managerServiceBound) {
            unbindService(serviceConnection);
            managerServiceBound = false;
        }
        synchronized (pollingLifecycleLock) {
            managerBinder = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (streamLauncher != null) {
            streamLauncher.onOwnerResumed();
        }

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(this);

        inForeground = true;
        hostPollingLifecycle.activate();
        startComputerUpdates();
        tryAutoReconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (streamLauncher != null) {
            streamLauncher.onOwnerPaused();
        }
        inForeground = false;
        cancelHostOperation();
        stopComputerUpdates();
    }

    private void tryAutoReconnect() {
        if (!inForeground ||
                managerBinder == null ||
                autoReconnectController == null) {
            return;
        }

        autoReconnectController.maybeResume(
                managerBinder,
                uuidString);
    }

    private void launchStream(
            NvApp app,
            ComputerDetails targetComputer) {
        ComputerManagerService.ComputerManagerBinder binder =
                managerBinder;
        if (binder == null || streamLauncher == null) {
            UiToast.makeText(
                    this,
                    R.string.error_manager_not_running,
                    UiToast.LENGTH_LONG).show();
            return;
        }

        AndroidStreamLauncher.Result result = streamLauncher.launch(
                targetComputer,
                app,
                binder.getUniqueId());
        AndroidStreamLaunchFeedback.showIfNeeded(this, result);
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
                        launchStream(app.app, computer);
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_restart, R.drawable.ic_reboot, new Runnable() {
                    @Override
                    public void run() {
                        restartCurrentApp(app);
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_quit, R.drawable.ic_exit, new Runnable() {
                    @Override
                    public void run() {
                        quitCurrentApp(app, null);
                    }
                }));
            }
            else {
                actions.add(new MenuAction(R.string.applist_menu_quit_and_start, R.drawable.ic_reboot, new Runnable() {
                    @Override
                    public void run() {
                        launchStream(app.app, computer);
                    }
                }));
            }
        }

        if (lastRunningAppId != app.app.getAppId() || app.isHidden) {
            actions.add(new MenuAction(app.isHidden ? R.string.applist_menu_show_app : R.string.applist_menu_hide_app,
                    app.isHidden ? R.drawable.ic_desktop : R.drawable.ic_unlink,
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

        actions.add(new MenuAction(R.string.applist_menu_details, R.drawable.ic_app_about, new Runnable() {
            @Override
            public void run() {
                Dialog.displayDialog(AppView.this, getResources().getString(R.string.title_details),
                        app.app.toString(), false);
            }
        }));

        if (canCreatePinnedShortcut(targetView)) {
            actions.add(new MenuAction(R.string.applist_menu_scut, R.drawable.ic_app_add, new Runnable() {
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

    private void quitCurrentApp(
            AppObject app,
            Runnable onQuitSucceeded) {
        ComputerManagerService.ComputerManagerBinder binder =
                managerBinder;
        HostUiOperationController controller =
                hostOperationController;
        if (binder == null || controller == null) {
            UiToast.makeText(
                    this,
                    R.string.error_manager_not_running,
                    UiToast.LENGTH_LONG).show();
            return;
        }

        final HostQuitUseCase.Backend backend;
        try {
            backend = new NvHttpHostQuitBackend(
                    AndroidNvHttpClientFactory.create(
                            this,
                            computer,
                            binder.getUniqueId()));
        }
        catch (IOException failure) {
            showQuitFailure(app.app.getAppName(), failure);
            return;
        }

        HostUiOperationController.RequestStatus status =
                controller.request(
                        () -> hostQuitUseCase.execute(backend),
                        result -> onQuitCompleted(
                                app,
                                onQuitSucceeded,
                                result));
        if (status == HostUiOperationController.RequestStatus.ACCEPTED) {
            suspendGridUpdates = true;
            UiToast.makeText(
                    this,
                    getText(R.string.applist_quit_app) + " " +
                            app.app.getAppName() + "...",
                    UiToast.LENGTH_SHORT).show();
        }
        else if (status ==
                HostUiOperationController.RequestStatus.ALREADY_RUNNING) {
            UiToast.makeText(
                    this,
                    R.string.host_operation_in_progress,
                    UiToast.LENGTH_SHORT).show();
        }
        else {
            UiToast.makeText(
                    this,
                    R.string.host_operation_unavailable,
                    UiToast.LENGTH_LONG).show();
        }
    }

    private void onQuitCompleted(
            AppObject app,
            Runnable onQuitSucceeded,
            HostUiOperationController.Result<HostQuitUseCase.Outcome>
                    result) {
        suspendGridUpdates = false;
        if (!result.isSuccessful()) {
            showQuitFailure(
                    app.app.getAppName(),
                    result.getFailure());
            return;
        }

        HostQuitUseCase.Outcome outcome = result.getValue();
        UiToast.makeText(
                this,
                HostQuitMessageResolver.resolve(
                        this,
                        app.app.getAppName(),
                        outcome),
                UiToast.LENGTH_LONG).show();
        if (outcome != HostQuitUseCase.Outcome.QUIT) {
            return;
        }
        ComputerManagerService.ApplistPoller currentPoller = poller;
        if (currentPoller != null) {
            currentPoller.pollNow();
        }
        if (onQuitSucceeded != null) {
            onQuitSucceeded.run();
        }
    }

    private void showQuitFailure(
            String appName,
            Exception failure) {
        UiToast.makeText(
                this,
                HostQuitMessageResolver.resolveFailure(
                        this,
                        appName,
                        failure),
                UiToast.LENGTH_LONG).show();
    }

    private void cancelHostOperation() {
        if (hostOperationController != null) {
            hostOperationController.cancelCurrent();
        }
        suspendGridUpdates = false;
    }

    private void restartCurrentApp(final AppObject app) {
        quitCurrentApp(app, () -> {
            ComputerManagerService.ComputerManagerBinder binder =
                    managerBinder;
            if (binder != null) {
                launchStream(app.app, computer);
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
                launchStream(app.app, computer);
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
