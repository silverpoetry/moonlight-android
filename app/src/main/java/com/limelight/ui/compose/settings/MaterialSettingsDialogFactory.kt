package com.limelight.ui.compose.settings

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.limelight.R
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
            Slider(
                value = value.toFloat(),
                onValueChange = { candidate ->
                    value = normalizer.normalize(candidate.roundToInt())
                },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = ((maximum - minimum) / step.coerceAtLeast(1) - 1)
                    .coerceAtLeast(0),
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
        val dialog = Dialog(activity)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
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
