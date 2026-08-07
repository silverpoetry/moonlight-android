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
        add(button(getString(R.string.gamepad_dpad), ControllerPacket.PADDLE1_FLAG, "▲-◀-▼-▶", 5))
        add(button("L3", ControllerPacket.LS_CLK_FLAG, getString(R.string.gamepad_left_stick_click)))
        add(button("R3", ControllerPacket.RS_CLK_FLAG, getString(R.string.gamepad_right_stick_click)))
        add(button("L1", ControllerPacket.LB_FLAG, getString(R.string.gamepad_left_bumper)))
        add(button("L2", ControllerPacket.PADDLE3_FLAG, getString(R.string.gamepad_left_trigger)).setShapeType(1))
        add(button("R1", ControllerPacket.RB_FLAG, getString(R.string.gamepad_right_bumper)))
        add(button("R2", ControllerPacket.PADDLE4_FLAG, getString(R.string.gamepad_right_trigger)).setShapeType(1))
        add(button("MODE", ControllerPacket.SPECIAL_BUTTON_FLAG, getString(R.string.gamepad_xbox_button)))
        add(button("SELECT", ControllerPacket.BACK_FLAG, getString(R.string.gamepad_view_button)))
        add(button("START", ControllerPacket.PLAY_FLAG, getString(R.string.gamepad_menu_button)))
        add(button(getString(R.string.gamepad_touchpad), ControllerPacket.TOUCHPAD_FLAG, getString(R.string.gamepad_touchpad_button)))

        addAll(stickEntries(getString(R.string.gamepad_left_stick), ControllerPacket.PADDLE5_FLAG))
        addAll(stickEntries(getString(R.string.gamepad_right_stick), ControllerPacket.PADDLE6_FLAG))

        add(button("A", ControllerPacket.A_FLAG, getString(R.string.gamepad_a_button)))
        add(button("B", ControllerPacket.B_FLAG, getString(R.string.gamepad_b_button)))
        add(button("X", ControllerPacket.X_FLAG, getString(R.string.gamepad_x_button)))
        add(button("Y", ControllerPacket.Y_FLAG, getString(R.string.gamepad_y_button)))
        add(button("▲", ControllerPacket.UP_FLAG, getString(R.string.gamepad_dpad_up)))
        add(button("▼", ControllerPacket.DOWN_FLAG, getString(R.string.gamepad_dpad_down)))
        add(button("◀", ControllerPacket.LEFT_FLAG, getString(R.string.gamepad_dpad_left)))
        add(button("▶", ControllerPacket.RIGHT_FLAG, getString(R.string.gamepad_dpad_right)))
        add(button("Share", ControllerPacket.MISC_FLAG, getString(R.string.gamepad_share_button)))
    }

    private fun button(
        name: String,
        code: Int,
        description: String,
        type: Int = 4,
    ) = GameMenuQuickBean(name, code, description, type, false).setGamePad(true)

    private fun stickEntries(name: String, code: Int): List<GameMenuQuickBean> = listOf(
        button(name, code, getString(R.string.gamepad_stick_standard), 3).setFreeStick(false),
        button(name, code, getString(R.string.gamepad_stick_standard_full_range), 3)
            .setFreeStick(false).setFixedStrokeFreeStick(true),
        button(name, code, getString(R.string.gamepad_stick_free), 3).setFreeStick(true),
        button(name, code, getString(R.string.gamepad_stick_free_show_on_touch), 3)
            .setFreeStick(true).setFreeeStickDrawNormal(false),
        button(name, code, getString(R.string.gamepad_stick_free_full_range), 3)
            .setFreeStick(true).setFixedStrokeFreeStick(true),
        button(name, code, getString(R.string.gamepad_stick_free_full_range_show_on_touch), 3)
            .setFreeStick(true)
            .setFixedStrokeFreeStick(true)
            .setFreeeStickDrawNormal(false),
    )
}
