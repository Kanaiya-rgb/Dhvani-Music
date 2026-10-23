package com.music.dhvani.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.artworkAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class AmbientGlowPalette(
    val dominant: Color,
    val vibrant: Color,
    val secondary: Color,
)

/**
 * Extracts dominant & vibrant ambient colors from artwork off-thread.
 * Returns null during loading or error states to gracefully fallback.
 */
@Composable
fun rememberAmbientGlowPalette(
    artworkUrl: String?,
    fallbackColor: Color? = null,
): AmbientGlowPalette? {
    val context = LocalContext.current
    var palette by remember(artworkUrl, fallbackColor) {
        mutableStateOf<AmbientGlowPalette?>(
            fallbackColor?.let { AmbientGlowPalette(it, it, it) }
        )
    }

    LaunchedEffect(artworkUrl, fallbackColor) {
        if (artworkUrl.isNullOrBlank()) {
            palette = fallbackColor?.let { AmbientGlowPalette(it, it, it) }
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = SingletonImageLoader.get(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl.artworkAt(ROW_ART_PX))
                    .size(96)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.image?.toBitmap()
                if (bitmap != null) {
                    val p = Palette.from(bitmap).maximumColorCount(16).generate()
                    val swatches = p.swatches.sortedByDescending { it.population }
                    val dominant = p.dominantSwatch?.rgb?.let { Color(it) }
                        ?: swatches.firstOrNull()?.rgb?.let { Color(it) }
                        ?: fallbackColor

                    val vibrant = p.vibrantSwatch?.rgb?.let { Color(it) }
                        ?: p.lightVibrantSwatch?.rgb?.let { Color(it) }
                        ?: swatches.getOrNull(1)?.rgb?.let { Color(it) }
                        ?: dominant

                    val secondary = p.mutedSwatch?.rgb?.let { Color(it) }
                        ?: swatches.getOrNull(2)?.rgb?.let { Color(it) }
                        ?: vibrant

                    if (dominant != null) {
                        palette = AmbientGlowPalette(
                            dominant = dominant,
                            vibrant = vibrant ?: dominant,
                            secondary = secondary ?: vibrant ?: dominant,
                        )
                    }
                }
            } catch (_: Throwable) {
                palette = fallbackColor?.let { AmbientGlowPalette(it, it, it) }
            }
        }
    }
    return palette
}

/**
 * Renders a lightweight, smooth ambient atmospheric background on a Canvas.
 * Gives Explore, Library, and Search screens dynamic colors to allow LiquidGlass
 * navigation bar and frosted headers to refract authentic, living colors.
 */
@Composable
fun ScreenAmbientBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    fallbackColor: Color? = null,
    intensity: Float = 1.0f,
) {
    val themeBg = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    val colors = rememberAmbientGlowPalette(artworkUrl, fallbackColor)

    val transitionProgress by animateFloatAsState(
        targetValue = if (colors != null) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ambientGlowFade",
    )

    val animatedDominant by animateColorAsState(
        targetValue = colors?.dominant ?: themeBg,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ambientGlowDom",
    )
    val animatedVibrant by animateColorAsState(
        targetValue = colors?.vibrant ?: themeBg,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ambientGlowVib",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colors?.secondary ?: themeBg,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ambientGlowSec",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBg),
    ) {
        if (transitionProgress > 0.005f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // 1. Top-left atmospheric bloom
                val center1 = Offset(width * 0.15f, height * 0.12f)
                val radius1 = width * 1.05f
                val alpha1 = (if (isDark) 0.32f else 0.18f) * transitionProgress * intensity
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedDominant.copy(alpha = alpha1),
                        0.45f to animatedDominant.copy(alpha = alpha1 * 0.45f),
                        0.75f to animatedDominant.copy(alpha = alpha1 * 0.12f),
                        1.0f to Color.Transparent,
                        center = center1,
                        radius = radius1,
                    ),
                    radius = radius1,
                    center = center1,
                )

                // 2. Bottom-center bloom directly under the floating navigation bar
                // This ensures the Liquid Glass pill ALWAYS has rich refraction to sample!
                val center2 = Offset(width * 0.50f, height * 0.92f)
                val radius2 = width * 0.90f
                val alpha2 = (if (isDark) 0.28f else 0.16f) * transitionProgress * intensity
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedVibrant.copy(alpha = alpha2),
                        0.40f to animatedVibrant.copy(alpha = alpha2 * 0.50f),
                        0.75f to animatedVibrant.copy(alpha = alpha2 * 0.14f),
                        1.0f to Color.Transparent,
                        center = center2,
                        radius = radius2,
                    ),
                    radius = radius2,
                    center = center2,
                )

                // 3. Mid-right secondary ambient tint
                val center3 = Offset(width * 0.85f, height * 0.45f)
                val radius3 = width * 0.75f
                val alpha3 = (if (isDark) 0.20f else 0.10f) * transitionProgress * intensity
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedSecondary.copy(alpha = alpha3),
                        0.50f to animatedSecondary.copy(alpha = alpha3 * 0.35f),
                        1.0f to Color.Transparent,
                        center = center3,
                        radius = radius3,
                    ),
                    radius = radius3,
                    center = center3,
                )

                // 4. Contrast protection scrim for perfect foreground legibility
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to themeBg.copy(alpha = if (isDark) 0.18f else 0.08f),
                        0.30f to themeBg.copy(alpha = if (isDark) 0.40f else 0.20f),
                        0.65f to themeBg.copy(alpha = if (isDark) 0.70f else 0.45f),
                        0.88f to themeBg.copy(alpha = if (isDark) 0.45f else 0.25f), // softer at the bottom so glass refracts
                        1.0f to themeBg.copy(alpha = if (isDark) 0.25f else 0.15f),
                    ),
                )
            }
        }
    }
}
