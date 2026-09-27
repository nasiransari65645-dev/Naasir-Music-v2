package com.example.model

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 9 Natural Colors Theme Preset alias
 */
typealias AppThemePreset = AppNaturalTheme


/**
 * Multi-Element Color Mapping across specific UI roles:
 * - Primary Play/Pause Button
 * - Secondary Skip & Rewind Buttons
 * - Seek Bar progress track and scrubber thumb
 * - Mini-Player outline border / accent glow
 * - Bottom navigation bar active tab icon & pill indicator
 * - Card headers, chevron arrows, and active switches
 */
data class MultiElementColorMap(
    val playPauseButtonColor: Color = Color(0xFF00E5FF),
    val skipRewindButtonColor: Color = Color(0xFFA855F7),
    val seekBarColor: Color = Color(0xFF00E5FF),
    val miniPlayerAccentColor: Color = Color(0xFF00E5FF),
    val bottomNavActiveColor: Color = Color(0xFF00E5FF),
    val cardHeaderAndSwitchColor: Color = Color(0xFF00E5FF)
) {
    fun serialize(): String {
        return listOf(
            playPauseButtonColor.value.toString(),
            skipRewindButtonColor.value.toString(),
            seekBarColor.value.toString(),
            miniPlayerAccentColor.value.toString(),
            bottomNavActiveColor.value.toString(),
            cardHeaderAndSwitchColor.value.toString()
        ).joinToString(",")
    }

    companion object {
        fun deserialize(value: String?): MultiElementColorMap? {
            if (value.isNullOrBlank()) return null
            return try {
                val parts = value.split(",")
                if (parts.size >= 6) {
                    MultiElementColorMap(
                        playPauseButtonColor = Color(parts[0].toULong()),
                        skipRewindButtonColor = Color(parts[1].toULong()),
                        seekBarColor = Color(parts[2].toULong()),
                        miniPlayerAccentColor = Color(parts[3].toULong()),
                        bottomNavActiveColor = Color(parts[4].toULong()),
                        cardHeaderAndSwitchColor = Color(parts[5].toULong())
                    )
                } else null
            } catch (t: Throwable) {
                null
            }
        }

        fun fromThemePreset(preset: AppThemePreset, shuffle: Boolean = false): MultiElementColorMap {
            val pal = preset.palette
            if (pal.isEmpty()) {
                return MultiElementColorMap(
                    playPauseButtonColor = preset.primaryColor,
                    skipRewindButtonColor = preset.secondaryColor,
                    seekBarColor = preset.primaryColor,
                    miniPlayerAccentColor = preset.primaryColor,
                    bottomNavActiveColor = preset.primaryColor,
                    cardHeaderAndSwitchColor = preset.primaryColor
                )
            }
            val colors = if (shuffle) pal.shuffled() else pal
            return MultiElementColorMap(
                playPauseButtonColor = colors[0 % colors.size],
                skipRewindButtonColor = colors[1 % colors.size],
                seekBarColor = colors[2 % colors.size],
                miniPlayerAccentColor = colors[0 % colors.size],
                bottomNavActiveColor = colors[1 % colors.size],
                cardHeaderAndSwitchColor = colors[3 % colors.size]
            )
        }
    }
}

enum class EdgeLightingShape(val title: String, val iconSymbol: String) {
    SOLID_LINE("Solid Line", "―"),
    HEARTS("Hearts", "♥"),
    STARS("Stars", "★"),
    MUSIC_NOTES("Music Notes", "♫"),
    NEON_DOTS("Neon Dots", "●")
}

enum class EdgeLightingStyle(
    val title: String,
    val colors: List<Color>
) {
    NEON_CYAN(
        title = "Hi-Fi Neon",
        colors = listOf(Color(0xFF00E5FF), Color(0xFF38BDF8), Color(0xFF00E5FF))
    ),
    CYBER_PULSE(
        title = "Cyberpunk",
        colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7), Color(0xFFFF007F), Color(0xFF00E5FF))
    ),
    RAINBOW_SPECTRUM(
        title = "Rainbow",
        colors = listOf(
            Color(0xFFFF0055),
            Color(0xFFFF9900),
            Color(0xFF00FF66),
            Color(0xFF00CCFF),
            Color(0xFF9900FF),
            Color(0xFFFF0055)
        )
    ),
    ELECTRIC_SUNSET(
        title = "Electric Sunset",
        colors = listOf(Color(0xFFFF5722), Color(0xFFFFB703), Color(0xFFFF007F), Color(0xFFFF5722))
    ),
    EMERALD_LASER(
        title = "Emerald Laser",
        colors = listOf(Color(0xFF10B981), Color(0xFF00E5FF), Color(0xFF34D399), Color(0xFF10B981))
    )
}

data class EdgeLightingSettings(
    val isEnabled: Boolean = false,
    val isNotificationBorderEnabled: Boolean = true,
    val style: EdgeLightingStyle = EdgeLightingStyle.CYBER_PULSE,
    val strokeWidthDp: Float = 4.0f,
    val animationSpeedSec: Float = 3.5f,
    val musicReactive: Boolean = true,
    val cornerRadiusDp: Float = 36.0f,
    val shape: EdgeLightingShape = EdgeLightingShape.SOLID_LINE
)

