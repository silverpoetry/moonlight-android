package com.limelight.ui.compose.settings

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toDrawable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.ui.compose.components.MoonlightDialogSurface
import com.limelight.ui.compose.components.MoonlightStepSlider
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import kotlin.math.roundToInt

/** Creates lifecycle-safe Material 3 editors for settings values. */
class MaterialSettingsDialogFactory(private val activity: Activity) {
    fun interface ListSelectionListener {
        fun onSelected(value: String)
    }

    fun interface IntegerSelectionListener {
        fun onSelected(value: Int)
    }

    fun interface IntegerFormatter {
        fun format(value: Int): CharSequence
    }

    fun interface IntegerNormalizer {
        fun normalize(value: Int): Int
    }

    fun interface TextSubmissionListener {
        /** Returns a validation error, or null when the dialog may close. */
        fun onSubmitted(value: String): CharSequence?
    }

    fun showList(
        title: CharSequence,
        entries: Array<CharSequence>,
        values: Array<CharSequence>,
        currentValue: String,
        listener: ListSelectionListener,
    ): Dialog = showDialog { dialog ->
        SettingsDialogCard(title = title) {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                entries.indices.forEach { index ->
                    val value = values[index].toString()
                    ListItem(
                        headlineContent = { Text(entries[index].toString()) },
                        leadingContent = {
                            RadioButton(
                                selected = value == currentValue,
                                onClick = null,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = ComposeColor.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                listener.onSelected(value)
                                dialog.dismiss()
                            },
                    )
                }
            }
        }
    }

    fun showSlider(
        title: CharSequence,
        message: CharSequence?,
        minimum: Int,
        maximum: Int,
        step: Int,
        currentValue: Int,
        formatter: IntegerFormatter,
        normalizer: IntegerNormalizer,
        listener: IntegerSelectionListener,
    ): Dialog = showDialog { dialog ->
        var value by remember(currentValue) {
            mutableIntStateOf(normalizer.normalize(currentValue))
        }
        SettingsDialogCard(title = title) {
            message?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatter.format(value).toString(),
                style = MaterialTheme.typography.headlineMedium,
            )
            MoonlightStepSlider(
                value = value.toFloat(),
                onValueChange = { candidate ->
                    normalizer.normalize(candidate.roundToInt()).toFloat()
                },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = 0,
                onEffectiveValueChanged = { value = it.roundToInt() },
            )
            ConfirmButtons(
                onCancel = dialog::dismiss,
                onConfirm = {
                    listener.onSelected(value)
                    dialog.dismiss()
                },
            )
        }
    }

    fun showDiscreteListSlider(
        title: CharSequence,
        entries: Array<CharSequence>,
        values: Array<CharSequence>,
        currentValue: String,
        listener: ListSelectionListener,
    ): Dialog = showDialog { dialog ->
        val initialIndex = values.indexOfFirst { it.toString() == currentValue }
            .coerceAtLeast(0)
        var selectedIndex by remember(currentValue, values.size) {
            mutableIntStateOf(initialIndex)
        }
        SettingsDialogCard(title = title) {
            Text(
                text = entries.getOrNull(selectedIndex)?.toString().orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
            )
            MoonlightStepSlider(
                value = selectedIndex.toFloat(),
                onValueChange = { it.roundToInt().coerceIn(entries.indices) .toFloat() },
                valueRange = 0f..entries.lastIndex.toFloat(),
                steps = (entries.size - 2).coerceAtLeast(0),
                onEffectiveValueChanged = { selectedIndex = it.roundToInt() },
            )
            ConfirmButtons(
                onCancel = dialog::dismiss,
                onConfirm = {
                    listener.onSelected(values[selectedIndex].toString())
                    dialog.dismiss()
                },
            )
        }
    }

    fun showSegmentedList(
        title: CharSequence,
        entries: Array<CharSequence>,
        values: Array<CharSequence>,
        currentValue: String,
        listener: ListSelectionListener,
    ): Dialog = showDialog { dialog ->
        var selectedIndex by remember(currentValue, values.size) {
            mutableIntStateOf(
                values.indexOfFirst { it.toString() == currentValue }
                    .coerceAtLeast(0),
            )
        }
        val haptics = LocalHapticFeedback.current
        SettingsDialogCard(title = title) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = selectedIndex == index,
                        onClick = {
                            if (selectedIndex != index) {
                                selectedIndex = index
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                                )
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = entries.size,
                        ),
                        label = { Text(entry.toString(), maxLines = 1) },
                    )
                }
            }
            ConfirmButtons(
                onCancel = dialog::dismiss,
                onConfirm = {
                    listener.onSelected(values[selectedIndex].toString())
                    dialog.dismiss()
                },
            )
        }
    }

    fun showText(
        title: CharSequence,
        message: CharSequence?,
        currentValue: String,
        decimal: Boolean,
        maxLength: Int,
        listener: TextSubmissionListener,
    ): Dialog = showDialog { dialog ->
        var value by remember(currentValue) { mutableStateOf(currentValue) }
        var validationError by remember {
            mutableStateOf<CharSequence?>(null)
        }
        val submit = {
            validationError = listener.onSubmitted(value)
            if (validationError == null) {
                dialog.dismiss()
            }
        }
        SettingsDialogCard(title = title) {
            message?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = { candidate ->
                    if (maxLength <= 0 || candidate.length <= maxLength) {
                        value = candidate
                        validationError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = validationError != null,
                supportingText = validationError?.let { error ->
                    { Text(error.toString()) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) {
                        KeyboardType.Decimal
                    } else {
                        KeyboardType.Text
                    },
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            ConfirmButtons(
                onCancel = dialog::dismiss,
                onConfirm = submit,
            )
        }
    }

    private fun showDialog(content: @Composable (Dialog) -> Unit): Dialog {
        val dialog = ComponentDialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow,
            )
            setContent {
                MoonlightThemeFromSettings {
                    content(dialog)
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.45f)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        return dialog
    }

    @Composable
    private fun SettingsDialogCard(
        title: CharSequence,
        content: @Composable () -> Unit,
    ) {
        MoonlightDialogSurface(maxWidth = 520.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = title.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                )
                content()
            }
        }
    }

    @Composable
    private fun ConfirmButtons(
        onCancel: () -> Unit,
        onConfirm: () -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onCancel) {
                Text(activity.getString(R.string.settings_cancel))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(activity.getString(R.string.settings_ok))
            }
        }
    }

}
