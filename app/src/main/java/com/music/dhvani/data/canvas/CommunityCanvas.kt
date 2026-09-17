package com.music.dhvani.data.canvas

import com.music.dhvani.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * Community-curated motion canvas fallback index from ViviMusic / BitChord.
 * Fetches and caches the index in-memory so lookups are fast and offline-friendly.
 */
object CommunityCanvas {
    private const val INDEX_URL = "https://vivimusicanvas.mkmdevilmi.workers.dev/canvas.json"
    private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L // 12 hours

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private data class CommunityItem(
        val song: String,
        val artist: String,
        val url: String,
        val album: String?,
    )

    @Volatile
    private var cachedItems: List<CommunityItem>? = null
    @Volatile
    private var lastFetchedMs: Long = 0L

    suspend fun fetch(title: String, artist: String, album: String? = null): CanvasArtwork? =
        withContext(Dispatchers.IO) {
            val items = getOrFetchItems() ?: return@withContext null

            for (item in items) {
                val candidate = CanvasArtwork(
                    url = item.url,
                    title = item.song,
                    artist = item.artist,
                    album = item.album,
                    source = CanvasSource.COMMUNITY,
                )

                if (candidate.matches(title, artist, album)) {
                    return@withContext candidate
                }
            }

            null
        }

    private fun getOrFetchItems(): List<CommunityItem>? {
        val now = System.currentTimeMillis()
        val inMemory = cachedItems
        if (inMemory != null && now - lastFetchedMs < CACHE_TTL_MS) {
            return inMemory
        }

        val request = Request.Builder()
            .url(INDEX_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        return runCatching {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use cachedItems
                val body = response.body?.string() ?: return@use cachedItems
                parseIndex(body).also {
                    cachedItems = it
                    lastFetchedMs = now
                }
            }
        }.getOrNull() ?: cachedItems
    }

    private fun parseIndex(jsonString: String): List<CommunityItem> {
        return runCatching {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val itemsArray = root["items"]?.jsonArray ?: return emptyList()
            val list = ArrayList<CommunityItem>(itemsArray.size)

            for (elem in itemsArray) {
                val obj = elem.jsonObject
                val song = obj["song"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                val artist = obj["artist"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                val url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                val album = obj["album"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() && it != "null" }

                if (song.isNotBlank() && url.isNotBlank()) {
                    list.add(CommunityItem(song = song, artist = artist, url = url, album = album))
                }
            }

            list
        }.getOrDefault(emptyList())
    }
}
