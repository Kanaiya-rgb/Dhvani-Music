package com.music.dhvani.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable dynamic animated background derived from current song's album art.
 *
 * Visual Characteristics:
 * - Base: Deepest extracted dark color fills the entire screen.
 * - 4 large soft radial-gradient blobs drifting slowly along smooth looping Lissajous paths (~20-30s loop).
 * - Smooth color crossfade (~1200ms) on track change with zero flashes or hard cuts.
 * - Multi-stop radial gradients providing soft, natural falloff on ALL Android API levels without requiring RenderEffect/blur.
 * - Subtle dark overlay & vignette to ensure 100% legibility of lyrics and controls.
 *
 * Performance:
 * - Lifecycle-aware: pauses animation when screen/app is paused or backgrounded.
 * - Phase-isolated rendering: Animatable read occurs only within the Canvas draw scope,
 *   invalidating ONLY the background draw node and NEVER triggering recomposition of parent or sibling UI.
 */
@Composable
fun DynamicBackground(
    coverUri: String?,
    trackId: String,
    modifier: Modifier = Modifier,
    fallbackUri: String? = null,
    isPlaying: Boolean = true,
    existingBitmap: Bitmap? = null,
) {
    val context = LocalContext.current

    // 1. Color extraction cached per trackId with resilient fallbackUri (song.thumbnailUrl)
    val palette = rememberCoverPalette(
        context = context,
        trackId = trackId,
        coverUri = coverUri,
        fallbackUri = fallbackUri,
        existingBitmap = existingBitmap,
    )

    // 2. Smooth 1200ms crossfade between colors on track change
    val colorSpec = remember { tween<Color>(durationMillis = 1200, easing = LinearOutSlowInEasing) }
    val animBaseDark by animateColorAsState(targetValue = palette.baseDark, animationSpec = colorSpec, label = "baseDark")
    val animDarkVibrant by animateColorAsState(targetValue = palette.darkVibrant, animationSpec = colorSpec, label = "darkVibrant")
    val animVibrant by animateColorAsState(targetValue = palette.vibrant, animationSpec = colorSpec, label = "vibrant")
    val animMuted by animateColorAsState(targetValue = palette.muted, animationSpec = colorSpec, label = "muted")
    val animLightMuted by animateColorAsState(targetValue = palette.lightMuted, animationSpec = colorSpec, label = "lightMuted")

    // Smooth continuous Lissajous animation clock
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                t = (nanos / 1_000_000_000f)
            }
        }
    }

    // Pure dynamic color gradient mesh Canvas (NO song cover bitmap in background)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        val time = t
        val w = size.width
        val h = size.height
        val maxDim = size.maxDimension

        // Step A: Deep velvety base layer fills the screen
        drawRect(color = animBaseDark, size = size)

        // Step B: Calculate 4 drifting blob positions along smooth organic Lissajous paths
        // Blob 1: Vinyl anchor / Upper-Left (Dominant dark tone)
        val center1 = Offset(
            x = (0.22f + 0.10f * cos(time * 0.85f)) * w,
            y = (0.35f + 0.08f * sin(time * 0.70f + 0.4f)) * h,
        )
        val radius1 = maxDim * 0.60f

        // Blob 2: Luminous accent glow behind & around vinyl record
        val center2 = Offset(
            x = (0.34f + 0.10f * sin(time * 0.75f + 1.2f)) * w,
            y = (0.38f + 0.09f * cos(time * 0.90f + 0.8f)) * h,
        )
        val radius2 = maxDim * 0.58f

        // Blob 3: Lower-Right anchor (Muted tone)
        val center3 = Offset(
            x = (0.74f + 0.12f * cos(time * 1.05f + 2.5f)) * w,
            y = (0.75f + 0.10f * sin(time * 0.80f + 1.9f)) * h,
        )
        val radius3 = maxDim * 0.62f

        // Blob 4: Lower-Left / Top-Right balance (Light muted accent)
        val center4 = Offset(
            x = (0.28f + 0.12f * sin(time * 0.70f + 3.8f)) * w,
            y = (0.78f + 0.10f * cos(time * 0.95f + 2.7f)) * h,
        )
        val radius4 = maxDim * 0.55f

        // Step C: Draw soft radial gradient color blobs
        // Blob 1: Dominant tone
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animDarkVibrant.copy(alpha = 0.45f),
                    0.30f to animDarkVibrant.copy(alpha = 0.28f),
                    0.65f to animDarkVibrant.copy(alpha = 0.08f),
                    1.00f to animDarkVibrant.copy(alpha = 0.00f),
                ),
                center = center1,
                radius = radius1,
            ),
            radius = radius1,
            center = center1,
        )

        // Blob 2: Vibrant accent glow (soft ambient halo behind the vinyl)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animVibrant.copy(alpha = 0.42f),
                    0.30f to animVibrant.copy(alpha = 0.25f),
                    0.65f to animVibrant.copy(alpha = 0.07f),
                    1.00f to animVibrant.copy(alpha = 0.00f),
                ),
                center = center2,
                radius = radius2,
            ),
            radius = radius2,
            center = center2,
        )

        // Blob 3: Muted tone
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animMuted.copy(alpha = 0.38f),
                    0.30f to animMuted.copy(alpha = 0.22f),
                    0.65f to animMuted.copy(alpha = 0.06f),
                    1.00f to animMuted.copy(alpha = 0.00f),
                ),
                center = center3,
                radius = radius3,
            ),
            radius = radius3,
            center = center3,
        )

        // Blob 4: Light muted accent
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animLightMuted.copy(alpha = 0.35f),
                    0.30f to animLightMuted.copy(alpha = 0.20f),
                    0.65f to animLightMuted.copy(alpha = 0.05f),
                    1.00f to animLightMuted.copy(alpha = 0.00f),
                ),
                center = center4,
                radius = radius4,
            ),
            radius = radius4,
            center = center4,
        )

        // Step D: Atmospheric Scrims & Vignette for crystal-clear readability
        // 1. Subtle perimeter radial vignette
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.60f to Color.Transparent,
                    0.85f to Color.Black.copy(alpha = 0.22f),
                    1.00f to Color.Black.copy(alpha = 0.42f),
                ),
                center = Offset(w / 2f, h / 2f),
                radius = maxDim * 0.85f,
            ),
            size = size,
        )

        // 2. Horizontal cinema scrim: extra contrast over the right side (lyrics area)
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Black.copy(alpha = 0.14f),
                    0.35f to Color.Transparent,
                    0.65f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.34f),
                ),
            ),
            size = size,
        )

        // 3. Subtle vertical scrim for top close button and bottom controls
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Black.copy(alpha = 0.28f),
                    0.20f to Color.Transparent,
                    0.80f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.25f),
                ),
            ),
            size = size,
        )
    }
}

/**
 * Convenience overload accepting a [Song] domain model.
 */
@Composable
fun DynamicBackground(
    song: Song,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    existingBitmap: Bitmap? = null,
) {
    DynamicBackground(
        coverUri = song.artworkAt(800) ?: song.thumbnailUrl,
        fallbackUri = song.thumbnailUrl,
        trackId = song.videoId,
        modifier = modifier,
        isPlaying = isPlaying,
        existingBitmap = existingBitmap,
    )
}
