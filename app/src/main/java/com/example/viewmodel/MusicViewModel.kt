package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioScanner
import com.example.audio.MediaPlaybackService
import com.example.audio.PlayerState
import com.example.audio.VolumeOverlayManager
import com.example.model.AppNaturalTheme
import com.example.model.AppThemePalette
import com.example.model.AppThemePreset
import com.example.model.AutoRotateDirection
import com.example.model.CuratedThemePalettes
import com.example.model.CustomThemeSettings
import com.example.model.DEFAULT_PALETTE
import com.example.model.DpsProfile
import com.example.model.EdgeLightingSettings
import com.example.model.EdgeLightingStyle
import com.example.model.LibraryCategory
import com.example.model.ProgressBarAnimation
import com.example.model.ProgressBarColorPreset
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SongSortOption
import com.example.model.SpatialEnvironment
import com.example.model.VolumeGaugeSettings
import com.example.model.getPaletteById
import com.example.model.getPaletteForPreset
import com.example.storage.SettingsPreferencesManager
import android.media.AudioManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class AppTab(val title: String) {
    ALL_SONGS("Library"),
    NOW_PLAYING("Now Playing"),
    EQUALIZER("Equalizer"),
    SPATIAL_3D("3D Effects"),
    SETTINGS("Settings")
}

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val simpleSongs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val selectedTab: AppTab = AppTab.NOW_PLAYING,
    val activeCategory: LibraryCategory = LibraryCategory.SONGS,
    val categoryEventId: Long = 0L,
    val playerState: PlayerState = PlayerState(),
    val hasStoragePermission: Boolean = false,
    val selectedTheme: AppNaturalTheme = AppNaturalTheme.BLUE,
    val currentThemePalette: AppThemePalette = CuratedThemePalettes.find { it.id == "blue" } ?: CuratedThemePalettes[0],
    val edgeLightingSettings: EdgeLightingSettings = EdgeLightingSettings(),
    val isVisualizerEnabled: Boolean = true,
    val selectedVisualizerEffectId: Int = 1,
    val customVisualizerText: String = "Naasir",
    val showVisualizerText: Boolean = true,
    val visualizerTextColorHex: Long = 0xFF00E5FF,
    val gaplessPlayback: Boolean = true,
    val crossfadeSec: Int = 0,
    val autoplayHeadset: Boolean = false,
    val notificationControls: Boolean = true,
    val keepScreenOn: Boolean = false,
    val shakeToSkip: Boolean = false,
    val sleepTimerMinutes: Int = 0,
    val spinningVinyl: Boolean = true,
    val albumArtStyle: String = "Vinyl Record",
    val customThemeSettings: CustomThemeSettings = CustomThemeSettings(),
    val appThemeMode: com.example.model.AppThemeMode = com.example.model.AppThemeMode.DARK_OLED,
    val useSystemMediaNotification: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = SettingsPreferencesManager(application)
    private val playerManager = AudioPlayerManager(application, viewModelScope)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _simpleSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isScanning = MutableStateFlow(false)
    private val _activeCategory = MutableStateFlow(LibraryCategory.SONGS)
    private val _categoryEventId = MutableStateFlow(0L)

    // Restore last selected tab or default to NOW_PLAYING (Request 3)
    private val initialTab: AppTab = try {
        AppTab.valueOf(prefsManager.loadLastTab(AppTab.NOW_PLAYING.name))
    } catch (t: Throwable) {
        AppTab.NOW_PLAYING
    }
    private val _selectedTab = MutableStateFlow(initialTab)

    private val _hasStoragePermission = MutableStateFlow(prefsManager.loadStoragePermission())
    private val _selectedTheme = MutableStateFlow(prefsManager.loadTheme())
    private val _currentThemePalette = MutableStateFlow(getPaletteById(prefsManager.loadSelectedThemePaletteId()))
    val currentThemePalette: StateFlow<AppThemePalette> = _currentThemePalette.asStateFlow()
    private val _edgeLightingSettings = MutableStateFlow(prefsManager.loadEdgeLighting())

    // Visualizer state (Request 7)
    private val _isVisualizerEnabled = MutableStateFlow(prefsManager.loadVisualizerEnabled())
    private val _selectedVisualizerEffectId = MutableStateFlow(prefsManager.loadVisualizerEffectId())
    private val _customVisualizerText = MutableStateFlow(prefsManager.loadCustomVisualizerText())

    // Expanded Settings & Preferences
    private val _showVisualizerText = MutableStateFlow(prefsManager.loadShowVisualizerText())
    private val _visualizerTextColorHex = MutableStateFlow(prefsManager.loadVisualizerTextColor())
    private val _gaplessPlayback = MutableStateFlow(prefsManager.loadGaplessPlayback())
    private val _crossfadeSec = MutableStateFlow(prefsManager.loadCrossfadeSec())
    private val _autoplayHeadset = MutableStateFlow(prefsManager.loadAutoplayHeadset())
    private val _notificationControls = MutableStateFlow(prefsManager.loadNotificationControls())
    private val _keepScreenOn = MutableStateFlow(prefsManager.loadKeepScreenOn())
    private val _shakeToSkip = MutableStateFlow(prefsManager.loadShakeToSkip())
    private val _sleepTimerMinutes = MutableStateFlow(prefsManager.loadSleepTimerMinutes())
    private val _spinningVinyl = MutableStateFlow(prefsManager.loadSpinningVinyl())
    private val _albumArtStyle = MutableStateFlow(prefsManager.loadAlbumArtStyle())

    private val _customThemeSettings = MutableStateFlow(prefsManager.loadCustomThemeSettings())
    private val _appThemeMode = MutableStateFlow(prefsManager.loadAppThemeMode())
    val appThemeMode: StateFlow<com.example.model.AppThemeMode> = _appThemeMode.asStateFlow()

    private val _useSystemMediaNotification = MutableStateFlow(prefsManager.loadUseSystemMediaNotification())
    val useSystemMediaNotification: StateFlow<Boolean> = _useSystemMediaNotification.asStateFlow()

    fun setUseSystemMediaNotification(enabled: Boolean) {
        _useSystemMediaNotification.value = enabled
        prefsManager.saveUseSystemMediaNotification(enabled)
        if (MediaPlaybackService.isServiceRunning) {
            val activeTheme = prefsManager.loadTheme()
            MediaPlaybackService.updateNotificationTheme(getApplication(), activeTheme.primaryColorInt)
        }
    }

    fun setAppThemeMode(mode: com.example.model.AppThemeMode) {
        _appThemeMode.value = mode
        prefsManager.saveAppThemeMode(mode)
        if (mode == com.example.model.AppThemeMode.LIGHT_WHITE) {
            _currentThemePalette.value = com.example.model.LIGHT_THEME_PALETTE
        } else {
            val matchingPalette = getPaletteForPreset(_selectedTheme.value)
            _currentThemePalette.value = matchingPalette
        }
    }

    // Right-Edge Analog Speedometer Volume Gauge State & Debounce
    private val _volumeGaugeSettings = MutableStateFlow(prefsManager.loadVolumeGaugeSettings())
    val volumeGaugeSettings: StateFlow<VolumeGaugeSettings> = _volumeGaugeSettings.asStateFlow()

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private fun getCurrentSystemVolumeFraction(): Float {
        return try {
            val am = audioManager ?: return 0.7f
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) current.toFloat() / max.toFloat() else 0.7f
        } catch (t: Throwable) {
            0.7f
        }
    }

    private val _volumeLevel = MutableStateFlow(getCurrentSystemVolumeFraction())
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    private val _isSpeedometerVisible = MutableStateFlow(false)
    val isSpeedometerVisible: StateFlow<Boolean> = _isSpeedometerVisible.asStateFlow()

    private var speedometerHideJob: Job? = null

    fun triggerSpeedometerVolumeDisplay(level: Float) {
        val clamped = level.coerceIn(0.0f, 1.0f)
        _volumeLevel.value = clamped
        _isSpeedometerVisible.value = true
        speedometerHideJob?.cancel()
        val delayMillis = (_volumeGaugeSettings.value.autoDismissDelaySec * 1000L).toLong().coerceAtLeast(800L)
        speedometerHideJob = viewModelScope.launch {
            delay(delayMillis)
            _isSpeedometerVisible.value = false
        }
        if (_volumeGaugeSettings.value.floatingOverlayEnabled && isMusicPlaying()) {
            VolumeOverlayManager.getInstance(getApplication()).showOrUpdateOverlay(clamped)
        }
    }

    /**
     * Pauses the auto-dismiss timer while the user is actively touching or dragging the gauge.
     */
    fun holdSpeedometerVisible() {
        speedometerHideJob?.cancel()
        _isSpeedometerVisible.value = true
    }

    /**
     * Resumes the auto-dismiss timer after user touch release.
     */
    fun releaseSpeedometerTouch() {
        val delayMillis = (_volumeGaugeSettings.value.autoDismissDelaySec * 1000L).toLong().coerceAtLeast(800L)
        speedometerHideJob?.cancel()
        speedometerHideJob = viewModelScope.launch {
            delay(delayMillis)
            _isSpeedometerVisible.value = false
        }
    }

    fun isMusicPlaying(): Boolean {
        return playerManager.state.value.isPlaying
    }

    /**
     * Intercepts hardware volume key presses.
     * When music is actively playing, displays the custom digital cyberpunk HUD and adjusts volume,
     * suppressing the default Android dialog by returning true.
     * When music is paused or stopped, returns false so standard Android system volume behavior is restored.
     */
    fun onVolumeKeyChanged(isUp: Boolean): Boolean {
        // CASE 2: When music is not actively playing, immediately dismiss custom overlay
        // and return false so Android's native system volume slider handles the key press.
        if (!isMusicPlaying()) {
            _isSpeedometerVisible.value = false
            speedometerHideJob?.cancel()
            VolumeOverlayManager.getInstance(getApplication()).dismissImmediate()
            return false
        }

        // CASE 1: Music is playing
        val am = audioManager ?: return false
        try {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val stepPercent = _volumeGaugeSettings.value.stepSizePercent.toFloat() / 100f
            val currentFraction = if (max > 0) current.toFloat() / max.toFloat() else 0.5f
            val targetFraction = if (isUp) {
                (currentFraction + stepPercent).coerceAtMost(1.0f)
            } else {
                (currentFraction - stepPercent).coerceAtLeast(0.0f)
            }
            val targetIndex = (targetFraction * max).roundToInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
            triggerSpeedometerVolumeDisplay(targetFraction)
            return true
        } catch (t: Throwable) {
            Log.e("MusicViewModel", "Failed to update volume via key: ${t.message}")
            return false
        }
    }

    fun onVolumeGaugeScrub(targetFraction: Float) {
        val clamped = targetFraction.coerceIn(0.0f, 1.0f)
        _volumeLevel.value = clamped
        val am = audioManager
        if (am != null) {
            try {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetIndex = (clamped * max).roundToInt().coerceIn(0, max)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
            } catch (t: Throwable) {
                Log.e("MusicViewModel", "Failed to set volume stream: ${t.message}")
            }
        }
        holdSpeedometerVisible()
    }

    fun setVolumeGaugeSettings(settings: VolumeGaugeSettings) {
        _volumeGaugeSettings.value = settings
        prefsManager.saveVolumeGaugeSettings(settings)
    }

    private val _songSortOption = MutableStateFlow(prefsManager.loadSongSortOption())
    val songSortOption: StateFlow<SongSortOption> = _songSortOption.asStateFlow()

    private fun sortSongList(list: List<Song>, option: SongSortOption): List<Song> {
        return when (option) {
            SongSortOption.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAdded.takeIf { d -> d > 0L } ?: it.id }
            SongSortOption.TITLE_A_TO_Z -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOption.TITLE_Z_TO_A -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOption.DURATION_LONGEST_FIRST -> list.sortedByDescending { it.durationMs }
            SongSortOption.ARTIST_NAME -> list.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist.ifBlank { "Unknown Artist" } }
            )
        }
    }

    val sortedSongs: StateFlow<List<Song>> = combine(_allSongs, _songSortOption) { songs, option ->
        sortSongList(songs, option)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class LibraryData(
        val songs: List<Song>,
        val simpleSongs: List<Song>,
        val filtered: List<Song>,
        val query: String,
        val isScanning: Boolean,
        val hasPerm: Boolean,
        val category: LibraryCategory,
        val categoryEventId: Long = 0L
    )

    private val songsAndFilterFlow = combine(_allSongs, _simpleSongs, _searchQuery, _songSortOption) { songs, simpleSongs, query, sortOption ->
        val sortedAll = sortSongList(songs, sortOption)
        val filtered = if (query.isBlank()) {
            sortedAll
        } else {
            sortedAll.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
        Triple(sortedAll, simpleSongs, Pair(filtered, query))
    }

    private val libraryDataFlow = combine(
        songsAndFilterFlow,
        _isScanning,
        _hasStoragePermission,
        _activeCategory,
        _categoryEventId
    ) { (songs, simpleSongs, filteredAndQuery), isScanning, hasPerm, category, catEventId ->
        val (filtered, query) = filteredAndQuery
        LibraryData(songs, simpleSongs, filtered, query, isScanning, hasPerm, category, catEventId)
    }

    private val extraSettingsFlow = combine(
        _selectedTheme,
        _edgeLightingSettings,
        _isVisualizerEnabled,
        _selectedVisualizerEffectId,
        _customVisualizerText
    ) { theme, edge, visEnabled, visEffectId, visText ->
        listOf(theme, edge, visEnabled, visEffectId, visText)
    }

    private val playbackPrefFlow = combine(
        _showVisualizerText,
        _visualizerTextColorHex,
        _gaplessPlayback,
        _crossfadeSec,
        _autoplayHeadset
    ) { showVisText, visColor, gapless, crossfade, autoplay ->
        listOf(showVisText, visColor, gapless, crossfade, autoplay)
    }

    private val miscPrefFlow = combine(
        combine(_notificationControls, _keepScreenOn, _shakeToSkip) { notif, keepScreen, shake ->
            Triple(notif, keepScreen, shake)
        },
        combine(_sleepTimerMinutes, _spinningVinyl, _albumArtStyle) { sleepTimer, spinning, artStyle ->
            Triple(sleepTimer, spinning, artStyle)
        }
    ) { (notif, keepScreen, shake), (sleepTimer, spinning, artStyle) ->
        listOf(notif, keepScreen, shake, sleepTimer, spinning, artStyle)
    }

    val uiState: StateFlow<MusicUiState> = combine(
        combine(libraryDataFlow, _selectedTab, playerManager.state) { lib, tab, pState ->
            Triple(lib, tab, pState)
        },
        extraSettingsFlow,
        playbackPrefFlow,
        miscPrefFlow,
        combine(_customThemeSettings, _currentThemePalette, _appThemeMode) { customTheme, palette, mode ->
            Triple(customTheme, palette, mode)
        }
    ) { (lib, tab, pState), extraList, playbackList, miscList, (customTheme, currentPalette, mode) ->
        val theme = extraList[0] as AppThemePreset
        val edgeSettings = extraList[1] as EdgeLightingSettings
        val visEnabled = extraList[2] as Boolean
        val visEffectId = extraList[3] as Int
        val visText = extraList[4] as String

        val showVisText = playbackList[0] as Boolean
        val visColor = playbackList[1] as Long
        val gapless = playbackList[2] as Boolean
        val crossfade = playbackList[3] as Int
        val autoplay = playbackList[4] as Boolean

        val notif = miscList[0] as Boolean
        val keepScreen = miscList[1] as Boolean
        val shake = miscList[2] as Boolean
        val sleepTimer = miscList[3] as Int
        val spinning = miscList[4] as Boolean
        val artStyle = miscList[5] as String

        MusicUiState(
            songs = lib.songs,
            simpleSongs = lib.simpleSongs,
            filteredSongs = lib.filtered,
            searchQuery = lib.query,
            isScanning = lib.isScanning,
            selectedTab = tab,
            activeCategory = lib.category,
            categoryEventId = lib.categoryEventId,
            playerState = pState,
            hasStoragePermission = lib.hasPerm,
            selectedTheme = if (customTheme.isEnabled) customTheme.themePreset else theme,
            currentThemePalette = currentPalette,
            edgeLightingSettings = edgeSettings,
            isVisualizerEnabled = visEnabled,
            selectedVisualizerEffectId = visEffectId,
            customVisualizerText = visText,
            showVisualizerText = showVisText,
            visualizerTextColorHex = visColor,
            gaplessPlayback = gapless,
            crossfadeSec = crossfade,
            autoplayHeadset = autoplay,
            notificationControls = notif,
            keepScreenOn = keepScreen,
            shakeToSkip = shake,
            sleepTimerMinutes = sleepTimer,
            spinningVinyl = spinning,
            albumArtStyle = artStyle,
            customThemeSettings = customTheme,
            appThemeMode = mode,
            useSystemMediaNotification = _useSystemMediaNotification.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MusicUiState(
            selectedTab = initialTab,
            selectedTheme = _selectedTheme.value,
            currentThemePalette = _currentThemePalette.value,
            edgeLightingSettings = _edgeLightingSettings.value,
            hasStoragePermission = _hasStoragePermission.value,
            isVisualizerEnabled = _isVisualizerEnabled.value,
            selectedVisualizerEffectId = _selectedVisualizerEffectId.value,
            customVisualizerText = _customVisualizerText.value,
            showVisualizerText = _showVisualizerText.value,
            visualizerTextColorHex = _visualizerTextColorHex.value,
            gaplessPlayback = _gaplessPlayback.value,
            crossfadeSec = _crossfadeSec.value,
            autoplayHeadset = _autoplayHeadset.value,
            notificationControls = _notificationControls.value,
            keepScreenOn = _keepScreenOn.value,
            shakeToSkip = _shakeToSkip.value,
            sleepTimerMinutes = _sleepTimerMinutes.value,
            spinningVinyl = _spinningVinyl.value,
            albumArtStyle = _albumArtStyle.value,
            customThemeSettings = _customThemeSettings.value,
            appThemeMode = _appThemeMode.value,
            useSystemMediaNotification = _useSystemMediaNotification.value
        )
    )

    init {
        // Restore saved equalizer, spatial audio, playback parameters, and favorites into audio player
        val savedEq = prefsManager.loadEqualizer()
        val savedFavs = prefsManager.loadFavorites()
        val savedShuffle = prefsManager.loadLastShuffle()
        val savedRepeat = try {
            com.example.model.RepeatMode.valueOf(prefsManager.loadLastRepeat())
        } catch (t: Throwable) {
            com.example.model.RepeatMode.OFF
        }
        val savedAppVol = prefsManager.loadAppVolume()

        playerManager.restoreSavedSettings(
            equalizerState = savedEq,
            favorites = savedFavs,
            shuffle = savedShuffle,
            repeat = savedRepeat,
            appVolume = savedAppVol
        )

        // Restore playback speed and pitch
        val savedSpeed = prefsManager.loadPlaybackSpeed()
        val savedPitch = prefsManager.loadPlaybackPitch()
        playerManager.setPlaybackSpeed(savedSpeed)
        playerManager.setPlaybackPitch(savedPitch)

        // Restore spatial preferences with 8D auto-rotate off by default
        val restoredSpatial = prefsManager.loadSpatial(playerManager.state.value)
        playerManager.restoreSavedSettings(savedEq, savedFavs)
        playerManager.setAutoRotateSpeed(restoredSpatial.autoRotateSpeedSec)
        playerManager.setAutoRotateDirection(restoredSpatial.autoRotateDirection)
        playerManager.setStereoWidth(restoredSpatial.stereoWidth)
        playerManager.setVirtualizer(restoredSpatial.virtualizerEnabled, restoredSpatial.virtualizerStrength)
        playerManager.setBassBoost(restoredSpatial.bassBoostEnabled, restoredSpatial.bassBoostStrength)
        playerManager.setSpatialEnvironment(restoredSpatial.spatialEnvironment)
        playerManager.setReverbMaster(restoredSpatial.reverbMasterEnabled)
        playerManager.setReverbWetDryMix(restoredSpatial.reverbWetDryMix)
        playerManager.setReverbDecayTime(restoredSpatial.reverbDecayTime)
        playerManager.setDpsMaster(restoredSpatial.dpsEnabled)
        playerManager.setDpsProfile(restoredSpatial.dpsProfile)
        playerManager.setDpsBassBoost(restoredSpatial.dpsBassBoost)
        playerManager.setDpsClarity(restoredSpatial.dpsClarity)
        playerManager.setSoundstageDistance(restoredSpatial.soundstageDistance)
        playerManager.setHeadShadowEnabled(restoredSpatial.headShadowEnabled)
        playerManager.setBinauralWidener(restoredSpatial.binauralWidener)

        // Restore Stereo Delay settings
        playerManager.setDelayEnabled(restoredSpatial.delayEnabled)
        playerManager.setDelayTimeMs(restoredSpatial.delayTimeMs)
        playerManager.setDelayFeedbackPercent(restoredSpatial.delayFeedbackPercent)
        playerManager.setDelayMixPercent(restoredSpatial.delayMixPercent)

        // Auto rotate defaults to false on app launch per Request 5
        playerManager.setAutoRotateEnabled(false)

        // Observe playback state: instantly dismiss custom volume HUD when paused or stopped
        viewModelScope.launch {
            playerManager.state.collect { state ->
                if (!state.isPlaying) {
                    _isSpeedometerVisible.value = false
                    speedometerHideJob?.cancel()
                    VolumeOverlayManager.getInstance(getApplication()).dismissImmediate()
                }
            }
        }

        // Scan audio library on start
        scanDeviceAudio()
    }

    fun onPermissionResult(granted: Boolean) {
        _hasStoragePermission.value = granted
        prefsManager.saveStoragePermission(granted)
        if (granted) {
            scanDeviceAudio()
        }
    }

    fun scanDeviceAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                // Scan user's device audio strictly on Dispatchers.IO (never block Dispatchers.Main)
                val scanned = AudioScanner.scanDeviceAudio(getApplication())
                withContext(Dispatchers.Main.immediate) {
                    _allSongs.value = scanned
                    _simpleSongs.value = emptyList()
                    playerManager.setPlaylist(scanned)
                    ensureTrackLoaded(autoPlay = false)
                }
            } catch (e: Exception) {
                // Keep existing songs on error
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setActiveCategory(category: LibraryCategory) {
        _activeCategory.value = category
        _categoryEventId.value = System.currentTimeMillis()
        _selectedTab.value = AppTab.ALL_SONGS
        prefsManager.saveLastTab(AppTab.ALL_SONGS.name)
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
        prefsManager.saveLastTab(tab.name)
    }

    // --- Visualizer Controls (Request 7) ---
    fun toggleVisualizer() {
        val newEnabled = !_isVisualizerEnabled.value
        _isVisualizerEnabled.value = newEnabled
        prefsManager.saveVisualizerState(newEnabled, _selectedVisualizerEffectId.value)
    }

    fun selectVisualizerEffect(effectId: Int) {
        _selectedVisualizerEffectId.value = effectId
        prefsManager.saveVisualizerState(_isVisualizerEnabled.value, effectId)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSongSortOption(option: SongSortOption) {
        _songSortOption.value = option
        prefsManager.saveSongSortOption(option)
    }

    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    /**
     * Prepares first or last-played song into view if currently unassigned, without auto-playing.
     */
    fun ensureTrackLoaded(autoPlay: Boolean = false) {
        if (playerManager.state.value.currentSong == null && _allSongs.value.isNotEmpty()) {
            val lastSongId = prefsManager.loadLastPlayedSongId()
            val savedPos = prefsManager.loadLastPositionMs()
            val targetSong = _allSongs.value.firstOrNull { it.id == lastSongId } ?: _allSongs.value.first()
            if (autoPlay) {
                playerManager.playSong(targetSong)
            } else {
                playerManager.loadSongSilently(targetSong, savedPos)
            }
        }
    }

    fun playAllShuffled() {
        if (_allSongs.value.isNotEmpty()) {
            if (!playerManager.state.value.shuffleEnabled) {
                playerManager.toggleShuffle()
            }
            val randomSong = _allSongs.value.random()
            playerManager.playSong(randomSong)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun skipBackward10() {
        val current = playerManager.state.value.currentPositionMs
        val target = (current - 10000L).coerceAtLeast(0L)
        playerManager.seekTo(target)
    }

    fun skipForward10() {
        val current = playerManager.state.value.currentPositionMs
        val duration = playerManager.state.value.durationMs
        val target = if (duration > 0) (current + 10000L).coerceAtMost(duration) else current + 10000L
        playerManager.seekTo(target)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun startFastForward() {
        playerManager.startFastForward()
    }

    fun stopFastForward() {
        playerManager.stopFastForward()
    }

    fun startRewind() {
        playerManager.startRewind()
    }

    fun stopRewind() {
        playerManager.stopRewind()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
        prefsManager.saveShuffle(playerManager.state.value.shuffleEnabled)
    }

    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
        prefsManager.saveRepeat(playerManager.state.value.repeatMode.name)
    }

    fun setAppVolume(volume: Float) {
        playerManager.setAppVolume(volume)
        prefsManager.saveAppVolume(volume)
    }

    fun toggleFavorite(songId: Long): Boolean {
        val result = playerManager.toggleFavorite(songId)
        prefsManager.saveFavorites(playerManager.state.value.favoriteIds)
        return result
    }

    fun clearAllFavorites() {
        playerManager.clearAllFavorites()
        prefsManager.saveFavorites(emptySet())
    }

    // --- UI Theme & Edge Lighting ---

    fun selectThemePalette(palette: AppThemePalette) {
        _currentThemePalette.value = palette
        prefsManager.saveSelectedThemePaletteId(palette.id)
    }

    fun setTheme(theme: AppThemePreset) {
        _selectedTheme.value = theme
        prefsManager.saveTheme(theme)
        val mode = if (theme.isDark) com.example.model.AppThemeMode.DARK_OLED else com.example.model.AppThemeMode.LIGHT_WHITE
        _appThemeMode.value = mode
        prefsManager.saveAppThemeMode(mode)
        val newColorMap = com.example.model.MultiElementColorMap.fromThemePreset(theme, shuffle = false)
        _customThemeSettings.update { it.copy(themePreset = theme, colorMap = newColorMap) }
        prefsManager.saveCustomThemeSettings(_customThemeSettings.value)
        val matchingPalette = getPaletteForPreset(theme)
        selectThemePalette(matchingPalette)

        // Ensure notification player stays 100% color-synchronized with active theme
        if (com.example.audio.MediaPlaybackService.isServiceRunning) {
            com.example.audio.MediaPlaybackService.updateNotificationTheme(
                context = getApplication(),
                accentColor = theme.primaryColorInt
            )
        }
    }

    fun setCustomThemeEnabled(enabled: Boolean) {
        _customThemeSettings.update { it.copy(isEnabled = enabled) }
        prefsManager.saveCustomThemeSettings(_customThemeSettings.value)
    }

    fun shuffleColors() {
        val currentTheme = _customThemeSettings.value.themePreset
        val shuffledMap = com.example.model.MultiElementColorMap.fromThemePreset(currentTheme, shuffle = true)
        _customThemeSettings.update { it.copy(colorMap = shuffledMap) }
        prefsManager.saveCustomThemeSettings(_customThemeSettings.value)
    }

    fun applyDefaultTheme() {
        resetToDefaultTheme()
    }

    fun resetThemeToDefault() {
        resetToDefaultTheme()
    }

    fun resetToDefaultTheme() {
        val defaultTheme = com.example.model.AppNaturalTheme.BLUE
        val defaultMap = com.example.model.MultiElementColorMap()
        _selectedTheme.value = defaultTheme
        prefsManager.saveTheme(defaultTheme)
        val mode = com.example.model.AppThemeMode.DARK_OLED
        _appThemeMode.value = mode
        prefsManager.saveAppThemeMode(mode)
        _customThemeSettings.update {
            it.copy(
                isEnabled = false,
                themePreset = defaultTheme,
                colorMap = defaultMap
            )
        }
        prefsManager.saveCustomThemeSettings(_customThemeSettings.value)
        val matchingPalette = getPaletteForPreset(defaultTheme)
        _currentThemePalette.value = matchingPalette
        prefsManager.saveSelectedThemePaletteId(matchingPalette.id)

        // Ensure notification player stays 100% color-synchronized with active theme
        if (com.example.audio.MediaPlaybackService.isServiceRunning) {
            com.example.audio.MediaPlaybackService.updateNotificationTheme(
                context = getApplication(),
                accentColor = defaultTheme.primaryColorInt
            )
        }
    }

    // --- Acoustic Environment Simplified Direct API ---
    fun setAcousticEnvironment(env: SpatialEnvironment) {
        playerManager.setAcousticEnvironment(env)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setReverbStrength(strength: Float) {
        playerManager.setReverbStrength(strength)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setEdgeLightingEnabled(enabled: Boolean) {
        _edgeLightingSettings.update { it.copy(isEnabled = enabled) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
        if (MediaPlaybackService.isServiceRunning) {
            val activeTheme = prefsManager.loadTheme()
            MediaPlaybackService.updateNotificationTheme(getApplication<Application>(), activeTheme.primaryColorInt)
        }
    }

    fun setNotificationEdgeLightingEnabled(enabled: Boolean) {
        _edgeLightingSettings.update { it.copy(isNotificationBorderEnabled = enabled) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
        if (MediaPlaybackService.isServiceRunning) {
            val activeTheme = prefsManager.loadTheme()
            MediaPlaybackService.updateNotificationTheme(getApplication<Application>(), activeTheme.primaryColorInt)
        }
    }

    fun setEdgeLightingShape(shape: com.example.model.EdgeLightingShape) {
        _edgeLightingSettings.update { it.copy(shape = shape) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    fun setEdgeLightingStyle(style: EdgeLightingStyle) {
        _edgeLightingSettings.update { it.copy(style = style) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    fun setEdgeLightingStrokeWidth(widthDp: Float) {
        _edgeLightingSettings.update { it.copy(strokeWidthDp = widthDp.coerceIn(2f, 12f)) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    fun setEdgeLightingSpeed(speedSec: Float) {
        _edgeLightingSettings.update { it.copy(animationSpeedSec = speedSec.coerceIn(0.5f, 8f)) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    fun setEdgeLightingCornerRadius(radiusDp: Float) {
        _edgeLightingSettings.update { it.copy(cornerRadiusDp = radiusDp.coerceIn(0f, 64f)) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    fun setEdgeLightingMusicReactive(reactive: Boolean) {
        _edgeLightingSettings.update { it.copy(musicReactive = reactive) }
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
    }

    // --- 13-Band DSP Equalizer ---

    fun setEqualizerEnabled(enabled: Boolean) {
        playerManager.setEqualizerEnabled(enabled)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    fun setEqualizerPreset(presetName: String) {
        playerManager.setEqualizerPreset(presetName)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
        playerManager.setEqualizerBandGain(bandIndex, gainDb)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    fun setEqualizerPreAmp(preAmpDb: Float) {
        playerManager.setEqualizerPreAmp(preAmpDb)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    fun setEqualizerBassPunch(punch: Float) {
        playerManager.setEqualizerBassPunch(punch)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    fun setEqualizerTrebleSparkle(sparkle: Float) {
        playerManager.setEqualizerTrebleSparkle(sparkle)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
    }

    // --- 3D Spatial & Auto-Rotate left to right ---

    fun setAutoRotateEnabled(enabled: Boolean) {
        playerManager.setAutoRotateEnabled(enabled)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setAutoRotateSpeed(speedSec: Float) {
        playerManager.setAutoRotateSpeed(speedSec)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setAutoRotateDirection(direction: AutoRotateDirection) {
        playerManager.setAutoRotateDirection(direction)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setStereoWidth(width: Float) {
        playerManager.setStereoWidth(width)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setManualPan(pan: Float) {
        playerManager.setManualPan(pan)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        playerManager.setVirtualizer(enabled, strength)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setVirtualizerMaster(enabled: Boolean, strength: Int = playerManager.state.value.virtualizerStrength) {
        playerManager.setVirtualizerMaster(enabled, strength)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setVirtualizerStrength(strength: Int) {
        playerManager.setVirtualizerMaster(playerManager.state.value.virtualizerEnabled, strength)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setVirtualizerMode(mode: String) {
        playerManager.setVirtualizerMode(mode)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setReverbMaster(enabled: Boolean) {
        playerManager.setReverbMaster(enabled)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setReverbWetDryMix(mix: Float) {
        playerManager.setReverbWetDryMix(mix)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setReverbDecayTime(decay: Float) {
        playerManager.setReverbDecayTime(decay)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setReverbPreset(env: SpatialEnvironment) {
        playerManager.setReverbPreset(env)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setRotationSpeedMultiplier(multiplier: Float) {
        playerManager.setRotationSpeedMultiplier(multiplier)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDpsMaster(enabled: Boolean) {
        playerManager.setDpsMaster(enabled)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDpsProfile(profile: DpsProfile) {
        playerManager.setDpsProfile(profile)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDpsBassBoost(bassBoost: Int) {
        playerManager.setDpsBassBoost(bassBoost)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDpsClarity(clarity: Int) {
        playerManager.setDpsClarity(clarity)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun toggleVirtualizer() {
        val next = !playerManager.state.value.virtualizerEnabled
        setVirtualizer(next, playerManager.state.value.virtualizerStrength)
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        playerManager.setBassBoost(enabled, strength)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setSpatialEnvironment(env: SpatialEnvironment) {
        playerManager.setSpatialEnvironment(env)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setSoundstageDistance(distance: String) {
        playerManager.setSoundstageDistance(distance)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setElevation(elevation: String) {
        playerManager.setElevation(elevation)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setHeadShadow(enabled: Boolean) {
        playerManager.setHeadShadowEnabled(enabled)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setBinauralWidener(widener: Float) {
        playerManager.setBinauralWidener(widener)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    // --- Stereo Delay / Echo Audio Effect Module ---

    fun setDelayEnabled(enabled: Boolean) {
        playerManager.setDelayEnabled(enabled)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDelayTimeMs(timeMs: Int) {
        playerManager.setDelayTimeMs(timeMs)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDelayFeedbackPercent(percent: Int) {
        playerManager.setDelayFeedbackPercent(percent)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun setDelayMixPercent(percent: Int) {
        playerManager.setDelayMixPercent(percent)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun resetAudioSettings() {
        playerManager.resetAudioSettings()
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun resetSpatialEffects() {
        playerManager.resetSpatialEffects()
        prefsManager.saveSpatial(playerManager.state.value)
    }

    fun saveAllSettingsNow() {
        prefsManager.saveTheme(_selectedTheme.value)
        prefsManager.saveEdgeLighting(_edgeLightingSettings.value)
        prefsManager.saveEqualizer(playerManager.state.value.equalizerState)
        prefsManager.saveSpatial(playerManager.state.value)
        prefsManager.saveFavorites(playerManager.state.value.favoriteIds)
        prefsManager.saveLastTab(_selectedTab.value.name)
        val pState = playerManager.state.value
        prefsManager.saveLastPlayback(
            songId = pState.currentSong?.id,
            positionMs = pState.currentPositionMs,
            isDemo = pState.currentSong?.isDemoTrack ?: false,
            shuffle = pState.shuffleEnabled,
            repeatModeName = pState.repeatMode.name
        )
        prefsManager.saveVisualizerState(_isVisualizerEnabled.value, _selectedVisualizerEffectId.value)
        prefsManager.saveCustomVisualizerText(_customVisualizerText.value)
        prefsManager.saveAppVolume(playerManager.state.value.appVolume)
        prefsManager.saveShuffle(playerManager.state.value.shuffleEnabled)
        prefsManager.saveRepeat(playerManager.state.value.repeatMode.name)
        prefsManager.saveShowVisualizerText(_showVisualizerText.value)
        prefsManager.saveVisualizerTextColor(_visualizerTextColorHex.value)
        prefsManager.saveGaplessPlayback(_gaplessPlayback.value)
        prefsManager.saveCrossfadeSec(_crossfadeSec.value)
        prefsManager.saveAutoplayHeadset(_autoplayHeadset.value)
        prefsManager.saveNotificationControls(_notificationControls.value)
        prefsManager.saveKeepScreenOn(_keepScreenOn.value)
        prefsManager.saveShakeToSkip(_shakeToSkip.value)
        prefsManager.saveSleepTimerMinutes(_sleepTimerMinutes.value)
    }

    // Custom Visualizer Branding Text (Request 6)
    fun updateCustomVisualizerText(text: String) {
        val sanitized = text.trim().take(10).ifBlank { SettingsPreferencesManager.DEFAULT_VISUALIZER_TEXT }
        prefsManager.saveCustomVisualizerText(sanitized)
        _customVisualizerText.value = sanitized
    }

    fun resetCustomVisualizerText() {
        val defaultText = SettingsPreferencesManager.DEFAULT_VISUALIZER_TEXT
        prefsManager.saveCustomVisualizerText(defaultText)
        _customVisualizerText.value = defaultText
    }

    // Additional Settings Controls
    fun setShowVisualizerText(show: Boolean) {
        _showVisualizerText.value = show
        prefsManager.saveShowVisualizerText(show)
    }

    fun setVisualizerTextColor(colorHex: Long) {
        _visualizerTextColorHex.value = colorHex
        prefsManager.saveVisualizerTextColor(colorHex)
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        prefsManager.saveGaplessPlayback(enabled)
    }

    fun setCrossfadeSec(sec: Int) {
        _crossfadeSec.value = sec
        prefsManager.saveCrossfadeSec(sec)
    }

    fun setAutoplayHeadset(enabled: Boolean) {
        _autoplayHeadset.value = enabled
        prefsManager.saveAutoplayHeadset(enabled)
    }

    fun setNotificationControls(enabled: Boolean) {
        _notificationControls.value = enabled
        prefsManager.saveNotificationControls(enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefsManager.saveKeepScreenOn(enabled)
    }

    fun setShakeToSkip(enabled: Boolean) {
        _shakeToSkip.value = enabled
        prefsManager.saveShakeToSkip(enabled)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        prefsManager.saveSleepTimerMinutes(minutes)
    }

    fun setRandomRotationEnabled(enabled: Boolean) {
        playerManager.setRandomRotationEnabled(enabled)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        prefsManager.savePlaybackSpeed(speed)
    }

    fun setPlaybackPitch(pitch: Float) {
        playerManager.setPlaybackPitch(pitch)
        prefsManager.savePlaybackPitch(pitch)
    }

    fun setAlbumArtStyle(style: String) {
        _albumArtStyle.value = style
        prefsManager.saveAlbumArtStyle(style)
    }

    fun setSpinningVinyl(enabled: Boolean) {
        _spinningVinyl.value = enabled
        prefsManager.saveSpinningVinyl(enabled)
    }

    fun setCustomThemeSettings(settings: CustomThemeSettings) {
        _customThemeSettings.value = settings
        if (settings.isEnabled) {
            _selectedTheme.value = settings.themePreset
            prefsManager.saveTheme(settings.themePreset)
            if (com.example.audio.MediaPlaybackService.isServiceRunning) {
                com.example.audio.MediaPlaybackService.updateNotificationTheme(
                    context = getApplication(),
                    accentColor = settings.themePreset.primaryColorInt
                )
            }
        }
        prefsManager.saveCustomThemeSettings(settings)
    }

    fun resetAllSettings() {
        prefsManager.resetAllSettings()
        playerManager.resetAudioSettings()
        _selectedTheme.value = com.example.model.AppNaturalTheme.BLUE
        val defaultPalette = getPaletteForPreset(com.example.model.AppNaturalTheme.BLUE)
        _currentThemePalette.value = defaultPalette
        prefsManager.saveSelectedThemePaletteId(defaultPalette.id)
        _appThemeMode.value = com.example.model.AppThemeMode.DARK_OLED
        prefsManager.saveAppThemeMode(com.example.model.AppThemeMode.DARK_OLED)
        if (com.example.audio.MediaPlaybackService.isServiceRunning) {
            com.example.audio.MediaPlaybackService.updateNotificationTheme(
                context = getApplication(),
                accentColor = com.example.model.AppNaturalTheme.BLUE.primaryColorInt
            )
        }
        _edgeLightingSettings.value = EdgeLightingSettings()
        _isVisualizerEnabled.value = true
        _selectedVisualizerEffectId.value = 1
        _customVisualizerText.value = SettingsPreferencesManager.DEFAULT_VISUALIZER_TEXT
        _showVisualizerText.value = true
        _visualizerTextColorHex.value = 0xFF00E5FF
        _gaplessPlayback.value = true
        _crossfadeSec.value = 0
        _autoplayHeadset.value = false
        _notificationControls.value = true
        _keepScreenOn.value = false
        _shakeToSkip.value = false
        _sleepTimerMinutes.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        saveAllSettingsNow()
        playerManager.release()
    }
}
