package com.music.dhvani.data.monochrome

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.net.URLEncoder

private const val TAG = "DhvaniMusic"

data class MonochromeTrack(
    val id: String,
    val title: String,
    val artistNames: String,
    val durationMs: Long,
    val artwork: String?,
    val isrc: String? = null,
    val playable: Boolean = true,
)

object MonochromeService {

    private const val PRIMARY_HOST = "https://tracks.monochrome.st"
    private val FALLBACK_HOSTS = emptyList<String>()

    val fastClient: okhttp3.OkHttpClient = Http.client.newBuilder()
        .connectTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .callTimeout(10000, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var offlineUntilMs = 0L

    fun isOffline(): Boolean = System.currentTimeMillis() < offlineUntilMs

    fun markOffline(durationMs: Long = 300_000L) {
        offlineUntilMs = System.currentTimeMillis() + durationMs
    }

    fun markOnline() {
        offlineUntilMs = 0L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Checks if the service is currently reachable.
     */
    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        if (isOffline()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$PRIMARY_HOST/search?q=test")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                .head()
                .build()
            fastClient.newCall(request).execute().use { response ->
                val ok = response.isSuccessful || response.code in 200..399
                if (ok) markOnline() else if (response.code in 502..504) markOffline(60_000L)
                ok
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Monochrome health check failed: ${e.message}")
            false
        }
    }

    /**
     * Searches Monochrome for tracks matching [query].
     * Parses both 'topResults' and 'tracks' arrays from the response.
     */
    suspend fun search(query: String): List<MonochromeTrack> = withContext(Dispatchers.IO) {
        if (isOffline()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urls = listOf("$PRIMARY_HOST/search?q=$encodedQuery") +
            FALLBACK_HOSTS.map { "$it/search?q=$encodedQuery" }

        for (url in urls) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(4500L) {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                        .get()
                        .build()

                    fastClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            if (response.code in 502..504) markOffline(60_000L)
                            return@use null
                        }
                        val bodyString = response.body?.string() ?: return@use null
                        val results = parseSearchResults(bodyString)
                        if (results.isNotEmpty()) {
                            markOnline()
                            TrackLog.d(TAG, "Monochrome search for '$query' returned ${results.size} tracks from $url")
                            return@withTimeoutOrNull results
                        }
                        null
                    }
                }?.let { return@withContext it }
            } catch (e: Exception) {
                TrackLog.w(TAG, "Monochrome search failed on $url: ${e.message}")
            }
        }

        emptyList()
    }

    /**
     * Returns the direct raw 24-bit 96kHz FLAC stream URL for a given track id.
     * Note: Monochrome requires the `?quality=LOSSLESS` query parameter to avoid Cloudflare 429 throttling.
     */
    fun getStreamUrl(trackId: String): String {
        return "$PRIMARY_HOST/track/$trackId?quality=LOSSLESS"
    }

    private fun parseSearchResults(body: String): List<MonochromeTrack> {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val results = mutableListOf<MonochromeTrack>()
            val seenIds = mutableSetOf<String>()

            fun extractTrack(item: JsonObject) {
                val id = item["id"]?.jsonPrimitive?.content
                    ?: item["trackId"]?.jsonPrimitive?.content
                    ?: return

                if (!seenIds.add(id)) return

                val title = item["title"]?.jsonPrimitive?.content.orEmpty().ifBlank { "Unknown Title" }
                val durationMs = item["duration"]?.jsonPrimitive?.longOrNull ?: 0L
                val artwork = item["artwork"]?.jsonPrimitive?.content
                val isrc = item["isrc"]?.jsonPrimitive?.content
                val playable = item["playable"]?.jsonPrimitive?.booleanOrNull ?: true

                val artistsRaw = item["artistNames"]
                val artistString = when (artistsRaw) {
                    is JsonArray -> artistsRaw.mapNotNull { it.jsonPrimitive.content }.joinToString(", ")
                    is JsonElement -> artistsRaw.jsonPrimitive.content
                    else -> "Unknown Artist"
                }.ifBlank { "Unknown Artist" }

                results.add(
                    MonochromeTrack(
                        id = id,
                        title = title,
                        artistNames = artistString,
                        durationMs = durationMs,
                        artwork = artwork,
                        isrc = isrc,
                        playable = playable,
                    )
                )
            }

            // 1. Process topResults first
            root["topResults"]?.let { top ->
                if (top is JsonArray) {
                    top.forEach { elem ->
                        if (elem is JsonObject) extractTrack(elem)
                    }
                }
            }

            // 2. Process general tracks array
            root["tracks"]?.let { trk ->
                if (trk is JsonArray) {
                    trk.forEach { elem ->
                        if (elem is JsonObject) extractTrack(elem)
                    }
                }
            }

            results
        }.getOrElse {
            TrackLog.w(TAG, "Failed to parse Monochrome search results: ${it.message}")
            emptyList()
        }
    }
}
