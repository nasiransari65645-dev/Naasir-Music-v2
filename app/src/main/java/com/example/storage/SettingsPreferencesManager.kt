package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.PlayerState
import com.example.model.AppThemePreset
import com.example.model.AutoRotateDirection
import com.example.model.DpsProfile
import com.example.model.EdgeLightingSettings
import com.example.model.EdgeLightingStyle
import com.example.model.EqualizerState
import com.example.model.SongSortOption
import com.example.model.SpatialEnvironment
import com.example.model.VolumeGaugePosition
import com.example.model.VolumeGaugeSettings
import com.example.model.VolumeGaugeTheme

/**
 * Manages persistent user preferences including audio equalizer, 3D spatial settings,
 * UI theme, edge lighting parameters, favorites, and storage permissions.
 */
class SettingsPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "naasir_music_user_settings"

        private const val KEY_THEME = "key_selected_theme"
        private const val KEY_THEME_PALETTE_ID = "key_theme_palette_id"
        private const val KEY_CUSTOM_THEME_ENABLED = "key_custom_theme_enabled"
        private const val KEY_CUSTOM_COLOR_MAP = "key_custom_color_map"
        private const val KEY_PROGRESS_BAR_COLOR = "key_progress_bar_color"
        private const val KEY_PROGRESS_BAR_ANIM = "key_progress_bar_anim"
        private const val KEY_STORAGE_PERMISSION = "key_storage_permission_granted"

        // Edge Lighting
        private const val KEY_EDGE_ENABLED = "key_edge_enabled"
        private const val KEY_EDGE_STYLE = "key_edge_style"
        private const val KEY_EDGE_SHAPE = "key_edge_shape"
        private const val KEY_EDGE_STROKE = "key_edge_stroke"
        private const val KEY_EDGE_SPEED = "key_edge_speed"
        private const val KEY_EDGE_REACTIVE = "key_edge_reactive"
        private const val KEY_EDGE_CORNER = "key_edge_corner"
        private const val KEY_EDGE_NOTIFICATION_BORDER = "key_edge_notification_border"
        private const val KEY_USE_SYSTEM_MEDIA_NOTIFICATION = "key_use_system_media_notification"

        // Equalizer
        private const val KEY_EQ_ENABLED = "key_eq_enabled"
        private const val KEY_EQ_PRESET = "key_eq_preset"
        private const val KEY_EQ_GAINS = "key_eq_gains"
        private const val KEY_EQ_PREAMP = "key_eq_preamp"
        private const val KEY_EQ_BASS_PUNCH = "key_eq_bass_punch"
        private const val KEY_EQ_TREBLE = "key_eq_treble"

        // 3D Spatial
        private const val KEY_SPATIAL_AUTO_ROTATE = "key_spatial_auto_rotate"
        private const val KEY_SPATIAL_SPEED = "key_spatial_speed"
        private const val KEY_ROTATION_SPEED_MULT = "key_rotation_speed_mult"
        private const val KEY_SPATIAL_DIRECTION = "key_spatial_direction"
        private const val KEY_SPATIAL_WIDTH = "key_spatial_width"
        private const val KEY_SPATIAL_VIRTUALIZER = "key_spatial_virtualizer"
        private const val KEY_SPATIAL_VIRT_STRENGTH = "key_spatial_virt_strength"
        private const val KEY_VIRTUALIZER_MODE = "key_virtualizer_mode"
        private const val KEY_SPATIAL_BASS = "key_spatial_bass"
        private const val KEY_SPATIAL_BASS_STRENGTH = "key_spatial_bass_strength"
        private const val KEY_SPATIAL_ENV = "key_spatial_env"
        private const val KEY_REVERB_MASTER_ENABLED = "key_reverb_master_enabled"
        private const val KEY_REVERB_WET_DRY = "key_reverb_wet_dry"
        private const val KEY_REVERB_DECAY = "key_reverb_decay"
        private const val KEY_DPS_MASTER_ENABLED = "key_dps_master_enabled"
        private const val KEY_DPS_PROFILE = "key_dps_profile"
        private const val KEY_DPS_BASS_BOOST = "key_dps_bass_boost"
        private const val KEY_DPS_CLARITY = "key_dps_clarity"
        private const val KEY_SPATIAL_HEAD_SHADOW = "key_spatial_head_shadow"
        private const val KEY_SPATIAL_WIDENER = "key_spatial_widener"

        // Stereo Delay / Echo Effect Module
        private const val KEY_DELAY_ENABLED = "key_delay_enabled"
        private const val KEY_DELAY_TIME_MS = "key_delay_time_ms"
        private const val KEY_DELAY_FEEDBACK_PCT = "key_delay_feedback_pct"
        private const val KEY_DELAY_MIX_PCT = "key_delay_mix_pct"

        // Favorites
        private const val KEY_FAVORITES = "key_favorite_ids"

        // Last UI and Playback State (Request 3)
        private const val KEY_LAST_TAB = "key_last_selected_tab"
        private const val KEY_LAST_SONG_ID = "key_last_song_id"
        private const val KEY_LAST_SONG_POS = "key_last_song_pos"
        private const val KEY_LAST_SONG_IS_DEMO = "key_last_song_is_demo"
        private const val KEY_SHUFFLE = "key_last_shuffle"
        private const val KEY_REPEAT = "key_last_repeat"
        private const val KEY_VISUALIZER_ENABLED = "key_visualizer_enabled"
        private const val KEY_VISUALIZER_EFFECT_ID = "key_visualizer_effect_id"
        private const val KEY_CUSTOM_VISUALIZER_TEXT = "key_custom_visualizer_text"
        const val DEFAULT_VISUALIZER_TEXT = "Naasir"

        // Expanded Persistence: Volume, Branding, Playback, Screen, Misc
        private const val KEY_APP_VOLUME = "key_app_volume"
        private const val KEY_SHOW_VISUALIZER_TEXT = "show_visualizer_text"
        private const val KEY_VISUALIZER_TEXT_COLOR = "visualizer_text_color"
        private const val KEY_GAPLESS_PLAYBACK = "key_gapless_playback"
        private const val KEY_CROSSFADE_SEC = "key_crossfade_sec"
        private const val KEY_AUTOPLAY_HEADSET = "key_autoplay_headset"
        private const val KEY_NOTIFICATION_CONTROLS = "key_notification_controls"
        private const val KEY_KEEP_SCREEN_ON = "key_keep_screen_on"
        private const val KEY_SHAKE_TO_SKIP = "key_shake_to_skip"
        private const val KEY_SLEEP_TIMER_MINUTES = "key_sleep_timer_minutes"

        // Detailed Settings Screen Preferences
        private const val KEY_SPINNING_VINYL = "key_spinning_vinyl"
        private const val KEY_ALBUM_ART_STYLE = "key_album_art_style"
        private const val KEY_SEEK_INTERVAL_SEC = "key_seek_interval_sec"
        private const val KEY_AUTO_PAUSE_HEADSET = "key_auto_pause_headset"
        private const val KEY_LOCK_SCREEN_CONTROLS = "key_lock_screen_controls"
        private const val KEY_SHAKE_SENSITIVITY = "key_shake_sensitivity"
        private const val KEY_VOLUME_BUTTONS_SKIP = "key_volume_buttons_skip"
        private const val KEY_ANDROID_AUTO_ENABLED = "key_android_auto_enabled"
        private const val KEY_ANDROID_AUTO_AUTOPLAY = "key_android_auto_autoplay"
        private const val KEY_PRO_STATUS = "key_pro_status"
        private const val KEY_PLAYBACK_SPEED = "key_playback_speed"
        private const val KEY_PLAYBACK_PITCH = "key_playback_pitch"
        private const val KEY_SONG_SORT_OPTION = "key_song_sort_option"
        private const val KEY_VOL_GAUGE_STEP = "key_vol_gauge_step"
        private const val KEY_VOL_GAUGE_POSITION = "key_vol_gauge_position"
        private const val KEY_VOL_GAUGE_THEME = "key_vol_gauge_theme"
        private const val KEY_VOL_GAUGE_HAPTIC = "key_vol_gauge_haptic"
        private const val KEY_VOL_GAUGE_DELAY = "key_vol_gauge_delay"
        private const val KEY_VOL_GAUGE_OVERLAY = "key_vol_gauge_overlay"
        private const val KEY_APP_THEME_MODE = "key_app_theme_mode"
    }

    // --- APP THEME MODE (DARK / LIGHT / SYSTEM) ---
    fun saveAppThemeMode(mode: com.example.model.AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME_MODE, mode.name).apply()
    }

    fun loadAppThemeMode(): com.example.model.AppThemeMode {
        val savedName = prefs.getString(KEY_APP_THEME_MODE, null) ?: return com.example.model.AppThemeMode.DARK_OLED
        return try {
            com.example.model.AppThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            com.example.model.AppThemeMode.DARK_OLED
        }
    }

    // --- SONG SORTING ---
    fun saveSongSortOption(option: SongSortOption) {
        prefs.edit().putString(KEY_SONG_SORT_OPTION, option.name).apply()
    }

    fun loadSongSortOption(): SongSortOption {
        val savedName = prefs.getString(KEY_SONG_SORT_OPTION, null) ?: return SongSortOption.DATE_ADDED_DESC
        return try {
            SongSortOption.valueOf(savedName)
        } catch (e: Exception) {
            SongSortOption.DATE_ADDED_DESC
        }
    }

    // --- PLAYBACK SPEED & PITCH ---
    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed.coerceIn(0.25f, 3.0f)).apply()
    }

    fun loadPlaybackSpeed(): Float {
        return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
    }

    fun savePlaybackPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_PITCH, pitch.coerceIn(0.5f, 2.0f)).apply()
    }

    fun loadPlaybackPitch(): Float {
        return prefs.getFloat(KEY_PLAYBACK_PITCH, 1.0f)
    }

    // --- APP VOLUME / GAIN ---
    fun saveAppVolume(volume: Float) {
        prefs.edit().putFloat(KEY_APP_VOLUME, volume.coerceIn(0f, 1f)).apply()
    }

    fun loadAppVolume(): Float {
        return prefs.getFloat(KEY_APP_VOLUME, 1.0f)
    }

    // --- SHUFFLE & REPEAT DEDICATED PERSISTENCE ---
    fun saveShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
    }

    fun saveRepeat(repeatMode: String) {
        prefs.edit().putString(KEY_REPEAT, repeatMode).apply()
    }

    // --- VISUALIZER BRANDING & COLOR ---
    fun saveShowVisualizerText(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_VISUALIZER_TEXT, show).apply()
    }

    fun loadShowVisualizerText(): Boolean {
        return prefs.getBoolean(KEY_SHOW_VISUALIZER_TEXT, true)
    }

    fun saveVisualizerTextColor(colorHex: Long) {
        prefs.edit().putLong(KEY_VISUALIZER_TEXT_COLOR, colorHex).apply()
    }

    fun loadVisualizerTextColor(): Long {
        return prefs.getLong(KEY_VISUALIZER_TEXT_COLOR, 0xFF00E5FF)
    }

    // --- ADVANCED PLAYBACK OPTIONS ---
    fun saveGaplessPlayback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAPLESS_PLAYBACK, enabled).apply()
    }

    fun loadGaplessPlayback(): Boolean {
        return prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true)
    }

    fun saveCrossfadeSec(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE_SEC, seconds.coerceIn(0, 10)).apply()
    }

    fun loadCrossfadeSec(): Int {
        return prefs.getInt(KEY_CROSSFADE_SEC, 5)
    }

    fun saveAutoplayHeadset(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY_HEADSET, enabled).apply()
    }

    fun loadAutoplayHeadset(): Boolean {
        return prefs.getBoolean(KEY_AUTOPLAY_HEADSET, false)
    }

    // --- SCREEN & NOTIFICATION ---
    fun saveNotificationControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_CONTROLS, enabled).apply()
    }

    fun loadNotificationControls(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_CONTROLS, true)
    }

    fun saveUseSystemMediaNotification(useSystem: Boolean) {
        prefs.edit().putBoolean(KEY_USE_SYSTEM_MEDIA_NOTIFICATION, useSystem).apply()
    }

    fun loadUseSystemMediaNotification(): Boolean {
        return prefs.getBoolean(KEY_USE_SYSTEM_MEDIA_NOTIFICATION, true)
    }

    fun saveKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun loadKeepScreenOn(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    // --- MISC (SLEEP TIMER, GESTURES) ---
    fun saveShakeToSkip(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_TO_SKIP, enabled).apply()
    }

    fun loadShakeToSkip(): Boolean {
        return prefs.getBoolean(KEY_SHAKE_TO_SKIP, false)
    }

    fun saveSleepTimerMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER_MINUTES, minutes).apply()
    }

    fun loadSleepTimerMinutes(): Int {
        return prefs.getInt(KEY_SLEEP_TIMER_MINUTES, 0)
    }

    // --- PLAYER SUB-SCREEN PREFERENCES ---
    fun saveSpinningVinyl(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPINNING_VINYL, enabled).apply()
    }

    fun loadSpinningVinyl(): Boolean {
        return prefs.getBoolean(KEY_SPINNING_VINYL, true)
    }

    fun saveAlbumArtStyle(style: String) {
        prefs.edit().putString(KEY_ALBUM_ART_STYLE, style).apply()
    }

    fun loadAlbumArtStyle(): String {
        val saved = prefs.getString(KEY_ALBUM_ART_STYLE, "Vinyl Record") ?: "Vinyl Record"
        return if (saved == "Full Screen" || saved.isBlank()) "Vinyl Record" else saved
    }

    fun saveSeekIntervalSec(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_INTERVAL_SEC, seconds).apply()
    }

    fun loadSeekIntervalSec(): Int {
        return prefs.getInt(KEY_SEEK_INTERVAL_SEC, 10)
    }

    // --- PLAYBACK SUB-SCREEN PREFERENCES ---
    fun saveAutoPauseHeadset(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PAUSE_HEADSET, enabled).apply()
    }

    fun loadAutoPauseHeadset(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PAUSE_HEADSET, true)
    }

    // --- SCREEN SUB-SCREEN PREFERENCES ---
    fun saveLockScreenControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_SCREEN_CONTROLS, enabled).apply()
    }

    fun loadLockScreenControls(): Boolean {
        return prefs.getBoolean(KEY_LOCK_SCREEN_CONTROLS, true)
    }

    // --- MISCELLANEOUS SUB-SCREEN PREFERENCES ---
    fun saveShakeSensitivity(sensitivity: String) {
        prefs.edit().putString(KEY_SHAKE_SENSITIVITY, sensitivity).apply()
    }

    fun loadShakeSensitivity(): String {
        return prefs.getString(KEY_SHAKE_SENSITIVITY, "Medium") ?: "Medium"
    }

    fun saveVolumeButtonsSkip(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOLUME_BUTTONS_SKIP, enabled).apply()
    }

    fun loadVolumeButtonsSkip(): Boolean {
        return prefs.getBoolean(KEY_VOLUME_BUTTONS_SKIP, false)
    }

    // --- ANDROID AUTO SUB-SCREEN PREFERENCES ---
    fun saveAndroidAutoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANDROID_AUTO_ENABLED, enabled).apply()
    }

    fun loadAndroidAutoEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANDROID_AUTO_ENABLED, true)
    }

    fun saveAndroidAutoAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANDROID_AUTO_AUTOPLAY, enabled).apply()
    }

    fun loadAndroidAutoAutoplay(): Boolean {
        return prefs.getBoolean(KEY_ANDROID_AUTO_AUTOPLAY, false)
    }

    // --- PURCHASE / PRO STATUS ---
    fun saveProStatus(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_PRO_STATUS, isPro).apply()
    }

    fun loadProStatus(): Boolean {
        return prefs.getBoolean(KEY_PRO_STATUS, true)
    }

    fun resetAllSettings() {
        prefs.edit().clear().apply()
    }

    // --- THEME ---
    fun saveTheme(theme: com.example.model.AppNaturalTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun loadTheme(): com.example.model.AppNaturalTheme {
        val name = prefs.getString(KEY_THEME, com.example.model.AppNaturalTheme.BLUE.name)
        return com.example.model.AppNaturalTheme.fromNameSafe(name)
    }

    fun saveSelectedThemePaletteId(id: String) {
        prefs.edit().putString(KEY_THEME_PALETTE_ID, id).apply()
    }

    fun loadSelectedThemePaletteId(): String {
        return prefs.getString(KEY_THEME_PALETTE_ID, "blue") ?: "blue"
    }

    fun saveCustomThemeSettings(settings: com.example.model.CustomThemeSettings) {
        prefs.edit()
            .putBoolean(KEY_CUSTOM_THEME_ENABLED, settings.isEnabled)
            .putString(KEY_THEME, settings.themePreset.name)
            .putString(KEY_PROGRESS_BAR_COLOR, settings.progressColor.name)
            .putString(KEY_PROGRESS_BAR_ANIM, settings.progressAnimation.name)
            .putString(KEY_CUSTOM_COLOR_MAP, settings.colorMap.serialize())
            .apply()
    }

    fun loadCustomThemeSettings(): com.example.model.CustomThemeSettings {
        val enabled = prefs.getBoolean(KEY_CUSTOM_THEME_ENABLED, false)
        val themeName = prefs.getString(KEY_THEME, com.example.model.AppNaturalTheme.BLUE.name)
        val themePreset = com.example.model.AppNaturalTheme.fromNameSafe(themeName)
        val colorName = prefs.getString(KEY_PROGRESS_BAR_COLOR, com.example.model.ProgressBarColorPreset.NEON_CYAN.name)
        val progressColor = try {
            com.example.model.ProgressBarColorPreset.valueOf(colorName ?: com.example.model.ProgressBarColorPreset.NEON_CYAN.name)
        } catch (t: Throwable) {
            com.example.model.ProgressBarColorPreset.NEON_CYAN
        }
        val animName = prefs.getString(KEY_PROGRESS_BAR_ANIM, com.example.model.ProgressBarAnimation.GLOW_PULSE.name)
        val progressAnim = try {
            com.example.model.ProgressBarAnimation.valueOf(animName ?: com.example.model.ProgressBarAnimation.GLOW_PULSE.name)
        } catch (t: Throwable) {
            com.example.model.ProgressBarAnimation.GLOW_PULSE
        }
        val colorMapStr = prefs.getString(KEY_CUSTOM_COLOR_MAP, null)
        val colorMap = com.example.model.MultiElementColorMap.deserialize(colorMapStr)
            ?: com.example.model.MultiElementColorMap.fromThemePreset(themePreset)

        return com.example.model.CustomThemeSettings(
            isEnabled = enabled,
            themePreset = themePreset,
            progressColor = progressColor,
            progressAnimation = progressAnim,
            colorMap = colorMap
        )
    }

    // --- STORAGE PERMISSION STATUS ---
    fun saveStoragePermission(granted: Boolean) {
        prefs.edit().putBoolean(KEY_STORAGE_PERMISSION, granted).apply()
    }

    fun loadStoragePermission(): Boolean {
        return prefs.getBoolean(KEY_STORAGE_PERMISSION, false)
    }

    // --- EDGE LIGHTING ---
    fun saveEdgeLighting(settings: EdgeLightingSettings) {
        prefs.edit()
            .putBoolean(KEY_EDGE_ENABLED, settings.isEnabled)
            .putBoolean(KEY_EDGE_NOTIFICATION_BORDER, settings.isNotificationBorderEnabled)
            .putString(KEY_EDGE_STYLE, settings.style.name)
            .putString(KEY_EDGE_SHAPE, settings.shape.name)
            .putFloat(KEY_EDGE_STROKE, settings.strokeWidthDp)
            .putFloat(KEY_EDGE_SPEED, settings.animationSpeedSec)
            .putBoolean(KEY_EDGE_REACTIVE, settings.musicReactive)
            .putFloat(KEY_EDGE_CORNER, settings.cornerRadiusDp)
            .apply()
    }

    fun loadEdgeLighting(): EdgeLightingSettings {
        val enabled = prefs.getBoolean(KEY_EDGE_ENABLED, false)
        val notifBorder = prefs.getBoolean(KEY_EDGE_NOTIFICATION_BORDER, true)
        val styleName = prefs.getString(KEY_EDGE_STYLE, EdgeLightingStyle.CYBER_PULSE.name)
        val style = try {
            EdgeLightingStyle.valueOf(styleName ?: EdgeLightingStyle.CYBER_PULSE.name)
        } catch (t: Throwable) {
            EdgeLightingStyle.CYBER_PULSE
        }
        val shapeName = prefs.getString(KEY_EDGE_SHAPE, com.example.model.EdgeLightingShape.SOLID_LINE.name)
        val shape = try {
            com.example.model.EdgeLightingShape.valueOf(shapeName ?: com.example.model.EdgeLightingShape.SOLID_LINE.name)
        } catch (t: Throwable) {
            com.example.model.EdgeLightingShape.SOLID_LINE
        }
        val stroke = prefs.getFloat(KEY_EDGE_STROKE, 4.0f)
        val speed = prefs.getFloat(KEY_EDGE_SPEED, 3.5f)
        val reactive = prefs.getBoolean(KEY_EDGE_REACTIVE, true)
        val corner = prefs.getFloat(KEY_EDGE_CORNER, 36.0f)

        return EdgeLightingSettings(
            isEnabled = enabled,
            isNotificationBorderEnabled = notifBorder,
            style = style,
            strokeWidthDp = stroke,
            animationSpeedSec = speed,
            musicReactive = reactive,
            cornerRadiusDp = corner,
            shape = shape
        )
    }

    fun saveNotificationEdgeLightingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EDGE_NOTIFICATION_BORDER, enabled).apply()
    }

    fun loadNotificationEdgeLightingEnabled(): Boolean {
        return prefs.getBoolean(KEY_EDGE_NOTIFICATION_BORDER, true)
    }

    // --- EQUALIZER ---
    fun saveEqualizer(state: EqualizerState) {
        val gainsString = state.bandGains.joinToString(",") { it.toString() }
        prefs.edit()
            .putBoolean(KEY_EQ_ENABLED, state.isEnabled)
            .putString(KEY_EQ_PRESET, state.selectedPresetName)
            .putString(KEY_EQ_GAINS, gainsString)
            .putFloat(KEY_EQ_PREAMP, state.preAmpGainDb)
            .putFloat(KEY_EQ_BASS_PUNCH, state.bassPunch)
            .putFloat(KEY_EQ_TREBLE, state.trebleSparkle)
            .apply()
    }

    fun loadEqualizer(): EqualizerState {
        val enabled = prefs.getBoolean(KEY_EQ_ENABLED, true)
        val preset = prefs.getString(KEY_EQ_PRESET, "Vocal & Bass Boost") ?: "Vocal & Bass Boost"
        val gainsStr = prefs.getString(KEY_EQ_GAINS, null)
        val gains = if (gainsStr != null) {
            gainsStr.split(",").mapNotNull { it.toFloatOrNull() }
        } else {
            EqualizerState().bandGains
        }
        val preAmp = prefs.getFloat(KEY_EQ_PREAMP, 0.0f)
        val bassPunch = prefs.getFloat(KEY_EQ_BASS_PUNCH, 0.5f)
        val treble = prefs.getFloat(KEY_EQ_TREBLE, 0.5f)

        return EqualizerState(
            isEnabled = enabled,
            selectedPresetName = preset,
            bandGains = gains,
            preAmpGainDb = preAmp,
            bassPunch = bassPunch,
            trebleSparkle = treble
        )
    }

    // --- 3D SPATIAL & STEREO DELAY ---
    fun saveSpatial(playerState: PlayerState) {
        prefs.edit()
            .putBoolean(KEY_SPATIAL_AUTO_ROTATE, playerState.autoRotateEnabled)
            .putFloat(KEY_SPATIAL_SPEED, playerState.autoRotateSpeedSec)
            .putFloat(KEY_ROTATION_SPEED_MULT, playerState.rotationSpeedMultiplier)
            .putString(KEY_SPATIAL_DIRECTION, playerState.autoRotateDirection.name)
            .putFloat(KEY_SPATIAL_WIDTH, playerState.stereoWidth)
            .putBoolean(KEY_SPATIAL_VIRTUALIZER, playerState.virtualizerEnabled)
            .putInt(KEY_SPATIAL_VIRT_STRENGTH, playerState.virtualizerStrength)
            .putString(KEY_VIRTUALIZER_MODE, playerState.virtualizerMode)
            .putBoolean(KEY_SPATIAL_BASS, playerState.bassBoostEnabled)
            .putInt(KEY_SPATIAL_BASS_STRENGTH, playerState.bassBoostStrength)
            .putString(KEY_SPATIAL_ENV, playerState.spatialEnvironment.name)
            .putBoolean(KEY_REVERB_MASTER_ENABLED, playerState.reverbMasterEnabled)
            .putFloat(KEY_REVERB_WET_DRY, playerState.reverbWetDryMix)
            .putFloat(KEY_REVERB_DECAY, playerState.reverbDecayTime)
            .putBoolean(KEY_DPS_MASTER_ENABLED, playerState.dpsEnabled)
            .putString(KEY_DPS_PROFILE, playerState.dpsProfile.name)
            .putInt(KEY_DPS_BASS_BOOST, playerState.dpsBassBoost)
            .putInt(KEY_DPS_CLARITY, playerState.dpsClarity)
            .putBoolean(KEY_SPATIAL_HEAD_SHADOW, playerState.headShadowEnabled)
            .putFloat(KEY_SPATIAL_WIDENER, playerState.binauralWidener)
            .putBoolean(KEY_DELAY_ENABLED, playerState.delayEnabled)
            .putInt(KEY_DELAY_TIME_MS, playerState.delayTimeMs)
            .putInt(KEY_DELAY_FEEDBACK_PCT, playerState.delayFeedbackPercent)
            .putInt(KEY_DELAY_MIX_PCT, playerState.delayMixPercent)
            .apply()
    }

    fun saveDelaySettings(enabled: Boolean, timeMs: Int, feedbackPct: Int, mixPct: Int) {
        prefs.edit()
            .putBoolean(KEY_DELAY_ENABLED, enabled)
            .putInt(KEY_DELAY_TIME_MS, timeMs)
            .putInt(KEY_DELAY_FEEDBACK_PCT, feedbackPct)
            .putInt(KEY_DELAY_MIX_PCT, mixPct)
            .apply()
    }

    fun loadSpatial(defaultState: PlayerState): PlayerState {
        // Default 8D auto-rotate to false on app launch per Request 5
        val autoRotate = prefs.getBoolean(KEY_SPATIAL_AUTO_ROTATE, false)
        val speed = prefs.getFloat(KEY_SPATIAL_SPEED, defaultState.autoRotateSpeedSec)
        val speedMult = prefs.getFloat(KEY_ROTATION_SPEED_MULT, defaultState.rotationSpeedMultiplier)
        val dirName = prefs.getString(KEY_SPATIAL_DIRECTION, defaultState.autoRotateDirection.name)
        val dir = try {
            AutoRotateDirection.valueOf(dirName ?: AutoRotateDirection.LEFT_TO_RIGHT.name)
        } catch (t: Throwable) {
            AutoRotateDirection.LEFT_TO_RIGHT
        }
        val width = prefs.getFloat(KEY_SPATIAL_WIDTH, defaultState.stereoWidth)
        val virtEnabled = prefs.getBoolean(KEY_SPATIAL_VIRTUALIZER, defaultState.virtualizerEnabled)
        val virtStrength = prefs.getInt(KEY_SPATIAL_VIRT_STRENGTH, defaultState.virtualizerStrength)
        val virtMode = prefs.getString(KEY_VIRTUALIZER_MODE, defaultState.virtualizerMode) ?: defaultState.virtualizerMode
        val bassEnabled = prefs.getBoolean(KEY_SPATIAL_BASS, defaultState.bassBoostEnabled)
        val bassStrength = prefs.getInt(KEY_SPATIAL_BASS_STRENGTH, defaultState.bassBoostStrength)
        val envName = prefs.getString(KEY_SPATIAL_ENV, defaultState.spatialEnvironment.name)
        val env = try {
            when (envName) {
                "OFF", "STUDIO", "STUDIO_DRY" -> SpatialEnvironment.STUDIO_DRY
                "ACOUSTIC_ROOM" -> SpatialEnvironment.ACOUSTIC_ROOM
                "PLATE", "LIVE_STAGE" -> SpatialEnvironment.LIVE_STAGE
                "CONCERT_HALL" -> SpatialEnvironment.CONCERT_HALL
                "CATHEDRAL", "GREAT_HALL" -> SpatialEnvironment.GREAT_HALL
                "ARENA", "MEGA_STADIUM" -> SpatialEnvironment.MEGA_STADIUM
                "ECHO_CHAMBER" -> SpatialEnvironment.ECHO_CHAMBER
                else -> SpatialEnvironment.valueOf(envName ?: SpatialEnvironment.STUDIO_DRY.name)
            }
        } catch (t: Throwable) {
            SpatialEnvironment.STUDIO_DRY
        }
        val reverbMaster = prefs.getBoolean(KEY_REVERB_MASTER_ENABLED, defaultState.reverbMasterEnabled)
        val reverbWetDry = prefs.getFloat(KEY_REVERB_WET_DRY, defaultState.reverbWetDryMix)
        val reverbDecay = prefs.getFloat(KEY_REVERB_DECAY, defaultState.reverbDecayTime)

        val dpsMaster = prefs.getBoolean(KEY_DPS_MASTER_ENABLED, defaultState.dpsEnabled)
        val dpsProfileName = prefs.getString(KEY_DPS_PROFILE, defaultState.dpsProfile.name)
        val dpsProfile = try {
            DpsProfile.valueOf(dpsProfileName ?: DpsProfile.MUSIC.name)
        } catch (t: Throwable) {
            DpsProfile.MUSIC
        }
        val dpsBass = prefs.getInt(KEY_DPS_BASS_BOOST, defaultState.dpsBassBoost)
        val dpsClarity = prefs.getInt(KEY_DPS_CLARITY, defaultState.dpsClarity)

        val headShadow = prefs.getBoolean(KEY_SPATIAL_HEAD_SHADOW, defaultState.headShadowEnabled)
        val widener = prefs.getFloat(KEY_SPATIAL_WIDENER, defaultState.binauralWidener)

        // Restore Stereo Delay settings
        val delayEnabled = prefs.getBoolean(KEY_DELAY_ENABLED, defaultState.delayEnabled)
        val delayTimeMs = prefs.getInt(KEY_DELAY_TIME_MS, defaultState.delayTimeMs)
        val delayFeedbackPct = prefs.getInt(KEY_DELAY_FEEDBACK_PCT, defaultState.delayFeedbackPercent)
        val delayMixPct = prefs.getInt(KEY_DELAY_MIX_PCT, defaultState.delayMixPercent)

        return defaultState.copy(
            autoRotateEnabled = autoRotate,
            autoRotateSpeedSec = speed,
            rotationSpeedMultiplier = speedMult,
            autoRotateDirection = dir,
            stereoWidth = width,
            virtualizerEnabled = virtEnabled,
            virtualizerStrength = virtStrength,
            virtualizerMode = virtMode,
            bassBoostEnabled = bassEnabled,
            bassBoostStrength = bassStrength,
            reverbMasterEnabled = reverbMaster,
            reverbWetDryMix = reverbWetDry,
            reverbDecayTime = reverbDecay,
            spatialEnvironment = env,
            dpsEnabled = dpsMaster,
            dpsProfile = dpsProfile,
            dpsBassBoost = dpsBass,
            dpsClarity = dpsClarity,
            headShadowEnabled = headShadow,
            binauralWidener = widener,
            delayEnabled = delayEnabled,
            delayTimeMs = delayTimeMs,
            delayFeedbackPercent = delayFeedbackPct,
            delayMixPercent = delayMixPct
        )
    }

    // --- FAVORITES ---
    fun saveFavorites(favorites: Set<Long>) {
        val stringSet = favorites.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_FAVORITES, stringSet).apply()
    }

    fun loadFavorites(): Set<Long> {
        val set = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    // --- LAST UI TAB & STATE (Request 3) ---
    fun saveLastTab(tabName: String) {
        prefs.edit().putString(KEY_LAST_TAB, tabName).apply()
    }

    fun loadLastTab(defaultTab: String = "NOW_PLAYING"): String {
        return prefs.getString(KEY_LAST_TAB, defaultTab) ?: defaultTab
    }

    // --- LAST PLAYBACK RESTORATION (Request 3) ---
    fun saveLastPlayback(
        songId: Long?,
        positionMs: Long,
        isDemo: Boolean,
        shuffle: Boolean,
        repeatModeName: String
    ) {
        val editor = prefs.edit()
        if (songId != null) {
            editor.putLong(KEY_LAST_SONG_ID, songId)
        } else {
            editor.remove(KEY_LAST_SONG_ID)
        }
        editor.putLong(KEY_LAST_SONG_POS, positionMs)
            .putBoolean(KEY_LAST_SONG_IS_DEMO, isDemo)
            .putBoolean(KEY_SHUFFLE, shuffle)
            .putString(KEY_REPEAT, repeatModeName)
            .apply()
    }

    fun loadLastSongId(): Long? {
        return if (prefs.contains(KEY_LAST_SONG_ID)) prefs.getLong(KEY_LAST_SONG_ID, -1L) else null
    }

    fun saveLastPlayedSongId(songId: Long) {
        prefs.edit().putLong(KEY_LAST_SONG_ID, songId).apply()
    }

    fun loadLastPlayedSongId(): Long? = loadLastSongId()

    fun loadLastSongPosition(): Long {
        return prefs.getLong(KEY_LAST_SONG_POS, 0L)
    }

    fun saveLastPositionMs(positionMs: Long) {
        prefs.edit().putLong(KEY_LAST_SONG_POS, positionMs).apply()
    }

    fun loadLastPositionMs(): Long = loadLastSongPosition()

    fun loadLastSongIsDemo(): Boolean {
        return prefs.getBoolean(KEY_LAST_SONG_IS_DEMO, false)
    }

    fun loadLastShuffle(): Boolean {
        return prefs.getBoolean(KEY_SHUFFLE, false)
    }

    fun saveShuffleEnabled(enabled: Boolean) = saveShuffle(enabled)
    fun loadShuffleEnabled(): Boolean = loadLastShuffle()

    fun loadLastRepeat(defaultMode: String = "OFF"): String {
        return prefs.getString(KEY_REPEAT, defaultMode) ?: defaultMode
    }

    fun saveRepeatMode(repeatMode: String) = saveRepeat(repeatMode)
    fun loadRepeatMode(): String = loadLastRepeat()

    // --- VISUALIZER STATE (Request 3 & Request 7) ---
    fun saveVisualizerState(enabled: Boolean, effectId: Int) {
        prefs.edit()
            .putBoolean(KEY_VISUALIZER_ENABLED, enabled)
            .putInt(KEY_VISUALIZER_EFFECT_ID, effectId)
            .apply()
    }

    fun loadVisualizerEnabled(): Boolean {
        return prefs.getBoolean(KEY_VISUALIZER_ENABLED, true)
    }

    fun loadVisualizerEffectId(): Int {
        return prefs.getInt(KEY_VISUALIZER_EFFECT_ID, 1).coerceIn(1, 30)
    }

    // --- CUSTOM VISUALIZER BRANDING TEXT (Request 6) ---
    fun saveCustomVisualizerText(text: String) {
        val cleanText = text.trim().take(10)
        prefs.edit()
            .putString(KEY_CUSTOM_VISUALIZER_TEXT, if (cleanText.isBlank()) DEFAULT_VISUALIZER_TEXT else cleanText)
            .apply()
    }

    fun loadCustomVisualizerText(): String {
        return prefs.getString(KEY_CUSTOM_VISUALIZER_TEXT, DEFAULT_VISUALIZER_TEXT) ?: DEFAULT_VISUALIZER_TEXT
    }

    // --- VOLUME GAUGE PREFERENCES ---
    fun saveVolumeGaugeSettings(settings: VolumeGaugeSettings) {
        prefs.edit()
            .putInt(KEY_VOL_GAUGE_STEP, settings.stepSizePercent)
            .putString(KEY_VOL_GAUGE_POSITION, settings.position.name)
            .putString(KEY_VOL_GAUGE_THEME, settings.theme.name)
            .putBoolean(KEY_VOL_GAUGE_HAPTIC, settings.hapticEnabled)
            .putFloat(KEY_VOL_GAUGE_DELAY, settings.autoDismissDelaySec)
            .putBoolean(KEY_VOL_GAUGE_OVERLAY, settings.floatingOverlayEnabled)
            .apply()
    }

    fun loadVolumeGaugeSettings(): VolumeGaugeSettings {
        val step = prefs.getInt(KEY_VOL_GAUGE_STEP, 5)
        val posStr = prefs.getString(KEY_VOL_GAUGE_POSITION, VolumeGaugePosition.RIGHT.name) ?: VolumeGaugePosition.RIGHT.name
        val pos = try { VolumeGaugePosition.valueOf(posStr) } catch (_: Throwable) { VolumeGaugePosition.RIGHT }
        val themeStr = prefs.getString(KEY_VOL_GAUGE_THEME, VolumeGaugeTheme.CYAN_NEON.name) ?: VolumeGaugeTheme.CYAN_NEON.name
        val theme = try { VolumeGaugeTheme.valueOf(themeStr) } catch (_: Throwable) { VolumeGaugeTheme.CYAN_NEON }
        val haptic = prefs.getBoolean(KEY_VOL_GAUGE_HAPTIC, true)
        val delay = prefs.getFloat(KEY_VOL_GAUGE_DELAY, 2.0f)
        val overlay = prefs.getBoolean(KEY_VOL_GAUGE_OVERLAY, true)
        return VolumeGaugeSettings(
            stepSizePercent = step,
            position = pos,
            theme = theme,
            hapticEnabled = haptic,
            autoDismissDelaySec = delay,
            floatingOverlayEnabled = overlay
        )
    }
}
