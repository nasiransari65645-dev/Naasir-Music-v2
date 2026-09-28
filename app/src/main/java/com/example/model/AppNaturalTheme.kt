package com.example.model

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Single Unified Theme:
 * BLUE (Deep Midnight Slate background, Electric Neon Cyan/Blue accents #06B6D4, high-contrast crisp text)
 */
enum class AppNaturalTheme(
    val title: String,
    val description: String,
    val isDark: Boolean,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val cardColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val onBackgroundColor: Color,
    val onSurfaceColor: Color,
    val onSurfaceVariantColor: Color,
    val outlineColor: Color,
    val palette: List<Color>
) {
    BLUE(
        title = "Blue",
        description = "Deep Midnight Slate with Electric Neon Cyan accents",
        isDark = true,
        backgroundColor = Color(0xFF070F1E),
        surfaceColor = Color(0xFF0E192E),
        cardColor = Color(0xFF162544),
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF38BDF8),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF0E7490),
        palette = listOf(Color(0xFF070F1E), Color(0xFF06B6D4), Color(0xFF38BDF8), Color(0xFFFFFFFF))
    );

    // Backward-compatibility aliases
    val primary: Color get() = primaryColor
    val secondary: Color get() = secondaryColor
    val background: Color get() = backgroundColor
    val surface: Color get() = surfaceColor
    val surfaceVariant: Color get() = cardColor

    val primaryColorInt: Int
        get() = android.graphics.Color.argb(
            (primaryColor.alpha * 255).toInt(),
            (primaryColor.red * 255).toInt(),
            (primaryColor.green * 255).toInt(),
            (primaryColor.blue * 255).toInt()
        )

    companion object {
        val RED: AppNaturalTheme get() = BLUE
        val GREEN: AppNaturalTheme get() = BLUE
        val ORANGE: AppNaturalTheme get() = BLUE
        val PINK: AppNaturalTheme get() = BLUE
        val BLACK: AppNaturalTheme get() = BLUE
        val WHITE: AppNaturalTheme get() = BLUE
        val PURPLE: AppNaturalTheme get() = BLUE
        val COSMIC_ORBIT: AppNaturalTheme get() = BLUE
        val PURE_BLACK: AppNaturalTheme get() = BLUE
        val PURE_WHITE: AppNaturalTheme get() = BLUE
        val DEEP_FOREST: AppNaturalTheme get() = BLUE
        val OCEAN_NAVY: AppNaturalTheme get() = BLUE
        val WARM_AMBER: AppNaturalTheme get() = BLUE
        val ROYAL_VIOLET: AppNaturalTheme get() = BLUE
        val CRIMSON_RUBY: AppNaturalTheme get() = BLUE
        val SLATE_GRAPHITE: AppNaturalTheme get() = BLUE
        val DESERT_SAND: AppNaturalTheme get() = BLUE
        val COSMIC_DARK: AppNaturalTheme get() = BLUE
        val SUNSET_CRIMSON: AppNaturalTheme get() = BLUE
        val EMERALD_MATRIX: AppNaturalTheme get() = BLUE
        val OLED_MIDNIGHT: AppNaturalTheme get() = BLUE
        val COSMIC_AURORA: AppNaturalTheme get() = BLUE
        val RETRO_WAVE: AppNaturalTheme get() = BLUE
        val CYBER_NEON: AppNaturalTheme get() = BLUE
        val DEEP_OCEAN: AppNaturalTheme get() = BLUE
        val TOXIC_LIME: AppNaturalTheme get() = BLUE
        val ROYAL_AMETHYST: AppNaturalTheme get() = BLUE
        val SOLAR_FLARE: AppNaturalTheme get() = BLUE

        fun fromNameSafe(name: String?): AppNaturalTheme = BLUE
    }

    fun toColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.Black,
            primaryContainer = cardColor,
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.Black,
            secondaryContainer = cardColor,
            onSecondaryContainer = onSurfaceColor,
            tertiary = secondaryColor,
            onTertiary = Color.Black,
            background = backgroundColor,
            onBackground = onBackgroundColor,
            surface = surfaceColor,
            onSurface = onSurfaceColor,
            surfaceVariant = cardColor,
            onSurfaceVariant = onSurfaceVariantColor,
            outline = outlineColor,
            outlineVariant = outlineColor.copy(alpha = 0.5f)
        )
    }
}
