package com.limelight.utils

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Material 3 progress dialog retaining the existing cancellation contract. */
class SpinnerDialog private constructor(
    private val activity: Activity,
    private val title: String,
    initialMessage: String,
    private val finish: Boolean,
) : Runnable, DialogInterface.OnCancelListener {
    private var progress: android.app.Dialog? = null
    private var messageText by mutableStateOf(initialMessage)
    private var finishOnCancelEnabled = finish

    fun dismiss() {
        activity.runOnUiThread(this)
    }

    fun setMessage(message: String) {
        activity.runOnUiThread { this.messageText = message }
    }

    fun setFinishOnCancelEnabled(enabled: Boolean) {
        finishOnCancelEnabled = enabled
        activity.runOnUiThread {
            progress?.let { dialog ->
                if (finish) {
                    dialog.setCancelable(enabled)
                    dialog.setCanceledOnTouchOutside(false)
                }
            }
        }
    }

    override fun run() {
        if (activity.isFinishing) return
        if (progress != null) {
            val dialog = progress
            progress = null
            synchronized(rundownDialogs) { rundownDialogs.remove(this) }
            dialog?.takeIf { it.isShowing }?.dismiss()
            return
        }

        val dialog = android.app.Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setOnCancelListener(this)
        dialog.setCancelable(finish && finishOnCancelEnabled)
        dialog.setCanceledOnTouchOutside(false)
        val content = ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow,
            )
            setContent {
                MoonlightThemeFromSettings {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .widthIn(max = 520.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            CircularProgressIndicator()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = messageText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        progress = dialog
        dialog.setContentView(content)
        synchronized(rundownDialogs) { rundownDialogs.add(this) }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.35f)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        synchronized(rundownDialogs) { rundownDialogs.remove(this) }
        progress = null
        if (finish && finishOnCancelEnabled) {
            activity.finish()
        }
    }

    companion object {
        private val rundownDialogs = ArrayList<SpinnerDialog>()

        @JvmStatic
        fun displayDialog(
            activity: Activity,
            title: String,
            message: String,
            finish: Boolean,
        ): SpinnerDialog = SpinnerDialog(activity, title, message, finish).also {
            activity.runOnUiThread(it)
        }

        @JvmStatic
        fun closeDialogs(activity: Activity) {
            val matches = synchronized(rundownDialogs) {
                rundownDialogs.filter { it.activity === activity }.also {
                    rundownDialogs.removeAll(it.toSet())
                }
            }
            matches.forEach { spinner ->
                spinner.progress?.takeIf { it.isShowing }?.dismiss()
                spinner.progress = null
            }
        }
    }
}
