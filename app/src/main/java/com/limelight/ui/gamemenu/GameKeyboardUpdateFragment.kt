package com.limelight.ui.gamemenu

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.shortcuts.GameMenuShortcutIds
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean
import com.limelight.utils.UiToast
import com.limelight.virtualcontrols.layout.VirtualControlElementIds

/** Compose shortcut and virtual-control element editor. */
class GameKeyboardUpdateFragment : ComposeGameMenuDialogFragment() {
    fun interface SelectionListener {
        fun onSelected(item: GameMenuQuickBean)
    }

    private data class KeyEntry(val code: Int, val label: String)

    @StringRes
    private var titleRes = R.string.keyboard_chord_title
    private var keyFrom = KEY_FROM_BUTTON_LIST
    private var selectionListener: SelectionListener? = null
    private val chordSelection = KeyChordSelection(MAX_CHORD_KEYS)

    fun setTitle(@StringRes value: Int) {
        titleRes = value
    }

    fun setKeyFrom(value: Int) {
        keyFrom = value
    }

    fun setSelectionListener(listener: SelectionListener?) {
        selectionListener = listener
    }

    @Composable
    override fun DialogContent() {
        var name by remember { mutableStateOf("") }
        var chordLabel by remember { mutableStateOf(chordSelection.displayName) }
        var selectedTab by remember { mutableIntStateOf(0) }
        val haptics = LocalHapticFeedback.current
        val tabs = if (keyFrom == KEY_FROM_BUTTON_LIST) {
            listOf(
                R.string.keyboard_full_pc,
                R.string.keyboard_numeric,
                R.string.keyboard_mouse_and_wheel,
                R.string.keyboard_function_keys,
            )
        } else {
            listOf(R.string.keyboard_full_pc, R.string.keyboard_numeric)
        }

        GameMenuComposePage(
            title = getString(titleRes),
            onBack = ::dismiss,
            action = {
                TextButton(onClick = {
                    chordSelection.clear()
                    chordLabel = ""
                    name = ""
                }) {
                    Text(getString(R.string.keyboard_clear))
                }
                TextButton(onClick = { saveChord(name) }) {
                    Text(getString(R.string.keyboard_save))
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(getString(R.string.keyboard_key_name)) },
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = chordLabel.ifEmpty {
                            getString(R.string.keyboard_chord_value_hint)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, labelRes ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = {
                                if (selectedTab != index) {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.SegmentTick,
                                    )
                                    selectedTab = index
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                        ) {
                            Text(
                                text = getString(labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                when (selectedTab) {
                    0 -> KeyGrid(fullKeyboardKeys()) { code, label ->
                        if (chordSelection.add(code.toString(), label)) {
                            chordLabel = chordSelection.displayName
                        } else {
                            showToast(R.string.keyboard_maximum_keys)
                        }
                    }
                    1 -> KeyGrid(numericKeyboardKeys()) { code, label ->
                        if (chordSelection.add(code.toString(), label)) {
                            chordLabel = chordSelection.displayName
                        } else {
                            showToast(R.string.keyboard_maximum_keys)
                        }
                    }
                    2 -> PresetGrid(KeyboardPresetFactory.createMouseAndTouchItems(requireContext()))
                    3 -> PresetGrid(KeyboardPresetFactory.createFunctionItems(requireContext()))
                }
            }
        }
    }

    @Composable
    private fun KeyGrid(keys: List<KeyEntry>, onSelected: (Int, String) -> Unit) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(keys, key = { it.code }) { key ->
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { onSelected(key.code, key.label) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = key.label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 9.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun PresetGrid(entries: List<GameMenuQuickBean>) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { "${it.name}:${it.desc}" }) { entry ->
                Card(
                    modifier = Modifier.clickable { selectPreset(entry) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp),
                    )
                    Text(
                        text = entry.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp, top = 2.dp, end = 10.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }

    private fun saveChord(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            showToast(R.string.keyboard_name_required)
            return
        }
        if (chordSelection.isEmpty) {
            showToast(R.string.keyboard_chord_required)
            return
        }
        val item = GameMenuQuickBean().apply {
            this.name = name
            id = if (keyFrom == KEY_FROM_SHORTCUT_LIST) {
                GameMenuShortcutIds.newCustom()
            } else {
                VirtualControlElementIds.newId()
            }
            btnType = BUTTON_TYPE_KEYBOARD
            codes = chordSelection.encodedKeyCodes
            desc = chordSelection.displayName
        }
        selectionListener?.onSelected(item)
        dismiss()
    }

    private fun selectPreset(item: GameMenuQuickBean) {
        item.id = VirtualControlElementIds.newId()
        selectionListener?.onSelected(item)
        dismiss()
    }

    private fun showToast(@StringRes messageRes: Int) {
        UiToast.makeText(requireActivity(), messageRes, UiToast.LENGTH_SHORT).show()
    }

    private fun fullKeyboardKeys(): List<KeyEntry> = buildList {
        add(KeyEntry(KeyEvent.KEYCODE_ESCAPE, "Esc"))
        repeat(12) { index ->
            add(KeyEntry(KeyEvent.KEYCODE_F1 + index, "F${index + 1}"))
        }
        add(KeyEntry(KeyEvent.KEYCODE_INSERT, "Ins"))
        add(KeyEntry(KeyEvent.KEYCODE_FORWARD_DEL, "Del"))
        add(KeyEntry(KeyEvent.KEYCODE_GRAVE, "`"))
        listOf(
            KeyEvent.KEYCODE_1 to "1", KeyEvent.KEYCODE_2 to "2",
            KeyEvent.KEYCODE_3 to "3", KeyEvent.KEYCODE_4 to "4",
            KeyEvent.KEYCODE_5 to "5", KeyEvent.KEYCODE_6 to "6",
            KeyEvent.KEYCODE_7 to "7", KeyEvent.KEYCODE_8 to "8",
            KeyEvent.KEYCODE_9 to "9", KeyEvent.KEYCODE_0 to "0",
            KeyEvent.KEYCODE_MINUS to "-", KeyEvent.KEYCODE_EQUALS to "=",
            KeyEvent.KEYCODE_DEL to "⌫", KeyEvent.KEYCODE_TAB to "Tab",
        ).forEach { add(KeyEntry(it.first, it.second)) }
        "QWERTYUIOP".forEach { add(KeyEntry(KeyEvent.keyCodeFromString("KEYCODE_$it"), it.toString())) }
        add(KeyEntry(KeyEvent.KEYCODE_LEFT_BRACKET, "["))
        add(KeyEntry(KeyEvent.KEYCODE_RIGHT_BRACKET, "]"))
        add(KeyEntry(KeyEvent.KEYCODE_BACKSLASH, "\\"))
        add(KeyEntry(KeyEvent.KEYCODE_CAPS_LOCK, "Caps"))
        "ASDFGHJKL".forEach { add(KeyEntry(KeyEvent.keyCodeFromString("KEYCODE_$it"), it.toString())) }
        add(KeyEntry(KeyEvent.KEYCODE_SEMICOLON, ";"))
        add(KeyEntry(KeyEvent.KEYCODE_APOSTROPHE, "'"))
        add(KeyEntry(KeyEvent.KEYCODE_ENTER, "Enter"))
        add(KeyEntry(KeyEvent.KEYCODE_SHIFT_LEFT, "Shift"))
        "ZXCVBNM".forEach { add(KeyEntry(KeyEvent.keyCodeFromString("KEYCODE_$it"), it.toString())) }
        add(KeyEntry(KeyEvent.KEYCODE_COMMA, ","))
        add(KeyEntry(KeyEvent.KEYCODE_PERIOD, "."))
        add(KeyEntry(KeyEvent.KEYCODE_SLASH, "/"))
        add(KeyEntry(KeyEvent.KEYCODE_SHIFT_RIGHT, "RShift"))
        add(KeyEntry(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl"))
        add(KeyEntry(KeyEvent.KEYCODE_META_LEFT, "Win"))
        add(KeyEntry(KeyEvent.KEYCODE_ALT_LEFT, "Alt"))
        add(KeyEntry(KeyEvent.KEYCODE_SPACE, "Space"))
        add(KeyEntry(KeyEvent.KEYCODE_ALT_RIGHT, "RAlt"))
        add(KeyEntry(KeyEvent.KEYCODE_META_RIGHT, "RWin"))
        add(KeyEntry(KeyEvent.KEYCODE_CTRL_RIGHT, "RCtrl"))
        add(KeyEntry(KeyEvent.KEYCODE_DPAD_UP, "↑"))
        add(KeyEntry(KeyEvent.KEYCODE_DPAD_LEFT, "←"))
        add(KeyEntry(KeyEvent.KEYCODE_DPAD_DOWN, "↓"))
        add(KeyEntry(KeyEvent.KEYCODE_DPAD_RIGHT, "→"))
    }

    private fun numericKeyboardKeys(): List<KeyEntry> = listOf(
        KeyEntry(KeyEvent.KEYCODE_NUM_LOCK, "Num"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_7, "7"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_8, "8"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_9, "9"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_DIVIDE, "÷"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_4, "4"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_5, "5"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_6, "6"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, "×"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_1, "1"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_2, "2"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_3, "3"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, "-"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_0, "0"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_DOT, "."),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_ENTER, "Enter"),
        KeyEntry(KeyEvent.KEYCODE_NUMPAD_ADD, "+"),
        KeyEntry(KeyEvent.KEYCODE_PAGE_UP, "PgUp"),
        KeyEntry(KeyEvent.KEYCODE_PAGE_DOWN, "PgDn"),
    )

    private companion object {
        const val MAX_CHORD_KEYS = 5
        const val KEY_FROM_BUTTON_LIST = 0
        const val KEY_FROM_SHORTCUT_LIST = 1
        const val BUTTON_TYPE_KEYBOARD = 4
    }
}
