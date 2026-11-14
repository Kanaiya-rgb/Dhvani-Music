package com.music.dhvani.ui.player

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min

/**
 * 4-color palette extracted from song album art plus a deep rich base color for backgrounds.
 */
@Immutable
data class ExtractedPalette(
    val baseDark: Color,
    val darkVibrant: Color,
    val vibrant: Color,
    val muted: Color,
    val lightMuted: Color,
) {
    companion object {
        val Default = ExtractedPalette(
            baseDark = Color(0xFF0A0C12),
            darkVibrant = Color(0xFF141824),
            vibrant = Color(0xFF222838),
            muted = Color(0xFF181C26),
            lightMuted = Color(0xFF2B3346),
        )
    }
}

object PaletteExtractor {
    // Thread-safe in-memory cache keyed by track ID to avoid re-extracting for the same track
    private val cache = ConcurrentHashMap<String, ExtractedPalette>()

    fun getCached(trackId: String): ExtractedPalette? = cache[trackId]

    fun clearCache() {
        cache.clear()
    }

    suspend fun extract(
        context: Context,
        trackId: String,
        coverUri: String?,
        fallbackUri: String? = null,
        existingBitmap: Bitmap? = null,
    ): ExtractedPalette {
        cache[trackId]?.let {
            android.util.Log.d("DynamicBackground", "[$trackId] Palette from CACHE: darkVibrant=${it.darkVibrant}, vibrant=${it.vibrant}, baseDark=${it.baseDark}")
            return it
        }

        if (coverUri.isNullOrBlank() && fallbackUri.isNullOrBlank() && existingBitmap == null) {
            android.util.Log.w("DynamicBackground", "[$trackId] No cover URI or bitmap provided; using Default")
            return ExtractedPalette.Default
        }

        return withContext(Dispatchers.IO) {
            var loadedBitmap: Bitmap? = existingBitmap
            val candidates = listOfNotNull(
                coverUri?.takeIf { it.isNotBlank() },
                fallbackUri?.takeIf { it.isNotBlank() },
            ).distinct()

            android.util.Log.d("DynamicBackground", "[$trackId] Starting fresh extraction. Candidate URLs: $candidates")

            for (url in candidates) {
                if (loadedBitmap != null) break
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(128)
                        .allowHardware(false)
                        .build()
                    val result = SingletonImageLoader.get(context).execute(request)
                    if (result is SuccessResult) {
                        loadedBitmap = result.image.toBitmap()
                        android.util.Log.d("DynamicBackground", "[$trackId] Loaded bitmap successfully from: $url (${loadedBitmap.width}x${loadedBitmap.height})")
                    } else if (result is coil3.request.ErrorResult) {
                        android.util.Log.w("DynamicBackground", "[$trackId] Coil ErrorResult for $url: ${result.throwable.message}")
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DynamicBackground", "[$trackId] Coil exception for $url: ${e.message}")
                }
            }

            if (loadedBitmap == null) {
                android.util.Log.w("DynamicBackground", "[$trackId] All URLs failed to load bitmap. Using Default neutral fallback")
                ExtractedPalette.Default
            } else {
                val extracted = extractFromBitmap(loadedBitmap)
                cache[trackId] = extracted
                android.util.Log.d(
                    "DynamicBackground",
                    "[$trackId] FRESH extraction SUCCESS: baseDark=${extracted.baseDark}, darkVibrant=${extracted.darkVibrant}, vibrant=${extracted.vibrant}, muted=${extracted.muted}, lightMuted=${extracted.lightMuted}",
                )
                extracted
            }
        }
    }

    fun extractFromBitmap(bitmap: Bitmap): ExtractedPalette {
        // Clear filters so near-black and near-white colors in dark/monochromatic albums are preserved!
        val palette = try {
            Palette.from(bitmap)
                .clearFilters()
                .maximumColorCount(32)
                .generate()
        } catch (_: Throwable) {
            return ExtractedPalette.Default
        }

        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty()) {
            return ExtractedPalette.Default
        }

        val totalPopulation = allSwatches.sumOf { it.population }.coerceAtLeast(1)
        val hsl = FloatArray(3)

        // Calculate weighted average saturation of the entire artwork
        val weightedSaturation = allSwatches.sumOf { swatch ->
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            (swatch.population.toDouble() / totalPopulation) * hsl[1]
        }.toFloat()

        // 1. Pick distinct swatches based on visual population in the artwork
        val distinctSwatches = mutableListOf<Palette.Swatch>()
        for (swatch in allSwatches) {
            val isClose = distinctSwatches.any { existing ->
                areSwatchesTooClose(swatch, existing)
            }
            if (!isClose) {
                distinctSwatches.add(swatch)
                if (distinctSwatches.size >= 4) break
            }
        }

        val fourSwatches = if (distinctSwatches.size >= 4) {
            distinctSwatches.take(4)
        } else {
            distinctSwatches.expandedToFour()
        }

        // 2. Base Dark: derive from the darkest prominent swatch
        val darkestSwatch = allSwatches.take(8).minByOrNull { swatch ->
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            hsl[2]
        } ?: allSwatches.first()

        val baseDark = toDeepBaseColor(Color(darkestSwatch.rgb), weightedSaturation)

