package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocalThemePalette
import com.example.model.Song

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    autoRotateActive: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onExpandClick: () -> Unit,
    isFastForwarding: Boolean = false,
    isRewinding: Boolean = false,
    onStartFastForward: () -> Unit = {},
    onStopFastForward: () -> Unit = {},
    onStartRewind: () -> Unit = {},
    onStopRewind: () -> Unit = {},
    customVisualizerText: String = "Naasir",
    showVisualizerText: Boolean = true,
    visualizerTextColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val palette = LocalThemePalette.current
    val resolvedVisualizerColor = if (visualizerTextColor != Color.Unspecified) visualizerTextColor else palette.primaryAccent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("mini_player_container"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Upper section: Clickable track info on left, independent action buttons on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track Info (Click to expand to Now Playing Screen)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onExpandClick)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("mini_player_expand_area"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Audio Disc
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, if (autoRotateActive) palette.primaryAccent else palette.secondaryAccent, CircleShape)
                            .testTag("mini_player_visualizer_disc"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isPlaying) palette.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Auto-Scrolling Title & Artist
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AutoScrollText(
                            text = currentSong.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFastForwarding) {
                                Text(
                                    text = ">> 2X SPEED • ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            } else if (isRewinding) {
                                Text(
                                    text = "<< REWIND • ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            } else if (autoRotateActive) {
                                Text(
                                    text = "3D 8D • ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Playback Buttons Row (Direct touch targets, zero click interference)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Previous Button
                    HoldableIconButton(
                        onClick = onPreviousClick,
                        onHoldStart = onStartRewind,
                        onHoldEnd = onStopRewind,
                        isHolding = isRewinding,
                        activeGlowColor = palette.secondaryAccent,
                        size = 40.dp,
                        modifier = Modifier.testTag("mini_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = if (isRewinding) palette.secondaryAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause Button with prominent glow
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("mini_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next Button
                    HoldableIconButton(
                        onClick = onNextClick,
                        onHoldStart = onStartFastForward,
                        onHoldEnd = onStopFastForward,
                        isHolding = isFastForwarding,
                        activeGlowColor = palette.primaryAccent,
                        size = 40.dp,
                        modifier = Modifier.testTag("mini_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = if (isFastForwarding) palette.primaryAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Direct-touchable progress bar with instant seek on tap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(progressMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.width(6.dp))

                TouchableProgressBar(
                    currentPositionMs = progressMs,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.weight(1f),
                    barHeight = 5.dp,
                    touchTargetHeight = 30.dp,
                    activeColor = palette.seekBarActiveTrack,
                    thumbColor = palette.primaryAccent,
                    showThumb = true,
                    tag = "mini_seek_bar"
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
