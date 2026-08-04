package com.limelight.ui.gamemenu

import android.content.res.Configuration
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A bottom sheet in portrait and a right-side sheet in landscape. */
@Composable
internal fun DraggableGameMenuPanel(
    onDismiss: () -> Unit,
    dragEnabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    var dragOffset by remember(isLandscape) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val settleOrDismiss: () -> Unit = {
        if (dragOffset >= threshold) {
            onDismiss()
        } else {
            val start = dragOffset
            scope.launch {
                animate(start, 0f) { value, _ -> dragOffset = value }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                if (isLandscape) IntOffset(dragOffset.roundToInt(), 0)
                else IntOffset(0, dragOffset.roundToInt())
            },
        shape = if (isLandscape) {
            RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        } else {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(Modifier.fillMaxSize()) {
            content(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isLandscape) 12.dp else 0.dp,
                        top = if (isLandscape) 0.dp else 12.dp,
                    ),
            )
            if (isLandscape) {
                val dragHandle = if (dragEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = settleOrDismiss,
                            onDragCancel = settleOrDismiss,
                        )
                    }
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(36.dp)
                        .then(dragHandle),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            } else {
                val dragHandle = if (dragEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = settleOrDismiss,
                            onDragCancel = settleOrDismiss,
                        )
                    }
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .then(dragHandle),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
    }
}
