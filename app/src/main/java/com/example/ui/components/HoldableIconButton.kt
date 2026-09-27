package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * An interactive playback button that supports both single-tap clicks and sustained long-press holding.
 *
 * - Single Tap (< 350ms): Executes [onClick] normally.
 * - Long-Press Hold (>= 350ms): Fires [onHoldStart], remains active while holding,
 *   and fires [onHoldEnd] immediately upon release, WITHOUT firing [onClick].
 */
@Composable
fun HoldableIconButton(
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isHolding: Boolean = false,
    activeGlowColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isHolding) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "hold_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (isHolding && activeGlowColor != Color.Unspecified) {
                    Modifier.background(activeGlowColor.copy(alpha = 0.25f))
                } else {
                    Modifier
                }
            )
            .indication(interactionSource, ripple(bounded = true, radius = size / 2))
            .pointerInput(onClick, onHoldStart, onHoldEnd) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isHoldingTriggered = false
                    val timerJob = coroutineScope.launch {
                        delay(350L) // Long-press activation threshold
                        isHoldingTriggered = true
                        onHoldStart()
                    }

                    var isReleased = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                isReleased = true
                                break
                            }
                        }
                    } finally {
                        timerJob.cancel()
                        if (isHoldingTriggered) {
                            onHoldEnd()
                        } else if (isReleased) {
                            onClick()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
