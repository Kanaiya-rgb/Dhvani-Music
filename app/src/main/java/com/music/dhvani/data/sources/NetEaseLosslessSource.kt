package com.music.dhvani.data.sources

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale

private const val TAG = "NetEaseLosslessSource"

/**
 * Native NetEase Cloud Music source for Dhvani Music.
 * Fetches high quality 320kbps MP3 audio streams via NetEase Cloud Music CDN.
 */
class NetEaseLosslessSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.NETEASE
    override val displayName: String get() = config.label.ifBlank { SourceKind.NETEASE.label }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun baseUrl(): String {
        val configured = config.baseUrl.trim().removeSuffix("/")
        return if (configured.isNotBlank()) configured else DEFAULT_API_BASE
    }

    override suspend fun health(): SourceHealth = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl()}?name=test&page=1&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    val root = runCatching { json.parseToJsonElement(body.orEmpty()).jsonObject }.getOrNull()
                    val code = root?.get("code")?.jsonPrimitive?.intOrNull ?: -1
                    if (code == 0 || root?.containsKey("data") == true) {
                        SourceHealth.Ok("Ready • 320kbps MP3")
                    } else {
                        SourceHealth.Unreachable("API response code: $code")
                    }
                } else {
                    SourceHealth.Unreachable("HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            SourceHealth.Unreachable(e.message ?: "NetEase network error")
        }
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val cacheKey = cleanQuery.lowercase(Locale.ROOT)
        searchCache[cacheKey]?.let {
            TrackLog.d(TAG, "NetEase search cache hit for '$cleanQuery'")
            return@withContext it
        }

        val encoded = runCatching { URLEncoder.encode(cleanQuery, "UTF-8") }.getOrDefault(cleanQuery)
        val url = "${baseUrl()}?name=$encoded&page=1&limit=$limit"

        TrackLog.d(TAG, "Searching NetEase: $url")
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    TrackLog.w(TAG, "NetEase search HTTP ${resp.code}")
                    return@use emptyList()
                }
                val body = resp.body?.string() ?: return@use emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"] as? JsonArray ?: return@use emptyList()

                val songs = data.mapIndexedNotNull { idx, el ->
                    val obj = el as? JsonObject ?: return@mapIndexedNotNull null
                    val songTitle = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?.replace(Regex("""^\d+\.\s*"""), "")
                        ?.ifBlank { null } ?: return@mapIndexedNotNull null

                    val singersList = obj["singers"] as? JsonArray
                    val singer = singersList?.mapNotNull { s ->
                        (s as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull?.trim()
                    }?.joinToString(", ")?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"

                    val songId = obj["id"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                    val rawPic = obj["picurl"]?.jsonPrimitive?.contentOrNull?.trim()
                    val picture = if (rawPic?.startsWith("http://", ignoreCase = true) == true) {
                        "https://" + rawPic.substring(7)
                    } else rawPic

                    val trackIdentifier = "${idx + 1}::$songId::$songTitle"

                    Song(
                        videoId = SourceRegistry.trackKey(config.id, trackIdentifier),
                        title = songTitle,
                        artist = singer,
                        thumbnailUrl = picture,
                        albumName = "NetEase Cloud Music",
                        durationText = "",
                        sourceQuality = "320kbps MP3",
                        isExplicit = false,
                    )
                }
                searchCache[cacheKey] = songs
                songs
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "NetEase search failed for '$query': ${e.message}")
            emptyList()
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? = withContext(Dispatchers.IO) {
        streamCache[trackId]?.let {
            TrackLog.d(TAG, "NetEase stream cache hit for trackId=$trackId")
            return@withContext it
        }

        TrackLog.d(TAG, "🔍 NetEase stream lookup for trackId=$trackId")

        // Parse index, songId, and title from trackId (format: "$index::$id::$title")
        val parts = trackId.split("::")
        val (index, songId, title) = when {
            parts.size >= 3 -> Triple(parts[0].toIntOrNull() ?: 1, parts[1], parts.subList(2, parts.size).joinToString("::"))
            parts.size == 2 -> Triple(parts[0].toIntOrNull() ?: 1, "", parts[1])
            else -> Triple(1, "", trackId)
        }

        // Try lookup by ID first if available
        var streamUrl: String? = null
        if (songId.isNotBlank() && songId.all { it.isDigit() }) {
            streamUrl = resolveStreamById(songId)
        }

        // Fallback to title and index if ID lookup was empty
        if (streamUrl.isNullOrBlank()) {
            streamUrl = resolveStreamByTitle(title, index)
        }

        if (streamUrl.isNullOrBlank()) {
            TrackLog.w(TAG, "NetEase stream URL unresolved for '$title'")
            return@withContext null
        }

        val secureStreamUrl = if (streamUrl.startsWith("http://", ignoreCase = true)) {
            "https://" + streamUrl.substring(7)
        } else {
            streamUrl
        }

        val stream = SourceStream(
            url = secureStreamUrl,
            format = StreamFormat(
                codec = "mp3",
                bitDepth = null,
                sampleRateHz = 44100,
                kbps = 320,
                source = "NetEase 320kbps MP3",
            ),
        )

        streamCache[trackId] = stream
        stream
    }

    private fun resolveStreamById(songId: String): String? {
        try {
            val url = "${baseUrl()}?id=$songId"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]
                return when (data) {
                    is JsonArray -> (data.firstOrNull() as? JsonObject)?.get("url")?.jsonPrimitive?.contentOrNull?.trim()
                    is JsonObject -> data["url"]?.jsonPrimitive?.contentOrNull?.trim()
                    else -> null
                }
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "NetEase stream lookup by ID failed for id=$songId: ${e.message}")
            return null
        }
    }

    private fun resolveStreamByTitle(title: String, index: Int): String? {
        try {
            val encodedTitle = runCatching { URLEncoder.encode(title, "UTF-8") }.getOrDefault(title)
            val url = "${baseUrl()}?name=$encodedTitle&n=$index"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"] as? JsonObject ?: return null
                return data["url"]?.jsonPrimitive?.contentOrNull?.trim()
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "NetEase stream lookup by title failed for '$title': ${e.message}")
            return null
        }
    }

    companion object {
        const val DEFAULT_API_BASE = "https://oiapi.net/api/Music_163"
        private val searchCache = HashMap<String, List<Song>>()
        private val streamCache = HashMap<String, SourceStream>()
    }
}

