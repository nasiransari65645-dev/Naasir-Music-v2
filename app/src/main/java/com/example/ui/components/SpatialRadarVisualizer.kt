package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutoRotateDirection
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpatialRadarVisualizer(
    angleDegrees: Float,
    panValue: Float,
    isRotating: Boolean,
    isPlaying: Boolean,
    direction: AutoRotateDirection,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "360° SOUNDSTAGE RADAR",
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRotating) {
                    if (direction == AutoRotateDirection.LEFT_TO_RIGHT) "Rotating: L ➔ R" else "Rotating: R ➔ L"
                } else {
                    "Manual Balance"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isRotating) CyanNeon else AmberGlow,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center circular Radar Canvas
        Box(
            modifier = Modifier
                .size(200.dp)
                .testTag("spatial_radar_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.width / 2f) - 16.dp.toPx()

                // Background grid rings
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    radius = radius * 0.66f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF1E293B).copy(alpha = 0.3f),
                    radius = radius * 0.33f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Crosshairs
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.4f),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.4f),
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 1.dp.toPx()
                )

                // Outer decorative soundwave arc
                if (isPlaying) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(CyanNeon, PurpleNeon, CyanNeon)
                        ),
                        startAngle = angleDegrees - 40f,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Sound Source Orb position on orbit
                // Angle 0: Top, 90: Right, 180: Bottom, 270: Left
                val rad = Math.toRadians((angleDegrees - 90.0)) // -90 so 0 is at Top
                val orbX = center.x + (radius * cos(rad)).toFloat()
                val orbY = center.y + (radius * sin(rad)).toFloat()

                // Glow trail
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanNeon.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(orbX, orbY),
                        radius = 24.dp.toPx() * (if (isPlaying) pulseScale else 1.0f)
                    ),
                    radius = 24.dp.toPx() * (if (isPlaying) pulseScale else 1.0f),
                    center = Offset(orbX, orbY)
                )

                // Solid audio orb
                drawCircle(
                    color = CyanNeon,
                    radius = 8.dp.toPx(),
                    center = Offset(orbX, orbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(orbX, orbY)
                )
            }

            // Head & Headphones in center
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(2.dp, PurpleNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Listener Head",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Left & Right Channel Power Distribution
        // pan is -1 (full Left) to +1 (full Right)
        val leftPower = ((1f - panValue) / 2f).coerceIn(0.05f, 1f)
        val rightPower = ((1f + panValue) / 2f).coerceIn(0.05f, 1f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left channel
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LEFT EAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (panValue < -0.2f) CyanNeon else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(leftPower * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { leftPower },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyanNeon,
                    trackColor = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right channel
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RIGHT EAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (panValue > 0.2f) CyanNeon else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(rightPower * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { rightPower },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PurpleNeon,
                    trackColor = Color(0xFF1E293B)
                )
            }
        }
    }
}
