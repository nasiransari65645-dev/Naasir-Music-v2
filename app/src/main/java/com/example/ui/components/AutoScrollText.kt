package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Auto-scrolling song title text:
 * Scrolls across when text exceeds container width, reaches the end,
 * waits 3 seconds, returns to the start, waits 3 seconds, and repeats.
 */
@Composable
fun AutoScrollText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    scrollSpeedPxPerSec: Float = 38f,
    initialDelayMs: Long = 3000L
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    var containerWidthPx by remember { mutableIntStateOf(0) }

    // Measure intrinsic text width in pixels
    val textLayoutResult = remember(text, style, density) {
        textMeasurer.measure(
            text = text,
            style = style.copy(fontWeight = fontWeight ?: style.fontWeight)
        )
    }
    val textWidthPx = textLayoutResult.size.width

    val isOverflowing = containerWidthPx > 0 && textWidthPx > containerWidthPx
    val maxScrollOffset = if (isOverflowing) (textWidthPx - containerWidthPx) else 0

    // Auto-scroll loop: Start -> End -> Wait 3s -> Return to Start -> Wait 3s -> Repeat
    LaunchedEffect(text, isOverflowing, maxScrollOffset, initialDelayMs) {
        if (!isOverflowing || maxScrollOffset <= 0) {
            scrollState.scrollTo(0)
            return@LaunchedEffect
        }

        var isFirstRun = true
        while (isActive) {
            // 1. Initial pause at the start
            scrollState.scrollTo(0)
            val waitAtStart = if (isFirstRun) initialDelayMs else 3000L
            isFirstRun = false
            delay(waitAtStart)

            if (!isActive) break

            // 2. Smoothly scroll to the end
            val durationMs = ((maxScrollOffset / scrollSpeedPxPerSec) * 1000).toInt().coerceAtLeast(1000)
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val targetScroll = (fraction * maxScrollOffset).toInt()
                scrollState.scrollTo(targetScroll)

                if (fraction >= 1f) break
                delay(16L) // ~60 fps
            }

            if (!isActive) break

            // 3. Wait 3 seconds after going to the last text
            delay(3000L)

            if (!isActive) break

            // 4. Smoothly or instantly return to the first text
            // Smoothly return in 500ms
            val returnDurationMs = 500
            val returnStartTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - returnStartTime
                val fraction = (elapsed.toFloat() / returnDurationMs.toFloat()).coerceIn(0f, 1f)
                val targetScroll = ((1f - fraction) * maxScrollOffset).toInt()
                scrollState.scrollTo(targetScroll)

                if (fraction >= 1f) break
                delay(16L)
            }
            scrollState.scrollTo(0)

            // Will repeat and wait 3 seconds again at start!
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width },
        contentAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isOverflowing) {
            // Use scrollState for the automated 3-second cycle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState, enabled = false)
            ) {
                Text(
                    text = text,
                    color = color,
                    style = style,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            Text(
                text = text,
                color = color,
                style = style,
                fontWeight = fontWeight,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
