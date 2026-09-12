package com.music.dhvani.playback

import android.content.Context
import android.net.Uri
import com.music.dhvani.data.model.Song
import kotlinx.coroutines.CoroutineScope

/**
 * Legacy lockscreen wallpaper manager - deactivated and replaced with safe no-ops
 * to guarantee device lockscreen wallpapers remain completely untouched.
 */
object LockscreenWallpaperManager {
    fun applyWallpaperFor(
        context: Context,
        scope: CoroutineScope? = null,
        song: Song?,
        force: Boolean = false,
    ) {
        // Safe no-op: lockscreen wallpaper is never modified
    }

    fun clearWallpaper(context: Context) {
        // Safe no-op
    }

    fun clearWallpaperSync(context: Context) {
        // Safe no-op
    }

    fun saveCustomRestoreWallpaper(context: Context, uri: Uri) {
        // Safe no-op
    }

    fun hasCustomRestoreWallpaper(context: Context): Boolean = false

    fun clearCustomRestoreWallpaper(context: Context) {
        // Safe no-op
    }
}
