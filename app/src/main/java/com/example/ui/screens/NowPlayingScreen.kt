package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex
import com.example.model.AppThemePalette
import com.example.model.CustomThemeSettings
import com.example.model.LocalAppThemePalette
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.ui.components.AutoScrollText
import com.example.ui.components.HoldableIconButton
import com.example.ui.components.TouchableProgressBar

@Composable
fun NowPlayingScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    autoRotateActive: Boolean,
    isFavorite: Boolean = false,
    allSongs: List<Song> = emptyList(),
    customThemeSettings: CustomThemeSettings = CustomThemeSettings(),
    themePalette: AppThemePalette = LocalAppThemePalette.current,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkipBackward10: () -> Unit = {},
    onSkipForward10: () -> Unit = {},
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit = {},
    onSongSelect: (Song) -> Unit = {},
    onEnsureTrackLoaded: () -> Unit = {},
    isFastForwarding: Boolean = false,
    isRewinding: Boolean = false,
    albumArtStyle: String = "Vinyl Record",
    spinningVinyl: Boolean = true,
    onStartFastForward: () -> Unit = {},
    onStopFastForward: () -> Unit = {},
    onStartRewind: () -> Unit = {},
    onStopRewind: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // If no song selected in Now Playing, prepare 1st track in paused state
    LaunchedEffect(currentSong, allSongs) {
        if (currentSong == null && allSongs.isNotEmpty()) {
            onEnsureTrackLoaded()
        }
    }

    val activePalette = themePalette

    // 6-Color harmonic brush & shade distribution:
    // Base 1 (c1Primary): Play button & master neon
    // Base 2 (c2Secondary): Seekbar progress start & wave peaks
    // Base 3 (c3Tertiary): Seekbar progress end & control icons
    // Base 4 (c4Accent): Thumb glow & active toggles
    // Base 5 (c5VinylRing): Vinyl disc outer rim & radar
    // Base 6 (c6SurfaceGlow): Ambient backdrop, card borders, tab icons

    val seekbarFillGradient = if (customThemeSettings.isEnabled) {
        listOf(customThemeSettings.colorMap.seekBarColor, customThemeSettings.colorMap.seekBarColor.copy(alpha = 0.8f))
    } else {
        listOf(activePalette.c2Secondary, activePalette.c3Tertiary, activePalette.c4Accent)
    }
    val seekbarActiveColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.seekBarColor else activePalette.c2Secondary
    val seekbarInactiveColor = if (customThemeSettings.isEnabled) Color(0xFF1E293B) else activePalette.c6SurfaceGlow.copy(alpha = 0.35f)
    val seekbarThumbColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.seekBarColor else activePalette.c4Accent
    val seekbarThumbGlowColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.seekBarColor.copy(alpha = 0.5f) else activePalette.c4Accent.copy(alpha = 0.55f)

    val activePlayColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.playPauseButtonColor else activePalette.c1Primary
    val activePlaySecondaryColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.playPauseButtonColor.copy(alpha = 0.85f) else activePalette.c2Secondary
    val activePlayGlowColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.playPauseButtonColor.copy(alpha = 0.5f) else activePalette.c4Accent.copy(alpha = 0.6f)
    val activePlayBrush = if (customThemeSettings.isEnabled) {
        Brush.linearGradient(listOf(customThemeSettings.colorMap.playPauseButtonColor, customThemeSettings.colorMap.playPauseButtonColor.copy(alpha = 0.85f)))
    } else {
        activePalette.playButtonBrush
    }

    val activeSkipColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.skipRewindButtonColor else activePalette.c3Tertiary
    val activeSkipGlowColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.skipRewindButtonColor.copy(alpha = 0.4f) else activePalette.c4Accent.copy(alpha = 0.35f)

    val active10sSkipColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.skipRewindButtonColor else activePalette.c4Accent

    val activeVinylCenterRing = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.seekBarColor else activePalette.c5VinylRing
    val activeVinylOuterSheen = if (customThemeSettings.isEnabled) Color(0xFF1E2230) else activePalette.c6SurfaceGlow
    val activeShuffleRepeatActiveColor = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.bottomNavActiveColor else activePalette.c4Accent
    val activeVisualizerPeak = if (customThemeSettings.isEnabled) customThemeSettings.colorMap.seekBarColor else activePalette.c1Primary
    val activeProgressColor = if (customThemeSettings.isEnabled) customThemeSettings.progressColor.primaryColor else activePalette.c1Primary
    val progressBarColor = if (customThemeSettings.isEnabled) customThemeSettings.progressColor.primaryColor else activePalette.c1Primary

    if (currentSong == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = progressBarColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading audio track...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Auto-loading from library 1st song",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
        return
    }

    val safeDuration = durationMs.coerceAtLeast(1L)

    // Blinking/flashing animation for current playback timestamp on pause (1.0 to 0.2 alpha oscillation)
    val blinkAlpha by if (!isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "pause_timestamp_blink")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = AnimRepeatMode.Reverse
            ),
            label = "blink_alpha"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    }

    val activeAccentColor = seekbarActiveColor

    val isCosmicOrbit = albumArtStyle == "Cosmic Orbit"

    val backdropModifier = if (!customThemeSettings.isEnabled) {
        modifier
            .fillMaxSize()
            .background(activePalette.ambientBackdropBrush)
    } else {
        modifier.fillMaxSize()
    }

    val currentIndex = remember(currentSong, allSongs) {
        if (currentSong == null || allSongs.isEmpty()) 0
        else {
            val idx = allSongs.indexOfFirst { it.id == currentSong.id }
            if (idx >= 0) idx else 0
        }
    }
    val totalTracks = remember(allSongs) {
        allSongs.size.coerceAtLeast(1)
    }

    // Clean single Column root container
    Column(
        modifier = backdropModifier
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

            // 1. ALBUM ART / VINYL DISC (Responsive Auto-Fit)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("now_playing_album_art_card"),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtPresentation(
                    style = albumArtStyle,
                    isPlaying = isPlaying,
                    spinningVinyl = spinningVinyl,
                    autoRotateActive = autoRotateActive,
                    isFastForwarding = isFastForwarding,
                    isRewinding = isRewinding,
                    activeAccentColor = activeVinylCenterRing,
                    activeSkipColor = activeSkipColor,
                    outerBorderColor = activeVinylOuterSheen,
                    visualizerPeakColor = activeVisualizerPeak,
                    songTitle = currentSong.title,
                    songArtist = currentSong.artist,
                    albumArtUri = currentSong.albumArtUri,
                    currentPositionMs = currentPositionMs,
                    durationMs = safeDuration,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. TRACK INFORMATION
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoScrollText(
                        text = currentSong.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentSong.artist} • ${currentSong.album}",
                        fontSize = 14.sp,
                        color = Color(0xFF7E8B9B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Favorite Heart Button
                IconButton(
                    onClick = { onToggleFavorite(currentSong.id) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite) Color(0xFFEF4444).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .testTag("now_playing_heart_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 3. SEEKBAR & INTEGRATED COUNTER ROW
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TouchableProgressBar(
                    currentPositionMs = currentPositionMs,
                    durationMs = safeDuration,
                    onSeekTo = onSeekTo,
                    barHeight = 6.dp,
                    touchTargetHeight = 36.dp,
                    activeGradient = listOf(Color(0xFF00E5FF), Color(0xFF00F5FF)),
                    inactiveColor = seekbarInactiveColor,
                    thumbColor = Color(0xFF00F5FF),
                    thumbGlowColor = Color(0xFF00F5FF).copy(alpha = 0.6f),
                    showThumb = true,
                    tag = "track_progress_slider"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMs(currentPositionMs),
                        fontSize = 12.sp,
                        color = Color(0xFF7E8B9B),
                        modifier = Modifier
                            .graphicsLayer { alpha = blinkAlpha }
                            .testTag("current_playback_timestamp")
                    )

                    Text(
                        text = "${currentIndex + 1} / ${totalTracks}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("standard_track_counter_badge")
                    )

                    Text(
                        text = formatMs(safeDuration),
                        fontSize = 12.sp,
                        color = Color(0xFF7E8B9B),
                        modifier = Modifier.testTag("total_duration_timestamp")
                    )
                }
            }

            // 4. SECONDARY TOGGLES ROW (SHUFFLE & REPEAT)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) Color(0xFF00F5FF) else Color(0xFF7E8B9B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("repeat_button")
                    ) {
                        val (icon, tint) = when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat to Color(0xFF7E8B9B)
                            RepeatMode.ALL -> Icons.Default.Repeat to Color(0xFF00F5FF)
                            RepeatMode.ONE -> Icons.Default.RepeatOne to Color(0xFF00F5FF)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat: ${repeatMode.name}",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Gesture UI Feedback Badge (shown while fast-forwarding or rewinding)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFastForwarding || isRewinding,
                    enter = fadeIn() + scaleIn(initialScale = 0.88f),
                    exit = fadeOut() + scaleOut(targetScale = 0.88f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isFastForwarding) Color(0xFF06283D).copy(alpha = 0.95f) else Color(0xFF28103A).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, if (isFastForwarding) activeAccentColor else activeSkipColor),
                        modifier = Modifier.testTag("playback_gesture_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFastForwarding) Icons.Default.FastForward else Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = if (isFastForwarding) activeAccentColor else activeSkipColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFastForwarding) ">> 2x Speed" else "<< Rewind",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 5. MAIN PLAYBACK CONTROLS ROW (COMFORTABLE TOUCH TARGETS, NO OVERFLOW)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Rewind 10s Button
                IconButton(
                    onClick = onSkipBackward10,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("skip_backward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 Seconds",
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 2. Previous Track Button
                HoldableIconButton(
                    onClick = onPrevious,
                    onHoldStart = onStartRewind,
                    onHoldEnd = onStopRewind,
                    isHolding = isRewinding,
                    size = 48.dp,
                    activeGlowColor = activeSkipGlowColor,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 3. Central Big Play/Pause Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(8.dp, CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF080B14),
                        modifier = Modifier.size(38.dp)
                    )
                }

                // 4. Next Track Button
                HoldableIconButton(
                    onClick = onNext,
                    onHoldStart = onStartFastForward,
                    onHoldEnd = onStopFastForward,
                    isHolding = isFastForwarding,
                    size = 48.dp,
                    activeGlowColor = activeSkipGlowColor,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 5. Forward 10s Button
                IconButton(
                    onClick = onSkipForward10,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("skip_forward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 Seconds",
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Dynamic Interactive Album Art Presentation supporting 6 distinct styles:
 * 1. Vinyl Record (Grooves, center label, 33⅓ RPM rotation when playing)
 * 2. Classic Cover (Modern rounded artwork card with neon border & glowing badge)
 * 3. CD Disk (Iridescent holographic sheen, mirror data ring, rotation when playing)
 * 4. Cassette Tape (Retro audio cassette shell with spinning dual cogwheel spools)
 * 5. Walkman (Vintage portable player chassis with transparent door & spinning tape)
 * 6. Full Screen (Pulsating audio energy visualizer canvas)
 *
 * All animations run ONLY when isPlaying == true and freeze in place when paused.
 */
@Composable
private fun AlbumArtPresentation(
    style: String,
    isPlaying: Boolean,
    spinningVinyl: Boolean,
    autoRotateActive: Boolean,
    isFastForwarding: Boolean,
    isRewinding: Boolean,
    activeAccentColor: Color,
    activeSkipColor: Color = Color.Unspecified,
    outerBorderColor: Color = Color.Unspecified,
    visualizerPeakColor: Color = Color.Unspecified,
    songTitle: String,
    songArtist: String,
    albumArtUri: Uri? = null,
    currentPositionMs: Long = 0L,
    durationMs: Long = 1L,
    modifier: Modifier = Modifier
) {
    // Rotation angle animatable that rotates continuously ONLY when isPlaying == true
    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3800, easing = LinearEasing),
                        repeatMode = AnimRepeatMode.Restart
                    )
                )
            }
        }
    }

    // Breathing pulse scale for acoustic visualizer mode
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                pulseAnim.animateTo(
                    targetValue = 1.12f,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                )
                pulseAnim.animateTo(
                    targetValue = 0.95f,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .testTag("now_playing_album_art_card"),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            "Cosmic Orbit" -> {
                // COSMIC ORBIT (Nebula Ring + Satellite Gauge + Cyan Waveform)
                val progressPercent = ((currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()) * 100).toInt().coerceIn(0, 100)

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Main Planetary Orbit Disc with Nebula Glow
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxR = size.minDimension / 2f
                            val centerPt = center

                            // Deep Space Nebula Glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6B21A8).copy(alpha = 0.55f),
                                        Color(0xFF9333EA).copy(alpha = 0.35f),
                                        Color(0xFF06B6D4).copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    center = centerPt,
                                    radius = maxR
                                ),
                                radius = maxR,
                                center = centerPt
                            )

                            // Nebula Cosmic Dust Ring
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFD946EF),
                                        Color(0xFF3B82F6),
                                        Color(0xFF00E5FF)
                                    )
                                ),
                                radius = maxR * 0.90f,
                                center = centerPt,
                                style = Stroke(width = 4.dp.toPx())
                            )

                            // Subtle starlight dust particles
                            for (i in 0..12) {
                                val angle = Math.toRadians((i * 30.0 + rotationAnim.value * 0.4).toDouble())
                                val starR = maxR * (0.86f + (i % 3) * 0.03f)
                                val starX = (centerPt.x + starR * Math.cos(angle)).toFloat()
                                val starY = (centerPt.y + starR * Math.sin(angle)).toFloat()
                                drawCircle(
                                    color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFF472B6),
                                    radius = 1.6.dp.toPx(),
                                    center = Offset(starX, starY)
                                )
                            }
                        }

                        // Dark Planetary Disc
                        Box(
                            modifier = Modifier
                                .size(138.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF111420))
                                .border(2.dp, Color(0xFF232A3E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerPt = center
                                val discR = size.minDimension / 2f

                                // Orbit Ellipse 1 (Horizontal tilt)
                                drawOval(
                                    color = Color(0xFF475569).copy(alpha = 0.45f),
                                    topLeft = Offset(centerPt.x - discR * 0.72f, centerPt.y - discR * 0.42f),
                                    size = androidx.compose.ui.geometry.Size(discR * 1.44f, discR * 0.84f),
                                    style = Stroke(width = 1.dp.toPx())
                                )

                                // Orbit Ellipse 2 (Cross tilt)
                                drawOval(
                                    color = Color(0xFF475569).copy(alpha = 0.35f),
                                    topLeft = Offset(centerPt.x - discR * 0.42f, centerPt.y - discR * 0.72f),
                                    size = androidx.compose.ui.geometry.Size(discR * 0.84f, discR * 1.44f),
                                    style = Stroke(width = 1.dp.toPx())
                                )

                                // Outer Disc Groove Ring
                                drawCircle(
                                    color = Color(0xFF1E293B),
                                    radius = discR * 0.88f,
                                    center = centerPt,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }

                            // Glowing Cyan Audio Waveform Visualizer in center
                            Box(
                                modifier = Modifier.size(76.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF).copy(alpha = 0.28f),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = size.minDimension * 0.45f
                                        ),
                                        radius = size.minDimension * 0.45f,
                                        center = center
                                    )
                                }

                                // 9 Vertical Cyan Waveform Bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val baseHeights = listOf(10.dp, 16.dp, 26.dp, 38.dp, 48.dp, 38.dp, 26.dp, 16.dp, 10.dp)
                                    baseHeights.forEachIndexed { index, baseH ->
                                        val animatedFactor = if (isPlaying) {
                                            val phase = (index % 3) * 0.25f
                                            (pulseAnim.value * (0.8f + phase)).coerceIn(0.5f, 1.45f)
                                        } else {
                                            0.5f
                                        }
                                        val barH = baseH * animatedFactor

                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(barH)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color(0xFF67E8F9),
                                                            Color(0xFF00E5FF),
                                                            Color(0xFF0891B2)
                                                        )
                                                    ),
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width((-8).dp))

                    // Attached Satellite Orbit Node (HUD Gauge + 96 KHz FLAC Badge)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0C101D))
                                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val r = size.minDimension / 2f
                                val c = center

                                val ticks = 24
                                for (i in 0 until ticks) {
                                    val angleRad = Math.toRadians((i * (360.0 / ticks) - 90.0).toDouble())
                                    val innerR = r * 0.74f
                                    val outerR = r * 0.88f
                                    val isFilled = (i.toFloat() / ticks) <= (progressPercent / 100f)
                                    drawLine(
                                        color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF334155),
                                        start = Offset(
                                            (c.x + innerR * Math.cos(angleRad)).toFloat(),
                                            (c.y + innerR * Math.sin(angleRad)).toFloat()
                                        ),
                                        end = Offset(
                                            (c.x + outerR * Math.cos(angleRad)).toFloat(),
                                            (c.y + outerR * Math.sin(angleRad)).toFloat()
                                        ),
                                        strokeWidth = 1.2.dp.toPx()
                                    )
                                }
                            }

                            Text(
                                text = "$progressPercent%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "96 KHz",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1),
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = "FLAC",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            "Vinyl Record" -> {
                // 1. VINYL RECORD
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C0E14))
                        .border(3.dp, if (outerBorderColor != Color.Unspecified) outerBorderColor else Color(0xFF1E2230), CircleShape)
                        .rotate(if (spinningVinyl && isPlaying) rotationAnim.value else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        // Concentric microgrooves
                        for (i in 1..14) {
                            drawCircle(
                                color = Color(0xFF1A1F2E).copy(alpha = 0.7f),
                                radius = maxR * (0.38f + i * 0.042f),
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        // Vinyl vinylite reflection highlights
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            radius = maxR * 0.96f,
                            center = center
                        )
                    }

                    // Center Vinyl Label Disc
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(activeAccentColor, if (outerBorderColor != Color.Unspecified) outerBorderColor.copy(alpha = 0.5f) else Color(0xFF1E1B4B))
                                )
                            )
                            .border(2.dp, activeAccentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Center Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06080F))
                            .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                    )
                }
            }

            "Classic Cover" -> {
                // 2. CLASSIC COVER (Displays real album artwork with fallback)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        var hasImageError by remember(albumArtUri) { mutableStateOf(false) }

                        if (albumArtUri != null && !hasImageError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(albumArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Album Artwork",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp)),
                                onError = { hasImageError = true }
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activeAccentColor.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = size.minDimension * 0.65f
                                    ),
                                    radius = size.minDimension * 0.65f,
                                    center = center
                                )
                                val maxR = size.minDimension / 2f
                                for (i in 1..3) {
                                    drawCircle(
                                        color = Color(0xFF374151).copy(alpha = 0.5f),
                                        radius = maxR * (0.4f + i * 0.16f),
                                        center = center,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(activeAccentColor, Color(0xFF1E1B4B))
                                        )
                                    )
                                    .border(2.dp, activeAccentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            "CD Disk" -> {
                // 3. COMPACT DISC (CD)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .border(2.dp, Color(0xFF94A3B8), CircleShape)
                        .rotate(rotationAnim.value),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        // Holographic rainbow diffraction sweep
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFE2E8F0),
                                    activeAccentColor.copy(alpha = 0.5f),
                                    Color(0xFFF43F5E).copy(alpha = 0.5f),
                                    Color(0xFFEAB308).copy(alpha = 0.5f),
                                    activeAccentColor.copy(alpha = 0.5f),
                                    Color(0xFFE2E8F0)
                                )
                            ),
                            radius = maxR * 0.96f,
                            center = center
                        )
                        // Mirror track separator ring
                        drawCircle(
                            color = Color(0xFF64748B).copy(alpha = 0.4f),
                            radius = maxR * 0.42f,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Polycarbonate inner ring
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                            .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                    )

                    // Center spindle hole
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF080B14))
                            .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                    )
                }
            }

            "Cassette Tape" -> {
                // 4. CASSETTE TAPE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, activeAccentColor.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top label strip
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = activeAccentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SIDE A",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccentColor
                                )
                                Text(
                                    text = songTitle.take(18),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "HI-FI 90",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Center Window with Dual Spinning Cogwheels
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(64.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Spool Cogwheel
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0))
                                        .border(2.dp, Color(0xFF94A3B8), CircleShape)
                                        .rotate(rotationAnim.value),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Central magnetic tape guide bridge
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(8.dp)
                                        .background(Color(0xFF451A03), RoundedCornerShape(4.dp))
                                )

                                // Right Spool Cogwheel
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0))
                                        .border(2.dp, Color(0xFF94A3B8), CircleShape)
                                        .rotate(rotationAnim.value),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Bottom trapezoid guide screws
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("AUDIO CASSETTE TAPE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, letterSpacing = 1.sp)
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            }

            "Walkman" -> {
                // 5. WALKMAN RETRO PLAYER
                val ledBreathingAlpha = if (isPlaying) {
                    val infiniteTransition = rememberInfiniteTransition(label = "LedBreathingTransition")
                    val breathingAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            repeatMode = AnimRepeatMode.Reverse
                        ),
                        label = "LedBreathingAlpha"
                    )
                    breathingAlpha
                } else {
                    1.0f
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(240.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Header: Walkman branding and LED Power Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "STEREO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccentColor,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WALKMAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 2.sp
                                )
                            }

                            // Power / Playback Indicator LED
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying) {
                                        // Subtle soft green glow / shadow behind the dot
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E).copy(alpha = ledBreathingAlpha * 0.35f))
                                        )
                                    }
                                    // Core LED dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlaying) {
                                                    Color(0xFF22C55E).copy(alpha = ledBreathingAlpha)
                                                } else {
                                                    Color(0xFFEF4444)
                                                }
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "PLAY" else "PAUSE",
                                    fontSize = 8.5.sp,
                                    color = if (isPlaying) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Large See-Through Cassette Chamber Door
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.5.dp, activeAccentColor.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Spool 1
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                            .rotate(rotationAnim.value),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Center viewing grid
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "◄ AUTO REVERSE ►",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeAccentColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(6.dp)
                                                .background(Color(0xFF451A03), RoundedCornerShape(2.dp))
                                        )
                                    }

                                    // Spool 2
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                            .rotate(rotationAnim.value),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Control Buttons styling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VOLUME MIN ◄► MAX", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("DOLBY NR • ON", fontSize = 8.sp, color = activeAccentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "Cyber Neon" -> {
                // 6. CYBER NEON HUD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF070B14))
                        .border(
                            BorderStroke(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        activeAccentColor,
                                        Color(0xFF00E5FF),
                                        Color(0xFFD500F9),
                                        activeAccentColor
                                    )
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val pulseScale = if (isPlaying) pulseAnim.value else 1f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        val centerPt = center

                        // Outer glowing pulse ring
                        drawCircle(
                            color = activeAccentColor.copy(alpha = 0.18f),
                            radius = maxR * 0.88f * pulseScale,
                            center = centerPt
                        )

                        // Cyber reticle ticks (36 radial ticks)
                        val tickCount = 36
                        for (i in 0 until tickCount) {
                            val angleRad = Math.toRadians((i * (360.0 / tickCount) + rotationAnim.value * 0.5).toDouble())
                            val innerR = maxR * 0.72f
                            val outerR = maxR * if (i % 3 == 0) 0.82f else 0.76f
                            val p1 = Offset(
                                (centerPt.x + innerR * Math.cos(angleRad)).toFloat(),
                                (centerPt.y + innerR * Math.sin(angleRad)).toFloat()
                            )
                            val p2 = Offset(
                                (centerPt.x + outerR * Math.cos(angleRad)).toFloat(),
                                (centerPt.y + outerR * Math.sin(angleRad)).toFloat()
                            )
                            val tickColor = if (i % 3 == 0) activeAccentColor else activeAccentColor.copy(alpha = 0.35f)
                            drawLine(
                                color = tickColor,
                                start = p1,
                                end = p2,
                                strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Inner dashed ring
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                            radius = maxR * 0.60f,
                            center = centerPt,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Rotating Cyber Inner Reticle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D1322))
                            .border(2.dp, activeAccentColor, CircleShape)
                            .rotate(-rotationAnim.value * 0.8f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        activeAccentColor.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 2f
                            )
                        }

                        // Center Holographic Icon
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Top Cyber Telemetry Header
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CYBER // HUD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeAccentColor,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = if (isPlaying) "AUDIO LINK ACTIVE" else "STANDBY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) Color(0xFF00E5FF) else Color(0xFFEF4444),
                            letterSpacing = 1.sp
                        )
                    }

                    // Bottom Frequency Label
                    Text(
                        text = "96kHz • 24BIT DSD STREAM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        letterSpacing = 1.sp
                    )
                }
            }

            "Studio Console" -> {
                // 7. STUDIO CONSOLE (Twin VU Meters + Master Knob)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                    border = BorderStroke(2.dp, Color(0xFF2A3142)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Console Top Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MASTER CONSOLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccentColor,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "BUS L / R",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Middle Section: Left & Right VU Meters + Master Center Knob
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Channel VU Meter
                            StudioVuMeterColumn(
                                label = "L",
                                isPlaying = isPlaying,
                                pulseFactor = pulseAnim.value,
                                accentColor = activeAccentColor
                            )

                            // Center Analog Gain Dial
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF262D3D), Color(0xFF0F121A))
                                        )
                                    )
                                    .border(2.5.dp, Color(0xFF3B4459), CircleShape)
                                    .rotate(if (isPlaying) (rotationAnim.value * 0.2f) % 90f - 45f else -45f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Dial outer tick marks
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = activeAccentColor.copy(alpha = 0.2f),
                                        radius = size.minDimension * 0.44f,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                // Dial Needle Indicator
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                        .width(3.dp)
                                        .height(18.dp)
                                        .background(activeAccentColor, RoundedCornerShape(2.dp))
                                )
                                // Knob Cap Center
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2433))
                                        .border(1.dp, activeAccentColor.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Right Channel VU Meter
                            StudioVuMeterColumn(
                                label = "R",
                                isPlaying = isPlaying,
                                pulseFactor = pulseAnim.value * 0.95f,
                                accentColor = activeAccentColor
                            )
                        }

                        // Console Bottom Info Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CLIP // 0.0dB",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) Color(0xFFF59E0B) else Color(0xFF64748B)
                            )
                            Text(
                                text = songTitle.take(18),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "HI-RES ANALOG",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccentColor
                            )
                        }
                    }
                }
            }

            "Acoustic Orb" -> {
                // 8. ACOUSTIC ORB (3D Harmonic Sound Sphere)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF1E1435),
                                    Color(0xFF090614)
                                )
                            )
                        )
                        .border(2.dp, activeAccentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val orbScale = if (isPlaying) pulseAnim.value else 1f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        val centerPt = center

                        // 3 Layer Acoustic Wave ripples
                        for (i in 1..4) {
                            drawCircle(
                                color = activeAccentColor.copy(alpha = (0.35f - i * 0.07f).coerceAtLeast(0.04f)),
                                radius = maxR * (0.35f + i * 0.15f) * orbScale,
                                center = centerPt,
                                style = Stroke(width = (2.dp - (0.3f * i).dp).toPx().coerceAtLeast(1f))
                            )
                        }

                        // Orbital dots
                        val orbitCount = 8
                        for (i in 0 until orbitCount) {
                            val angleRad = Math.toRadians((i * (360.0 / orbitCount) + rotationAnim.value * 0.7).toDouble())
                            val orbitR = maxR * 0.76f
                            val dotPos = Offset(
                                (centerPt.x + orbitR * Math.cos(angleRad)).toFloat(),
                                (centerPt.y + orbitR * Math.sin(angleRad)).toFloat()
                            )
                            drawCircle(
                                color = if (i % 2 == 0) activeAccentColor else Color(0xFF00E5FF),
                                radius = if (i % 2 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                                center = dotPos
                            )
                        }
                    }

                    // Glowing Central Plasma Core
                    Box(
                        modifier = Modifier
                            .size((90.dp * orbScale).coerceIn(70.dp, 120.dp))
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White,
                                        activeAccentColor,
                                        Color(0xFF7C3AED),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            "Minimalist Edge" -> {
                // 9. MINIMALIST EDGE (Modern Edge-to-Edge Frosted Cover)
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(2.dp, activeAccentColor.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background artwork or gradient art
                        var hasImageError by remember(albumArtUri) { mutableStateOf(false) }

                        if (albumArtUri != null && !hasImageError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(albumArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Minimalist Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onError = { hasImageError = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                activeAccentColor.copy(alpha = 0.45f),
                                                Color(0xFF0F172A),
                                                Color(0xFF020617)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = activeAccentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        // Gradient ambient shadow layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0xFF030712).copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        // Floating Glassmorphism Pill at bottom
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = songTitle,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = songArtist,
                                        color = activeAccentColor,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(activeAccentColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Waves else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // 10. FULL SCREEN / ACOUSTIC VISUALIZER CANVAS
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, activeAccentColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxR = size.minDimension / 2f
                            // Pulsating sound wave rings
                            for (i in 1..4) {
                                val scale = (0.35f + i * 0.16f) * pulseAnim.value
                                drawCircle(
                                    color = activeAccentColor.copy(alpha = (0.5f - i * 0.08f).coerceAtLeast(0.05f)),
                                    radius = maxR * scale,
                                    center = center,
                                    style = Stroke(width = (2.5f - i * 0.3f).dp.toPx())
                                )
                            }
                        }

                        // Animated Center Bouncing Bars
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val heights = listOf(28.dp, 44.dp, 60.dp, 75.dp, 88.dp, 75.dp, 60.dp, 44.dp, 28.dp)
                            heights.forEachIndexed { idx, baseH ->
                                val barHeight = if (isPlaying) {
                                    val factor = (pulseAnim.value * (0.8f + (idx % 3) * 0.2f)).coerceIn(0.6f, 1.4f)
                                    baseH * factor
                                } else {
                                    baseH * 0.6f
                                }
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(barHeight)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    if (visualizerPeakColor != Color.Unspecified) visualizerPeakColor else activeAccentColor,
                                                    activeAccentColor
                                                )
                                            ),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fast-Forward & Rewind High-Contrast Center Badge
        if (isFastForwarding) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeAccentColor,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast-Forward",
                        tint = Color(0xFF080B14),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "2x",
                        color = Color(0xFF080B14),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        } else if (isRewinding) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeSkipColor,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewinding",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "<<",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioVuMeterColumn(
    label: String,
    isPlaying: Boolean,
    pulseFactor: Float,
    accentColor: Color
) {
    val segmentColors = listOf(
        Color(0xFFEF4444), // +3dB Red (Peak)
        Color(0xFFF59E0B), // 0dB Amber
        Color(0xFF10B981), // -3dB Green
        Color(0xFF10B981), // -6dB Green
        Color(0xFF059669), // -12dB Green
        Color(0xFF047857)  // -20dB Green
    )
    val activeCount = if (isPlaying) {
        ((pulseFactor * 4.5f).toInt()).coerceIn(2, 6)
    } else {
        1
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        segmentColors.forEachIndexed { idx, col ->
            val segFromBottom = segmentColors.size - 1 - idx
            val isLit = segFromBottom < activeCount
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isLit) col else Color(0xFF1E293B))
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
    }
}

/**
 * Dedicated Cosmic Orbit Layout precisely matching the user's uploaded design.
 * Features:
 * - "Playlist: Starlight Selections" header & title
 * - Glowing Purple Heart favorite button
 * - Planetary Disc with etched orbital paths, live cyan waveform bars & attached satellite gauge HUD (percentage + 96 KHz FLAC)
 * - Analog tick ruler above seekbar
 * - Sleek pill seekbar with timestamps inside/aligned
 * - 3D embossed Shuffle & Repeat buttons with purple neon halo glow (#A855F7)
 * - 3D Previous, Play/Pause, Next controls with cyan & purple concentric rims
 */
@Composable
private fun CosmicOrbitNowPlayingLayout(
    currentSong: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    isFastForwarding: Boolean,
    isRewinding: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkipBackward10: () -> Unit,
    onSkipForward10: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onStartFastForward: () -> Unit,
    onStopFastForward: () -> Unit,
    onStartRewind: () -> Unit,
    onStopRewind: () -> Unit,
    currentIndex: Int = 0,
    totalTracks: Int = 1
) {
    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6000, easing = LinearEasing),
                        repeatMode = AnimRepeatMode.Restart
                    )
                )
            }
        }
    }

    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                pulseAnim.animateTo(
                    targetValue = 1.25f,
                    animationSpec = tween(durationMillis = 450, easing = LinearEasing)
                )
                pulseAnim.animateTo(
                    targetValue = 0.85f,
                    animationSpec = tween(durationMillis = 450, easing = LinearEasing)
                )
            }
        }
    }

    val progressPercent = ((currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()) * 100).toInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 1. Center Cosmic Orbit Disc (Hard-locked to exactly 200.dp, centered horizontally)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.size(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Main Planetary Orbit Disc with Nebula Glow
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Cosmic Nebula Ring Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        val centerPt = center

                        // Deep Space Nebula Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6B21A8).copy(alpha = 0.55f),
                                    Color(0xFF9333EA).copy(alpha = 0.35f),
                                    Color(0xFF06B6D4).copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = centerPt,
                                radius = maxR
                            ),
                            radius = maxR,
                            center = centerPt
                        )

                        // Nebula Cosmic Dust Ring
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFD946EF),
                                    Color(0xFF3B82F6),
                                    Color(0xFF00E5FF)
                                )
                            ),
                            radius = maxR * 0.90f,
                            center = centerPt,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Subtle starlight dust particles
                        for (i in 0..12) {
                            val angle = Math.toRadians((i * 30.0 + rotationAnim.value * 0.4).toDouble())
                            val starR = maxR * (0.86f + (i % 3) * 0.03f)
                            val starX = (centerPt.x + starR * Math.cos(angle)).toFloat()
                            val starY = (centerPt.y + starR * Math.sin(angle)).toFloat()
                            drawCircle(
                                color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFF472B6),
                                radius = 1.6.dp.toPx(),
                                center = Offset(starX, starY)
                            )
                        }
                    }

                    // Dark Planetary Disc
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111420))
                            .border(2.dp, Color(0xFF232A3E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Etched Planetary / Atomic Orbit Trajectories
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerPt = center
                            val discR = size.minDimension / 2f

                            // Orbit Ellipse 1 (Horizontal tilt)
                            drawOval(
                                color = Color(0xFF475569).copy(alpha = 0.45f),
                                topLeft = Offset(centerPt.x - discR * 0.72f, centerPt.y - discR * 0.42f),
                                size = androidx.compose.ui.geometry.Size(discR * 1.44f, discR * 0.84f),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Orbit Ellipse 2 (Cross tilt)
                            drawOval(
                                color = Color(0xFF475569).copy(alpha = 0.35f),
                                topLeft = Offset(centerPt.x - discR * 0.42f, centerPt.y - discR * 0.72f),
                                size = androidx.compose.ui.geometry.Size(discR * 0.84f, discR * 1.44f),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Outer Disc Groove Ring
                            drawCircle(
                                color = Color(0xFF1E293B),
                                radius = discR * 0.88f,
                                center = centerPt,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Glowing Cyan Audio Waveform Visualizer in center
                        Box(
                            modifier = Modifier.size(78.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Soft cyan ambient radial aura behind wave bars
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = size.minDimension * 0.45f
                                    ),
                                    radius = size.minDimension * 0.45f,
                                    center = center
                                )
                            }

                            // 9 Vertical Cyan Waveform Bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val baseHeights = listOf(10.dp, 16.dp, 26.dp, 38.dp, 48.dp, 38.dp, 26.dp, 16.dp, 10.dp)
                                baseHeights.forEachIndexed { index, baseH ->
                                    val animatedFactor = if (isPlaying) {
                                        val phase = (index % 3) * 0.25f
                                        (pulseAnim.value * (0.8f + phase)).coerceIn(0.5f, 1.45f)
                                    } else {
                                        0.5f
                                    }
                                    val barH = baseH * animatedFactor

                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barH)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF67E8F9),
                                                        Color(0xFF00E5FF),
                                                        Color(0xFF0891B2)
                                                    )
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width((-8).dp))

                // Attached Satellite Orbit Node (HUD Gauge + 96 KHz FLAC Badge)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    // Circular Satellite Gauge Pod
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C101D))
                            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Radial ticks and progress arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            val c = center

                            // Outer radial tick marks
                            val ticks = 24
                            for (i in 0 until ticks) {
                                val angleRad = Math.toRadians((i * (360.0 / ticks) - 90.0).toDouble())
                                val innerR = r * 0.74f
                                val outerR = r * 0.88f
                                val isFilled = (i.toFloat() / ticks) <= (progressPercent / 100f)
                                drawLine(
                                    color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF334155),
                                    start = Offset(
                                        (c.x + innerR * Math.cos(angleRad)).toFloat(),
                                        (c.y + innerR * Math.sin(angleRad)).toFloat()
                                    ),
                                    end = Offset(
                                        (c.x + outerR * Math.cos(angleRad)).toFloat(),
                                        (c.y + outerR * Math.sin(angleRad)).toFloat()
                                    ),
                                    strokeWidth = 1.2.dp.toPx()
                                )
                            }
                        }

                        // Center Percentage Display
                        Text(
                            text = "$progressPercent%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // 96 KHz / FLAC Audio Badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "96 KHz",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "FLAC",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }

        // 2. Playlist Tag & Track Title & Heart Row (Exactly matching image!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Playlist: Starlight Selections",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoScrollText(
                    text = currentSong.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentSong.artist} • ${currentSong.album}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Glowing Purple Heart Favorite Button (matching user's image)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9D4EDD).copy(alpha = 0.18f))
                    .clickable { onToggleFavorite(currentSong.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) Color(0xFF9D4EDD) else Color(0xFF9D4EDD).copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SEEKBAR & TIMESTAMPS ROW (WITH CENTERED SONG COUNT)
        TouchableProgressBar(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs.coerceAtLeast(1L),
            onSeekTo = onSeekTo,
            barHeight = 6.dp,
            touchTargetHeight = 36.dp,
            activeGradient = listOf(Color(0xFF00F5FF), Color(0xFF9D4EDD)),
            inactiveColor = Color(0xFF334155).copy(alpha = 0.5f),
            thumbColor = Color(0xFF00F5FF),
            thumbGlowColor = Color(0xFF00F5FF).copy(alpha = 0.6f),
            showThumb = true,
            tag = "cosmic_progress_slider"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMs(currentPositionMs),
                fontSize = 12.sp,
                color = Color(0xFF7E8B9B),
                modifier = Modifier.testTag("current_playback_timestamp")
            )

            Text(
                text = "${currentIndex + 1} / ${totalTracks}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag("track_counter_pill")
            )

            Text(
                text = formatMs(durationMs),
                fontSize = 12.sp,
                color = Color(0xFF7E8B9B),
                modifier = Modifier.testTag("total_duration_timestamp")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Shuffle, Track Counter & Repeat Row (Cosmic Orbit Theme)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Shuffle Button with Neumorphic Dual Shadow (Bottom-Right: dark shadow, Top-Left: white highlight)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (shuffleEnabled) {
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF9D4EDD).copy(alpha = 0.35f),
                                    Color(0xFF9D4EDD).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        } else {
                            SolidColor(Color.Transparent)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x2B000000),
                            spotColor = Color(0x2B000000)
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF223048),
                                    Color(0xFF162544),
                                    Color(0xFF0E192E)
                                ),
                                start = Offset.Zero,
                                end = Offset(80f, 80f)
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (shuffleEnabled) Color(0xFF00F5FF) else Color(0xFF334155)
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onToggleShuffle)
                        .testTag("shuffle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) Color(0xFF00F5FF) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Right: Repeat Button with Neumorphic Dual Shadow
            val (repIcon, isRepActive) = when (repeatMode) {
                RepeatMode.OFF -> Icons.Default.Repeat to false
                RepeatMode.ALL -> Icons.Default.Repeat to true
                RepeatMode.ONE -> Icons.Default.RepeatOne to true
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRepActive) {
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF00F5FF).copy(alpha = 0.35f),
                                    Color(0xFF00F5FF).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        } else {
                            SolidColor(Color.Transparent)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x2B000000),
                            spotColor = Color(0x2B000000)
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF223048),
                                    Color(0xFF162544),
                                    Color(0xFF0E192E)
                                ),
                                start = Offset.Zero,
                                end = Offset(80f, 80f)
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isRepActive) Color(0xFF00F5FF) else Color(0xFF334155)
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onCycleRepeat)
                        .testTag("repeat_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = repIcon,
                        contentDescription = "Repeat",
                        tint = if (isRepActive) Color(0xFF00F5FF) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. MAIN PLAYBACK CONTROLS ROW (ENLARGED & EDGE-ALIGNED)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Rewind 10s Button: Modifier.size(46.dp), icon 30.dp
            IconButton(
                onClick = onSkipBackward10,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("skip_backward_10_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 Seconds",
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(30.dp)
                )
            }

            // 2. Previous Button with 3D Neumorphic Extrusion: Container 52.dp, icon 36.dp
            HoldableIconButton(
                onClick = onPrevious,
                onHoldStart = onStartRewind,
                onHoldEnd = onStopRewind,
                isHolding = isRewinding,
                size = 52.dp,
                activeGlowColor = Color(0xFF00F5FF).copy(alpha = 0.5f),
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x2B000000),
                        spotColor = Color(0x2B000000)
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF223048),
                                Color(0xFF162544),
                                Color(0xFF0E192E)
                            ),
                            start = Offset.Zero,
                            end = Offset(80f, 80f)
                        ),
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        if (isRewinding) Color(0xFF00F5FF) else Color(0xFF334155),
                        CircleShape
                    )
                    .testTag("previous_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = if (isRewinding) Color(0xFF00F5FF) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(36.dp)
                )
            }

            // 3. Central Big Play/Pause Circular Button: strictly 86.dp, icon 46.dp, Vibrant Neon Cyan #00F5FF
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(86.dp)
                    .shadow(10.dp, CircleShape, ambientColor = Color(0xFF00F5FF), spotColor = Color(0xFF00F5FF))
                    .clip(CircleShape)
                    .background(Color(0xFF00F5FF))
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF080B14),
                    modifier = Modifier.size(46.dp)
                )
            }

            // 4. Next Button with 3D Neumorphic Extrusion: Container 52.dp, icon 36.dp
            HoldableIconButton(
                onClick = onNext,
                onHoldStart = onStartFastForward,
                onHoldEnd = onStopFastForward,
                isHolding = isFastForwarding,
                size = 52.dp,
                activeGlowColor = Color(0xFF00F5FF).copy(alpha = 0.5f),
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x2B000000),
                        spotColor = Color(0x2B000000)
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF223048),
                                Color(0xFF162544),
                                Color(0xFF0E192E)
                            ),
                            start = Offset.Zero,
                            end = Offset(80f, 80f)
                        ),
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        if (isFastForwarding) Color(0xFF00F5FF) else Color(0xFF334155),
                        CircleShape
                    )
                    .testTag("next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = if (isFastForwarding) Color(0xFF00F5FF) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(36.dp)
                )
            }

            // 5. Forward 10s Button: Modifier.size(46.dp), icon 30.dp
            IconButton(
                onClick = onSkipForward10,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("skip_forward_10_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 Seconds",
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}


