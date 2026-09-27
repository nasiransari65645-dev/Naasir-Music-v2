package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeon
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Studio Pro Rack-Style Rotary Knob Composable.
 * Features high-precision touch/drag tracking, 270° rotary sweep arc,
 * glowing neon indicator gauge, and digital LCD readout badge.
 */
@Composable
fun RackRotaryKnob(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    title: String,
    readoutText: String,
    minLabel: String,
    maxLabel: String,
    modifier: Modifier = Modifier,
    knobSize: Dp = 72.dp,
    activeColor: Color = CyanNeon,
    isEnabled: Boolean = true,
    testTag: String = "rack_knob_${title.lowercase().replace(" ", "_")}"
) {
    val minVal = valueRange.start
    val maxVal = valueRange.endInclusive
    val rangeSpan = (maxVal - minVal).coerceAtLeast(0.001f)
    val fraction = ((value - minVal) / rangeSpan).coerceIn(0f, 1f)

    // Standard audio rack knob has a 270° sweep (-135° to +135°, with 0° pointing straight up)
    // In Canvas drawArc: 0° is 3 o'clock (right).
    // So 135° is bottom-left, sweeping 270° clockwise to 45° (bottom-right).
    val startAngleDeg = 135f
    val sweepAngleDeg = 270f
    val currentSweep = sweepAngleDeg * fraction

    // Drag sensitivity accumulator
    var dragAccumulator by remember { mutableFloatStateOf(value) }

    Column(
        modifier = modifier
            .testTag(testTag)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Knob Header Label
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEnabled) Color(0xFFE2E8F0) else Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontSize = 11.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Knob Dial Container with Rotary Touch Detection
        Box(
            modifier = Modifier
                .size(knobSize + 22.dp)
                .pointerInput(isEnabled, valueRange) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { dragAccumulator = value },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Drag up reduces Y -> increases value; drag down increases Y -> decreases value
                            // 200dp vertical drag traverses full range
                            val step = -dragAmount.y / 240f * rangeSpan
                            val newVal = (dragAccumulator + step).coerceIn(minVal, maxVal)
                            dragAccumulator = newVal
                            onValueChange(newVal)
                        }
                    )
                }
                .pointerInput(isEnabled, valueRange) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures { offset ->
                        // Angular tap detection around center
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Map angle from 135°..360° and 0°..45° to 0..1 fraction
                        val arcAngle = if (angleDeg >= 135f) {
                            angleDeg - 135f
                        } else if (angleDeg <= 45f) {
                            (360f - 135f) + angleDeg
                        } else {
                            // In bottom dead-zone (45° to 135°), snap to closest boundary
                            if (angleDeg < 90f) 270f else 0f
                        }
                        val tappedFraction = (arcAngle / sweepAngleDeg).coerceIn(0f, 1f)
                        val newVal = minVal + (tappedFraction * rangeSpan)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Arc Gauge Canvas
            Canvas(modifier = Modifier.size(knobSize + 20.dp)) {
                val canvasSize = size.minDimension
                val strokeWidth = 4.dp.toPx()
                val radius = (canvasSize - strokeWidth) / 2f
                val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)

                // 1. Background Inactive Arc Track
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepAngleDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Active Glowing Neon Arc Track
                if (isEnabled && currentSweep > 1f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                activeColor.copy(alpha = 0.6f),
                                activeColor,
                                if (activeColor == CyanNeon) PurpleNeon else AmberGlow
                            )
                        ),
                        startAngle = startAngleDeg,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth + 0.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. Subtle tick markers around perimeter
                val center = Offset(size.width / 2f, size.height / 2f)
                val tickRadius = radius + 3.dp.toPx()
                for (i in 0..10) {
                    val tickFraction = i / 10f
                    val tickAngleDeg = startAngleDeg + (sweepAngleDeg * tickFraction)
                    val tickRad = Math.toRadians(tickAngleDeg.toDouble())
                    val tickStart = Offset(
                        (center.x + (tickRadius - 2.dp.toPx()) * cos(tickRad)).toFloat(),
                        (center.y + (tickRadius - 2.dp.toPx()) * sin(tickRad)).toFloat()
                    )
                    val tickEnd = Offset(
                        (center.x + tickRadius * cos(tickRad)).toFloat(),
                        (center.y + tickRadius * sin(tickRad)).toFloat()
                    )
                    val isPastValue = tickFraction <= fraction && isEnabled
                    drawLine(
                        color = if (isPastValue) activeColor.copy(alpha = 0.8f) else Color(0xFF334155),
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = if (i % 5 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Central Physical Metallic Knob Body
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF070B14)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        if (isEnabled) activeColor.copy(alpha = 0.45f) else Color(0xFF334155),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Pointer Needle on Knob Face
                Canvas(modifier = Modifier.size(knobSize)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val pointerAngleDeg = startAngleDeg + currentSweep
                    val pointerRad = Math.toRadians(pointerAngleDeg.toDouble())
                    val needleLen = (size.minDimension / 2f) - 6.dp.toPx()

                    // Needle Line
                    val needleEnd = Offset(
                        (center.x + needleLen * cos(pointerRad)).toFloat(),
                        (center.y + needleLen * sin(pointerRad)).toFloat()
                    )
                    val needleStart = Offset(
                        (center.x + (needleLen * 0.35f) * cos(pointerRad)).toFloat(),
                        (center.y + (needleLen * 0.35f) * sin(pointerRad)).toFloat()
                    )

                    drawLine(
                        color = if (isEnabled) activeColor else Color(0xFF94A3B8),
                        start = needleStart,
                        end = needleEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center Metallic Cap Dot
                    drawCircle(
                        color = if (isEnabled) Color(0xFF1E293B) else Color(0xFF0F172A),
                        radius = 7.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = if (isEnabled) activeColor.copy(alpha = 0.8f) else Color(0xFF475569),
                        radius = 2.5.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        // Min & Max range indicator marks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 9.sp
            )
            Text(
                text = maxLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Digital LCD Readout Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isEnabled) Color(0xFF030712) else Color(0xFF0B101D))
                .border(
                    1.dp,
                    if (isEnabled) activeColor.copy(alpha = 0.5f) else Color(0xFF1E293B),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = readoutText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isEnabled) activeColor else Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
