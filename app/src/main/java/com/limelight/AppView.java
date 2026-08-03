package com.limelight;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.HostPollingClientLifecycle;
import com.limelight.computers.apps.HiddenAppRepository;
import com.limelight.computers.apps.HiddenAppSelection;
import com.limelight.computers.apps.android.SharedPreferencesHiddenAppRepository;
import com.limelight.computers.http.android.AndroidNvHttpClientFactory;
import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.session.HostQuitUseCase;
import com.limelight.computers.session.NvHttpHostQuitBackend;
import com.limelight.binding.video.AndroidDecoderCrashStore;
import com.limelight.grid.AppGridAdapter;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
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
import com.limelight.ui.gamemenu.GameDisplayFragment;
import com.limelight.ui.gamemenu.GameDisplayHost;
import com.limelight.ui.decoder.AndroidDecoderCrashNotificationController;
import com.limelight.ui.hosts.HostQuitMessageResolver;
import com.limelight.ui.hosts.HostServiceBindingController;
import com.limelight.ui.hosts.HostUiOperationController;
import com.limelight.ui.compose.apps.ApplicationScreenRenderer;
import com.limelight.ui.compose.components.ActionMenuPresenter;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import com.limelight.utils.UiToast;

import org.xmlpull.v1.XmlPullParserException;

public class AppView extends BaseActivity implements GameDisplayHost {
    private AppGridAdapter appGridAdapter;
    private String uuidString;
    private HostId hostId;
    private ShortcutHelper shortcutHelper;

