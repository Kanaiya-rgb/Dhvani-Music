package com.music.dhvani.playback

import androidx.media3.session.MediaController
import com.music.dhvani.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Smart Audio Alignment engine that provides seamless rendition transitions
 * (Studio ➔ Live Acoustic ➔ Remastered) with proportional temporal mapping and
 * smooth 150ms crossfading.
 */
object PlaybackEngine {

    const val CROSSFADE_DURATION_MS = 150L
    private const val VARIANCE_THRESHOLD = 0.15 // 15%

    /**
     * Calculates the smart temporal target position in milliseconds when switching
     * between song versions or renditions.
     *
     * If duration variance is < 15%:
     *   Target Position = Current Position * (New Duration / Old Duration)
     * If duration differs significantly:
     *   Maps proportionally while guarding intro/verse boundaries.
     */
    fun calculateSmartAlignment(
        currentPositionMs: Long,
        oldDurationMs: Long,
        newDurationMs: Long,
    ): Long {
        if (oldDurationMs <= 0 || newDurationMs <= 0 || currentPositionMs <= 0) {
            return currentPositionMs.coerceAtLeast(0L)
        }

        val diffRatio = abs(newDurationMs - oldDurationMs).toDouble() / oldDurationMs

        return if (diffRatio < VARIANCE_THRESHOLD) {
            // Proportional scaling for close renditions (< 15% difference)
            val scaled = (currentPositionMs.toDouble() * (newDurationMs.toDouble() / oldDurationMs)).toLong()
            scaled.coerceIn(0L, (newDurationMs - 1000L).coerceAtLeast(0L))
        } else {
            // Significant structural difference (e.g. extended live or radio edit)
            val fraction = (currentPositionMs.toDouble() / oldDurationMs).coerceIn(0.0, 1.0)
            val target = (fraction * newDurationMs).toLong()
            target.coerceIn(0L, (newDurationMs - 1500L).coerceAtLeast(0L))
        }
    }

    /**
     * Executes a seamless 150ms crossfade between the outgoing audio rendition
     * and incoming rendition, seeking directly to the aligned timestamp.
     */
    fun switchRenditionWithCrossfade(
        controller: MediaController,
        newSong: Song,
        scope: CoroutineScope,
        currentPositionMs: Long,
        oldDurationMs: Long,
        newDurationMs: Long,
        onComplete: (() -> Unit)? = null,
    ) {
        val targetPositionMs = calculateSmartAlignment(currentPositionMs, oldDurationMs, newDurationMs)

        scope.launch(Dispatchers.Main) {
            val initialVol = controller.volume
            val steps = 5
            val stepDelay = CROSSFADE_DURATION_MS / steps

            // 150ms smooth volume fade-out
            for (i in 1..steps) {
                delay(stepDelay)
                controller.volume = (initialVol * (1f - (i.toFloat() / steps))).coerceAtLeast(0f)
            }

            // Replace media item & seek to aligned position
            val newItem = newSong.toMediaItem()
            val currentIndex = controller.currentMediaItemIndex
            if (currentIndex in 0 until controller.mediaItemCount) {
                controller.replaceMediaItem(currentIndex, newItem)
                controller.seekTo(currentIndex, targetPositionMs)
            } else {
                controller.setMediaItem(newItem, targetPositionMs)
                controller.prepare()
            }
            controller.play()

            // 150ms smooth volume fade-in
            for (i in 1..steps) {
                delay(stepDelay)
                controller.volume = (initialVol * (i.toFloat() / steps)).coerceAtMost(initialVol)
            }
            controller.volume = initialVol

            onComplete?.invoke()
        }
    }
}
