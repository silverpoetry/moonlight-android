package com.limelight.input.diagnostics

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumentation contracts for the Compose diagnostics presentation. */
class InputDiagnosticsActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<InputDiagnosticsActivity>()

    @Test
    fun launchesAndPublishesLocalDiagnostics() {
        assertSummaryIsPopulated(DEVICE_SUMMARY_TEST_TAG)
        assertSummaryIsPopulated(SENSOR_SUMMARY_TEST_TAG)
        composeRule.onNodeWithTag(CONTROLLER_VIBRATION_TEST_TAG)
            .assertExists()
    }

    private fun assertSummaryIsPopulated(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithTag(tag)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .any { it.text.isNotBlank() }
        }
        val text = composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
        assertTrue(text.any { it.text.isNotBlank() })
    }
}
