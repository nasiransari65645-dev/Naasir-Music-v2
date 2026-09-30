package com.example.audio

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.RepeatMode
import java.io.File

/**
 * Clean Standard Android MediaStyle Notification Manager
 * Provides standard Android Media Notification player with standard actions (Shuffle, Previous, Play/Pause, Next, Repeat)
 * and no extra non-standard visual overlays.
 */
object MediaNotificationManager {

    const val NOTIFICATION_ID = 9101
    const val CHANNEL_ID = "naasir_music_playback_channel_v2"

    const val ACTION_PLAY_PAUSE = "com.example.audio.ACTION_PLAY_PAUSE"
    const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.example.audio.ACTION_PREVIOUS"
    const val ACTION_STOP = "com.example.audio.ACTION_STOP"
    const val ACTION_UPDATE = "com.example.audio.ACTION_UPDATE"
    const val ACTION_UPDATE_THEME = "com.example.audio.ACTION_UPDATE_THEME"
    const val ACTION_TOGGLE_SHUFFLE = "com.example.audio.ACTION_TOGGLE_SHUFFLE"
    const val ACTION_TOGGLE_REPEAT = "com.example.audio.ACTION_TOGGLE_REPEAT"
    const val ACTION_SEEK_TO = "com.example.audio.ACTION_SEEK_TO"
    const val EXTRA_SEEK_POSITION = "extra_seek_position"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Naasir Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Naasir Music Pro background playback & media controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Builds a standard Android MediaStyle Notification Player.
     */
    fun buildNotification(
        context: Context,
        title: String,
        artist: String,
        album: String,
        uriString: String?,
        albumArtUriString: String? = null,
        songPath: String? = null,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        shuffleEnabled: Boolean,
        repeatMode: RepeatMode,
        themeAccentColor: Int = 0,
        isEdgeLightingEnabled: Boolean = false,
        isNotificationEdgeLightingEnabled: Boolean = false,
        mediaSession: MediaSessionCompat?,
        useSystemMediaNotification: Boolean = true
    ): Notification {
        // 1. PendingIntents
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            context, 101, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(
            context, 102, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            context, 103, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shuffleIntent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_TOGGLE_SHUFFLE }
        val shufflePendingIntent = PendingIntent.getService(
            context, 104, shuffleIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val repeatIntent = Intent(context, MediaPlaybackService::class.java).apply { action = ACTION_TOGGLE_REPEAT }
        val repeatPendingIntent = PendingIntent.getService(
            context, 105, repeatIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Load clean standard album artwork
        val artworkBitmap = loadArtworkBitmap(
            context = context,
            albumArtUriString = albumArtUriString,
            audioUriString = uriString,
            filePath = songPath
        )

        // 3. Shuffle & Repeat vector drawables for clear on/off visual state
        val shuffleDrawableRes = if (shuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
        val repeatDrawableRes = if (repeatMode != RepeatMode.OFF) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off

        // 4. Update MediaSessionCompat state & metadata for system-wide lockscreen/status-bar integration
        if (mediaSession != null) {
            val stateActions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                    PlaybackStateCompat.ACTION_STOP

            val customShuffle = PlaybackStateCompat.CustomAction.Builder(
                ACTION_TOGGLE_SHUFFLE,
                if (shuffleEnabled) "Shuffle On" else "Shuffle Off",
                shuffleDrawableRes
            ).build()
            val customRepeat = PlaybackStateCompat.CustomAction.Builder(
                ACTION_TOGGLE_REPEAT,
                when (repeatMode) {
                    RepeatMode.ONE -> "Repeat One"
                    RepeatMode.ALL -> "Repeat All"
                    RepeatMode.OFF -> "Repeat Off"
                },
                repeatDrawableRes
            ).build()

            val playbackState = PlaybackStateCompat.Builder()
                .setActions(stateActions)
                .addCustomAction(customShuffle)
                .addCustomAction(customRepeat)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    positionMs,
                    if (isPlaying) 1.0f else 0.0f
                )
                .build()
            mediaSession.setPlaybackState(playbackState)

            mediaSession.setShuffleMode(
                if (shuffleEnabled) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE
            )
            mediaSession.setRepeatMode(
                when (repeatMode) {
                    RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
                    RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
                    RepeatMode.OFF -> PlaybackStateCompat.REPEAT_MODE_NONE
                }
            )

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)

            if (artworkBitmap != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artworkBitmap)
            }
            mediaSession.setMetadata(metadataBuilder.build())
        }

        // 5. Standard Media Notification Actions
        val shuffleAction = NotificationCompat.Action(
            shuffleDrawableRes,
            if (shuffleEnabled) "Shuffle On" else "Shuffle Off",
            shufflePendingIntent
        )

        val prevAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous,
            "Previous",
            prevPendingIntent
        )

        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next,
            "Next",
            nextPendingIntent
        )

        val repeatAction = NotificationCompat.Action(
            repeatDrawableRes,
            when (repeatMode) {
                RepeatMode.ONE -> "Repeat One"
                RepeatMode.ALL -> "Repeat All"
                RepeatMode.OFF -> "Repeat Off"
            },
            repeatPendingIntent
        )

        // 6. Standard MediaStyle layout: compact view displays indices 1 (prev), 2 (play/pause), 3 (next)
        val mediaStyle: androidx.media.app.NotificationCompat.MediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
        if (mediaSession != null) {
            mediaStyle.setMediaSession(mediaSession.sessionToken)
        }
        mediaStyle.setShowActionsInCompactView(1, 2, 3)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setContentTitle(title)
            .setContentText(if (album.isNotBlank()) "$artist • $album" else artist)
            .setSubText("Naasir Music")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(mediaStyle)
            .setOngoing(isPlaying)
            .addAction(shuffleAction)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(repeatAction)

        if (artworkBitmap != null) {
            builder.setLargeIcon(artworkBitmap)
        }

        return builder.build()
    }

    /**
     * Loads clean standard artwork for the notification player.
     */
    fun loadArtworkBitmap(
        context: Context,
        albumArtUriString: String?,
        audioUriString: String?,
        filePath: String?
    ): Bitmap? {
        val targetSize = 256

        // 1. Try albumArtUriString from MediaStore / local storage
        if (!albumArtUriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(albumArtUriString)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                if (options.outWidth > 0 && options.outHeight > 0) {
                    var sample = 1
                    while (options.outWidth / (sample * 2) >= targetSize && options.outHeight / (sample * 2) >= targetSize) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOpts)
                    }
                    if (raw != null) {
                        return Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. Try MediaMetadataRetriever embedded picture from audio file
        try {
            val mmr = MediaMetadataRetriever()
            var dataSourceSet = false
            if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                mmr.setDataSource(filePath)
                dataSourceSet = true
            } else if (!audioUriString.isNullOrBlank()) {
                mmr.setDataSource(context, Uri.parse(audioUriString))
                dataSourceSet = true
            }
            if (dataSourceSet) {
                val picture = mmr.embeddedPicture
                if (picture != null) {
                    val raw = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    if (raw != null) {
                        mmr.release()
                        return Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                    }
                }
            }
            mmr.release()
        } catch (_: Throwable) {}

        // 3. Try ContentResolver loadThumbnail on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !audioUriString.isNullOrBlank()) {
            try {
                val bmp = context.contentResolver.loadThumbnail(
                    Uri.parse(audioUriString),
                    android.util.Size(targetSize, targetSize),
                    null
                )
                if (bmp != null) {
                    return Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
                }
            } catch (_: Throwable) {}
        }

        // 4. Clean standard fallback album art (neutral dark slate disc)
        return try {
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(26, 32, 44) // Slate 800
                style = Paint.Style.FILL
            }
            canvas.drawRect(RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat()), paint)

            paint.color = Color.rgb(74, 85, 104)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 2.6f, paint)

            paint.strokeWidth = 2f
            paint.color = Color.rgb(113, 128, 150)
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 4f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(45, 55, 72)
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 8f, paint)

            paint.color = Color.WHITE
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 24f, paint)

            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}
