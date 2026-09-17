package com.music.dhvani.data.canvas

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.music.dhvani.data.lyrics.LyricsCleaner
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Repository orchestrating motion canvas video artwork lookups across:
 * 1. Apple Music
 * 2. Tidal
 * 3. Community index
 *
 * Employs mutex-serialized lookups and aggressive in-memory negative LRU caching
 * to eliminate duplicate and redundant network queries.
 */
object CanvasRepository {
    private const val CACHE_LIMIT = 150

    private data class CacheEntry(val artwork: CanvasArtwork?)

    private val cache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(CACHE_LIMIT, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean =
                size > CACHE_LIMIT
        },
    )

    private val mutex = Mutex()

    fun getCached(videoId: String): CanvasArtwork? {
        if (videoId.isBlank()) return null
        return cache[videoId]?.artwork
    }

    fun hasCached(videoId: String): Boolean =
        videoId.isNotBlank() && cache.containsKey(videoId)

    suspend fun getCanvas(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (!AppSettings.animatedCanvas.value) return@withContext null
        if (videoId.isBlank() && title.isBlank()) return@withContext null

        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)

        val cacheKey = if (videoId.isNotBlank()) videoId else "${cleanTitle.trim()}|${cleanArtist.trim()}"

        // Instant retrieval from bounded LRU cache (including negative hits)
        cache[cacheKey]?.let { entry ->
            return@withContext entry.artwork
        }

        // Cellular data restriction guard
        if (!AppSettings.canvasOverCellular.value && isCellular(context)) {
            return@withContext null
        }

        mutex.withLock {
            // Re-check cache after acquiring lock
            cache[cacheKey]?.let { entry ->
                return@withContext entry.artwork
            }

            // Lookup chain with cleaned metadata first: Apple Music -> Tidal -> Community
            var result = AppleMusicCanvas.fetch(cleanTitle, cleanArtist, album)
                ?: TidalCanvas.fetch(cleanTitle, cleanArtist, album)
                ?: CommunityCanvas.fetch(cleanTitle, cleanArtist, album)

            // If clean didn't match and raw differs, fallback to raw query
            if (result == null && (cleanTitle != title || cleanArtist != artist)) {
                result = AppleMusicCanvas.fetch(title, artist, album)
                    ?: TidalCanvas.fetch(title, artist, album)
                    ?: CommunityCanvas.fetch(title, artist, album)
            }

            // Store result (or null for negative caching)
            cache[cacheKey] = CacheEntry(result)
            result
        }
    }

    private fun isCellular(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun clearCache() {
        cache.clear()
    }
}
