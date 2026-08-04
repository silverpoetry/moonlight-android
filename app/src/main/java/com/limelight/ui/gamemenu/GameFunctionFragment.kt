package com.limelight.ui.gamemenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.limelight.R

/** Compact Material 3 surface for Windows-side session actions. */
class GameFunctionFragment : ComposeGameMenuDialogFragment() {
    fun interface ActionSelectionListener {
        fun onActionSelected(title: String, index: Int)
    }

    private data class Action(
        val titleRes: Int,
        val iconRes: Int,
        val index: Int,
    )

    private var title: String? = null
    private var actionSelectionListener: ActionSelectionListener? = null

    fun setTitle(value: String) {
        title = value
    }

    fun setActionSelectionListener(listener: ActionSelectionListener?) {
        actionSelectionListener = listener
    }

    @Composable
    override fun DialogContent() {
        GameMenuComposePage(
            title = title ?: stringResource(R.string.game_menu_action_windows),
            onBack = ::dismiss,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp,
                ),
            ) {
                actionGroup(
                    title = "窗口与工具",
                    actions = listOf(
                        Action(R.string.game_menu_task_manager, R.drawable.ic_m3_task_manager, 4),
                        Action(R.string.game_menu_open_clipboard, R.drawable.ic_m3_content_paste, 6),
                        Action(R.string.game_menu_send_clipboard, R.drawable.ic_m3_content_copy, 5),
                        Action(R.string.game_menu_windows_settings, R.drawable.ic_m3_settings, 7),
                        Action(R.string.game_menu_this_pc, R.drawable.ic_m3_folder, 8),
                        Action(R.string.game_menu_mobility_center, R.drawable.ic_m3_tune, 9),
                        Action(R.string.game_menu_projection_mode, R.drawable.ic_m3_cast, 10),
                    ),
                )
                actionGroup(
                    title = "显示器",
                    actions = listOf(
                        Action(R.string.game_menu_display_1, R.drawable.ic_m3_display, 11),
                        Action(R.string.game_menu_display_2, R.drawable.ic_m3_display, 12),
                        Action(R.string.game_menu_display_3, R.drawable.ic_m3_display, 13),
                        Action(R.string.game_menu_display_4, R.drawable.ic_m3_display, 14),
                    ),
                )
                actionGroup(
                    title = "电源",
                    actions = listOf(
                        Action(R.string.game_menu_logout, R.drawable.ic_m3_logout, 0),
                        Action(R.string.game_menu_sleep, R.drawable.ic_m3_bedtime, 2),
                        Action(R.string.game_menu_restart, R.drawable.ic_m3_restart, 3),
                        Action(R.string.game_menu_shutdown, R.drawable.ic_m3_power, 1),
                    ),
                )
            }
        }
    }

    private fun androidx.compose.foundation.lazy.LazyListScope.actionGroup(
        title: String,
        actions: List<Action>,
    ) {
        item {
            GameMenuSectionLabel(title)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column {
                    actions.forEachIndexed { position, action ->
                        val label = stringResource(action.titleRes)
                        GameMenuActionRow(
                            title = label,
                            iconRes = action.iconRes,
                            onClick = {
                                actionSelectionListener?.onActionSelected(label, action.index)
                            },
                        )
                        if (position != actions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 44.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
