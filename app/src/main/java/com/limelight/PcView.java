package com.limelight;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.net.UnknownHostException;

import com.limelight.binding.PlatformBinding;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.HostPollingClientLifecycle;
import com.limelight.computers.model.HostId;
import com.limelight.computers.pairing.HostPairingUseCase;
import com.limelight.computers.pairing.NvHttpPairingBackend;
import com.limelight.grid.PcGridAdapter;
import com.limelight.grid.assets.DiskAssetLoader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.http.PairingManager.PairState;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.preferences.AddComputerManually;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.StreamSettings;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader;
import com.limelight.settings.app.AppPresentationSettings;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.ui.hosts.ScreenBackgroundPresenter;
import com.limelight.ui.hosts.HostPairingController;
import com.limelight.utils.AutoReconnectHelper;
import com.limelight.utils.DeviceUtils;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.UiHelper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import org.xmlpull.v1.XmlPullParserException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.axi.gamepad.an.AxiGamePadIndexActivity;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

public class PcView extends Activity implements AdapterFragmentCallbacks {
    private View noPcFoundLayout;
    private PcGridAdapter pcGridAdapter;
    private ShortcutHelper shortcutHelper;
    private volatile ComputerManagerService.ComputerManagerBinder
            managerBinder;
    private final Object managerBindingLock = new Object();
    private volatile boolean freezeUpdates;
    private volatile boolean inForeground;
    private boolean completeOnCreateCalled;
    private boolean hostListReady, managerHasKnownHosts;
    private boolean managerServiceBound;
    private volatile boolean activityDestroyed;
    private final HostPairingUseCase hostPairingUseCase =
            new HostPairingUseCase();
    private HostPairingController hostPairingController;
    private final HostPollingClientLifecycle hostPollingLifecycle =
            new HostPollingClientLifecycle();
    private volatile long managerBindingGeneration;
    private ComputerObject pendingHostMenuComputer;
    private android.app.AlertDialog pendingHostMenuDialog;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);
            final long bindingGeneration;
            synchronized (managerBindingLock) {
                bindingGeneration = ++managerBindingGeneration;
            }

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    if (!localBinder.waitForReady()) {
                        return;
                    }

                    synchronized (managerBindingLock) {
                        if (activityDestroyed ||
                                bindingGeneration !=
                                        managerBindingGeneration) {
                            return;
                        }
                        managerBinder = localBinder;
                    }
                    startComputerUpdates();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!activityDestroyed &&
                                    bindingGeneration ==
                                            managerBindingGeneration) {
                                tryAutoReconnect();
                            }
                        }
                    });

                    // Force a keypair to be generated early to avoid discovery delays
                    PlatformBinding.getCryptoProvider(PcView.this).getClientCertificate();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            synchronized (managerBindingLock) {
                managerBindingGeneration++;
                managerBinder = null;
                freezeUpdates = true;
            }
            runCloseAction(hostPollingLifecycle.onConnectionLost());
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Only reinitialize views if completeOnCreate() was called
        // before this callback. If it was not, completeOnCreate() will
        // handle initializing views with the config change accounted for.
        // This is not prone to races because both callbacks are invoked
        // in the main thread.
        if (completeOnCreateCalled) {
            // Reinitialize views just in case orientation changed
            initializeViews();
        }
    }

    private void initializeViews() {
        setContentView(R.layout.activity_pc_view_new);
        UiHelper.notifyNewEdgeToEdgeRootView(
                this, R.id.rv_top_view, R.id.pcFragmentContainer);
        // Allow floating expanded PiP overlays while browsing PCs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }
        ImageView imageView=findViewById(R.id.iv_root_view);

        AppPresentationSettings presentationSettings =
                AndroidAppPresentationSettingsLoader.load(this);
        ScreenBackgroundPresenter.apply(
                this,
                imageView,
                presentationSettings);
        TextView tx_label=findViewById(R.id.tx_label);
        if (!presentationSettings.getHostListLabel().isEmpty()) {
            tx_label.setText(
                    presentationSettings.getHostListLabel());
        }
        // Setup the list view
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        ImageButton addComputerButton = findViewById(R.id.manuallyAddPc);
        ImageButton helpButton = findViewById(R.id.helpButton);
        ImageButton axButton = findViewById(R.id.axiButton);
        axButton.setVisibility(View.VISIBLE);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PcView.this, StreamSettings.class));
            }
        });
        addComputerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PcView.this, AddComputerManually.class);
                startActivity(i);
            }
        });
        helpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PcView.this,AboutActivity.class));
