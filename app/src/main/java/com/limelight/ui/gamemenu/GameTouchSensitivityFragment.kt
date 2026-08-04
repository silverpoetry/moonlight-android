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
import androidx.compose.ui.unit.dp
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
            title = "触控灵敏度",
            onBack = ::dismiss,
            action = {
                TextButton(onClick = {
                    updateInput(InputSettingsUpdate.resetSensitivity())
                    updateController(ControllerSettingsUpdate.mouseSensitivityPercent(100))
                }) { Text("重置") }
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
                    GameMenuControlGroup("触控板指针") {
                        GameMenuSliderRow("水平", input.touchpadPointerSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.touchpadPointerSensitivityX(it))
                        }
                        GameMenuSliderRow("垂直", input.touchpadPointerSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.touchpadPointerSensitivityY(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup("虚拟触控板") {
                        GameMenuSliderRow("水平", input.virtualTouchpadSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.virtualTouchpadSensitivityX(it))
                        }
                        GameMenuSliderRow("垂直", input.virtualTouchpadSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.virtualTouchpadSensitivityY(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup("外接触控板") {
                        GameMenuSliderRow("水平", input.externalTouchpadSensitivityX, 10, 300) {
                            updateInput(InputSettingsUpdate.externalTouchpadSensitivityX(it))
                        }
                        GameMenuSliderRow("垂直", input.externalTouchpadSensitivityY, 10, 300) {
                            updateInput(InputSettingsUpdate.externalTouchpadSensitivityY(it))
                        }
                        GameMenuSliderRow("滚动量", input.externalTouchpadScrollAmount, 1, 30) {
                            updateInput(InputSettingsUpdate.externalTouchpadScrollAmount(it))
                        }
                    }
                }
                item {
                    GameMenuControlGroup("鼠标") {
                        GameMenuSliderRow("手柄鼠标", controller.mouseSensitivityPercent, 10, 300) {
                            updateController(ControllerSettingsUpdate.mouseSensitivityPercent(it))
                        }
                        GameMenuSliderRow("滚轮量", input.mouseWheelScrollAmount, 1, 30) {
                            updateInput(InputSettingsUpdate.mouseWheelScrollAmount(it))
                        }
                    }
                }
            }
        }
    }

}
