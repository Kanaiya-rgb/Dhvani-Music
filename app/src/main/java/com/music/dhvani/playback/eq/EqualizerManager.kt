package com.music.dhvani.playback.eq

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import com.music.dhvani.data.settings.EqualizerMode
import com.music.dhvani.playback.toneCurve
import com.music.dhvani.playback.manualCurve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global Equalizer controller connecting UI and Playback Audio Processors.
 */
object EqualizerManager {
    private const val TAG = "EqualizerManager"
    private const val PREFS_NAME = "music_equalizer_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_GAINS = "eq_gains"
    private const val KEY_PREAMP = "eq_preamp"
    private const val KEY_LOUDNESS_ENABLED = "eq_loudness_enabled"
    private const val KEY_LOUDNESS_BOOST_MB = "eq_loudness_boost_mb"

    private val processors = mutableListOf<CustomEqualizerAudioProcessor>()

    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Flat")
    val selectedPreset = _selectedPreset.asStateFlow()

    private val _bandGains = MutableStateFlow(List(10) { 0.0 })
    val bandGains = _bandGains.asStateFlow()

    private val _preampGain = MutableStateFlow(0.0)
    val preampGain = _preampGain.asStateFlow()

    // Loudness Booster (>100% Volume Boost via Android LoudnessEnhancer)
    // 0 to 400 mB (0 dB to +4 dB clean boost without hardware clipping)
    private val _loudnessEnabled = MutableStateFlow(false)
    val loudnessEnabled = _loudnessEnabled.asStateFlow()

    private val _loudnessBoostMb = MutableStateFlow(0) // Default 0 mB (clean 0 dB output)
    val loudnessBoostMb = _loudnessBoostMb.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _selectedPreset.value = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"
        _preampGain.value = prefs.getFloat(KEY_PREAMP, 0.0f).toDouble()
        // Overdrive booster is strictly transient in-session and never automatically re-engaged on app start
        _loudnessEnabled.value = false
        _loudnessBoostMb.value = 0

        val gainsStr = prefs.getString(KEY_GAINS, null)
        if (!gainsStr.isNullOrEmpty()) {
            val parsed = gainsStr.split(",").mapNotNull { it.toDoubleOrNull() }
            if (parsed.size == 10) {
                _bandGains.value = parsed
            }
        }
        applyCurrentState()
    }

    fun registerProcessor(processor: CustomEqualizerAudioProcessor) {
        synchronized(processors) {
            processors.add(processor)
            applyToProcessor(processor)
        }
    }

    fun unregisterProcessor(processor: CustomEqualizerAudioProcessor) {
        synchronized(processors) {
            processors.remove(processor)
        }
    }

    fun setEnabled(enable: Boolean) {
        _enabled.value = enable
        save()
        applyCurrentState()
    }

    fun selectPreset(preset: EQPreset) {
        _selectedPreset.value = preset.name
        _bandGains.value = preset.gains
        _preampGain.value = preset.preamp
        save()
        applyCurrentState()
    }

    fun updateBandGain(index: Int, gainDb: Double) {
        if (index !in 0..9) return
        val updated = _bandGains.value.toMutableList()
        updated[index] = gainDb.coerceIn(-12.0, 12.0)
        _bandGains.value = updated
        _selectedPreset.value = "Custom"
        save()
        applyCurrentState()
    }

    fun updatePreamp(gainDb: Double) {
        _preampGain.value = gainDb.coerceIn(-12.0, 12.0)
        _selectedPreset.value = "Custom"
        save()
        applyCurrentState()
    }

    fun setLoudnessEnabled(enable: Boolean) {
        _loudnessEnabled.value = false
        _loudnessBoostMb.value = 0
    }

    fun setLoudnessBoostMb(gainMb: Int, persist: Boolean = false) {
        _loudnessEnabled.value = false
        _loudnessBoostMb.value = 0
    }

    fun openSystemEqualizer(context: Context, audioSessionId: Int = 0): Boolean {
        return com.music.dhvani.playback.SystemEqualizerUtils.openSystemEqualizer(context, audioSessionId)
    }

    fun reset() {
        _enabled.value = false
        _selectedPreset.value = "Flat"
        _bandGains.value = List(10) { 0.0 }
        _preampGain.value = 0.0
        _loudnessEnabled.value = false
        _loudnessBoostMb.value = 0
        applyCurrentState()
        save()
    }

    fun applyCurrentState() {
        synchronized(processors) {
            processors.forEach { applyToProcessor(it) }
        }
    }

    private fun applyToProcessor(processor: CustomEqualizerAudioProcessor) {
        val enabled = com.music.dhvani.data.settings.AppSettings.equalizerEnabled.value
        val balance = com.music.dhvani.data.settings.AppSettings.equalizerBalance.value
        if (!enabled) {
            processor.disable()
            return
        }

        val mode = com.music.dhvani.data.settings.AppSettings.equalizerMode.value
        val curve = when (mode) {
            EqualizerMode.DYNAMIC -> toneCurve(
                x = com.music.dhvani.data.settings.AppSettings.equalizerToneX.value,
                y = com.music.dhvani.data.settings.AppSettings.equalizerToneY.value,
                focused = com.music.dhvani.data.settings.AppSettings.equalizerFocused.value,
            )
            EqualizerMode.MANUAL -> manualCurve(
                bandsDb = com.music.dhvani.data.settings.AppSettings.equalizerBands.value
            )
        }
        processor.applyCurve(curve, balance)
    }

    private fun save() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, _enabled.value)
            .putString(KEY_PRESET, _selectedPreset.value)
            .putString(KEY_GAINS, _bandGains.value.joinToString(","))
            .putFloat(KEY_PREAMP, _preampGain.value.toFloat())
            .putBoolean(KEY_LOUDNESS_ENABLED, false)
            .putInt(KEY_LOUDNESS_BOOST_MB, 0)
            .apply()
    }
}
