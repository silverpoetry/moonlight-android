package com.limelight.ui.gamemenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
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
        fun onActionRequested(
            viewId: Int,
            shortcutEntry: Any?,
            dismissMenuBeforeExecution: Boolean,
        )
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
        val dismissesMenuBeforeExecution: Boolean,
    )

    private data class RenderState(
        val menuState: GameMenuState,
        val cards: List<MenuCard>,
        val hiddenCards: List<MenuCard>,
    )

    private data class CardDragState(
        val cardId: String,
        val offset: Offset,
    )

    private data class GridReorder(
        val targetIndex: Int,
        val rebasedOffset: Offset,
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
            dismissesMenuBeforeExecution =
                card.dismissesMenuBeforeExecution(),
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
        val haptics = LocalHapticFeedback.current
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
                        haptics.performHapticFeedback(
                            HapticFeedbackType.VirtualKey,
                        )
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
        var dragState by remember(visible) {
            mutableStateOf<CardDragState?>(null)
        }
        val haptics = LocalHapticFeedback.current
        val density = LocalDensity.current
        val rowCount = (cards.size + ACTION_COLUMN_COUNT - 1) /
            ACTION_COLUMN_COUNT
        val gridHeight = ACTION_TILE_HEIGHT * rowCount +
            ACTION_GRID_VERTICAL_SPACING * (rowCount - 1)

        LazyVerticalGrid(
            columns = GridCells.Fixed(ACTION_COLUMN_COUNT),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(
                ACTION_GRID_HORIZONTAL_SPACING,
            ),
            verticalArrangement = Arrangement.spacedBy(
                ACTION_GRID_VERTICAL_SPACING,
            ),
        ) {
            itemsIndexed(
                items = cards,
                key = { _, card -> card.source.id },
            ) { _, card ->
                val activeDrag = dragState
                ActionTile(
                    state = state,
                    card = card,
                    visible = visible,
                    isDragging = activeDrag?.cardId == card.source.id,
                    dragOffset = if (activeDrag?.cardId == card.source.id) {
                        activeDrag.offset
                    } else {
                        Offset.Zero
                    },
                    onDragStarted = { cardId ->
                        dragState = CardDragState(cardId, Offset.Zero)
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress,
                        )
                    },
                    onDragBy = { cardId, amount, itemSize ->
                        val active = dragState
                        if (active == null || active.cardId != cardId) {
                            return@ActionTile
                        }
                        val nextOffset = active.offset + amount
                        val currentCards = this@GameMenuComposeRenderer
                            .state
                            ?.cards
                            ?: return@ActionTile
                        val currentIndex = currentCards.indexOfFirst {
                            it.source.id == cardId
                        }
                        val reorder = resolveGridReorder(
                            currentIndex = currentIndex,
                            itemCount = currentCards.size,
                            dragOffset = nextOffset,
                            itemSize = itemSize,
                            density = density,
                        )
                        if (reorder == null) {
                            dragState = active.copy(offset = nextOffset)
                        } else {
                            moveVisibleCard(currentIndex, reorder.targetIndex)
                            dragState = active.copy(
                                offset = reorder.rebasedOffset,
                            )
                            haptics.performHapticFeedback(
                                HapticFeedbackType.TextHandleMove,
                            )
                        }
                    },
                    onDragFinished = { cardId ->
                        if (dragState?.cardId == cardId) {
                            dragState = null
                        }
                    },
                    modifier = (if (activeDrag?.cardId == card.source.id) {
                        Modifier
                    } else {
                        Modifier.animateItem()
                    }).fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun ActionTile(
        state: RenderState,
        card: MenuCard,
        visible: Boolean,
        isDragging: Boolean,
        dragOffset: Offset,
        onDragStarted: (String) -> Unit,
        onDragBy: (String, Offset, IntSize) -> Unit,
        onDragFinished: (String) -> Unit,
        modifier: Modifier,
    ) {
        val currentOnDragStarted by rememberUpdatedState(onDragStarted)
        val currentOnDragBy by rememberUpdatedState(onDragBy)
        val currentOnDragFinished by rememberUpdatedState(onDragFinished)
        val haptics = LocalHapticFeedback.current
        val editGesture = if (editing && visible) {
            Modifier.pointerInput(card.source.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        currentOnDragStarted(card.source.id)
                    },
                    onDragEnd = {
                        currentOnDragFinished(card.source.id)
                    },
                    onDragCancel = {
                        currentOnDragFinished(card.source.id)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        currentOnDragBy(card.source.id, amount, size)
                    },
                )
            }
        } else {
            Modifier
        }
        val active = isCardActive(state.menuState, card.viewId)
        Column(
            modifier = modifier
                .height(ACTION_TILE_HEIGHT)
                .padding(horizontal = 2.dp)
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                }
                .then(editGesture),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (editing) {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.SegmentTick,
                                    )
                                    toggleCardVisibility(card, visible)
                                } else {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.VirtualKey,
                                    )
                                    listener?.onActionRequested(
                                        card.viewId,
                                        card.shortcutEntry,
                                        card.dismissesMenuBeforeExecution,
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

    private fun resolveGridReorder(
        currentIndex: Int,
        itemCount: Int,
        dragOffset: Offset,
        itemSize: IntSize,
        density: androidx.compose.ui.unit.Density,
    ): GridReorder? {
        if (currentIndex !in 0 until itemCount) return null

        val cellWidth = itemSize.width + with(density) {
            ACTION_GRID_HORIZONTAL_SPACING.roundToPx()
        }
        val cellHeight = itemSize.height + with(density) {
            ACTION_GRID_VERTICAL_SPACING.roundToPx()
        }
        val columnDelta = (dragOffset.x / cellWidth.coerceAtLeast(1))
            .roundToInt()
        val rowDelta = (dragOffset.y / cellHeight.coerceAtLeast(1))
            .roundToInt()
        val targetIndex = (currentIndex + columnDelta +
            rowDelta * ACTION_COLUMN_COUNT)
            .coerceIn(0, itemCount - 1)
        if (targetIndex == currentIndex) return null

        val fromColumn = currentIndex % ACTION_COLUMN_COUNT
        val toColumn = targetIndex % ACTION_COLUMN_COUNT
        val fromRow = currentIndex / ACTION_COLUMN_COUNT
        val toRow = targetIndex / ACTION_COLUMN_COUNT
        return GridReorder(
            targetIndex = targetIndex,
            rebasedOffset = dragOffset - Offset(
                x = (toColumn - fromColumn) * cellWidth.toFloat(),
                y = (toRow - fromRow) * cellHeight.toFloat(),
            ),
        )
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
                        listener?.onActionRequested(
                            entry.viewId,
                            null,
                            false,
                        )
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
        val ACTION_TILE_HEIGHT = 82.dp
        val ACTION_GRID_HORIZONTAL_SPACING = 6.dp
        val ACTION_GRID_VERTICAL_SPACING = 10.dp
    }
}
