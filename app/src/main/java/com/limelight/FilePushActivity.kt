package com.limelight

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.limelight.ui.compose.components.MoonlightActionRow
import com.limelight.ui.compose.components.MoonlightGroup
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import com.limelight.ui.filepush.FilePushController
import com.limelight.ui.filepush.FilePushTarget
import com.limelight.ui.filepush.android.AndroidFilePushHostCatalog
import com.limelight.ui.filepush.android.AndroidFilePushUploader
import com.limelight.utils.BackNavigationRegistration
import com.limelight.utils.UiToast
import java.util.concurrent.Executor

class FilePushActivity : BaseActivity() {
    private var controller: FilePushController? = null
    private var backNavigationRegistration: BackNavigationRegistration? = null
    private var selectionCount = 0
    private var uiState: FilePushUiState by mutableStateOf(FilePushUiState.LoadingHosts)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        backNavigationRegistration =
            BackNavigationRegistration.register(this, ::handleBackNavigation)

        val sharedUris = collectSharedUris(intent)
        selectionCount = sharedUris.size
        setContent {
            MoonlightThemeFromSettings {
                FilePushScreen(
                    selectionCount = selectionCount,
                    state = uiState,
                    onBack = ::handleBackNavigation,
                    onClose = ::finish,
                    onRetry = ::showHostPicker,
                    onHostSelected = ::beginUpload,
                )
            }
        }

        if (sharedUris.isEmpty()) {
            showTerminalError(getString(R.string.file_push_no_shared_items))
            return
        }

        controller = FilePushController.create(
            AndroidFilePushHostCatalog(this),
            AndroidFilePushUploader(this, sharedUris),
            Executor { command -> runOnUiThread(command) },
        )
        showHostPicker()
    }

    override fun onDestroy() {
        backNavigationRegistration?.unregister()
        backNavigationRegistration = null
        controller?.destroy()
        controller = null
        super.onDestroy()
    }

    private fun handleBackNavigation() {
        if (controller?.isUploadInProgress == true) {
            UiToast.makeText(
                this,
                R.string.file_push_in_progress,
                UiToast.LENGTH_SHORT,
            ).show()
            return
        }
        finish()
    }

    private fun showHostPicker() {
        uiState = FilePushUiState.LoadingHosts
        val status = controller?.loadHosts { result ->
            if (isFinishing || isDestroyed) {
                return@loadHosts
            }
            if (!result.isSuccessful) {
                showTerminalError(errorMessage(result.failure))
                return@loadHosts
            }

            val hosts = result.value.orEmpty()
            if (hosts.isEmpty()) {
                showTerminalError(getString(R.string.file_push_no_hosts))
            } else {
                uiState = FilePushUiState.HostPicker(hosts)
            }
        }
        if (status != FilePushController.RequestStatus.ACCEPTED) {
            showTerminalError(getString(R.string.file_push_unknown_error))
        }
    }

    private fun beginUpload(target: FilePushTarget) {
        val activeController = controller ?: return
        val status = activeController.upload(
            target,
            object : FilePushController.UploadCallback {
                override fun onProgress(transferredBytes: Long, totalBytes: Long) {
                    updateProgress(transferredBytes, totalBytes)
                }

                override fun onCompleted(result: FilePushController.Result<FilePushTarget>) {
                    if (result.isSuccessful) {
                        showUploadComplete(result.value.displayName)
                    } else {
                        LimeLog.warning(
                            "Desktop file upload failed: " +
                                (result.failure?.javaClass?.simpleName ?: "Unknown"),
                        )
                        showUploadError(errorMessage(result.failure))
                    }
                }
            },
        )
        if (status == FilePushController.RequestStatus.ACCEPTED) {
            uiState = FilePushUiState.Uploading(target.displayName, 0, 0)
        }
    }

    private fun updateProgress(transferred: Long, total: Long) {
        if (controller?.isUploadInProgress != true || isFinishing || isDestroyed) {
            return
        }
        val state = uiState
        if (state is FilePushUiState.Uploading) {
            uiState = state.copy(transferredBytes = transferred, totalBytes = total)
        }
    }

    private fun showUploadComplete(computerName: String) {
        if (!isFinishing && !isDestroyed) {
            uiState = FilePushUiState.Complete(computerName)
        }
    }

    private fun showUploadError(message: String) {
        if (!isFinishing && !isDestroyed) {
            uiState = FilePushUiState.RecoverableError(message)
        }
    }

    private fun showTerminalError(message: String) {
        uiState = FilePushUiState.TerminalError(message)
    }

    private fun errorMessage(failure: Exception?): String {
        return failure?.message?.takeIf { it.isNotBlank() }
            ?: getString(R.string.file_push_unknown_error)
    }

    private companion object {
        fun collectSharedUris(intent: Intent?): List<Uri> {
            if (intent == null) {
                return emptyList()
            }
            val uris = ArrayList<Uri>()
            when (intent.action) {
                Intent.ACTION_SEND_MULTIPLE -> {
                    IntentCompat.getParcelableArrayListExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java,
                    )?.let(uris::addAll)
                }

                Intent.ACTION_SEND -> {
                    IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java,
                    )?.let(uris::add)
                }
            }
            val clipData: ClipData? = intent.clipData
            if (clipData != null) {
                repeat(clipData.itemCount) { index ->
                    clipData.getItemAt(index).uri?.let { uri ->
                        if (uri !in uris) {
                            uris.add(uri)
                        }
                    }
                }
            }
            return uris
        }
    }
}

