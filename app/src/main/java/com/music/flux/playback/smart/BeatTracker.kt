/*
 * Ported from Orchard (https://github.com/SFG5453/Orchard).
 *
 * Copyright (C) 2026 SFG545 (original Orchard implementation)
 * Copyright (C) 2026 Kushagra Singh (BitChord adaptation)
 */

package com.music.flux.playback.smart

import android.content.Context
import kotlin.math.abs

/**
 * Lightweight BeatTracker stub when on-device neural model is omitted for reduced APK size.
 * Gracefully returns null so transitions fall back to standard crossfade.
 */
class BeatTracker(private val context: Context) {

    /** A tracked grid on the analysed audio's own timeline, in seconds. */
    data class Grid(
        val beats: List<Double>,
        val downbeats: List<Double>,
        val bpm: Double,
        val beatInterval: Double,
        val firstBeat: Double,
        val beatConfidence: Double,
    )

    fun track(pcm: FloatArray, offsetSeconds: Double = 0.0): Grid? = null

    fun release() {}

    companion object {
        const val WINDOW_SECONDS = 30.0
    }
}
