package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.LibraryCategory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.model.LocalAppThemePalette
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EdgeLightingOverlay
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NaasirMusicLogo
import com.example.ui.components.RightEdgeSpeedometerVolumeGauge
import com.example.ui.components.SideNavigationDrawerContent
import kotlinx.coroutines.launch
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpatialScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleNeon
import com.example.viewmodel.AppTab
import com.example.viewmodel.MusicViewModel

@Composable
fun MainScreen(
    viewModel: MusicViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortedSongs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val songSortOption by viewModel.songSortOption.collectAsStateWithLifecycle()
    val volumeLevel by viewModel.volumeLevel.collectAsStateWithLifecycle()
    val isSpeedometerVisible by viewModel.isSpeedometerVisible.collectAsStateWithLifecycle()
    val volumeGaugeSettings by viewModel.volumeGaugeSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission setup for reading device audio and displaying playback notifications
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val isGranted = results[audioPermission] ?: false
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            audioPermission
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)

        val ungranted = permissionsToRequest.filter { perm: String ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    // Request 1: Dynamic Navigation Bar transparency when Edge Lighting is enabled; visible device navigation when off
    val isEdgeLightingEnabled = uiState.edgeLightingSettings.isEnabled
    val activity = context as? Activity

    DisposableEffect(isEdgeLightingEnabled, uiState.selectedTheme) {
        val window = activity?.window
        if (window != null) {
            val originalNavBarColor = window.navigationBarColor
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isEdgeLightingEnabled) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                }
            } else {
                // When Edge Lighting is OFF, explicitly make device navigation visible, solid, and non-transparent
                window.navigationBarColor = uiState.selectedTheme.surfaceColor.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = 0xFF1E293B.toInt()
                }
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                insetsController.isAppearanceLightNavigationBars = false
            }
            onDispose {
                try {
                    window.navigationBarColor = originalNavBarColor
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                } catch (t: Throwable) {}
            }
        } else {
            onDispose { }
        }
    }

    // Keep Screen On Window Flag Handler
    DisposableEffect(uiState.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (uiState.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Shake to Skip Accelerometer Sensor Handler
    DisposableEffect(uiState.shakeToSkip) {
        if (!uiState.shakeToSkip) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            var lastShakeTime = 0L
            val listener = object : android.hardware.SensorEventListener {
                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    if (event == null) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gX = x / android.hardware.SensorManager.GRAVITY_EARTH
                    val gY = y / android.hardware.SensorManager.GRAVITY_EARTH
                    val gZ = z / android.hardware.SensorManager.GRAVITY_EARTH
                    val gForce = kotlin.math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
                    if (gForce > 2.5f) {
                        val now = System.currentTimeMillis()
                        if (now - lastShakeTime > 1200L) {
                            lastShakeTime = now
                            viewModel.playNext()
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }
            sensorManager?.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Intercept back press when drawer is open, or on another tab to return to library smoothly
    BackHandler(enabled = drawerState.isOpen || uiState.selectedTab != AppTab.ALL_SONGS) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.setActiveCategory(LibraryCategory.SONGS)
        }
    }

    val activeNaturalTheme = com.example.model.AppNaturalTheme.BLUE
    val isNowPlaying = uiState.selectedTab == AppTab.NOW_PLAYING
    val palette = com.example.model.getPaletteForPreset(activeNaturalTheme)

    CompositionLocalProvider(LocalAppThemePalette provides palette) {
        MyApplicationTheme(
            naturalTheme = activeNaturalTheme,
            isDarkTheme = true
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerContentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(320.dp)
                    ) {
                        SideNavigationDrawerContent(
                            totalSongsCount = uiState.songs.size,
                            activeCategory = uiState.activeCategory,
                            onSelectCategory = { category ->
                                viewModel.setActiveCategory(category)
                                viewModel.selectTab(AppTab.ALL_SONGS)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onNavigateSettings = {
                                viewModel.selectTab(AppTab.SETTINGS)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onCloseDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            },
                            isCosmicOrbit = false
                        )
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            NaasirTopBar(
                                isPlaying = uiState.playerState.isPlaying,
                                autoRotateActive = uiState.playerState.autoRotateEnabled,
                                backgroundColor = MaterialTheme.colorScheme.background,
                                primaryColor = palette.primaryAccent,
                                isLightBackground = false,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                        },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Mini Player with Next/Prev/Play and Touchable Progress Bar
                            if (uiState.playerState.currentSong != null && uiState.selectedTab != AppTab.NOW_PLAYING) {
                                MiniPlayer(
                                    currentSong = uiState.playerState.currentSong,
                                    isPlaying = uiState.playerState.isPlaying,
                                    progressMs = uiState.playerState.currentPositionMs,
                                    durationMs = uiState.playerState.durationMs,
                                    autoRotateActive = uiState.playerState.autoRotateEnabled,
                                    onPlayPauseClick = { viewModel.togglePlayPause() },
                                    onPreviousClick = { viewModel.playPrevious() },
                                    onNextClick = { viewModel.playNext() },
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onExpandClick = { viewModel.selectTab(AppTab.NOW_PLAYING) },
                                    isFastForwarding = uiState.playerState.isFastForwarding,
                                    isRewinding = uiState.playerState.isRewinding,
                                    onStartFastForward = { viewModel.startFastForward() },
                                    onStopFastForward = { viewModel.stopFastForward() },
                                    onStartRewind = { viewModel.startRewind() },
                                    onStopRewind = { viewModel.stopRewind() },
                                    customVisualizerText = uiState.customVisualizerText,
                                    showVisualizerText = uiState.showVisualizerText,
                                    visualizerTextColor = Color(uiState.visualizerTextColorHex),
                                    isCosmicOrbit = false
                                )
                            }

                            // Themed Bottom Navigation Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Top subtle accent line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        palette.primaryAccent.copy(alpha = 0.15f),
                                                        palette.primaryAccent.copy(alpha = 0.60f),
                                                        palette.secondaryAccent.copy(alpha = 0.60f),
                                                        palette.primaryAccent.copy(alpha = 0.15f)
                                                    )
                                                )
                                            )
                                    )

                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .testTag("main_bottom_nav")
                                    ) {
                                        val navItemColors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Tab 1: Library
                                        val isLibrarySelected = uiState.selectedTab == AppTab.ALL_SONGS
                                        NavigationBarItem(
                                            selected = isLibrarySelected,
                                            onClick = { viewModel.selectTab(AppTab.ALL_SONGS) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.LibraryMusic,
                                                    contentDescription = "Library",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "Library",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isLibrarySelected) FontWeight.Bold else FontWeight.Medium,
                                                        letterSpacing = (-0.2).sp
                                                    ),
                                                    color = if (isLibrarySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = navItemColors,
                                            modifier = Modifier.testTag("nav_tab_library")
                                        )

                                        // Tab 2: Now Playing
                                        val isNowPlayingSelected = uiState.selectedTab == AppTab.NOW_PLAYING
                                        NavigationBarItem(
                                            selected = isNowPlayingSelected,
                                            onClick = { viewModel.selectTab(AppTab.NOW_PLAYING) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = "Now Playing",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "Now Playing",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isNowPlayingSelected) FontWeight.Bold else FontWeight.Medium,
                                                        letterSpacing = (-0.2).sp
                                                    ),
                                                    color = if (isNowPlayingSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = navItemColors,
                                            modifier = Modifier.testTag("nav_tab_now_playing")
                                        )

                                        // Tab 3: Equalizer
                                        val isEqSelected = uiState.selectedTab == AppTab.EQUALIZER
                                        NavigationBarItem(
                                            selected = isEqSelected,
                                            onClick = { viewModel.selectTab(AppTab.EQUALIZER) },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (uiState.playerState.equalizerState.isEnabled) {
                                                            Badge(
                                                                containerColor = palette.primaryAccent,
                                                                modifier = Modifier.size(6.dp)
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GraphicEq,
                                                        contentDescription = "Equalizer",
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = "Equalizer",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isEqSelected) FontWeight.Bold else FontWeight.Medium,
                                                        letterSpacing = (-0.2).sp
                                                    ),
                                                    color = if (isEqSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = navItemColors,
                                            modifier = Modifier.testTag("nav_tab_equalizer")
                                        )

                                        // Tab 4: 3D Effects
                                        val isSpatialSelected = uiState.selectedTab == AppTab.SPATIAL_3D
                                        NavigationBarItem(
                                            selected = isSpatialSelected,
                                            onClick = { viewModel.selectTab(AppTab.SPATIAL_3D) },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (uiState.playerState.autoRotateEnabled) {
                                                            Badge(
                                                                containerColor = palette.secondaryAccent,
                                                                modifier = Modifier.size(6.dp)
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SurroundSound,
                                                        contentDescription = "3D Effects",
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = "3D Effects",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSpatialSelected) FontWeight.Bold else FontWeight.Medium,
                                                        letterSpacing = (-0.2).sp
                                                    ),
                                                    color = if (isSpatialSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = navItemColors,
                                            modifier = Modifier.testTag("nav_tab_spatial_3d")
                                        )

                                        // Tab 5: Settings
                                        val isSettingsSelected = uiState.selectedTab == AppTab.SETTINGS
                                        NavigationBarItem(
                                            selected = isSettingsSelected,
                                            onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "Settings",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Clip,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                                                        letterSpacing = (-0.2).sp
                                                    ),
                                                    color = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            colors = navItemColors,
                                            modifier = Modifier.testTag("nav_tab_settings")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.selectedTab) {
                            AppTab.ALL_SONGS -> {
                                LibraryScreen(
                                    songs = uiState.songs,
                                    filteredSongs = uiState.filteredSongs,
                                    sortedSongs = sortedSongs,
                                    simpleSongs = uiState.simpleSongs,
                                    currentSong = uiState.playerState.currentSong,
                                    isPlaying = uiState.playerState.isPlaying,
                                    isScanning = uiState.isScanning,
                                    searchQuery = uiState.searchQuery,
                                    hasStoragePermission = uiState.hasStoragePermission,
                                    favoriteIds = uiState.playerState.favoriteIds,
                                    activeCategory = uiState.activeCategory,
                                    categoryEventId = uiState.categoryEventId,
                                    sortOption = songSortOption,
                                    onSortOptionSelected = { viewModel.setSongSortOption(it) },
                                    onSelectCategory = { viewModel.setActiveCategory(it) },
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onSongClick = { song -> viewModel.playSong(song) },
                                    onShuffleAllClick = { viewModel.playAllShuffled() },
                                    onRescanClick = { viewModel.scanDeviceAudio() },
                                    onRequestPermissionClick = { permissionLauncher.launch(permissionsToRequest) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onRenameSong = { id, title, artist -> viewModel.renameSong(id, title, artist) },
                                    onDeleteSong = { id -> viewModel.deleteSong(id) },
                                    onSetCustomAlbumArt = { id, uri -> viewModel.setCustomAlbumArt(id, uri) },
                                    onDownloadAlbumArt = { id -> viewModel.downloadAlbumArtForSong(id) }
                                )
                            }

                            AppTab.NOW_PLAYING -> {
                                NowPlayingScreen(
                                    currentSong = uiState.playerState.currentSong,
                                    isPlaying = uiState.playerState.isPlaying,
                                    currentPositionMs = uiState.playerState.currentPositionMs,
                                    durationMs = uiState.playerState.durationMs,
                                    shuffleEnabled = uiState.playerState.shuffleEnabled,
                                    repeatMode = uiState.playerState.repeatMode,
                                    autoRotateActive = uiState.playerState.autoRotateEnabled,
                                    isFavorite = uiState.playerState.currentSong?.let { uiState.playerState.favoriteIds.contains(it.id) } == true,
                                    allSongs = uiState.songs,
                                    customThemeSettings = uiState.customThemeSettings,
                                    themePalette = palette,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onNext = { viewModel.playNext() },
                                    onPrevious = { viewModel.playPrevious() },
                                    onSkipBackward10 = { viewModel.skipBackward10() },
                                    onSkipForward10 = { viewModel.skipForward10() },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onSongSelect = { song -> viewModel.playSong(song) },
                                    onEnsureTrackLoaded = { viewModel.ensureTrackLoaded(autoPlay = false) },
                                    isFastForwarding = uiState.playerState.isFastForwarding,
                                    isRewinding = uiState.playerState.isRewinding,
                                    albumArtStyle = uiState.albumArtStyle,
                                    spinningVinyl = uiState.spinningVinyl,
                                    onStartFastForward = { viewModel.startFastForward() },
                                    onStopFastForward = { viewModel.stopFastForward() },
                                    onStartRewind = { viewModel.startRewind() },
                                    onStopRewind = { viewModel.stopRewind() },
                                    onBack = { viewModel.setActiveCategory(LibraryCategory.SONGS) }
                                )
                            }

                        AppTab.EQUALIZER -> {
                            EqualizerScreen(
                                equalizerState = uiState.playerState.equalizerState,
                                onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                                onSelectPreset = { viewModel.setEqualizerPreset(it) },
                                onSetBandGain = { band, gain -> viewModel.setEqualizerBandGain(band, gain) },
                                onSetPreAmp = { viewModel.setEqualizerPreAmp(it) },
                                onSetBassPunch = { viewModel.setEqualizerBassPunch(it) },
                                onSetTrebleSparkle = { viewModel.setEqualizerTrebleSparkle(it) }
                            )
                        }

                        AppTab.SPATIAL_3D -> {
                            SpatialScreen(
                                playerState = uiState.playerState,
                                onNavigateBack = { viewModel.selectTab(AppTab.ALL_SONGS) },
                                onSetReverbMaster = { viewModel.setReverbMaster(it) },
                                onSetReverbWetDryMix = { viewModel.setReverbWetDryMix(it) },
                                onSetReverbDecayTime = { viewModel.setReverbDecayTime(it) },
                                onSetReverbPreset = { viewModel.setReverbPreset(it) },
                                onSetRotationMaster = { viewModel.setAutoRotateEnabled(it) },
                                onSetRotationSpeedMultiplier = { viewModel.setRotationSpeedMultiplier(it) },
                                onSetRotationDirection = { viewModel.setAutoRotateDirection(it) },
                                onSetVirtualizerMaster = { viewModel.setVirtualizerMaster(it) },
                                onSetVirtualizerStrength = { viewModel.setVirtualizerStrength(it) },
                                onSetVirtualizerMode = { viewModel.setVirtualizerMode(it) },
                                onSetDpsMaster = { viewModel.setDpsMaster(it) },
                                onSetDpsProfile = { viewModel.setDpsProfile(it) },
                                onSetDpsBassBoost = { viewModel.setDpsBassBoost(it) },
                                onSetDpsClarity = { viewModel.setDpsClarity(it) },
                                onToggleAutoRotate = { viewModel.setAutoRotateEnabled(it) },
                                onSetAutoRotateSpeed = { viewModel.setAutoRotateSpeed(it) },
                                onSetDirection = { viewModel.setAutoRotateDirection(it) },
                                onSetStereoWidth = { viewModel.setStereoWidth(it) },
                                onToggleRandomRotation = { viewModel.setRandomRotationEnabled(it) },
                                onSetManualPan = { viewModel.setManualPan(it) },
                                onSetVirtualizer = { enabled, strength -> viewModel.setVirtualizer(enabled, strength) },
                                onSetBassBoost = { enabled, strength -> viewModel.setBassBoost(enabled, strength) },
                                onSetEnvironment = { viewModel.setSpatialEnvironment(it) },
                                onSetSoundstageDistance = { viewModel.setSoundstageDistance(it) },
                                onSetElevation = { viewModel.setElevation(it) },
                                onSetHeadShadow = { viewModel.setHeadShadow(it) },
                                onSetBinauralWidener = { viewModel.setBinauralWidener(it) },
                                onSetDelayEnabled = { viewModel.setDelayEnabled(it) },
                                onSetDelayTimeMs = { viewModel.setDelayTimeMs(it) },
                                onSetDelayFeedbackPercent = { viewModel.setDelayFeedbackPercent(it) },
                                onSetDelayMixPercent = { viewModel.setDelayMixPercent(it) }
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                playerState = uiState.playerState,
                                totalSongsCount = uiState.songs.size,
                                simpleSongsCount = uiState.simpleSongs.size,
                                selectedTheme = uiState.selectedTheme,
                                appThemeMode = uiState.appThemeMode,
                                onSetAppThemeMode = { viewModel.setAppThemeMode(it) },
                                customThemeSettings = uiState.customThemeSettings,
                                edgeLightingSettings = uiState.edgeLightingSettings,
                                edgeLightingShape = uiState.edgeLightingSettings.shape,
                                isVisualizerEnabled = uiState.isVisualizerEnabled,
                                selectedVisualizerEffectId = uiState.selectedVisualizerEffectId,
                                onSelectTheme = { viewModel.setTheme(it) },
                                onUpdateCustomThemeSettings = { viewModel.setCustomThemeSettings(it) },
                                onShuffleTheme = { viewModel.shuffleColors() },
                                onResetCustomTheme = { viewModel.resetThemeToDefault() },
                                onToggleCustomTheme = { viewModel.setCustomThemeEnabled(it) },
                                onSetEdgeLightingEnabled = { viewModel.setEdgeLightingEnabled(it) },
                                onSetNotificationEdgeLightingEnabled = { viewModel.setNotificationEdgeLightingEnabled(it) },
                                onSetEdgeLightingStyle = { viewModel.setEdgeLightingStyle(it) },
                                onSetEdgeLightingShape = { viewModel.setEdgeLightingShape(it) },
                                onSetEdgeLightingSpeed = { viewModel.setEdgeLightingSpeed(it) },
                                onSetEdgeLightingStrokeWidth = { viewModel.setEdgeLightingStrokeWidth(it) },
                                onSetEdgeLightingCornerRadius = { viewModel.setEdgeLightingCornerRadius(it) },
                                onSetEdgeLightingMusicReactive = { viewModel.setEdgeLightingMusicReactive(it) },
                                onToggleVisualizer = { viewModel.toggleVisualizer() },
                                onSelectVisualizerEffect = { viewModel.selectVisualizerEffect(it) },
                                onNavigateToEqualizer = { viewModel.selectTab(AppTab.EQUALIZER) },
                                onNavigateToSpatial = { viewModel.selectTab(AppTab.SPATIAL_3D) },
                                onToggleAutoRotate = { viewModel.setAutoRotateEnabled(it) },
                                onSetAutoRotateSpeed = { viewModel.setAutoRotateSpeed(it) },
                                onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
                                onSelectEqualizerPreset = { viewModel.setEqualizerPreset(it) },
                                onSetBassPunch = { viewModel.setEqualizerBassPunch(it) },
                                onSetTrebleSparkle = { viewModel.setEqualizerTrebleSparkle(it) },
                                onRescanLibrary = { viewModel.scanDeviceAudio() },
                                onResetAudioSettings = { viewModel.resetAudioSettings() },
                                onClearAllFavorites = { viewModel.clearAllFavorites() },
                                customVisualizerText = uiState.customVisualizerText,
                                onSaveCustomVisualizerText = { viewModel.updateCustomVisualizerText(it) },
                                onResetCustomVisualizerText = { viewModel.resetCustomVisualizerText() },
                                showVisualizerText = uiState.showVisualizerText,
                                onToggleShowVisualizerText = { viewModel.setShowVisualizerText(it) },
                                visualizerTextColorHex = uiState.visualizerTextColorHex,
                                onSelectVisualizerTextColor = { viewModel.setVisualizerTextColor(it) },
                                gaplessPlayback = uiState.gaplessPlayback,
                                onToggleGaplessPlayback = { viewModel.setGaplessPlayback(it) },
                                crossfadeSec = uiState.crossfadeSec,
                                onSetCrossfadeSec = { viewModel.setCrossfadeSec(it) },
                                autoplayHeadset = uiState.autoplayHeadset,
                                onToggleAutoplayHeadset = { viewModel.setAutoplayHeadset(it) },
                                notificationControls = uiState.notificationControls,
                                onToggleNotificationControls = { viewModel.setNotificationControls(it) },
                                useSystemMediaNotification = uiState.useSystemMediaNotification,
                                onToggleUseSystemMediaNotification = { viewModel.setUseSystemMediaNotification(it) },
                                keepScreenOn = uiState.keepScreenOn,
                                onToggleKeepScreenOn = { viewModel.setKeepScreenOn(it) },
                                shakeToSkip = uiState.shakeToSkip,
                                onToggleShakeToSkip = { viewModel.setShakeToSkip(it) },
                                playbackSpeed = uiState.playerState.playbackSpeed,
                                onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                                playbackPitch = uiState.playerState.playbackPitch,
                                onSetPlaybackPitch = { viewModel.setPlaybackPitch(it) },
                                albumArtStyle = uiState.albumArtStyle,
                                onSetAlbumArtStyle = { viewModel.setAlbumArtStyle(it) },
                                spinningVinyl = uiState.spinningVinyl,
                                onSetSpinningVinyl = { viewModel.setSpinningVinyl(it) },
                                volumeGaugeSettings = volumeGaugeSettings,
                                onUpdateVolumeGaugeSettings = { viewModel.setVolumeGaugeSettings(it) }
                            )
                        }
                    }
                }
            }

            // Edge Lighting visual overlay along all device borders (Request 10)
            EdgeLightingOverlay(
                settings = uiState.edgeLightingSettings,
                isPlaying = uiState.playerState.isPlaying
            )

            // Speedometer Volume Gauge Overlay (Left or Right Edge, Isolated from audio threads)
            RightEdgeSpeedometerVolumeGauge(
                visible = isSpeedometerVisible,
                volumeLevel = volumeLevel,
                settings = volumeGaugeSettings,
                onVolumeChange = { viewModel.onVolumeGaugeScrub(it) },
                onTouchHold = { viewModel.holdSpeedometerVisible() },
                onTouchRelease = { viewModel.releaseSpeedometerTouch() }
            )
        }
    }
}
}
}

/**
 * Top App Bar featuring "Naasir Music" with Navigation Drawer trigger
 */
@Composable
fun NaasirTopBar(
    isPlaying: Boolean,
    autoRotateActive: Boolean,
    backgroundColor: Color,
    primaryColor: Color,
    isLightBackground: Boolean = false,
    onOpenDrawer: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("naasir_top_bar"),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger Drawer Button (Request 3: Open drawer via hamburger icon)
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("drawer_hamburger_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation menu",
                    tint = if (isLightBackground) Color(0xFF0F172A) else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // App Icon with click zoom effect
            NaasirMusicLogo(
                size = 36.dp,
                animatedWavePulse = true,
                glowIntensity = 0.5f,
                onClick = { /* Interactive click zoom bounce */ }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Naasir Music",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isLightBackground) Color(0xFF0F172A) else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primaryColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = "Hi-Res Audio Engine Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLightBackground) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
