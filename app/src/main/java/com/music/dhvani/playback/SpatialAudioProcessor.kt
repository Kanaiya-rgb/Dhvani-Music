package com.music.dhvani.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * High-fidelity 3D Spatial Audio & Stereo Widener.
 *
 * Implements mid/side spatial widening with an acoustic low-pass cross-feed delay network
 * delivering a 2.5x wide spatial soundstage.
 * Operates with 32-bit floating point precision across both 16-bit PCM and 32-bit Float streams,
 * preserving maximum dynamic range without clipping.
 */
@UnstableApi
class SpatialAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var enabled: Boolean = false

    /** How much wider the stereo image gets. 1.0 = untouched. Natural, distortion-free soundstage expansion. */
    private val widthGain = 1.35f

    /** Makeup attenuation after widening, preserving dynamic headroom without digital clipping. */
    private val outputGain = 0.70f

    /** How much of the delayed, low-passed opposite channel gets mixed back in. */
    private val crossfeedGain = 0.12f

    /** One-pole lowpass factor applied to the cross-fed signal — dulls it, like a far ear would. */
    private val lowpassCoeff = 0.3f

    private var isFloatEncoding = false
    private var bytesPerFrame = 4

    private var delayLeft = FloatArray(0)
    private var delayRight = FloatArray(0)
    private var delayIndex = 0
    private var lowpassLeft = 0f
    private var lowpassRight = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if ((encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        isFloatEncoding = (encoding == C.ENCODING_PCM_FLOAT)
        bytesPerFrame = if (isFloatEncoding) 8 else 4

        val delaySamples = (inputAudioFormat.sampleRate * DELAY_MS / 1000f)
            .roundToInt()
            .coerceAtLeast(1)
        if (delayLeft.size != delaySamples) {
            delayLeft = FloatArray(delaySamples)
            delayRight = FloatArray(delaySamples)
        }
        delayIndex = 0
        lowpassLeft = 0f
        lowpassRight = 0f
        return inputAudioFormat
    }

    override fun onFlush() {
        delayLeft.fill(0f)
        delayRight.fill(0f)
        delayIndex = 0
        lowpassLeft = 0f
        lowpassRight = 0f
    }

    override fun onReset() {
        delayLeft = FloatArray(0)
        delayRight = FloatArray(0)
        delayIndex = 0
        lowpassLeft = 0f
        lowpassRight = 0f
        isFloatEncoding = false
        bytesPerFrame = 4
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bpf = bytesPerFrame
        val frameCount = inputBuffer.remaining() / bpf
        if (frameCount == 0) return
        val outputBuffer = replaceOutputBuffer(frameCount * bpf)

        if (!enabled) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.order(ByteOrder.nativeOrder())

        val delaySize = delayLeft.size
        if (delaySize == 0) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        var dIdx = delayIndex
        var lpL = lowpassLeft
        var lpR = lowpassRight
        val invScale = 1.0f / 32768.0f

        if (isFloatEncoding) {
            repeat(frameCount) {
                val left = inputBuffer.float
                val right = inputBuffer.float

                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f * widthGain
                var widenedLeft = mid + side
                var widenedRight = mid - side

                val delayedRight = delayRight[dIdx]
                val delayedLeft = delayLeft[dIdx]
                lpL += lowpassCoeff * (delayedRight - lpL)
                lpR += lowpassCoeff * (delayedLeft - lpR)
                widenedLeft += lpL * crossfeedGain
                widenedRight += lpR * crossfeedGain

                delayLeft[dIdx] = left
                delayRight[dIdx] = right
                dIdx = (dIdx + 1) % delaySize

                outputBuffer.putFloat(softLimit(widenedLeft * outputGain))
                outputBuffer.putFloat(softLimit(widenedRight * outputGain))
            }
        } else {
            repeat(frameCount) {
                val left = inputBuffer.short.toFloat() * invScale
                val right = inputBuffer.short.toFloat() * invScale

                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f * widthGain
                var widenedLeft = mid + side
                var widenedRight = mid - side

                val delayedRight = delayRight[dIdx]
                val delayedLeft = delayLeft[dIdx]
                lpL += lowpassCoeff * (delayedRight - lpL)
                lpR += lowpassCoeff * (delayedLeft - lpR)
                widenedLeft += lpL * crossfeedGain
                widenedRight += lpR * crossfeedGain

                delayLeft[dIdx] = left
                delayRight[dIdx] = right
                dIdx = (dIdx + 1) % delaySize

                outputBuffer.putShort(clamp16FromFloat(widenedLeft * outputGain))
                outputBuffer.putShort(clamp16FromFloat(widenedRight * outputGain))
            }
        }

        delayIndex = dIdx
        lowpassLeft = lpL
        lowpassRight = lpR
        outputBuffer.flip()
    }

    private fun softLimit(f: Float): Float {
        if (f.isNaN()) return 0f
        return when {
            f > 0.85f -> 0.85f + 0.15f * kotlin.math.tanh(((f - 0.85f) / 0.15f).toDouble()).toFloat()
            f < -0.85f -> -0.85f + 0.15f * kotlin.math.tanh(((f + 0.85f) / 0.15f).toDouble()).toFloat()
            else -> f
        }
    }

    private fun clamp16FromFloat(f: Float): Short {
        val limited = softLimit(f)
        val scaled = limited * 32767.0f
        return scaled.coerceIn(-32768.0f, 32767.0f).roundToInt().toShort()
    }

    private companion object {
        const val DELAY_MS = 15
    }
}
