package com.example.audio

import android.content.Context
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
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
import com.example.model.VolumeGaugePosition
import com.example.model.VolumeGaugeSettings
import com.example.storage.SettingsPreferencesManager
import com.example.ui.components.RightEdgeSpeedometerVolumeGauge
import kotlin.math.roundToInt

/**
 * Custom lifecycle and saved state registry owner to enable hosting Jetpack Compose ComposeView
 * inside Android WindowManager overlay outside of an Activity context without crashing.
 */
private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
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
            Log.e("VolumeOverlayManager", "Error destroying overlay lifecycle: ${t.message}")
        }
    }
}

/**
 * Manages the floating Speedometer Volume HUD window overlay over other applications.
 *
 * Requirements:
 * 1. Shows floating borderless HUD on hardware volume key press ONLY when music is actively playing.
 * 2. When music is paused, stopped, or idle: instantly dismisses the HUD and leaves volume events
 *    to Android's native system volume slider.
 * 3. Supports direct touch dragging and auto-dismisses after 2 seconds of inactivity.
 * 4. Safe lifecycle and clean window removal with zero window leaks.
 */
class VolumeOverlayManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefsManager = SettingsPreferencesManager(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isViewAttached = false

    private var volumeFractionState by mutableFloatStateOf(0.7f)
    private var volumeGaugeSettingsState by mutableStateOf(VolumeGaugeSettings())

    private var lastObservedVolume = -1
    private var isDismissPending = false

    private val dismissRunnable = Runnable {
        dismissImmediate()
    }

    private val volumeObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            val currentVol = try {
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            } catch (t: Throwable) {
                return
            }

            if (currentVol == lastObservedVolume) {
                return
            }
            lastObservedVolume = currentVol

            val isPlaying = isMusicPlaying()
            if (!isPlaying) {
                // When music is paused/stopped, immediately dismiss HUD and let native system volume handle it
                mainHandler.post { dismissImmediate() }
                return
            }

            // Music is playing: show custom speedometer HUD if overlay is allowed and enabled
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val fraction = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0.5f

            mainHandler.post {
                showOrUpdateOverlay(fraction)
            }
        }
    }

    init {
        registerVolumeObserver()
    }

    fun registerVolumeObserver() {
        try {
            appContext.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver
            )
            lastObservedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register volume ContentObserver: ${t.message}")
        }
    }

    fun unregisterVolumeObserver() {
        try {
            appContext.contentResolver.unregisterContentObserver(volumeObserver)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to unregister volume ContentObserver: ${t.message}")
        }
    }

    private fun isMusicPlaying(): Boolean {
        return AudioPlayerManager.instance?.state?.value?.isPlaying ?: false
    }

    /**
     * Called whenever playback state changes (play, pause, stop).
     * When playback is paused or stopped, immediately hides any active overlay.
     */
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        mainHandler.post {
            if (!isPlaying) {
                dismissImmediate()
            }
        }
    }

    /**
     * Displays or refreshes the floating Speedometer HUD on the right edge.
     */
    fun showOrUpdateOverlay(fraction: Float) {
        // Must have overlay permission and feature enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) {
            return
        }

        val settings = prefsManager.loadVolumeGaugeSettings()
        if (!settings.floatingOverlayEnabled) {
            return
        }

        volumeFractionState = fraction.coerceIn(0f, 1f)
        volumeGaugeSettingsState = settings

        if (!isViewAttached) {
            attachOverlayView(settings)
        }

        scheduleAutoDismiss(settings.autoDismissDelaySec)
    }

    private fun attachOverlayView(settings: VolumeGaugeSettings) {
        if (isViewAttached) return

        try {
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner

            val view = ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    RightEdgeSpeedometerVolumeGauge(
                        visible = true,
                        volumeLevel = volumeFractionState,
                        settings = volumeGaugeSettingsState,
                        fillScreen = false,
                        onVolumeChange = { newFraction ->
                            onOverlayVolumeScrub(newFraction)
                        },
                        onTouchHold = {
                            cancelAutoDismiss()
                        },
                        onTouchRelease = {
                            scheduleAutoDismiss(volumeGaugeSettingsState.autoDismissDelaySec)
                        }
                    )
                }
            }
            composeView = view

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val gravity = if (settings.position == VolumeGaugePosition.LEFT) {
                Gravity.START or Gravity.CENTER_VERTICAL
            } else {
                Gravity.END or Gravity.CENTER_VERTICAL
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
            }

            windowManager.addView(view, params)
            isViewAttached = true
            Log.d(TAG, "Floating Speedometer HUD successfully attached to WindowManager")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to attach floating overlay: ${t.message}", t)
            isViewAttached = false
        }
    }

    private fun onOverlayVolumeScrub(newFraction: Float) {
        volumeFractionState = newFraction.coerceIn(0f, 1f)
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetIndex = (volumeFractionState * max).roundToInt().coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set stream volume during overlay scrub: ${t.message}")
        }
        cancelAutoDismiss()
    }

    private fun scheduleAutoDismiss(delaySec: Float) {
        cancelAutoDismiss()
        val delayMs = (delaySec * 1000L).toLong().coerceAtLeast(1000L)
        mainHandler.postDelayed(dismissRunnable, delayMs)
    }

    private fun cancelAutoDismiss() {
        mainHandler.removeCallbacks(dismissRunnable)
    }

    /**
     * Immediately dismisses and removes the floating HUD view cleanly without window leaks.
     */
    fun dismissImmediate() {
        cancelAutoDismiss()
        if (isViewAttached && composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (t: Throwable) {
                Log.e(TAG, "Error removing overlay view: ${t.message}")
            }
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        composeView = null
        isViewAttached = false
    }

    fun cleanUp() {
        unregisterVolumeObserver()
        dismissImmediate()
    }

    companion object {
        private const val TAG = "VolumeOverlayManager"

        @Volatile
        private var instance: VolumeOverlayManager? = null

        fun getInstance(context: Context): VolumeOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: VolumeOverlayManager(context).also { instance = it }
            }
        }
    }
}
