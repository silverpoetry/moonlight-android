package com.limelight;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.HostPollingClientLifecycle;
import com.limelight.computers.LegacyHostRuntimeAdapter;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.stream.launch.StreamLaunchRequest;
import com.limelight.stream.launch.android.AndroidStreamLaunchIntentFactory;
import com.limelight.stream.launch.android.AndroidStreamLaunchContract;
import com.limelight.stream.launch.android.AndroidStreamLaunchRequestFactory;
import com.limelight.ui.hosts.HostServiceBindingController;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ShortcutTrampoline extends Activity {
    private String uuidString;
    private String requestedHostName;
    private String requestedAppName;
    private NvApp app;
    private final ArrayList<Intent> intentStack = new ArrayList<>();

    private final AtomicInteger wakeHostTries = new AtomicInteger(10);
    private ComputerDetails computer;
    private SpinnerDialog blockingLoadSpinner;

    private volatile ComputerManagerService.ComputerManagerBinder managerBinder;
    private final Object serviceLifecycleLock = new Object();
    private final HostPollingClientLifecycle hostPollingLifecycle =
            new HostPollingClientLifecycle();
    private HostServiceBindingController hostBindingController;
    private volatile boolean managerServiceBound;
    private volatile boolean activityDestroyed;

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
                            cancellation -> initializeServiceBinding(
                                    localBinder,
                                    cancellation),
                            ShortcutTrampoline.this::
                                    onServiceBindingInitialized);
            if (status != HostServiceBindingController.ConnectStatus.ACCEPTED) {
                LimeLog.severe(
                        "Unable to initialize shortcut service binding: " +
                                status);
                showConnectionFailure();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (hostBindingController != null) {
                hostBindingController.disconnect();
            }
            synchronized (serviceLifecycleLock) {
                managerBinder = null;
            }
            runCloseAction(hostPollingLifecycle.onConnectionLost());
        }
    };

    private BindingOutcome initializeServiceBinding(
            ComputerManagerService.ComputerManagerBinder localBinder,
            HostServiceBindingController.CancellationSignal
                    cancellation) throws Exception {
        if (!localBinder.waitForReady() || cancellation.isCanceled()) {
            return BindingOutcome.CANCELED;
        }

        HostRuntimeSnapshot loadedHost = uuidString != null
                ? localBinder.getHost(HostId.of(uuidString))
                : localBinder.getHostByName(requestedHostName);
        ComputerDetails loadedComputer = loadedHost == null
                ? null
                : LegacyHostRuntimeAdapter.toComputerDetails(loadedHost);
        if (loadedComputer == null) {
            return BindingOutcome.MISSING_HOST;
        }

        uuidString = loadedComputer.uuid;
        if (app == null && requestedAppName != null) {
            app = findCachedAppByName(
                    uuidString,
                    requestedAppName);
            if (app == null) {
                return BindingOutcome.INVALID_APP;
            }
        }
        if (cancellation.isCanceled()) {
            return BindingOutcome.CANCELED;
        }

        synchronized (serviceLifecycleLock) {
            if (activityDestroyed || cancellation.isCanceled()) {
                return BindingOutcome.CANCELED;
            }
            computer = loadedComputer;
            managerBinder = localBinder;
        }

        localBinder.invalidateHostState(
                loadedHost.getRecord().getIdentity().getId());
        HostPollingClientLifecycle.StartToken startToken =
                hostPollingLifecycle.beginStart();
        if (startToken == null || cancellation.isCanceled()) {
            hostPollingLifecycle.failStart(startToken);
            return BindingOutcome.CANCELED;
        }

        ComputerManagerService.HostPollingSubscription subscription;
        try {
            subscription = localBinder.startPolling(
                    snapshot -> handleComputerUpdate(
                            localBinder,
                            startToken,
                            LegacyHostRuntimeAdapter.toComputerDetails(
                                    snapshot)));
        }
        catch (RuntimeException | Error error) {
            hostPollingLifecycle.failStart(startToken);
            throw error;
        }
        hostPollingLifecycle.completeStart(
                startToken,
                subscription::close);
        return BindingOutcome.READY;
    }

    private void onServiceBindingInitialized(
            HostServiceBindingController.Result<BindingOutcome> result) {
        if (!result.isSuccessful()) {
            LimeLog.severe(
                    "Unable to initialize shortcut host polling: " +
                            result.getFailure().getClass().getSimpleName());
            showConnectionFailure();
            return;
        }
        BindingOutcome outcome = result.getValue();
        if (outcome == null) {
            return;
        }
        switch (outcome) {
            case READY:
            case CANCELED:
                return;
            case MISSING_HOST:
                showMissingComputer();
                return;
            case INVALID_APP:
                showInvalidApp();
                return;
            default:
                throw new AssertionError(
                        "Unhandled shortcut binding outcome: " +
                                outcome);
        }
    }

    private enum BindingOutcome {
        READY,
        MISSING_HOST,
        INVALID_APP,
        CANCELED
    }

    private void handleComputerUpdate(
            ComputerManagerService.ComputerManagerBinder localBinder,
            HostPollingClientLifecycle.StartToken startToken,
            ComputerDetails details) {
        if (!hostPollingLifecycle.owns(startToken) ||
                details.uuid == null ||
                !details.uuid.equalsIgnoreCase(uuidString)) {
            return;
        }

        if (details.state == ComputerDetails.State.OFFLINE &&
                details.macAddress != null &&
                wakeHostTries.getAndDecrement() > 0) {
            try {
                WakeOnLanSender.sendWolPacket(computer);
                if (hostPollingLifecycle.owns(startToken)) {
                    localBinder.invalidateHostState(
                            HostId.of(computer.uuid));
                }
                return;
            }
            catch (IOException error) {
                // Fall through to the terminal offline result when no Wake-on-
                // LAN packet could be sent.
                error.printStackTrace();
            }
        }

        if (details.state != ComputerDetails.State.UNKNOWN) {
            runOnUiThread(() -> handleTerminalComputerState(
                    localBinder,
                    startToken,
                    details));
        }
    }

    private void handleTerminalComputerState(
            ComputerManagerService.ComputerManagerBinder localBinder,
            HostPollingClientLifecycle.StartToken startToken,
            ComputerDetails details) {
        if (!hostPollingLifecycle.owns(startToken) ||
                managerBinder != localBinder ||
                activityDestroyed) {
            return;
        }

        dismissBlockingSpinner();
        if (details.state == ComputerDetails.State.ONLINE &&
                details.pairState == PairingManager.PairState.PAIRED) {
            launchRequestedTarget(details, localBinder);
        }
        else if (details.state == ComputerDetails.State.OFFLINE) {
            Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.error_pc_offline),
                    true);
        }
        else if (details.pairState != PairingManager.PairState.PAIRED) {
            Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_not_paired),
                    true);
        }
        releaseServiceConnection();
    }

    private void launchRequestedTarget(
            ComputerDetails details,
            ComputerManagerService.ComputerManagerBinder localBinder) {
        if (app != null) {
            if (details.runningGameId == 0 ||
                    details.runningGameId == app.getAppId()) {
                Intent streamIntent = createStreamIntent(
                        details,
                        app,
                        localBinder);
                if (streamIntent == null) {
                    return;
                }
                intentStack.add(streamIntent);
                finish();
                startActivities(intentStack.toArray(new Intent[0]));
                return;
            }

            Intent startIntent = createStreamIntent(
                    details,
                    app,
                    localBinder);
            if (startIntent == null) {
                return;
            }
            UiHelper.displayQuitConfirmationDialog(
                    this,
                    () -> {
                        intentStack.add(startIntent);
                        finish();
                        startActivities(intentStack.toArray(
                                new Intent[0]));
                    },
                    this::finish);
            return;
        }

        Intent runningStreamIntent = null;
        if (details.runningGameId != 0) {
            runningStreamIntent = createStreamIntent(
                    details,
                    new NvApp(
                            null,
                            details.runningGameId,
                            false),
                    localBinder);
            if (runningStreamIntent == null) {
                return;
            }
        }

        Intent pcIntent = new Intent(this, PcView.class);
        pcIntent.setAction(Intent.ACTION_MAIN);
        pcIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        intentStack.add(pcIntent);

        Intent appIntent = new Intent(getIntent())
                .putExtra(AppView.UUID_EXTRA, details.uuid);
        appIntent.setClass(this, AppView.class);
        intentStack.add(appIntent);
        if (runningStreamIntent != null) {
            intentStack.add(runningStreamIntent);
        }
        finish();
        startActivities(intentStack.toArray(new Intent[0]));
    }

    private Intent createStreamIntent(
            ComputerDetails details,
            NvApp targetApp,
            ComputerManagerService.ComputerManagerBinder binder) {
        try {
            StreamLaunchRequest request =
                    AndroidStreamLaunchRequestFactory.create(
                            details,
                            targetApp,
                            binder.getUniqueId());
            return AndroidStreamLaunchIntentFactory.create(
                    this,
                    request);
        }
        catch (CertificateEncodingException |
                IllegalArgumentException failure) {
            LimeLog.warning(
                    "Invalid shortcut stream launch: " +
                            failure.getClass().getSimpleName());
            showConnectionFailure();
            return null;
        }
    }

    private void showMissingComputer() {
        dismissBlockingSpinner();
        Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.scut_pc_not_found),
                true);
        releaseServiceConnection();
    }

    private void showConnectionFailure() {
        dismissBlockingSpinner();
        Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.conn_error_msg),
                true);
        releaseServiceConnection();
    }

    private void showInvalidApp() {
        dismissBlockingSpinner();
        Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.scut_invalid_app_id),
                true);
        releaseServiceConnection();
    }

    private NvApp findCachedAppByName(
            String hostId,
            String appName) {
        try {
            String rawAppList = CacheHelper.readInputStreamToString(
                    CacheHelper.openCacheFileForInput(
                            getCacheDir(),
                            "applist",
                            hostId));
            if (rawAppList.isEmpty()) {
                return null;
            }
            for (NvApp candidate : NvHTTP.getAppListByReader(
                    new StringReader(rawAppList))) {
                if (candidate.getAppName().equals(appName)) {
                    return candidate;
                }
            }
        }
        catch (IOException | XmlPullParserException error) {
            return null;
        }
        return null;
    }

    protected boolean validateInput(String uuidString, String appIdString, String nameString) {
        // Validate PC UUID/Name
        if (uuidString == null && nameString == null) {
            Dialog.displayDialog(ShortcutTrampoline.this,
                    getResources().getString(R.string.conn_error_title),
                    getResources().getString(R.string.scut_invalid_uuid),
                    true);
            return false;
        }

        if (uuidString != null && !uuidString.isEmpty()) {
            try {
                UUID.fromString(uuidString);
            } catch (IllegalArgumentException ex) {
                Dialog.displayDialog(ShortcutTrampoline.this,
                        getResources().getString(R.string.conn_error_title),
                        getResources().getString(R.string.scut_invalid_uuid),
                        true);
                return false;
            }
        } else {
            // UUID is null, so fallback to Name
            if (nameString == null || nameString.isEmpty()) {
                Dialog.displayDialog(ShortcutTrampoline.this,
                        getResources().getString(R.string.conn_error_title),
                        getResources().getString(R.string.scut_invalid_uuid),
                        true);
                return false;
            }
        }

        // Validate App ID (if provided)
        if (appIdString != null && !appIdString.isEmpty()) {
            try {
                Integer.parseInt(appIdString);
            } catch (NumberFormatException ex) {
                Dialog.displayDialog(ShortcutTrampoline.this,
                        getResources().getString(R.string.conn_error_title),
                        getResources().getString(R.string.scut_invalid_app_id),
                        true);
                return false;
            }
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hostPollingLifecycle.activate();
        hostBindingController = HostServiceBindingController.create(
                this::runOnUiThread);

        UiHelper.notifyNewRootView(this);
        // PC arguments, both are optional, but at least one must be provided
        uuidString = getIntent().getStringExtra(AppView.UUID_EXTRA);
        requestedHostName =
                getIntent().getStringExtra(AppView.NAME_EXTRA);

        // App arguments, both are optional, but one must be provided in order to start an app
        String appIdString = getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_APP_ID);
        String appNameString = getIntent().getStringExtra(
                AndroidStreamLaunchContract.EXTRA_APP_NAME);

        if (!validateInput(
                uuidString,
                appIdString,
                requestedHostName)) {
            // Invalid input, so just return
            return;
        }

        if (uuidString != null && uuidString.isEmpty()) {
            uuidString = null;
        }

        if (appIdString != null && !appIdString.isEmpty()) {
            app = new NvApp(getIntent().getStringExtra(
                    AndroidStreamLaunchContract.EXTRA_APP_NAME),
                    Integer.parseInt(appIdString),
                    getIntent().getBooleanExtra(
                            AndroidStreamLaunchContract.EXTRA_APP_HDR,
                            false));
        }
        else if (appNameString != null && !appNameString.isEmpty()) {
            requestedAppName = appNameString;
        }

        // Bind to the computer manager service
        managerServiceBound = bindService(
                new Intent(this, ComputerManagerService.class),
                serviceConnection,
                Service.BIND_AUTO_CREATE);
        if (!managerServiceBound) {
            Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.conn_error_msg),
                    true);
            return;
        }

        blockingLoadSpinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.conn_establishing_title),
                getResources().getString(R.string.applist_connect_msg), true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        dismissBlockingSpinner();

        Dialog.closeDialogs();
        releaseServiceConnection();
        finish();
    }

    @Override
    protected void onDestroy() {
        releaseServiceConnection();
        super.onDestroy();
    }

    private void dismissBlockingSpinner() {
        if (blockingLoadSpinner != null) {
            blockingLoadSpinner.dismiss();
            blockingLoadSpinner = null;
        }
    }

    private void releaseServiceConnection() {
        Runnable closeAction;
        boolean shouldUnbind;
        HostServiceBindingController bindingController;
        synchronized (serviceLifecycleLock) {
            if (activityDestroyed) {
                return;
            }
            activityDestroyed = true;
            managerBinder = null;
            bindingController = hostBindingController;
            hostBindingController = null;
            shouldUnbind = managerServiceBound;
            managerServiceBound = false;
            closeAction = hostPollingLifecycle.destroy();
        }
        if (bindingController != null) {
            bindingController.destroy();
        }
        runCloseAction(closeAction);
        if (shouldUnbind) {
            unbindService(serviceConnection);
        }
    }

    private static void runCloseAction(Runnable closeAction) {
        if (closeAction != null) {
            closeAction.run();
        }
    }
}
