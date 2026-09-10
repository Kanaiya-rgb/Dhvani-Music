package com.music.dhvani.playback.eq

data class EQPreset(
    val name: String,
    val gains: List<Double>, // 10 band gains in dB (-12 dB to +12 dB)
    val preamp: Double = 0.0
)

object EqualizerPresets {
    val FREQUENCIES = listOf(
        31.0, 62.0, 125.0, 250.0, 500.0,
        1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )

    val FREQUENCY_LABELS = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )

    val PRESETS = listOf(
        EQPreset("Flat", listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
        EQPreset("Bass Boost", listOf(6.0, 5.5, 4.0, 2.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0), -3.0),
        EQPreset("Treble Boost", listOf(0.0, 0.0, 0.0, 0.0, 0.5, 1.5, 3.0, 4.5, 6.0, 6.5), -2.0),
        EQPreset("Vocal Boost", listOf(-2.0, -1.0, 0.0, 1.5, 3.5, 4.0, 3.0, 1.5, 0.0, -1.0), -2.0),
        EQPreset("Electronic", listOf(5.0, 4.5, 2.0, 0.0, -1.5, 1.5, 0.0, 2.5, 4.5, 5.0), -2.5),
        EQPreset("Rock", listOf(4.5, 3.5, -1.0, -1.5, -0.5, 2.0, 3.5, 4.0, 4.5, 4.5), -2.5),
        EQPreset("Pop", listOf(1.5, 2.0, 3.0, 1.5, -0.5, -1.0, 1.0, 2.5, 3.5, 3.0), -1.5),
        EQPreset("Jazz", listOf(3.5, 2.5, 1.0, 1.5, -1.5, -1.5, 0.0, 1.5, 3.0, 3.5), -1.5),
        EQPreset("Acoustic", listOf(3.5, 3.0, 2.0, 1.0, 1.5, 1.5, 2.5, 3.0, 3.5, 2.5), -1.5),
        EQPreset("Hip Hop", listOf(6.5, 5.5, 3.0, 0.5, -1.0, -1.0, 1.0, -0.5, 2.5, 3.5), -3.0)
    )

    fun toParametricEQ(preset: EQPreset): ParametricEQ {
        val bands = FREQUENCIES.zip(preset.gains).map { (freq, gain) ->
            ParametricEQBand(
                frequency = freq,
                gain = gain,
                q = 1.41,
                filterType = FilterType.PK,
                enabled = true
            )
        }
        return ParametricEQ(
            preamp = preset.preamp,
            bands = bands,
            metadata = mapOf("name" to preset.name)
        )
    }

    fun customToParametricEQ(gains: List<Double>, preamp: Double = 0.0): ParametricEQ {
        val bands = FREQUENCIES.zip(gains).map { (freq, gain) ->
            ParametricEQBand(
                frequency = freq,
                gain = gain,
                q = 1.41,
                filterType = FilterType.PK,
                enabled = true
            )
        }
        return ParametricEQ(
            preamp = preamp,
            bands = bands,
            metadata = mapOf("name" to "Custom")
        )
    }
}
