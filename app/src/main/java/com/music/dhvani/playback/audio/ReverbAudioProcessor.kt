package com.music.dhvani.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lightweight, high-fidelity algorithmic room reverberator based on Freeverb DSP.
 * Runs directly inside ExoPlayer's [AudioProcessor] pipeline so it is 100% immune
 * to OEM Android audio effect drops, works across all Bluetooth codecs and headsets,
 * and adds spacey, atmospheric depth for Slowed + Reverb mode.
 *
 * Architecture:
 * - 8 parallel Lowpass Feedback Comb (LBCF) filters per stereo channel with stereo spread
 * - 4 cascaded Allpass filters per channel for reflection diffusion
 * - Zero allocation in the audio rendering loop; passes straight through when wet == 0.
 */
@UnstableApi
class ReverbAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var enabled: Boolean = false

    /** Wet level: 0.0 (dry only) to 1.0 (fully reverberant). Default 0.4 for sweet lofi room. */
    @Volatile
    var wet: Float = 0.40f

    /** Dry level: untouched direct sound factor. */
    @Volatile
    var dry: Float = 1.0f

    /** Reverb decay / room size (0.0 to 1.0). */
    @Volatile
    var roomSize: Float = 0.82f

    /** High frequency damping factor (0.0 to 1.0) — higher values give a warmer, softer tail. */
    @Volatile
    var damp: Float = 0.35f

    /**
     * Lo-Fi Warmth / Low-Pass Muffle factor (0.0 = crisp/open, up to 0.85 = dreamy, warm "next-door / rainy day" aesthetic).
     */
    @Volatile
    var lofiWarmth: Float = 0.0f

    private var filterL: Float = 0f
    private var filterR: Float = 0f

    // Internal comb filter helper
    private class CombFilter(bufferSize: Int) {
        val buffer = FloatArray(bufferSize)
        var bufferIndex = 0
        var filterStore = 0f

        fun process(input: Float, feedback: Float, damp: Float): Float {
            val output = buffer[bufferIndex]
            filterStore = (output * (1f - damp)) + (filterStore * damp)
            buffer[bufferIndex] = input + (filterStore * feedback)
            if (++bufferIndex >= buffer.size) {
                bufferIndex = 0
            }
            return output
        }

        fun mute() {
            buffer.fill(0f)
            bufferIndex = 0
            filterStore = 0f
        }
    }

    // Internal allpass filter helper
    private class AllpassFilter(bufferSize: Int) {
        val buffer = FloatArray(bufferSize)
        var bufferIndex = 0

        fun process(input: Float): Float {
            val bufOut = buffer[bufferIndex]
            val output = -input + bufOut
            buffer[bufferIndex] = input + (bufOut * 0.5f)
            if (++bufferIndex >= buffer.size) {
                bufferIndex = 0
            }
            return output
        }

        fun mute() {
            buffer.fill(0f)
            bufferIndex = 0
        }
    }

    private var combLeft = emptyArray<CombFilter>()
    private var combRight = emptyArray<CombFilter>()
    private var allpassLeft = emptyArray<AllpassFilter>()
    private var allpassRight = emptyArray<AllpassFilter>()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRateRatio = inputAudioFormat.sampleRate / 44100.0f
        val stereoSpread = (23 * sampleRateRatio).toInt()

        // Standard tuned delays (scaled by sample rate)
        val combTunings = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        val allpassTunings = intArrayOf(556, 441, 341, 225)

        combLeft = Array(combTunings.size) { i ->
            CombFilter((combTunings[i] * sampleRateRatio).toInt().coerceAtLeast(1))
        }
        combRight = Array(combTunings.size) { i ->
            CombFilter(((combTunings[i] + stereoSpread) * sampleRateRatio).toInt().coerceAtLeast(1))
        }

        allpassLeft = Array(allpassTunings.size) { i ->
            AllpassFilter((allpassTunings[i] * sampleRateRatio).toInt().coerceAtLeast(1))
        }
        allpassRight = Array(allpassTunings.size) { i ->
            AllpassFilter(((allpassTunings[i] + stereoSpread) * sampleRateRatio).toInt().coerceAtLeast(1))
        }

        return inputAudioFormat
    }

    override fun onFlush() {
        for (c in combLeft) c.mute()
        for (c in combRight) c.mute()
        for (a in allpassLeft) a.mute()
        for (a in allpassRight) a.mute()
        filterL = 0f
        filterR = 0f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val frameCount = inputBuffer.remaining() / BYTES_PER_FRAME
        if (frameCount == 0) return
        val outputBuffer = replaceOutputBuffer(frameCount * BYTES_PER_FRAME)

        val currentWet = wet
        if (!enabled || currentWet <= 0.001f || combLeft.isEmpty()) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.order(ByteOrder.nativeOrder())

        val currentDry = dry
        val currentRoomSize = roomSize.coerceIn(0.1f, 0.95f)
        val currentDamp = damp.coerceIn(0.0f, 0.9f)
        val currentWarmth = lofiWarmth.coerceIn(0.0f, 0.90f)
        val alpha = if (currentWarmth > 0.01f) (1f - currentWarmth * 0.75f) else 1f
        val gain = 0.015f // Input attenuation to prevent comb accumulator blowup

        repeat(frameCount) {
            val inLeft = inputBuffer.short.toFloat()
            val inRight = inputBuffer.short.toFloat()

            val monoIn = (inLeft + inRight) * gain

            var outLeft = 0f
            var outRight = 0f

            // 8 parallel comb filters
            for (i in combLeft.indices) {
                outLeft += combLeft[i].process(monoIn, currentRoomSize, currentDamp)
                outRight += combRight[i].process(monoIn, currentRoomSize, currentDamp)
            }

            // 4 series all-pass filters
            for (i in allpassLeft.indices) {
                outLeft = allpassLeft[i].process(outLeft)
                outRight = allpassRight[i].process(outRight)
            }

            // Mix dry and wet signals
            var mixedLeft = (inLeft * currentDry) + (outLeft * currentWet * 2.5f)
            var mixedRight = (inRight * currentDry) + (outRight * currentWet * 2.5f)

            // Warm low-pass filter stage
            if (alpha < 0.99f) {
                filterL = (alpha * mixedLeft) + ((1f - alpha) * filterL)
                filterR = (alpha * mixedRight) + ((1f - alpha) * filterR)
                mixedLeft = filterL
                mixedRight = filterR
            }

            outputBuffer.putShort(clampToShort(mixedLeft))
            outputBuffer.putShort(clampToShort(mixedRight))
        }

        outputBuffer.flip()
    }

    override fun onReset() {
        super.onReset()
        filterL = 0f
        filterR = 0f
    }

    private fun clampToShort(value: Float): Short =
        value.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()

    private companion object {
        const val BYTES_PER_FRAME = 4 // stereo 16-bit PCM
    }
}