private sealed interface FilePushUiState {
    data object LoadingHosts : FilePushUiState
    data class HostPicker(val hosts: List<FilePushTarget>) : FilePushUiState
    data class Uploading(
        val computerName: String,
        val transferredBytes: Long,
        val totalBytes: Long,
    ) : FilePushUiState
    data class Complete(val computerName: String) : FilePushUiState
    data class RecoverableError(val message: String) : FilePushUiState
    data class TerminalError(val message: String) : FilePushUiState
}

@Composable
private fun FilePushScreen(
    selectionCount: Int,
    state: FilePushUiState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onHostSelected: (FilePushTarget) -> Unit,
) {
    val title = when (state) {
        is FilePushUiState.Uploading -> stringResource(R.string.file_push_uploading_title)
        is FilePushUiState.Complete -> stringResource(R.string.file_push_complete_title)
        is FilePushUiState.RecoverableError,
        is FilePushUiState.TerminalError -> stringResource(R.string.file_push_failed_title)
        else -> stringResource(R.string.desktop_file_share_target)
    }
    MoonlightScreen(title = title, onBack = onBack) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = subtitleForState(state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoonlightGroup {
                        MoonlightActionRow(
                        title = pluralStringResource(
                                R.plurals.file_push_selection_count,
                                selectionCount,
                                selectionCount,
                            ),
                            supportingText = stringResource(R.string.file_push_selection_hint),
                            iconRes = R.drawable.ic_clipboard_send,
                            onClick = {},
                            showDivider = false,
                        )
                    }
                }
            }

            when (state) {
                FilePushUiState.LoadingHosts -> item {
                    LoadingPanel()
                }

                is FilePushUiState.HostPicker -> item {
                    HostPicker(
                        hosts = state.hosts,
                        onHostSelected = onHostSelected,
                    )
                }

                is FilePushUiState.Uploading -> item {
                    UploadProgress(state)
                }

                is FilePushUiState.Complete -> item {
                    CompletionPanel()
                }

                is FilePushUiState.RecoverableError -> item {
                    ErrorPanel(state.message)
                }

                is FilePushUiState.TerminalError -> item {
                    ErrorPanel(state.message)
                }
            }

            item {
                ActionButtons(
                    state = state,
                    onClose = onClose,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun subtitleForState(state: FilePushUiState): String = when (state) {
    is FilePushUiState.Uploading -> stringResource(
        R.string.file_push_uploading_to,
        state.computerName,
    )
    is FilePushUiState.Complete -> stringResource(
        R.string.file_push_complete_message,
        state.computerName,
    )
    is FilePushUiState.TerminalError -> stringResource(R.string.desktop_file_share_target)
    else -> stringResource(R.string.file_push_choose_host)
}

@Composable
private fun LoadingPanel() {
    MoonlightGroup(modifier = Modifier.widthIn(max = 640.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.file_push_preparing),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.file_push_keep_open),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HostPicker(
    hosts: List<FilePushTarget>,
    onHostSelected: (FilePushTarget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.file_push_host_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        MoonlightGroup {
            hosts.forEachIndexed { index, host ->
                MoonlightActionRow(
                    title = host.displayName,
                    supportingText = stringResource(
                        R.string.file_push_host_paired,
                        host.address,
                    ),
                    iconRes = R.drawable.ic_host_computer,
                    onClick = { onHostSelected(host) },
                    showDivider = index != hosts.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun UploadProgress(state: FilePushUiState.Uploading) {
    val context = LocalContext.current
    val hasTotal = state.totalBytes > 0
    val detail = if (hasTotal) {
        stringResource(
            R.string.file_transfer_progress_bytes,
            Formatter.formatFileSize(context, state.transferredBytes),
            Formatter.formatFileSize(context, state.totalBytes),
        )
    } else {
        stringResource(
            R.string.file_transfer_progress_transferred,
            Formatter.formatFileSize(context, state.transferredBytes),
        )
    }
    MoonlightGroup(modifier = Modifier.widthIn(max = 640.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (state.transferredBytes == 0L) {
                    stringResource(R.string.file_push_preparing)
                } else {
                    stringResource(R.string.file_push_uploading_title)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (hasTotal) {
                LinearProgressIndicator(
                    progress = {
                        (state.transferredBytes.toFloat() / state.totalBytes)
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletionPanel() {
    MoonlightGroup(modifier = Modifier.widthIn(max = 640.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.file_push_complete_title),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ActionButtons(
    state: FilePushUiState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state is FilePushUiState.Uploading) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        when (state) {
            is FilePushUiState.RecoverableError -> {
                OutlinedButton(onClick = onClose) {
                    Text(stringResource(R.string.file_transfer_close))
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(stringResource(R.string.file_push_retry))
                }
            }

            is FilePushUiState.Complete -> Button(onClick = onClose) {
                Text(stringResource(R.string.file_transfer_done))
            }

            is FilePushUiState.TerminalError -> Button(onClick = onClose) {
                Text(stringResource(R.string.file_transfer_close))
            }

            else -> OutlinedButton(onClick = onClose) {
                Text(stringResource(R.string.file_push_cancel))
            }
        }
    }
}
