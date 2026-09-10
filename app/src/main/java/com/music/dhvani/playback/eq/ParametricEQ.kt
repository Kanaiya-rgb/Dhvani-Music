package com.music.dhvani.playback.eq

import kotlinx.serialization.Serializable

@Serializable
data class ParametricEQBand(
    val frequency: Double,                      // Center frequency in Hz
    val gain: Double,                           // Gain in dB
    val q: Double = 1.41,                       // Q factor (bandwidth) - default to sqrt(2)
    val filterType: FilterType = FilterType.PK, // Filter type
    val enabled: Boolean = true                 // Whether this band is active
)

@Serializable
data class ParametricEQ(
    val preamp: Double = 0.0,                   // Preamp/gain in dB (to prevent clipping)
    val bands: List<ParametricEQBand> = emptyList(), // List of EQ bands
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val MAX_BANDS = 20
    }
}
