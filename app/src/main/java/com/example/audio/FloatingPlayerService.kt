package com.example.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.SweepGradient
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.model.RepeatMode as AppRepeatMode
import com.example.storage.SettingsPreferencesManager
import com.example.ui.components.AutoScrollText

/**
 * Custom lifecycle & saved-state owner for hosting Jetpack Compose inside WindowManager
 * outside of an Activity context.
 */
private class FloatingOverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        } catch (t: Throwable) {
            Log.e("FloatingPlayerService", "Error destroying overlay lifecycle: ${t.message}")
        }
    }
}

/**
 * Interactive Floating Desktop Player Overlay Service.
 *
 * Implements an Android System Alert Window overlay that:
 * 1. Floats over the Android Home Screen/Desktop when minimized or when home is pressed.
 * 2. Displays spinning disc thumbnail, track title, and artist name.
 * 3. Has real-time interactive scrubbable seekbar (Neon Cyan).
 * 4. Provides full playback controls (Shuffle, Previous, Play/Pause, Next, Repeat).
 * 5. Supports smooth drag gestures across the screen.
 * 6. Includes dynamic rotating Rainbow Edge Lighting border.
 * 7. Tapping the disc thumbnail launches MainActivity into NowPlayingScreen.
 */
class FloatingPlayerService : Service() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: FloatingOverlayLifecycleOwner? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private var isViewAttached = false

    private var screenWidthPx = 1080
    private var screenHeightPx = 1920

    private val rainbowEdgeEnabledState = mutableStateOf(true)

    companion object {
        private const val TAG = "FloatingPlayerService"
        const val EXTRA_OPEN_NOW_PLAYING = "extra_open_now_playing"
        const val ACTION_START = "com.example.audio.ACTION_START_FLOATING_PLAYER"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP_FLOATING_PLAYER"
        const val ACTION_UPDATE_RAINBOW = "com.example.audio.ACTION_UPDATE_RAINBOW"
        const val EXTRA_RAINBOW_ENABLED = "extra_rainbow_enabled"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            try {
                val intent = Intent(context, FloatingPlayerService::class.java).apply {
                    action = ACTION_START
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed starting FloatingPlayerService: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, FloatingPlayerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed stopping FloatingPlayerService: ${t.message}")
            }
        }

        fun updateRainbowEdge(context: Context, enabled: Boolean) {
            try {
                val intent = Intent(context, FloatingPlayerService::class.java).apply {
                    action = ACTION_UPDATE_RAINBOW
                    putExtra(EXTRA_RAINBOW_ENABLED, enabled)
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = resources.displayMetrics
        screenWidthPx = displayMetrics.widthPixels
        screenHeightPx = displayMetrics.heightPixels

        val prefs = SettingsPreferencesManager(applicationContext)
        rainbowEdgeEnabledState.value = prefs.loadFloatingRainbowEdgeEnabled()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeOverlayView()
                stopSelf()
            }
            ACTION_UPDATE_RAINBOW -> {
                val enabled = intent.getBooleanExtra(EXTRA_RAINBOW_ENABLED, true)
                rainbowEdgeEnabledState.value = enabled
            }
            ACTION_START, null -> {
                val prefs = SettingsPreferencesManager(applicationContext)
                rainbowEdgeEnabledState.value = prefs.loadFloatingRainbowEdgeEnabled()
                showOverlayView()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlayView() {
        if (isViewAttached) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val player = AudioPlayerManager.instance
        if (player == null || !player.state.value.isPlaying) {
            // Only show overlay when music is actively playing
            stopSelf()
            return
        }

        try {
            val owner = FloatingOverlayLifecycleOwner()
            lifecycleOwner = owner

            val density = resources.displayMetrics.density
            val widthPx = (320 * density).toInt()
            val heightPx = (140 * density).toInt()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = ((screenWidthPx - widthPx) / 2).coerceAtLeast(0)
                y = (screenHeightPx * 0.18f).toInt()
            }
            windowLayoutParams = params

            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    val rainbowEdgeEnabled by rainbowEdgeEnabledState

                    FloatingDesktopPlayerContent(
                        playerManager = AudioPlayerManager.instance,
                        rainbowEdgeEnabled = rainbowEdgeEnabled,
                        onDismiss = {
                            removeOverlayView()
                            stopSelf()
                        },
                        onOpenApp = {
                            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(EXTRA_OPEN_NOW_PLAYING, true)
                            }
                            startActivity(launchIntent)
                            removeOverlayView()
                            stopSelf()
                        },
                        onDragWindow = { dx, dy ->
                            handleWindowDrag(dx, dy)
                        }
                    )
                }
            }

            composeView = view
            windowManager.addView(view, params)
            isViewAttached = true
            isRunning = true
        } catch (t: Throwable) {
            Log.e(TAG, "Error attaching floating player window: ${t.message}", t)
            stopSelf()
        }
    }

    private fun handleWindowDrag(dx: Float, dy: Float) {
        val params = windowLayoutParams ?: return
        val view = composeView ?: return
        val newX = params.x + dx.toInt()
        val newY = params.y + dy.toInt()
        val maxX = (screenWidthPx - params.width).coerceAtLeast(0)
        val maxY = (screenHeightPx - params.height).coerceAtLeast(0)

        params.x = newX.coerceIn(0, maxX)
        params.y = newY.coerceIn(0, maxY)

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Throwable) {}
    }

    private fun removeOverlayView() {
        if (!isViewAttached) return
        try {
            composeView?.let { windowManager.removeView(it) }
        } catch (t: Throwable) {
            Log.e(TAG, "Error removing floating player view: ${t.message}")
        } finally {
            composeView = null
            lifecycleOwner?.destroy()
            lifecycleOwner = null
            isViewAttached = false
            isRunning = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
    }
}

