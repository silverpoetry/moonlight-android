package com.limelight.preferences

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import com.limelight.BaseActivity
import com.limelight.LimeLog
import com.limelight.R
import com.limelight.SrvResolver
import com.limelight.computers.ComputerManagerService
import com.limelight.computers.model.HostEndpoint
import com.limelight.computers.model.ManualHostEndpointParser
import com.limelight.computers.reachability.ClientConnectivityEndpoint
import com.limelight.computers.reachability.Ipv4SubnetMatcher
import com.limelight.nvstream.http.NvHTTP
import com.limelight.nvstream.jni.MoonBridge
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import com.limelight.ui.hosts.HostUiOperationController
import com.limelight.utils.UiToast
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.Executor

/** Material 3 dialog-style presentation adapter for manual host admission. */
class AddComputerManually : BaseActivity() {
    private data class AddResult(
        val successful: Boolean,
        val wrongSiteLocalAddress: Boolean,
        val portTestResult: Int,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    private var operationController: HostUiOperationController? = null
    private var serviceBound = false
    private var started = false
    private var destroyed = false
    private var hostText by mutableStateOf("")
    private var operationInProgress by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            if (!destroyed) {
                managerBinder = binder as ComputerManagerService.ComputerManagerBinder
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            managerBinder = null
            operationController?.cancelCurrent()
            operationInProgress = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        operationController = HostUiOperationController.create(
            Executor { command -> mainHandler.post(command) },
        )
        setContent {
            MoonlightThemeFromSettings {
                AddComputerDialog(
                    hostText = hostText,
                    operationInProgress = operationInProgress,
                    errorMessage = errorMessage,
                    onHostTextChanged = { hostText = it },
                    onConfirm = ::submitHostAddition,
                    onDismiss = ::finish,
                    onDismissError = { errorMessage = null },
                )
            }
        }
        serviceBound = bindService(
            Intent(this, ComputerManagerService::class.java),
            serviceConnection,
            Service.BIND_AUTO_CREATE,
        )
    }

    override fun onStart() {
        super.onStart()
        started = true
    }

    override fun onStop() {
        started = false
        operationController?.cancelCurrent()
        operationInProgress = false
        super.onStop()
    }

    override fun onDestroy() {
        destroyed = true
        managerBinder = null
        operationController?.destroy()
        operationController = null
        mainHandler.removeCallbacksAndMessages(null)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        super.onDestroy()
    }

    private fun submitHostAddition() {
        val rawInput = hostText.trim()
        if (rawInput.isEmpty()) {
            showToast(R.string.addpc_enter_ip, UiToast.LENGTH_LONG)
            return
        }
        val endpoint = ManualHostEndpointParser.parse(rawInput, NvHTTP.DEFAULT_HTTP_PORT)
        if (endpoint == null) {
            showError(R.string.addpc_unknown_host)
            return
        }
        val binder = managerBinder
        if (binder == null) {
            showError(R.string.addpc_fail)
            return
        }
        val status = operationController?.request(
            { addHost(binder, endpoint) },
            ::onHostAdditionCompleted,
        ) ?: HostUiOperationController.RequestStatus.UNAVAILABLE
        handleRequestStatus(status)
    }

    @Throws(InterruptedException::class)
    private fun addHost(
        binder: ComputerManagerService.ComputerManagerBinder,
        endpoint: HostEndpoint,
    ): AddResult {
        val successful = binder.addHostBlocking(endpoint)
        val wrongSiteLocalAddress = !successful &&
            isWrongSubnetSiteLocalAddress(endpoint.address)
        val portTestResult = if (!successful && !wrongSiteLocalAddress) {
            MoonBridge.testClientConnectivity(
                ClientConnectivityEndpoint.HOST,
                ClientConnectivityEndpoint.HTTPS_PORT,
                MoonBridge.ML_PORT_FLAG_TCP_47984 or MoonBridge.ML_PORT_FLAG_TCP_47989,
            )
        } else {
            MoonBridge.ML_TEST_RESULT_INCONCLUSIVE
        }
        return AddResult(successful, wrongSiteLocalAddress, portTestResult)
    }

    private fun onHostAdditionCompleted(result: HostUiOperationController.Result<AddResult>) {
        operationInProgress = false
        if (!started || destroyed) return
        if (!result.isSuccessful) {
            if (result.failure !is InterruptedException) {
                LimeLog.warning(
                    "Manual host admission failed: " +
                        (result.failure?.javaClass?.simpleName ?: "Unknown"),
                )
                showError(R.string.addpc_fail)
            }
            return
        }
        val addResult = result.value
        when {
            addResult.successful -> finish()
            addResult.wrongSiteLocalAddress -> showError(R.string.addpc_wrong_sitelocal)
            addResult.portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE &&
                addResult.portTestResult != 0 -> showError(R.string.nettest_text_blocked)
            else -> showError(R.string.addpc_fail)
        }
    }

    @Suppress("unused")
    private fun submitSrvResolution() {
        val input = hostText.trim()
        if (input.isEmpty()) {
            showToast(R.string.addpc_enter_ip, UiToast.LENGTH_LONG)
            return
        }
        val status = operationController?.request(
            { SrvResolver.resolveSRVRecord(input) },
            ::onSrvResolutionCompleted,
        ) ?: HostUiOperationController.RequestStatus.UNAVAILABLE
        handleRequestStatus(status)
    }

    private fun onSrvResolutionCompleted(
        result: HostUiOperationController.Result<SrvResolver.ResultCode>,
    ) {
        operationInProgress = false
        if (!started || destroyed) return
        if (!result.isSuccessful || result.value == null) {
            if (result.failure != null && result.failure !is InterruptedException) {
                LimeLog.warning(
                    "SRV host resolution failed: ${result.failure.javaClass.simpleName}",
                )
            }
            showError(R.string.addpc_unknown_host)
            return
        }
        val resolved = result.value
        when {
            resolved.code != 0 -> UiToast.makeText(
                this,
                resolved.result,
                UiToast.LENGTH_SHORT,
            ).show()
            resolved.result.isNullOrEmpty() -> showToast(
                R.string.addpc_srv_not_found,
                UiToast.LENGTH_SHORT,
            )
            else -> hostText = resolved.result
        }
    }

    private fun handleRequestStatus(status: HostUiOperationController.RequestStatus) {
        when (status) {
            HostUiOperationController.RequestStatus.ACCEPTED -> operationInProgress = true
            HostUiOperationController.RequestStatus.ALREADY_RUNNING -> Unit
            HostUiOperationController.RequestStatus.DESTROYED,
            HostUiOperationController.RequestStatus.UNAVAILABLE -> showError(R.string.addpc_fail)
        }
    }

    private fun showError(messageResource: Int) {
        if (!destroyed) {
            errorMessage = getString(messageResource)
        }
    }

    private fun showToast(messageResource: Int, duration: Int) {
        UiToast.makeText(this, getString(messageResource), duration).show()
    }

    private fun isWrongSubnetSiteLocalAddress(address: String): Boolean {
        return try {
            val targetAddress = InetAddress.getByName(address)
            if (targetAddress !is Inet4Address || !targetAddress.isSiteLocalAddress) {
                false
            } else {
                Collections.list(NetworkInterface.getNetworkInterfaces()).none { networkInterface ->
                    networkInterface.interfaceAddresses.any { interfaceAddress: InterfaceAddress ->
                        val localAddress = interfaceAddress.address
                        localAddress is Inet4Address &&
                            localAddress.isSiteLocalAddress &&
                            Ipv4SubnetMatcher.isSameSubnet(
                                targetAddress.address,
                                localAddress.address,
                                interfaceAddress.networkPrefixLength.toInt(),
                            )
                    }
                }
            }
        } catch (_: Exception) {
            LimeLog.warning("Unable to evaluate manual host subnet")
            false
        }
    }
}

@Composable
private fun AddComputerDialog(
    hostText: String,
    operationInProgress: Boolean,
    errorMessage: String?,
    onHostTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = if (isLandscape) 520.dp else 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.title_add_pc),
                    style = MaterialTheme.typography.headlineSmall,
                )
                OutlinedTextField(
                    value = hostText,
                    onValueChange = onHostTextChanged,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    enabled = !operationInProgress,
                    label = { Text(stringResource(R.string.ip_hint)) },
                    placeholder = {
                        Text(stringResource(R.string.addpc_address_placeholder))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (operationInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 20.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !operationInProgress,
                    ) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !operationInProgress,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.conn_error_title)) },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}
