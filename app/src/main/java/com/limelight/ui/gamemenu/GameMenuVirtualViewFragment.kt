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
            title = "虚拟控制",
            onBack = ::dismiss,
            action = {
                TextButton(onClick = { host.updateVirtualView() }) {
                    Text("刷新布局")
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
                    GameMenuControlGroup("虚拟手柄") {
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
                        GameMenuSwitchRow("按键振动", gamepadHaptics) {
                            gamepadHaptics = it
                            host.setOnscreenControllerRumbleEnabled(it)
                        }
                        GameMenuSliderRow("整体透明度", settings.controlOpacityPercent, 0, 100, "%") {
                            update(VirtualControlSettingsUpdate.controlOpacityPercent(it))
                        }
                        GameMenuSliderRow("整体缩放", settings.gamepadScalePercent, 20, 180, "%") {
                            update(VirtualControlSettingsUpdate.gamepadScalePercent(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup("虚拟按键") {
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
                        GameMenuSwitchRow("按键振动", settings.isKeyboardHapticsEnabled) {
                            update(VirtualControlSettingsUpdate.keyboardHapticsEnabled(it))
                        }
                        GameMenuSliderRow("键盘透明度", settings.keyboardOpacityPercent, 0, 100, "%") {
                            update(VirtualControlSettingsUpdate.keyboardOpacityPercent(it))
                        }
                        GameMenuSliderRow("键盘高度", settings.keyboardHeightDp, 100, 400, " dp") {
                            update(VirtualControlSettingsUpdate.keyboardHeightDp(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup("按键颜色") {
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
        val options = listOf(
            "默认" to VirtualControlEditMode.ACTIVE,
            "编辑布局" to VirtualControlEditMode.MOVE_BUTTONS,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, mode) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("布局方案", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEachIndexed { index, value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelected(value) },
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
        val options = listOf(
            "黑色" to 0xF0000000.toInt(),
            "白色" to 0xF0FFFFFF.toInt(),
            "灰色" to 0xFF888888.toInt(),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, color) ->
                SegmentedButton(
                    selected = settings.normalColor == color,
                    onClick = { onSelected(color) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}
