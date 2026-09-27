package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.LocalThemePalette

@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Unspecified,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 18.dp
) {
    val resolvedBarColor = if (barColor != Color.Unspecified) barColor else LocalThemePalette.current.primaryAccent
    val transition = rememberInfiniteTransition(label = "equalizer")

    val h1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    val currentH1 = if (isPlaying) h1 else 0.3f
    val currentH2 = if (isPlaying) h2 else 0.6f
    val currentH3 = if (isPlaying) h3 else 0.4f

    Canvas(
        modifier = modifier
            .width(barWidth * 3 + 4.dp)
            .height(maxHeight)
    ) {
        val barW = barWidth.toPx()
        val spacing = 2.dp.toPx()
        val totalH = size.height
        val cr = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        val heights = floatArrayOf(currentH1, currentH2, currentH3)
        for (i in 0..2) {
            val barH = (totalH * heights[i]).coerceAtLeast(2.dp.toPx())
            val left = i * (barW + spacing)
            val top = totalH - barH
            drawRoundRect(
                color = resolvedBarColor,
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = cr
            )
        }
    }
}
