package com.limelight.preferences

import android.app.Instrumentation.ActivityMonitor
import android.app.Activity
import android.util.TypedValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.limelight.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

/** Instrumentation contracts for the Compose settings presentation. */
class StreamSettingsRenderingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<StreamSettings>()

    @Test
    fun settingsActivityRendersMaterialContent() {
        composeRule.onNodeWithTag(SettingsScreenRenderer.FEATURED_TEST_TAG)
            .assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.settings_featured_settings),
        ).assertExists()

        val windowBackground = TypedValue()
        check(
            composeRule.activity.theme.resolveAttribute(
                android.R.attr.windowBackground,
                windowBackground,
                true,
            ),
        )
        assertEquals(R.color.moonlight_window_background, windowBackground.resourceId)
        assertEquals(
            R.style.SettingsActivityAnimation,
            composeRule.activity.window.attributes.windowAnimations,
        )
    }

    @Test
    fun switchMutationUpdatesInPlace() {
        val toggle = composeRule.onAllNodes(isToggleable())[0]
        val before = toggle.fetchSemanticsNode()
            .config[SemanticsProperties.ToggleableState]

        toggle.performClick()
        composeRule.waitForIdle()

        val after = composeRule.onAllNodes(isToggleable())[0]
            .fetchSemanticsNode()
            .config[SemanticsProperties.ToggleableState]
        assertNotSame(before, after)

        // Restore the persisted setting so this test is hermetic.
        composeRule.onAllNodes(isToggleable())[0].performClick()
        composeRule.waitForIdle()
        assertEquals(
            before,
            composeRule.onAllNodes(isToggleable())[0]
                .fetchSemanticsNode()
                .config[SemanticsProperties.ToggleableState],
        )
    }

    @Test
    fun sectionNavigationUsesAdaptiveDestination() {
        val section = SettingsRegistry.load(composeRule.activity).first()
        val sectionTag = SettingsScreenRenderer.SECTION_CARD_TEST_TAG_PREFIX + section.key

        if (composeRule.activity.resources.configuration.screenWidthDp >= 720) {
            composeRule.onNodeWithTag(sectionTag)
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithTag(
                SettingsScreenRenderer.SECTION_TEST_TAG_PREFIX + section.key,
            ).assertExists()
            return
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor: ActivityMonitor = instrumentation.addMonitor(
            StreamSettings::class.java.name,
            null,
            false,
        )
        composeRule.onNodeWithTag(sectionTag)
            .performScrollTo()
            .performClick()
        val detail = instrumentation.waitForMonitorWithTimeout(monitor, 2_000)
            as StreamSettings?
        instrumentation.removeMonitor(monitor)

        assertNotNull(detail)
        assertNotSame(composeRule.activity, detail)
        instrumentation.runOnMainSync { detail?.finish() }
    }

    @Test
    fun canceledDocumentResultKeepsActivityAlive() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.handleDocumentActivityResult(
                Activity.RESULT_CANCELED,
                null,
            )
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
    }
}
