package com.limelight.ui.compose.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.limelight.R
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Lifecycle-bound Material 3 action dialog used by host and app menus. */
class ActionMenuPresenter(private val context: Context) {
    data class Action(
        val label: CharSequence,
        val iconRes: Int,
        val command: Runnable?,
    )

    private var activeDialog: Dialog? = null
    private var activeComposition: ComposeView? = null

    fun show(
        title: CharSequence,
        status: CharSequence?,
        actions: List<Action>,
        onDismiss: Runnable? = null,
    ) {
        dismiss()
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content = ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow,
            )
            setContent {
                MoonlightThemeFromSettings {
                    ActionMenu(
                        title = title,
                        status = status,
                        actions = actions,
                        onAction = { action ->
                            dialog.dismiss()
                            action.command?.run()
                        },
                        onDismiss = dialog::dismiss,
                    )
                }
            }
        }
        activeDialog = dialog
        activeComposition = content
        dialog.setContentView(content)
        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
                activeComposition = null
            }
            onDismiss?.run()
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.45f)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    fun dismiss() {
        activeDialog?.dismiss()
        activeDialog = null
        activeComposition = null
    }

    fun destroy() = dismiss()

    @Composable
    private fun ActionMenu(
        title: CharSequence,
        status: CharSequence?,
        actions: List<Action>,
        onAction: (Action) -> Unit,
        onDismiss: () -> Unit,
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
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                status?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    actions.forEachIndexed { index, action ->
                        ListItem(
                            headlineContent = { Text(action.label.toString()) },
                            leadingContent = if (action.iconRes != 0) {
                                {
                                    Icon(
                                        painter = painterResource(action.iconRes),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = ComposeColor.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(action) }
                                .padding(horizontal = 8.dp),
                        )
                        if (index != actions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(context.getString(R.string.settings_cancel))
                }
            }
        }
    }
}
