package com.music.dhvani.ui.components

import androidx.compose.ui.Modifier
import com.music.dhvani.data.settings.AppSettings
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

/**
 * Keeps Haze's visual style while allowing it to reduce its sampling resolution.
 * Haze 1.x otherwise processes every effect at full resolution, even when a
 * large blur makes those extra source pixels invisible.
 *
 * Automatically bypasses the expensive GPU blur pipeline when blur is disabled,
 * performance mode is active, or running on low-end hardware to eliminate heating.
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.optimizedHazeEffect(
    state: HazeState,
    style: HazeStyle = HazeStyle.Unspecified,
    block: (HazeEffectScope.() -> Unit)? = null,
): Modifier {
    if (AppSettings.reduceDynamicBlur.value || AppSettings.performanceMode.value || AppSettings.isLowEndDevice()) {
        return this
    }
    return hazeEffect(state, style) {
        inputScale = HazeInputScale.Fixed(0.33f)
        block?.invoke(this)
    }
}
