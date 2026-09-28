package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerFrequencies
import com.example.model.EqualizerPresets
import com.example.model.EqualizerState
import com.example.ui.components.RackRotaryKnob

/**
 * Production-ready 13-Band Pro Equalizer
 * 1. Main Parent View: Clean category card leading to sub-screen (no sliders/toggles on outer card)
 * 2. Dedicated Sub-Screen:
 *    - Master ON/OFF toggle with state badge
 *    - Reset to Flat button
 *    - Frequency Response curve visualizer
 *    - Presets chips
 *    - 13 high-precision faders (-12 dB to +12 dB)
 *    - Pre-amp, Bass Punch, and Treble Sparkle enhancers
 */
@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState? = null,
    onToggleEnabled: (Boolean) -> Unit = {},
    onSelectPreset: (String) -> Unit = {},
    onSetBandGain: (Int, Float) -> Unit = { _, _ -> },
    onSetPreAmp: (Float) -> Unit = {},
    onSetBassPunch: (Float) -> Unit = {},
    onSetTrebleSparkle: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val eq = equalizerState ?: EqualizerState()
    var isSubScreenOpen by remember { mutableStateOf(false) }

    if (isSubScreenOpen) {
        BackHandler { isSubScreenOpen = false }
        EqualizerSubScreen(
            eq = eq,
            onBackClick = { isSubScreenOpen = false },
            onToggleEnabled = onToggleEnabled,
            onSelectPreset = onSelectPreset,
            onSetBandGain = onSetBandGain,
            onSetPreAmp = onSetPreAmp,
            onSetBassPunch = onSetBassPunch,
            onSetTrebleSparkle = onSetTrebleSparkle,
            modifier = modifier
        )
    } else {
        EqualizerParentScreen(
            eq = eq,
            onOpenSubScreen = { isSubScreenOpen = true },
            modifier = modifier
        )
    }
}

/**
 * Main "Equalizer" Tab Screen (Clean Parent View)
 * Clean vertical layout with primary category card and chevron indicator.
 * No knobs or sliders on this outer card per user specification.
 */
@Composable
private fun EqualizerParentScreen(
    eq: EqualizerState,
    onOpenSubScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("equalizer_parent_screen")
    ) {
        // Top App Title
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        // Primary Category Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenSubScreen() }
                .testTag("equalizer_primary_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                if (eq.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Large glowing circular badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (eq.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Center: Title and dynamic subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "13-Band Equalizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (eq.isEnabled) {
                            "Active • ${eq.selectedPresetName} • Tap to configure"
                        } else {
                            "Disabled • Tap to configure frequency bands"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                // Right: ONLY a clean chevron arrow indicator ( > )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open 13-Band Equalizer",
                    tint = if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Informational Note Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "High-precision 13-band hardware audio processor. Tap the category card above to adjust individual frequencies, switch acoustic presets, or fine-tune pre-amp and punch levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Dedicated "13-Band Equalizer" Sub-Screen (Opens on Card Click)
 */
@Composable
private fun EqualizerSubScreen(
    eq: EqualizerState,
    onBackClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSetBandGain: (Int, Float) -> Unit,
    onSetPreAmp: (Float) -> Unit,
    onSetBassPunch: (Float) -> Unit,
    onSetTrebleSparkle: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("equalizer_sub_screen")
    ) {
        // Top Bar with Back Arrow and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("equalizer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "13-Band Equalizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Reset to Flat button
            OutlinedButton(
                onClick = {
                    onSelectPreset("Flat")
                    onSetPreAmp(0f)
                    onSetBassPunch(0.5f)
                    onSetTrebleSparkle(0.5f)
                    for (i in 0 until 13) {
                        onSetBandGain(i, 0f)
                    }
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("equalizer_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Flat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Control Header: Master ON/OFF Switch with State Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (eq.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // State Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (eq.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = if (eq.isEnabled) "DSP ACTIVE" else "DSP BYPASS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Sound Enhancer",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (eq.isEnabled) "Master DSP Engine Active" else "Master DSP Engine Bypassed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = eq.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("equalizer_master_switch")
                )
            }
        }

        // Collapsible Controls (fold/unfold based on Sound Enhancer master switch state)
        AnimatedVisibility(
            visible = eq.isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // Visual Frequency Response Curve Canvas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    FrequencyResponseCurve(
                        bandGains = eq.bandGains,
                        isEnabled = eq.isEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }

        // Presets Selector Horizontal Row
        Text(
            text = "PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EqualizerPresets.PRESETS) { preset ->
                val isSelected = eq.selectedPresetName.equals(preset.name, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = eq.isEnabled) { onSelectPreset(preset.name) },
                    color = when {
                        !eq.isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected && eq.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = preset.name,
                        color = when {
                            !eq.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // 13 Frequency Bands
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FREQUENCY BANDS (13-BAND)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "RANGE: -12dB ~ +12dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                EqualizerFrequencies.BANDS_13.forEachIndexed { index, freqLabel ->
                    val gain = eq.bandGains.getOrElse(index) { 0f }
                    BandSliderRow(
                        frequencyLabel = freqLabel,
                        gainDb = gain,
                        isEnabled = eq.isEnabled,
                        onGainChange = { newGain -> onSetBandGain(index, newGain) },
                        index = index
                    )
                    if (index < EqualizerFrequencies.BANDS_13.size - 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Sound Tuning Enhancements: Pre-Amp, Bass Punch, Treble Sparkle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SOUND TUNING ENHANCEMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 3D Metallic Rotary Knob Dials for Pre-Amp, Bass Punch, and Treble Sparkle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RackRotaryKnob(
                        value = eq.preAmpGainDb,
                        valueRange = -10f..10f,
                        onValueChange = onSetPreAmp,
                        title = "PRE-AMP",
                        readoutText = "${if (eq.preAmpGainDb > 0) "+" else ""}${String.format("%.1f", eq.preAmpGainDb)}dB",
                        minLabel = "-10dB",
                        maxLabel = "+10dB",
                        knobSize = 64.dp,
                        activeColor = Color(0xFF00F5FF),
                        isEnabled = eq.isEnabled,
                        testTag = "rack_knob_preamp"
                    )

                    RackRotaryKnob(
                        value = eq.bassPunch,
                        valueRange = 0f..1f,
                        onValueChange = onSetBassPunch,
                        title = "BASS PUNCH",
                        readoutText = "${(eq.bassPunch * 100).toInt()}%",
                        minLabel = "0%",
                        maxLabel = "100%",
                        knobSize = 64.dp,
                        activeColor = Color(0xFF9D4EDD),
                        isEnabled = eq.isEnabled,
                        testTag = "rack_knob_bass_punch"
                    )

                    RackRotaryKnob(
                        value = eq.trebleSparkle,
                        valueRange = 0f..1f,
                        onValueChange = onSetTrebleSparkle,
                        title = "TREBLE",
                        readoutText = "${(eq.trebleSparkle * 100).toInt()}%",
                        minLabel = "0%",
                        maxLabel = "100%",
                        knobSize = 64.dp,
                        activeColor = Color(0xFF00F5FF),
                        isEnabled = eq.isEnabled,
                        testTag = "rack_knob_treble"
                    )
                }
            }
        }
    }
}
    }
}

@Composable
private fun BandSliderRow(
    frequencyLabel: String,
    gainDb: Float,
    isEnabled: Boolean,
    onGainChange: (Float) -> Unit,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = frequencyLabel,
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(62.dp)
        )

        // Fader Slider (-12 dB to +12 dB) with cyan glowing tracks
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            enabled = enabledOrDim(isEnabled),
            colors = SliderDefaults.colors(
                thumbColor = if (isEnabled) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                activeTrackColor = if (isEnabled) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outline,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("band_slider_$index")
        )

        // Value Readout
        Text(
            text = "${if (gainDb > 0) "+" else ""}${String.format("%.1f", gainDb)}dB",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) {
                if (gainDb != 0f) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
            } else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
    }
}

