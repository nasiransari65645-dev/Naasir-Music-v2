package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.PlayerState
import com.example.model.AppThemePreset
import com.example.model.CustomThemeSettings
import com.example.model.EdgeLightingSettings
import com.example.model.EdgeLightingShape
import com.example.model.EdgeLightingStyle
import com.example.model.LocalAppThemePalette
import com.example.model.VolumeGaugePosition
import com.example.model.VolumeGaugeSettings
import com.example.model.VolumeGaugeTheme
import com.example.storage.SettingsPreferencesManager
import kotlinx.coroutines.launch

/**
 * 10 Distinct, Immersive Player UI Designs (Cosmic Orbit as 2nd UI)
 */
data class UiDesignOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
)

val ALL_UI_DESIGNS = listOf(
    UiDesignOption("Vinyl Record", "Vinyl Record", "33⅓ RPM rotating turntable vinyl disc", Icons.Default.Album, "Retro"),
    UiDesignOption("Cosmic Orbit", "Cosmic Orbit", "Nebula orbit ring, satellite gauge & cyan wave", Icons.Default.AutoAwesome, "Cosmic"),
    UiDesignOption("Cyber Neon", "Cyber Neon", "Futuristic HUD rings & pulsing neon aura", Icons.Default.AutoAwesome, "Sci-Fi"),
    UiDesignOption("Studio Console", "Studio Console", "Twin stereo VU meters & analog knob", Icons.Default.GraphicEq, "Studio"),
    UiDesignOption("CD Disk", "CD Disk", "Holographic laser compact disc", Icons.Default.Album, "90s"),
    UiDesignOption("Cassette Tape", "Cassette Tape", "Vintage Hi-Fi analog dual spool tape", Icons.Default.Radio, "Analog"),
    UiDesignOption("Walkman", "Walkman", "Retro portable stereo player with LED", Icons.Default.Headphones, "Vintage"),
    UiDesignOption("Acoustic Orb", "Acoustic Orb", "3D harmonic pulsating energy sphere", Icons.Default.SurroundSound, "3D"),
    UiDesignOption("Minimalist Edge", "Minimalist Edge", "Frosted glassmorphism edge cover", Icons.Default.Waves, "Modern"),
    UiDesignOption("Classic Cover", "Classic Cover", "High-res full album artwork cover", Icons.Default.MusicNote, "Default")
)

/**
 * Exactly 5 primary categories requested by user:
 * 1. Player
 * 2. Audio Engine
 * 3. Look & Feel
 * 4. Library & Storage
 * 5. About
 */
enum class SettingsSubCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    PLAYER(
        title = "Player",
        subtitle = "Player interface, album art animation, and playback controls",
        icon = Icons.Default.PhoneAndroid,
        accentColor = Color(0xFF00E5FF)
    ),
    AUDIO_ENGINE(
        title = "Audio Engine",
        subtitle = "Crossfade, gapless playback, volume normalization, and audio output",
        icon = Icons.Default.GraphicEq,
        accentColor = Color(0xFFD500F9)
    ),
    LOOK_AND_FEEL(
        title = "Look & Feel",
        subtitle = "Player UI designs, dynamic edge lighting borders, and audio visualizer",
        icon = Icons.Default.Palette,
        accentColor = Color(0xFF00E676)
    ),
    ABOUT(
        title = "About",
        subtitle = "Version, build details, licenses, and developer info",
        icon = Icons.Default.Info,
        accentColor = Color(0xFF38BDF8)
    )
}

/**
 * Premium Settings Screen with prominent cards, large rounded icons (36-40dp in 52-56dp containers),
 * and functional sub-screens for each of the 5 categories.
 */
