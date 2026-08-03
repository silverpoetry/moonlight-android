package com.limelight.ui.gamemenu

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Compose presentation shell for the in-stream command menu. */
class GameMenuComposeRenderer(
    private val composeView: ComposeView,
    private var listener: Listener?,
) {
    interface Listener {
        fun onActionRequested(viewId: Int, shortcutEntry: Any?)
        fun onActionLongPressed(viewId: Int)
        fun onCustomizeRequested()
    }

    private data class MenuCard(
        val viewId: Int,
        val label: String,
        val contentDescription: String,
        val iconRes: Int,
        val shortcutEntry: Any?,
    )

    private data class RenderState(
        val menuState: GameMenuState,
        val cards: List<MenuCard>,
    )

    private var state: RenderState? by mutableStateOf(null)

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        composeView.setContent {
            MoonlightThemeFromSettings {
                state?.let { currentState ->
                    GameMenuSurface(currentState)
                }
            }
        }
    }

    fun update(menuState: GameMenuState, sourceCards: List<*>) {
        val cards = sourceCards.mapNotNull { value ->
            val card = value as? GameMenuCardCatalog.Card ?: return@mapNotNull null
            if (card.requiresGamepad() && !menuState.isMouseEmulationAvailable) {
                return@mapNotNull null
            }
            MenuCard(
                viewId = card.action?.viewId ?: 0,
                label = card.label,
                contentDescription = card.contentDescription,
                iconRes = card.iconRes,
                shortcutEntry = card.shortcut,
            )
        }
        state = RenderState(menuState, cards)
    }

    fun destroy() {
        listener = null
        state = null
        composeView.disposeComposition()
    }

    @Composable
    private fun GameMenuSurface(state: RenderState) {
        val landscape = LocalConfiguration.current.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            if (landscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    MenuContent(
                        state = state,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    SideActions(
                        modifier = Modifier.width(76.dp).fillMaxHeight(),
                        vertical = true,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    MenuContent(
                        state = state,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    SideActions(
                        modifier = Modifier.fillMaxWidth(),
                        vertical = false,
                    )
                }
            }
        }
    }

    @Composable
    private fun MenuContent(state: RenderState, modifier: Modifier) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.game_menu_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.game_menu_session_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (state.menuState.batteryPercent ==
                        GameMenuState.UNKNOWN_BATTERY_PERCENT
                    ) {
                        stringResource(R.string.game_menu_battery_unknown)
                    } else {
                        stringResource(
                            R.string.game_menu_battery_percent,
                            state.menuState.batteryPercent,
                        )
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.game_menu_quick_actions_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { listener?.onCustomizeRequested() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.game_menu_customize),
                    )
                }
            }
            ActionGrid(state)

            Text(
                text = stringResource(R.string.game_menu_input_controls_title),
                style = MaterialTheme.typography.titleMedium,
            )
            NavigationGroup(
                entries = listOf(
                    NavigationEntry(R.id.bt_touch_list, R.string.game_menu_section_mouse_touch, R.drawable.ic_touch),
                    NavigationEntry(R.id.bt_touch_sensitivity, R.string.game_menu_section_touch_sensitivity, R.drawable.ic_touch_sensitivity),
                    NavigationEntry(R.id.bt_quick_list, R.string.game_menu_section_shortcuts, R.drawable.ic_quick_actions),
                    NavigationEntry(R.id.bt_virtual_view, R.string.game_menu_section_virtual_controls, R.drawable.ic_gamepad),
                    NavigationEntry(R.id.bt_device, R.string.game_menu_section_peripherals, R.drawable.ic_gamepad_device),
                ),
            )

            Text(
                text = stringResource(R.string.game_menu_stream_experience_title),
                style = MaterialTheme.typography.titleMedium,
            )
            NavigationGroup(
                entries = listOf(
                    NavigationEntry(R.id.bt_display, R.string.game_menu_section_display, R.drawable.ic_gamepad_display),
                ),
            )
        }
    }

    @Composable
    private fun ActionGrid(state: RenderState) {
        if (state.cards.isEmpty()) {
            Text(
                text = stringResource(R.string.game_menu_customize_no_visible),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.cards.chunked(3).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowCards.forEach { card ->
                        val active = isCardActive(state.menuState, card.viewId)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = {
                                        listener?.onActionRequested(
                                            card.viewId,
                                            card.shortcutEntry,
                                        )
                                    },
                                    onLongClick = if (card.viewId == R.id.btn_performance) {
                                        { listener?.onActionLongPressed(card.viewId) }
                                    } else {
                                        null
                                    },
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(card.iconRes),
                                    contentDescription = card.contentDescription,
                                    tint = if (active) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(28.dp),
                                )
                                Text(
                                    text = card.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat(3 - rowCards.size) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    private data class NavigationEntry(
        val viewId: Int,
        val titleRes: Int,
        val iconRes: Int,
    )

    @Composable
    private fun NavigationGroup(entries: List<NavigationEntry>) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            entries.forEachIndexed { index, entry ->
                ListItem(
                    headlineContent = { Text(stringResource(entry.titleRes)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(entry.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow),
                            contentDescription = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        listener?.onActionRequested(entry.viewId, null)
                    },
                )
                if (index != entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun SideActions(modifier: Modifier, vertical: Boolean) {
        val actions = listOf(
            NavigationEntry(R.id.btn_soft_function, R.string.game_menu_tab_actions, R.drawable.ic_menu_grid),
            NavigationEntry(R.id.btn_soft_keyboard, R.string.game_menu_tab_keyboard, R.drawable.ic_keyboard),
            NavigationEntry(R.id.btn_desktop, R.string.game_menu_tab_desktop, R.drawable.ic_desktop),
            NavigationEntry(R.id.btn_window, R.string.game_menu_tab_windows, R.drawable.ic_window),
        )
        val container: @Composable (@Composable () -> Unit) -> Unit = { content ->
            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = content,
            )
        }
        container {
            if (vertical) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    actions.forEach { entry -> SideAction(entry) }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    actions.forEach { entry -> SideAction(entry) }
                }
            }
        }
    }

    @Composable
    private fun SideAction(entry: NavigationEntry) {
        Column(
            modifier = Modifier
                .clickable { listener?.onActionRequested(entry.viewId, null) }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(entry.iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = stringResource(entry.titleRes),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    private fun isCardActive(state: GameMenuState, viewId: Int): Boolean = when (viewId) {
        R.id.btn_performance -> state.uiSettings.isPerformanceOverlayEnabled
        R.id.btn_game_pad -> state.isVirtualControllerVisible
        R.id.btn_v_keyboard -> state.isVirtualKeysVisible
        R.id.btn_screen_move -> state.isScreenMoveZoom
        R.id.btn_mic -> state.isMicrophoneActive
        R.id.btn_audio_mute -> state.audioSettings.isMuted
        R.id.btn_video_visibility -> state.isVideoHidden
        else -> false
    }
}
