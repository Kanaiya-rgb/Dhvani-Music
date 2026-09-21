package com.music.dhvani.data.canvas

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.music.dhvani.data.lyrics.LyricsCleaner
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.download.DownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Repository orchestrating motion canvas video artwork lookups across:
 * 1. Spotify
 * 2. Apple Music
 * 3. Tidal
 * 4. Community index
 *
 * Employs concurrent queries, mutex-serialized lookups, and aggressive in-memory
 * LRU caching to eliminate duplicate and redundant network queries.
 */
object CanvasRepository {
    private const val CACHE_LIMIT = 150

    private data class CacheEntry(
        val artworks: Map<CanvasSource, CanvasArtwork>,
        var selectedSource: CanvasSource? = null,
    )

    private val cache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(CACHE_LIMIT, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean =
                size > CACHE_LIMIT
        },
    )

    private val mutex = Mutex()
    private var lastUserSelectedSource: CanvasSource? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getSelectedSource(videoId: String): CanvasSource? {
        if (videoId.isBlank()) return lastUserSelectedSource
        return cache[videoId]?.selectedSource ?: lastUserSelectedSource
    }

    fun setSelectedSource(videoId: String, source: CanvasSource) {
        lastUserSelectedSource = source
        if (videoId.isNotBlank()) {
            cache[videoId]?.selectedSource = source
        }
    }

    fun getCached(videoId: String): CanvasArtwork? {
        if (videoId.isBlank()) return null
        appContext?.let { ctx ->
            DownloadStore.getOfflineCanvas(ctx, videoId)?.let { return it }
        }
        val entry = cache[videoId] ?: return null
        val map = entry.artworks
        val chosen = entry.selectedSource ?: lastUserSelectedSource
        return (chosen?.let { map[it] })
            ?: map[CanvasSource.SPOTIFY]
            ?: map[CanvasSource.APPLE_MUSIC]
            ?: map[CanvasSource.TIDAL]
            ?: map[CanvasSource.COMMUNITY]
    }

    fun getCachedMap(videoId: String): Map<CanvasSource, CanvasArtwork> {
        if (videoId.isBlank()) return emptyMap()
        appContext?.let { ctx ->
            DownloadStore.getOfflineCanvas(ctx, videoId)?.let {
                return mapOf(it.source to it)
            }
        }
        return cache[videoId]?.artworks ?: emptyMap()
    }

    fun hasCached(videoId: String): Boolean {
        if (videoId.isBlank()) return false
        if (cache.containsKey(videoId)) return true
        return appContext?.let { DownloadStore.hasOfflineCanvas(it, videoId) } ?: false
    }

    suspend fun getAvailableCanvases(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
    ): Map<CanvasSource, CanvasArtwork> = withContext(Dispatchers.IO) {
        if (!AppSettings.animatedCanvas.value) return@withContext emptyMap()
        if (videoId.isBlank() && title.isBlank()) return@withContext emptyMap()

        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)

        val cacheKey = if (videoId.isNotBlank()) videoId else "${cleanTitle.trim()}|${cleanArtist.trim()}"

        // Instant offline canvas file retrieval
        val offline = DownloadStore.getOfflineCanvas(context, videoId)
        if (offline != null) {
            val offlineMap = mapOf(offline.source to offline)
            cache[cacheKey] = CacheEntry(offlineMap, offline.source)
            return@withContext offlineMap
        }

        // Instant retrieval from bounded LRU cache
        cache[cacheKey]?.let { entry ->
            return@withContext entry.artworks
        }

        // Cellular data restriction guard
        if (!AppSettings.canvasOverCellular.value && isCellular(context)) {
            return@withContext emptyMap()
        }

        mutex.withLock {
            // Re-check cache after acquiring lock
            cache[cacheKey]?.let { entry ->
                return@withContext entry.artworks
            }

            // Query all 4 sources concurrently in parallel
            val resultMap = mutableMapOf<CanvasSource, CanvasArtwork>()
            coroutineScope {
                val spotifyDeferred = async {
                    SpotifyCanvas.fetch(cleanTitle, cleanArtist, album)
                        ?: if (cleanTitle != title || cleanArtist != artist) SpotifyCanvas.fetch(title, artist, album) else null
                }
                val appleDeferred = async {
                    AppleMusicCanvas.fetch(cleanTitle, cleanArtist, album)
                        ?: if (cleanTitle != title || cleanArtist != artist) AppleMusicCanvas.fetch(title, artist, album) else null
                }
                val tidalDeferred = async {
                    TidalCanvas.fetch(cleanTitle, cleanArtist, album)
                        ?: if (cleanTitle != title || cleanArtist != artist) TidalCanvas.fetch(title, artist, album) else null
                }
                val communityDeferred = async {
                    CommunityCanvas.fetch(cleanTitle, cleanArtist, album)
                        ?: if (cleanTitle != title || cleanArtist != artist) CommunityCanvas.fetch(title, artist, album) else null
                }

                spotifyDeferred.await()?.let { resultMap[CanvasSource.SPOTIFY] = it }
                appleDeferred.await()?.let { resultMap[CanvasSource.APPLE_MUSIC] = it }
                tidalDeferred.await()?.let { resultMap[CanvasSource.TIDAL] = it }
                communityDeferred.await()?.let { resultMap[CanvasSource.COMMUNITY] = it }
            }

            // Store result
            val existingSelected = cache[cacheKey]?.selectedSource ?: lastUserSelectedSource
            cache[cacheKey] = CacheEntry(resultMap, existingSelected)
            resultMap
        }
    }

    suspend fun getCanvas(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? {
        val map = getAvailableCanvases(context, videoId, title, artist, album)
        val chosen = getSelectedSource(videoId)
        return (chosen?.let { map[it] })
            ?: map[CanvasSource.SPOTIFY]
            ?: map[CanvasSource.APPLE_MUSIC]
            ?: map[CanvasSource.TIDAL]
            ?: map[CanvasSource.COMMUNITY]
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