enum class ProgressBarColorPreset(
    val title: String,
    val primaryColor: Color,
    val gradientColors: List<Color>
) {
    NEON_CYAN(
        title = "Neon Cyan",
        primaryColor = Color(0xFF00E5FF),
        gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF38BDF8))
    ),
    ELECTRIC_PURPLE(
        title = "Electric Purple",
        primaryColor = Color(0xFFA855F7),
        gradientColors = listOf(Color(0xFFA855F7), Color(0xFFD946EF))
    ),
    SUNSET_AMBER(
        title = "Sunset Amber",
        primaryColor = Color(0xFFFF9800),
        gradientColors = listOf(Color(0xFFFF5722), Color(0xFFFFB703))
    ),
    EMERALD_MINT(
        title = "Emerald Mint",
        primaryColor = Color(0xFF10B981),
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399))
    ),
    ROSE_CRIMSON(
        title = "Rose Crimson",
        primaryColor = Color(0xFFFF007F),
        gradientColors = listOf(Color(0xFFFF007F), Color(0xFFFF5252))
    ),
    GOLDEN_GLOW(
        title = "Golden Glow",
        primaryColor = Color(0xFFFFD700),
        gradientColors = listOf(Color(0xFFFFB703), Color(0xFFFFE082))
    )
}

enum class ProgressBarAnimation(
    val title: String,
    val description: String
) {
    SMOOTH_SOLID(
        title = "Smooth Solid",
        description = "Clean modern steady high-precision track"
    ),
    GLOW_PULSE(
        title = "Glow Pulse",
        description = "Subtle pulsing neon glow accent along progress head"
    ),
    SHIMMER_WAVE(
        title = "Shimmer Wave",
        description = "Flowing holographic light gradient shimmer across bar"
    )
}

data class CustomThemeSettings(
    val isEnabled: Boolean = false,
    val themePreset: AppThemePreset = AppThemePreset.BLUE,
    val progressColor: ProgressBarColorPreset = ProgressBarColorPreset.NEON_CYAN,
    val progressAnimation: ProgressBarAnimation = ProgressBarAnimation.GLOW_PULSE,
    val colorMap: MultiElementColorMap = MultiElementColorMap()
)

data class AppThemePalette(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val c1Primary: Color,         // Base 1: Play button & master neon
    val c2Secondary: Color,       // Base 2: Seekbar progress start & waves
    val c3Tertiary: Color,        // Base 3: Seekbar progress end & control icons
    val c4Accent: Color,          // Base 4: Thumb glow & active toggles
    val c5VinylRing: Color,       // Base 5: Vinyl disc outer rim & radar
    val c6SurfaceGlow: Color,     // Base 6: Ambient backdrop, card borders, tab icons
    val surface: Color = Color(0xFF101526),
    val background: Color = Color(0xFF080B14)
) {
    // Aliases for seamless backwards-compatibility
    val c4Quaternary: Color get() = c4Accent
    val c5SurfaceAccent: Color get() = c5VinylRing
    val c6GlowHighlight: Color get() = c6SurfaceGlow
    val primaryAccent: Color get() = c1Primary
    val secondaryAccent: Color get() = c2Secondary
    val seekBarActiveTrack: Color get() = c2Secondary
    val seekBarInactiveTrack: Color get() = c4Accent.copy(alpha = 0.25f)
    val seekBarThumbGlow: Color get() = c4Accent
    val edgeLightingColor: Color get() = c6SurfaceGlow
    val visualizerColor: Color get() = c1Primary

    // Seamless Gradient Blends for NowPlaying
    val playButtonBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(c1Primary, c2Secondary)
        )

    val seekBarProgressBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(c2Secondary, c3Tertiary, c4Accent)
        )

    val vinylGlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(c5VinylRing.copy(alpha = 0.45f), c6SurfaceGlow.copy(alpha = 0.15f), Color.Transparent)
        )

    val ambientBackdropBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                c6SurfaceGlow.copy(alpha = 0.22f),
                surface.copy(alpha = 0.85f),
                background
            )
        )

    val visualizerBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(c1Primary, c2Secondary, c3Tertiary.copy(alpha = 0.35f))
        )
}

val CuratedThemePalettes: List<AppThemePalette> = AppNaturalTheme.entries.map { getPaletteForPreset(it) }

val DEFAULT_PALETTE: AppThemePalette get() = getPaletteForPreset(AppNaturalTheme.BLUE)
val LIGHT_THEME_PALETTE: AppThemePalette get() = getPaletteForPreset(AppNaturalTheme.WHITE)

val LocalAppThemePalette = staticCompositionLocalOf { DEFAULT_PALETTE }
val LocalThemePalette = LocalAppThemePalette

fun getPaletteById(id: String): AppThemePalette {
    return CuratedThemePalettes.firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: getPaletteForPreset(AppNaturalTheme.fromNameSafe(id))
}

fun getPaletteForPreset(preset: AppNaturalTheme): AppThemePalette {
    return AppThemePalette(
        id = preset.name.lowercase(),
        name = preset.title,
        subtitle = preset.description,
        c1Primary = preset.primaryColor,
        c2Secondary = preset.secondaryColor,
        c3Tertiary = if (preset.palette.size > 2) preset.palette[2] else preset.primaryColor,
        c4Accent = preset.primaryColor,
        c5VinylRing = preset.outlineColor,
        c6SurfaceGlow = preset.surfaceColor,
        surface = preset.surfaceColor,
        background = preset.backgroundColor
    )
}

