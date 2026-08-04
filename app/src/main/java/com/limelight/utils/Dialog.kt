package com.limelight.utils

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.limelight.R
import com.limelight.ui.compose.components.MoonlightDialogSurface
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Material 3 generic message dialog with process-wide rundown support. */
class Dialog private constructor(
    private val activity: Activity,
    private val title: String,
    private val message: String,
    private val runOnDismiss: Runnable,
) : Runnable {
    private var alert: android.app.Dialog? = null

    override fun run() {
        if (activity.isFinishing) return
        val platformDialog = ComponentDialog(activity)
        platformDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        platformDialog.setCancelable(false)
        platformDialog.setCanceledOnTouchOutside(false)
        val content = ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow,
            )
            setContent {
                MoonlightThemeFromSettings {
                    MoonlightDialogSurface(maxWidth = 520.dp) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(title, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = message,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = {
                                        dismissAndRun()
                                        HelpLauncher.launchTroubleshooting(activity)
                                    },
                                ) {
                                    Text(activity.getString(R.string.help))
                                }
                                Button(
                                    onClick = ::dismissAndRun,
                                    modifier = Modifier.padding(start = 12.dp),
                                ) {
                                    Text(activity.getString(android.R.string.ok))
                                }
                            }
                        }
                    }
                }
            }
        }
        alert = platformDialog
        platformDialog.setContentView(content)
        synchronized(rundownDialogs) {
            rundownDialogs.add(this)
        }
        platformDialog.show()
        platformDialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.45f)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun dismissAndRun() {
        synchronized(rundownDialogs) {
            rundownDialogs.remove(this)
        }
        alert?.dismiss()
        alert = null
        runOnDismiss.run()
    }

    companion object {
        private val rundownDialogs = ArrayList<Dialog>()

        @JvmStatic
        fun closeDialogs() {
            val dialogs = synchronized(rundownDialogs) {
                rundownDialogs.toList().also { rundownDialogs.clear() }
            }
            dialogs.forEach { dialog ->
                dialog.alert?.takeIf { it.isShowing }?.dismiss()
                dialog.alert = null
            }
        }

        @JvmStatic
        fun displayDialog(
            activity: Activity,
            title: String,
            message: String,
            endAfterDismiss: Boolean,
        ) {
            activity.runOnUiThread(
                Dialog(
                    activity,
                    title,
                    message,
                    Runnable { if (endAfterDismiss) activity.finish() },
                ),
            )
        }

        @JvmStatic
        fun displayDialog(
            activity: Activity,
            title: String,
            message: String,
            runOnDismiss: Runnable,
        ) {
            activity.runOnUiThread(
                Dialog(activity, title, message, runOnDismiss),
            )
        }
    }
}
