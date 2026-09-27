package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlayerState
import com.example.model.AutoRotateDirection
import com.example.model.DpsProfile
import com.example.model.SpatialEnvironment
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeon
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SpatialSubScreen {
    ACOUSTIC_ENVIRONMENT,
    ROTATION_360,
    VIRTUALIZER,
    DPS_EFFECTS
}

/**
 * 3D Effects Screen:
 * Features a main dashboard with exactly 4 primary items navigating to dedicated sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialScreen(
    playerState: PlayerState? = null,
    onNavigateBack: () -> Unit = {},
    // Acoustic Environment callbacks
    onSetReverbMaster: (Boolean) -> Unit = {},
    onSetReverbWetDryMix: (Float) -> Unit = {},
    onSetReverbDecayTime: (Float) -> Unit = {},
    onSetReverbPreset: (SpatialEnvironment) -> Unit = {},
    // 3D 360 Rotation callbacks
    onSetRotationMaster: (Boolean) -> Unit = {},
    onSetRotationSpeedMultiplier: (Float) -> Unit = {},
    onSetRotationDirection: (AutoRotateDirection) -> Unit = {},
    // 3D Virtualizer callbacks
    onSetVirtualizerMaster: (Boolean) -> Unit = {},
    onSetVirtualizerStrength: (Int) -> Unit = {},
    onSetVirtualizerMode: (String) -> Unit = {},
    // DPS Effects callbacks
    onSetDpsMaster: (Boolean) -> Unit = {},
    onSetDpsProfile: (DpsProfile) -> Unit = {},
    onSetDpsBassBoost: (Int) -> Unit = {},
    onSetDpsClarity: (Int) -> Unit = {},
    // Legacy callbacks for compatibility
    onToggleAutoRotate: (Boolean) -> Unit = {},
    onSetAutoRotateSpeed: (Float) -> Unit = {},
    onSetDirection: (AutoRotateDirection) -> Unit = {},
    onSetStereoWidth: (Float) -> Unit = {},
    onToggleRandomRotation: (Boolean) -> Unit = {},
    onSetManualPan: (Float) -> Unit = {},
    onSetVirtualizer: (Boolean, Int) -> Unit = { _, _ -> },
    onSetBassBoost: (Boolean, Int) -> Unit = { _, _ -> },
    onSetEnvironment: (SpatialEnvironment) -> Unit = {},
    onSetSoundstageDistance: (String) -> Unit = {},
    onSetElevation: (String) -> Unit = {},
    onSetHeadShadow: (Boolean) -> Unit = {},
    onSetBinauralWidener: (Float) -> Unit = {},
    onSetDelayEnabled: (Boolean) -> Unit = {},
    onSetDelayTimeMs: (Int) -> Unit = {},
    onSetDelayFeedbackPercent: (Int) -> Unit = {},
    onSetDelayMixPercent: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubScreen by remember { mutableStateOf<SpatialSubScreen?>(null) }

    // Intercept hardware/system back button if a sub-screen is open
    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    val state = playerState ?: PlayerState()

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 2 } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "SpatialSubScreenAnimation",
        modifier = modifier.fillMaxSize()
    ) { currentSubScreen ->
        when (currentSubScreen) {
            null -> {
                SpatialMainDashboard(
                    onBackClick = onNavigateBack,
                    onOpenSubScreen = { activeSubScreen = it }
                )
            }

            SpatialSubScreen.ACOUSTIC_ENVIRONMENT -> {
                AcousticEnvironmentSubScreen(
                    reverbMaster = state.reverbMasterEnabled,
                    wetDryMix = state.reverbWetDryMix,
                    decayTime = state.reverbDecayTime,
                    currentPreset = state.spatialEnvironment,
                    onToggleMaster = onSetReverbMaster,
                    onWetDryMixChange = onSetReverbWetDryMix,
                    onDecayTimeChange = onSetReverbDecayTime,
                    onSelectPreset = {
                        onSetReverbPreset(it)
                        onSetEnvironment(it)
                    },
                    onBackClick = { activeSubScreen = null }
                )
            }

            SpatialSubScreen.ROTATION_360 -> {
                Rotation360SubScreen(
                    isMasterEnabled = state.autoRotateEnabled,
                    speedMultiplier = state.rotationSpeedMultiplier,
                    direction = state.autoRotateDirection,
                    currentAngleDegrees = state.currentAngleDegrees,
                    onToggleMaster = {
                        onSetRotationMaster(it)
                        onToggleAutoRotate(it)
                    },
                    onSpeedMultiplierChange = onSetRotationSpeedMultiplier,
                    onDirectionChange = {
                        onSetRotationDirection(it)
                        onSetDirection(it)
                    },
                    onBackClick = { activeSubScreen = null }
                )
            }

            SpatialSubScreen.VIRTUALIZER -> {
                VirtualizerSubScreen(
                    isMasterEnabled = state.virtualizerEnabled,
                    strength = state.virtualizerStrength,
                    mode = state.virtualizerMode,
                    onToggleMaster = {
                        onSetVirtualizerMaster(it)
                        onSetVirtualizer(it, state.virtualizerStrength)
                    },
                    onStrengthChange = {
                        onSetVirtualizerStrength(it)
                        onSetVirtualizer(state.virtualizerEnabled, it)
                    },
                    onModeChange = onSetVirtualizerMode,
                    onBackClick = { activeSubScreen = null }
                )
            }

            SpatialSubScreen.DPS_EFFECTS -> {
                DpsEffectsSubScreen(
                    isMasterEnabled = state.dpsEnabled,
                    currentProfile = state.dpsProfile,
                    bassBoost = state.dpsBassBoost,
                    clarity = state.dpsClarity,
                    onToggleMaster = onSetDpsMaster,
                    onProfileChange = onSetDpsProfile,
                    onBassBoostChange = onSetDpsBassBoost,
                    onClarityChange = onSetDpsClarity,
                    onBackClick = { activeSubScreen = null }
                )
            }
        }
    }
}

/**
 * Main "3D Effects" Dashboard Screen:
 * Top AppBar with Title "3D Effects" and Back button.
 * Clean vertical list of EXACTLY 4 category cards without toggle switches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpatialMainDashboard(
    onBackClick: () -> Unit,
    onOpenSubScreen: (SpatialSubScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("spatial_main_dashboard")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "3D Effects",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("spatial_dashboard_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Acoustic Environment
            item {
                SpatialCategoryCard(
                    title = "Acoustic Environment",
                    subtitle = "Simulate room acoustics and reverb types",
                    icon = Icons.Default.SurroundSound,
                    accentColor = Color(0xFF10B981), // Emerald glow
                    testTag = "spatial_card_acoustic",
                    onClick = { onOpenSubScreen(SpatialSubScreen.ACOUSTIC_ENVIRONMENT) }
                )
            }

            // 2. 3D 360° Rotation
            item {
                SpatialCategoryCard(
                    title = "3D 360° Rotation",
                    subtitle = "Dynamic spatialized audio rotation control",
                    icon = Icons.Default.Sync,
                    accentColor = PurpleNeon, // Violet glow
                    testTag = "spatial_card_rotation",
                    onClick = { onOpenSubScreen(SpatialSubScreen.ROTATION_360) }
                )
            }

            // 3. 3D Virtualizer
            item {
                SpatialCategoryCard(
                    title = "3D Virtualizer",
                    subtitle = "Expand stereo field for immersive surround sound",
                    icon = Icons.Default.Waves,
                    accentColor = CyanNeon, // Cyan glow
                    testTag = "spatial_card_virtualizer",
                    onClick = { onOpenSubScreen(SpatialSubScreen.VIRTUALIZER) }
                )
            }

            // 4. DPS Effects
            item {
                SpatialCategoryCard(
                    title = "DPS Effects",
                    subtitle = "Digital Power Station audio enhancement",
                    icon = Icons.Default.ElectricBolt,
                    accentColor = Color(0xFFF59E0B), // Amber glow
                    testTag = "spatial_card_dps",
                    onClick = { onOpenSubScreen(SpatialSubScreen.DPS_EFFECTS) }
                )
            }
        }
    }
}

/**
 * Outer Category Card Component adhering to:
 * - Large neon glow circular icon on the left (52dp box).
 * - Bold title and muted description in center.
 * - Far right: ONLY clean chevron arrow ( > ).
 * - NO toggle switch.
 */
