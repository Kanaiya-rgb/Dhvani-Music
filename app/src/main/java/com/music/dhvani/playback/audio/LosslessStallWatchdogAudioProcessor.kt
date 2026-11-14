/**
 * Ported from Convx (C) 2026 under GPL-3.0
 */

package com.music.dhvani.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Lossless streams can silently stop producing audio while playback
 * position keeps advancing (decoder/AudioTrack stall) - no PlaybackException
 * is ever thrown, so bypassing non-16-bit PCM (used by hi-res FLAC / passthrough)
 * means the existing "skip silence" feature can't catch it either.
 *
 * This is a dedicated watchdog covering 16/24/32-bit PCM and float PCM
 * - the encodings lossless streams actually use.
 */
@UnstableApi
class LosslessStallWatchdogAudioProcessor(
    private val minStallDurationUs: Long = 10_000_000L,
    private val silenceThreshold: Int = 16,
    private val onStallDetected: () -> Unit,
) : BaseAudioProcessor() {

    @Volatile
    var armed: Boolean = false

    private var sampleRate = 0
    private var channelCount = 0
    private var bytesPerSample = 0
    private var isFloat = false

    private var consecutiveSilentFrames: Long = 0
    private var notifiedThisStall = false

    override fun isActive(): Boolean = armed && sampleRate > 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        val (bytes, float) = bytesPerSampleFor(inputAudioFormat.encoding)
        bytesPerSample = bytes
        isFloat = float
        if (bytesPerSample == 0) {
            sampleRate = 0
            channelCount = 0
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (armed && sampleRate > 0 && channelCount > 0) {
            detectStall(inputBuffer)
        } else {
            resetTracking()
        }

        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        out.flip()
    }

    private fun detectStall(inputBuffer: ByteBuffer) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val frameSize = bytesPerSample * channelCount
        if (frameSize <= 0) return
        val frameCount = inputBuffer.remaining() / frameSize
        val basePosition = inputBuffer.position()

        val step = 16
        var frameIndex = 0
        while (frameIndex < frameCount) {
            var framePeak = 0
            for (channelIndex in 0 until channelCount) {
                val sampleIndex = basePosition + (frameIndex * channelCount + channelIndex) * bytesPerSample
                val magnitude = sampleMagnitude(inputBuffer, sampleIndex)
                if (magnitude > framePeak) framePeak = magnitude
            }

            if (framePeak < silenceThreshold) {
                consecutiveSilentFrames += step
                val stallDurationUs = (consecutiveSilentFrames * 1_000_000L) / sampleRate
                if (stallDurationUs >= minStallDurationUs && !notifiedThisStall) {
                    notifiedThisStall = true
                    onStallDetected()
                }
            } else {
                resetTracking()
            }
            frameIndex += step
        }
    }

    /** Returns a value in the same [0, 32767]-ish range regardless of source bit depth. */
    private fun sampleMagnitude(buffer: ByteBuffer, index: Int): Int {
        return if (isFloat) {
            (abs(buffer.getFloat(index)) * 32767f).toInt()
        } else {
            when (bytesPerSample) {
                2 -> abs(buffer.getShort(index).toInt())
                3 -> {
                    val b0 = buffer.get(index).toInt() and 0xFF
                    val b1 = buffer.get(index + 1).toInt() and 0xFF
                    val b2 = buffer.get(index + 2).toInt()
                    val value = (b2 shl 16) or (b1 shl 8) or b0
                    abs(value) shr 8
                }
                4 -> abs(buffer.getInt(index)) shr 16
                else -> 0
            }
        }
    }

    private fun bytesPerSampleFor(encoding: Int): Pair<Int, Boolean> = when (encoding) {
        C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2 to false
        C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3 to false
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> 4 to false
        C.ENCODING_PCM_FLOAT -> 4 to true
        else -> 0 to false
    }

    private fun resetTracking() {
        consecutiveSilentFrames = 0
        notifiedThisStall = false
    }

    fun resetForNewTrack() {
        resetTracking()
    }

    override fun onFlush() {
        resetTracking()
    }

    override fun onReset() {
        resetTracking()
        sampleRate = 0
        channelCount = 0
        bytesPerSample = 0
        isFloat = false
    }
}
