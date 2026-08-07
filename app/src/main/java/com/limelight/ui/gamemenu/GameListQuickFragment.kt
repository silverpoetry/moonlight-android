package com.limelight.ui.gamemenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.shortcuts.GameMenuShortcut
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean
import com.limelight.utils.UiHelper
import com.limelight.utils.UiToast

/** Material 3 shortcut browser; persistence remains owned by GameMenuHost. */
class GameListQuickFragment : ComposeGameMenuDialogFragment() {
    fun interface ShortcutSelectedListener {
        fun onShortcutSelected(shortcut: GameMenuShortcut)
    }

    private val host: GameMenuHost
        get() = (requireActivity() as GameMenuHostProvider).gameMenuHost

    private var title: String? = null
    private var hideBuiltInShortcuts = false
    private var shortcutSelectedListener: ShortcutSelectedListener? = null
    private var shortcutsChangedListener: Runnable? = null

    fun setTitle(value: String) {
        title = value
    }

    fun setHideBuiltInShortcuts(hide: Boolean) {
        hideBuiltInShortcuts = hide
    }

    fun setOnShortcutSelectedListener(listener: ShortcutSelectedListener?) {
        shortcutSelectedListener = listener
    }

    fun setOnShortcutsChangedListener(listener: Runnable?) {
        shortcutsChangedListener = listener
    }

    @Composable
    override fun DialogContent() {
        var shortcuts by remember { mutableStateOf(loadShortcuts()) }
        var pendingDelete by remember { mutableStateOf<GameMenuShortcut?>(null) }

        GameMenuComposePage(
            title = title ?: stringResource(R.string.game_menu_shortcuts_title),
            onBack = ::dismiss,
            action = {
                TextButton(onClick = ::showShortcutEditor) {
                    Text(stringResource(R.string.game_menu_shortcuts_add))
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp,
                ),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.game_menu_shortcuts_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column {
                            shortcuts.forEachIndexed { index, shortcut ->
                                GameMenuActionRow(
                                    title = shortcut.name,
                                    summary = shortcut.description.takeIf { it.isNotBlank() },
                                    iconRes = R.drawable.ic_m3_keyboard,
                                    trailing = if (shortcut.isEditable) {
                                        {
                                            IconButton(onClick = { pendingDelete = shortcut }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_m3_delete),
                                                    contentDescription = stringResource(R.string.keyboard_delete),
                                                )
                                            }
                                        }
                                    } else null,
                                    onClick = {
                                        shortcutSelectedListener?.onShortcutSelected(shortcut)
                                    },
                                )
                                if (index != shortcuts.lastIndex) {
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

        pendingDelete?.let { shortcut ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(shortcut.name) },
                text = { Text(stringResource(R.string.keyboard_delete_confirmation)) },
                confirmButton = {
                    TextButton(onClick = {
                        if (host.deleteGameMenuShortcut(shortcut.id)) {
                            shortcuts = loadShortcuts()
                            shortcutsChangedListener?.run()
                        } else {
                            showPersistenceFailure()
                        }
                        pendingDelete = null
                    }) { Text(stringResource(R.string.keyboard_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.keyboard_cancel))
                    }
                },
            )
        }
    }

    private fun loadShortcuts(): List<GameMenuShortcut> =
        GameMenuShortcutCatalog.loadShortcuts(
            host.state.shortcuts,
            !hideBuiltInShortcuts,
            requireContext(),
        )

    private fun showShortcutEditor() {
        val editorWidthPx = UiHelper.dpToPx(requireContext(), 364f)
        val fragment = GameKeyboardUpdateFragment().apply {
            setWidth(editorWidthPx)
            setTitle(R.string.keyboard_shortcut_setup_title)
            setKeyFrom(1)
            setSelectionListener(::saveShortcut)
        }
        fragment.show(parentFragmentManager)
    }

    private fun saveShortcut(bean: GameMenuQuickBean) {
        try {
            if (!host.saveGameMenuShortcut(GameMenuShortcutMapper.fromEditor(bean))) {
                showPersistenceFailure()
                return
            }
            shortcutsChangedListener?.run()
            dismiss()
        } catch (_: IllegalArgumentException) {
            showPersistenceFailure()
        }
    }

    private fun showPersistenceFailure() {
        UiToast.makeText(
            requireActivity(),
            R.string.keyboard_shortcut_save_failed,
            UiToast.LENGTH_SHORT,
        ).show()
    }
}
