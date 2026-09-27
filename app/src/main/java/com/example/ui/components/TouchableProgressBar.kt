package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.LocalThemePalette

/**
 * High-precision, touch-responsive progress bar that enables instant direct skipping
 * on tap anywhere along the track, as well as smooth scrubbing/dragging.
 */
@Composable
fun TouchableProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 6.dp,
    touchTargetHeight: Dp = 36.dp,
    activeColor: Color = Color.Unspecified,
    activeGradient: List<Color>? = null,
    inactiveColor: Color = Color.Unspecified,
    thumbColor: Color = Color.Unspecified,
    thumbGlowColor: Color = Color.Unspecified,
    showThumb: Boolean = true,
    tag: String = "touchable_progress_bar"
) {
    val palette = LocalThemePalette.current
    val resolvedActiveColor = if (activeColor != Color.Unspecified) activeColor else palette.seekBarActiveTrack
    val resolvedInactiveColor = if (inactiveColor != Color.Unspecified) inactiveColor else palette.c4Quaternary.copy(alpha = 0.25f)
    val resolvedThumbColor = if (thumbColor != Color.Unspecified) thumbColor else palette.c1Primary
    val resolvedThumbGlowColor = if (thumbGlowColor != Color.Unspecified) thumbGlowColor else palette.c6GlowHighlight.copy(alpha = 0.5f)
    val resolvedGradient = activeGradient ?: listOf(palette.c1Primary, palette.c2Secondary)

    val safeDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) {
        dragProgressFraction
    } else {
        (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(touchTargetHeight)
            .testTag(tag)
            .pointerInput(safeDuration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width
                    if (width > 0) {
                        val fraction = (down.position.x / width).coerceIn(0f, 1f)
                        dragProgressFraction = fraction
                        isDragging = true
                        onSeekTo((fraction * safeDuration).toLong())
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            change.consume()
                            if (width > 0) {
                                val fraction = (change.position.x / width).coerceIn(0f, 1f)
                                dragProgressFraction = fraction
                                onSeekTo((fraction * safeDuration).toLong())
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    isDragging = false
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(touchTargetHeight)
                .padding(horizontal = 2.dp)
        ) {
            val width = size.width
            val height = size.height
            val trackHeightPx = barHeight.toPx()
            val trackY = (height - trackHeightPx) / 2f
            val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

            // Background Inactive Track
            drawRoundRect(
                color = resolvedInactiveColor,
                topLeft = Offset(0f, trackY),
                size = Size(width, trackHeightPx),
                cornerRadius = cornerRadius
            )

            // Active Progress Track
            val activeWidth = (width * currentFraction).coerceIn(0f, width)
            if (activeWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(resolvedGradient),
                    topLeft = Offset(0f, trackY),
                    size = Size(activeWidth, trackHeightPx),
                    cornerRadius = cornerRadius
                )
            }

            // Glow and Thumb bead
            if (showThumb) {
                val thumbRadius = if (isDragging) 8.dp.toPx() else 6.dp.toPx()
                val thumbX = activeWidth.coerceIn(thumbRadius, width - thumbRadius)
                val thumbY = height / 2f

                // Outer neon glow
                drawCircle(
                    color = resolvedThumbGlowColor.copy(alpha = if (isDragging) 0.6f else 0.4f),
                    radius = thumbRadius + 4.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )

                // Solid Thumb
                drawCircle(
                    color = resolvedThumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY)
                )

                // White center core
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius * 0.45f,
                    center = Offset(thumbX, thumbY)
                )
            }
        }
    }
}
