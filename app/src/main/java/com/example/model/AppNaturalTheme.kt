package com.example.model

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 8 Vibrant, High-Contrast Themes:
 * 1. RED (Deep Charcoal/Black background, Vivid Crimson Red accents #EF4444, high-contrast crisp text)
 * 2. GREEN (Rich OLED Dark background, Vivid Emerald Green accents #10B981, high-contrast crisp text)
 * 3. BLUE (Deep Midnight Slate background, Electric Neon Cyan/Blue accents #06B6D4, high-contrast crisp text)
 * 4. ORANGE (Warm Dark Charcoal background, Punchy Neon Amber/Orange accents #F97316, high-contrast crisp text)
 * 5. PINK (Sleek Dark Obsidian background, Vivid Hot Pink accents #EC4899, high-contrast crisp text)
 * 6. BLACK (Pitch 100% OLED Black #000000 background, Crisp White/Cyan highlights, high-contrast borders)
 * 7. WHITE (Clean Crisp Off-White #F8FAFC background, Sleek #FFFFFF card surfaces, Deep Slate/Charcoal #0F172A text & icons, vibrant primary accents)
 * 8. PURPLE (Deep Dark Violet background, Vibrant Neon Electric Purple/Magenta accents #A855F7, high-contrast crisp text)
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
    RED(
        title = "Red",
        description = "Deep Charcoal with Vivid Crimson Red accents",
        isDark = true,
        backgroundColor = Color(0xFF0D0A0A),
        surfaceColor = Color(0xFF171010),
        cardColor = Color(0xFF221414),
        primaryColor = Color(0xFFEF4444),
        secondaryColor = Color(0xFFF87171),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF991B1B),
        palette = listOf(Color(0xFF0D0A0A), Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFFFFFFF))
    ),
    GREEN(
        title = "Green",
        description = "Rich OLED Dark with Vivid Emerald Green accents",
        isDark = true,
        backgroundColor = Color(0xFF040D08),
        surfaceColor = Color(0xFF091710),
        cardColor = Color(0xFF11241A),
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF34D399),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF065F46),
        palette = listOf(Color(0xFF040D08), Color(0xFF10B981), Color(0xFF34D399), Color(0xFFFFFFFF))
    ),
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
    ),
    ORANGE(
        title = "Orange",
        description = "Warm Dark Charcoal with Punchy Neon Amber accents",
        isDark = true,
        backgroundColor = Color(0xFF120C06),
        surfaceColor = Color(0xFF1C130A),
        cardColor = Color(0xFF2A1C10),
        primaryColor = Color(0xFFF97316),
        secondaryColor = Color(0xFFFBBF24),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF9A3412),
        palette = listOf(Color(0xFF120C06), Color(0xFFF97316), Color(0xFFFBBF24), Color(0xFFFFFFFF))
    ),
    PINK(
        title = "Pink",
        description = "Sleek Dark Obsidian with Vivid Hot Pink accents",
        isDark = true,
        backgroundColor = Color(0xFF12070E),
        surfaceColor = Color(0xFF1D0C17),
        cardColor = Color(0xFF2B1323),
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFFF472B6),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF9D174D),
        palette = listOf(Color(0xFF12070E), Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFFFFFFF))
    ),
    BLACK(
        title = "Black",
        description = "Pitch 100% OLED Black with Crisp White/Cyan highlights",
        isDark = true,
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF0B0F15),
        cardColor = Color(0xFF161B22),
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFF0F6FC),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF30363D),
        palette = listOf(Color(0xFF000000), Color(0xFF00E5FF), Color(0xFFF0F6FC), Color(0xFFFFFFFF))
    ),
    WHITE(
        title = "White",
        description = "Clean Crisp Off-White with Sleek White cards and Deep Slate text",
        isDark = false,
        backgroundColor = Color(0xFFF8FAFC),
        surfaceColor = Color(0xFFFFFFFF),
        cardColor = Color(0xFFFFFFFF),
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF0F766E),
        onBackgroundColor = Color(0xFF0F172A),
        onSurfaceColor = Color(0xFF0F172A),
        onSurfaceVariantColor = Color(0xFF475569),
        outlineColor = Color(0xFFCBD5E1),
        palette = listOf(Color(0xFFF8FAFC), Color(0xFF0284C7), Color(0xFF0F766E), Color(0xFF0F172A))
    ),
    PURPLE(
        title = "Purple",
        description = "Deep Dark Violet with Vibrant Neon Electric Purple accents",
        isDark = true,
        backgroundColor = Color(0xFF0E0618),
        surfaceColor = Color(0xFF180B28),
        cardColor = Color(0xFF25123E),
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFFC084FC),
        onBackgroundColor = Color(0xFFFFFFFF),
        onSurfaceColor = Color(0xFFFFFFFF),
        onSurfaceVariantColor = Color(0xFFCBD5E1),
        outlineColor = Color(0xFF7E22CE),
        palette = listOf(Color(0xFF0E0618), Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFFFFFFFF))
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
        val PURE_BLACK: AppNaturalTheme get() = BLACK
        val PURE_WHITE: AppNaturalTheme get() = WHITE
        val DEEP_FOREST: AppNaturalTheme get() = GREEN
        val OCEAN_NAVY: AppNaturalTheme get() = BLUE
        val WARM_AMBER: AppNaturalTheme get() = ORANGE
        val ROYAL_VIOLET: AppNaturalTheme get() = PURPLE
        val CRIMSON_RUBY: AppNaturalTheme get() = RED
        val SLATE_GRAPHITE: AppNaturalTheme get() = BLUE
        val DESERT_SAND: AppNaturalTheme get() = ORANGE
        val COSMIC_DARK: AppNaturalTheme get() = BLACK
        val SUNSET_CRIMSON: AppNaturalTheme get() = ORANGE
        val EMERALD_MATRIX: AppNaturalTheme get() = GREEN
        val OLED_MIDNIGHT: AppNaturalTheme get() = BLACK
        val COSMIC_AURORA: AppNaturalTheme get() = BLUE
        val RETRO_WAVE: AppNaturalTheme get() = PURPLE
        val CYBER_NEON: AppNaturalTheme get() = BLACK
        val DEEP_OCEAN: AppNaturalTheme get() = BLUE
        val TOXIC_LIME: AppNaturalTheme get() = GREEN
        val ROYAL_AMETHYST: AppNaturalTheme get() = PURPLE
        val SOLAR_FLARE: AppNaturalTheme get() = ORANGE

        fun fromNameSafe(name: String?): AppNaturalTheme {
            if (name.isNullOrBlank()) return BLUE
            return try {
                valueOf(name)
            } catch (e: Exception) {
                when (name.uppercase()) {
                    "RED", "CRIMSON_RUBY" -> RED
                    "GREEN", "DEEP_FOREST", "EMERALD_MATRIX", "TOXIC_LIME" -> GREEN
                    "BLUE", "OCEAN_NAVY", "DEEP_OCEAN", "COSMIC_AURORA", "SLATE_GRAPHITE" -> BLUE
                    "ORANGE", "WARM_AMBER", "SUNSET_CRIMSON", "SOLAR_FLARE", "DESERT_SAND" -> ORANGE
                    "PINK" -> PINK
                    "BLACK", "PURE_BLACK", "COSMIC_DARK", "OLED_MIDNIGHT", "CYBER_NEON" -> BLACK
                    "WHITE", "PURE_WHITE", "CLEAN_WHITE", "LIGHT_WHITE" -> WHITE
                    "PURPLE", "ROYAL_VIOLET", "RETRO_WAVE", "ROYAL_AMETHYST" -> PURPLE
                    else -> BLUE
                }
            }
        }
    }

    fun toColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = if (primaryColor == Color(0xFF00E5FF)) Color.Black else Color.White,
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
        } else {
            lightColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF1F5F9),
                onPrimaryContainer = primaryColor,
                secondary = secondaryColor,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF1F5F9),
                onSecondaryContainer = onSurfaceColor,
                tertiary = secondaryColor,
                onTertiary = Color.White,
                background = backgroundColor,
                onBackground = onBackgroundColor,
                surface = surfaceColor,
                onSurface = onSurfaceColor,
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = onSurfaceVariantColor,
                outline = outlineColor,
                outlineVariant = outlineColor.copy(alpha = 0.6f)
            )
        }
    }
}
