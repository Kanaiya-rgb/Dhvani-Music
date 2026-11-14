package com.music.dhvani.data.artist

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Universal signature / autograph processor.
 *
 * Automatically handles:
 * 1. Scanned physical signatures on paper (JPEGs, white-background PNGs like Arijit Singh, Kishore Kumar):
 *    - Automatically detects paper background from perimeter border sampling
 *    - Strips paper background completely to transparent alpha
 *    - Converts dark ink into crisp, smooth antialiased pure white ink (255, 255, 255, alpha)
 * 2. Transparent digital vector/PNG signatures (like Taylor Swift, Billie Eilish, Atif Aslam):
 *    - Preserves existing transparency and antialiasing
 *    - Renders existing strokes/ink in pure white for high contrast over dark artist art
 * 3. Auto-crops empty margins/whitespace so signatures always scale up to their optimal visible size.
 */
class ArtistSignatureTransformation : Transformation() {

    override val cacheKey: String = "ArtistSignatureTransformation_v4"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Check corners for native transparency
        val cornerIndices = intArrayOf(
            0,
            width - 1,
            (height - 1) * width,
            (height - 1) * width + (width - 1),
        )

        var transparentCorners = 0
        for (idx in cornerIndices) {
            val a = (pixels[idx] ushr 24) and 0xFF
            if (a < 50) {
                transparentCorners++
            }
        }

        // Quick check: If at least 2 corners are transparent, it's natively transparent
        val isNativelyTransparent = transparentCorners >= 2

        val outPixels = IntArray(width * height)

        if (isNativelyTransparent) {
            // Already transparent PNG / SVG (Taylor Swift, Atif Aslam, etc.)
            // Make any non-transparent stroke pure white while preserving alpha antialiasing
            for (i in pixels.indices) {
                val color = pixels[i]
                val a = (color ushr 24) and 0xFF
                outPixels[i] = if (a == 0) 0 else (a shl 24) or 0x00FFFFFF
            }
        } else {
            // Scanned paper autograph (Arijit Singh, IndianAutographs JPEGs, etc.)
            // Sample top and bottom rows to compute dominant paper background luminance
            var borderLumSum = 0.0
            var sampleCount = 0

            for (x in 0 until width step 2) {
                val topColor = pixels[x]
                val bottomColor = pixels[(height - 1) * width + x]

                val tr = (topColor ushr 16) and 0xFF
                val tg = (topColor ushr 8) and 0xFF
                val tb = topColor and 0xFF
                borderLumSum += (0.299f * tr + 0.587f * tg + 0.114f * tb)
                sampleCount++

                val br = (bottomColor ushr 16) and 0xFF
                val bg = (bottomColor ushr 8) and 0xFF
                val bb = bottomColor and 0xFF
                borderLumSum += (0.299f * br + 0.587f * bg + 0.114f * bb)
                sampleCount++
            }

            val bgLum = if (sampleCount > 0) (borderLumSum / sampleCount).toFloat() else 255f

            for (i in pixels.indices) {
                val color = pixels[i]
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                // Ink contrast relative to paper background
                val ink = (bgLum - lum).coerceAtLeast(0f)

                // Noise floor threshold: paper texture & faint scan artifacts (< 30) become transparent
                val alpha = when {
                    ink <= 30f -> 0
                    ink >= 160f -> 255
                    else -> (((ink - 30f) * 255f) / (160f - 30f)).toInt().coerceIn(0, 255)
                }

                outPixels[i] = if (alpha == 0) 0 else (alpha shl 24) or 0x00FFFFFF
            }
        }

        // Auto-crop empty bounding margins so ink occupies the full container
        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val a = (outPixels[rowOffset + x] ushr 24) and 0xFF
                if (a > 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        fullBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)

        if (maxX >= minX && maxY >= minY) {
            val pad = 4
            val cropX = (minX - pad).coerceAtLeast(0)
            val cropY = (minY - pad).coerceAtLeast(0)
            val cropW = (maxX - minX + 1 + pad * 2).coerceAtMost(width - cropX)
            val cropH = (maxY - minY + 1 + pad * 2).coerceAtMost(height - cropY)
            if (cropW > 0 && cropH > 0 && (cropW < width || cropH < height)) {
                val cropped = Bitmap.createBitmap(fullBitmap, cropX, cropY, cropW, cropH)
                if (cropped != fullBitmap) {
                    fullBitmap.recycle()
                }
                return cropped
            }
        }

        return fullBitmap
    }
}
