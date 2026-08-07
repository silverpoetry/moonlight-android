package com.limelight.ui.gamemenu

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode
import com.limelight.settings.virtualcontrols.VirtualControlSettings
import com.limelight.settings.virtualcontrols.VirtualControlSettingsUpdate

/** Live Material 3 editor for virtual gamepad and virtual-key presentation. */
class GameMenuVirtualViewFragment : ComposeGameMenuDialogFragment() {
    private val host: GameMenuHost
        get() = (requireActivity() as GameMenuHostProvider).gameMenuHost

    @Composable
    override fun DialogContent() {
        var settings by remember { mutableStateOf(host.state.virtualControlSettings) }
        var gamepadMode by remember { mutableStateOf(host.state.virtualGamepadEditMode) }
        var keyMode by remember { mutableStateOf(host.state.virtualKeysEditMode) }
        var gamepadHaptics by remember {
            mutableStateOf(host.state.controllerSettings.isOnscreenRumbleEnabled)
        }

        fun update(update: VirtualControlSettingsUpdate<*>) {
            settings = update.applyTo(settings)
            host.applyVirtualControlSettingsUpdate(update)
        }

        GameMenuComposePage(
            title = stringResource(R.string.game_menu_virtual_controls_title),
            onBack = ::dismiss,
            action = {
                TextButton(onClick = { host.updateVirtualView() }) {
                    Text(stringResource(R.string.game_menu_refresh_layout))
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 10.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_virtual_gamepad)) {
                        ModeSelector(
                            selected = gamepadMode,
                            onSelected = {
                                gamepadMode = it
                                host.setVirtualGamepadEditMode(it)
                            },
                        )
                        LayoutSelector(
                            names = resources.getStringArray(R.array.gamepad_layout_names),
                            values = resources.getStringArray(R.array.gamepad_layout_values),
                            selected = settings.gamepadLayoutId,
                            onSelected = {
                                update(VirtualControlSettingsUpdate.gamepadLayoutId(it))
                            },
                        )
                        GameMenuSwitchRow(stringResource(R.string.game_menu_button_haptics), gamepadHaptics) {
                            gamepadHaptics = it
                            host.setOnscreenControllerRumbleEnabled(it)
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_control_opacity), settings.controlOpacityPercent, 0, 100, "%") {
                            update(VirtualControlSettingsUpdate.controlOpacityPercent(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_control_scale), settings.gamepadScalePercent, 20, 180, "%") {
                            update(VirtualControlSettingsUpdate.gamepadScalePercent(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_virtual_keys)) {
                        ModeSelector(
                            selected = keyMode,
                            onSelected = {
                                keyMode = it
                                host.setVirtualKeysEditMode(it)
                            },
                        )
                        LayoutSelector(
                            names = resources.getStringArray(R.array.keyboard_layout_names),
                            values = resources.getStringArray(R.array.keyboard_layout_values),
                            selected = settings.keyboardLayoutId,
                            onSelected = {
                                update(VirtualControlSettingsUpdate.keyboardLayoutId(it))
                            },
                        )
                        GameMenuSwitchRow(stringResource(R.string.game_menu_button_haptics), settings.isKeyboardHapticsEnabled) {
                            update(VirtualControlSettingsUpdate.keyboardHapticsEnabled(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_keyboard_opacity), settings.keyboardOpacityPercent, 0, 100, "%") {
                            update(VirtualControlSettingsUpdate.keyboardOpacityPercent(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_keyboard_height), settings.keyboardHeightDp, 100, 400, " dp") {
                            update(VirtualControlSettingsUpdate.keyboardHeightDp(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_button_color)) {
                        ColorSelector(settings) {
                            update(VirtualControlSettingsUpdate.normalColor(it))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModeSelector(
        selected: VirtualControlEditMode,
        onSelected: (VirtualControlEditMode) -> Unit,
    ) {
        val haptics = LocalHapticFeedback.current
        val options = listOf(
            stringResource(R.string.game_menu_edit_mode_default) to VirtualControlEditMode.ACTIVE,
            stringResource(R.string.game_menu_edit_mode_layout) to VirtualControlEditMode.MOVE_BUTTONS,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, mode) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = {
                        if (selected != mode) {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.SegmentTick,
                            )
                            onSelected(mode)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }

    @Composable
    private fun LayoutSelector(
        names: Array<String>,
        values: Array<String>,
        selected: String,
        onSelected: (String) -> Unit,
    ) {
        val haptics = LocalHapticFeedback.current
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.game_menu_layout_scheme), style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEachIndexed { index, value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = {
                            if (selected != value) {
                                haptics.performHapticFeedback(
                                    HapticFeedbackType.SegmentTick,
                                )
                                onSelected(value)
                            }
                        },
                        label = {
                            Text(
                                names.getOrElse(index) { value },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun ColorSelector(
        settings: VirtualControlSettings,
        onSelected: (Int) -> Unit,
    ) {
        val haptics = LocalHapticFeedback.current
        val options = listOf(
            stringResource(R.string.game_menu_color_black) to 0xF0000000.toInt(),
            stringResource(R.string.game_menu_color_white) to 0xF0FFFFFF.toInt(),
            stringResource(R.string.game_menu_color_gray) to 0xFF888888.toInt(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, color) ->
                SegmentedButton(
                    selected = settings.normalColor == color,
                    onClick = {
                        if (settings.normalColor != color) {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.SegmentTick,
                            )
                            onSelected(color)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}
