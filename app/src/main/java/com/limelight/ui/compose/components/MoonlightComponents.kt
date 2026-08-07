package com.limelight.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.limelight.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonlightScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    collapsible: Boolean = false,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = if (collapsible) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_m3_arrow_back),
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = if (collapsible) scrollBehavior else null,
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/** Responsive card shared by all Compose-backed platform dialogs. */
@Composable
fun MoonlightDialogSurface(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun MoonlightGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        content = { Column(content = content) },
    )
}

@Composable
fun MoonlightActionRow(
    title: String,
    supportingText: String? = null,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { value ->
            { Text(value) }
        },
        leadingContent = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_m3_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    )
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Slider that emits one light haptic only when its effective value changes. */
@Composable
fun MoonlightStepSlider(
    value: Float,
    onValueChange: (Float) -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onEffectiveValueChanged: (Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Slider(
        value = value,
        onValueChange = { candidate ->
            val effective = onValueChange(candidate)
            if (effective != value) {
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove,
                )
                onEffectiveValueChanged(effective)
            }
        },
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
    )
}

/** Switch with one semantic haptic for each user-requested state change. */
@Composable
fun MoonlightSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = if (enabled && onCheckedChange != null) {
            { newChecked ->
                haptics.performToggleHapticFeedback(newChecked)
                onCheckedChange(newChecked)
            }
        } else {
            null
        },
        modifier = modifier,
        enabled = enabled,
    )
}

/** Shared toggle feedback used when a larger settings row changes a switch. */
fun HapticFeedback.performToggleHapticFeedback(checked: Boolean) {
    performHapticFeedback(
        if (checked) {
            HapticFeedbackType.ToggleOn
        } else {
            HapticFeedbackType.ToggleOff
        },
    )
}
