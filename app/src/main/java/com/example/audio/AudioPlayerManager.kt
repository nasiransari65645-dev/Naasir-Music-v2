package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.model.AutoRotateDirection
import com.example.model.DpsProfile
import com.example.model.EqualizerFrequencies
import com.example.model.EqualizerPresets
import com.example.model.EqualizerState
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SpatialEnvironment
import com.example.storage.SettingsPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val appVolume: Float = 1.0f,
    // 3D Audio Effects (Defaults to OFF so no effects alter playback per User Request 1)
    val autoRotateEnabled: Boolean = false,
    val autoRotateSpeedSec: Float = 6.0f, // seconds per full 360 circle
    val rotationSpeedMultiplier: Float = 1.0f, // 0.1x to 5.0x
    val autoRotateDirection: AutoRotateDirection = AutoRotateDirection.LEFT_TO_RIGHT,
    val randomRotationEnabled: Boolean = false, // -90° to +90° random stochastic rotation
    val stereoWidth: Float = 1.0f, // 0.2 to 1.0 (stereo panning depth)
    val manualPan: Float = 0.0f, // -1.0 (full left) to 1.0 (full right)
    val currentAngleDegrees: Float = 0.0f, // 0 to 360 for UI Radar
    val currentPanValue: Float = 0.0f, // -1.0 to 1.0 for UI meter
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0, // 0 to 1000 (0% to 100%)
    val virtualizerMode: String = "Headphones", // "Headphones" or "External Speakers"
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0, // 0 to 1000
    // Acoustic Environment Master & Controls
    val reverbMasterEnabled: Boolean = false,
    val reverbWetDryMix: Float = 0.70f, // 0.0f to 1.0f
    val reverbDecayTime: Float = 2.0f, // 0.1f to 5.0f seconds
    val spatialEnvironment: SpatialEnvironment = SpatialEnvironment.STUDIO_DRY,
    // DPS Audio Engine
    val dpsEnabled: Boolean = false,
    val dpsProfile: DpsProfile = DpsProfile.MUSIC,
    val dpsBassBoost: Int = 50, // 0 to 100%
    val dpsClarity: Int = 50, // 0 to 100%
    val soundstageDistance: String = "MEDIUM", // NEAR, MEDIUM, FAR, STADIUM
    val elevation: String = "EYE_LEVEL", // ABOVE, EYE_LEVEL, BELOW
    val headShadowEnabled: Boolean = false,
    val binauralWidener: Float = 1.0f, // 0.5 to 2.0
    val isEffectSupported: Boolean = true,
    // 13-Band Equalizer State
    val equalizerState: EqualizerState = EqualizerState(),
    // Stereo Delay / Echo Audio Effect (Rack Style)
    val delayEnabled: Boolean = false,
    val delayTimeMs: Int = 350, // 50ms to 1000ms (default 350ms)
    val delayFeedbackPercent: Int = 40, // 0% to 85% (default 40%)
    val delayMixPercent: Int = 30, // 0% to 100% (default 30%)
    // In-memory Favorite song IDs set (session-only)
    val favoriteIds: Set<Long> = emptySet(),
    // Long-Press Fast-Forward (2x speed) & Continuous Rewind states
    val isFastForwarding: Boolean = false,
    val isRewinding: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val playSessionId: Long = 0L
)

