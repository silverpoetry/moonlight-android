package com.limelight.ui.compose.hosts

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.computers.model.HostConnectionState
import com.limelight.computers.model.HostRuntimeSnapshot
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Material 3 host browser. Host discovery and operations stay in PcView. */
class HostScreenRenderer(
    private val context: Context,
    private var listener: Listener?,
) {
    interface Listener {
        fun onSettingsRequested()
        fun onAddComputerRequested()
        fun onStreamRequested(host: HostRuntimeSnapshot)
        fun onAppsRequested(host: HostRuntimeSnapshot)
        fun onHostMenuRequested(host: HostRuntimeSnapshot)
    }

    private data class State(
        val title: String,
        val hosts: List<HostRuntimeSnapshot>,
        val showEmptyState: Boolean,
        val hostsWithRecentStreams: Set<String>,
    )

    private var state: State by mutableStateOf(
        State(
            context.getString(R.string.app_label),
            emptyList(),
            false,
            emptySet(),
        ),
    )
    private var composeView: ComposeView? = null

    fun createRootView(): View {
        composeView?.let { return it }
        return ComposeView(context).also { view ->
            composeView = view
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            view.setContent {
                MoonlightThemeFromSettings {
                    HostScreen(state)
                }
            }
        }
    }

    fun updateTitle(title: CharSequence) {
        state = state.copy(title = title.toString())
    }

    fun updateHosts(
        hosts: List<HostRuntimeSnapshot>,
        showEmptyState: Boolean,
        hostsWithRecentStreams: Set<String>,
    ) {
        state = state.copy(
            hosts = hosts.toList(),
            showEmptyState = showEmptyState,
            hostsWithRecentStreams = hostsWithRecentStreams.toSet(),
        )
    }

    fun destroy() {
        listener = null
        composeView?.disposeComposition()
        composeView = null
    }

    @Composable
    private fun HostScreen(state: State) {
        MoonlightScreen(
            title = state.title,
            actions = {
                IconButton(onClick = { listener?.onSettingsRequested() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_m3_settings),
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { listener?.onAddComputerRequested() },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_m3_add),
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.title_add_pc)) },
                )
            },
        ) { padding ->
            when {
                state.hosts.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            items = state.hosts,
                            key = { it.record.identity.id.value },
                        ) { host ->
                            HostCard(
                                host = host,
                                hasRecentStream = host.record.identity.id.value in
                                        state.hostsWithRecentStreams,
                            )
                        }
                    }
                }
                state.showEmptyState -> {
                    EmptyHostState(
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                }
                else -> Unit
            }
        }
    }

    @Composable
    private fun EmptyHostState(
        modifier: Modifier,
    ) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(32.dp).widthIn(max = 420.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_m3_computer),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = stringResource(R.string.host_empty_state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun HostCard(
        host: HostRuntimeSnapshot,
        hasRecentStream: Boolean,
    ) {
        val connection = host.connectionState
        val connectionStatusColor = statusColor(connection)
        val hostName = host.record.identity.displayName
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { listener?.onStreamRequested(host) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_m3_computer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hostName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(connectionStatusColor, CircleShape),
                            )
                            Text(
                                text = stringResource(statusText(connection)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    IconButton(onClick = { listener?.onHostMenuRequested(host) }) {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.host_active_endpoint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = displayEndpoint(host)
                            ?: stringResource(R.string.host_active_endpoint_pending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { listener?.onStreamRequested(host) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(primaryActionIcon(connection)),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(
                                primaryActionText(connection, hasRecentStream),
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = { listener?.onAppsRequested(host) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_m3_more_vert),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.host_action_apps),
                            modifier = Modifier.padding(start = 8.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun statusColor(connection: HostConnectionState): Color = when (
        connection.reachability
    ) {
        HostConnectionState.Reachability.ONLINE -> Color(0xFF43A047)
        HostConnectionState.Reachability.OFFLINE -> Color(0xFF757575)
        HostConnectionState.Reachability.UNKNOWN -> Color(0xFF9E9E9E)
    }

    private fun statusText(connection: HostConnectionState): Int = when (
        connection.reachability
    ) {
        HostConnectionState.Reachability.ONLINE -> R.string.pcview_menu_header_online
        HostConnectionState.Reachability.OFFLINE -> R.string.pcview_menu_header_offline
        HostConnectionState.Reachability.UNKNOWN -> R.string.pcview_menu_header_unknown
    }

    private fun primaryActionText(
        connection: HostConnectionState,
        hasRecentStream: Boolean,
    ): Int = when {
        connection.reachability != HostConnectionState.Reachability.ONLINE ->
            R.string.pcview_menu_header_offline
        connection.pairingStatus != HostConnectionState.PairingStatus.PAIRED ->
            R.string.pcview_menu_pair_pc
        connection.runningAppId != 0 ->
            R.string.applist_menu_resume
        hasRecentStream ->
            R.string.applist_menu_resume
        else ->
            R.string.host_action_stream
    }

    private fun primaryActionIcon(connection: HostConnectionState): Int = when {
        connection.reachability != HostConnectionState.Reachability.ONLINE ->
            R.drawable.ic_m3_link_off
        connection.pairingStatus != HostConnectionState.PairingStatus.PAIRED ->
            R.drawable.ic_m3_key
        else ->
            R.drawable.ic_m3_play_arrow
    }

    private fun displayEndpoint(host: HostRuntimeSnapshot): String? {
        return host.connectionState.activeEndpoint?.address
    }
}
