package com.limelight.ui.gamemenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.settings.controller.ControllerSettings
import com.limelight.settings.controller.ControllerSettingsUpdate
import com.limelight.settings.input.InputSettings
import com.limelight.settings.input.InputSettingsUpdate

/** Compact, grouped live sensitivity editor for the current stream. */
class GameTouchSensitivityFragment : ComposeGameMenuDialogFragment() {
    private val host: GameMenuHost
        get() = (requireActivity() as GameMenuHostProvider).gameMenuHost

    @Composable
    override fun DialogContent() {
        var input by remember { mutableStateOf(host.state.inputSettings) }
        var controller by remember { mutableStateOf(host.state.controllerSettings) }
        fun updateInput(update: InputSettingsUpdate) {
            input = update.applyTo(input)
            host.applyInputSettingsUpdate(update)
        }
        fun updateController(update: ControllerSettingsUpdate) {
            controller = update.applyTo(controller)
            host.applyControllerSettingsUpdate(update)
        }
        GameMenuComposePage(
            title = stringResource(R.string.game_menu_touch_sensitivity_title),
            onBack = ::dismiss,
            action = {
                TextButton(onClick = {
                    updateInput(InputSettingsUpdate.resetSensitivity())
                    updateController(ControllerSettingsUpdate.mouseSensitivityPercent(100))
                }) { Text(stringResource(R.string.game_menu_reset)) }
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
                    GameMenuControlGroup(stringResource(R.string.game_menu_group_touchpad_pointer)) {
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_horizontal), input.touchpadPointerSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.touchpadPointerSensitivityX(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_vertical), input.touchpadPointerSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.touchpadPointerSensitivityY(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_group_virtual_touchpad)) {
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_horizontal), input.virtualTouchpadSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.virtualTouchpadSensitivityX(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_vertical), input.virtualTouchpadSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.virtualTouchpadSensitivityY(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_group_external_touchpad)) {
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_horizontal), input.externalTouchpadSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.externalTouchpadSensitivityX(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_axis_vertical), input.externalTouchpadSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.externalTouchpadSensitivityY(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_scroll_amount), input.externalTouchpadScrollAmount, 1, 30) {
                            updateInput(InputSettingsUpdate.externalTouchpadScrollAmount(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup(stringResource(R.string.game_menu_group_mouse)) {
                        GameMenuSliderRow(stringResource(R.string.game_menu_controller_mouse), controller.mouseSensitivityPercent, 10, 300) {
                            updateController(ControllerSettingsUpdate.mouseSensitivityPercent(it))
                        }
                        GameMenuSliderRow(stringResource(R.string.game_menu_scroll_amount), input.mouseWheelScrollAmount, 1, 30) {
                            updateInput(InputSettingsUpdate.mouseWheelScrollAmount(it))
                        }
                    }
                }
            }
        }
    }

}
