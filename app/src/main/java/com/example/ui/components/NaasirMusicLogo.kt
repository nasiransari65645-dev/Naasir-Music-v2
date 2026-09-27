package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Official Electric Cyan & Cobalt Palette from Logo
val NaasirCyan = Color(0xFF00E5FF)
val NaasirCyanLight = Color(0xFF38BDF8)
val NaasirSkyBlue = Color(0xFF0284C7)
val NaasirCobalt = Color(0xFF2563EB)
val NaasirDeepBlue = Color(0xFF1D4ED8)
val NaasirNavyDark = Color(0xFF0E1A38)

/**
 * 3D Vector & Canvas implementation of the Naasir Music Logo.
 * Features the circular 3D swirl ribbon, interwoven 'N' musical note fusion,
 * metallic chrome specular highlights, and radiating top-right soundwaves.
 * Supports interactive zoom effect on click.
 */
@Composable
fun NaasirMusicLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    animatedWavePulse: Boolean = true,
    glowIntensity: Float = 0.8f,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val clickZoom = remember { Animatable(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "NaasirLogoPulse")

    val pulseScale by if (animatedWavePulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    val waveAlpha1 by if (animatedWavePulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "waveAlpha1"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    val waveAlpha2 by if (animatedWavePulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, delayMillis = 250, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "waveAlpha2"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.85f) }
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            coroutineScope.launch {
                clickZoom.animateTo(1.25f, tween(120, easing = FastOutSlowInEasing))
                clickZoom.animateTo(
                    1.0f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(clickZoom.value)
            .then(clickModifier)
            .testTag("naasir_music_logo"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Scale to canonical 512x512 coordinate space
            val scaleFactor = w / 512f
            scale(scaleFactor, pivot = Offset.Zero) {
                // 1. Ambient Radial Glow behind the emblem
                if (glowIntensity > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NaasirCyan.copy(alpha = 0.35f * glowIntensity),
                                NaasirCobalt.copy(alpha = 0.20f * glowIntensity),
                                Color.Transparent
                            ),
                            center = Offset(256f, 256f),
                            radius = 240f * pulseScale
                        ),
                        center = Offset(256f, 256f),
                        radius = 240f * pulseScale
                    )
                }

                // 2. Outer Swirl Ribbon - Bottom & Right Arc (Cobalt & Deep Blue)
                val bottomRibbonPath = Path().apply {
                    moveTo(140f, 370f)
                    cubicTo(180f, 425f, 240f, 440f, 295f, 425f)
                    cubicTo(350f, 410f, 385f, 370f, 398f, 315f)
                    cubicTo(408f, 265f, 395f, 215f, 370f, 175f)
                    lineTo(345f, 190f)
                    cubicTo(368f, 228f, 376f, 270f, 368f, 310f)
                    cubicTo(356f, 355f, 324f, 388f, 280f, 398f)
                    cubicTo(235f, 408f, 190f, 392f, 155f, 348f)
                    close()
                }
                drawPath(
                    path = bottomRibbonPath,
                    brush = Brush.linearGradient(
                        colors = listOf(NaasirDeepBlue, NaasirSkyBlue, NaasirCyan),
                        start = Offset(140f, 420f),
                        end = Offset(398f, 200f)
                    )
                )

                // 3. Outer Swirl Ribbon - Left Arc (Electric Cyan & Sky Blue)
                val leftRibbonPath = Path().apply {
                    moveTo(105f, 255f)
                    cubicTo(105f, 185f, 150f, 130f, 215f, 115f)
                    cubicTo(255f, 105f, 292f, 115f, 320f, 135f)
                    lineTo(300f, 165f)
                    cubicTo(280f, 148f, 248f, 138f, 218f, 145f)
                    cubicTo(168f, 158f, 135f, 200f, 135f, 255f)
                    cubicTo(135f, 300f, 158f, 340f, 190f, 362f)
                    lineTo(168f, 390f)
                    cubicTo(128f, 360f, 105f, 315f, 105f, 255f)
                    close()
                }
                drawPath(
                    path = leftRibbonPath,
                    brush = Brush.linearGradient(
                        colors = listOf(NaasirSkyBlue, NaasirCyan, NaasirCyanLight),
                        start = Offset(105f, 310f),
                        end = Offset(320f, 115f)
                    )
                )

                // 3D Specular Ridge on Left Arc
                val leftRidgePath = Path().apply {
                    moveTo(118f, 245f)
                    cubicTo(118f, 195f, 152f, 148f, 210f, 132f)
                    cubicTo(240f, 122f, 272f, 126f, 295f, 142f)
                }
                drawPath(
                    path = leftRidgePath,
                    color = Color.White.copy(alpha = 0.55f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // 4. Musical Note Head (Bottom-Left Bulb)
                val noteHeadPath = Path().apply {
                    moveTo(152f, 345f)
                    cubicTo(125f, 345f, 108f, 322f, 112f, 298f)
                    cubicTo(116f, 274f, 140f, 260f, 165f, 260f)
                    cubicTo(190f, 260f, 206f, 284f, 202f, 308f)
                    cubicTo(198f, 332f, 175f, 345f, 152f, 345f)
                    close()
                }
                drawPath(
                    path = noteHeadPath,
                    brush = Brush.linearGradient(
                        colors = listOf(NaasirNavyDark, NaasirCobalt, NaasirSkyBlue),
                        start = Offset(108f, 320f),
                        end = Offset(202f, 265f)
                    )
                )

                // Note Head Specular Glint
                val noteGlint = Path().apply {
                    moveTo(142f, 286f)
                    cubicTo(155f, 278f, 172f, 278f, 178f, 288f)
                }
                drawPath(
                    path = noteGlint,
                    color = Color.White.copy(alpha = 0.65f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // 5. Central 3D 'N' Note: Left Stem & Valley (Electric Cyan Tube)
                val leftStemPath = Path().apply {
                    moveTo(165f, 300f)
                    lineTo(170f, 185f)
                    cubicTo(172f, 160f, 192f, 148f, 215f, 165f)
                    cubicTo(230f, 178f, 275f, 265f, 275f, 265f)
                    lineTo(275f, 290f)
                    lineTo(220f, 195f)
                    cubicTo(215f, 188f, 208f, 190f, 208f, 198f)
                    lineTo(200f, 312f)
                    close()
                }
                drawPath(
                    path = leftStemPath,
                    brush = Brush.linearGradient(
                        colors = listOf(NaasirSkyBlue, NaasirCyan, NaasirCyanLight),
                        start = Offset(165f, 310f),
                        end = Offset(275f, 150f)
                    )
                )

                // 6. Central 3D 'N' Note: Diagonal, Right Upright & Note Flag
                val rightStemPath = Path().apply {
                    moveTo(210f, 185f)
                    cubicTo(232f, 215f, 275f, 285f, 285f, 298f)
                    cubicTo(298f, 312f, 318f, 305f, 318f, 288f)
                    lineTo(322f, 160f)
                    cubicTo(328f, 125f, 362f, 105f, 392f, 128f)
                    cubicTo(412f, 142f, 415f, 165f, 398f, 182f)
                    cubicTo(382f, 195f, 360f, 190f, 355f, 215f)
                    lineTo(348f, 328f)
                    cubicTo(342f, 352f, 318f, 365f, 300f, 345f)
                    lineTo(242f, 258f)
                    lineTo(258f, 235f)
                    lineTo(300f, 305f)
                    cubicTo(308f, 318f, 322f, 312f, 322f, 298f)
                    lineTo(324f, 198f)
                    cubicTo(330f, 165f, 350f, 148f, 375f, 145f)
                    cubicTo(386f, 143f, 388f, 136f, 380f, 130f)
                    cubicTo(362f, 118f, 338f, 130f, 334f, 160f)
                    lineTo(322f, 282f)
                    cubicTo(318f, 294f, 310f, 295f, 298f, 278f)
                    lineTo(235f, 178f)
                    cubicTo(222f, 158f, 205f, 162f, 205f, 182f)
                    close()
                }
                drawPath(
                    path = rightStemPath,
                    brush = Brush.linearGradient(
                        colors = listOf(NaasirCyan, NaasirSkyBlue, NaasirCobalt, NaasirDeepBlue),
                        start = Offset(210f, 350f),
                        end = Offset(412f, 110f)
                    )
                )

                // 7. 3D Tubular Metallic Spine Highlights
                val leftSpineHighlight = Path().apply {
                    moveTo(185f, 190f)
                    cubicTo(188f, 168f, 202f, 162f, 215f, 172f)
                    cubicTo(230f, 185f, 272f, 255f, 282f, 275f)
                }
                drawPath(
                    path = leftSpineHighlight,
                    color = Color.White.copy(alpha = 0.70f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                val rightSpineHighlight = Path().apply {
                    moveTo(322f, 290f)
                    lineTo(324f, 165f)
                    cubicTo(328f, 138f, 352f, 122f, 380f, 132f)
                }
                drawPath(
                    path = rightSpineHighlight,
                    color = Color.White.copy(alpha = 0.65f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                // 8. Top-Right Radiating Soundwaves (Broadcasting Arcs)
                // Inner Soundwave Arc
                val innerWavePath = Path().apply {
                    moveTo(410f, 115f)
                    cubicTo(428f, 140f, 428f, 168f, 410f, 192f)
                }
                drawPath(
                    path = innerWavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NaasirCyan.copy(alpha = waveAlpha1),
                            NaasirCobalt.copy(alpha = waveAlpha1)
                        ),
                        start = Offset(410f, 115f),
                        end = Offset(428f, 192f)
                    ),
                    style = Stroke(width = 15f, cap = StrokeCap.Round)
                )

                // Outer Soundwave Arc
                val outerWavePath = Path().apply {
                    moveTo(445f, 90f)
                    cubicTo(475f, 125f, 475f, 182f, 445f, 215f)
                }
                drawPath(
                    path = outerWavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NaasirCyan.copy(alpha = waveAlpha2),
                            NaasirDeepBlue.copy(alpha = waveAlpha2)
                        ),
                        start = Offset(445f, 90f),
                        end = Offset(475f, 215f)
                    ),
                    style = Stroke(width = 15f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
