package com.limelight.ui.gamemenu

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.R
import com.limelight.binding.input.virtual_controller.keyboard.VirtualControlEditMode
import com.limelight.settings.audio.StreamAudioSettings
import com.limelight.settings.controller.ControllerSettings
import com.limelight.settings.input.InputSettings
import com.limelight.settings.ui.GameMenuCardLayoutLoadResult
import com.limelight.settings.ui.StreamUiSettings
import com.limelight.settings.virtualcontrols.VirtualControlSettings
import org.junit.Rule
import org.junit.Test

/** Instrumentation contracts for the Material 3 in-stream menu shell. */
class GameMenuComposeRendererTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersStructuredControlSectionsWithoutLegacySideRail() {
        lateinit var renderer: GameMenuComposeRenderer
        composeRule.setContent {
            AndroidView(
                factory = { context ->
                    ComposeView(context).also { composeView ->
                        renderer = GameMenuComposeRenderer(composeView, null)
                        renderer.update(
                            defaultState(),
                            GameMenuCardConfiguration.defaults(emptyList()),
                        )
                    }
                },
            )
        }

        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        listOf(
            R.string.game_menu_title,
            R.string.game_menu_quick_actions_title,
            R.string.game_menu_input_controls_title,
            R.string.game_menu_section_mouse_touch,
        ).forEach { labelRes ->
            composeRule.onNodeWithText(context.getString(labelRes))
                .assertExists()
        }
    }

    private fun defaultState() = GameMenuState(
        true,
        false,
        false,
        true,
        false,
        false,
        false,
        75,
        GameMenuCardLayoutLoadResult.absent(),
        emptyList(),
        InputSettings.builder().build(),
        ControllerSettings.builder().build(),
        StreamAudioSettings.builder().build(),
        StreamUiSettings.builder().build(),
        VirtualControlSettings.builder().build(),
        VirtualControlEditMode.NONE,
        VirtualControlEditMode.NONE,
    )
}
