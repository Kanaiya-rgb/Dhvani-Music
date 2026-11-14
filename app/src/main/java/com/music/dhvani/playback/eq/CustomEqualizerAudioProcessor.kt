package com.music.dhvani.playback.eq

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import com.music.dhvani.playback.EqCurve
import com.music.dhvani.playback.EqLayout
import com.music.dhvani.playback.FilterKind

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
    private var pendingCurve: Pair<EqCurve, Float>? = null

    @Volatile
    private var balanceGainL = 1.0
    @Volatile
    private var balanceGainR = 1.0

    companion object {
        private const val TAG = "CustomEqualizerAudioProc"
    }

    @Synchronized
    fun applyCurve(curve: EqCurve, balance: Float = 0f) {
        val b = balance.coerceIn(-1f, 1f)
        balanceGainL = if (b > 0f) (1f - b).toDouble() else 1.0
        balanceGainR = if (b < 0f) (1f + b).toDouble() else 1.0

        if (sampleRate == 0) {
            pendingCurve = curve to balance
            return
        }

        preampGain = 10.0.pow(curve.preampDb / 20.0)
        filters = EqLayout.slots.mapIndexedNotNull { index, slot ->
            val gainDb = curve.gainsDb.getOrElse(index) { 0f }.toDouble()
            if (kotlin.math.abs(gainDb) < 0.05) return@mapIndexedNotNull null
            val q = curve.qs.getOrElse(index) { 0.707f }.toDouble()
            val fType = when (slot.kind) {
                FilterKind.LOW_SHELF -> FilterType.LSC
                FilterKind.HIGH_SHELF -> FilterType.HSC
                FilterKind.BELL -> FilterType.PK
            }
            if (slot.frequencyHz >= sampleRate / 2.0) return@mapIndexedNotNull null
            BiquadFilter(
                sampleRate = sampleRate,
                frequency = slot.frequencyHz.toDouble(),
                gain = gainDb,
                q = q,
                filterType = fType,
            )
        }
        equalizerEnabled = true
        filters.forEach { it.reset() }
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
        pendingCurve = null
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

        pendingCurve?.let { (curve, balance) ->
            applyCurve(curve, balance)
            pendingCurve = null
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

        val hasBalance = balanceGainL != 1.0 || balanceGainR != 1.0
        if (!equalizerEnabled && !hasBalance) {
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
                    if (equalizerEnabled) {
                        for (filter in filters) {
                            processed = filter.processSample(processed)
                        }
                        processed *= preampGain
                    }
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

                    if (equalizerEnabled) {
                        for (filter in filters) {
                            val (left, right) = filter.processStereo(processedLeft, processedRight)
                            processedLeft = left
                            processedRight = right
                        }
                        processedLeft *= preampGain
                        processedRight *= preampGain
                    }

                    processedLeft *= balanceGainL
                    processedRight *= balanceGainR

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
