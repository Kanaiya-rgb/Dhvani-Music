package com.music.dhvani.playback

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Automatically renders and applies dynamic, iOS-style full-screen album art
 * wallpapers to the device lockscreen (FLAG_LOCK).
 *
 * Does not touch the home screen wallpaper.
 */
object LockscreenWallpaperManager {

    private var currentJob: Job? = null
    private var lastAppliedVideoId: String? = null

    /**
     * Renders and applies the lockscreen wallpaper for the currently playing song.
     */
    fun applyWallpaperFor(context: Context, scope: CoroutineScope, song: Song?) {
        if (!AppSettings.dynamicLockscreenArt.value) return
        if (song == null) return
        if (song.videoId == lastAppliedVideoId) return

        currentJob?.cancel()
        currentJob = scope.launch(Dispatchers.IO) {
            val url = song.artworkAt(1024) ?: song.thumbnailUrl
            if (url.isNullOrBlank()) return@launch

            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isSetWallpaperAllowed) return@launch

            val cover = loadCoverBitmap(context, url) ?: return@launch
            val screenMetrics = context.resources.displayMetrics
            val screenW = screenMetrics.widthPixels.coerceAtLeast(1080)
            val screenH = screenMetrics.heightPixels.coerceAtLeast(1920)

            val wallpaper = composeLockscreenWallpaper(cover, screenW, screenH)

            try {
                wallpaperManager.setBitmap(wallpaper, null, true, WallpaperManager.FLAG_LOCK)
                lastAppliedVideoId = song.videoId
            } catch (e: Exception) {
                // Ignore if device OEM blocks lockscreen wallpaper updates
            } finally {
                wallpaper.recycle()
            }
        }
    }

    /**
     * Clears the dynamic lockscreen wallpaper so the lockscreen displays
     * the system default / home screen wallpaper again.
     */
    fun clearWallpaper(context: Context, scope: CoroutineScope) {
        currentJob?.cancel()
        currentJob = scope.launch(Dispatchers.IO) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isSetWallpaperAllowed) return@launch
            try {
                wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
                lastAppliedVideoId = null
            } catch (e: Exception) {
                // Ignore OEM limitations
            }
        }
    }

    private suspend fun loadCoverBitmap(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(1024)
            .allowHardware(false)
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    /**
     * Composes a breathtaking iOS 16-style lockscreen wallpaper:
     * 1. Ambient Background: scaled and blurred version of the cover art filling the canvas.
     * 2. Dark Scrim: subtle darkening across the whole image for clock/status bar legibility.
     * 3. Centerpiece Album Cover: crisp, sharp cover art centered in the upper-mid region with rounded corners.
     * 4. Bottom Dark Gradient Ramp: smooth transition to deep dark tones at the bottom 40% where the
     *    lockscreen media player card and controls sit.
     */
    private fun composeLockscreenWallpaper(cover: Bitmap, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Ambient Blurred Background
        // Downscale cover to a working size, scale up with bilinear filtering for a dreamy blur
        val blurW = 120
        val blurH = (120f * (height.toFloat() / width.toFloat())).toInt().coerceAtLeast(180)
        val downscaled = Bitmap.createScaledBitmap(cover, blurW, blurH, true)
        val bgPaint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(downscaled, null, Rect(0, 0, width, height), bgPaint)
        downscaled.recycle()

        // 2. Subtle ambient dark tint over background
        val tintPaint = Paint().apply {
            color = Color.argb(90, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tintPaint)

        // 3. Crisp Foreground Album Art
        // Positioned in the upper-mid area: width ~ 82% of screen width, centered horizontally
        val artW = (width * 0.82f).toInt()
        val artH = artW // square album cover
        val artLeft = (width - artW) / 2f
        val artTop = height * 0.16f // 16% from top, below clock
        val cornerRadius = width * 0.055f // ~24dp rounded corners

        val artRect = RectF(artLeft, artTop, artLeft + artW, artTop + artH)

        // Soft drop shadow / ambient glow behind the album cover
        val shadowPaint = Paint().apply {
            color = Color.argb(120, 0, 0, 0)
            isAntiAlias = true
        }
        val shadowRect = RectF(artRect.left - 4f, artRect.top + 8f, artRect.right + 4f, artRect.bottom + 16f)
        canvas.drawRoundRect(shadowRect, cornerRadius + 4f, cornerRadius + 4f, shadowPaint)

        // Draw rounded cover art
        val roundedCover = cover.toRoundedCorners(artW, artH, cornerRadius)
        canvas.drawBitmap(roundedCover, artLeft, artTop, Paint().apply { isFilterBitmap = true; isAntiAlias = true })
        roundedCover.recycle()

        // 4. Smooth Bottom Dark Gradient
        // Starts around 58% of screen height down to bottom so media player card and shortcuts have maximum contrast
        val gradientTop = height * 0.55f
        val gradientShader = LinearGradient(
            0f, gradientTop,
            0f, height.toFloat(),
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(160, 10, 10, 15),
                Color.argb(230, 8, 8, 12),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        val gradientPaint = Paint().apply {
            shader = gradientShader
        }
        canvas.drawRect(0f, gradientTop, width.toFloat(), height.toFloat(), gradientPaint)

        return output
    }

    private fun Bitmap.toRoundedCorners(destWidth: Int, destHeight: Int, cornerRadius: Float): Bitmap {
        val scaled = Bitmap.createScaledBitmap(this, destWidth, destHeight, true)
        val rounded = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rounded)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val rect = RectF(0f, 0f, destWidth.toFloat(), destHeight.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        if (scaled != this) scaled.recycle()
        return rounded
    }
}
