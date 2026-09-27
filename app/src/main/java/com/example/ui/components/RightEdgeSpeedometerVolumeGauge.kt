package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VolumeGaugePosition
import com.example.model.VolumeGaugeSettings
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Curved Rotating-Dial Speedometer Volume HUD with Direct Touch Drag:
 *
 * 1. Right-Edge Curved Arc Layout & Geometry:
 *    - Flush against the screen edge (default Right bezel), vertically centered.
 *    - Semi-translucent dark OLED capsule bowing inwards from the edge.
 *    - Rotating Radial Calibration Track:
 *      * Circular arc path with tick marks and round step numbers (... 20, 30, 40, 50, 60, 70, 80 ...).
 *      * Dynamic rotation aligns the nearest multiple of 10 under the indicator lens at the focal baseline.
 *    - Magnifier Glass Pill Capsule:
 *      * Fixed rounded-rectangle glass capsule with neon violet glowing border directly on the arc track at the focal baseline.
 *    - Focal Numeric Readout:
 *      * Real-time volume integer in bold glowing amber/peach (#FFD1A4) inside the inner curve.
 *
 * 2. Direct Touch Drag & Gesture Control:
 *    - Vertical drag gestures directly update volume, cancel auto-dismiss timer on touch,
 *      fire haptic ticks on integer change, and restart 2-second debounce timer on release.
 *    - Full synchronization between on-screen touch drag and physical volume keys.
 */
@Composable
fun RightEdgeSpeedometerVolumeGauge(
    visible: Boolean,
    volumeLevel: Float, // 0.0f to 1.0f
    settings: VolumeGaugeSettings,
    onVolumeChange: (Float) -> Unit,
    onTouchHold: () -> Unit = {},
    onTouchRelease: () -> Unit = {},
    modifier: Modifier = Modifier,
    fillScreen: Boolean = true
) {
    val isLeft = settings.position == VolumeGaugePosition.LEFT

    val enterTransition = if (isLeft) {
        slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
    } else {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    }

    val exitTransition = if (isLeft) {
        slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    } else {
        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    }

    val alignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

    val containerModifier = if (fillScreen) {
        modifier.fillMaxSize()
    } else {
        modifier
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = containerModifier
    ) {
        Box(
            modifier = if (fillScreen) Modifier.fillMaxSize() else Modifier,
            contentAlignment = alignment
        ) {
            CurvedSpeedometerVolumeHud(
                volumeLevel = volumeLevel.coerceIn(0.0f, 1.0f),
                settings = settings,
                onVolumeChange = onVolumeChange,
                onTouchHold = onTouchHold,
                onTouchRelease = onTouchRelease
            )
        }
    }
}

@Composable
private fun CurvedSpeedometerVolumeHud(
    volumeLevel: Float,
    settings: VolumeGaugeSettings,
    onVolumeChange: (Float) -> Unit,
    onTouchHold: () -> Unit,
    onTouchRelease: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isLeft = settings.position == VolumeGaugePosition.LEFT
    val density = LocalDensity.current

    val volumePercent = (volumeLevel * 100f).coerceIn(0f, 100f)
    val percentageInt = volumePercent.roundToInt()

    // Smooth rotational animation to eliminate flickering or jumping
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "volume_hud_arc_rotation"
    )

    // Remember last haptic tick integer
    var lastHapticInt by remember { mutableIntStateOf(percentageInt) }
    var currentVolFraction by remember(volumeLevel) { mutableFloatStateOf(volumeLevel) }

    fun checkAndFireHaptic(newInt: Int) {
        if (settings.hapticEnabled && newInt != lastHapticInt) {
            lastHapticInt = newInt
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Throwable) {}
        }
    }

    val volumeIcon = when {
        percentageInt == 0 -> Icons.Default.VolumeMute
        percentageInt < 50 -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }

    // Pre-create paint for native canvas text rendering
    val textPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = with(density) { 9.5.sp.toPx() }
        }
    }

    // Dynamic Fitted Dimensions: Ultra-compact container hugging the dial and indicator
    val hudWidth = 106.dp
    val hudHeight = 236.dp

    // Curved Organic Pill Contour:
    // When right-docked: flush against right screen bezel (0dp corners), inner left side curves smoothly
    // following the speedometer arc curvature (radius = half height).
    val contourShape = if (isLeft) {
        RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = hudHeight / 2,
            bottomEnd = hudHeight / 2
        )
    } else {
        RoundedCornerShape(
            topStart = hudHeight / 2,
            bottomStart = hudHeight / 2,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        )
    }

    // Razor-sharp 1dp contour outline stroke with cyan to purple neon gradient
    val contourBorder = BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(
                Color(0xFF00F5FF).copy(alpha = 0.6f),
                Color(0xFFBD00FF).copy(alpha = 0.8f),
                Color(0xFF00F5FF).copy(alpha = 0.4f)
            )
        )
    )

    // Dynamic Fitted Speedometer HUD Container
    Box(
        modifier = Modifier
            .width(hudWidth)
            .height(hudHeight)
            .testTag("curved_speedometer_volume_hud")
            .shadow(
                elevation = 10.dp,
                shape = contourShape,
                ambientColor = Color(0xFFBD00FF).copy(alpha = 0.35f),
                spotColor = Color(0xFF00F5FF).copy(alpha = 0.35f)
            )
            .clip(contourShape)
            .background(Color(0xCC0D1117))
            .border(border = contourBorder, shape = contourShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTouchHold()
                        tryAwaitRelease()
                        onTouchRelease()
                    },
                    onTap = { offset ->
                        onTouchHold()
                        // Vertical tap: top = 100%, bottom = 0%
                        val tappedRatio = (1.0f - (offset.y / size.height.toFloat())).coerceIn(0.0f, 1.0f)
                        val newInt = (tappedRatio * 100f).roundToInt()
                        checkAndFireHaptic(newInt)
                        currentVolFraction = tappedRatio
                        onVolumeChange(tappedRatio)
                        onTouchRelease()
                    }
                )
            }
            .pointerInput(Unit) {
                val dragSensitivity = 210.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = {
                        onTouchHold()
                    },
                    onDragEnd = {
                        onTouchRelease()
                    },
                    onDragCancel = {
                        onTouchRelease()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onTouchHold()
                        // Dragging upwards decreases Y offset (negative dragAmount), thus increasing volume
                        val delta = -dragAmount / dragSensitivity
                        val targetVolume = (currentVolFraction + delta).coerceIn(0.0f, 1.0f)
                        currentVolFraction = targetVolume
                        val newInt = (targetVolume * 100f).roundToInt()
                        checkAndFireHaptic(newInt)
                        onVolumeChange(targetVolume)
                    }
                )
            }
    ) {
        // 1. Rotating Radial Calibration Track Canvas (Curved Arc + Ticks + Rotating Step Numbers)
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f

            // Curved Arc Geometry: snug radius and center synchronized with the contour
            val arcRadius = 112.dp.toPx()
            val arcCenterX = if (isLeft) -90.dp.toPx() else w + 90.dp.toPx()

            // Angular step per 10 volume units
            val angleStepRad = 0.145f

            // 1a. Draw subtle base arc track guideline
            val baselineAngleSpan = 0.74f
            for (step in -20..20) {
                val th1 = (step / 20f) * baselineAngleSpan
                val th2 = ((step + 1) / 20f) * baselineAngleSpan
                val x1 = if (isLeft) arcCenterX + arcRadius * cos(th1) else arcCenterX - arcRadius * cos(th1)
                val y1 = centerY - arcRadius * sin(th1)
                val x2 = if (isLeft) arcCenterX + arcRadius * cos(th2) else arcCenterX - arcRadius * cos(th2)
                val y2 = centerY - arcRadius * sin(th2)
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.28f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 1b. Draw Tick Marks along the circular arc
            // k ranges from 0 to 100 in steps of 2
            for (k in 0..100 step 2) {
                val theta = (k - animatedVolume) / 10f * angleStepRad

                // Visible range: |theta| <= 0.72 rad
                if (kotlin.math.abs(theta) <= 0.72f) {
                    val fadeFactor = (1.0f - (kotlin.math.abs(theta) / 0.72f).pow(2f)).coerceIn(0.12f, 1.0f)

                    val arcX = if (isLeft) arcCenterX + arcRadius * cos(theta) else arcCenterX - arcRadius * cos(theta)
                    val arcY = centerY - arcRadius * sin(theta)

                    val radialDirX = if (isLeft) cos(theta) else -cos(theta)
                    val radialDirY = -sin(theta)

                    val isMajor = k % 10 == 0
                    val isMedium = k % 5 == 0

                    val tickLength = when {
                        isMajor -> 10.dp.toPx()
                        isMedium -> 6.5.dp.toPx()
                        else -> 4.dp.toPx()
                    }

                    val tickStroke = when {
                        isMajor -> 1.8.dp.toPx()
                        isMedium -> 1.3.dp.toPx()
                        else -> 1.dp.toPx()
                    }

                    val tickColor = when {
                        isMajor -> Color(0xFF00E5FF).copy(alpha = fadeFactor)
                        isMedium -> Color(0xFF38BDF8).copy(alpha = fadeFactor * 0.85f)
                        else -> Color(0xFF64748B).copy(alpha = fadeFactor * 0.5f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(arcX, arcY),
                        end = Offset(arcX + tickLength * radialDirX, arcY + tickLength * radialDirY),
                        strokeWidth = tickStroke
                    )
                }
            }

            // 1c. Draw Round Step Numbers (... 20, 30, 40, 50, 60, 70, 80 ...)
            // Numbers fit snugly inside ticks, skipping immediate focal center (|theta| < 0.12f)
            for (stepNum in 0..100 step 10) {
                val theta = (stepNum - animatedVolume) / 10f * angleStepRad

                if (kotlin.math.abs(theta) in 0.12f..0.70f) {
                    val fadeFactor = (1.0f - (kotlin.math.abs(theta) / 0.70f).pow(1.6f)).coerceIn(0.15f, 1.0f)

                    // Positioned snugly on the inner radial lane
                    val numberDistance = arcRadius + 18.dp.toPx()
                    val numX = if (isLeft) arcCenterX + numberDistance * cos(theta) else arcCenterX - numberDistance * cos(theta)
                    val numY = centerY - numberDistance * sin(theta)

                    val alphaByte = (fadeFactor * 200).roundToInt().coerceIn(30, 200)
                    textPaint.color = android.graphics.Color.argb(alphaByte, 148, 163, 184)
                    textPaint.isFakeBoldText = false
                    textPaint.textSize = with(density) { 9.5.sp.toPx() }

                    drawContext.canvas.nativeCanvas.drawText(
                        "$stepNum",
                        numX,
                        numY + textPaint.textSize * 0.35f,
                        textPaint
                    )
                }
            }

            // 1d. Subtle focal baseline indicator tick on the arc
            val focalArcX = if (isLeft) arcCenterX + arcRadius else arcCenterX - arcRadius
            val needleLen = 5.dp.toPx()
            val needleStart = if (isLeft) focalArcX + needleLen else focalArcX - needleLen
            drawLine(
                color = Color(0xFF00F5FF),
                start = Offset(needleStart, centerY),
                end = Offset(focalArcX, centerY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 2. Magnifier Lens Capsule Container with Embedded Real-Time Volume Readout
        // Snug 5dp internal padding from the curved front border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isLeft) (hudWidth - 53.dp) else 5.dp,
                    end = if (isLeft) 5.dp else (hudWidth - 53.dp)
                ),
            contentAlignment = if (isLeft) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Docked Volume Icon & "VOL" label neatly centered just above the purple box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "Volume Icon",
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "VOL",
                        color = Color(0xFF00F5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Magnifier Lens Capsule: purple border, purple translucent fill, centered numeric value
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 48.dp, height = 30.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0xFFBD00FF),
                            spotColor = Color(0xFFBD00FF)
                        )
                        .background(Color(0x33BD00FF), RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFFBD00FF), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "$percentageInt",
                        color = Color(0xFF00F5FF),
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}
