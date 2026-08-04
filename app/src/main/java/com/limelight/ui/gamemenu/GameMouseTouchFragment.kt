package com.limelight.ui.gamemenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.limelight.R

/** Pointer controls that can be applied without reconnecting the stream. */
class GameMouseTouchFragment : ComposeGameMenuDialogFragment() {
    private val host: GameMenuHost
        get() = (requireActivity() as GameMenuHostProvider).gameMenuHost

    @Composable
    override fun DialogContent() {
        val modeNames = resources.getStringArray(R.array.mouse_mode_names)
        val summaries = intArrayOf(
            R.string.game_menu_pointer_direct_touch_summary,
            R.string.game_menu_pointer_mouse_summary,
            R.string.game_menu_pointer_touchpad_summary,
            R.string.game_menu_pointer_disabled_summary,
        )
        var selectedMode by remember {
            mutableIntStateOf(host.state.inputSettings.touchModePreferenceValue)
        }

        GameMenuComposePage(
            title = stringResource(R.string.game_menu_pointer_title),
            onBack = ::dismiss,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 12.dp),
            ) {
                item {
                    GameMenuSectionLabel(
                        stringResource(R.string.game_menu_pointer_mode_section),
                    )
                    ModeGroup(
                        indices = 0..3,
                        modeNames = modeNames,
                        summaries = summaries,
                        selectedMode = selectedMode,
                    ) { mode ->
                        selectedMode = mode
                        host.switchMouseModel(mode)
                    }
                }
            }
        }
    }

    @Composable
    private fun ModeGroup(
        indices: IntRange,
        modeNames: Array<String>,
        summaries: IntArray,
        selectedMode: Int,
        onModeSelected: (Int) -> Unit,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            androidx.compose.foundation.layout.Column {
                indices.forEachIndexed { position, mode ->
                    GameMenuSelectableRow(
                        title = modeNames[mode],
                        summary = stringResource(summaries[mode]),
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                    )
                    if (position != indices.count() - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}
