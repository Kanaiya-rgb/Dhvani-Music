package com.music.dhvani.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture

/**
 * Media3 [BitmapLoader] powered by Coil 3.
 *
 * Provides decoded, software-rendered [Bitmap] instances to [androidx.media3.session.MediaSession]
 * and [androidx.media3.session.DefaultMediaNotificationProvider].
 *
 * [allowHardware(false)] ensures bitmaps are safe for IPC transfer across Binder to
 * SystemUI, Samsung One UI Now Bar, and lockscreen controllers without HardwareBuffer errors.
 */
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return scope.async(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalArgumentException("Failed to decode bitmap from byte array")
        }.asListenableFuture()
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (!AppSettings.dynamicLockscreenArt.value) {
            return Futures.immediateFailedFuture(IllegalStateException("Dynamic lockscreen art disabled"))
        }

        return scope.async(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(800) // Optimal size for lockscreen card and notification without exceeding Binder transaction limit
                .allowHardware(false)
                .build()
            val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
            if (result is SuccessResult) {
                result.image.toBitmap()
            } else {
                throw IllegalStateException("Failed to load bitmap from $uri")
            }
        }.asListenableFuture()
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        if (!AppSettings.dynamicLockscreenArt.value) {
            return Futures.immediateFailedFuture(IllegalStateException("Dynamic lockscreen art disabled"))
        }
        metadata.artworkData?.let { return decodeBitmap(it) }
        metadata.artworkUri?.let { return loadBitmap(it) }
        return Futures.immediateFailedFuture(IllegalArgumentException("No artwork provided in metadata"))
    }
}