    private volatile HostRuntimeSnapshot hostSnapshot;
    private volatile ComputerManagerService.ApplistPoller poller;
    private SpinnerDialog blockingLoadSpinner;
    private String lastRawApplist;
    private int lastRunningAppId;
    private volatile boolean suspendGridUpdates;
    private volatile boolean inForeground;
    private boolean showHiddenApps;
    private HashSet<Integer> hiddenAppIds = new HashSet<>();
    private final HostQuitUseCase hostQuitUseCase =
            new HostQuitUseCase();
    private HostUiOperationController hostOperationController;
    private HostServiceBindingController hostBindingController;
    private AndroidStreamLauncher streamLauncher;
    private AndroidStreamAutoReconnectController
            autoReconnectController;
    private AndroidDecoderCrashNotificationController
            decoderCrashNotificationController;
    private HiddenAppRepository hiddenAppRepository;
    private ApplicationScreenRenderer applicationScreenRenderer;
    private ActionMenuPresenter actionMenuPresenter;

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
                hostId);
        if (loadedHost == null) {
            return AppBindingInitialization.missingHost();
        }
        AppGridAdapter loadedAdapter = new AppGridAdapter(
                this,
                appPresentationSettings.usesSmallAppIcons(),
                loadedHost,
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
                    loadedHost.getRecord().getIdentity(),
                    true,
                    getIntent().getBooleanExtra(
                            NEW_PAIR_EXTRA,
                            false));
            shortcutHelper.reportComputerShortcutUsed(
                    loadedHost.getRecord().getIdentity());
            if (cancellation.isCanceled()) {
                return null;
            }

            AppBindingInitialization result =
                    AppBindingInitialization.ready(
                            binder,
                            loadedHost,
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
            hostSnapshot = initialization.host;
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
        refreshApplicationScreen();
    }

    private static final class AppBindingInitialization {
        private final ComputerManagerService.ComputerManagerBinder binder;
        private final HostRuntimeSnapshot host;
        private final AppGridAdapter adapter;
        private final String cachedRawAppList;
        private final List<NvApp> cachedApps;

        private AppBindingInitialization(
                ComputerManagerService.ComputerManagerBinder binder,
                HostRuntimeSnapshot host,
                AppGridAdapter adapter,
                String cachedRawAppList,
                List<NvApp> cachedApps) {
            this.binder = binder;
            this.host = host;
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
                HostRuntimeSnapshot host,
                AppGridAdapter adapter,
                String cachedRawAppList,
                List<NvApp> cachedApps) {
            return new AppBindingInitialization(
                    binder,
                    host,
                    adapter,
                    cachedRawAppList,
                    cachedApps);
        }

        boolean hasHost() {
            return host != null;
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
            refreshApplicationScreen();
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
                if (!hostPollingLifecycle.owns(startToken)) {
                    return;
                }

                // Don't care about other computers
                if (!snapshot.getRecord().getIdentity().getId()
                        .equals(hostId)) {
                    return;
                }

                // Keep every action on the latest immutable endpoint,
                // certificate, and host state even while grid updates are
                // temporarily suspended by a host operation.
                hostSnapshot = snapshot;
                AppGridAdapter currentAdapter = appGridAdapter;
                if (currentAdapter != null) {
                    currentAdapter.updateHost(snapshot);
                }
                if (suspendGridUpdates) {
                    return;
                }

                HostConnectionState connectionState =
                        snapshot.getConnectionState();
                if (connectionState.getReachability() ==
                        HostConnectionState.Reachability.OFFLINE) {
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
                if (connectionState.getReachability() ==
                        HostConnectionState.Reachability.ONLINE &&
                        connectionState.getPairingStatus() !=
                                HostConnectionState.PairingStatus.PAIRED) {
                    AppView.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!hostPollingLifecycle.owns(startToken)) {
                                return;
                            }
                            // Disable shortcuts referencing this PC for now
                            shortcutHelper.disableComputerShortcut(
                                    snapshot.getRecord().getIdentity(),
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

                String rawAppList = snapshot.getRawAppList();
                int runningAppId = connectionState.getRunningAppId();
                // App list is the same or empty
                if (rawAppList == null ||
                        rawAppList.equals(lastRawApplist)) {

                    // Let's check if the running app ID changed
                    if (runningAppId != lastRunningAppId) {
                        // Update the currently running game using the app ID
                        lastRunningAppId = runningAppId;
                        updateUiWithServerinfo(runningAppId);
                    }

                    return;
                }

                lastRunningAppId = runningAppId;
                lastRawApplist = rawAppList;

                try {
                    updateUiWithAppList(NvHTTP.getAppListByReader(
                            new StringReader(rawAppList)));
                    updateUiWithServerinfo(runningAppId);

                    if (blockingLoadSpinner != null) {
                        blockingLoadSpinner.dismiss();
                        blockingLoadSpinner = null;
                    }
                } catch (XmlPullParserException | IOException e) {
                    LimeLog.warning(
                            "Unable to refresh the host app list",
                            e);
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
                    hostId);
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

        decoderCrashNotificationController =
                new AndroidDecoderCrashNotificationController(
                        this,
                        new AndroidDecoderCrashStore(this));
        actionMenuPresenter = new ActionMenuPresenter(this);

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

        applicationScreenRenderer = new ApplicationScreenRenderer(
                this,
                new ApplicationScreenRenderer.Listener() {
                    @Override
                    public void onBackRequested() {
                        finish();
                    }

                    @Override
                    public void onDisplayOptionsRequested() {
                        if (dialogFragment != null) {
                            dialogFragment.dismiss();
                            dialogFragment = null;
                        }
                        dialogFragment =
                                GameDisplayFragment.newInstance(false);
                        dialogFragment.setWidth(
                                UiHelper.dpToPx(AppView.this, 364));
                        dialogFragment.show(getSupportFragmentManager());
                    }

                    @Override
                    public void onAppRequested(AppObject app) {
                        launchStream(app.app);
                    }

                    @Override
                    public void onAppMenuRequested(
                            AppObject app,
                            Bitmap artwork) {
                        showAppOptionsDialog(app, artwork);
                    }
                });
        setContentView(applicationScreenRenderer.createRootView());

        // Allow floating expanded PiP overlays while browsing apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        showHiddenApps = getIntent().getBooleanExtra(SHOW_HIDDEN_APPS_EXTRA, false);
        uuidString = getIntent().getStringExtra(UUID_EXTRA);
        hostId = HostId.of(uuidString);
        hiddenAppRepository =
                new SharedPreferencesHiddenAppRepository(this);
        hiddenAppIds.addAll(
                hiddenAppRepository.load(hostId).getAppIds());

        String computerName = getIntent().getStringExtra(NAME_EXTRA);

        setTitle(computerName);
        applicationScreenRenderer.updateTitle(computerName);

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
                        this);

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
        hiddenAppRepository.save(
                hostId,
                HiddenAppSelection.of(hiddenAppIds));

        appGridAdapter.updateHiddenApps(hiddenAppIds, hideImmediately);
        refreshApplicationScreen();
    }

    private void loadAppsBlocking() {
        if (applicationScreenRenderer != null) {
            applicationScreenRenderer.showLoading();
        }
        if (actionMenuPresenter != null) {
            actionMenuPresenter.destroy();
            actionMenuPresenter = null;
        }
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
        if (applicationScreenRenderer != null) {
            applicationScreenRenderer.destroy();
            applicationScreenRenderer = null;
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

        if (refreshSettingsAfterResume()) {
            return;
        }

        if (streamLauncher != null) {
            streamLauncher.onOwnerResumed();
        }

        // Display a decoder crash notification if we've returned after a crash
        decoderCrashNotificationController.showIfNeeded();

        inForeground = true;
        hostPollingLifecycle.activate();
        startComputerUpdates();
        tryAutoReconnect();
    }

    private boolean refreshSettingsAfterResume() {
        if (settingsRepository == null) {
            return false;
        }
        AppPresentationSettings updatedPresentation =
                AndroidAppPresentationSettingsLoader.load(this);
        boolean presentationChanged =
                !updatedPresentation.equals(appPresentationSettings);
        if (appPresentationSettings != null &&
                hasStructuralPresentationChange(
                        appPresentationSettings,
                        updatedPresentation)) {
            recreate();
            return true;
        }
        appPresentationSettings = updatedPresentation;
        if (presentationChanged) {
            refreshApplicationScreen();
        }
        streamVideoSettingsState.replace(
                StreamVideoSettingsLoader.load(
                        settingsRepository,
                        AndroidDisplayAspectProvider.get(this)));
        streamAudioSettingsState.replace(
                StreamAudioSettingsLoader.load(settingsRepository));
        return false;
    }

    private static boolean hasStructuralPresentationChange(
            AppPresentationSettings previous,
            AppPresentationSettings current) {
        return previous.usesSmallAppIcons() !=
                        current.usesSmallAppIcons() ||
                previous.usesLightTheme() !=
                        current.usesLightTheme() ||
                !previous.getLanguage().equals(
                        current.getLanguage());
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
            NvApp app) {
        ComputerManagerService.ComputerManagerBinder binder =
                managerBinder;
        HostRuntimeSnapshot targetHost = hostSnapshot;
        if (binder == null || streamLauncher == null ||
                targetHost == null) {
            UiToast.makeText(
                    this,
                    R.string.error_manager_not_running,
                    UiToast.LENGTH_LONG).show();
            return;
        }

        AndroidStreamLauncher.Result result = streamLauncher.launch(
                targetHost,
                app,
                binder.getUniqueId());
        AndroidStreamLaunchFeedback.showIfNeeded(this, result);
    }

    private void showAppOptionsDialog(
            final AppObject app,
            final Bitmap artwork) {
        if (app == null || actionMenuPresenter == null) {
            return;
        }
        ArrayList<ActionMenuPresenter.Action> presentedActions =
                new ArrayList<>();
        for (MenuAction action : buildAppMenuActions(app, artwork)) {
            presentedActions.add(new ActionMenuPresenter.Action(
                    getText(action.labelResId),
                    action.iconResId,
                    action.runnable));
        }
        actionMenuPresenter.show(
                app.app.getAppName(),
                getText(app.isRunning
                        ? R.string.applist_menu_status_running
                        : R.string.applist_menu_status_available),
                presentedActions,
                null);
    }

    private ArrayList<MenuAction> buildAppMenuActions(
            final AppObject app,
            final Bitmap artwork) {
        ArrayList<MenuAction> actions = new ArrayList<>();

        if (lastRunningAppId != 0) {
            if (lastRunningAppId == app.app.getAppId()) {
                actions.add(new MenuAction(R.string.applist_menu_resume, R.drawable.ic_play, new Runnable() {
                    @Override
                    public void run() {
                        launchStream(app.app);
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
                        launchStream(app.app);
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

        if (canCreatePinnedShortcut(artwork)) {
            actions.add(new MenuAction(R.string.applist_menu_scut, R.drawable.ic_app_add, new Runnable() {
                @Override
                public void run() {
                    HostRuntimeSnapshot targetHost = hostSnapshot;
                    if (targetHost == null ||
                            !shortcutHelper.createPinnedGameShortcut(
                                    targetHost.getRecord().getIdentity(),
                                    app.app,
                                    artwork)) {
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
        HostRuntimeSnapshot targetHost = hostSnapshot;
        if (binder == null || controller == null ||
                targetHost == null) {
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
                            targetHost,
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
                launchStream(app.app);
            }
        });
    }

    private boolean canCreatePinnedShortcut(Bitmap artwork) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                artwork != null;
    }

    private void updateUiWithServerinfo(final int runningAppId) {
        AppView.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;

                    // Look through our current app list to tag the running app
                for (int i = 0; i < appGridAdapter.getCount(); i++) {
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                    // There can only be one or zero apps running.
                    if (existingApp.isRunning &&
                            existingApp.app.getAppId() == runningAppId) {
                        // This app was running and still is, so we're done now
                        return;
                    }
                    else if (existingApp.app.getAppId() == runningAppId) {
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
                    refreshApplicationScreen();
                }
            }
        });
    }

    private void updateUiWithAppList(final List<NvApp> appList) {
        AppView.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean updated = false;
                HostRuntimeSnapshot currentHost = hostSnapshot;
                HostIdentity shortcutHost =
                        currentHost == null
                        ? null
                        : currentHost.getRecord().getIdentity();

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
                        if (shortcutHost != null) {
                            shortcutHelper.enableAppShortcut(
                                    shortcutHost,
                                    app);
                        }

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
                        if (shortcutHost != null) {
                            shortcutHelper.disableAppShortcut(
                                    shortcutHost,
                                    existingApp.app,
                                    "App removed from PC");
                        }
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
                    refreshApplicationScreen();
                }
            }
        });
    }

    private void refreshApplicationScreen() {
        if (applicationScreenRenderer == null || appGridAdapter == null) {
            return;
        }
        ArrayList<AppObject> apps = new ArrayList<>();
        for (int index = 0; index < appGridAdapter.getCount(); index++) {
            apps.add((AppObject) appGridAdapter.getItem(index));
        }
        applicationScreenRenderer.updateApps(appGridAdapter, apps);
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
