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
import androidx.compose.foundation.layout.width
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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val backgroundInteractionSource = remember { MutableInteractionSource() }

    // Request 2: Blinking/flashing animation for current playback timestamp on pause (1.0 to 0.2 alpha oscillation)
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

    // Search bar in Now Playing state (Request 9)
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val activeAccentColor = seekbarActiveColor

    val backdropModifier = if (!customThemeSettings.isEnabled) {
        modifier
            .fillMaxSize()
            .background(activePalette.ambientBackdropBrush)
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = backdropModifier
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null
            ) {
                // Dismiss keyboard and clear focus on tap outside
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        // Base Content Layer (Fixed, stable, never pushed by search results)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Reserved space matching Search Bar height at top
            Spacer(modifier = Modifier.height(56.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Interactive Album Art Presentation supporting 6 distinct styles
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
            albumArtUri = currentSong.albumArtUri
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Song Title with Auto-Scroll & Favorite Heart Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                // Auto-scrolling song title with 3-second hold cycle
                AutoScrollText(
                    text = currentSong.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentSong.artist} • ${currentSong.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Prominent Heart Favorite Button
            IconButton(
                onClick = { onToggleFavorite(currentSong.id) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) Color(0xFFEF4444).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("now_playing_heart_button")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Request 8: Touchable Progress Bar with instant skip on direct tap
        Column(modifier = Modifier.fillMaxWidth()) {
            TouchableProgressBar(
                currentPositionMs = currentPositionMs,
                durationMs = safeDuration,
                onSeekTo = onSeekTo,
                barHeight = 6.dp,
                touchTargetHeight = 40.dp,
                activeGradient = seekbarFillGradient,
                inactiveColor = seekbarInactiveColor,
                thumbColor = seekbarThumbColor,
                thumbGlowColor = seekbarThumbGlowColor,
                showThumb = true,
                tag = "track_progress_slider"
            )

            // Request 3: Reposition Shuffle and Repeat buttons directly in line with progress bar timestamps
            // Far Left: Shuffle | Next: Current Timestamp | Spacer | Duration | Far Right: Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Far Left: Shuffle button (compact & subtle, cyan accent only when active)
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) activeShuffleRepeatActiveColor else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Current playback timestamp (Request 2: Blinking/flashing animation when paused)
                Text(
                    text = formatMs(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .graphicsLayer { alpha = blinkAlpha }
                        .testTag("current_playback_timestamp")
                )

                Spacer(modifier = Modifier.weight(1f))

                // Total duration timestamp
                Text(
                    text = formatMs(safeDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("total_duration_timestamp")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Far Right: Repeat button (compact & subtle, active accent only when active)
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("repeat_button")
                ) {
                    val (icon, tint) = when (repeatMode) {
                        RepeatMode.OFF -> Icons.Default.Repeat to Color(0xFF64748B)
                        RepeatMode.ALL -> Icons.Default.Repeat to activeShuffleRepeatActiveColor
                        RepeatMode.ONE -> Icons.Default.RepeatOne to activeShuffleRepeatActiveColor
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat: ${repeatMode.name}",
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Gesture UI Feedback Badge (Active while holding Next for 2x Fast-Forward or Previous for Continuous Rewind)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Main Playback Controls: 10s Rev, Prev, Play/Pause, Next, 10s Fwd (Ergonomically Lifted & High Contrast)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 10s Rewind / Skip Reverse (Shifted to outermost end to prevent accidental taps)
            IconButton(
                onClick = onSkipBackward10,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("skip_backward_10_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 Seconds",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Center Playback Group with comfortable spacing between Prev, Play, Next
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous (Single tap = Previous track; Long press = Continuous rewind)
                HoldableIconButton(
                    onClick = onPrevious,
                    onHoldStart = onStartRewind,
                    onHoldEnd = onStopRewind,
                    isHolding = isRewinding,
                    size = 52.dp,
                    activeGlowColor = activeSkipGlowColor,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Massive 2X Center Button (88.dp container, 46.dp bold icon)
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(activePlayBrush)
                        .border(2.5.dp, activePlayGlowColor, CircleShape)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF080B14),
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Next (Single tap = Next track; Long press = Fast-forward 2x)
                HoldableIconButton(
                    onClick = onNext,
                    onHoldStart = onStartFastForward,
                    onHoldEnd = onStopFastForward,
                    isHolding = isFastForwarding,
                    size = 52.dp,
                    activeGlowColor = activeSkipGlowColor,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // 10s Fast-Forward / Skip Forward (Shifted to outermost end to prevent accidental taps)
            IconButton(
                onClick = onSkipForward10,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("skip_forward_10_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 Seconds",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Top Floating Overlay Layer: Search Bar & Floating Search Results (Never pushes base content)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(20f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(44.dp)
                .testTag("now_playing_search_bar"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search songs to play immediately...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Floating / expanding scrollable dropdown overlay
        AnimatedVisibility(
            visible = searchQuery.isNotBlank(),
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 380.dp)
                    .testTag("now_playing_search_results"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "SEARCH RESULTS (${searchResults.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    if (searchResults.isEmpty()) {
                        Text(
                            text = "No songs matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 330.dp)
                        ) {
                            items(searchResults, key = { it.id }) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            onSongSelect(song)
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatMs(song.durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
    albumArtUri: Uri? = null
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
        modifier = Modifier
            .fillMaxWidth(0.70f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .testTag("now_playing_album_art_card"),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            "Vinyl Record" -> {
                // 1. VINYL RECORD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

            else -> {
                // 6. FULL SCREEN / ACOUSTIC VISUALIZER CANVAS
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
