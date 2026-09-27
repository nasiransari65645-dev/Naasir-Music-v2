package com.example.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates sample offline audio tracks with real 16-bit PCM WAV stereo data.
 * This guarantees the user can immediately test playback, 3D surround sound,
 * and the Left-to-Right auto-rotate panning system offline out-of-the-box.
 */
object SampleAudioGenerator {

    data class SampleTrackDef(
        val filename: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val melodyType: Int
    )

    val SAMPLE_TRACKS = listOf(
        SampleTrackDef(
            filename = "naasir_8d_orbit_anthem.wav",
            title = "Naasir 8D Spatial Anthem",
            artist = "Naasir Studio",
            album = "Spatial Horizons",
            durationSeconds = 24,
            melodyType = 1
        ),
        SampleTrackDef(
            filename = "midnight_cyber_resonance.wav",
            title = "Midnight Cyber Resonance",
            artist = "Acoustic Lab",
            album = "Surround Odyssey",
            durationSeconds = 20,
            melodyType = 2
        ),
        SampleTrackDef(
            filename = "lofi_spatial_breeze.wav",
            title = "Lo-Fi Spatial Breeze",
            artist = "Chill Horizon",
            album = "Binaural Dreams",
            durationSeconds = 22,
            melodyType = 3
        )
    )

    fun ensureSampleTracksExist(context: Context): List<File> {
        val samplesDir = File(context.filesDir, "samples").apply { mkdirs() }
        val generatedFiles = mutableListOf<File>()

        for (track in SAMPLE_TRACKS) {
            val file = File(samplesDir, track.filename)
            // Ensure 44.1kHz full wav file exists
            val minExpectedSize = 44100 * 2 * 2 * track.durationSeconds // 44.1kHz 16-bit stereo
            if (!file.exists() || file.length() < (minExpectedSize - 1000)) {
                generateWavFile(file, track.durationSeconds, track.melodyType)
            }
            if (file.exists() && file.length() > 1000) {
                generatedFiles.add(file)
            }
        }
        return generatedFiles
    }

    private fun generateWavFile(file: File, durationSeconds: Int, melodyType: Int) {
        val sampleRate = 44100
        val channels = 2 // Stereo
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * channels * (bitsPerSample / 8)

        FileOutputStream(file).use { fos ->
            // WAV Header
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(36 + dataSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size
                putShort(1) // AudioFormat (1 = PCM)
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * channels * (bitsPerSample / 8)) // ByteRate
                putShort((channels * (bitsPerSample / 8)).toShort()) // BlockAlign
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            // Generate stereo samples
            val buffer = ByteBuffer.allocate(sampleRate * channels * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }

            // Note frequencies (Hz)
            val notes = when (melodyType) {
                1 -> listOf(261.63, 329.63, 392.00, 523.25, 440.0, 349.23, 392.0, 523.25) // C major arpeggio
                2 -> listOf(220.00, 261.63, 329.63, 440.00, 493.88, 392.00, 329.63, 293.66) // A minor electro
                else -> listOf(196.00, 246.94, 293.66, 370.00, 329.63, 246.94, 220.00, 196.00) // G maj chill
            }

            var noteIndex = 0
            val samplesPerNote = sampleRate / 2

            for (i in 0 until totalSamples) {
                if (i % samplesPerNote == 0) {
                    noteIndex = (noteIndex + 1) % notes.size
                }
                val freq = notes[noteIndex]
                val t = i.toDouble() / sampleRate

                // Synth note with harmonic overtones
                val envelope = 1.0 - (i % samplesPerNote).toDouble() / samplesPerNote * 0.4
                val wave1 = sin(2 * PI * freq * t)
                val wave2 = 0.5 * sin(2 * PI * freq * 2.0 * t)
                val wave3 = 0.25 * sin(2 * PI * (freq * 0.5) * t) // Bass sub
                val sampleValue = ((wave1 + wave2 + wave3) * envelope * 0.35 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                // Stereo distribution (subtle stereo separation built-in)
                val leftSample = sampleValue
                val rightSample = (sampleValue * 0.95).toInt().toShort()

                buffer.putShort(leftSample)
                buffer.putShort(rightSample)

                if (!buffer.hasRemaining()) {
                    fos.write(buffer.array())
                    buffer.clear()
                }
            }

            if (buffer.position() > 0) {
                fos.write(buffer.array(), 0, buffer.position())
            }
        }
    }
}