@Composable
private fun SpatialCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large neon glow circular icon container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and Subtitle in center
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Far right: ONLY clean chevron arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Sub-Screen 1: Acoustic Environment
 * - Top AppBar with Back button
 * - Master ON/OFF Switch
 * - Wet/Dry Mix Slider (0% to 100%)
 * - Room Decay Slider (0.1s to 5.0s)
 * - Presets list/chips: Studio Dry, Acoustic Room, Live Stage, Concert Hall, Great Hall, Mega Stadium, Echo Chamber
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcousticEnvironmentSubScreen(
    reverbMaster: Boolean,
    wetDryMix: Float,
    decayTime: Float,
    currentPreset: SpatialEnvironment,
    onToggleMaster: (Boolean) -> Unit,
    onWetDryMixChange: (Float) -> Unit,
    onDecayTimeChange: (Float) -> Unit,
    onSelectPreset: (SpatialEnvironment) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("acoustic_environment_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Acoustic Environment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("acoustic_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reverb Strength Slider Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acoustic_reverb_strength_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reverb Strength",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${(wetDryMix * 100).roundToInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = wetDryMix,
                        onValueChange = {
                            onWetDryMixChange(it)
                            if (it > 0.01f && !reverbMaster) {
                                onToggleMaster(true)
                            } else if (it <= 0.01f && reverbMaster) {
                                onToggleMaster(false)
                            }
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("acoustic_strength_slider")
                    )
                }
            }

            // Presets Selection 2-Column Grid
            Text(
                text = "ENVIRONMENT PRESETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            val presets = SpatialEnvironment.values().toList()

            presets.chunked(2).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPresets.forEach { env ->
                        val isSelected = currentPreset == env
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectPreset(env)
                                    onWetDryMixChange(env.wetMix)
                                    onDecayTimeChange(env.decaySeconds)
                                    if (env != SpatialEnvironment.STUDIO_DRY && !reverbMaster) {
                                        onToggleMaster(true)
                                    }
                                }
                                .testTag("preset_${env.name.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = env.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = env.decayLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowPresets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Sub-Screen 2: 3D 360° Rotation
 * - Top AppBar with Back button
 * - Master ON/OFF Switch
 * - Rotation Speed Slider (0.1x to 5.0x multiplier)
 * - Direction Toggle: Left-to-Right vs Right-to-Left
 * - Visual 360° orbital ring with moving audio node
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Rotation360SubScreen(
    isMasterEnabled: Boolean,
    speedMultiplier: Float,
    direction: AutoRotateDirection,
    currentAngleDegrees: Float,
    onToggleMaster: (Boolean) -> Unit,
    onSpeedMultiplierChange: (Float) -> Unit,
    onDirectionChange: (AutoRotateDirection) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("rotation_360_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "3D 360° Rotation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("rotation_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master ON/OFF Switch Card
            MasterSwitchCard(
                title = "360° Orbital Rotation",
                subtitle = if (isMasterEnabled) "Active • Dynamic spatial soundstage running" else "Disabled • Tap switch to activate spatial 360° orbit controls",
                isEnabled = isMasterEnabled,
                accentColor = MaterialTheme.colorScheme.primary,
                onToggle = onToggleMaster,
                testTag = "rotation_master_switch"
            )

            // Collapsible Fold/Unfold Animation for 360 Rotation Controls
            AnimatedVisibility(
                visible = isMasterEnabled,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual 360° Orbital Ring Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outline
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                val surfaceColor = MaterialTheme.colorScheme.surface

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ORBITAL SOUNDSTAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .testTag("orbital_ring_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = (size.minDimension / 2f) - 18.dp.toPx()

                            // Outer dashed orbital guide ring
                            drawCircle(
                                color = outlineColor.copy(alpha = 0.5f),
                                radius = radius,
                                center = Offset(centerX, centerY),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            )

                            // Glowing subtle path if enabled
                            if (isMasterEnabled) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.25f),
                                    radius = radius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }

                            // Center listener representation
                            drawCircle(
                                color = surfaceVariant,
                                radius = 22.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = if (isMasterEnabled) primaryColor.copy(alpha = 0.4f) else outlineColor,
                                radius = 20.dp.toPx(),
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = if (isMasterEnabled) primaryColor else onSurfaceVariant,
                                radius = 6.dp.toPx(),
                                center = Offset(centerX, centerY)
                            )

                            // Calculate orbiting audio source dot position based on currentAngleDegrees
                            val angleRad = Math.toRadians(currentAngleDegrees.toDouble())
                            val orbiterX = centerX + radius * sin(angleRad).toFloat()
                            val orbiterY = centerY - radius * cos(angleRad).toFloat()

                            // Outer glow for orbiter
                            if (isMasterEnabled) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.35f),
                                    radius = 16.dp.toPx(),
                                    center = Offset(orbiterX, orbiterY)
                                )
                            }
                            // Main orbiter node
                            drawCircle(
                                color = if (isMasterEnabled) primaryColor else onSurfaceVariant,
                                radius = 9.dp.toPx(),
                                center = Offset(orbiterX, orbiterY)
                            )
                            drawCircle(
                                color = surfaceColor,
                                radius = 3.dp.toPx(),
                                center = Offset(orbiterX, orbiterY)
                            )
                        }

                        // Labels for Left, Right, Front, Back
                        Text(
                            text = "FRONT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        Text(
                            text = "BACK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        Text(
                            text = "L",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        Text(
                            text = "R",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isMasterEnabled) "Active Orbit: ${currentAngleDegrees.roundToInt()}°" else "Orbit Paused (Dead Center)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMasterEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Rotation Speed Multiplier Slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rotation Speed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1fx", speedMultiplier),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust orbital rotation velocity (0.1x slow pan to 5.0x fast orbit)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = speedMultiplier,
                        onValueChange = onSpeedMultiplierChange,
                        valueRange = 0.1f..5.0f,
                        enabled = isMasterEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rotation_speed_slider")
                    )
                }
            }

            // Direction Selector: Left-to-Right vs Right-to-Left
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Orbit Direction",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Direction of sound panning trajectory around head",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isLeftToRight = direction == AutoRotateDirection.LEFT_TO_RIGHT
                        DirectionOptionCard(
                            label = "Left to Right",
                            isSelected = isLeftToRight,
                            enabled = isMasterEnabled,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onDirectionChange(AutoRotateDirection.LEFT_TO_RIGHT) }
                        )
                        DirectionOptionCard(
                            label = "Right to Left",
                            isSelected = !isLeftToRight,
                            enabled = isMasterEnabled,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onDirectionChange(AutoRotateDirection.RIGHT_TO_LEFT) }
                        )
                    }
                }
            }
                }
            }
        }
    }
}

