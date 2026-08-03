package com.limelight.input.diagnostics

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.limelight.BaseActivity
import com.limelight.R
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import com.limelight.utils.UiToast

/** Presentation for local input-device diagnostics. */
class InputDiagnosticsActivity : BaseActivity(),
    InputDiagnosticsController.Listener {
    private lateinit var controller: InputDiagnosticsController

    private var deviceSummary by mutableStateOf("")
    private var inputEventSummary by mutableStateOf("")
    private var sensorSummary by mutableStateOf("")
    private var controllerVibrationAvailable by mutableStateOf(false)
    private var controllerVibrationDevice by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceSummary = getString(R.string.input_diagnostics_loading)
        inputEventSummary = getString(R.string.input_diagnostics_input_hint)
        sensorSummary = getString(R.string.input_diagnostics_loading)
        controllerVibrationDevice = getString(
            R.string.input_diagnostics_test_controller_vibration,
        )
        controller = InputDiagnosticsController.create(this, this)

        setContent {
            MoonlightThemeFromSettings {
                InputDiagnosticsScreen(
                    deviceSummary = deviceSummary,
                    inputEventSummary = inputEventSummary,
                    sensorSummary = sensorSummary,
                    controllerVibrationAvailable =
                        controllerVibrationAvailable,
                    controllerVibrationDevice = controllerVibrationDevice,
                    onBack = ::finish,
                    onRefresh = controller::refreshDevices,
                    onDeviceVibration = {
                        showVibrationResult(
                            controller.testDeviceVibration(),
                            R.string.input_diagnostics_no_device_vibrator,
                        )
                    },
                    onControllerVibration = {
                        showVibrationResult(
                            controller.testControllerVibration(),
                            R.string.input_diagnostics_no_controller_vibrator,
                        )
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.start()
    }

    override fun onStop() {
        controller.stop()
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        controller.onKeyEvent(event)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        controller.onKeyEvent(event)
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        controller.onMotionEvent(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onDeviceSummaryChanged(summary: String) {
        deviceSummary = summary
    }

    override fun onInputEventChanged(summary: String) {
        inputEventSummary = summary
    }

    override fun onSensorSummaryChanged(summary: String) {
        sensorSummary = summary
    }

    override fun onControllerVibrationAvailabilityChanged(
        available: Boolean,
        deviceName: String,
    ) {
        controllerVibrationAvailable = available
        controllerVibrationDevice = deviceName
    }

    private fun showVibrationResult(successful: Boolean, unavailableMessage: Int) {
        if (!successful) {
            UiToast.makeText(
                this,
                unavailableMessage,
                UiToast.LENGTH_SHORT,
            ).show()
        }
    }
}

@Composable
private fun InputDiagnosticsScreen(
    deviceSummary: String,
    inputEventSummary: String,
    sensorSummary: String,
    controllerVibrationAvailable: Boolean,
    controllerVibrationDevice: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDeviceVibration: () -> Unit,
    onControllerVibration: () -> Unit,
) {
    MoonlightScreen(
        title = stringResource(R.string.input_diagnostics_title),
        onBack = onBack,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.input_diagnostics_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.input_diagnostics_refresh))
                }
                Button(
                    onClick = onDeviceVibration,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            R.string.input_diagnostics_test_device_vibration,
                        ),
                    )
                }
            }
            OutlinedButton(
                onClick = onControllerVibration,
                enabled = controllerVibrationAvailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CONTROLLER_VIBRATION_TEST_TAG),
            ) {
                Text(
                    stringResource(
                        R.string.input_diagnostics_test_controller_vibration_format,
                        controllerVibrationDevice,
                    ),
                )
            }
            DiagnosticSection(
                title = stringResource(R.string.input_diagnostics_devices),
                summary = deviceSummary,
                summaryTestTag = DEVICE_SUMMARY_TEST_TAG,
            )
            DiagnosticSection(
                title = stringResource(R.string.input_diagnostics_live_input),
                summary = inputEventSummary,
                summaryTestTag = INPUT_SUMMARY_TEST_TAG,
            )
            DiagnosticSection(
                title = stringResource(R.string.input_diagnostics_sensors),
                summary = sensorSummary,
                summaryTestTag = SENSOR_SUMMARY_TEST_TAG,
            )
        }
    }
}

@Composable
private fun DiagnosticSection(
    title: String,
    summary: String,
    summaryTestTag: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .testTag(summaryTestTag),
            )
        }
    }
}

internal const val DEVICE_SUMMARY_TEST_TAG = "input_diagnostics_device_summary"
internal const val INPUT_SUMMARY_TEST_TAG = "input_diagnostics_input_summary"
internal const val SENSOR_SUMMARY_TEST_TAG = "input_diagnostics_sensor_summary"
internal const val CONTROLLER_VIBRATION_TEST_TAG =
    "input_diagnostics_controller_vibration"
