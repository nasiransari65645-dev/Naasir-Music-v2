package com.example.audio

import kotlin.math.sin
import kotlin.math.tanh

/**
 * High-Performance Stereo Delay / Echo DSP Audio Processor.
 *
 * Implements a dual-channel circular buffer delay line with stereo cross-feed
 * (ping-pong effect), feedback saturation limiting, and wet/dry mix blending.
 *
 * Specifications:
 * - Delay Time: 50ms to 1000ms (default 350ms)
 * - Feedback: 0% to 85% (default 40%)
 * - Wet/Dry Mix: 0% to 100% (default 30%)
 * - Stereo Cross-Feed (Ping-Pong): Delayed taps alternate between left and right channels for earphones
 * - Master Bypass toggle
 */
class StereoDelayAudioProcessor(
    private val sampleRate: Int = 44100
) {
    // Maximum delay buffer: 1.2 seconds at 48kHz = ~57,600 stereo frames
    private val maxDelayFrames = (sampleRate * 1.2).toInt().coerceAtLeast(48000)

    // Circular delay lines for Left and Right audio channels
    private val bufferLeft = FloatArray(maxDelayFrames)
    private val bufferRight = FloatArray(maxDelayFrames)
    private var writeIndex = 0

    // Master settings with thread-safe visibility
    @Volatile
    var isEnabled: Boolean = false

    @Volatile
    var delayTimeMs: Int = 350
        set(value) {
            field = value.coerceIn(50, 1000)
        }

    @Volatile
    var feedbackPercent: Int = 40
        set(value) {
            field = value.coerceIn(0, 85)
        }

    @Volatile
    var mixPercent: Int = 30
        set(value) {
            field = value.coerceIn(0, 100)
        }

    /**
     * Resets the internal circular buffers to eliminate trailing audio echoes.
     */
    fun resetBuffers() {
        bufferLeft.fill(0f)
        bufferRight.fill(0f)
        writeIndex = 0
    }

    /**
     * Processes interleaved 16-bit stereo PCM audio in-place.
     * @param pcmData ByteArray containing signed 16-bit little-endian stereo PCM
     * @param offset starting index in the byte array
     * @param length number of bytes to process
     */
    fun processPcmStereo(pcmData: ByteArray, offset: Int, length: Int) {
        if (!isEnabled || length < 4) return

        val wet = (mixPercent / 100f).coerceIn(0f, 1f)
        if (wet <= 0f) return

        val dry = 1f - wet
        val feedback = (feedbackPercent / 100f).coerceIn(0f, 0.85f)
        val delayFrames = ((delayTimeMs.toFloat() / 1000f) * sampleRate).toInt().coerceIn(1, maxDelayFrames - 1)

        val totalFrames = length / 4 // 2 bytes per sample * 2 channels = 4 bytes per stereo frame
        var byteIdx = offset

        for (i in 0 until totalFrames) {
            // Read 16-bit Little-Endian Left Sample
            val rawL = (pcmData[byteIdx].toInt() and 0xFF) or (pcmData[byteIdx + 1].toInt() shl 8)
            val sampleL = (if (rawL >= 0x8000) rawL - 0x10000 else rawL) / 32768f

            // Read 16-bit Little-Endian Right Sample
            val rawR = (pcmData[byteIdx + 2].toInt() and 0xFF) or (pcmData[byteIdx + 3].toInt() shl 8)
            val sampleR = (if (rawR >= 0x8000) rawR - 0x10000 else rawR) / 32768f

            // Calculate Circular Read Index
            val readIndex = (writeIndex - delayFrames + maxDelayFrames) % maxDelayFrames

            // Fetch delayed taps
            val tapLeft = bufferLeft[readIndex]
            val tapRight = bufferRight[readIndex]

            // Ping-Pong Cross-Feed Topology:
            // Left delayed tap feeds into Right buffer, Right delayed tap feeds into Left buffer.
            // Soft saturation with tanh prevents harsh digital clipping on repetitive echoes.
            val newEchoL = tanh(sampleL + (tapRight * feedback))
            val newEchoR = tanh(sampleR + (tapLeft * feedback))

            bufferLeft[writeIndex] = newEchoL
            bufferRight[writeIndex] = newEchoR

            // Wet ping-pong stereo output with wide earphone soundstage:
            // Delayed tap alternates smoothly across earphone stereo channels
            val outL = ((sampleL * dry) + (tapRight * wet)).coerceIn(-1.0f, 1.0f)
            val outR = ((sampleR * dry) + (tapLeft * wet)).coerceIn(-1.0f, 1.0f)

            // Write back to 16-bit Little-Endian
            val intL = (outL * 32767f).toInt().coerceIn(-32768, 32767)
            pcmData[byteIdx] = (intL and 0xFF).toByte()
            pcmData[byteIdx + 1] = ((intL shr 8) and 0xFF).toByte()

            val intR = (outR * 32767f).toInt().coerceIn(-32768, 32767)
            pcmData[byteIdx + 2] = (intR and 0xFF).toByte()
            pcmData[byteIdx + 3] = ((intR shr 8) and 0xFF).toByte()

            byteIdx += 4
            writeIndex = (writeIndex + 1) % maxDelayFrames
        }
    }

    /**
     * Calculates the real-time ping-pong pan modulation factor for earphones
     * based on the current playback time position and delay interval.
     * Alternates smoothly between -1.0 (left ear) and +1.0 (right ear).
     */
    fun calculatePingPongPanOffset(currentPlaybackMs: Long): Float {
        if (!isEnabled || mixPercent <= 0) return 0f
        val interval = delayTimeMs.coerceAtLeast(50)
        // Angular frequency synchronized to ping-pong echo delay cycle
        val cycle = (currentPlaybackMs % (interval * 2L)).toFloat() / (interval * 2L)
        val angle = cycle * 2.0 * Math.PI
        val pingPongSine = sin(angle).toFloat()
        val intensity = (mixPercent / 100f) * 0.35f
        return pingPongSine * intensity
    }
}
