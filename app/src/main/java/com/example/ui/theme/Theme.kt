package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppNaturalTheme
import com.example.model.AppThemeMode
import com.example.model.AppThemePreset

@Composable
fun MyApplicationTheme(
    naturalTheme: AppNaturalTheme = AppNaturalTheme.BLUE,
    themePreset: AppThemePreset = naturalTheme,
    themeMode: AppThemeMode = if (naturalTheme.isDark) AppThemeMode.DARK_OLED else AppThemeMode.LIGHT_WHITE,
    isDarkTheme: Boolean = naturalTheme.isDark,
    content: @Composable () -> Unit
) {
    val activeTheme = themePreset
    val isDark = activeTheme.isDark
    val dynamicColorScheme = activeTheme.toColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Light icons for dark theme, dark icons for light theme
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}
