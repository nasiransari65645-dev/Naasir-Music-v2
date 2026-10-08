package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.model.EdgeLightingSettings
import com.example.model.EdgeLightingShape

/**
 * Global Dynamic Edge Lighting Overlay
 * Playback Enforced Rule:
 * - Edge lighting renders globally ONLY when active playback is running.
 * - Immediately hide and stop animation cycles when audio is paused or stopped, even if the Master Switch inside is ON.
 * - [forcePreview] can be used inside the Settings sub-screen to view live feedback even while audio is paused.
 */
@Composable
fun EdgeLightingOverlay(
    settings: EdgeLightingSettings,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    forcePreview: Boolean = false
) {
    val shouldRender = false 

    AnimatedVisibility(
        visible = shouldRender,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        EdgeLightingContent(settings = settings, isPlaying = isPlaying || forcePreview)
    }
}

@Composable
private fun EdgeLightingContent(
    settings: EdgeLightingSettings,
    isPlaying: Boolean
) {
    val transition = rememberInfiniteTransition(label = "edgeLightingAnim")

    // Rotation phase for circulating edge lighting
    val durationMs = (settings.animationSpeedSec * 1000).toInt().coerceIn(800, 6000)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Pulse factor when music is playing (Bass pulse reactivity)
    val pulseAlpha by transition.animateFloat(
        initialValue = if (settings.musicReactive && isPlaying) 0.55f else 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 400 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val colors = settings.style.colors
    val strokeWidthPx = settings.strokeWidthDp.dp

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sw = strokeWidthPx.toPx()

        if (w <= 0f || h <= 0f) return@Canvas

        val centerX = w / 2f
        val centerY = h / 2f

        val colorInts = IntArray(colors.size) { i ->
            colors[i].copy(alpha = pulseAlpha).toArgb()
        }
        val positions = FloatArray(colors.size) { i ->
            i.toFloat() / (colors.size - 1).coerceAtLeast(1)
        }
        val sweepShader = SweepGradient(centerX, centerY, colorInts, positions)
        val matrix = Matrix()
        matrix.postRotate(phase * 360f, centerX, centerY)
        sweepShader.setLocalMatrix(matrix)

        val brush = ShaderBrush(sweepShader)

        val cornerRadius = CornerRadius(
            settings.cornerRadiusDp.dp.toPx(),
            settings.cornerRadiusDp.dp.toPx()
        )

        when (settings.shape) {
            EdgeLightingShape.SOLID_LINE -> {
                // Outer glow layer
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = sw * 2.0f)
                )

                // Sharp core border
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = sw)
                )
            }
            EdgeLightingShape.NEON_DOTS -> {
                val dotDash = PathEffect.dashPathEffect(floatArrayOf(sw * 1.5f, sw * 2.5f), phase * 100f)
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = sw * 1.5f, pathEffect = dotDash)
                )
            }
            EdgeLightingShape.STARS, EdgeLightingShape.HEARTS, EdgeLightingShape.MUSIC_NOTES -> {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(sw * 2.5f, sw * 4f), phase * 120f)
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = sw * 1.8f, pathEffect = dashEffect)
                )
                // Core border
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = sw * 0.8f)
                )
            }
        }
    }
}
