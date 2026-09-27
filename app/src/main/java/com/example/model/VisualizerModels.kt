package com.example.model

import androidx.compose.ui.graphics.Color

enum class VisualizerCategory(val displayName: String) {
    ALL("All (30)"),
    NEON_CYBER("Neon & Cyber"),
    SPECTRUM_WAVE("Waves & Spectrum"),
    SPACE_COSMOS("Cosmos & Particles"),
    GEOMETRIC("Geometric & 3D"),
    RETRO_ANALOG("Retro & Analog")
}

data class VisualizerEffect(
    val id: Int,
    val name: String,
    val category: VisualizerCategory,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color = Color.White
)

object VisualizerCatalog {
    val EFFECTS: List<VisualizerEffect> = listOf(
        VisualizerEffect(
            id = 1,
            name = "Neon Spectrum Bars",
            category = VisualizerCategory.NEON_CYBER,
            description = "Classic vertical frequency columns with floating glowing peak caps",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFA855F7),
            accentColor = Color(0xFFFF007F)
        ),
        VisualizerEffect(
            id = 2,
            name = "Radial Sunburst",
            category = VisualizerCategory.SPECTRUM_WAVE,
            description = "360-degree circular equalizer radiating bursts from center hub",
            primaryColor = Color(0xFFFF9900),
            secondaryColor = Color(0xFFFF0055),
            accentColor = Color(0xFFFFEA00)
        ),
        VisualizerEffect(
            id = 3,
            name = "Cyber Hologram Waves",
            category = VisualizerCategory.NEON_CYBER,
            description = "Triple stacked sine waves oscillating with holographic chromatic trails",
            primaryColor = Color(0xFF00F0FF),
            secondaryColor = Color(0xFF7000FF),
            accentColor = Color(0xFF00FF88)
        ),
        VisualizerEffect(
            id = 4,
            name = "Liquid Aurora Ribbons",
            category = VisualizerCategory.SPECTRUM_WAVE,
            description = "Silky undulating fluid streams dancing across celestial horizons",
            primaryColor = Color(0xFF10B981),
            secondaryColor = Color(0xFF06B6D4),
            accentColor = Color(0xFFA855F7)
        ),
        VisualizerEffect(
            id = 5,
            name = "Circular Pulse Orbit",
            category = VisualizerCategory.GEOMETRIC,
            description = "Expanding concentric sound rings pulsing outward with kick drums",
            primaryColor = Color(0xFFEC4899),
            secondaryColor = Color(0xFF8B5CF6),
            accentColor = Color(0xFF38BDF8)
        ),
        VisualizerEffect(
            id = 6,
            name = "Bass Volcano Eruption",
            category = VisualizerCategory.NEON_CYBER,
            description = "Vertical magma particle plumes launching upward on sub-bass drops",
            primaryColor = Color(0xFFFF3300),
            secondaryColor = Color(0xFFFF8800),
            accentColor = Color(0xFFFFDD00)
        ),
        VisualizerEffect(
            id = 7,
            name = "Floating Audio Particles",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Flock of luminescent fireflies swirling in vortex physics",
            primaryColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFFF472B6),
            accentColor = Color(0xFFFDE047)
        ),
        VisualizerEffect(
            id = 8,
            name = "Retro Synthwave Grid",
            category = VisualizerCategory.RETRO_ANALOG,
            description = "3D neon perspective wireframe horizon with pulsating sunset sun",
            primaryColor = Color(0xFFFF007F),
            secondaryColor = Color(0xFF7928CA),
            accentColor = Color(0xFFFFB703)
        ),
        VisualizerEffect(
            id = 9,
            name = "Digital VU Meter Matrix",
            category = VisualizerCategory.RETRO_ANALOG,
            description = "Studio stereo VU meter blocks with green, amber, and red peak clipping",
            primaryColor = Color(0xFF22C55E),
            secondaryColor = Color(0xFFEAB308),
            accentColor = Color(0xFFEF4444)
        ),
        VisualizerEffect(
            id = 10,
            name = "Quantum Sound Rings",
            category = VisualizerCategory.GEOMETRIC,
            description = "Interlocking orbital electron rings tilting on 3D gyroscope axes",
            primaryColor = Color(0xFF6366F1),
            secondaryColor = Color(0xFF06B6D4),
            accentColor = Color(0xFFA7F3D0)
        ),
        VisualizerEffect(
            id = 11,
            name = "Starlight Galaxy Vortex",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Spiral nebula galaxy accelerating spin rate with music tempo",
            primaryColor = Color(0xFF818CF8),
            secondaryColor = Color(0xFFC084FC),
            accentColor = Color(0xFFE0E7FF)
        ),
        VisualizerEffect(
            id = 12,
            name = "Oscilloscope Laser Trace",
            category = VisualizerCategory.RETRO_ANALOG,
            description = "CRT green phosphor electron beam drawing continuous Lissajous curves",
            primaryColor = Color(0xFF00FF66),
            secondaryColor = Color(0xFF00CC44),
            accentColor = Color(0xFFCCFFCC)
        ),
        VisualizerEffect(
            id = 13,
            name = "Hexagonal Frequency Hive",
            category = VisualizerCategory.GEOMETRIC,
            description = "Honeycomb grid where individual hexagonal cells illuminate on pitch",
            primaryColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFFD97706),
            accentColor = Color(0xFFFDE68A)
        ),
        VisualizerEffect(
            id = 14,
            name = "Supernova Shockwave",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Exploding star shockwaves flashing bright chromatic dispersion",
            primaryColor = Color(0xFFF43F5E),
            secondaryColor = Color(0xFFFB923C),
            accentColor = Color(0xFFFFFFFF)
        ),
        VisualizerEffect(
            id = 15,
            name = "Mirror Reflection Spectrum",
            category = VisualizerCategory.SPECTRUM_WAVE,
            description = "Dual symmetrical audio spectrum mirrored over calm water ripple ripples",
            primaryColor = Color(0xFF0284C7),
            secondaryColor = Color(0xFF0EA5E9),
            accentColor = Color(0xFF7DD3FC)
        ),
        VisualizerEffect(
            id = 16,
            name = "3D Wireframe Tunnel",
            category = VisualizerCategory.GEOMETRIC,
            description = "Endless geometric speed tunnel rushing forward into bass drops",
            primaryColor = Color(0xFF9333EA),
            secondaryColor = Color(0xFF3B82F6),
            accentColor = Color(0xFFF43F5E)
        ),
        VisualizerEffect(
            id = 17,
            name = "Electric Lightning Arc",
            category = VisualizerCategory.NEON_CYBER,
            description = "High-voltage Tesla coil electric bolts bridging audio nodes",
            primaryColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFF818CF8),
            accentColor = Color(0xFFE0F2FE)
        ),
        VisualizerEffect(
            id = 18,
            name = "DNA Double Helix",
            category = VisualizerCategory.GEOMETRIC,
            description = "Twisting bioluminescent molecular ladder undulating to musical harmony",
            primaryColor = Color(0xFF14B8A6),
            secondaryColor = Color(0xFFA855F7),
            accentColor = Color(0xFFF0ABFC)
        ),
        VisualizerEffect(
            id = 19,
            name = "Cyber Matrix Rain Pulse",
            category = VisualizerCategory.NEON_CYBER,
            description = "Cascading digital code streams that flare bright on beat transients",
            primaryColor = Color(0xFF22C55E),
            secondaryColor = Color(0xFF15803D),
            accentColor = Color(0xFF86EFAC)
        ),
        VisualizerEffect(
            id = 20,
            name = "Flame Equalizer Pyre",
            category = VisualizerCategory.SPECTRUM_WAVE,
            description = "Dynamic fire tongues that flicker and roar higher with audio volume",
            primaryColor = Color(0xFFEA580C),
            secondaryColor = Color(0xFFDC2626),
            accentColor = Color(0xFFFDE047)
        ),
        VisualizerEffect(
            id = 21,
            name = "Kaleidoscope Prism",
            category = VisualizerCategory.GEOMETRIC,
            description = "Six-fold reflective symmetry prism turning beats into mandalas",
            primaryColor = Color(0xFFEC4899),
            secondaryColor = Color(0xFF06B6D4),
            accentColor = Color(0xFFFACC15)
        ),
        VisualizerEffect(
            id = 22,
            name = "Frequency Waterfall",
            category = VisualizerCategory.SPECTRUM_WAVE,
            description = "Cascading spectrogram heat-map cascading down in real-time scroll",
            primaryColor = Color(0xFF8B5CF6),
            secondaryColor = Color(0xFFEC4899),
            accentColor = Color(0xFF06B6D4)
        ),
        VisualizerEffect(
            id = 23,
            name = "Plasma Energy Orb",
            category = VisualizerCategory.NEON_CYBER,
            description = "Spherical plasma filament discharge reacting to touch and bass",
            primaryColor = Color(0xFFD946EF),
            secondaryColor = Color(0xFF8B5CF6),
            accentColor = Color(0xFF67E8F9)
        ),
        VisualizerEffect(
            id = 24,
            name = "Floating Sound Bubbles",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Iridescent translucent bubbles floating and bouncing on rhythm",
            primaryColor = Color(0xFF60A5FA),
            secondaryColor = Color(0xFFA78BFA),
            accentColor = Color(0xFFF472B6)
        ),
        VisualizerEffect(
            id = 25,
            name = "Hyperdrive Warp Stars",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Relativistic light-speed star streaks stretching on volume spikes",
            primaryColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFFFFFFFF),
            accentColor = Color(0xFF93C5FD)
        ),
        VisualizerEffect(
            id = 26,
            name = "Dual Stereo Waveform",
            category = VisualizerCategory.RETRO_ANALOG,
            description = "Discrete Left & Right channels showing 8D spatial orbit balance",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFFF007F),
            accentColor = Color(0xFFFFFFFF)
        ),
        VisualizerEffect(
            id = 27,
            name = "Crystal Gem Resonance",
            category = VisualizerCategory.GEOMETRIC,
            description = "Floating faceted crystal polygons refracting spectral light beams",
            primaryColor = Color(0xFF2DD4BF),
            secondaryColor = Color(0xFF38BDF8),
            accentColor = Color(0xFFE879F9)
        ),
        VisualizerEffect(
            id = 28,
            name = "Cosmic Black Hole Horizon",
            category = VisualizerCategory.SPACE_COSMOS,
            description = "Glowing gravitational accretion disc warping audio rings around singularity",
            primaryColor = Color(0xFFF97316),
            secondaryColor = Color(0xFF7C3AED),
            accentColor = Color(0xFFFDE047)
        ),
        VisualizerEffect(
            id = 29,
            name = "Retro Cassette Tape Reels",
            category = VisualizerCategory.RETRO_ANALOG,
            description = "Vintage audio cassette with rotating spools and vibrating tape ribbon",
            primaryColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFF78716C),
            accentColor = Color(0xFFE5E5E5)
        ),
        VisualizerEffect(
            id = 30,
            name = "Cyber City Skyline Beats",
            category = VisualizerCategory.NEON_CYBER,
            description = "Futuristic metropolis skyscrapers lighting up windows to sound spectrum",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFFF007F),
            accentColor = Color(0xFFFACC15)
        )
    )
}