        // 3. Map the 4 distinct swatches to blob colors with natural saturation scaling
        val darkVibrant = Color(fourSwatches[0].rgb).tuneForBlob(
            weightedSat = weightedSaturation,
            targetLightness = if (weightedSaturation < 0.16f) 0.16f else 0.24f,
        )
        val vibrant = Color(fourSwatches[1].rgb).tuneForBlob(
            weightedSat = weightedSaturation,
            targetLightness = if (weightedSaturation < 0.16f) 0.32f else 0.38f,
        )
        val muted = Color(fourSwatches[2].rgb).tuneForBlob(
            weightedSat = weightedSaturation,
            targetLightness = if (weightedSaturation < 0.16f) 0.20f else 0.28f,
        )
        val lightMuted = Color(fourSwatches[3].rgb).tuneForBlob(
            weightedSat = weightedSaturation,
            targetLightness = if (weightedSaturation < 0.16f) 0.26f else 0.34f,
        )

        return ExtractedPalette(
            baseDark = baseDark,
            darkVibrant = darkVibrant,
            vibrant = vibrant,
            muted = muted,
            lightMuted = lightMuted,
        )
    }

    private fun areSwatchesTooClose(s1: Palette.Swatch, s2: Palette.Swatch): Boolean {
        val hsl1 = FloatArray(3)
        val hsl2 = FloatArray(3)
        ColorUtils.colorToHSL(s1.rgb, hsl1)
        ColorUtils.colorToHSL(s2.rgb, hsl2)
        val hueDiff = abs(hsl1[0] - hsl2[0]).let { min(it, 360f - it) }
        val lightnessDiff = abs(hsl1[2] - hsl2[2])
        val satDiff = abs(hsl1[1] - hsl2[1])

        // For low-saturation (neutral/grey/black) swatches, hue is jittery; distinguish primarily by lightness
        if (hsl1[1] < 0.15f && hsl2[1] < 0.15f) {
            return lightnessDiff < 0.12f
        }
        return hueDiff < 25f && lightnessDiff < 0.16f && satDiff < 0.20f
    }

    private fun List<Palette.Swatch>.expandedToFour(): List<Palette.Swatch> {
        if (isEmpty()) {
            return listOf(
                Palette.Swatch(0xFF141824.toInt(), 1),
                Palette.Swatch(0xFF222838.toInt(), 1),
                Palette.Swatch(0xFF181C26.toInt(), 1),
                Palette.Swatch(0xFF2B3346.toInt(), 1),
            )
        }
        val out = toMutableList()
        var step = 1
        while (out.size < 4) {
            val base = this[(out.size - size) % size]
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(base.rgb, hsl)
            hsl[0] = (hsl[0] + 18f * step) % 360f
            hsl[2] = (hsl[2] + 0.08f * (if (step % 2 == 1) 1 else -1)).coerceIn(0.12f, 0.65f)
            val shiftedRgb = ColorUtils.HSLToColor(hsl)
            out.add(Palette.Swatch(shiftedRgb, (base.population / (step + 1)).coerceAtLeast(1)))
            step++
        }
        return out
    }

    private fun Color.tuneForBlob(
        weightedSat: Float,
        targetLightness: Float,
    ): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(toArgb(), hsl)

        if (weightedSat < 0.16f) {
            // Predominantly monochrome / neutral artwork (e.g. Sia - This Is Acting):
            // Strictly cap saturation so tiny skin tone or lip highlights never hijack the ambient room into red!
            hsl[1] = hsl[1].coerceAtMost(0.12f)
        } else if (weightedSat < 0.32f) {
            // Subtle / muted artwork: gently scale saturation
            hsl[1] = (hsl[1] * 1.15f).coerceAtMost(0.40f)
        } else {
            // Vibrant artwork: enhance and preserve rich colors
            hsl[1] = (hsl[1] * 1.25f).coerceIn(0.25f, 0.85f)
        }

        hsl[2] = hsl[2].coerceIn(targetLightness - 0.06f, targetLightness + 0.08f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun toDeepBaseColor(color: Color, weightedSat: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        if (weightedSat < 0.16f) {
            hsl[1] = hsl[1].coerceAtMost(0.03f)
        } else {
            hsl[1] = (hsl[1] * 0.5f).coerceIn(0.04f, 0.30f)
        }
        hsl[2] = 0.055f // Deep velvety rich background tone (~5.5% luminance)
        return Color(ColorUtils.HSLToColor(hsl))
    }
}

/**
 * Composable helper that remembers and updates [ExtractedPalette] reactively
 * without blocking the main composition thread.
 */
@Composable
fun rememberCoverPalette(
    context: Context,
    trackId: String,
    coverUri: String?,
    fallbackUri: String? = null,
    existingBitmap: Bitmap? = null,
): ExtractedPalette {
    val state = remember(trackId) {
        mutableStateOf(PaletteExtractor.getCached(trackId) ?: ExtractedPalette.Default)
    }

    LaunchedEffect(trackId, coverUri, fallbackUri) {
        val extracted = PaletteExtractor.extract(context, trackId, coverUri, fallbackUri, existingBitmap)
        state.value = extracted
    }

    return state.value
}