/**
 * High-fidelity Jetpack Compose UI for the Floating Desktop Player Card.
 */
@Composable
private fun FloatingDesktopPlayerContent(
    playerManager: AudioPlayerManager?,
    rainbowEdgeEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onDragWindow: (Float, Float) -> Unit
) {
    val playerState = playerManager?.state?.collectAsState()?.value
    val currentSong = playerState?.currentSong
    val isPlaying = playerState?.isPlaying ?: false
    val currentPosMs = playerState?.currentPositionMs ?: 0L
    val durationMs = playerState?.durationMs?.coerceAtLeast(1L) ?: 1L
    val shuffleEnabled = playerState?.shuffleEnabled ?: false
    val repeatMode = playerState?.repeatMode ?: AppRepeatMode.OFF

    // Disc spinning animation
    val rotationAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    // Dynamic rotating Rainbow Edge Lighting
    val infiniteTransition = rememberInfiniteTransition(label = "floating_rainbow_edge")
    val rainbowAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow_rotation"
    )

    val rainbowColors = listOf(
        Color(0xFFFF0055), // Red
        Color(0xFFFF7700), // Orange
        Color(0xFFFFEE00), // Yellow
        Color(0xFF00FF66), // Green
        Color(0xFF00E5FF), // Cyan
        Color(0xFF0066FF), // Blue
        Color(0xFF9900FF), // Violet
        Color(0xFFFF0055)  // Red
    )

    // Outer Card Container with drag detection
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (!change.isConsumed) {
                        change.consume()
                        onDragWindow(dragAmount.x, dragAmount.y)
                    }
                }
            }
            .testTag("floating_desktop_player_overlay")
    ) {
        // 1. Frosted Glass Dark Navy Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B111E).copy(alpha = 0.65f))
        )

        // 2. Rainbow Edge Lighting Border (or static subtle cyan border if disabled)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (rainbowEdgeEnabled) {
                val rainbowIntColors = intArrayOf(
                    0xFFFF0055.toInt(), // Red
                    0xFFFF7700.toInt(), // Orange
                    0xFFFFEE00.toInt(), // Yellow
                    0xFF00FF66.toInt(), // Green
                    0xFF00E5FF.toInt(), // Cyan
                    0xFF0066FF.toInt(), // Blue
                    0xFF9900FF.toInt(), // Violet
                    0xFFFF0055.toInt()  // Red
                )
                val positions = floatArrayOf(0f, 0.14f, 0.28f, 0.42f, 0.57f, 0.71f, 0.85f, 1f)
                val sweepShader = SweepGradient(center.x, center.y, rainbowIntColors, positions)
                val matrix = Matrix()
                matrix.postRotate(rainbowAngle, center.x, center.y)
                sweepShader.setLocalMatrix(matrix)

                drawRoundRect(
                    brush = ShaderBrush(sweepShader),
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // 3. Inner Content Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP ROW: Mini Disc Thumbnail (44.dp), Track Title & Artist, Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Spinning Disc (44.dp, Click opens full-screen app)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C0E14))
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                            .rotate(rotationAnim.value)
                            .clickable { onOpenApp() }
                            .testTag("floating_player_disc_thumb"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            for (i in 1..4) {
                                drawCircle(
                                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                    radius = r * (0.35f + i * 0.14f),
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                        // Center label with album art or neon cyan music note
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.35f))
                                .border(1.dp, Color(0xFF00E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentSong?.albumArtUri != null) {
                                AsyncImage(
                                    model = currentSong.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Artist column with AutoScrollText
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenApp() }
                    ) {
                        AutoScrollText(
                            text = currentSong?.title ?: "No track playing",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSong?.artist ?: "Tap to select song",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Close / Dismiss overlay button (does NOT stop music)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() }
                            .testTag("floating_player_btn_dismiss"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Floating Player",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // MIDDLE ROW: Interactive Neon Cyan Seekbar & Timestamps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFloatingTime(currentPosMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Scrubbable Interactive Seekbar Canvas
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubFraction by remember { mutableFloatStateOf(0f) }

                    val activeFraction = if (isScrubbing) {
                        scrubFraction
                    } else {
                        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    }

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .pointerInput(durationMs) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val targetMs = (frac * durationMs).toLong()
                                    playerManager?.seekTo(targetMs)
                                }
                            }
                            .pointerInput(durationMs) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isScrubbing = true
                                        scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val targetMs = (scrubFraction * durationMs).toLong()
                                        playerManager?.seekTo(targetMs)
                                    },
                                    onDragEnd = {
                                        val targetMs = (scrubFraction * durationMs).toLong()
                                        playerManager?.seekTo(targetMs)
                                        isScrubbing = false
                                    },
                                    onDragCancel = {
                                        isScrubbing = false
                                    }
                                )
                            }
                            .testTag("floating_player_seekbar")
                    ) {
                        val trackHeight = 3.5.dp.toPx()
                        val yCenter = size.height / 2f
                        val barWidth = size.width
                        val activeWidth = barWidth * activeFraction

                        // Inactive track
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(0f, yCenter),
                            end = Offset(barWidth, yCenter),
                            strokeWidth = trackHeight,
                            cap = StrokeCap.Round
                        )

                        // Active Neon Cyan Progress
                        if (activeWidth > 0f) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF00B0FF), Color(0xFF00E5FF))
                                ),
                                start = Offset(0f, yCenter),
                                end = Offset(activeWidth, yCenter),
                                strokeWidth = trackHeight,
                                cap = StrokeCap.Round
                            )
                        }

                        // Glowing Slider Thumb
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                            radius = 6.dp.toPx(),
                            center = Offset(activeWidth, yCenter)
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 4.dp.toPx(),
                            center = Offset(activeWidth, yCenter)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.6.dp.toPx(),
                            center = Offset(activeWidth, yCenter)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = formatFloatingTime(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // BOTTOM ROW: Playback Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { playerManager?.toggleShuffle() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("floating_btn_shuffle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (shuffleEnabled) Color(0xFF00E5FF) else Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Previous Track
                    IconButton(
                        onClick = { playerManager?.skipToPrevious() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("floating_btn_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play / Pause Toggle Button (Prominent Glowing Neon Button)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { playerManager?.togglePlayPause() }
                            .testTag("floating_btn_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF080B14),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Next Track
                    IconButton(
                        onClick = { playerManager?.skipToNext() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("floating_btn_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(
                        onClick = { playerManager?.cycleRepeatMode() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("floating_btn_repeat")
                    ) {
                        val (icon, tint) = when (repeatMode) {
                            AppRepeatMode.ONE -> Icons.Default.RepeatOne to Color(0xFF00E5FF)
                            AppRepeatMode.ALL -> Icons.Default.Repeat to Color(0xFF00E5FF)
                            AppRepeatMode.OFF -> Icons.Default.Repeat to Color(0xFF64748B)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Cycle Repeat Mode",
                            tint = tint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatFloatingTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(mins, secs)
}
