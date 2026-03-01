package com.example.projekat.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A container that wraps content and allows swiping from the left edge
 * to navigate back. The content slides to the right during the swipe and
 * a dark scrim fades in behind it. Releasing past the threshold triggers
 * [onBack]; otherwise it snaps back to origin.
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val dismissThreshold = screenWidthPx * 0.35f // 35% of screen width

    // Scrim alpha based on drag progress (0 = no scrim, 0.4 = fully dimmed)
    val scrimAlpha = (offsetX.value / screenWidthPx).coerceIn(0f, 0.4f)

    Box(modifier = modifier.fillMaxSize()) {
        // Dark scrim behind the content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Draggable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > dismissThreshold) {
                                    // Animate out and trigger back
                                    offsetX.animateTo(
                                        targetValue = screenWidthPx,
                                        animationSpec = tween(200)
                                    )
                                    onBack()
                                } else {
                                    // Snap back to origin
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(200)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, animationSpec = tween(200))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                // Only allow dragging to the right (positive direction)
                                val newValue = (offsetX.value + dragAmount).coerceAtLeast(0f)
                                offsetX.snapTo(newValue)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
