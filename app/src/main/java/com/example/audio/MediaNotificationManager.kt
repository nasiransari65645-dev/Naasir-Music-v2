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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.SweepGradient
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.model.RepeatMode
import java.io.File
import java.io.InputStream

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

    private const val COLOR_GREY_INACTIVE = 0xFF757575.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_CARD_BACKGROUND = 0xFF101726.toInt()

    fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Naasir Music Playback",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
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
        themeAccentColor: Int,
        isEdgeLightingEnabled: Boolean,
        isNotificationEdgeLightingEnabled: Boolean,
        mediaSession: MediaSessionCompat?,
        useSystemMediaNotification: Boolean = false
    ): Notification {
        val packageName = context.packageName

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

        // 2. Artwork & Dynamic Edge Lighting Border Backgrounds
        val showEdgeLighting = isEdgeLightingEnabled && isNotificationEdgeLightingEnabled
        val artworkBitmap = loadArtworkBitmap(
            context = context,
            albumArtUriString = albumArtUriString,
            audioUriString = uriString,
            filePath = songPath,
            accentColor = themeAccentColor,
            hasRainbowBorder = showEdgeLighting
        )

        // Create lightweight background bitmaps with concentric soundwave rings & rainbow edge glow
        // Using compact dimensions scaled smoothly via fitXY to ensure safe IPC transfer under 1MB Binder limit
        val cardWidth = 480
        val heightCollapsed = 72
        val heightExpanded = 160
        val cornerRadius = 24f
        val strokeWidth = 4

        val bgSmall = createNotificationCardBackground(
            widthPx = cardWidth,
            heightPx = heightCollapsed,
            accentColor = themeAccentColor,
            hasRainbowEdge = showEdgeLighting,
            strokeWidthPx = strokeWidth,
            cornerRadiusPx = cornerRadius
        )
        val bgBig = createNotificationCardBackground(
            widthPx = cardWidth,
            heightPx = heightExpanded,
            accentColor = themeAccentColor,
            hasRainbowEdge = showEdgeLighting,
            strokeWidthPx = strokeWidth,
            cornerRadiusPx = cornerRadius
        )

        // Pre-render action icons as crisp, safely tinted Bitmaps
        val iconPrevBitmap = getTintedVectorBitmap(context, R.drawable.ic_skip_previous, COLOR_WHITE, 34)
        val iconPlayPauseBitmap = getTintedVectorBitmap(
            context,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
            themeAccentColor,
            38
        )
        val iconNextBitmap = getTintedVectorBitmap(context, R.drawable.ic_skip_next, COLOR_WHITE, 34)
        val shuffleColor = if (shuffleEnabled) themeAccentColor else COLOR_GREY_INACTIVE
        val repeatColor = if (repeatMode != RepeatMode.OFF) themeAccentColor else COLOR_GREY_INACTIVE

        val iconShuffleBitmap = getTintedVectorBitmap(
            context,
            if (shuffleEnabled) R.drawable.ic_shuffle else R.drawable.ic_shuffle_off,
            shuffleColor,
            30
        )
        val iconRepeatBitmap = getTintedVectorBitmap(
            context,
            when {
                repeatMode == RepeatMode.ONE -> R.drawable.ic_repeat_one
                repeatMode == RepeatMode.ALL -> R.drawable.ic_repeat
                else -> R.drawable.ic_repeat_off
            },
            repeatColor,
            30
        )

        // 3. Small (Collapsed) RemoteViews
        val smallViews = RemoteViews(packageName, R.layout.notification_collapsed).apply {
            setImageViewBitmap(R.id.notification_bg, bgSmall)
            setTextViewText(R.id.notification_title, title)
            setTextColor(R.id.notification_title, themeAccentColor)
            setTextViewText(R.id.notification_artist, artist)
            setImageViewBitmap(R.id.notification_artwork, artworkBitmap)

            // Compact controls: Shuffle, Previous, Play/Pause, Next, Repeat
            setOnClickPendingIntent(R.id.btn_shuffle, shufflePendingIntent)
            setImageViewBitmap(R.id.btn_shuffle, iconShuffleBitmap)
            setInt(R.id.btn_shuffle, "setColorFilter", shuffleColor)

            setOnClickPendingIntent(R.id.btn_prev, prevPendingIntent)
            setImageViewBitmap(R.id.btn_prev, iconPrevBitmap)

            setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)
            setImageViewBitmap(R.id.btn_play_pause, iconPlayPauseBitmap)

            setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
            setImageViewBitmap(R.id.btn_next, iconNextBitmap)

            setOnClickPendingIntent(R.id.btn_repeat, repeatPendingIntent)
            setImageViewBitmap(R.id.btn_repeat, iconRepeatBitmap)
            setInt(R.id.btn_repeat, "setColorFilter", repeatColor)
        }

        // 4. Big (Expanded) RemoteViews
        val bigViews = RemoteViews(packageName, R.layout.notification_expanded).apply {
            setImageViewBitmap(R.id.notification_bg, bgBig)
            setTextViewText(R.id.notification_app_name, "Naasir Music")
            setTextColor(R.id.notification_app_name, themeAccentColor)
            setTextViewText(R.id.notification_title, title)
            setTextColor(R.id.notification_title, themeAccentColor)
            setTextViewText(R.id.notification_artist, if (album.isNotBlank()) "$artist • $album" else artist)
            setImageViewBitmap(R.id.notification_artwork, artworkBitmap)

            // Control Row: [ Shuffle ]  [ Previous ]  [ Play / Pause ]  [ Next ]  [ Repeat ]
            setOnClickPendingIntent(R.id.btn_shuffle, shufflePendingIntent)
            setImageViewBitmap(R.id.btn_shuffle, iconShuffleBitmap)
            setInt(R.id.btn_shuffle, "setColorFilter", shuffleColor)

            setOnClickPendingIntent(R.id.btn_prev, prevPendingIntent)
            setImageViewBitmap(R.id.btn_prev, iconPrevBitmap)

            setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)
            setImageViewBitmap(R.id.btn_play_pause, iconPlayPauseBitmap)

            setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
            setImageViewBitmap(R.id.btn_next, iconNextBitmap)

            setOnClickPendingIntent(R.id.btn_repeat, repeatPendingIntent)
            setImageViewBitmap(R.id.btn_repeat, iconRepeatBitmap)
            setInt(R.id.btn_repeat, "setColorFilter", repeatColor)

            // Real-Time Progress Bar
            val progressVal = if (durationMs > 0) ((positionMs * 1000) / durationMs).toInt().coerceIn(0, 1000) else 0
            setProgressBar(R.id.notification_progress, 1000, progressVal, false)

            // Real-Time Touchable Seekbar Zones (Direct Touch-to-Seek)
            val seekTouchIds = intArrayOf(
                R.id.seek_touch_1, R.id.seek_touch_2, R.id.seek_touch_3, R.id.seek_touch_4, R.id.seek_touch_5,
                R.id.seek_touch_6, R.id.seek_touch_7, R.id.seek_touch_8, R.id.seek_touch_9, R.id.seek_touch_10
            )
            for (i in seekTouchIds.indices) {
                val fraction = (i + 0.5f) / 10f
                val targetMs = (durationMs * fraction).toLong().coerceIn(0L, durationMs.coerceAtLeast(1L))
                val seekIntent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_SEEK_TO
                    putExtra(EXTRA_SEEK_POSITION, targetMs)
                }
                val seekPendingIntent = PendingIntent.getService(
                    context,
                    200 + i,
                    seekIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(seekTouchIds[i], seekPendingIntent)
            }

            // Elapsed & total time labels (e.g. 02:10 / 04:09)
            setTextViewText(R.id.tv_elapsed_time, formatTime(positionMs))
            setTextViewText(R.id.tv_total_time, formatTime(durationMs))
        }

        // 5. Update MediaSessionCompat state & metadata
        if (mediaSession != null) {
            val stateActions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE

            val customShuffle = PlaybackStateCompat.CustomAction.Builder(
                ACTION_TOGGLE_SHUFFLE,
                if (shuffleEnabled) "Shuffle On" else "Shuffle Off",
                if (shuffleEnabled) R.drawable.ic_shuffle else R.drawable.ic_shuffle_off
            ).build()
            val customRepeat = PlaybackStateCompat.CustomAction.Builder(
                ACTION_TOGGLE_REPEAT,
                when (repeatMode) {
                    RepeatMode.ONE -> "Repeat One"
                    RepeatMode.ALL -> "Repeat All"
                    RepeatMode.OFF -> "Repeat Off"
                },
                when (repeatMode) {
                    RepeatMode.ONE -> R.drawable.ic_repeat_one
                    RepeatMode.ALL -> R.drawable.ic_repeat
                    RepeatMode.OFF -> R.drawable.ic_repeat_off
                }
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

        // Pre-build standard NotificationCompat.Action instances with tinted IconCompat
        val shuffleActionTitle = if (shuffleEnabled) "Shuffle On" else "Shuffle Off"
        val shuffleIconCompat = IconCompat.createWithBitmap(iconShuffleBitmap)
        val shuffleAction = NotificationCompat.Action.Builder(
            shuffleIconCompat,
            shuffleActionTitle,
            shufflePendingIntent
        ).build()

        val prevAction = NotificationCompat.Action.Builder(
            IconCompat.createWithBitmap(iconPrevBitmap),
            "Previous",
            prevPendingIntent
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            IconCompat.createWithBitmap(iconPlayPauseBitmap),
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            IconCompat.createWithBitmap(iconNextBitmap),
            "Next",
            nextPendingIntent
        ).build()

        val repeatActionTitle = when (repeatMode) {
            RepeatMode.ONE -> "Repeat One"
            RepeatMode.ALL -> "Repeat All"
            RepeatMode.OFF -> "Repeat Off"
        }
        val repeatIconCompat = IconCompat.createWithBitmap(iconRepeatBitmap)
        val repeatAction = NotificationCompat.Action.Builder(
            repeatIconCompat,
            repeatActionTitle,
            repeatPendingIntent
        ).build()

        // 6. Support for Android System Media Player (MediaStyle) when selected
        if (useSystemMediaNotification && mediaSession != null) {
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1, 2, 3)

            val systemBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow)
                .setContentTitle(title)
                .setContentText(if (album.isNotBlank()) "$artist • $album" else artist)
                .setSubText("Naasir Music")
                .setContentIntent(openAppPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(mediaStyle)
                .setOngoing(true)
                .addAction(shuffleAction)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .addAction(repeatAction)

            if (artworkBitmap != null) {
                systemBuilder.setLargeIcon(artworkBitmap)
            }

            return systemBuilder.build()
        }

        // 7. Custom Notification Player with DecoratedMediaCustomViewStyle (Full-width custom expanded size + MediaSession integration)
        val style = if (mediaSession != null) {
            androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1, 2, 3)
        } else {
            NotificationCompat.DecoratedCustomViewStyle()
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setContentTitle(title)
            .setContentText(if (album.isNotBlank()) "$artist • $album" else artist)
            .setSubText("Naasir Music")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setStyle(style)
            .setCustomContentView(smallViews)
            .setCustomBigContentView(bigViews)
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
     * Formats milliseconds into mm:ss display (e.g. 02:10)
     */
    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Safely rasterizes vector or shape drawables into a hardware bitmap with direct tinting,
     * completely avoiding RemoteViews reflection and NoSuchMethodException crashes on SystemUI.
     */
    fun getTintedVectorBitmap(
        context: Context,
        @DrawableRes resId: Int,
        tintColor: Int,
        sizeDp: Int = 36
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.colorFilter = android.graphics.PorterDuffColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN)
        drawable.setTint(tintColor)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Generates a sleek card background bitmap with:
     * - Rich dark slate card base
     * - Atmospheric concentric circular audio-wave / vinyl disc rings (matching reference screenshot)
     * - Radiant Rainbow Style Edge Glow border when edge lighting switch is toggled ON
     */
    fun createNotificationCardBackground(
        widthPx: Int,
        heightPx: Int,
        accentColor: Int,
        hasRainbowEdge: Boolean,
        strokeWidthPx: Int = 4,
        cornerRadiusPx: Float = 24f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val halfStroke = strokeWidthPx / 2f
        val rect = RectF(
            halfStroke,
            halfStroke,
            widthPx.toFloat() - halfStroke,
            heightPx.toFloat() - halfStroke
        )

        // 1. Dark sleek card surface base
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BACKGROUND
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)

        // 2. Concentric circular soundwave / vinyl disc rings in the background
        val ringCenterX = widthPx * 0.76f
        val ringCenterY = heightPx * 0.50f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(48, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            strokeWidth = 3f * (heightPx / 150f).coerceAtLeast(1.5f)
        }
        canvas.drawCircle(ringCenterX, ringCenterY, heightPx * 0.95f, ringPaint)

        ringPaint.strokeWidth = 2.2f * (heightPx / 150f).coerceAtLeast(1.5f)
        ringPaint.color = Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        canvas.drawCircle(ringCenterX, ringCenterY, heightPx * 0.65f, ringPaint)

        ringPaint.strokeWidth = 1.6f * (heightPx / 150f).coerceAtLeast(1.5f)
        ringPaint.color = Color.argb(22, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        canvas.drawCircle(ringCenterX, ringCenterY, heightPx * 0.35f, ringPaint)

        // Left subtle ambient wave ring
        val leftRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(25, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            strokeWidth = 2f
        }
        canvas.drawCircle(widthPx * 0.12f, ringCenterY, heightPx * 0.70f, leftRingPaint)

        // 3. EYE-CATCHING GLOWING BORDER (ALWAYS VISIBLE!)
        if (hasRainbowEdge) {
            val rainbowColors = intArrayOf(
                0xFFFF0055.toInt(), // Vibrant Red-Pink
                0xFFFF7700.toInt(), // Warm Orange
                0xFFFFDD00.toInt(), // Golden Yellow
                0xFF00FF66.toInt(), // Neon Green
                0xFF00E5FF.toInt(), // Electric Cyan
                0xFF3B82F6.toInt(), // Deep Sky Blue
                0xFFA855F7.toInt(), // Royal Purple
                0xFFFF00AA.toInt(), // Hot Magenta
                0xFFFF0055.toInt()  // Seamless loop to Red-Pink
            )
            val rainbowPositions = floatArrayOf(
                0.0f, 0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f, 0.875f, 1.0f
            )

            val sweepShader = SweepGradient(
                widthPx / 2f,
                heightPx / 2f,
                rainbowColors,
                rainbowPositions
            )

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx.toFloat()
                shader = sweepShader
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
        } else {
            // Glowing neon accent border - crisp & vibrant
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx.toFloat()
                color = accentColor
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)

            // Inner subtle specular highlight
            val innerRect = RectF(
                halfStroke + 1.2f,
                halfStroke + 1.2f,
                widthPx.toFloat() - (halfStroke + 1.2f),
                heightPx.toFloat() - (halfStroke + 1.2f)
            )
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = Color.argb(90, 255, 255, 255)
            }
            canvas.drawRoundRect(innerRect, (cornerRadiusPx - 1.2f).coerceAtLeast(0f), (cornerRadiusPx - 1.2f).coerceAtLeast(0f), innerPaint)
        }

        return bitmap
    }

    /**
     * Clips bitmap with smooth rounded corners so album art renders cleanly inside the notification card.
     */
    fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float, borderAccentColor: Int? = null): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        if (borderAccentColor != null) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = borderAccentColor
            }
            val borderRect = RectF(1.25f, 1.25f, bitmap.width.toFloat() - 1.25f, bitmap.height.toFloat() - 1.25f)
            canvas.drawRoundRect(borderRect, cornerRadiusPx, cornerRadiusPx, strokePaint)
        }
        return output
    }

    /**
     * Loads high-res artwork from URI / embedded picture / thumbnail with smooth rounded corners & fine border,
     * or generates a clean dark neon vinyl disc fallback.
     */
    fun loadArtworkBitmap(
        context: Context,
        albumArtUriString: String?,
        audioUriString: String?,
        filePath: String?,
        accentColor: Int,
        hasRainbowBorder: Boolean = false
    ): Bitmap? {
        val targetSize = 180

        // 1. Try albumArtUriString from MediaStore
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
                        val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                        return getRoundedCornerBitmap(scaled, 18f, accentColor)
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
                        val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                        return getRoundedCornerBitmap(scaled, 18f, accentColor)
                    }
                }
            }
            mmr.release()
        } catch (_: Throwable) {}

        // 3. Try ContentResolver thumbnail on API 29+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !audioUriString.isNullOrBlank()) {
            try {
                val bmp = context.contentResolver.loadThumbnail(
                    Uri.parse(audioUriString),
                    android.util.Size(targetSize, targetSize),
                    null
                )
                if (bmp != null) {
                    val scaled = Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
                    return getRoundedCornerBitmap(scaled, 18f, accentColor)
                }
            } catch (_: Throwable) {}
        }

        // 4. Premium stylized vinyl disc artwork fallback (Matching neon theme)
        return try {
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Dark sleek base with rounded corners
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat()), 18f, 18f, paint)

            // Vinyl outer groove ring
            paint.color = accentColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 2.5f, paint)

            // Concentric soundwave track rings
            paint.strokeWidth = 2f
            paint.color = Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 3.2f, paint)

            paint.strokeWidth = 1.5f
            paint.color = Color.argb(70, 255, 255, 255)
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 4.4f, paint)

            // Center vinyl neon spindle
            paint.style = Paint.Style.FILL
            paint.color = accentColor
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 8f, paint)

            paint.color = Color.WHITE
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 18f, paint)

            // Rounded border outline
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = accentColor
            }
            canvas.drawRoundRect(RectF(1.25f, 1.25f, targetSize - 1.25f, targetSize - 1.25f), 18f, 18f, borderPaint)

            bitmap
        } catch (t: Throwable) {
            null
        }
    }
}
