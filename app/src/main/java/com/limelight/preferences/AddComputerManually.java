package com.limelight.preferences;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.BaseActivity;
import com.limelight.SrvResolver;
import com.limelight.computers.ComputerManagerService;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.ManualHostEndpointParser;
import com.limelight.computers.reachability.Ipv4SubnetMatcher;
import com.limelight.computers.reachability.ClientConnectivityEndpoint;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.ui.hosts.HostUiOperationController;
import com.limelight.utils.Dialog;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/** Android presentation adapter for manual host admission. */
public class AddComputerManually extends BaseActivity {
    private static final class AddResult {
        private final boolean successful;
        private final boolean wrongSiteLocalAddress;
        private final int portTestResult;

        private AddResult(
                boolean successful,
                boolean wrongSiteLocalAddress,
                int portTestResult) {
            this.successful = successful;
            this.wrongSiteLocalAddress = wrongSiteLocalAddress;
            this.portTestResult = portTestResult;
        }
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView hostText;
    private volatile ComputerManagerService.ComputerManagerBinder managerBinder;
    private HostUiOperationController operationController;
    private SpinnerDialog activeProgress;
    private boolean serviceBound;
    private boolean started;
    private boolean destroyed;

    private final ServiceConnection serviceConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(
                        ComponentName className,
                        IBinder binder) {
                    if (!destroyed) {
                        managerBinder =
                                (ComputerManagerService.ComputerManagerBinder)
                                        binder;
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    managerBinder = null;
                    if (operationController != null) {
                        operationController.cancelCurrent();
                    }
                    dismissProgress();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_computer_manually);
        UiHelper.notifyNewEdgeToEdgeRootView(
                this,
                R.id.rv_top_view,
                R.id.addComputerContent);

        operationController = HostUiOperationController.create(
                command -> mainHandler.post(command));
        hostText = findViewById(R.id.hostTextView);
        hostText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        hostText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null &&
                            keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                            keyEvent.getKeyCode() ==
                                    KeyEvent.KEYCODE_ENTER)) {
                return submitHostAddition();
            }
            if (actionId == EditorInfo.IME_ACTION_PREVIOUS) {
                InputMethodManager inputManager =
                        (InputMethodManager) getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(
                        hostText.getWindowToken(),
                        0);
            }
            return false;
        });

        findViewById(R.id.addPcButton).setOnClickListener(
                view -> submitHostAddition());
        findViewById(R.id.razerPort).setOnClickListener(
                view -> hostText.append(":51337"));
        findViewById(R.id.getPcButton).setOnClickListener(
                view -> submitSrvResolution());

        serviceBound = bindService(
                new Intent(this, ComputerManagerService.class),
                serviceConnection,
                Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
    }

    @Override
    protected void onStop() {
        started = false;
        operationController.cancelCurrent();
        dismissProgress();
        Dialog.closeDialogs();
        SpinnerDialog.closeDialogs(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        managerBinder = null;
        if (operationController != null) {
            operationController.destroy();
            operationController = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
        dismissProgress();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }

    private boolean submitHostAddition() {
        String rawInput = hostText.getText().toString().trim();
        if (rawInput.isEmpty()) {
            showToast(R.string.addpc_enter_ip, UiToast.LENGTH_LONG);
            return true;
        }

        HostEndpoint endpoint = ManualHostEndpointParser.parse(
                rawInput,
                NvHTTP.DEFAULT_HTTP_PORT);
        if (endpoint == null) {
            showErrorDialog(R.string.addpc_unknown_host);
            return true;
        }

        ComputerManagerService.ComputerManagerBinder binder = managerBinder;
        if (binder == null) {
            showErrorDialog(R.string.addpc_fail);
            return true;
        }

        HostUiOperationController.RequestStatus status =
                operationController.request(
                        () -> addHost(binder, endpoint),
                        this::onHostAdditionCompleted);
        handleRequestStatus(status);
        return true;
    }

    private AddResult addHost(
            ComputerManagerService.ComputerManagerBinder binder,
            HostEndpoint endpoint) throws InterruptedException {
        boolean successful = binder.addHostBlocking(endpoint);
        boolean wrongSiteLocalAddress = !successful &&
                isWrongSubnetSiteLocalAddress(endpoint.getAddress());
        int portTestResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE;
        if (!successful && !wrongSiteLocalAddress) {
            portTestResult = MoonBridge.testClientConnectivity(
                    ClientConnectivityEndpoint.HOST,
                    ClientConnectivityEndpoint.HTTPS_PORT,
                    MoonBridge.ML_PORT_FLAG_TCP_47984 |
                            MoonBridge.ML_PORT_FLAG_TCP_47989);
        }
        return new AddResult(
                successful,
                wrongSiteLocalAddress,
                portTestResult);
    }

    private void onHostAdditionCompleted(
            HostUiOperationController.Result<AddResult> result) {
        dismissProgress();
        if (!started || destroyed) {
            return;
        }
        if (!result.isSuccessful()) {
            if (!(result.getFailure() instanceof InterruptedException)) {
                LimeLog.warning(
                        "Manual host admission failed: " +
                                result.getFailure()
                                        .getClass()
                                        .getSimpleName());
                showErrorDialog(R.string.addpc_fail);
            }
            return;
        }

        AddResult addResult = result.getValue();
        if (addResult.successful) {
            finish();
        }
        else if (addResult.wrongSiteLocalAddress) {
            showErrorDialog(R.string.addpc_wrong_sitelocal);
        }
        else if (addResult.portTestResult !=
                        MoonBridge.ML_TEST_RESULT_INCONCLUSIVE &&
                addResult.portTestResult != 0) {
            showErrorDialog(R.string.nettest_text_blocked);
        }
        else {
            showErrorDialog(R.string.addpc_fail);
        }
    }

    private void submitSrvResolution() {
        String input = hostText.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            showToast(R.string.addpc_enter_ip, UiToast.LENGTH_LONG);
            return;
        }

        HostUiOperationController.RequestStatus status =
                operationController.request(
                        () -> SrvResolver.resolveSRVRecord(input),
                        this::onSrvResolutionCompleted);
        handleRequestStatus(status);
    }

    private void onSrvResolutionCompleted(
            HostUiOperationController.Result<SrvResolver.ResultCode>
                    result) {
        dismissProgress();
        if (!started || destroyed) {
            return;
        }
        if (!result.isSuccessful() || result.getValue() == null) {
            if (result.getFailure() != null &&
                    !(result.getFailure() instanceof InterruptedException)) {
                LimeLog.warning(
                        "SRV host resolution failed: " +
                                result.getFailure()
                                        .getClass()
                                        .getSimpleName());
            }
            showErrorDialog(R.string.addpc_unknown_host);
            return;
        }

        SrvResolver.ResultCode resolved = result.getValue();
        if (resolved.getCode() != 0) {
            UiToast.makeText(
                    this,
                    resolved.getResult(),
                    UiToast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(resolved.getResult())) {
            showToast(R.string.addpc_srv_not_found, UiToast.LENGTH_SHORT);
        }
        else {
            hostText.setText(resolved.getResult());
        }
    }

    private void handleRequestStatus(
            HostUiOperationController.RequestStatus status) {
        switch (status) {
            case ACCEPTED:
                activeProgress = SpinnerDialog.displayDialog(
                        this,
                        getString(R.string.title_add_pc),
                        getString(R.string.msg_add_pc),
                        false);
                break;
            case ALREADY_RUNNING:
                // The existing progress UI already represents the operation.
                break;
            case DESTROYED:
            case UNAVAILABLE:
                showErrorDialog(R.string.addpc_fail);
                break;
            default:
                throw new AssertionError(
                        "Unhandled request status: " + status);
        }
    }

    private void dismissProgress() {
        if (activeProgress != null) {
            activeProgress.dismiss();
            activeProgress = null;
        }
    }

    private void showErrorDialog(int messageResource) {
        if (!destroyed) {
            Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(messageResource),
                    false);
        }
    }

    private void showToast(int messageResource, int duration) {
        UiToast.makeText(
                this,
                getString(messageResource),
                duration).show();
    }

    private boolean isWrongSubnetSiteLocalAddress(String address) {
        try {
            InetAddress targetAddress = InetAddress.getByName(address);
            if (!(targetAddress instanceof Inet4Address) ||
                    !targetAddress.isSiteLocalAddress()) {
                return false;
            }

            for (NetworkInterface networkInterface :
                    Collections.list(
                            NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress interfaceAddress :
                        networkInterface.getInterfaceAddresses()) {
                    InetAddress localAddress =
                            interfaceAddress.getAddress();
                    if (!(localAddress instanceof Inet4Address) ||
                            !localAddress.isSiteLocalAddress()) {
                        continue;
                    }
                    if (Ipv4SubnetMatcher.isSameSubnet(
                            targetAddress.getAddress(),
                            localAddress.getAddress(),
                            interfaceAddress.getNetworkPrefixLength())) {
                        return false;
                    }
                }
            }
            return true;
        }
        catch (Exception error) {
            // Some Android builds throw from network-interface enumeration.
            LimeLog.warning(
                    "Unable to evaluate manual host subnet");
            return false;
        }
    }
}