class AudioPlayerManager(
    private val context: Context,
    externalScope: CoroutineScope? = null
) {
    private val TAG = "AudioPlayerManager"
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null
    private var environmentalReverb: EnvironmentalReverb? = null
    private var hardwareEqualizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var currentAttachedAudioSessionId: Int = -1
    private var lastAcousticPreset: Short = -1
    private var lastAcousticActive: Boolean = false
    private var isReverbAuxAttached: Boolean = false

    // Dedicated Real-time Stereo Delay / Echo DSP Processor
    val stereoDelayProcessor = StereoDelayAudioProcessor()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var playlist: List<Song> = emptyList()
    private var progressJob: Job? = null
    private var autoRotateJob: Job? = null
    private var rewindJob: Job? = null

    // Audio Parameter Smoothing (De-zippering) & Equal-Power Panning state
    private var smoothedLeftGain: Float = 0.7071f
    private var smoothedRightGain: Float = 0.7071f
    private var targetLeftGain: Float = 0.7071f
    private var targetRightGain: Float = 0.7071f
    private var lastAppliedLeftGain: Float = -1.0f
    private var lastAppliedRightGain: Float = -1.0f

    // Continuous Orbit Angular Trajectory in radians: [0, 2π)
    private var orbitAngleRad: Float = 0.0f
    private var randomTargetAngleRad: Float = 0.0f
    private var randomTargetTimer: Int = 0

    // For shuffle queue
    private var shuffledIndices: List<Int> = emptyList()
    private var currentShufflePointer: Int = 0

    private var isUserInitiatedPlay: Boolean = false
    @Volatile
    private var isSwitchingTrack: Boolean = false

    private val prefsManager by lazy { SettingsPreferencesManager(context) }

    fun saveCurrentPlaybackState(customPrefs: SettingsPreferencesManager? = null) {
        val prefs = customPrefs ?: prefsManager
        val song = _state.value.currentSong ?: return
        val pos = try {
            mediaPlayer?.currentPosition?.toLong() ?: _state.value.currentPositionMs
        } catch (_: Exception) {
            _state.value.currentPositionMs
        }
        prefs.saveLastPlayedSongId(song.id)
        prefs.saveLastPositionMs(pos)
        prefs.saveShuffleEnabled(_state.value.shuffleEnabled)
        prefs.saveRepeatMode(_state.value.repeatMode.name)
    }

    companion object {
        @Volatile
        var instance: AudioPlayerManager? = null
            private set

        private const val TWO_PI = (2.0 * Math.PI).toFloat()
        private const val HALF_PI = (Math.PI * 0.5).toFloat()

        // Audio Parameter Smoothing (De-zippering) constants:
        // Exponential interpolation factor (one-pole low-pass filter)
        // With an update tick of 20ms, alpha = 0.28f yields ~35ms smoothing window
        // for click-free de-zippering and buffer slew-rate limiting.
        private const val DE_ZIPPER_SMOOTHING_ALPHA = 0.28f
        private const val MAX_GAIN_SLEW_PER_STEP = 0.06f
        private const val ORBIT_UPDATE_INTERVAL_MS = 20L
    }

    init {
        instance = this
        startAutoRotateLoop()
        registerAudioBecomingNoisyReceiver()
    }

    private var isNoisyReceiverRegistered: Boolean = false
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val shouldAutoPause = prefsManager.loadAutoPauseHeadset()
                Log.d(TAG, "ACTION_AUDIO_BECOMING_NOISY received. autoPauseHeadset=$shouldAutoPause, isPlaying=${_state.value.isPlaying}")
                if (shouldAutoPause && _state.value.isPlaying) {
                    pause()
                }
            }
        }
    }

    private fun registerAudioBecomingNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(audioBecomingNoisyReceiver, filter)
                isNoisyReceiverRegistered = true
                Log.d(TAG, "Registered ACTION_AUDIO_BECOMING_NOISY receiver")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to register ACTION_AUDIO_BECOMING_NOISY receiver: ${t.message}")
            }
        }
    }

    private fun unregisterAudioBecomingNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(audioBecomingNoisyReceiver)
                isNoisyReceiverRegistered = false
                Log.d(TAG, "Unregistered ACTION_AUDIO_BECOMING_NOISY receiver")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to unregister ACTION_AUDIO_BECOMING_NOISY receiver: ${t.message}")
            }
        }
    }

    fun setPlaylist(songs: List<Song>) {
        val idsChanged = playlist.map { it.id } != songs.map { it.id }
        playlist = songs
        if (idsChanged) {
            updateShuffleList()
        }
        val cur = _state.value.currentSong
        if (cur != null) {
            val updated = songs.find { it.id == cur.id }
            if (updated != null && updated != cur) {
                _state.update { it.copy(currentSong = updated) }
                if (MediaPlaybackService.isServiceRunning) {
                    MediaPlaybackService.startOrUpdate(
                        context = context,
                        song = updated,
                        isPlaying = _state.value.isPlaying,
                        positionMs = _state.value.currentPositionMs,
                        durationMs = _state.value.durationMs
                    )
                }
            }
        }
    }

    fun updateCurrentSongAlbumArt(artUriString: String) {
        val cur = _state.value.currentSong ?: return
        val updated = cur.copy(customAlbumArtUri = artUriString)
        _state.update { it.copy(currentSong = updated) }
        playlist = playlist.map { if (it.id == cur.id) updated else it }
        if (MediaPlaybackService.isServiceRunning) {
            MediaPlaybackService.startOrUpdate(
                context = context,
                song = updated,
                isPlaying = _state.value.isPlaying,
                positionMs = _state.value.currentPositionMs,
                durationMs = _state.value.durationMs
            )
        }
    }

    private fun updateShuffleList() {
        if (playlist.isEmpty()) {
            shuffledIndices = emptyList()
            currentShufflePointer = 0
            return
        }
        shuffledIndices = playlist.indices.shuffled()
        currentShufflePointer = 0
    }

    fun loadSongSilently(song: Song, initialPositionMs: Long = 0L) {
        isUserInitiatedPlay = false
        scope.launch(Dispatchers.Main.immediate) {
            prepareAndSetup(song, autoPlay = false, initialSeekPos = initialPositionMs)
        }
    }

    fun playSong(song: Song) {
        isUserInitiatedPlay = true
        scope.launch(Dispatchers.Main.immediate) {
            try {
                prepareAndSetup(song, autoPlay = true)
            } catch (t: Throwable) {
                Log.e(TAG, "Error playing song: ${song.title} - ${t.message}", t)
                _state.update { it.copy(isPlaying = false) }
            }
        }
    }

    private fun prepareAndSetup(song: Song, autoPlay: Boolean, initialSeekPos: Long = 0L) {
        isSwitchingTrack = true
        try { mediaPlayer?.setVolume(1.0f, 1.0f) } catch (_: Exception) {}

        var player = mediaPlayer
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (t: Throwable) {}
            try {
                player.reset()
            } catch (t: Throwable) {
                try { player.release() } catch (e: Throwable) {}
                releaseEffects()
                player = null
                mediaPlayer = null
            }
        }

        if (player == null) {
            player = MediaPlayer().apply {
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    scope.launch(Dispatchers.Main.immediate) {
                        _state.update { it.copy(isPlaying = false) }
                        try { mp.reset() } catch (t: Throwable) {}
                    }
                    true
                }
            }
            mediaPlayer = player
        }

        try {
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        } catch (t: Throwable) {}
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        // Safe data source loading without premature closing of descriptors
        if (song.path.isNotEmpty() && java.io.File(song.path).exists()) {
            player.setDataSource(song.path)
        } else {
            player.setDataSource(context, song.uri)
        }

        try {
            player.prepare()
        } catch (t: Throwable) {
            Log.e(TAG, "MediaPlayer prepare error: ${t.message}")
            isSwitchingTrack = false
            return
        }

        val sessionId = try { player.audioSessionId } catch (t: Throwable) { 0 }
        if (autoPlay && isUserInitiatedPlay) {
            if (sessionId > 0) {
                initAudioEffects(sessionId)
            }
            applySpatialSettings()
            applyEqualizerBands()
            applyPlaybackParams()
        } else {
            // PRELOAD ONLY: Do NOT call applyPlaybackParams() while paused.
            // MediaPlayer.playbackParams implicitly forces playback to start!
            scope.launch(Dispatchers.Default) {
                if (sessionId > 0) {
                    initAudioEffects(sessionId)
                }
                withContext(Dispatchers.Main.immediate) {
                    applySpatialSettings()
                    applyEqualizerBands()
                    // OMIT applyPlaybackParams() here so the engine never starts
                }
            }
        }

        val dur = try {
            player.duration.toLong().coerceAtLeast(song.durationMs)
        } catch (t: Throwable) {
            song.durationMs
        }

        player.setOnCompletionListener {
            handleTrackCompletion()
        }

        if (autoPlay && isUserInitiatedPlay) {
            try {
                if (!player.isPlaying) {
                    player.start()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start player in prepareAndSetup", t)
            }
            // Immediately apply current spatial pan
            val curPan = if (_state.value.autoRotateEnabled) {
                _state.value.currentPanValue * _state.value.stereoWidth
            } else {
                0.0f
            }
            applyVolumePan(curPan)

            _state.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    durationMs = dur,
                    currentPositionMs = 0L,
                    playSessionId = it.playSessionId + 1
                )
            }
            startProgressTracker()
            notifyServiceState(song = song, isPlaying = true, pos = 0L, dur = dur)
        } else {
            // HARD LOCK IDLE STATE
            val targetPos = initialSeekPos.coerceAtLeast(0L).toInt()
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                if (targetPos > 0) {
                    player.seekTo(targetPos)
                } else {
                    player.seekTo(0)
                }
            } catch (_: Exception) {}

            _state.update {
                it.copy(
                    currentSong = song,
                    isPlaying = false,
                    durationMs = dur,
                    currentPositionMs = targetPos.toLong()
                )
            }
            notifyServiceState(song = song, isPlaying = false, pos = targetPos.toLong(), dur = dur)
        }
        isSwitchingTrack = false
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val realIsPlaying = try { player?.isPlaying == true } catch (_: Exception) { false }
        if (_state.value.isPlaying || realIsPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val target = positionMs.coerceAtLeast(0L)
                mediaPlayer?.let { player ->
                    try {
                        player.seekTo(target.toInt())
                    } catch (t: Throwable) {}
                }
                _state.update { it.copy(currentPositionMs = target) }
                notifyServiceState(pos = target)
                saveCurrentPlaybackState()
            } catch (t: Throwable) {
                Log.e(TAG, "Seek error: ${t.message}")
            }
        }
    }

    fun playNext() {
        isUserInitiatedPlay = true
        if (playlist.isEmpty()) return
        val current = _state.value.currentSong
        val nextSong: Song = if (_state.value.shuffleEnabled) {
            if (shuffledIndices.isEmpty()) updateShuffleList()
            if (shuffledIndices.isEmpty()) {
                playlist.first()
            } else {
                currentShufflePointer = (currentShufflePointer + 1) % shuffledIndices.size
                val targetIdx = shuffledIndices.getOrElse(currentShufflePointer) { 0 }
                playlist.getOrElse(targetIdx) { playlist.first() }
            }
        } else {
            val currentIdx = playlist.indexOfFirst { it.id == current?.id }
            val nextIdx = if (currentIdx in playlist.indices) {
                (currentIdx + 1) % playlist.size
            } else {
                0
            }
            playlist.getOrElse(nextIdx) { playlist.first() }
        }
        playSong(nextSong)
    }

    fun playPrevious() {
        isUserInitiatedPlay = true
        if (playlist.isEmpty()) return
        val currentPosition = try {
            mediaPlayer?.currentPosition ?: 0
        } catch (t: Throwable) {
            0
        }
        if (currentPosition > 3000) {
            seekTo(0L)
            return
        }

        val current = _state.value.currentSong
        val prevSong: Song = if (_state.value.shuffleEnabled) {
            if (shuffledIndices.isEmpty()) updateShuffleList()
            if (shuffledIndices.isEmpty()) {
                playlist.first()
            } else {
                currentShufflePointer = if (currentShufflePointer - 1 < 0) {
                    shuffledIndices.size - 1
                } else {
                    currentShufflePointer - 1
                }
                val targetIdx = shuffledIndices.getOrElse(currentShufflePointer) { 0 }
                playlist.getOrElse(targetIdx) { playlist.first() }
            }
        } else {
            val currentIdx = playlist.indexOfFirst { it.id == current?.id }
            val prevIdx = if (currentIdx in playlist.indices) {
                if (currentIdx - 1 < 0) playlist.size - 1 else currentIdx - 1
            } else {
                0
            }
            playlist.getOrElse(prevIdx) { playlist.first() }
        }
        playSong(prevSong)
    }

    fun skipToNext() {
        playNext()
    }

    fun skipToPrevious() {
        playPrevious()
    }

    fun toggleShuffle() {
        _state.update {
            val newShuffle = !it.shuffleEnabled
            if (newShuffle) updateShuffleList()
            it.copy(shuffleEnabled = newShuffle)
        }
        prefsManager.saveShuffle(_state.value.shuffleEnabled)
        notifyServiceState()
    }

    fun cycleRepeatMode() {
        _state.update {
            it.copy(repeatMode = it.repeatMode.next())
        }
        prefsManager.saveRepeat(_state.value.repeatMode.name)
        notifyServiceState()
    }

    fun toggleFavorite(songId: Long): Boolean {
        var isFav = false
        _state.update {
            val favs = it.favoriteIds.toMutableSet()
            if (favs.contains(songId)) {
                favs.remove(songId)
                isFav = false
            } else {
                favs.add(songId)
                isFav = true
            }
            it.copy(favoriteIds = favs)
        }
        prefsManager.saveFavorites(_state.value.favoriteIds)
        return isFav
    }

    fun clearAllFavorites() {
        _state.update { it.copy(favoriteIds = emptySet()) }
    }

    fun play() {
        scope.launch(Dispatchers.Main.immediate) {
            val current = _state.value.currentSong ?: playlist.firstOrNull() ?: return@launch
            val player = mediaPlayer
            if (player == null) {
                isUserInitiatedPlay = true
                prepareAndSetup(current, autoPlay = true)
                return@launch
            }
            try {
                if (!player.isPlaying) {
                    player.start()
                }
                applyPlaybackParams()
                val curPan = if (_state.value.autoRotateEnabled) {
                    _state.value.currentPanValue * _state.value.stereoWidth
                } else {
                    0.0f
                }
                applyVolumePan(curPan)
                _state.update { it.copy(isPlaying = true) }
                startProgressTracker()
                VolumeOverlayManager.getInstance(context).onPlaybackStateChanged(true)
                notifyServiceState(song = _state.value.currentSong ?: current, isPlaying = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start player", e)
            }
        }
    }

    fun pause() {
        scope.launch(Dispatchers.Main.immediate) {
            val player = mediaPlayer ?: return@launch
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                _state.update { it.copy(isPlaying = false) }
                stopProgressTracker()
                saveCurrentPlaybackState()
                VolumeOverlayManager.getInstance(context).onPlaybackStateChanged(false)
                notifyServiceState(song = _state.value.currentSong, isPlaying = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause player", e)
            }
        }
    }

    fun setAppVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _state.update { it.copy(appVolume = clamped) }
        applyVolumePan(pan = _state.value.currentPanValue, immediate = true)
    }

    private fun notifyServiceState(
        song: Song? = _state.value.currentSong,
        isPlaying: Boolean = _state.value.isPlaying,
        pos: Long = _state.value.currentPositionMs,
        dur: Long = _state.value.durationMs
    ) {
        if (song != null) {
            if (isPlaying || MediaPlaybackService.isServiceRunning) {
                MediaPlaybackService.startOrUpdate(
                    context = context,
                    song = song,
                    isPlaying = isPlaying,
                    positionMs = pos,
                    durationMs = dur
                )
            }
        }
    }

    // --- Fast-Forward (2x Speed) & Continuous Rewind (Gesture Support) ---

    /**
     * Sustained 2x Fast-Forward:
     * Increases playback speed to 2.0x while the Next button is held down.
     */
    fun startFastForward() {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                rewindJob?.cancel()
                rewindJob = null
                _state.update { it.copy(isFastForwarding = true, isRewinding = false) }
                val player = mediaPlayer ?: return@launch
                if (!player.isPlaying) {
                    player.start()
                    _state.update { it.copy(isPlaying = true) }
                    startProgressTracker()
                    notifyServiceState(isPlaying = true)
                }
                val params = try {
                    player.playbackParams
                } catch (e: Throwable) {
                    PlaybackParams()
                }
                params.speed = 2.0f
                player.playbackParams = params
            } catch (t: Throwable) {
                Log.e(TAG, "startFastForward error: ${t.message}")
            }
        }
    }

    /**
     * Release Fast-Forward:
     * Instantly returns playback speed to user's set playback speed and continues playback.
     */
    fun stopFastForward() {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                _state.update { it.copy(isFastForwarding = false) }
                val player = mediaPlayer ?: return@launch
                val params = try {
                    player.playbackParams
                } catch (e: Throwable) {
                    PlaybackParams()
                }
                params.speed = _state.value.playbackSpeed
                params.pitch = _state.value.playbackPitch
                player.playbackParams = params
                if (!player.isPlaying && _state.value.currentSong != null) {
                    player.start()
                    _state.update { it.copy(isPlaying = true) }
                    startProgressTracker()
                    notifyServiceState(isPlaying = true)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "stopFastForward error: ${t.message}")
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _state.update { it.copy(playbackSpeed = clamped) }
        applyPlaybackParams()
    }

    fun setPlaybackPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackPitch = clamped) }
        applyPlaybackParams()
    }

    fun applyPlaybackParams() {
        try {
            val player = mediaPlayer ?: return
            if (!player.isPlaying && !_state.value.isPlaying) {
                return
            }
            val current = _state.value
            val params = try {
                player.playbackParams
            } catch (e: Throwable) {
                PlaybackParams()
            }
            val targetSpeed = if (current.isFastForwarding) 2.0f else current.playbackSpeed
            params.speed = targetSpeed
            params.pitch = current.playbackPitch
            player.playbackParams = params
            Log.d(TAG, "Applied playbackParams: speed=$targetSpeed, pitch=${current.playbackPitch}")
        } catch (t: Throwable) {
            Log.w(TAG, "applyPlaybackParams error: ${t.message}")
        }
    }

    /**
     * Sustained Continuous Rewind:
     * Rapidly seeks backward in intervals while Previous button is held down.
     */
    fun startRewind() {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                rewindJob?.cancel()
                _state.update { it.copy(isRewinding = true, isFastForwarding = false) }
                val player = mediaPlayer ?: return@launch

                // Ensure normal playback speed
                try {
                    val params = player.playbackParams
                    if (params.speed != 1.0f) {
                        params.speed = 1.0f
                        player.playbackParams = params
                    }
                } catch (e: Throwable) {}

                rewindJob = scope.launch(Dispatchers.Main.immediate) {
                    while (isActive) {
                        val p = mediaPlayer
                        if (p != null) {
                            val cur = try { p.currentPosition.toLong() } catch (e: Throwable) { _state.value.currentPositionMs }
                            // Step backward by 1500ms every 120ms (~12.5x rewind rate)
                            val target = (cur - 1500L).coerceAtLeast(0L)
                            try {
                                p.seekTo(target.toInt())
                            } catch (e: Throwable) {}
                            _state.update { it.copy(currentPositionMs = target) }
                            if (target <= 0L) {
                                break
                            }
                        }
                        delay(120L)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "startRewind error: ${t.message}")
            }
        }
    }

    /**
     * Release Rewind:
     * Stops continuous rewind and resumes normal playback at 1.0x speed.
     */
    fun stopRewind() {
        scope.launch(Dispatchers.Main.immediate) {
            try {
                rewindJob?.cancel()
                rewindJob = null
                _state.update { it.copy(isRewinding = false) }
                val player = mediaPlayer ?: return@launch
                try {
                    val params = player.playbackParams
                    if (params.speed != 1.0f) {
                        params.speed = 1.0f
                        player.playbackParams = params
                    }
                } catch (e: Throwable) {}
                if (!player.isPlaying && _state.value.currentSong != null) {
                    player.start()
                    _state.update { it.copy(isPlaying = true) }
                    startProgressTracker()
                    notifyServiceState(isPlaying = true)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "stopRewind error: ${t.message}")
            }
        }
    }

    private fun handleTrackCompletion() {
        rewindJob?.cancel()
        rewindJob = null
        _state.update { it.copy(isFastForwarding = false, isRewinding = false) }
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                try {
                    mediaPlayer?.start()
                    _state.update { it.copy(isPlaying = true, playSessionId = it.playSessionId + 1) }
                    notifyServiceState(isPlaying = true)
                } catch (t: Throwable) {}
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val current = _state.value.currentSong
                val currentIndex = playlist.indexOfFirst { it.id == current?.id }
                if (!_state.value.shuffleEnabled && currentIndex == playlist.size - 1) {
                    _state.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                } else {
                    playNext()
                }
            }
        }
    }

    // --- 3D SURROUND & SPATIAL ENGINE ---

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAttachedAudioSessionId == audioSessionId && (virtualizer != null || bassBoost != null || hardwareEqualizer != null || dynamicsProcessing != null || loudnessEnhancer != null)) {
            // Already safely attached to active audioSessionId. DO NOT recreate session or tear down playback!
            return
        }
        currentAttachedAudioSessionId = audioSessionId
        releaseEffects()

        // 1. Hardware 3D Virtualizer Surround (Priority 0 is standard Android user-space priority)
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.virtualizerEnabled || _state.value.equalizerState.isEnabled
                if (strengthSupported) {
                    val str = if (_state.value.virtualizerEnabled) _state.value.virtualizerStrength else 0
                    setStrength(str.toShort())
                }
                try {
                    val mode = if (_state.value.virtualizerMode == "External Speakers") {
                        Virtualizer.VIRTUALIZATION_MODE_TRANSAURAL
                    } else {
                        Virtualizer.VIRTUALIZATION_MODE_BINAURAL
                    }
                    forceVirtualizationMode(mode)
                } catch (t: Throwable) {}
            }
            Log.d(TAG, "Hardware Virtualizer attached to session $audioSessionId")
        } catch (t: Throwable) {
            Log.w(TAG, "Virtualizer effect not supported: ${t.message}")
            virtualizer = null
        }

        // 2. Hardware BassBoost (Priority 0)
        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                val effectiveBass = if (_state.value.dpsEnabled) {
                    val mult = when (_state.value.dpsProfile) {
                        DpsProfile.MOVIE -> 1.25f
                        DpsProfile.GAMING -> 1.10f
                        DpsProfile.MUSIC -> 1.0f
                        DpsProfile.VOICE -> 0.35f
                    }
                    (_state.value.dpsBassBoost * 10f * mult).toInt().coerceIn(0, 1000)
                } else if (_state.value.bassBoostEnabled) {
                    _state.value.bassBoostStrength
                } else 0
                val shouldEnable = _state.value.dpsEnabled || _state.value.bassBoostEnabled || _state.value.equalizerState.isEnabled
                enabled = shouldEnable
                if (strengthSupported && shouldEnable) {
                    setStrength(effectiveBass.toShort())
                }
            }
            Log.d(TAG, "Hardware BassBoost attached to session $audioSessionId")
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost effect not supported: ${t.message}")
            bassBoost = null
        }

        // 3. Hardware Dual-Engine Reverb (Acoustic Environment)
        val currentEnv = _state.value.spatialEnvironment
        val isAcousticActive = _state.value.reverbMasterEnabled && currentEnv != SpatialEnvironment.STUDIO_DRY
        applyAcousticEnvironment(mapEnvironmentToPresetIndex(currentEnv), isAcousticActive)

        // 4. Hardware Equalizer (Priority 0)
        try {
            hardwareEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.equalizerState.isEnabled || _state.value.dpsEnabled
            }
            Log.d(TAG, "Hardware Equalizer attached to session $audioSessionId with ${hardwareEqualizer?.numberOfBands} bands")
        } catch (t: Throwable) {
            Log.w(TAG, "Hardware Equalizer not supported: ${t.message}")
            hardwareEqualizer = null
        }

        // 5. Hardware LoudnessEnhancer (DPS Engine)
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(_state.value.dpsProfile.loudnessTarget)
                enabled = _state.value.dpsEnabled
            }
            Log.d(TAG, "Hardware LoudnessEnhancer attached to session $audioSessionId")
        } catch (t: Throwable) {
            Log.w(TAG, "Hardware LoudnessEnhancer not supported: ${t.message}")
            loudnessEnhancer = null
        }

        // 6. Hardware DynamicsProcessing (DPS Multi-band Compression & Headroom Limiter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val channelCount = 2
                val configBuilder = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    channelCount,
                    true, // pre-eq
                    4,
                    true, // multi-band compression
                    4,
                    true, // post-eq
                    4,
                    true  // soft limiter
                )
                // Soft limiter (-0.5 dB ceiling) inside DPS audio chain to prevent clipping/overflow
                val limiter = DynamicsProcessing.Limiter(
                    true, // inUse
                    true, // enabled
                    0,    // linkGroup
                    1.0f, // attackTimeMs
                    50.0f,// releaseTimeMs
                    10.0f,// ratio
                    -0.5f,// thresholdDb (-0.5 dB ceiling)
                    0.0f  // postGainDb
                )
                for (ch in 0 until channelCount) {
                    configBuilder.setLimiterByChannelIndex(ch, limiter)
                }
                val config = configBuilder.build()
                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                    enabled = _state.value.dpsEnabled
                }
                Log.d(TAG, "Hardware DynamicsProcessing attached to session $audioSessionId with -0.5dB limiter ceiling")
            } catch (e: Exception) {
                Log.e("DPS", "Effect init failed", e)
                dynamicsProcessing = null
            }
        }
    }

    fun mapEnvironmentToPresetIndex(env: SpatialEnvironment): Short {
        return when (env) {
            SpatialEnvironment.STUDIO_DRY -> 0.toShort()
            SpatialEnvironment.ACOUSTIC_ROOM -> 0.toShort()
            SpatialEnvironment.LIVE_STAGE -> 0.toShort()
            SpatialEnvironment.CONCERT_HALL -> 1.toShort()
            SpatialEnvironment.ECHO_CHAMBER -> 1.toShort()
            SpatialEnvironment.GREAT_HALL -> 2.toShort()
            SpatialEnvironment.MEGA_STADIUM -> 2.toShort()
        }
    }

    /**
     * Dual-Engine Reverb architecture:
     * Priority Strategy 1: EnvironmentalReverb directly on active audioSessionId.
     * Strategy 2: Fallback to PresetReverb auxiliary routing on Session 0.
     */
    fun applyAcousticEnvironment(presetIndex: Short, isEnabled: Boolean) {
        if (lastAcousticPreset == presetIndex && lastAcousticActive == isEnabled) return
        lastAcousticPreset = presetIndex
        lastAcousticActive = isEnabled

        val player = mediaPlayer ?: return
        val session = try { player.audioSessionId } catch (e: Exception) { 0 }
        if (session <= 0) return

        scope.launch(Dispatchers.Default) {
            val isAutoRotating = _state.value.autoRotateEnabled
            // Only duck when not in active 360 rotation to keep panning ticker smooth
            if (!isAutoRotating) {
                withContext(Dispatchers.Main.immediate) {
                    try { player.setVolume(0.7f, 0.7f) } catch (_: Exception) {}
                }
                delay(10)
            }

            try {
                if (presetReverb == null) {
                    try {
                        presetReverb = PresetReverb(0, session)
                    } catch (e: Exception) {
                        presetReverb = PresetReverb(0, 0)
                    }
                }
                val reverbPreset: Short = when (presetIndex) {
                    0.toShort() -> PresetReverb.PRESET_SMALLROOM
                    1.toShort() -> PresetReverb.PRESET_LARGEHALL
                    2.toShort() -> PresetReverb.PRESET_PLATE
                    3.toShort() -> PresetReverb.PRESET_MEDIUMHALL
                    else -> PresetReverb.PRESET_MEDIUMHALL
                }
                presetReverb?.preset = reverbPreset
                presetReverb?.enabled = isEnabled

                val sendLevel = if (isEnabled) {
                    _state.value.reverbWetDryMix.coerceIn(0.1f, 1.0f)
                } else {
                    0.0f
                }
                withContext(Dispatchers.Main.immediate) {
                    try { player.setAuxEffectSendLevel(sendLevel) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reverb update failed safely: ${e.message}")
            }

            if (!isAutoRotating) {
                delay(10)
                withContext(Dispatchers.Main.immediate) {
                    try {
                        val currentPan = if (_state.value.manualPan != 0f) {
                            _state.value.manualPan
                        } else {
                            0.0f
                        }
                        applyVolumePan(currentPan)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun mapEnvironmentToPreset(env: SpatialEnvironment): Short {
        return when (env) {
            SpatialEnvironment.STUDIO_DRY -> PresetReverb.PRESET_NONE
            SpatialEnvironment.ACOUSTIC_ROOM -> PresetReverb.PRESET_SMALLROOM
            SpatialEnvironment.LIVE_STAGE -> PresetReverb.PRESET_PLATE
            SpatialEnvironment.CONCERT_HALL -> PresetReverb.PRESET_MEDIUMHALL
            SpatialEnvironment.GREAT_HALL -> PresetReverb.PRESET_LARGEHALL
            SpatialEnvironment.MEGA_STADIUM -> PresetReverb.PRESET_LARGEROOM
            SpatialEnvironment.ECHO_CHAMBER -> PresetReverb.PRESET_MEDIUMROOM
        }
    }

    private fun mapEnvironmentToAuxSendLevel(env: SpatialEnvironment): Float {
        return when (env) {
            SpatialEnvironment.STUDIO_DRY -> 0.0f
            SpatialEnvironment.ACOUSTIC_ROOM -> 0.65f
            SpatialEnvironment.LIVE_STAGE -> 0.80f
            SpatialEnvironment.CONCERT_HALL -> 0.92f
            SpatialEnvironment.GREAT_HALL -> 1.0f
            SpatialEnvironment.MEGA_STADIUM -> 1.0f
            SpatialEnvironment.ECHO_CHAMBER -> 0.85f
        }
    }

    private fun applySpatialSettings() {
        if (currentAttachedAudioSessionId <= 0 && mediaPlayer != null) {
            val sessionId = try { mediaPlayer?.audioSessionId ?: 0 } catch (t: Throwable) { 0 }
            if (sessionId > 0) {
                initAudioEffects(sessionId)
            }
        }

        val s = _state.value
        try {
            virtualizer?.let {
                it.enabled = s.virtualizerEnabled
                if (it.strengthSupported && s.virtualizerEnabled) {
                    it.setStrength(s.virtualizerStrength.toShort())
                }
                try {
                    val mode = if (s.virtualizerMode == "External Speakers") {
                        Virtualizer.VIRTUALIZATION_MODE_TRANSAURAL
                    } else {
                        Virtualizer.VIRTUALIZATION_MODE_BINAURAL
                    }
                    it.forceVirtualizationMode(mode)
                } catch (t: Throwable) {}
            }

            bassBoost?.let {
                val effectiveBass = if (s.dpsEnabled) {
                    s.dpsProfile.bassGain.toInt()
                } else if (s.bassBoostEnabled) {
                    s.bassBoostStrength
                } else 0
                val shouldEnable = s.dpsEnabled || s.bassBoostEnabled
                it.enabled = shouldEnable
                if (it.strengthSupported && shouldEnable) {
                    it.setStrength(effectiveBass.toShort())
                }
            }

            val isReverbActive = s.reverbMasterEnabled && s.spatialEnvironment != SpatialEnvironment.STUDIO_DRY
            if (isReverbActive || lastAcousticActive) {
                applyAcousticEnvironment(mapEnvironmentToPresetIndex(s.spatialEnvironment), isReverbActive)
                if (isReverbActive) {
                    environmentalReverb?.let {
                        try {
                            it.decayTime = (s.spatialEnvironment.decaySeconds * 1000f).toInt().coerceIn(100, 20000)
                        } catch (t: Throwable) {}
                    }
                    presetReverb?.let {
                        try {
                            mediaPlayer?.setAuxEffectSendLevel(s.spatialEnvironment.wetMix.coerceIn(0.0f, 1.0f))
                        } catch (t: Throwable) {}
                    }
                }
            }

            loudnessEnhancer?.let {
                it.enabled = s.dpsEnabled
                if (s.dpsEnabled) {
                    it.setTargetGain(s.dpsProfile.loudnessTarget)
                }
            }

            try {
                dynamicsProcessing?.enabled = s.dpsEnabled
            } catch (e: Exception) {
                Log.e("DPS", "Effect init failed", e)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not apply hardware effects: ${t.message}")
        }

        // Apply volume pan and preAmpFactor consistently
        val eqState = s.equalizerState
        val preAmpFactor = if (eqState.isEnabled && eqState.preAmpGainDb != 0f) {
            10.0.pow(eqState.preAmpGainDb.toDouble() / 20.0).toFloat()
        } else 1.0f

        if (s.autoRotateEnabled) {
            val curPan = s.currentPanValue
            val virtualizerWidening = if (s.virtualizerEnabled) {
                1.0f + (s.virtualizerStrength / 1000.0f) * 0.40f
            } else {
                1.0f
            }
            val effectivePan = (curPan * s.stereoWidth * virtualizerWidening).coerceIn(-1.0f, 1.0f)
            applyVolumePan(pan = effectivePan, depth = 1.0f, immediate = false, factor = preAmpFactor)
        } else if (s.manualPan != 0.0f) {
            applyVolumePan(pan = s.manualPan.coerceIn(-1.0f, 1.0f), depth = 1.0f, immediate = false, factor = preAmpFactor)
        } else {
            applyVolumePan(pan = 0.0f, depth = 1.0f, immediate = true, factor = preAmpFactor)
        }
    }

    // --- 13-BAND EQUALIZER ENGINE (HARDWARE + HYBRID DSP) ---

    private fun applyEqualizerBands() {
        scope.launch(Dispatchers.Default) {
            applyEqualizerBandsInternal()
        }
    }

    private fun applyEqualizerBandsInternal() {
        if (currentAttachedAudioSessionId <= 0 && mediaPlayer != null) {
            val sessionId = try { mediaPlayer?.audioSessionId ?: 0 } catch (t: Throwable) { 0 }
            if (sessionId > 0) {
                initAudioEffects(sessionId)
            }
        }

        val eqState = _state.value.equalizerState

        // 1. Hardware Equalizer multi-band mapping
        val eq = hardwareEqualizer
        if (eq != null) {
            try {
                val isEqActive = eqState.isEnabled || _state.value.dpsEnabled
                eq.enabled = isEqActive
                if (isEqActive) {
                    val numBands = eq.numberOfBands.toInt()
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]

                    for (b in 0 until numBands) {
                        val centerFreqMhz = eq.getCenterFreq(b.toShort())
                        val centerFreqHz = (centerFreqMhz / 1000).coerceAtLeast(20)
                        val freqRange = try { eq.getBandFreqRange(b.toShort()) } catch (t: Throwable) { null }
                        val minHz = if (freqRange != null) freqRange[0] / 1000 else (centerFreqHz / 2)
                        val maxHz = if (freqRange != null) freqRange[1] / 1000 else (centerFreqHz * 2)

                        val matchingIndices = EqualizerFrequencies.FREQ_HZ.mapIndexedNotNull { idx, freqHz ->
                            if (freqHz in minHz..maxHz) idx else null
                        }

                        var bandGainDb = if (matchingIndices.isNotEmpty()) {
                            matchingIndices.map { eqState.bandGains.getOrElse(it) { 0f } }.average().toFloat()
                        } else {
                            val closestIdx = EqualizerFrequencies.FREQ_HZ.indices.minByOrNull {
                                kotlin.math.abs(EqualizerFrequencies.FREQ_HZ[it] - centerFreqHz)
                            } ?: 0
                            eqState.bandGains.getOrElse(closestIdx) { 0f }
                        }

                        if (_state.value.dpsEnabled) {
                            val profile = _state.value.dpsProfile
                            val clarityBoost = profile.clarityBoostDb.toFloat()
                            when (profile) {
                                DpsProfile.MUSIC -> {
                                    if (centerFreqHz >= 4000) bandGainDb += clarityBoost
                                    if (centerFreqHz <= 150) bandGainDb += 2.0f
                                }
                                DpsProfile.MOVIE -> {
                                    if (centerFreqHz in 1000..4000) bandGainDb += clarityBoost
                                    if (centerFreqHz <= 150) bandGainDb += 3.5f
                                }
                                DpsProfile.VOICE -> {
                                    if (centerFreqHz in 500..5000) bandGainDb += clarityBoost
                                    if (centerFreqHz <= 150) bandGainDb -= 2.0f
                                }
                                DpsProfile.GAMING -> {
                                    if (centerFreqHz in 1500..8000) bandGainDb += clarityBoost
                                    if (centerFreqHz <= 150) bandGainDb += 2.5f
                                }
                            }
                        }

                        // 1 dB = 100 millibels in Android OpenSL/Equalizer API
                        val targetMb = (bandGainDb * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                        eq.setBandLevel(b.toShort(), targetMb.toShort())
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "applyEqualizerBands hardware error: ${t.message}")
            }
        }

        // 2. Hybrid DSP Audio Enhancer (Dynamic Bass and Treble enhancement)
        try {
            val bassBoostFromEq = if (eqState.isEnabled) {
                val bassAvg = (0..3).map { eqState.bandGains.getOrElse(it) { 0f } }.average().toFloat()
                if (bassAvg > 0f) (bassAvg / 12f * 800f).toInt() else 0
            } else 0
            val dpsBassVal = if (_state.value.dpsEnabled) {
                val mult = when (_state.value.dpsProfile) {
                    DpsProfile.MOVIE -> 1.25f
                    DpsProfile.GAMING -> 1.10f
                    DpsProfile.MUSIC -> 1.0f
                    DpsProfile.VOICE -> 0.35f
                }
                (_state.value.dpsBassBoost * 10f * mult).toInt().coerceIn(0, 1000)
            } else 0
            val effectiveBass = if (_state.value.bassBoostEnabled) {
                maxOf(_state.value.bassBoostStrength, maxOf(bassBoostFromEq, dpsBassVal))
            } else maxOf(bassBoostFromEq, dpsBassVal)

            bassBoost?.let {
                val shouldEnable = effectiveBass > 0 || _state.value.bassBoostEnabled || _state.value.dpsEnabled
                it.enabled = shouldEnable
                if (it.strengthSupported && shouldEnable) {
                    it.setStrength(effectiveBass.coerceIn(0, 1000).toShort())
                }
            }

            val trebleBoostFromEq = if (eqState.isEnabled) {
                val trebleAvg = (8..12).map { eqState.bandGains.getOrElse(it) { 0f } }.average().toFloat()
                if (trebleAvg > 0f) (trebleAvg / 12f * 800f).toInt() else 0
            } else 0
            val dpsClarityBoost = if (_state.value.dpsEnabled) {
                (_state.value.dpsClarity / 100f * 400f).toInt()
            } else 0
            val effectiveVirt = if (_state.value.virtualizerEnabled) {
                maxOf(_state.value.virtualizerStrength, trebleBoostFromEq + dpsClarityBoost)
            } else (trebleBoostFromEq + dpsClarityBoost)

            virtualizer?.let {
                val shouldEnable = effectiveVirt > 0 || _state.value.virtualizerEnabled
                it.enabled = shouldEnable
                if (it.strengthSupported && shouldEnable) {
                    it.setStrength(effectiveVirt.coerceIn(0, 1000).toShort())
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "applyEqualizerBands hybrid DSP error: ${t.message}")
        }

        // Apply updated volume pan with pre-amp factor (only when auto rotate is not active)
        if (!_state.value.autoRotateEnabled) {
            val preAmpFactor = if (eqState.isEnabled && eqState.preAmpGainDb != 0f) {
                10.0.pow(eqState.preAmpGainDb.toDouble() / 20.0).toFloat()
            } else 1.0f

            val curPan = if (_state.value.manualPan != 0f) {
                _state.value.manualPan
            } else {
                0.0f
            }
            applyVolumePan(curPan, factor = preAmpFactor)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _state.update {
            val updated = it.equalizerState.copy(isEnabled = enabled)
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    fun setEqualizerPreset(presetName: String) {
        val preset = EqualizerPresets.PRESETS.find { it.name.equals(presetName, ignoreCase = true) }
            ?: EqualizerPresets.PRESETS.first()
        _state.update {
            val updated = it.equalizerState.copy(
                selectedPresetName = preset.name,
                bandGains = preset.gains
            )
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
        _state.update {
            val newGains = it.equalizerState.bandGains.toMutableList()
            if (bandIndex in newGains.indices) {
                newGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
            }
            val updated = it.equalizerState.copy(
                selectedPresetName = "Custom",
                bandGains = newGains
            )
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    fun setEqualizerPreAmp(preAmpDb: Float) {
        _state.update {
            val updated = it.equalizerState.copy(preAmpGainDb = preAmpDb.coerceIn(-6f, 6f))
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    fun setEqualizerBassPunch(punch: Float) {
        _state.update {
            val updated = it.equalizerState.copy(bassPunch = punch.coerceIn(0f, 1f))
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    fun setEqualizerTrebleSparkle(sparkle: Float) {
        _state.update {
            val updated = it.equalizerState.copy(trebleSparkle = sparkle.coerceIn(0f, 1f))
            it.copy(equalizerState = updated)
        }
        applyEqualizerBands()
    }

    /**
     * When auto rotate is toggled off, reset orbit angle and smoothly de-zipper to dead center.
     */
    fun setAutoRotateEnabled(enabled: Boolean) {
        if (!enabled) {
            orbitAngleRad = 0.0f
            _state.update {
                it.copy(
                    autoRotateEnabled = false,
                    currentAngleDegrees = 0.0f,
                    currentPanValue = 0.0f,
                    manualPan = 0.0f
                )
            }
            applyVolumePan(pan = 0.0f, depth = 1.0f, immediate = true)
        } else {
            _state.update { it.copy(autoRotateEnabled = true) }
        }
    }

    fun setAutoRotateSpeed(speedSec: Float) {
        _state.update { it.copy(autoRotateSpeedSec = speedSec.coerceIn(0.5f, 20.0f)) }
    }

    fun setRandomRotationEnabled(enabled: Boolean) {
        _state.update { it.copy(randomRotationEnabled = enabled) }
        if (enabled && !_state.value.autoRotateEnabled) {
            _state.update { it.copy(autoRotateEnabled = true) }
        }
    }

    fun setAutoRotateDirection(dir: AutoRotateDirection) {
        _state.update { it.copy(autoRotateDirection = dir) }
    }

    fun setStereoWidth(width: Float) {
        _state.update { it.copy(stereoWidth = width.coerceIn(0.2f, 1.0f)) }
    }

    fun setManualPan(pan: Float) {
        val clamped = pan.coerceIn(-1.0f, 1.0f)
        _state.update { it.copy(manualPan = clamped) }
        if (!_state.value.autoRotateEnabled) {
            val s = _state.value
            val virtualizerWidening = if (s.virtualizerEnabled) {
                1.0f + (s.virtualizerStrength / 1000.0f) * 0.40f
            } else {
                0.88f
            }
            val envDepthFactor = when (s.spatialEnvironment) {
                SpatialEnvironment.STUDIO_DRY -> 1.0f
                SpatialEnvironment.ACOUSTIC_ROOM -> 1.03f
                SpatialEnvironment.LIVE_STAGE -> 1.06f
                SpatialEnvironment.CONCERT_HALL -> 1.10f
                SpatialEnvironment.GREAT_HALL -> 1.15f
                SpatialEnvironment.MEGA_STADIUM -> 1.22f
                SpatialEnvironment.ECHO_CHAMBER -> 1.18f
            }
            val effectivePan = (clamped * s.stereoWidth * (s.binauralWidener / 1.25f) * virtualizerWidening)
                .coerceIn(-1.0f, 1.0f)
            applyVolumePan(pan = effectivePan, depth = envDepthFactor, immediate = false)
        }
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        _state.update { it.copy(virtualizerEnabled = enabled, virtualizerStrength = strength.coerceIn(0, 1000)) }
        applySpatialSettings()
    }

    fun setVirtualizerMaster(enabled: Boolean, strength: Int = _state.value.virtualizerStrength) {
        _state.update { it.copy(virtualizerEnabled = enabled, virtualizerStrength = strength.coerceIn(0, 1000)) }
        applySpatialSettings()
    }

    fun setVirtualizerMode(mode: String) {
        _state.update { it.copy(virtualizerMode = mode) }
        applySpatialSettings()
    }

    fun toggleVirtualizer(): Boolean {
        val newState = !_state.value.virtualizerEnabled
        setVirtualizer(newState, _state.value.virtualizerStrength)
        return newState
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        _state.update { it.copy(bassBoostEnabled = enabled, bassBoostStrength = strength) }
        applySpatialSettings()
    }

    fun setSpatialEnvironment(env: SpatialEnvironment) {
        _state.update { it.copy(spatialEnvironment = env) }
        applySpatialSettings()
    }

    // --- ACOUSTIC ENVIRONMENT (REVERB) CONTROLS ---
    fun setAcousticEnvironment(env: SpatialEnvironment) {
        val masterEnabled = (env != SpatialEnvironment.STUDIO_DRY)
        _state.update {
            it.copy(
                spatialEnvironment = env,
                reverbMasterEnabled = masterEnabled,
                reverbWetDryMix = if (masterEnabled) env.wetMix else 0.0f,
                reverbDecayTime = env.decaySeconds
            )
        }
        applySpatialSettings()
    }

    fun setReverbStrength(strength: Float) {
        val clamped = strength.coerceIn(0.0f, 1.0f)
        val masterEnabled = clamped > 0.01f && _state.value.spatialEnvironment != SpatialEnvironment.STUDIO_DRY
        _state.update {
            it.copy(
                reverbWetDryMix = clamped,
                reverbMasterEnabled = masterEnabled
            )
        }
        applySpatialSettings()
    }

    fun setReverbMaster(enabled: Boolean) {
        _state.update { it.copy(reverbMasterEnabled = enabled) }
        applySpatialSettings()
    }

    fun setReverbWetDryMix(mix: Float) {
        val clamped = mix.coerceIn(0.0f, 1.0f)
        _state.update { it.copy(reverbWetDryMix = clamped) }
        applySpatialSettings()
    }

    fun setReverbDecayTime(decay: Float) {
        val clamped = decay.coerceIn(0.1f, 5.0f)
        _state.update { it.copy(reverbDecayTime = clamped) }
        applySpatialSettings()
    }

    fun setReverbPreset(env: SpatialEnvironment) {
        _state.update {
            it.copy(
                spatialEnvironment = env,
                reverbWetDryMix = env.wetMix,
                reverbDecayTime = env.decaySeconds
            )
        }
        applySpatialSettings()
    }

    // --- 3D 360° ROTATION CONTROLS ---
    fun setRotationSpeedMultiplier(multiplier: Float) {
        _state.update { it.copy(rotationSpeedMultiplier = multiplier.coerceIn(0.1f, 5.0f)) }
    }

    // --- DPS AUDIO ENGINE CONTROLS ---
    fun setDpsMaster(enabled: Boolean) {
        _state.update { it.copy(dpsEnabled = enabled) }
        scope.launch(Dispatchers.Default) {
            try {
                dynamicsProcessing?.enabled = enabled
            } catch (e: Exception) {
                Log.e("DPS", "Effect init failed", e)
            }
            try {
                loudnessEnhancer?.let {
                    it.enabled = enabled
                    if (enabled) {
                        it.setTargetGain(_state.value.dpsProfile.loudnessTarget)
                    }
                }
            } catch (e: Exception) {
                Log.e("DPS", "LoudnessEnhancer update failed", e)
            }
            applyEqualizerBandsInternal()
        }
    }

    fun setDpsProfile(profile: DpsProfile) {
        _state.update { it.copy(dpsProfile = profile) }
        scope.launch(Dispatchers.Default) {
            try {
                loudnessEnhancer?.setTargetGain(profile.loudnessTarget)
            } catch (t: Throwable) {}
            applyEqualizerBandsInternal()
        }
    }

    fun setDpsBassBoost(boostPercent: Int) {
        val clamped = boostPercent.coerceIn(0, 100)
        _state.update { it.copy(dpsBassBoost = clamped) }
        scope.launch(Dispatchers.Default) {
            applyEqualizerBandsInternal()
        }
    }

    fun setDpsClarity(clarityPercent: Int) {
        val clamped = clarityPercent.coerceIn(0, 100)
        _state.update { it.copy(dpsClarity = clamped) }
        scope.launch(Dispatchers.Default) {
            applyEqualizerBandsInternal()
        }
    }

    fun setSoundstageDistance(distance: String) {
        _state.update { it.copy(soundstageDistance = distance) }
        applySpatialSettings()
    }

    fun setElevation(elevation: String) {
        _state.update { it.copy(elevation = elevation) }
    }

    fun setHeadShadowEnabled(enabled: Boolean) {
        _state.update { it.copy(headShadowEnabled = enabled) }
    }

    fun setBinauralWidener(widener: Float) {
        _state.update { it.copy(binauralWidener = widener.coerceIn(0.5f, 2.0f)) }
    }

    // --- STEREO DELAY / ECHO AUDIO EFFECT MODULE CONTROLS ---

    fun setDelayEnabled(enabled: Boolean) {
        stereoDelayProcessor.isEnabled = enabled
        _state.update { it.copy(delayEnabled = enabled) }
    }

    fun setDelayTimeMs(timeMs: Int) {
        val clamped = timeMs.coerceIn(50, 1000)
        stereoDelayProcessor.delayTimeMs = clamped
        _state.update { it.copy(delayTimeMs = clamped) }
    }

    fun setDelayFeedbackPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 85)
        stereoDelayProcessor.feedbackPercent = clamped
        _state.update { it.copy(delayFeedbackPercent = clamped) }
    }

    fun setDelayMixPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        stereoDelayProcessor.mixPercent = clamped
        _state.update { it.copy(delayMixPercent = clamped) }
    }

    fun resetSpatialEffects() {
        orbitAngleRad = 0.0f
        _state.update {
            it.copy(
                autoRotateEnabled = false,
                manualPan = 0.0f,
                currentAngleDegrees = 0.0f,
                currentPanValue = 0.0f,
                virtualizerEnabled = false,
                virtualizerStrength = 0,
                bassBoostEnabled = false,
                bassBoostStrength = 0,
                spatialEnvironment = SpatialEnvironment.STUDIO_DRY,
                stereoWidth = 1.0f,
                binauralWidener = 1.0f,
                headShadowEnabled = false
            )
        }
        applySpatialSettings()
    }

    fun resetAudioSettings() {
        orbitAngleRad = 0.0f
        _state.update {
            it.copy(
                autoRotateEnabled = false,
                autoRotateSpeedSec = 6.0f,
                autoRotateDirection = AutoRotateDirection.LEFT_TO_RIGHT,
                stereoWidth = 1.0f,
                manualPan = 0.0f,
                currentAngleDegrees = 0.0f,
                currentPanValue = 0.0f,
                virtualizerEnabled = false,
                virtualizerStrength = 0,
                bassBoostEnabled = false,
                bassBoostStrength = 0,
                spatialEnvironment = SpatialEnvironment.STUDIO_DRY,
                soundstageDistance = "MEDIUM",
                elevation = "EYE_LEVEL",
                headShadowEnabled = false,
                binauralWidener = 1.0f,
                equalizerState = EqualizerState()
            )
        }
        applySpatialSettings()
        applyEqualizerBands()
        try {
            mediaPlayer?.setVolume(1.0f, 1.0f)
        } catch (t: Throwable) {}
    }

    fun restoreSavedSettings(
        equalizerState: EqualizerState,
        favorites: Set<Long>,
        shuffle: Boolean = false,
        repeat: RepeatMode = RepeatMode.OFF,
        appVolume: Float = 1.0f
    ) {
        _state.update {
            it.copy(
                equalizerState = equalizerState,
                favoriteIds = favorites,
                shuffleEnabled = shuffle,
                repeatMode = repeat,
                appVolume = appVolume.coerceIn(0f, 1f)
            )
        }
        if (shuffle) updateShuffleList()
        applyEqualizerBands()
        applySpatialSettings()
    }

    /**
     * Continuous Orbit Calculations & Audio Parameter Smoothing Loop:
     * Calculates the panning position smoothly along a continuous angular trajectory
     * (0 to 2π radians) linked to the orbit speed cycle, and executes real-time
     * parameter smoothing (de-zippering) to eliminate clicks and jumps.
     */
    private fun startAutoRotateLoop() {
        autoRotateJob?.cancel()
        autoRotateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    if (isSwitchingTrack || mediaPlayer == null) {
                        delay(30)
                        continue
                    }
                    val player = mediaPlayer ?: run {
                        delay(30)
                        continue
                    }
                    val isPlaying = try {
                        player.isPlaying
                    } catch (t: Throwable) {
                        false
                    }
                    val currentState = _state.value

                    if (currentState.autoRotateEnabled && (isPlaying || currentState.isPlaying)) {
                        // 1. Continuous Orbit Calculations:
                        // Angular frequency omega = 2π / T (radians per second)
                        val speedMult = currentState.rotationSpeedMultiplier.coerceIn(0.1f, 5.0f)
                        val baseSpeed = currentState.autoRotateSpeedSec.coerceAtLeast(0.5f)
                        val speed = (baseSpeed / speedMult).coerceAtLeast(0.2f)
                        val angularVelocityRadPerSec = TWO_PI / speed
                        val deltaRad = angularVelocityRadPerSec * (ORBIT_UPDATE_INTERVAL_MS / 1000.0f)

                        // Progress along continuous angular trajectory [0, 2π) or stochastic random pan [-90°, +90°]
                        val rawPan: Float
                        val frontBack: Float
                        val angleDegrees: Float

                        if (currentState.randomRotationEnabled) {
                            // Stochastic rotation randomly panning between -90° and +90°
                            if (abs(orbitAngleRad - randomTargetAngleRad) < 0.06f || randomTargetTimer <= 0) {
                                val randNorm = kotlin.random.Random.nextFloat()
                                randomTargetAngleRad = (randNorm * Math.PI.toFloat()) - HALF_PI
                                randomTargetTimer = (speed * 18).toInt().coerceAtLeast(10)
                            } else {
                                randomTargetTimer--
                            }

                            val step = (deltaRad * 0.85f).coerceAtLeast(0.012f)
                            if (orbitAngleRad < randomTargetAngleRad) {
                                orbitAngleRad = (orbitAngleRad + step).coerceAtMost(randomTargetAngleRad)
                            } else {
                                orbitAngleRad = (orbitAngleRad - step).coerceAtLeast(randomTargetAngleRad)
                            }

                            rawPan = sin(orbitAngleRad).coerceIn(-1.0f, 1.0f)
                            frontBack = cos(orbitAngleRad).coerceIn(0.0f, 1.0f)
                            angleDegrees = Math.toDegrees(orbitAngleRad.toDouble()).toFloat()
                        } else {
                            // Standard continuous circular 360 orbit
                            orbitAngleRad = if (currentState.autoRotateDirection == AutoRotateDirection.LEFT_TO_RIGHT) {
                                (orbitAngleRad + deltaRad) % TWO_PI
                            } else {
                                val prev = orbitAngleRad - deltaRad
                                if (prev < 0f) prev + TWO_PI else prev
                            }

                            rawPan = sin(orbitAngleRad)
                            frontBack = cos(orbitAngleRad)
                            angleDegrees = (Math.toDegrees(orbitAngleRad.toDouble()).toFloat() % 360.0f + 360.0f) % 360.0f
                        }

                        // Acoustic head shadow / front-back occlusion simulation
                        val depthFactor = if (currentState.headShadowEnabled) {
                            0.82f + (frontBack * 0.18f)
                        } else {
                            1.0f
                        }

                        // Elevation modulation factor
                        val elevationFactor = when (currentState.elevation) {
                            "ABOVE" -> 1.05f
                            "BELOW" -> 0.95f
                            else -> 1.0f
                        }

                        // 3D Virtualizer soundstage widening factor
                        val virtualizerWidening = if (currentState.virtualizerEnabled) {
                            1.0f + (currentState.virtualizerStrength / 1000.0f) * 0.40f
                        } else {
                            0.88f
                        }

                        // Acoustic environment spatial depth / room factor
                        val envDepthFactor = when (currentState.spatialEnvironment) {
                            SpatialEnvironment.STUDIO_DRY -> 1.0f
                            SpatialEnvironment.ACOUSTIC_ROOM -> 1.03f
                            SpatialEnvironment.LIVE_STAGE -> 1.06f
                            SpatialEnvironment.CONCERT_HALL -> 1.10f
                            SpatialEnvironment.GREAT_HALL -> 1.15f
                            SpatialEnvironment.MEGA_STADIUM -> 1.22f
                            SpatialEnvironment.ECHO_CHAMBER -> 1.18f
                        }

                        // Stereo Ping-Pong cross-feed modulation (alternating delay taps between earphones)
                        val curPosMs = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Throwable) { 0L }
                        val pingPongOffset = if (currentState.delayEnabled) {
                            stereoDelayProcessor.calculatePingPongPanOffset(curPosMs)
                        } else {
                            0f
                        }

                        val effectivePan = ((rawPan * currentState.stereoWidth * (currentState.binauralWidener / 1.25f) * virtualizerWidening) + pingPongOffset)
                            .coerceIn(-1.0f, 1.0f)

                        // Update UI Radar telemetry
                        _state.update {
                            it.copy(
                                currentAngleDegrees = angleDegrees,
                                currentPanValue = rawPan
                            )
                        }

                        if (isPlaying) {
                            applyVolumePan(
                                pan = effectivePan,
                                depth = depthFactor * elevationFactor * envDepthFactor,
                                immediate = false
                            )
                        }
                    } else if (currentState.delayEnabled && (isPlaying || currentState.isPlaying)) {
                        // When auto-rotate is off, but Stereo Delay is enabled:
                        // Maintain alternating ping-pong earphone reflections
                        val curPosMs = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Throwable) { 0L }
                        val pingPongOffset = stereoDelayProcessor.calculatePingPongPanOffset(curPosMs)
                        val effectivePan = (currentState.manualPan + pingPongOffset).coerceIn(-1.0f, 1.0f)
                        if (isPlaying) {
                            applyVolumePan(pan = effectivePan, depth = 1.0f, immediate = false)
                        }
                    }

                    // 2. Audio Parameter Smoothing (De-zippering) execution
                    performDezipperingStep()

                } catch (t: Throwable) {
                    Log.w(TAG, "Error in audio spatial tick: ${t.message}")
                }
                delay(ORBIT_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Constant Power Panning (Equal-Power Sine/Cosine Pan Law):
     * Replaces linear gain scaling with an equal-power law:
     *   gain_L = cos(theta), gain_R = sin(theta)
     * where theta = ((pan + 1) / 2) * (pi / 2) = (pan + 1) * (pi / 4).
     * This guarantees: gain_L^2 + gain_R^2 = cos^2(theta) + sin^2(theta) = 1.0,
     * so total acoustic energy and perceived volume remain strictly uniform
     * across the entire stereo spectrum.
     */
    private fun applyVolumePan(pan: Float, depth: Float = 1.0f, immediate: Boolean = false, factor: Float = 1.0f) {
        if (isSwitchingTrack) return
        val clampedPan = pan.coerceIn(-1.0f, 1.0f)

        // Equal-power Sine/Cosine Pan Law with center normalization:
        val gainL: Float
        val gainR: Float
        if (clampedPan == 0.0f) {
            gainL = 1.0f
            gainR = 1.0f
        } else {
            val theta = ((clampedPan + 1.0f) * 0.5f) * HALF_PI
            val norm = 1.4142135f // sqrt(2) so center is 1.0
            gainL = (cos(theta) * norm).coerceIn(0f, 1f)
            gainR = (sin(theta) * norm).coerceIn(0f, 1f)
        }

        val appVol = _state.value.appVolume
        val finalTargetLeft = (gainL * depth * appVol * factor).coerceIn(0.1f, 1f)
        val finalTargetRight = (gainR * depth * appVol * factor).coerceIn(0.1f, 1f)

        targetLeftGain = finalTargetLeft
        targetRightGain = finalTargetRight

        if (immediate) {
            smoothedLeftGain = finalTargetLeft
            smoothedRightGain = finalTargetRight
            lastAppliedLeftGain = finalTargetLeft
            lastAppliedRightGain = finalTargetRight
            try {
                mediaPlayer?.setVolume(finalTargetLeft, finalTargetRight)
            } catch (t: Throwable) {
                // Player may be in transition
            }
        } else {
            performDezipperingStep()
        }
    }

    /**
     * Audio Parameter Smoothing (De-zippering):
     * Smooths transitions of gain values using exponential interpolation
     * (one-pole lowpass filter) and slew-rate limiting to eliminate buffer-level
     * clicks, pops, and zipper noise during rapid panning or sudden parameter jumps.
     */
    private fun performDezipperingStep(): Boolean {
        val deltaL = (targetLeftGain - smoothedLeftGain).coerceIn(-MAX_GAIN_SLEW_PER_STEP, MAX_GAIN_SLEW_PER_STEP)
        smoothedLeftGain = if (abs(targetLeftGain - smoothedLeftGain) < 0.0008f) {
            targetLeftGain
        } else {
            (smoothedLeftGain + deltaL * DE_ZIPPER_SMOOTHING_ALPHA).coerceIn(0f, 1f)
        }

        val deltaR = (targetRightGain - smoothedRightGain).coerceIn(-MAX_GAIN_SLEW_PER_STEP, MAX_GAIN_SLEW_PER_STEP)
        smoothedRightGain = if (abs(targetRightGain - smoothedRightGain) < 0.0008f) {
            targetRightGain
        } else {
            (smoothedRightGain + deltaR * DE_ZIPPER_SMOOTHING_ALPHA).coerceIn(0f, 1f)
        }

        if (abs(smoothedLeftGain - lastAppliedLeftGain) > 0.001f || abs(smoothedRightGain - lastAppliedRightGain) > 0.001f) {
            try {
                mediaPlayer?.setVolume(smoothedLeftGain, smoothedRightGain)
                lastAppliedLeftGain = smoothedLeftGain
                lastAppliedRightGain = smoothedRightGain
            } catch (t: Throwable) {
                // Player state transition
            }
            return true
        }
        return false
    }

    private fun startProgressTracker() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                try {
                    val player = mediaPlayer
                    val realIsPlaying = try { player?.isPlaying == true } catch (_: Exception) { false }
                    if (_state.value.isPlaying != realIsPlaying) {
                        _state.update { it.copy(isPlaying = realIsPlaying) }
                        notifyServiceState(isPlaying = realIsPlaying)
                    }
                    if (player != null && realIsPlaying) {
                        val pos = player.currentPosition.toLong()
                        val dur = player.duration.toLong().coerceAtLeast(pos)
                        _state.update {
                            it.copy(currentPositionMs = pos, durationMs = dur)
                        }
                    }
                } catch (t: Throwable) {
                    // Ignore transition states
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releaseEffects() {
        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (t: Throwable) {}
        virtualizer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (t: Throwable) {}
        bassBoost = null

        try {
            presetReverb?.enabled = false
            presetReverb?.release()
        } catch (t: Throwable) {}
        presetReverb = null
        isReverbAuxAttached = false

        try {
            environmentalReverb?.enabled = false
            environmentalReverb?.release()
        } catch (t: Throwable) {}
        environmentalReverb = null

        try {
            hardwareEqualizer?.enabled = false
            hardwareEqualizer?.release()
        } catch (t: Throwable) {}
        hardwareEqualizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (t: Throwable) {}
        loudnessEnhancer = null

        try {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
        } catch (t: Throwable) {}
        dynamicsProcessing = null
        currentAttachedAudioSessionId = -1
    }

    fun applyAllEffects() {
        applySpatialSettings()
        applyEqualizerBands()
        applyPlaybackParams()
    }

    fun release() {
        if (instance == this) {
            instance = null
        }
        unregisterAudioBecomingNoisyReceiver()
        try {
            MediaPlaybackService.stop(context)
        } catch (t: Throwable) {}
        progressJob?.cancel()
        autoRotateJob?.cancel()
        rewindJob?.cancel()
        releaseEffects()
        try {
            mediaPlayer?.stop()
        } catch (t: Throwable) {}
        try {
            mediaPlayer?.release()
        } catch (t: Throwable) {}
        mediaPlayer = null
        try {
            scope.cancel()
        } catch (_: Throwable) {}
    }
}
