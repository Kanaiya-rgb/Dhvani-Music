package com.music.dhvani.playback.eq

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
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

    private val processors = mutableListOf<CustomEqualizerAudioProcessor>()

    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Flat")
    val selectedPreset = _selectedPreset.asStateFlow()

    private val _bandGains = MutableStateFlow(List(10) { 0.0 })
    val bandGains = _bandGains.asStateFlow()

    private val _preampGain = MutableStateFlow(0.0)
    val preampGain = _preampGain.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _selectedPreset.value = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"
        _preampGain.value = prefs.getFloat(KEY_PREAMP, 0.0f).toDouble()

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

    fun openSystemEqualizer(context: Context, audioSessionId: Int = 0): Boolean {
        return try {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                if (audioSessionId != 0) {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                }
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch system equalizer", e)
            false
        }
    }

    private fun applyCurrentState() {
        synchronized(processors) {
            processors.forEach { applyToProcessor(it) }
        }
    }

    private fun applyToProcessor(processor: CustomEqualizerAudioProcessor) {
        if (!_enabled.value) {
            processor.disable()
            return
        }
        val parametricEQ = EqualizerPresets.customToParametricEQ(
            gains = _bandGains.value,
            preamp = _preampGain.value
        )
        processor.applyProfile(parametricEQ)
    }

    private fun save() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, _enabled.value)
            .putString(KEY_PRESET, _selectedPreset.value)
            .putString(KEY_GAINS, _bandGains.value.joinToString(","))
            .putFloat(KEY_PREAMP, _preampGain.value.toFloat())
            .apply()
    }
}