private fun enabledOrDim(enabled: Boolean): Boolean = enabled

/**
 * Visual Frequency Response Curve Canvas
 */
@Composable
private fun FrequencyResponseCurve(
    bandGains: List<Float>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val midY = h / 2f

        // Draw grid lines: +6dB, 0dB, -6dB
        val gridColor = outlineColor.copy(alpha = 0.4f)
        drawLine(
            color = gridColor,
            start = Offset(0f, midY - h * 0.25f),
            end = Offset(w, midY - h * 0.25f),
            strokeWidth = 1f
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.8f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, midY + h * 0.25f),
            end = Offset(w, midY + h * 0.25f),
            strokeWidth = 1f
        )

        val nBands = bandGains.size.coerceAtLeast(1)
        val stepX = w / (nBands + 1)

        val points = mutableListOf<Offset>()
        points.add(Offset(0f, midY)) // start at edge 0dB

        for (i in 0 until nBands) {
            val gain = if (isEnabled) bandGains[i] else 0f
            // gain is -12 to +12. Map to midY - (gain / 12) * (h * 0.42f)
            val normalizedY = midY - (gain / 12f) * (h * 0.42f)
            val x = stepX * (i + 1)
            points.add(Offset(x, normalizedY.coerceIn(8f, h - 8f)))
        }
        points.add(Offset(w, midY)) // end at edge 0dB

        // Smooth cubic path
        val curvePath = Path()
        val fillPath = Path()

        curvePath.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(0f, h)
        fillPath.lineTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            curvePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        // Gradient fill under curve
        val fillBrush = Brush.verticalGradient(
            colors = if (isEnabled) listOf(
                primaryColor.copy(alpha = 0.28f),
                primaryColor.copy(alpha = 0.05f),
                Color.Transparent
            ) else listOf(
                surfaceVariantColor.copy(alpha = 0.15f),
                Color.Transparent
            )
        )
        drawPath(path = fillPath, brush = fillBrush)

        // Curve stroke
        val strokeColor = if (isEnabled) primaryColor else outlineColor
        drawPath(
            path = curvePath,
            color = strokeColor,
            style = Stroke(width = if (isEnabled) 3.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glowing points at each band
        if (isEnabled) {
            for (i in 1 until points.size - 1) {
                drawCircle(
                    color = surfaceColor,
                    radius = 3.dp.toPx(),
                    center = points[i]
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f),
                    radius = 5.5.dp.toPx(),
                    center = points[i]
                )
            }
        }
    }
}