@Composable
private fun DirectionOptionCard(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Sub-Screen 3: 3D Virtualizer
 * - Top AppBar with Back button
 * - Master ON/OFF Switch
 * - Virtualizer Strength Slider (0% to 100%)
 * - Mode Selector: "Headphones (Binaural)" vs "External Speakers (Transaural)"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VirtualizerSubScreen(
    isMasterEnabled: Boolean,
    strength: Int,
    mode: String,
    onToggleMaster: (Boolean) -> Unit,
    onStrengthChange: (Int) -> Unit,
    onModeChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("virtualizer_sub_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "3D Virtualizer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("virtualizer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master ON/OFF Switch Card
            MasterSwitchCard(
                title = "Virtualizer Surround",
                subtitle = "Expand stereo acoustic field for immersive spatial listening",
                isEnabled = isMasterEnabled,
                accentColor = MaterialTheme.colorScheme.primary,
                onToggle = onToggleMaster,
                testTag = "virtualizer_master_switch"
            )

            // Strength Slider (0% to 100%)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val strengthPercent = (strength / 10f).roundToInt().coerceIn(0, 100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Virtualizer Strength",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$strengthPercent%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Intensity of stereo widening and acoustic phase separation",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = strength.toFloat(),
                        onValueChange = { onStrengthChange(it.roundToInt().coerceIn(0, 1000)) },
                        valueRange = 0f..1000f,
                        enabled = isMasterEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("virtualizer_strength_slider")
                    )
                }
            }

            // Mode Selector: Headphones (Binaural) vs External Speakers (Transaural)
            Text(
                text = "VIRTUALIZATION MODE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            val modes = listOf(
                Triple("HEADPHONES", "Headphones (Binaural)", "HRTF filters optimized for in-ear / over-ear isolation"),
                Triple("SPEAKERS", "External Speakers (Transaural)", "Crosstalk cancellation tuned for stereo speaker arrays")
            )

            modes.forEach { (modeKey, title, desc) ->
                val isSelected = mode.equals(modeKey, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isMasterEnabled) { onModeChange(modeKey) }
                        .testTag("virt_mode_${modeKey.lowercase()}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (modeKey == "HEADPHONES") Icons.Default.Headphones else Icons.Default.SpeakerGroup,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sub-Screen 4: DPS Effects (Digital Power Station)
 * - Top AppBar with Back button
 * - Master ON/OFF Switch
 * - DPS Profiles: Music, Movie, Voice, Gaming
 * - Dynamic Bass Boost slider (0 to 100%)
 * - Clarity enhancer slider (0 to 100%)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DpsEffectsSubScreen(
    isMasterEnabled: Boolean,
    currentProfile: DpsProfile,
    bassBoost: Int = 0,
    clarity: Int = 0,
    onToggleMaster: (Boolean) -> Unit,
    onProfileChange: (DpsProfile) -> Unit,
    onBassBoostChange: (Int) -> Unit = {},
    onClarityChange: (Int) -> Unit = {},
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dps_effects_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "DPS Effects",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("dps_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master ON/OFF Switch Card
            MasterSwitchCard(
                title = "Digital Power Station",
                subtitle = "Hardware-accelerated dynamic real-time audio remastering engine",
                isEnabled = isMasterEnabled,
                accentColor = MaterialTheme.colorScheme.primary,
                onToggle = onToggleMaster,
                testTag = "dps_master_switch"
            )

            // DPS Profiles Selection Chips
            Text(
                text = "DPS ENHANCEMENT PROFILES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            val profiles = listOf(
                Triple(DpsProfile.MUSIC, "Music", Icons.Default.MusicNote to "Harmonic balance & vocal richness"),
                Triple(DpsProfile.MOVIE, "Movie", Icons.Default.Movie to "Cinematic immersion & sub-bass depth"),
                Triple(DpsProfile.VOICE, "Voice", Icons.Default.RecordVoiceOver to "Enhanced dialogue intelligibility"),
                Triple(DpsProfile.GAMING, "Gaming", Icons.Default.SportsEsports to "Spatial cues & footstep definition")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { (profile, name, detail) ->
                    val isSelected = currentProfile == profile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isMasterEnabled) { onProfileChange(profile) }
                            .testTag("dps_profile_${name.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = detail.first,
                                contentDescription = name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Informative Status / Mode Badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dps_active_status_badge"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMasterEnabled) "Active: ${currentProfile.displayName} Mode" else "DPS Engine Offline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMasterEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isMasterEnabled) currentProfile.statusDescription else "Turn master switch on to activate pre-tuned studio mastering",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Master Switch Card at top of sub-screens.
 */
@Composable
private fun MasterSwitchCard(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isEnabled) 1.5.dp else 1.dp,
            color = if (isEnabled) accentColor else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
