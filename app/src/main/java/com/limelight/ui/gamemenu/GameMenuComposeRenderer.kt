package com.limelight.ui.gamemenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.limelight.R
import com.limelight.settings.ui.GameMenuCardLayout
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import kotlin.math.roundToInt

/** Compose presentation shell for the in-stream command menu. */
internal class GameMenuComposeRenderer(
    private val composeView: ComposeView,
    private var listener: Listener?,
) {
    interface Listener {
        fun onActionRequested(viewId: Int, shortcutEntry: Any?)
        fun onActionLongPressed(viewId: Int)
        fun onCardLayoutChanged(layout: GameMenuCardLayout)
        fun onDismissRequested()
    }

    private data class MenuCard(
        val viewId: Int,
        val label: String,
        val contentDescription: String,
        val iconRes: Int,
        val shortcutEntry: Any?,
        val source: GameMenuCardCatalog.Card,
        val available: Boolean,
    )

    private data class RenderState(
        val menuState: GameMenuState,
        val cards: List<MenuCard>,
        val hiddenCards: List<MenuCard>,
    )

    private var state: RenderState? by mutableStateOf(null)
    private var editing by mutableStateOf(false)

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

    fun update(
        menuState: GameMenuState,
        configuration: GameMenuCardConfiguration.State,
    ) {
        if (editing) {
            state = state?.copy(menuState = menuState)
            return
        }
        state = RenderState(
            menuState = menuState,
            cards = mapCards(menuState, configuration.visible),
            hiddenCards = mapCards(menuState, configuration.hidden),
        )
    }

    private fun mapCards(
        menuState: GameMenuState,
        sourceCards: List<GameMenuCardCatalog.Card>,
    ): List<MenuCard> = sourceCards.map { card ->
        MenuCard(
            viewId = card.action?.viewId ?: 0,
            label = card.label,
            contentDescription = card.contentDescription,
            iconRes = card.iconRes,
            shortcutEntry = card.shortcut,
            source = card,
            available = !card.requiresGamepad() ||
                menuState.isMouseEmulationAvailable,
        )
    }

    fun destroy() {
        listener = null
        editing = false
        state = null
        composeView.disposeComposition()
    }

    @Composable
    private fun GameMenuSurface(state: RenderState) {
        DraggableGameMenuPanel(
            onDismiss = { listener?.onDismissRequested() },
            dragEnabled = !editing,
        ) { contentModifier ->
            MenuContent(state = state, modifier = contentModifier)
        }
    }

    @Composable
    private fun MenuContent(state: RenderState, modifier: Modifier) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.game_menu_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
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
                IconButton(
                    onClick = {
                        if (editing) {
                            listener?.onCardLayoutChanged(
                                GameMenuCardConfiguration.toLayout(
                                    state.cards.map { it.source },
                                    state.hiddenCards.map { it.source },
                                ),
                            )
                        }
                        editing = !editing
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (editing) R.drawable.ic_m3_check
                            else R.drawable.ic_m3_edit,
                        ),
                        contentDescription = stringResource(R.string.game_menu_customize),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            ActionGrid(
                state = state,
                cards = if (editing) {
                    state.cards
                } else {
                    state.cards.filter { it.available }
                },
                visible = true,
            )
            if (editing) {
                GameMenuSectionLabel(
                    stringResource(R.string.game_menu_customize_hidden),
                )
                ActionGrid(
                    state = state,
                    cards = state.hiddenCards,
                    visible = false,
                )
            }

            Text(
                text = stringResource(R.string.game_menu_input_controls_title),
                style = MaterialTheme.typography.titleMedium,
            )
            NavigationGroup(
                entries = listOf(
                    NavigationEntry(R.id.bt_touch_list, R.string.game_menu_section_mouse_touch, R.drawable.ic_m3_mouse),
                    NavigationEntry(R.id.bt_touch_sensitivity, R.string.game_menu_section_touch_sensitivity, R.drawable.ic_m3_tune),
                    NavigationEntry(R.id.bt_quick_list, R.string.game_menu_section_shortcuts, R.drawable.ic_m3_keyboard),
                    NavigationEntry(R.id.bt_virtual_view, R.string.game_menu_section_virtual_controls, R.drawable.ic_m3_gamepad),
                ),
            )

        }
    }

    @Composable
    private fun ActionGrid(
        state: RenderState,
        cards: List<MenuCard>,
        visible: Boolean,
    ) {
        if (cards.isEmpty()) {
            Text(
                text = stringResource(
                    if (visible) R.string.game_menu_customize_visible_empty
                    else R.string.game_menu_customize_hidden_empty,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.chunked(ACTION_COLUMN_COUNT).forEachIndexed { rowIndex, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowCards.forEachIndexed { columnIndex, card ->
                        ActionTile(
                            state = state,
                            card = card,
                            visible = visible,
                            index = rowIndex * ACTION_COLUMN_COUNT + columnIndex,
                            itemCount = cards.size,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(ACTION_COLUMN_COUNT - rowCards.size) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionTile(
        state: RenderState,
        card: MenuCard,
        visible: Boolean,
        index: Int,
        itemCount: Int,
        modifier: Modifier,
    ) {
        var dragX by remember(card.source.id) { mutableFloatStateOf(0f) }
        var dragY by remember(card.source.id) { mutableFloatStateOf(0f) }
        var dragTarget by remember(card.source.id) { mutableIntStateOf(index) }
        val haptics = LocalHapticFeedback.current
        val editGesture = if (editing && visible) {
            Modifier.pointerInput(card.source.id, index, itemCount) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                        )
                    },
                    onDragEnd = {
                        if (dragTarget != index) {
                            moveVisibleCard(index, dragTarget)
                            haptics.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                            )
                        }
                        dragX = 0f
                        dragY = 0f
                        dragTarget = index
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                        dragTarget = index
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                        val columnDelta = (dragX / size.width.coerceAtLeast(1))
                            .roundToInt()
                        val rowDelta = (dragY / size.height.coerceAtLeast(1))
                            .roundToInt()
                        val target = (index + columnDelta +
                            rowDelta * ACTION_COLUMN_COUNT)
                            .coerceIn(0, itemCount - 1)
                        if (target != dragTarget) {
                            dragTarget = target
                            haptics.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                            )
                        }
                    },
                )
            }
        } else {
            Modifier
        }
        val active = isCardActive(state.menuState, card.viewId)
        Column(
            modifier = modifier
                .padding(horizontal = 2.dp)
                .zIndex(if (dragX != 0f || dragY != 0f) 1f else 0f)
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                }
                .then(editGesture),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .combinedClickable(
                            onClick = {
                                if (editing) {
                                    toggleCardVisibility(card, visible)
                                } else {
                                    listener?.onActionRequested(
                                        card.viewId,
                                        card.shortcutEntry,
                                    )
                                }
                            },
                            onLongClick = if (!editing && card.viewId == R.id.btn_performance) {
                                { listener?.onActionLongPressed(card.viewId) }
                            } else null,
                        ),
                    shape = CircleShape,
                    color = if (active && !editing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(card.iconRes),
                            contentDescription = card.contentDescription,
                            tint = if (active && !editing) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (editing) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (visible) R.drawable.ic_m3_remove_circle
                                else R.drawable.ic_m3_add,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }
            Text(
                text = card.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    private fun toggleCardVisibility(card: MenuCard, visible: Boolean) {
        val current = state ?: return
        state = if (visible) {
            current.copy(
                cards = current.cards - card,
                hiddenCards = current.hiddenCards + card,
            )
        } else {
            current.copy(
                cards = current.cards + card,
                hiddenCards = current.hiddenCards - card,
            )
        }
    }

    private fun moveVisibleCard(from: Int, to: Int) {
        val current = state ?: return
        if (from !in current.cards.indices || to !in current.cards.indices) return
        val reordered = current.cards.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        state = current.copy(cards = reordered)
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
                            painter = painterResource(R.drawable.ic_m3_chevron_right),
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

    private companion object {
        const val ACTION_COLUMN_COUNT = 4
    }
}