//                HelpLauncher.launchSetupGuide(PcView.this);
            }
        });

        // Amazon review didn't like the help button because the wiki was not entirely
        // navigable via the Fire TV remote (though the relevant parts were). Let's hide
        // it on Fire TV.
        if (getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
            helpButton.setVisibility(View.GONE);
        }

        axButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(PcView.this, AxiGamePadIndexActivity.class);
                startActivity(i);
            }
        });

        getFragmentManager().beginTransaction()
            .replace(R.id.pcFragmentContainer, new AdapterFragment())
            .commitAllowingStateLoss();

        noPcFoundLayout = findViewById(R.id.no_pc_found_layout);
        updateNoPcFoundVisibility();
        pcGridAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hostPairingController = HostPairingController.create(
                this::runOnUiThread);

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;
        hostPollingLifecycle.activate();

        // Create a GLSurfaceView to fetch GLRenderer unless we have
        // a cached result already.
        final GlPreferences glPrefs = GlPreferences.readPreferences(this);
        if (!glPrefs.savedFingerprint.equals(Build.FINGERPRINT) || glPrefs.glRenderer.isEmpty()) {
            GLSurfaceView surfaceView = new GLSurfaceView(this);
            surfaceView.setRenderer(new GLSurfaceView.Renderer() {
                @Override
                public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                    // Save the GLRenderer string so we don't need to do this next time
                    glPrefs.glRenderer = gl10.glGetString(GL10.GL_RENDERER);
                    glPrefs.savedFingerprint = Build.FINGERPRINT;
                    glPrefs.writePreferences();

                    LimeLog.info("Fetched GL Renderer: " + glPrefs.glRenderer);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            completeOnCreate();
                        }
                    });
                }

                @Override
                public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                }

                @Override
                public void onDrawFrame(GL10 gl10) {
                }
            });
            setContentView(surfaceView);
        }
        else {
            LimeLog.info("Cached GL Renderer: " + glPrefs.glRenderer);
            completeOnCreate();
        }
    }

    private void completeOnCreate() {
        completeOnCreateCalled = true;

        shortcutHelper = new ShortcutHelper(this);

        AndroidAppLocale.apply(this);

        // Bind to the computer manager service
        managerServiceBound = bindService(
                new Intent(PcView.this, ComputerManagerService.class),
                serviceConnection,
                Service.BIND_AUTO_CREATE);

        pcGridAdapter = new PcGridAdapter(this);

        initializeViews();

    }

    private void startComputerUpdates() {
        ComputerManagerService.ComputerManagerBinder binder;
        synchronized (managerBindingLock) {
            if (managerBinder == null || !inForeground) {
                return;
            }
            binder = managerBinder;
        }
        HostPollingClientLifecycle.StartToken startToken =
                hostPollingLifecycle.beginStart();
        if (startToken == null) {
            return;
        }
        synchronized (managerBindingLock) {
            if (managerBinder != binder || !inForeground) {
                hostPollingLifecycle.failStart(startToken);
                return;
            }
            freezeUpdates = false;
            managerHasKnownHosts = binder.getComputerCount() > 0;
            hostListReady = true;
        }
        updateNoPcFoundVisibilityOnUiThread();

        ComputerManagerService.HostPollingSubscription newSubscription;
        try {
            newSubscription = binder.startPolling(
                    new ComputerManagerListener() {
                    @Override
                    public void notifyComputerUpdated(
                            final ComputerDetails details) {
                        if (hostPollingLifecycle.owns(startToken) &&
                                !freezeUpdates) {
                            PcView.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (hostPollingLifecycle.owns(
                                            startToken)) {
                                        updateComputer(details);
                                    }
                                }
                            });

                            // Add a launcher shortcut for this PC (off the main thread to prevent ANRs)
                            if (details.pairState == PairState.PAIRED) {
                                shortcutHelper.createAppViewShortcutForOnlineHost(
                                        details);
                            }

                            PcView.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (hostPollingLifecycle.owns(
                                            startToken)) {
                                        tryAutoReconnect();
                                    }
                                }
                            });
                        }
                    }
                    });
        }
        catch (RuntimeException | Error error) {
            hostPollingLifecycle.failStart(startToken);
            freezeUpdates = true;
            throw error;
        }

        hostPollingLifecycle.completeStart(
                startToken,
                newSubscription::close);
    }

    private void stopComputerUpdates(boolean wait) {
        ComputerManagerService.ComputerManagerBinder binder;
        synchronized (managerBindingLock) {
            freezeUpdates = true;
            binder = managerBinder;
        }

        runCloseAction(hostPollingLifecycle.deactivate());
        if (wait && binder != null) {
            binder.waitForPollingStopped();
        }
    }

    @Override
    public void onDestroy() {
        activityDestroyed = true;
        runCloseAction(hostPollingLifecycle.destroy());
        if (hostPairingController != null) {
            hostPairingController.destroy();
            hostPairingController = null;
        }
        if (managerServiceBound) {
            unbindService(serviceConnection);
            managerServiceBound = false;
        }
        synchronized (managerBindingLock) {
            managerBindingGeneration++;
            managerBinder = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        inForeground = false;
        stopComputerUpdates(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Dialog.closeDialogs();
    }

    private void tryAutoReconnect() {
        if (!inForeground || managerBinder == null) {
            return;
        }

        AutoReconnectHelper.maybeResumeStream(this, managerBinder, null);
    }

    private void doPair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.pair_pc_offline), UiToast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), UiToast.LENGTH_LONG).show();
            return;
        }

        final ComputerManagerService.ComputerManagerBinder binder =
                managerBinder;
        HostPairingController.RequestStatus requestStatus =
                hostPairingController.request(
                        cancellation -> performPairing(
                                computer,
                                binder,
                                cancellation),
                        result -> handlePairingResult(
                                computer,
                                result));
        if (requestStatus ==
                HostPairingController.RequestStatus.ACCEPTED) {
            UiToast.makeText(
                    this,
                    R.string.pairing,
                    UiToast.LENGTH_SHORT).show();
        }
        else if (requestStatus == HostPairingController.RequestStatus
                .ALREADY_RUNNING) {
            UiToast.makeText(
                    this,
                    R.string.pair_already_in_progress,
                    UiToast.LENGTH_LONG).show();
        }
        else if (requestStatus ==
                HostPairingController.RequestStatus.UNAVAILABLE) {
            UiToast.makeText(
                    this,
                    R.string.pair_fail,
                    UiToast.LENGTH_LONG).show();
        }
    }

    private HostPairingUseCase.Outcome performPairing(
            ComputerDetails computer,
            ComputerManagerService.ComputerManagerBinder binder,
            HostPairingUseCase.CancellationSignal cancellation)
            throws Exception {
        // This executes only after the controller has admitted the request,
        // so a duplicate click cannot stop polling for the active operation.
        stopComputerUpdates(true);

        HostId hostId = HostId.of(computer.uuid);
        try (ComputerManagerService.HostCredentialWriteSession
                     credentialSession =
                     binder.openHostCredentialWriteSession(hostId)) {
            if (credentialSession == null) {
                throw new IOException(
                        "Host repository is unavailable for pairing");
            }

            NvHTTP http = new NvHTTP(
                    ServerHelper.getCurrentAddressFromComputer(computer),
                    computer.httpsPort,
                    binder.getUniqueId(),
                    computer.serverCert,
                    PlatformBinding.getCryptoProvider(this));
            http.setClientName(
                    DeviceUtils.getManufacturer() + "-" +
                            DeviceUtils.getModel());
            return hostPairingUseCase.execute(
                    hostId,
                    computer.runningGameId != 0,
                    new NvHttpPairingBackend(http),
                    pin -> Dialog.displayDialog(
                            this,
                            getString(R.string.pair_pairing_title),
                            getString(R.string.pair_pairing_msg) + " " +
                                    pin + "\n\n" +
                                    getString(R.string.pair_pairing_help),
                            false),
                    (persistedHostId, certificate) -> {
                        if (!hostId.equals(persistedHostId) ||
                                !credentialSession.updatePinnedCertificate(
                                        certificate)) {
                            throw new IOException(
                                    "Unable to persist paired host certificate");
                        }
                    },
                    binder::invalidateStateForComputer,
                    cancellation);
        }
    }

    private static void runCloseAction(Runnable closeAction) {
        if (closeAction != null) {
            closeAction.run();
        }
    }

    private void handlePairingResult(
            ComputerDetails computer,
            HostPairingController.Result result) {
        Dialog.closeDialogs();
        if (result.isSuccessful()) {
            // Preserve the legacy pairing-entry contract for both a newly
            // completed pair and a host that reports it was already paired.
            // AppView must perform its post-pair refresh because PcView's
            // observed pair state may still be stale in either case.
            doAppList(computer, true, false);
            return;
        }

        CharSequence message = getPairingFailureMessage(result);
        if (message != null) {
            UiToast.makeText(
                    this,
                    message,
                    UiToast.LENGTH_LONG).show();
        }
        startComputerUpdates();
    }

    private CharSequence getPairingFailureMessage(
            HostPairingController.Result result) {
        Exception failure = result.getFailure();
        if (failure instanceof UnknownHostException) {
            return getString(R.string.error_unknown_host);
        }
        if (failure instanceof FileNotFoundException) {
            return getString(R.string.error_404);
        }
        if (failure != null) {
            LimeLog.warning(
                    "Host pairing failed: " +
                            failure.getClass().getSimpleName());
            return getString(R.string.pair_fail);
        }

        switch (result.getOutcome()) {
            case PIN_WRONG:
                return getString(R.string.pair_incorrect_pin);
            case HOST_IN_GAME:
                return getString(R.string.pair_pc_ingame);
            case ALREADY_IN_PROGRESS:
                return getString(R.string.pair_already_in_progress);
            case FAILED:
                return getString(R.string.pair_fail);
            case CANCELED:
                return null;
            default:
                throw new AssertionError(
                        "Unexpected pairing outcome: " +
                                result.getOutcome());
        }
    }

    private void doWakeOnLan(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.ONLINE) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.wol_pc_online), UiToast.LENGTH_SHORT).show();
            return;
        }

        if (computer.macAddress == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.wol_no_mac), UiToast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    WakeOnLanSender.sendWolPacket(computer);
                    message = getResources().getString(R.string.wol_waking_msg);
                } catch (IOException e) {
                    message = getResources().getString(R.string.wol_fail);
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UiToast.makeText(PcView.this, toastMessage, UiToast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doUnpair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), UiToast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), UiToast.LENGTH_LONG).show();
            return;
        }

        UiToast.makeText(PcView.this, getResources().getString(R.string.unpairing), UiToast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                try {
                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(PcView.this));
                    httpConn.setClientName(DeviceUtils.getManufacturer()+"-"+DeviceUtils.getModel());
                    if (httpConn.getPairState() == PairingManager.PairState.PAIRED) {
                        httpConn.unpair();
                        if (httpConn.getPairState() == PairingManager.PairState.NOT_PAIRED) {
                            message = getResources().getString(R.string.unpair_success);
                        }
                        else {
                            message = getResources().getString(R.string.unpair_fail);
                        }
                    }
                    else {
                        message = getResources().getString(R.string.unpair_error);
                    }
                } catch (UnknownHostException e) {
                    message = getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    message = e.getMessage();
                    e.printStackTrace();
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UiToast.makeText(PcView.this, toastMessage, UiToast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doAppList(ComputerDetails computer, boolean newlyPaired, boolean showHiddenGames) {
        if (computer.state == ComputerDetails.State.OFFLINE) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), UiToast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), UiToast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(this, AppView.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);
        i.putExtra(AppView.UUID_EXTRA, computer.uuid);
        i.putExtra(AppView.NEW_PAIR_EXTRA, newlyPaired);
        i.putExtra(AppView.SHOW_HIDDEN_APPS_EXTRA, showHiddenGames);
        startActivity(i);
    }

    private void doRecentSession(ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.state == ComputerDetails.State.UNKNOWN) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), UiToast.LENGTH_SHORT).show();
            return;
        }
        if (computer.pairState != PairState.PAIRED) {
            doPair(computer);
            return;
        }
        if (managerBinder == null) {
            UiToast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), UiToast.LENGTH_LONG).show();
            return;
        }

        if (computer.runningGameId != 0) {
            ServerHelper.doStart(this, new NvApp("app", computer.runningGameId, false), computer, managerBinder);
            return;
        }

        NvApp recentApp = ServerHelper.getRecentSession(this, computer);
        if (recentApp != null) {
            ServerHelper.doStart(this, recentApp, computer, managerBinder);
        }
        else {
            doAppList(computer, false, false);
        }
    }

    private void openPcContextMenu(int position) {
        pendingHostMenuComputer = (ComputerObject) pcGridAdapter.getItem(position);
        showHostOptionsDialog();
    }

    private void showHostOptionsDialog() {
        final ComputerObject computer = pendingHostMenuComputer;
        if (computer == null) {
            return;
        }

        stopComputerUpdates(false);

        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_host_options, null, false);
        final TextView titleView = dialogView.findViewById(R.id.tv_host_menu_title);
        final TextView statusView = dialogView.findViewById(R.id.tv_host_menu_status);
        final LinearLayout actionList = dialogView.findViewById(R.id.layout_host_menu_actions);
        final TextView cancelButton = dialogView.findViewById(R.id.btn_host_menu_cancel);

        titleView.setText(computer.details.name);
        statusView.setText(getResources().getString(computer.details.state == ComputerDetails.State.ONLINE
                ? R.string.pcview_menu_header_online
                : computer.details.state == ComputerDetails.State.OFFLINE
                ? R.string.pcview_menu_header_offline
                : R.string.pcview_menu_header_unknown));

        final ArrayList<MenuAction> actions = buildHostMenuActions(computer);
        for (int i = 0; i < actions.size(); i++) {
            View item = createHostOptionView(actionList, actions.get(i));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                itemParams.topMargin = UiHelper.dpToPx(this, 6);
            }
            actionList.addView(item, itemParams);
        }

        pendingHostMenuDialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        pendingHostMenuDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface dialog) {
                pendingHostMenuDialog = null;
                pendingHostMenuComputer = null;
                startComputerUpdates();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingHostMenuDialog != null) {
                    pendingHostMenuDialog.dismiss();
                }
            }
        });
        pendingHostMenuDialog.show();
        if (pendingHostMenuDialog.getWindow() != null) {
            pendingHostMenuDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private View createHostOptionView(
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
                if (pendingHostMenuDialog != null) {
                    pendingHostMenuDialog.dismiss();
                }
                if (action.runnable != null) {
                    action.runnable.run();
                }
            }
        });
        return item;
    }

    private ArrayList<MenuAction> buildHostMenuActions(final ComputerObject computer) {
        ArrayList<MenuAction> actions = new ArrayList<>();
        if (computer.details.state == ComputerDetails.State.OFFLINE ||
                computer.details.state == ComputerDetails.State.UNKNOWN) {
            actions.add(new MenuAction(R.string.pcview_menu_send_wol, R.drawable.ic_axi_sleep, new Runnable() {
                @Override
                public void run() {
                    doWakeOnLan(computer.details);
                }
            }));
            actions.add(new MenuAction(R.string.pcview_menu_eol, R.drawable.ic_axi_app_about, new Runnable() {
                @Override
                public void run() {
                    HelpLauncher.launchGameStreamEolFaq(PcView.this);
                }
            }));
        }
        else if (computer.details.pairState != PairState.PAIRED) {
            actions.add(new MenuAction(R.string.pcview_menu_pair_pc, R.drawable.ic_axi_app_add, new Runnable() {
                @Override
                public void run() {
                    doPair(computer.details);
                }
            }));
            if (computer.details.nvidiaServer) {
                actions.add(new MenuAction(R.string.pcview_menu_eol, R.drawable.ic_axi_app_about, new Runnable() {
                    @Override
                    public void run() {
                        HelpLauncher.launchGameStreamEolFaq(PcView.this);
                    }
                }));
            }
        }
        else {
            if (computer.details.runningGameId != 0) {
                final NvApp runningApp = new NvApp("app", computer.details.runningGameId, false);
                actions.add(new MenuAction(R.string.applist_menu_resume, R.drawable.ic_play, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder != null) {
                            ServerHelper.doStart(PcView.this, runningApp, computer.details, managerBinder);
                        }
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_restart, R.drawable.ic_axi_reboot, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder != null) {
                            ServerHelper.doQuit(PcView.this, computer.details, runningApp, managerBinder, new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ServerHelper.doStart(PcView.this, runningApp, computer.details, managerBinder);
                                        }
                                    });
                                }
                            });
                        }
                    }
                }));
                actions.add(new MenuAction(R.string.applist_menu_quit, R.drawable.ic_axi_exit, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder != null) {
                            ServerHelper.doQuit(PcView.this, computer.details, runningApp, managerBinder, null);
                        }
                    }
                }));
            }

            if (computer.details.nvidiaServer) {
                actions.add(new MenuAction(R.string.pcview_menu_eol, R.drawable.ic_axi_app_about, new Runnable() {
                    @Override
                    public void run() {
                        HelpLauncher.launchGameStreamEolFaq(PcView.this);
                    }
                }));
            }

            actions.add(new MenuAction(R.string.pcview_menu_app_list, R.drawable.ic_menu_grid, new Runnable() {
                @Override
                public void run() {
                    doAppList(computer.details, false, true);
                }
            }));
        }

        actions.add(new MenuAction(R.string.pcview_menu_test_network, R.drawable.ic_axi_performance, new Runnable() {
            @Override
            public void run() {
                ServerHelper.doNetworkTest(PcView.this);
            }
        }));
        actions.add(new MenuAction(R.string.pcview_menu_delete_pc, R.drawable.ic_axi_delete, new Runnable() {
            @Override
            public void run() {
                if (ActivityManager.isUserAMonkey()) {
                    LimeLog.info("Ignoring delete PC request from monkey");
                    return;
                }
                UiHelper.displayDeletePcConfirmationDialog(PcView.this, computer.details, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder == null) {
                            UiToast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), UiToast.LENGTH_LONG).show();
                            return;
                        }
                        removeComputer(computer.details);
                    }
                }, null);
            }
        }));
        actions.add(new MenuAction(R.string.pcview_menu_details, R.drawable.ic_axi_app_about, new Runnable() {
            @Override
            public void run() {
                Dialog.displayDialog(PcView.this, getResources().getString(R.string.title_details), computer.details.toString(), false);
            }
        }));
        return actions;
    }

    private static final class MenuAction {
        public final int labelResId;
        public final int iconResId;
        public final Runnable runnable;

        MenuAction(int labelResId, int iconResId, Runnable runnable) {
            this.labelResId = labelResId;
            this.iconResId = iconResId;
            this.runnable = runnable;
        }
    }

    private void performPcDefaultAction(AbsListView listView, View targetView, int position, long id, ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.UNKNOWN ||
            computer.state == ComputerDetails.State.OFFLINE) {
            openPcContextMenu(position);
        } else if (computer.pairState != PairState.PAIRED) {
            doPair(computer);
        } else {
            doAppList(computer, false, false);
        }
    }

    private void removeComputer(ComputerDetails details) {
        managerBinder.removeComputer(details);

        new DiskAssetLoader(this).deleteAssetsForComputer(details.uuid);

        // Delete hidden games preference value
        getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .remove(details.uuid)
                .apply();

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            if (details.equals(computer.details)) {
                // Disable or delete shortcuts referencing this PC
                shortcutHelper.disableComputerShortcut(details,
                        getResources().getString(R.string.scut_deleted_pc));

                pcGridAdapter.removeComputer(computer);
                pcGridAdapter.notifyDataSetChanged();

                if (pcGridAdapter.getCount() == 0) {
                    managerHasKnownHosts = false;
                    updateNoPcFoundVisibility();
                }

                break;
            }
        }
    }

    private void updateComputer(ComputerDetails details) {
        ComputerObject existingEntry = null;
        managerHasKnownHosts = true;
        hostListReady = true;

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            // Check if this is the same computer
            if (details.uuid.equals(computer.details.uuid)) {
                existingEntry = computer;
                break;
            }
        }

        if (existingEntry != null) {
            // Replace the information in the existing entry
            existingEntry.details = details;
        }
        else {
            // Add a new entry
            pcGridAdapter.addComputer(new ComputerObject(details));
        }

        // Notify the view that the data has changed
        pcGridAdapter.notifyDataSetChanged();
        updateNoPcFoundVisibility();
    }

    private void updateNoPcFoundVisibilityOnUiThread() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateNoPcFoundVisibility();
            }
        });
    }

    private void updateNoPcFoundVisibility() {
        if (noPcFoundLayout == null) {
            return;
        }

        noPcFoundLayout.setVisibility(hostListReady && !managerHasKnownHosts &&
                pcGridAdapter.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getAdapterFragmentLayoutId() {
        return R.layout.pc_grid_view_new;
    }

    @Override
    public void receiveAbsListView(AbsListView listView) {
        listView.setAdapter(pcGridAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long id) {
                ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(pos);
                performPcDefaultAction(listView, arg1, pos, id, computer.details);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(position);
                doRecentSession(computer.details);
                return true;
            }
        });
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean handled = handlePcItemMenuTouch(listView, event);
                if (handled && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return handled;
            }
        });
        UiHelper.applyStatusBarPadding(listView);
    }

    private boolean handlePcItemMenuTouch(AbsListView listView, MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return false;
        }

        int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
        if (position == AdapterView.INVALID_POSITION) {
            return false;
        }

        View itemView = listView.getChildAt(position - listView.getFirstVisiblePosition());
        if (itemView == null) {
            return false;
        }

        View menuButton = itemView.findViewById(R.id.pc_item_menu_button);
        if (menuButton == null || !isTouchInsideChild(listView, menuButton, event)) {
            return false;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            menuButton.setAlpha(0.65f);
            return true;
        }

        menuButton.setAlpha(1.0f);
        if (action == MotionEvent.ACTION_UP) {
            menuButton.performClick();
            openPcContextMenu(position);
        }
        return true;
    }

    private static boolean isTouchInsideChild(AbsListView listView, View child, MotionEvent event) {
        int[] listLocation = new int[2];
        int[] childLocation = new int[2];
        listView.getLocationOnScreen(listLocation);
        child.getLocationOnScreen(childLocation);

        float rawX = listLocation[0] + event.getX();
        float rawY = listLocation[1] + event.getY();
        return rawX >= childLocation[0] && rawX < childLocation[0] + child.getWidth() &&
                rawY >= childLocation[1] && rawY < childLocation[1] + child.getHeight();
    }

    public static class ComputerObject {
        public ComputerDetails details;

        public ComputerObject(ComputerDetails details) {
            if (details == null) {
                throw new IllegalArgumentException("details must not be null");
            }
            this.details = details;
        }

        @Override
        public String toString() {
            return details.name;
        }
    }

}
