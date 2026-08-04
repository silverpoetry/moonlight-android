package com.limelight.ui.gamemenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.nvstream.input.ControllerPacket
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean
import com.limelight.virtualcontrols.layout.VirtualControlElementIds

/** Material 3 element picker used by the virtual-controller layout editor. */
class GamePadAddFragment : ComposeGameMenuDialogFragment() {
    fun interface GamepadElementSelectionListener {
        fun onGamepadElementSelected(bean: GameMenuQuickBean)
    }

    private var title: String? = null
    private var elementSelectionListener: GamepadElementSelectionListener? = null

    fun setTitle(value: String?) {
        title = value
    }

    fun setElementSelectionListener(listener: GamepadElementSelectionListener?) {
        elementSelectionListener = listener
    }

    @Composable
    override fun DialogContent() {
        val entries = remember { createEntries() }
        GameMenuComposePage(
            title = title ?: getString(R.string.game_menu_gamepad_buttons),
            onBack = ::dismiss,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { "${it.name}:${it.desc}" }) { entry ->
                    Card(
                        modifier = Modifier.clickable { select(entry) },
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
    }

    private fun select(entry: GameMenuQuickBean) {
        entry.id = VirtualControlElementIds.newId()
        elementSelectionListener?.onGamepadElementSelected(entry)
        dismiss()
    }

    private fun createEntries(): List<GameMenuQuickBean> = buildList {
        add(button("ABXY", ControllerPacket.PADDLE2_FLAG, "Y-X-A-B", 5))
        add(button("十字键", ControllerPacket.PADDLE1_FLAG, "▲-◀-▼-▶", 5))
        add(button("L3", ControllerPacket.LS_CLK_FLAG, "左摇杆按下"))
        add(button("R3", ControllerPacket.RS_CLK_FLAG, "右摇杆按下"))
        add(button("L1", ControllerPacket.LB_FLAG, "左肩键"))
        add(button("L2", ControllerPacket.PADDLE3_FLAG, "左扳机").setShapeType(1))
        add(button("R1", ControllerPacket.RB_FLAG, "右肩键"))
        add(button("R2", ControllerPacket.PADDLE4_FLAG, "右扳机").setShapeType(1))
        add(button("MODE", ControllerPacket.SPECIAL_BUTTON_FLAG, "Xbox 键"))
        add(button("SELECT", ControllerPacket.BACK_FLAG, "视图键"))
        add(button("START", ControllerPacket.PLAY_FLAG, "菜单键"))
        add(button("触控板", ControllerPacket.TOUCHPAD_FLAG, "触控板按键"))

        addAll(stickEntries("左摇杆", ControllerPacket.PADDLE5_FLAG))
        addAll(stickEntries("右摇杆", ControllerPacket.PADDLE6_FLAG))

        add(button("A", ControllerPacket.A_FLAG, "A 键"))
        add(button("B", ControllerPacket.B_FLAG, "B 键"))
        add(button("X", ControllerPacket.X_FLAG, "X 键"))
        add(button("Y", ControllerPacket.Y_FLAG, "Y 键"))
        add(button("▲", ControllerPacket.UP_FLAG, "十字键·上"))
        add(button("▼", ControllerPacket.DOWN_FLAG, "十字键·下"))
        add(button("◀", ControllerPacket.LEFT_FLAG, "十字键·左"))
        add(button("▶", ControllerPacket.RIGHT_FLAG, "十字键·右"))
        add(button("Share", ControllerPacket.MISC_FLAG, "手柄分享键"))
    }

    private fun button(
        name: String,
        code: Int,
        description: String,
        type: Int = 4,
    ) = GameMenuQuickBean(name, code, description, type, false).setGamePad(true)

    private fun stickEntries(name: String, code: Int): List<GameMenuQuickBean> = listOf(
        button(name, code, "常规模式", 3).setFreeStick(false),
        button(name, code, "常规·最大偏转", 3)
            .setFreeStick(false).setFixedStrokeFreeStick(true),
        button(name, code, "自由摇杆", 3).setFreeStick(true),
        button(name, code, "自由摇杆·触发显示", 3)
            .setFreeStick(true).setFreeeStickDrawNormal(false),
        button(name, code, "自由摇杆·最大偏转", 3)
            .setFreeStick(true).setFixedStrokeFreeStick(true),
        button(name, code, "自由·最大偏转·触发显示", 3)
            .setFreeStick(true)
            .setFixedStrokeFreeStick(true)
            .setFreeeStickDrawNormal(false),
    )
}
