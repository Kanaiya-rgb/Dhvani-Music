package com.music.flux.playback.eq

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * AudioProcessor for ExoPlayer applying parametric EQ using Biquad filters.
 */
@UnstableApi
class CustomEqualizerAudioProcessor : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channelCount = 0
    @Volatile
    private var equalizerEnabled = false

    private var filters: List<BiquadFilter> = emptyList()
    private var preampGain: Double = 1.0
    private var pendingProfile: ParametricEQ? = null

    companion object {
        private const val TAG = "CustomEqualizerAudioProc"
    }

    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        if (sampleRate == 0) {
            Log.d(TAG, "Audio processor not configured yet. Storing profile as pending (${parametricEQ.bands.size} bands)")
            pendingProfile = parametricEQ
            return
        }

        preampGain = 10.0.pow(parametricEQ.preamp / 20.0)
        createFilters(parametricEQ.bands)
        equalizerEnabled = true
        filters.forEach { it.reset() }
        Log.d(TAG, "Applied EQ profile with ${filters.size} bands, preamp ${parametricEQ.preamp} dB")
    }

    @Synchronized
    fun disable() {
        equalizerEnabled = false
        filters = emptyList()
        preampGain = 1.0
        pendingProfile = null
        Log.d(TAG, "Equalizer disabled")
    }

    fun isEnabled(): Boolean = equalizerEnabled

    private fun createFilters(bands: List<ParametricEQBand>) {
        if (sampleRate == 0) return
        filters = bands
            .filter { it.enabled && it.frequency < sampleRate / 2.0 }
            .map { band ->
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || channelCount > 2 || channelCount < 1) {
            Log.w(TAG, "Equalizer inactive: format $inputAudioFormat is not 16-bit PCM mono/stereo")
            return AudioProcessor.AudioFormat.NOT_SET
        }

        pendingProfile?.let { profile ->
            preampGain = 10.0.pow(profile.preamp / 20.0)
            createFilters(profile.bands)
            equalizerEnabled = true
            pendingProfile = null
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!equalizerEnabled || filters.isEmpty()) {
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.nativeOrder())

        val sampleCount = remaining / 2

        when (channelCount) {
            1 -> {
                repeat(sampleCount) {
                    val sample = inputBuffer.getShort().toDouble() / 32768.0
                    var processed = sample
                    for (filter in filters) {
                        processed = filter.processSample(processed)
                    }
                    processed *= preampGain
                    val outputSample = (processed * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    outputBuffer.putShort(outputSample)
                }
            }
            2 -> {
                repeat(sampleCount / 2) {
                    val leftSample = inputBuffer.getShort().toDouble() / 32768.0
                    val rightSample = inputBuffer.getShort().toDouble() / 32768.0

                    var processedLeft = leftSample
                    var processedRight = rightSample

                    for (filter in filters) {
                        val (left, right) = filter.processStereo(processedLeft, processedRight)
                        processedLeft = left
                        processedRight = right
                    }

                    processedLeft *= preampGain
                    processedRight *= preampGain

                    val outputLeft = (processedLeft * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val outputRight = (processedRight * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                    outputBuffer.putShort(outputLeft)
                    outputBuffer.putShort(outputRight)
                }
            }
        }

        outputBuffer.flip()
    }

    override fun onFlush() {
        filters.forEach { it.reset() }
    }

    override fun onReset() {
        filters.forEach { it.reset() }
        sampleRate = 0
        channelCount = 0
    }
}
