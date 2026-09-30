package com.example.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.storage.SettingsPreferencesManager
import java.io.InputStream

/**
 * Foreground Service for persistent background audio playback and Android MediaStyle
 * notification controls with full lock-screen and system media widget support.
 */
class MediaPlaybackService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false
    private var isNoisyReceiverRegistered = false

    // Stored playback state for instantaneous notification re-issues on dynamic theme changes
    private var lastTitle: String = "Naasir Music Pro"
    private var lastArtist: String = "Unknown Artist"
    private var lastAlbum: String = "Audio Library"
    private var lastUriString: String? = null
    private var lastAlbumArtUriString: String? = null
    private var lastSongPath: String? = null
    private var lastIsPlaying: Boolean = false
    private var lastPositionMs: Long = 0L
    private var lastDurationMs: Long = 1L
    private var lastAccentColor: Int? = null
    private var lastShuffleEnabled: Boolean = false
    private var lastRepeatMode: RepeatMode = RepeatMode.OFF

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == "key_selected_theme" || key == "key_custom_color_map" || key == "key_theme_palette_id" || key == "key_edge_enabled" || key == "key_edge_notification_border") {
            Log.d(TAG, "Theme or edge lighting preference changed ($key), refreshing notification colors immediately")
            val prefs = SettingsPreferencesManager(applicationContext)
            val activeTheme = prefs.loadTheme()
            updateNotificationThemeInternal(activeTheme.primaryColorInt)
        }
    }

    private val noisyAudioReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val prefs = SettingsPreferencesManager(applicationContext)
                val shouldAutoPause = prefs.loadAutoPauseHeadset()
                Log.d(TAG, "MediaPlaybackService: ACTION_AUDIO_BECOMING_NOISY received. autoPauseHeadset=$shouldAutoPause")
                if (shouldAutoPause) {
                    AudioPlayerManager.instance?.pause()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MediaPlaybackService"
        const val NOTIFICATION_ID = 9101
        const val CHANNEL_ID = "naasir_music_playback_channel_v2"

        const val ACTION_PLAY_PAUSE = "com.example.audio.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.audio.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.audio.ACTION_UPDATE"
        const val ACTION_UPDATE_THEME = "com.example.audio.ACTION_UPDATE_THEME"
        const val ACTION_TOGGLE_SHUFFLE = MediaNotificationManager.ACTION_TOGGLE_SHUFFLE
        const val ACTION_TOGGLE_REPEAT = MediaNotificationManager.ACTION_TOGGLE_REPEAT
        const val ACTION_SEEK_TO = MediaNotificationManager.ACTION_SEEK_TO
        const val EXTRA_SEEK_POSITION = MediaNotificationManager.EXTRA_SEEK_POSITION

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_SONG_ALBUM = "extra_song_album"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_SONG_ALBUM_ART_URI = "extra_song_album_art_uri"
        const val EXTRA_SONG_PATH = "extra_song_path"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_THEME_ACCENT_COLOR = "extra_theme_accent_color"
        const val EXTRA_SHUFFLE_ENABLED = "extra_shuffle_enabled"
        const val EXTRA_REPEAT_MODE = "extra_repeat_mode"

        @Volatile
        var isServiceRunning = false
            private set

        fun startOrUpdate(
            context: Context,
            song: Song?,
            isPlaying: Boolean,
            positionMs: Long,
            durationMs: Long
        ) {
            try {
                val playerState = AudioPlayerManager.instance?.state?.value
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_SONG_TITLE, song?.title ?: "Naasir Music Pro")
                    putExtra(EXTRA_SONG_ARTIST, song?.artist ?: "Unknown Artist")
                    putExtra(EXTRA_SONG_ALBUM, song?.album ?: "Audio Library")
                    putExtra(EXTRA_SONG_URI, song?.uri?.toString())
                    putExtra(EXTRA_SONG_ALBUM_ART_URI, song?.albumArtUri?.toString())
                    putExtra(EXTRA_SONG_PATH, song?.path)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                    putExtra(EXTRA_POSITION_MS, positionMs)
                    putExtra(EXTRA_DURATION_MS, durationMs.coerceAtLeast(1L))
                    putExtra(EXTRA_SHUFFLE_ENABLED, playerState?.shuffleEnabled ?: false)
                    putExtra(EXTRA_REPEAT_MODE, playerState?.repeatMode?.name ?: "OFF")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error starting/updating MediaPlaybackService: ${t.message}")
            }
        }

        fun updateNotificationTheme(context: Context, accentColor: Int? = null) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_UPDATE_THEME
                    if (accentColor != null) {
                        putExtra(EXTRA_THEME_ACCENT_COLOR, accentColor)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error updating notification theme in MediaPlaybackService: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Error stopping MediaPlaybackService: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        initMediaSession()
        acquireWakeLock()
        registerNoisyReceiver()
        try {
            val sharedPrefs = applicationContext.getSharedPreferences("naasir_music_user_settings", Context.MODE_PRIVATE)
            sharedPrefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Error registering prefChangeListener: ${t.message}")
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                registerReceiver(noisyAudioReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                isNoisyReceiverRegistered = true
                Log.d(TAG, "MediaPlaybackService: Registered ACTION_AUDIO_BECOMING_NOISY receiver")
            } catch (t: Throwable) {
                Log.w(TAG, "MediaPlaybackService: Error registering noisy receiver: ${t.message}")
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(noisyAudioReceiver)
                isNoisyReceiverRegistered = false
                Log.d(TAG, "MediaPlaybackService: Unregistered ACTION_AUDIO_BECOMING_NOISY receiver")
            } catch (t: Throwable) {
                Log.w(TAG, "MediaPlaybackService: Error unregistering noisy receiver: ${t.message}")
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NaasirMusic:MediaWakeLock")?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not acquire WakeLock: ${t.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error releasing WakeLock: ${t.message}")
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Naasir Music Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Naasir Music Pro background playback & media controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "NaasirMusicSession").apply {
            isActive = true
            val initialActions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(initialActions)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 0.0f)
                    .build()
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    AudioPlayerManager.instance?.play()
                }

                override fun onPause() {
                    AudioPlayerManager.instance?.pause()
                }

                override fun onSkipToNext() {
                    AudioPlayerManager.instance?.playNext()
                }

                override fun onSkipToPrevious() {
                    AudioPlayerManager.instance?.playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    AudioPlayerManager.instance?.seekTo(pos)
                }

                override fun onSetShuffleMode(shuffleMode: Int) {
                    AudioPlayerManager.instance?.toggleShuffle()
                    val currentShuffle = AudioPlayerManager.instance?.state?.value?.shuffleEnabled ?: !lastShuffleEnabled
                    lastShuffleEnabled = currentShuffle
                    updateNotificationAndSession(
                        title = lastTitle,
                        artist = lastArtist,
                        album = lastAlbum,
                        uriString = lastUriString,
                        albumArtUriString = lastAlbumArtUriString,
                        songPath = lastSongPath,
                        isPlaying = lastIsPlaying,
                        positionMs = lastPositionMs,
                        durationMs = lastDurationMs
                    )
                }

                override fun onSetRepeatMode(repeatMode: Int) {
                    AudioPlayerManager.instance?.cycleRepeatMode()
                    val currentRepeat = AudioPlayerManager.instance?.state?.value?.repeatMode ?: lastRepeatMode.next()
                    lastRepeatMode = currentRepeat
                    updateNotificationAndSession(
                        title = lastTitle,
                        artist = lastArtist,
                        album = lastAlbum,
                        uriString = lastUriString,
                        albumArtUriString = lastAlbumArtUriString,
                        songPath = lastSongPath,
                        isPlaying = lastIsPlaying,
                        positionMs = lastPositionMs,
                        durationMs = lastDurationMs
                    )
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    when (action) {
                        ACTION_TOGGLE_SHUFFLE -> {
                            AudioPlayerManager.instance?.toggleShuffle()
                            val currentShuffle = AudioPlayerManager.instance?.state?.value?.shuffleEnabled ?: !lastShuffleEnabled
                            lastShuffleEnabled = currentShuffle
                            updateNotificationAndSession(
                                title = lastTitle,
                                artist = lastArtist,
                                album = lastAlbum,
                                uriString = lastUriString,
                                albumArtUriString = lastAlbumArtUriString,
                                songPath = lastSongPath,
                                isPlaying = lastIsPlaying,
                                positionMs = lastPositionMs,
                                durationMs = lastDurationMs
                            )
                        }
                        ACTION_TOGGLE_REPEAT -> {
                            AudioPlayerManager.instance?.cycleRepeatMode()
                            val currentRepeat = AudioPlayerManager.instance?.state?.value?.repeatMode ?: lastRepeatMode.next()
                            lastRepeatMode = currentRepeat
                            updateNotificationAndSession(
                                title = lastTitle,
                                artist = lastArtist,
                                album = lastAlbum,
                                uriString = lastUriString,
                                albumArtUriString = lastAlbumArtUriString,
                                songPath = lastSongPath,
                                isPlaying = lastIsPlaying,
                                positionMs = lastPositionMs,
                                durationMs = lastDurationMs
                            )
                        }
                    }
                }

                override fun onStop() {
                    AudioPlayerManager.instance?.pause()
                    stopSelf()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_PLAY_PAUSE -> {
                AudioPlayerManager.instance?.togglePlayPause()
            }
            ACTION_NEXT -> {
                AudioPlayerManager.instance?.playNext()
            }
            ACTION_PREVIOUS -> {
                AudioPlayerManager.instance?.playPrevious()
            }
            ACTION_STOP -> {
                AudioPlayerManager.instance?.pause()
                VolumeOverlayManager.getInstance(applicationContext).onPlaybackStateChanged(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_THEME -> {
                val accentColor = intent.getIntExtra(EXTRA_THEME_ACCENT_COLOR, 0)
                updateNotificationThemeInternal(if (accentColor != 0) accentColor else null)
            }
            ACTION_TOGGLE_SHUFFLE -> {
                AudioPlayerManager.instance?.toggleShuffle()
                val currentShuffle = AudioPlayerManager.instance?.state?.value?.shuffleEnabled ?: !lastShuffleEnabled
                lastShuffleEnabled = currentShuffle
                updateNotificationAndSession(
                    title = lastTitle,
                    artist = lastArtist,
                    album = lastAlbum,
                    uriString = lastUriString,
                    albumArtUriString = lastAlbumArtUriString,
                    songPath = lastSongPath,
                    isPlaying = lastIsPlaying,
                    positionMs = lastPositionMs,
                    durationMs = lastDurationMs
                )
            }
            ACTION_TOGGLE_REPEAT -> {
                AudioPlayerManager.instance?.cycleRepeatMode()
                val currentRepeat = AudioPlayerManager.instance?.state?.value?.repeatMode ?: lastRepeatMode.next()
                lastRepeatMode = currentRepeat
                updateNotificationAndSession(
                    title = lastTitle,
                    artist = lastArtist,
                    album = lastAlbum,
                    uriString = lastUriString,
                    albumArtUriString = lastAlbumArtUriString,
                    songPath = lastSongPath,
                    isPlaying = lastIsPlaying,
                    positionMs = lastPositionMs,
                    durationMs = lastDurationMs
                )
            }
            ACTION_SEEK_TO -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                AudioPlayerManager.instance?.seekTo(pos)
                updateNotificationThemeInternal(null)
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "Naasir Music"
                val artist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: "Unknown Artist"
                val album = intent.getStringExtra(EXTRA_SONG_ALBUM) ?: "Audio Library"
                val uriString = intent.getStringExtra(EXTRA_SONG_URI)
                val albumArtUriString = intent.getStringExtra(EXTRA_SONG_ALBUM_ART_URI)
                val songPath = intent.getStringExtra(EXTRA_SONG_PATH)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val positionMs = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 1L)
                val shuffleEnabled = intent.getBooleanExtra(
                    EXTRA_SHUFFLE_ENABLED,
                    AudioPlayerManager.instance?.state?.value?.shuffleEnabled ?: lastShuffleEnabled
                )
                val repeatModeStr = intent.getStringExtra(EXTRA_REPEAT_MODE)
                val repeatMode = repeatModeStr?.let {
                    try { RepeatMode.valueOf(it) } catch (_: Throwable) { null }
                } ?: AudioPlayerManager.instance?.state?.value?.repeatMode ?: lastRepeatMode

                lastShuffleEnabled = shuffleEnabled
                lastRepeatMode = repeatMode

                VolumeOverlayManager.getInstance(applicationContext).onPlaybackStateChanged(isPlaying)

                updateNotificationAndSession(
                    title = title,
                    artist = artist,
                    album = album,
                    uriString = uriString,
                    albumArtUriString = albumArtUriString,
                    songPath = songPath,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val isPlaying = AudioPlayerManager.instance?.state?.value?.isPlaying ?: lastIsPlaying
        Log.d(TAG, "onTaskRemoved: Recents task removed. isPlaying=$isPlaying")
        if (isPlaying) {
            // Keep playback and foreground service alive! Do NOT stop service when song is playing.
            Log.d(TAG, "onTaskRemoved: Music is playing, maintaining foreground playback.")
        } else {
            // Only stop if music is stopped or paused
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateNotificationThemeInternal(forcedAccentColor: Int?) {
        updateNotificationAndSession(
            title = lastTitle,
            artist = lastArtist,
            album = lastAlbum,
            uriString = lastUriString,
            albumArtUriString = lastAlbumArtUriString,
            songPath = lastSongPath,
            isPlaying = lastIsPlaying,
            positionMs = lastPositionMs,
            durationMs = lastDurationMs,
            forcedAccentColor = forcedAccentColor
        )
    }

    private fun updateNotificationAndSession(
        title: String,
        artist: String,
        album: String,
        uriString: String?,
        albumArtUriString: String? = lastAlbumArtUriString,
        songPath: String? = lastSongPath,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        forcedAccentColor: Int? = null
    ) {
        lastTitle = title
        lastArtist = artist
        lastAlbum = album
        lastUriString = uriString
        lastAlbumArtUriString = albumArtUriString
        lastSongPath = songPath
        lastIsPlaying = isPlaying
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        if (forcedAccentColor != null) {
            lastAccentColor = forcedAccentColor
        }

        val session = mediaSession

        val prefs = SettingsPreferencesManager(applicationContext)
        val activeTheme = prefs.loadTheme()
        val themePrimaryArgb = forcedAccentColor ?: lastAccentColor ?: activeTheme.primaryColorInt

        val edgeSettings = prefs.loadEdgeLighting()
        val playerState = AudioPlayerManager.instance?.state?.value

        val effectiveShuffle = playerState?.shuffleEnabled ?: lastShuffleEnabled
        val effectiveRepeat = playerState?.repeatMode ?: lastRepeatMode

        val notification = try {
            MediaNotificationManager.buildNotification(
                context = this,
                title = title,
                artist = artist,
                album = album,
                uriString = uriString,
                albumArtUriString = albumArtUriString,
                songPath = songPath,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = effectiveShuffle,
                repeatMode = effectiveRepeat,
                themeAccentColor = themePrimaryArgb,
                isEdgeLightingEnabled = edgeSettings.isEnabled,
                isNotificationEdgeLightingEnabled = edgeSettings.isNotificationBorderEnabled,
                mediaSession = session,
                useSystemMediaNotification = prefs.loadUseSystemMediaNotification()
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Error building custom notification, falling back to system style: ${t.message}", t)
            MediaNotificationManager.buildNotification(
                context = this,
                title = title,
                artist = artist,
                album = album,
                uriString = uriString,
                albumArtUriString = albumArtUriString,
                songPath = songPath,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = effectiveShuffle,
                repeatMode = effectiveRepeat,
                themeAccentColor = themePrimaryArgb,
                isEdgeLightingEnabled = false,
                isNotificationEdgeLightingEnabled = false,
                mediaSession = session,
                useSystemMediaNotification = true
            )
        }

        try {
            if (!isForeground || isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForeground = true
            }

            // Direct re-issue via NotificationManager ensures instant live update on status bar
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to startForeground/notify: ${t.message}", t)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        VolumeOverlayManager.getInstance(applicationContext).onPlaybackStateChanged(false)
        isServiceRunning = false
        isForeground = false
        try {
            val sharedPrefs = applicationContext.getSharedPreferences("naasir_music_user_settings", Context.MODE_PRIVATE)
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Error unregistering prefChangeListener: ${t.message}")
        }
        unregisterNoisyReceiver()
        releaseWakeLock()
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
    }
}
