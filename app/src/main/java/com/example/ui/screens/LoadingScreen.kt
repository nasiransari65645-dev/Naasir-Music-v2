package com.example.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NaasirCobalt
import com.example.ui.components.NaasirCyan
import com.example.ui.components.NaasirMusicLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Quick, high-impact Loading Screen featuring the 3D Naasir Music Logo
 * with dynamic Zoom-In and Zoom-Through effects upon clicking/opening the app icon.
 * Pure and snappy: No progress bar, no percentage counters, no progress phases.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    onLoadingComplete: () -> Unit = {},
    isManualPreview: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Entrance Zoom Animation: Dynamic zoom-in when icon is clicked
    val zoomScale = remember { Animatable(0.25f) }
    val contentAlpha = remember { Animatable(0.0f) }

    // Click zoom interactive punch
    val tapZoom = remember { Animatable(1.0f) }

    // Quick loading sequence with zoom-in then smooth zoom-through exit
    LaunchedEffect(Unit) {
        // Dynamic Zoom-in on app launch (after clicking icon)
        launch {
            zoomScale.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
            zoomScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 240, easing = LinearEasing)
            )
        }

        // Quick snappy display time (no progress bar, purely swift visual feedback)
        val stayDuration = if (isManualPreview) 1500L else 950L
        delay(stayDuration)

        // Exit Zoom-through into the main app interface
        launch {
            zoomScale.animateTo(
                targetValue = 1.35f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 240, easing = LinearEasing)
            )
        }
        delay(260L)
        onLoadingComplete()
    }

    // Ambient background subtle pulse
    val infiniteTransition = rememberInfiniteTransition(label = "quick_loading_pulse")
    val ambientGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Clicking on screen/icon triggers instant zoom burst into the app
                coroutineScope.launch {
                    tapZoom.animateTo(1.30f, tween(120, easing = FastOutSlowInEasing))
                    onLoadingComplete()
                }
            }
            .testTag("app_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Radial Background Cyan Glow
        Box(
            modifier = Modifier
                .size(360.dp)
                .scale(zoomScale.value * ambientGlow)
                .alpha(contentAlpha.value * 0.7f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NaasirCyan.copy(alpha = 0.22f),
                            NaasirCobalt.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Centered Content with Zoom Effect
        Column(
            modifier = Modifier
                .scale(zoomScale.value * tapZoom.value)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D Logo from uploaded image with Zoom Effect
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                NaasirMusicLogo(
                    size = 190.dp,
                    animatedWavePulse = true,
                    glowIntensity = ambientGlow,
                    onClick = {
                        coroutineScope.launch {
                            tapZoom.animateTo(1.35f, tween(140, easing = FastOutSlowInEasing))
                            onLoadingComplete()
                        }
                    }
                )
            }

            // App Brand Name (Clean display typography)
            Text(
                text = "NAASIR MUSIC",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontSize = 25.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Tag
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NaasirCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HI-RES AUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaasirCyan,
                    letterSpacing = 2.5.sp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
