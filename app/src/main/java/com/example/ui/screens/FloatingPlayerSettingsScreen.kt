package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FloatingPlayerService
import com.example.storage.SettingsPreferencesManager

/**
 * Dedicated Floating Player Settings Screen (Sub-Screen under Look & Feel).
 *
 * Controls:
 * 1. Switch for "Floating Desktop Player": Show interactive floating player on home screen.
 * 2. Switch for "Floating Rainbow Edge Lighting": Dynamic rotating rainbow border around floating player.
 *    (Dimmed/disabled when Floating Desktop Player is OFF).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingPlayerSettingsScreen(
    prefsManager: SettingsPreferencesManager?,
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Floating Player",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Desktop Overlay Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("floating_settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            FloatingPlayerSettingsContent(
                prefsManager = prefsManager,
                onBack = onBack
            )
        }
    }
}

/**
 * Reusable content composable for the Floating Player Preferences,
 * rendered either in a standalone Scaffold or embedded within Settings sub-screen hierarchies.
 */
@Composable
fun FloatingPlayerSettingsContent(
    prefsManager: SettingsPreferencesManager?,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isAwaitingOverlayPermission by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val initialHasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    var isFloatingPlayerEnabled by remember {
        mutableStateOf(
            (prefsManager?.loadFloatingPlayerEnabled() ?: true) && initialHasPermission
        )
    }
    var isFloatingRainbowEdgeEnabled by remember {
        mutableStateOf(prefsManager?.loadFloatingRainbowEdgeEnabled() ?: true)
    }
    var floatingBorderSize by remember {
        mutableFloatStateOf(prefsManager?.loadFloatingBorderSize() ?: 2.5f)
    }
    var floatingAnimSpeed by remember {
        mutableFloatStateOf(prefsManager?.loadFloatingAnimationSpeed() ?: 2.5f)
    }
    var floatingWindowOpacity by remember {
        mutableFloatStateOf(prefsManager?.loadFloatingWindowOpacity() ?: 0.95f)
    }

    // Automatically detect when user returns from Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true

                if (isAwaitingOverlayPermission) {
                    isAwaitingOverlayPermission = false
                    if (hasPermission) {
                        // Automatically enable without extra user click
                        isFloatingPlayerEnabled = true
                        prefsManager?.saveFloatingPlayerEnabled(true)
                        FloatingPlayerService.start(context)
                    }
                } else {
                    isFloatingPlayerEnabled = (prefsManager?.loadFloatingPlayerEnabled() ?: true) && hasPermission
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Card Item 1: Switch for "Floating Desktop Player"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_floating_desktop_player_setting"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Floating Desktop Player",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Desktop Player",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Show interactive floating player on home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isFloatingPlayerEnabled,
                    onCheckedChange = { checked ->
                        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else true

                        if (checked) {
                            if (!hasOverlayPermission) {
                                isAwaitingOverlayPermission = true
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(fallback)
                                    } catch (_: Exception) {}
                                }
                            } else {
                                isFloatingPlayerEnabled = true
                                prefsManager?.saveFloatingPlayerEnabled(true)
                                FloatingPlayerService.start(context)
                            }
                        } else {
                            isAwaitingOverlayPermission = false
                            isFloatingPlayerEnabled = false
                            prefsManager?.saveFloatingPlayerEnabled(false)
                            FloatingPlayerService.stop(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("switch_floating_desktop_player_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card Item: Window Opacity Slider
        val isOpacityActive = isFloatingPlayerEnabled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isOpacityActive) 1f else 0.45f)
                .testTag("card_floating_opacity_setting"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Window Opacity",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Window Opacity",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${(floatingWindowOpacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Adjust transparency of floating player on desktop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = floatingWindowOpacity,
                    enabled = isOpacityActive,
                    onValueChange = {
                        floatingWindowOpacity = it
                        prefsManager?.saveFloatingWindowOpacity(it)
                        FloatingPlayerService.updateOpacity(context, it)
                    },
                    valueRange = 0.20f..1.0f,
                    steps = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_floating_window_opacity")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card Item 2: Switch for "Floating Rainbow Edge Lighting"
        // (Dimmed and disabled when Floating Desktop Player is OFF)
        val isRainbowActive = isFloatingPlayerEnabled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isRainbowActive) 1f else 0.45f)
                .testTag("card_floating_rainbow_edge_setting"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF0055).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF0055).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Floating Rainbow Edge Lighting",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Rainbow Edge Lighting",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Dynamic rotating rainbow border around floating player",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isFloatingRainbowEdgeEnabled && isRainbowActive,
                    enabled = isRainbowActive,
                    onCheckedChange = { checked ->
                        isFloatingRainbowEdgeEnabled = checked
                        prefsManager?.saveFloatingRainbowEdgeEnabled(checked)
                        FloatingPlayerService.updateRainbowEdge(context, checked, floatingBorderSize, floatingAnimSpeed)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("switch_floating_rainbow_edge_toggle")
                )
            }

            if (isFloatingRainbowEdgeEnabled && isRainbowActive) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Border Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Border Size",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(floatingBorderSize * 10).roundToInt() / 10f} dp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = floatingBorderSize,
                        onValueChange = {
                            floatingBorderSize = it
                            prefsManager?.saveFloatingBorderSize(it)
                            FloatingPlayerService.updateRainbowEdge(context, isFloatingRainbowEdgeEnabled, it, floatingAnimSpeed)
                        },
                        valueRange = 1.0f..8.0f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth().testTag("slider_floating_border_size")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Animation Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Animation Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(floatingAnimSpeed * 10).roundToInt() / 10f} s",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = floatingAnimSpeed,
                        onValueChange = {
                            floatingAnimSpeed = it
                            prefsManager?.saveFloatingAnimationSpeed(it)
                            FloatingPlayerService.updateRainbowEdge(context, isFloatingRainbowEdgeEnabled, floatingBorderSize, it)
                        },
                        valueRange = 0.5f..5.0f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth().testTag("slider_floating_anim_speed")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Info Card explaining Desktop Overlay mechanics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "When enabled, pressing Home or using the Minimize button on the Now Playing screen will launch the interactive floating player over your desktop with scrubbable seekbar and full playback controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
