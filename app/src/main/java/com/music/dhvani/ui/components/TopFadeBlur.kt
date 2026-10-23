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
private val FADE_RUN = 64.dp

/**
 * How much blur the fade reaches at its outer edge — restrained to prevent
 * excessive opacity bands over the ambient artwork.
 */
private const val PEAK = 0.75f

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

    val scrollProgress by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "topFadeBlurScroll",
    )

    val topBarH = topBarHeight()
    val totalHeight = topBarH + (FADE_RUN * scrollProgress)

    // 1. Hardware blur layer (when supported and blur is not disabled)
    if (!reduceDynamicBlur && scrollProgress > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(totalHeight)
                .optimizedHazeEffect(
                    state = hazeState,
                    // Translucent wash keyed to pageColor to prevent solid dark blocks
                    style = HazeMaterials.ultraThin(pageColor.copy(alpha = (0.28f * scrollProgress).coerceIn(0f, 1f))),
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
    // Even on devices where GPU blur is bypassed (e.g. low-end devices or battery saver),
    // this ensures content scrolling beneath the bar is smoothly obscured and NEVER
    // produces double-exposure illegibility over the header glyphs/text.
    //
    // At rest (scrollProgress == 0), only a very gentle status bar vignette (0.12f alpha)
    // is applied at the top edge, leaving the rest 100% transparent into the ambient background.
    val backgroundBrush = remember(scrimColor, scrollProgress, topBarH, totalHeight) {
        val barFraction = if (totalHeight > 0.dp) (topBarH / totalHeight).coerceIn(0.5f, 0.85f) else 0.72f
        val peakAlpha = (0.86f * scrollProgress).coerceIn(0f, 1f)
        val midAlpha = (0.72f * scrollProgress).coerceIn(0f, 1f)
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to scrimColor.copy(alpha = if (scrollProgress > 0.01f) peakAlpha else 0.12f),
                barFraction * 0.75f to scrimColor.copy(alpha = if (scrollProgress > 0.01f) peakAlpha else 0.05f),
                barFraction to scrimColor.copy(alpha = if (scrollProgress > 0.01f) midAlpha else 0.0f),
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
