package com.example.model

import androidx.compose.ui.graphics.Color

enum class VolumeGaugePosition(val displayName: String) {
    RIGHT("Right Edge"),
    LEFT("Left Edge")
}

enum class VolumeGaugeTheme(
    val displayName: String,
    val primaryColor: Color,
    val glowColor: Color
) {
    CYAN_NEON("Cyan Neon", Color(0xFF00E5FF), Color(0xFF38BDF8)),
    CYBER_VIOLET("Cyber Violet", Color(0xFFA855F7), Color(0xFFE879F9)),
    LIME_GREEN("Lime Green", Color(0xFF10B981), Color(0xFF34D399)),
    ELECTRIC_AMBER("Electric Amber", Color(0xFFF59E0B), Color(0xFFFBBF24))
}

data class VolumeGaugeSettings(
    val stepSizePercent: Int = 5, // 2, 5, 10
    val position: VolumeGaugePosition = VolumeGaugePosition.RIGHT,
    val theme: VolumeGaugeTheme = VolumeGaugeTheme.CYAN_NEON,
    val hapticEnabled: Boolean = true,
    val autoDismissDelaySec: Float = 2.0f, // 1.5, 2.0, 3.0, 5.0
    val floatingOverlayEnabled: Boolean = true
)
