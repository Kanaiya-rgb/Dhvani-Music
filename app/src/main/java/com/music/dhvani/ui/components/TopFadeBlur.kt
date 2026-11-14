package com.music.dhvani.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.settings.AppSettings
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * The run the fade needs below the bar to get from full blur to none without
 * the eye finding where it got there.
 */
private val FADE_RUN = 100.dp

/**
 * How much blur the fade reaches at its outer edge — restrained to prevent
 * excessive opacity bands over the ambient artwork.
 */
private const val PEAK = 0.70f

/**
 * The glass behind every top bar: full blur along the top edge, ramping to
 * nothing on the way down.
 *
 * At rest, the header remains transparent and continuous with the ambient
 * background. When content scrolls beneath the bar, it smoothly ramps up
 * backdrop blur and a protective frosted wash so content is cleanly obscured.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun TopFadeBlur(
    hazeState: HazeState,
    /**
     * The colour of the page behind this: the theme's background on a feed, a
     * detail page's artwork wash on one of those.
     */
    pageColor: Color,
    modifier: Modifier = Modifier,
    /**
     * A wash laid over the blur to keep the bar's glyphs readable.
     */
    scrimColor: Color,
    /**
     * Whether the content beneath has scrolled under the header.
     * At rest (false), the header stays transparent and seamlessly blended with
     * the ambient background, while softly ramping up blur/scrim when scrolled.
     */
    scrolled: Boolean = true,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val frostedHeaderBlur by AppSettings.frostedHeaderBlur.collectAsStateWithLifecycle()

    val canBlurHeader = frostedHeaderBlur && !reduceDynamicBlur

    val scrollProgress by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "topFadeBlurScroll",
    )

    val topBarH = topBarHeight()
    val totalHeight = if (canBlurHeader) topBarH + (FADE_RUN * scrollProgress) else topBarH

    // 1. Hardware blur layer (when supported and frosted header blur is enabled)
    if (canBlurHeader && scrollProgress > 0.005f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(totalHeight)
                .optimizedHazeEffect(
                    state = hazeState,
                    // Translucent wash keyed to pageColor to prevent solid dark blocks
                    style = HazeMaterials.ultraThin(pageColor.copy(alpha = (0.20f * scrollProgress).coerceIn(0f, 1f))),
                ) {
                    progressive = HazeProgressive.verticalGradient(
                        easing = EaseOutCubic,
                        startIntensity = PEAK * scrollProgress,
                        endIntensity = 0f,
                    )
                    noiseFactor = 0f
                },
        )
    }

    // 2. Guaranteed Frosted Protective Surface & Readability Scrim:
    if (canBlurHeader && scrollProgress > 0.005f && scrimColor != Color.Transparent) {
        val backgroundBrush = remember(scrimColor, scrollProgress, topBarH, totalHeight) {
            val barFraction = if (totalHeight > 0.dp) (topBarH / totalHeight).coerceIn(0.40f, 0.70f) else 0.55f
            val peakAlpha = (0.50f * scrollProgress).coerceIn(0f, 1f)
            val midAlpha = (0.28f * scrollProgress).coerceIn(0f, 1f)
            val tailAlpha = (0.08f * scrollProgress).coerceIn(0f, 1f)
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to scrimColor.copy(alpha = peakAlpha),
                    barFraction * 0.50f to scrimColor.copy(alpha = peakAlpha * 0.85f),
                    barFraction to scrimColor.copy(alpha = midAlpha),
                    barFraction + (1f - barFraction) * 0.40f to scrimColor.copy(alpha = tailAlpha),
                    barFraction + (1f - barFraction) * 0.75f to scrimColor.copy(alpha = tailAlpha * 0.35f),
                    1.0f to Color.Transparent,
                )
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(totalHeight)
                .background(backgroundBrush),
        )
    } else if (!canBlurHeader && scrollProgress > 0.005f && scrimColor != Color.Transparent) {
        // When frosted header blur is disabled, provide clean soft scrim fading gently over totalHeight
        val backgroundBrush = remember(scrimColor, scrollProgress, topBarH, totalHeight) {
            val barFraction = if (totalHeight > 0.dp) (topBarH / totalHeight).coerceIn(0.40f, 0.70f) else 0.55f
            val peakAlpha = (0.85f * scrollProgress).coerceIn(0f, 1f)
            val midAlpha = (0.55f * scrollProgress).coerceIn(0f, 1f)
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to scrimColor.copy(alpha = peakAlpha),
                    barFraction to scrimColor.copy(alpha = midAlpha),
                    barFraction + (1f - barFraction) * 0.50f to scrimColor.copy(alpha = midAlpha * 0.35f),
                    1.0f to Color.Transparent,
                )
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(totalHeight)
                .background(backgroundBrush),
        )
    }
}
