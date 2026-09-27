package com.example.model

/**
 * 13-band Equalizer definitions and presets.
 * Exact 13 center frequencies:
 * 32 Hz, 64 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 10 kHz, 12 kHz, 14 kHz, 16 kHz
 */
object EqualizerFrequencies {
    val BANDS_13 = listOf(
        "32 Hz",
        "64 Hz",
        "125 Hz",
        "250 Hz",
        "500 Hz",
        "1 kHz",
        "2 kHz",
        "4 kHz",
        "8 kHz",
        "10 kHz",
        "12 kHz",
        "14 kHz",
        "16 kHz"
    )

    val FREQ_HZ = listOf(
        32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 10000, 12000, 14000, 16000
    )
}

data class EqualizerPreset(
    val name: String,
    val gains: List<Float> // 13 values in dB (range -12f to +12f)
)

object EqualizerPresets {
    val PRESETS = listOf(
        EqualizerPreset(
            name = "Flat",
            gains = List(13) { 0f }
        ),
        EqualizerPreset(
            name = "Bass Boost",
            gains = listOf(8f, 7.5f, 6.5f, 4.5f, 2.5f, 0.5f, 0f, 0f, 0f, 0.5f, 0.5f, 1f, 1f)
        ),
        EqualizerPreset(
            name = "Treble Boost",
            gains = listOf(0f, 0f, 0f, 0.5f, 1f, 2f, 3.5f, 5f, 6.5f, 7.5f, 8f, 7.5f, 6.5f)
        ),
        EqualizerPreset(
            name = "Electronic",
            gains = listOf(6f, 5f, 3.5f, 0f, -1.5f, 1f, 2f, 3f, 4.5f, 5.5f, 6f, 5.5f, 4.5f)
        ),
        EqualizerPreset(
            name = "Rock",
            gains = listOf(5f, 4f, 3f, 1f, -1f, -2f, 0f, 2.5f, 4.5f, 5f, 5.5f, 5f, 4.5f)
        ),
        EqualizerPreset(
            name = "Vocal",
            gains = listOf(-2f, -1.5f, 0f, 2f, 4.5f, 5.5f, 5f, 3.5f, 2f, 1f, 0f, 0f, 0f)
        ),
        EqualizerPreset(
            name = "Hip-Hop",
            gains = listOf(7f, 6.5f, 5f, 2.5f, 1f, 0f, 1.5f, 2.5f, 3.5f, 4f, 4.5f, 4f, 3.5f)
        ),
        EqualizerPreset(
            name = "Acoustic",
            gains = listOf(3f, 2.5f, 2f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 4f, 3.5f)
        ),
        EqualizerPreset(
            name = "Jazz",
            gains = listOf(3.5f, 3f, 2f, 1.5f, -1f, -1f, 0f, 1.5f, 2.5f, 3f, 3.5f, 3f, 2.5f)
        ),
        EqualizerPreset(
            name = "Cyberpunk",
            gains = listOf(8f, 7f, 4f, 1f, -2f, 2f, 4f, 5f, 6.5f, 7.5f, 8f, 7f, 6f)
        ),
        EqualizerPreset(
            name = "Hi-Res Custom",
            gains = listOf(4f, 3.5f, 2f, 1f, 0.5f, 1f, 2f, 3f, 4.5f, 5.5f, 6f, 5.5f, 4.5f)
        ),
        EqualizerPreset(
            name = "Custom",
            gains = List(13) { 0f }
        )
    )
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val selectedPresetName: String = "Flat",
    val bandGains: List<Float> = List(13) { 0f },
    val preAmpGainDb: Float = 0f,
    val bassPunch: Float = 0.5f,
    val trebleSparkle: Float = 0.5f
)