@Composable
fun SettingsScreen(
    playerState: PlayerState? = null,
    totalSongsCount: Int = 0,
    simpleSongsCount: Int = 0,
    selectedTheme: AppThemePreset = AppThemePreset.COSMIC_DARK,
    appThemeMode: com.example.model.AppThemeMode = com.example.model.AppThemeMode.DARK_OLED,
    onSetAppThemeMode: (com.example.model.AppThemeMode) -> Unit = {},
    customThemeSettings: CustomThemeSettings = CustomThemeSettings(),
    edgeLightingSettings: EdgeLightingSettings = EdgeLightingSettings(),
    edgeLightingShape: EdgeLightingShape = EdgeLightingShape.SOLID_LINE,
    isVisualizerEnabled: Boolean = true,
    selectedVisualizerEffectId: Int = 1,
    onSelectTheme: (AppThemePreset) -> Unit = {},
    onUpdateCustomThemeSettings: (CustomThemeSettings) -> Unit = {},
    onShuffleTheme: () -> Unit = {},
    onResetCustomTheme: () -> Unit = {},
    onToggleCustomTheme: (Boolean) -> Unit = {},
    onSetEdgeLightingEnabled: (Boolean) -> Unit = {},
    onSetNotificationEdgeLightingEnabled: (Boolean) -> Unit = {},
    onSetEdgeLightingStyle: (EdgeLightingStyle) -> Unit = {},
    onSetEdgeLightingShape: (com.example.model.EdgeLightingShape) -> Unit = {},
    onSetEdgeLightingSpeed: (Float) -> Unit = {},
    onSetEdgeLightingStrokeWidth: (Float) -> Unit = {},
    onSetEdgeLightingCornerRadius: (Float) -> Unit = {},
    onSetEdgeLightingMusicReactive: (Boolean) -> Unit = {},
    onToggleVisualizer: () -> Unit = {},
    onSelectVisualizerEffect: (Int) -> Unit = {},
    onNavigateToVisualizer: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToSpatial: () -> Unit = {},
    onToggleAutoRotate: (Boolean) -> Unit = {},
    onSetAutoRotateSpeed: (Float) -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit = {},
    onSelectEqualizerPreset: (String) -> Unit = {},
    onSetBassPunch: (Float) -> Unit = {},
    onSetTrebleSparkle: (Float) -> Unit = {},
    onRescanLibrary: () -> Unit = {},
    onResetAudioSettings: () -> Unit = {},
    onClearAllFavorites: () -> Unit = {},
    customVisualizerText: String = "",
    onSaveCustomVisualizerText: (String) -> Unit = {},
    onResetCustomVisualizerText: () -> Unit = {},
    showVisualizerText: Boolean = true,
    onToggleShowVisualizerText: (Boolean) -> Unit = {},
    visualizerTextColorHex: Long = 0xFF00E5FF,
    onSelectVisualizerTextColor: (Long) -> Unit = {},
    gaplessPlayback: Boolean = true,
    onToggleGaplessPlayback: (Boolean) -> Unit = {},
    crossfadeSec: Int = 0,
    onSetCrossfadeSec: (Int) -> Unit = {},
    autoplayHeadset: Boolean = false,
    onToggleAutoplayHeadset: (Boolean) -> Unit = {},
    notificationControls: Boolean = true,
    onToggleNotificationControls: (Boolean) -> Unit = {},
    useSystemMediaNotification: Boolean = false,
    onToggleUseSystemMediaNotification: (Boolean) -> Unit = {},
    keepScreenOn: Boolean = false,
    onToggleKeepScreenOn: (Boolean) -> Unit = {},
    shakeToSkip: Boolean = false,
    onToggleShakeToSkip: (Boolean) -> Unit = {},
    sleepTimerMinutes: Int = 0,
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSetPlaybackSpeed: (Float) -> Unit = {},
    playbackPitch: Float = 1.0f,
    onSetPlaybackPitch: (Float) -> Unit = {},
    albumArtStyle: String = "Vinyl Record",
    onSetAlbumArtStyle: (String) -> Unit = {},
    spinningVinyl: Boolean = true,
    onSetSpinningVinyl: (Boolean) -> Unit = {},
    volumeGaugeSettings: VolumeGaugeSettings = VolumeGaugeSettings(),
    onUpdateVolumeGaugeSettings: (VolumeGaugeSettings) -> Unit = {},
    onSetAppVolume: (Float) -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { SettingsPreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentCategory by remember { mutableStateOf<SettingsSubCategory?>(null) }

    // Intercept back navigation when inside a sub-screen
    BackHandler(enabled = currentCategory != null) {
        currentCategory = null
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = currentCategory,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "SettingsSubScreenTransition"
        ) { category ->
            if (category == null) {
                MainPreferencesList(
                    albumArtStyle = albumArtStyle,
                    onSetAlbumArtStyle = onSetAlbumArtStyle,
                    prefsManager = prefsManager,
                    onSelectCategory = { currentCategory = it }
                )
            } else {
                SubScreenContainer(
                    category = category,
                    onBack = { currentCategory = null }
                ) {
                    when (category) {
                        SettingsSubCategory.PLAYER -> PlayerSubScreen(
                            prefsManager = prefsManager,
                            keepScreenOn = keepScreenOn,
                            onToggleKeepScreenOn = onToggleKeepScreenOn,
                            notificationControls = notificationControls,
                            onToggleNotificationControls = onToggleNotificationControls,
                            useSystemMediaNotification = useSystemMediaNotification,
                            onToggleUseSystemMediaNotification = onToggleUseSystemMediaNotification,
                            shakeToSkip = shakeToSkip,
                            onToggleShakeToSkip = onToggleShakeToSkip,
                            albumArtStyle = albumArtStyle,
                            onSetAlbumArtStyle = onSetAlbumArtStyle,
                            spinningVinyl = spinningVinyl,
                            onSetSpinningVinyl = onSetSpinningVinyl
                        )
                        SettingsSubCategory.AUDIO_ENGINE -> AudioEngineSubScreen(
                            prefsManager = prefsManager,
                            gaplessPlayback = gaplessPlayback,
                            onToggleGaplessPlayback = onToggleGaplessPlayback,
                            crossfadeSec = crossfadeSec,
                            onSetCrossfadeSec = onSetCrossfadeSec,
                            autoplayHeadset = autoplayHeadset,
                            onToggleAutoplayHeadset = onToggleAutoplayHeadset,
                            onNavigateToEqualizer = onNavigateToEqualizer,
                            onNavigateToSpatial = onNavigateToSpatial,
                            playbackSpeed = playbackSpeed,
                            onSetPlaybackSpeed = onSetPlaybackSpeed,
                            playbackPitch = playbackPitch,
                            onSetPlaybackPitch = onSetPlaybackPitch
                        )
                        SettingsSubCategory.LOOK_AND_FEEL -> LookAndFeelSubScreen(
                            albumArtStyle = albumArtStyle,
                            onSetAlbumArtStyle = onSetAlbumArtStyle,
                            prefsManager = prefsManager,
                            selectedTheme = selectedTheme,
                            appThemeMode = appThemeMode,
                            onSetAppThemeMode = onSetAppThemeMode,
                            onSelectTheme = onSelectTheme,
                            customThemeSettings = customThemeSettings,
                            onShuffleTheme = onShuffleTheme,
                            onResetCustomTheme = onResetCustomTheme,
                            onToggleCustomTheme = onToggleCustomTheme,
                            edgeLightingSettings = edgeLightingSettings,
                            edgeLightingShape = edgeLightingShape,
                            onSetEdgeLightingEnabled = onSetEdgeLightingEnabled,
                            onSetNotificationEdgeLightingEnabled = onSetNotificationEdgeLightingEnabled,
                            onSetEdgeLightingStyle = onSetEdgeLightingStyle,
                            onSetEdgeLightingShape = onSetEdgeLightingShape,
                            onSetEdgeLightingSpeed = onSetEdgeLightingSpeed,
                            onSetEdgeLightingStrokeWidth = onSetEdgeLightingStrokeWidth,
                            onSetEdgeLightingCornerRadius = onSetEdgeLightingCornerRadius,
                            onSetEdgeLightingMusicReactive = onSetEdgeLightingMusicReactive,
                            isVisualizerEnabled = isVisualizerEnabled,
                            onToggleVisualizer = onToggleVisualizer,
                            selectedVisualizerEffectId = selectedVisualizerEffectId,
                            onSelectVisualizerEffect = onSelectVisualizerEffect,
                            customVisualizerText = customVisualizerText,
                            onSaveCustomVisualizerText = onSaveCustomVisualizerText,
                            onResetCustomVisualizerText = onResetCustomVisualizerText,
                            showVisualizerText = showVisualizerText,
                            onToggleShowVisualizerText = onToggleShowVisualizerText,
                            visualizerTextColorHex = visualizerTextColorHex,
                            onSelectVisualizerTextColor = onSelectVisualizerTextColor,
                            volumeGaugeSettings = volumeGaugeSettings,
                            onUpdateVolumeGaugeSettings = onUpdateVolumeGaugeSettings
                        )
                        SettingsSubCategory.ABOUT -> AboutSubScreen(
                            onResetAllSettings = {
                                onResetAllSettings()
                                coroutineScope.launch { snackbarHostState.showSnackbar("All settings reset to defaults.") }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Main Settings Screen with high-fidelity rounded cards and prominent icons.
 */
@Composable
private fun MainPreferencesList(
    albumArtStyle: String = "Vinyl Record",
    onSetAlbumArtStyle: (String) -> Unit = {},
    prefsManager: SettingsPreferencesManager,
    onSelectCategory: (SettingsSubCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header section banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize player, audio engine, look & feel, and about",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        items(SettingsSubCategory.values()) { category ->
            SettingsCategoryCard(
                category = category,
                onClick = { onSelectCategory(category) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/**
 * Large, prominent icon card matching dynamic theme colors.
 * Elevated container with icon on the left and dynamic text contrast.
 */
@Composable
private fun SettingsCategoryCard(
    category: SettingsSubCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings_card_${category.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Large Icon Container (52dp box with 32dp icon, 12dp rounded corners)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.accentColor.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        category.accentColor.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = category.accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and Subtitle Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Forward Chevron Indicator
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${category.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Top App Bar & Scrollable Container for sub-screens.
 */
@Composable
private fun SubScreenContainer(
    category: SettingsSubCategory,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_subscreen_${category.name.lowercase()}")
    ) {
        // Sub-screen Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp).testTag("settings_subscreen_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                Text(
                    text = "Preferences & Controls",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }

        // Sub-screen Content
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// SUB-SCREEN 1: PLAYER
// -----------------------------------------------------------------------------
@Composable
private fun PlayerSubScreen(
    prefsManager: SettingsPreferencesManager,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    notificationControls: Boolean,
    onToggleNotificationControls: (Boolean) -> Unit,
    useSystemMediaNotification: Boolean = false,
    onToggleUseSystemMediaNotification: (Boolean) -> Unit = {},
    shakeToSkip: Boolean,
    onToggleShakeToSkip: (Boolean) -> Unit,
    albumArtStyle: String = "Vinyl Record",
    onSetAlbumArtStyle: (String) -> Unit = {},
    spinningVinyl: Boolean = true,
    onSetSpinningVinyl: (Boolean) -> Unit = {},
    volumeGaugeSettings: VolumeGaugeSettings = VolumeGaugeSettings(),
    onUpdateVolumeGaugeSettings: (VolumeGaugeSettings) -> Unit = {}
) {
    val context = LocalContext.current
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var shakeSensitivity by remember { mutableStateOf(prefsManager.loadShakeSensitivity()) }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = {
                Text(
                    text = "Overlay Permission Required",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Allow Naasir Music to display over other apps so the custom volume HUD stays visible while music is playing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(fallback)
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Permission", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showOverlayPermissionDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Toggle: Always Keep Screen On
    SettingToggleCard(
        title = "Always Keep Screen On",
        description = "Prevents display from dimming or locking while audio is actively playing",
        checked = keepScreenOn,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = {
            onToggleKeepScreenOn(it)
            prefsManager.saveKeepScreenOn(it)
        }
    )

    // Automatically ensure media notifications, system controls, spinning vinyl, and 10s seek interval remain permanently in background
    LaunchedEffect(Unit) {
        prefsManager.saveSeekIntervalSec(10)
        if (!spinningVinyl) {
            onSetSpinningVinyl(true)
            prefsManager.saveSpinningVinyl(true)
        }
        if (!notificationControls) {
            onToggleNotificationControls(true)
            prefsManager.saveNotificationControls(true)
        }
        if (!useSystemMediaNotification) {
            onToggleUseSystemMediaNotification(true)
            prefsManager.saveUseSystemMediaNotification(true)
        }
    }

    // Toggle: Shake Device to Skip
    SettingToggleCard(
        title = "Shake Device to Skip Track",
        description = "Use accelerometer to skip to the next track when phone is shaken",
        checked = shakeToSkip,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = {
            onToggleShakeToSkip(it)
            prefsManager.saveShakeToSkip(it)
        }
    )
}

// -----------------------------------------------------------------------------
// SUB-SCREEN 2: AUDIO ENGINE
// -----------------------------------------------------------------------------
@Composable
private fun AudioEngineSubScreen(
    prefsManager: SettingsPreferencesManager,
    gaplessPlayback: Boolean,
    onToggleGaplessPlayback: (Boolean) -> Unit,
    crossfadeSec: Int,
    onSetCrossfadeSec: (Int) -> Unit,
    autoplayHeadset: Boolean,
    onToggleAutoplayHeadset: (Boolean) -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToSpatial: () -> Unit,
    playbackSpeed: Float = 1.0f,
    onSetPlaybackSpeed: (Float) -> Unit = {},
    playbackPitch: Float = 1.0f,
    onSetPlaybackPitch: (Float) -> Unit = {}
) {
    var hiResOutput by remember { mutableStateOf(true) }
    var volumeNormalization by remember { mutableStateOf(false) }

    // Enforce 5-second crossfade and auto-pause on disconnect permanently in background
    LaunchedEffect(Unit) {
        onSetCrossfadeSec(5)
        prefsManager.saveCrossfadeSec(5)
        prefsManager.saveAutoPauseHeadset(true)
    }

    // Toggle: Gapless Playback
    SettingToggleCard(
        title = "Gapless Playback",
        description = "Eliminates silence between consecutive tracks for continuous live album listening",
        checked = gaplessPlayback,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = {
            onToggleGaplessPlayback(it)
            prefsManager.saveGaplessPlayback(it)
        }
    )

    // Toggle: Auto-Resume on Reconnect
    SettingToggleCard(
        title = "Auto-Resume on Reconnect",
        description = "Automatically resume playback when headphones are plugged back in",
        checked = autoplayHeadset,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = {
            onToggleAutoplayHeadset(it)
            prefsManager.saveAutoplayHeadset(it)
        }
    )

    // Toggle: 32-Bit Float Audio Output
    SettingToggleCard(
        title = "32-Bit Floating Point Audio Output",
        description = "Route uncompressed audio buffers through low-jitter hardware PCM pipeline",
        checked = hiResOutput,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = { hiResOutput = it }
    )

    // Toggle: ReplayGain Volume Normalization
    SettingToggleCard(
        title = "Volume Normalization (ReplayGain)",
        description = "Balance perceived loudness across quiet acoustic and loud mastered tracks",
        checked = volumeNormalization,
        accentColor = MaterialTheme.colorScheme.primary,
        onCheckedChange = { volumeNormalization = it }
    )

    // Playback Speed Adjustment Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tempo control without pitch distortion",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                speedPresets.forEach { speed ->
                    val isSelected = kotlin.math.abs(playbackSpeed - speed) < 0.05f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetPlaybackSpeed(speed) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Playback Pitch Adjustment Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Audio Pitch Key Shift",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Frequency transposition (0.8x deep to 1.2x bright)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val pitchPresets = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pitchPresets.forEach { pitch ->
                    val isSelected = kotlin.math.abs(playbackPitch - pitch) < 0.05f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSetPlaybackPitch(pitch) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pitch}x",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Shortcuts: Equalizer & 3D Spatial
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Audio Processing Suites",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Equalizer", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = onNavigateToSpatial,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.SurroundSound, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("3D Effects", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private enum class LookHierarchy {
    PLAYER_UI_DESIGNS,
    DIGITAL_VOLUME_SLIDER,
    DYNAMIC_EDGE_LIGHTING,
    AUDIO_VISUALIZER
}

// -----------------------------------------------------------------------------
// SUB-SCREEN 3: LOOK & FEEL
// -----------------------------------------------------------------------------
@Composable
private fun LookAndFeelSubScreen(
    selectedTheme: AppThemePreset,
    appThemeMode: com.example.model.AppThemeMode = com.example.model.AppThemeMode.DARK_OLED,
    onSetAppThemeMode: (com.example.model.AppThemeMode) -> Unit = {},
    onSelectTheme: (AppThemePreset) -> Unit,
    customThemeSettings: CustomThemeSettings,
    onShuffleTheme: () -> Unit,
    onResetCustomTheme: () -> Unit,
    onToggleCustomTheme: (Boolean) -> Unit,
    edgeLightingSettings: EdgeLightingSettings,
    edgeLightingShape: com.example.model.EdgeLightingShape,
    onSetEdgeLightingEnabled: (Boolean) -> Unit,
    onSetNotificationEdgeLightingEnabled: (Boolean) -> Unit = {},
    onSetEdgeLightingStyle: (EdgeLightingStyle) -> Unit,
    onSetEdgeLightingShape: (com.example.model.EdgeLightingShape) -> Unit,
    onSetEdgeLightingSpeed: (Float) -> Unit,
    onSetEdgeLightingStrokeWidth: (Float) -> Unit,
    onSetEdgeLightingCornerRadius: (Float) -> Unit,
    onSetEdgeLightingMusicReactive: (Boolean) -> Unit,
    isVisualizerEnabled: Boolean,
    onToggleVisualizer: () -> Unit,
    selectedVisualizerEffectId: Int,
    onSelectVisualizerEffect: (Int) -> Unit,
    customVisualizerText: String,
    onSaveCustomVisualizerText: (String) -> Unit,
    onResetCustomVisualizerText: () -> Unit,
    showVisualizerText: Boolean,
    onToggleShowVisualizerText: (Boolean) -> Unit,
    visualizerTextColorHex: Long,
    onSelectVisualizerTextColor: (Long) -> Unit,
    albumArtStyle: String = "Vinyl Record",
    onSetAlbumArtStyle: (String) -> Unit = {},
    prefsManager: SettingsPreferencesManager? = null,
    volumeGaugeSettings: VolumeGaugeSettings = VolumeGaugeSettings(),
    onUpdateVolumeGaugeSettings: (VolumeGaugeSettings) -> Unit = {}
) {
    val context = LocalContext.current
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var activeLookHierarchy by remember { mutableStateOf<LookHierarchy?>(null) }

    BackHandler(enabled = activeLookHierarchy != null) {
        activeLookHierarchy = null
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = {
                Text(
                    text = "Overlay Permission Required",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Allow Naasir Music to display over other apps so the custom volume HUD stays visible while music is playing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(fallback)
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Permission", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showOverlayPermissionDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    when (activeLookHierarchy) {
        null -> {
            // Hierarchy Item 1: Player Ui Designs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeLookHierarchy = LookHierarchy.PLAYER_UI_DESIGNS }
                    .testTag("lookandfeel_player_ui_designs_tile"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Player Ui Designs",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Player Ui Designs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Active: $albumArtStyle • 10 Designs available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Player Ui Designs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hierarchy Item 2: Digital volume slider
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeLookHierarchy = LookHierarchy.DIGITAL_VOLUME_SLIDER }
                    .testTag("lookandfeel_digital_volume_slider_tile"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Digital volume slider",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Digital volume slider",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Floating LED capsule HUD, step size, themes & haptics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Digital volume slider",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hierarchy Item 3: Dynamic Edge Lighting
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeLookHierarchy = LookHierarchy.DYNAMIC_EDGE_LIGHTING }
                    .testTag("lookandfeel_dynamic_edge_lighting_tile"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Dynamic Edge Lighting",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Edge Lighting",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (edgeLightingSettings.isEnabled) "Enabled • ${edgeLightingSettings.style.title}" else "Disabled • Screen edge glow effects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Dynamic Edge Lighting",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hierarchy Item 4: Audio Visualizer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeLookHierarchy = LookHierarchy.AUDIO_VISUALIZER }
                    .testTag("lookandfeel_audio_visualizer_tile"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE040FB).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFE040FB).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio Visualizer",
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Visualizer",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isVisualizerEnabled) "Enabled • Real-time frequency waveform" else "Disabled • Real-time frequency waveform",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Audio Visualizer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        LookHierarchy.PLAYER_UI_DESIGNS -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeLookHierarchy = null },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Look & Feel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Player Ui Designs",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Active: $albumArtStyle • 10 Designs available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val chunkedDesigns = ALL_UI_DESIGNS.chunked(3)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunkedDesigns.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { design ->
                            val isSelected = albumArtStyle == design.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSetAlbumArtStyle(design.id)
                                        prefsManager?.saveAlbumArtStyle(design.id)
                                    }
                                    .testTag("player_ui_design_${design.id.lowercase().replace(" ", "_")}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = design.icon,
                                        contentDescription = design.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = design.title,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        val remaining = 3 - rowItems.size
                        repeat(remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        LookHierarchy.DIGITAL_VOLUME_SLIDER -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeLookHierarchy = null },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Look & Feel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Digital volume slider",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Segmented LED capsule HUD with touch drag & precision haptics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_volume_gauge_settings"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Floating Overlay Toggle (System Alert Window)
                    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else {
                        true
                    }
                    val isOverlayActive = volumeGaugeSettings.floatingOverlayEnabled && hasOverlayPermission

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "System Floating Speedometer HUD",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Display floating borderless speedometer over other apps ONLY while music is playing. Automatically reverts to native volume when paused.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }

                        Switch(
                            checked = isOverlayActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (!hasOverlayPermission) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(floatingOverlayEnabled = true))
                                    }
                                } else {
                                    onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(floatingOverlayEnabled = false))
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Setting 1: Step Size
                    Text(
                        text = "Volume Step Size (Hardware Keys)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Increment/decrement step per single hardware volume key tap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val steps = listOf(2, 5, 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEach { step ->
                            val isSelected = volumeGaugeSettings.stepSizePercent == step
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(stepSizePercent = step))
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$step%",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Setting 2: Gauge Screen Position
                    Text(
                        text = "Gauge Screen Position",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Screen edge where digital capsule HUD appears",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VolumeGaugePosition.values().forEach { pos ->
                            val isSelected = volumeGaugeSettings.position == pos
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(position = pos))
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = pos.displayName,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Setting 3: Theme Color Scheme
                    Text(
                        text = "Theme Color Scheme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Illuminated neon gradient for arc needle and focal display",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val themes = VolumeGaugeTheme.values()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themes.take(2).forEach { theme ->
                                val isSelected = volumeGaugeSettings.theme == theme
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(theme = theme))
                                        },
                                    color = if (isSelected) theme.primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(theme.primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = theme.displayName,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themes.drop(2).forEach { theme ->
                                val isSelected = volumeGaugeSettings.theme == theme
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(theme = theme))
                                        },
                                    color = if (isSelected) theme.primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(theme.primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = theme.displayName,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Setting 4: Haptic Feedback Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Tactile Haptic Feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Vibrate tactile tick on volume steps and dragging",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = volumeGaugeSettings.hapticEnabled,
                            onCheckedChange = {
                                onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(hapticEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Setting 5: Auto-Dismiss Delay
                    Text(
                        text = "Auto-Dismiss Delay",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Duration the HUD stays visible after key press or drag release",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val delays = listOf(1.5f, 2.0f, 3.0f, 5.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        delays.forEach { delay ->
                            val isSelected = volumeGaugeSettings.autoDismissDelaySec == delay
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateVolumeGaugeSettings(volumeGaugeSettings.copy(autoDismissDelaySec = delay))
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${delay}s",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        LookHierarchy.DYNAMIC_EDGE_LIGHTING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeLookHierarchy = null },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Look & Feel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Dynamic Edge Lighting",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Luminous border effect wrapping around device screen edges",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_dynamic_edge_lighting_hierarchy"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Main Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Dynamic Edge Lighting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable glowing luminous edge lighting borders around screen during playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = edgeLightingSettings.isEnabled,
                            onCheckedChange = onSetEdgeLightingEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    // Unfolds when Main Switch is ON
                    AnimatedVisibility(
                        visible = edgeLightingSettings.isEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            // Sub-option: Media Notification Edge Lighting
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Notification Player Border",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Luminous theme-colored glowing border on lock screen & notification",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                }
                                Switch(
                                    checked = edgeLightingSettings.isNotificationBorderEnabled,
                                    onCheckedChange = onSetNotificationEdgeLightingEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Lighting Animation Style",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val styles = EdgeLightingStyle.values()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                styles.forEach { style ->
                                    val isSelected = edgeLightingSettings.style == style
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSetEdgeLightingStyle(style) },
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = style.title,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.5.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Border Size (Stroke Width)
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
                                    text = "${String.format(java.util.Locale.US, "%.1f", edgeLightingSettings.strokeWidthDp)} dp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = edgeLightingSettings.strokeWidthDp,
                                onValueChange = onSetEdgeLightingStrokeWidth,
                                valueRange = 2f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("slider_edge_border_size")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Animation Speed
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
                                    text = "${String.format(java.util.Locale.US, "%.1f", edgeLightingSettings.animationSpeedSec)}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = edgeLightingSettings.animationSpeedSec,
                                onValueChange = onSetEdgeLightingSpeed,
                                valueRange = 1f..6f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("slider_edge_anim_speed")
                            )
                        }
                    }
                }
            }
        }

        LookHierarchy.AUDIO_VISUALIZER -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeLookHierarchy = null },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Look & Feel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Audio Visualizer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Real-time frequency waveform rendered behind controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_audio_visualizer_hierarchy"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Main Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Audio Visualizer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable live dynamic frequency spectrum waveform in player",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = isVisualizerEnabled,
                            onCheckedChange = { onToggleVisualizer() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    // Unfolds when Main Switch is ON
                    AnimatedVisibility(
                        visible = isVisualizerEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Visualizer Style",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val visualizerEffects = listOf(
                                1 to "Bars",
                                2 to "Wave",
                                3 to "Neon Orbit",
                                4 to "Spectrum"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                visualizerEffects.forEach { (id, name) ->
                                    val isSelected = selectedVisualizerEffectId == id
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectVisualizerEffect(id) },
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Branding Text Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Display Custom Brand Name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Overlay signature text in the visualizer center",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                }
                                Switch(
                                    checked = showVisualizerText,
                                    onCheckedChange = onToggleShowVisualizerText,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            if (showVisualizerText) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = customVisualizerText,
                                    onValueChange = onSaveCustomVisualizerText,
                                    label = { Text("Brand / Signature Text") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Text Color Accent",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val colorOptions = listOf(
                                    0xFF00E5FF to "Cyan",
                                    0xFFFF4081 to "Pink",
                                    0xFF00E676 to "Green",
                                    0xFFFFD600 to "Gold",
                                    0xFFD500F9 to "Purple"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colorOptions.forEach { (colorHex, colorName) ->
                                        val isSelected = visualizerTextColorHex == colorHex
                                        val color = Color(colorHex)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onSelectVisualizerTextColor(colorHex) },
                                            color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = colorName,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SUB-SCREEN 4: LIBRARY & STORAGE
// -----------------------------------------------------------------------------
@Composable
private fun LibraryStorageSubScreen(
    totalSongsCount: Int,
    onRescanLibrary: () -> Unit,
    onClearAllFavorites: () -> Unit,
    onResetAudioSettings: () -> Unit
) {
    // Library Statistics Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "$totalSongsCount Songs Indexed",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Internal device storage & SD card media store",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Action 1: Rescan Device Audio
    Button(
        onClick = onRescanLibrary,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Rescan Device Audio Library", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }

    // Action 2: Clear All Favorites
    Button(
        onClick = onClearAllFavorites,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFF43F5E))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Clear All Favorites", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }

    // Action 3: Reset Audio Effects
    Button(
        onClick = onResetAudioSettings,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reset Audio Effects to Default", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// -----------------------------------------------------------------------------
// SUB-SCREEN 5: ABOUT
// -----------------------------------------------------------------------------
@Composable
private fun AboutSubScreen(
    onResetAllSettings: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    // App Identity Hero Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "3D Music Player Pro",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Version 2.5.0 Pro Studio Edition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "• Architecture: Kotlin 2.0 • Jetpack Compose • Material 3",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Audio Engine: OpenSL ES • AndroidX Media3 • 3D Spatial Virtualizer",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Digital Power Station (DPS): Multi-Band Remastering Matrix",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Reset All Settings Button
    Button(
        onClick = { showResetDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626).copy(alpha = 0.15f),
            contentColor = Color(0xFFEF4444)
        ),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
    ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reset All Preferences & Cache", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will restore default audio, appearance, and player settings. Your audio files will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetAllSettings()
                }) {
                    Text("Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Reusable row toggle card with consistent styling.
 */
@Composable
private fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (checked) accentColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
