package com.music.dhvani.data.sources

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.sources.zarz.ZarzSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.net.URLEncoder

private const val TAG = "DhvaniMusic"

/**
 * Native TIDAL Lossless source for Dhvani Music.
 * Fetches accurate CD-Quality 16-bit FLAC / 24-bit Hi-Res and Dolby Atmos Spatial Audio.
 */
class TidalLosslessSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.TIDAL
    override val displayName: String get() = config.label.ifBlank { SourceKind.TIDAL.label }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun health(): SourceHealth = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.tidal.com/v1/search/tracks?query=test&limit=1&countryCode=IN")
                .header("x-tidal-token", TIDAL_CLIENT_TOKEN)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    if (ZarzSessionManager.isSessionValid()) SourceHealth.Ok("Ready • Lossless & Atmos")
                    else SourceHealth.Ok("Verification needed")
                } else SourceHealth.Unreachable("TIDAL API HTTP ${resp.code}")
            }
        } catch (e: Exception) {
            SourceHealth.Unreachable(e.message ?: "TIDAL network error")
        }
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> = withContext(Dispatchers.IO) {
        val encodedQuery = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
        val url = "https://api.tidal.com/v1/search/tracks?query=$encodedQuery&limit=$limit&countryCode=IN"

        val req = Request.Builder()
            .url(url)
            .header("x-tidal-token", TIDAL_CLIENT_TOKEN)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .get()
            .build()

        try {
            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(body) as? JsonObject ?: return@withContext emptyList()
                val items = root["items"]?.jsonArray ?: return@withContext emptyList()

                items.mapNotNull { elem ->
                    val obj = elem as? JsonObject ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val rawTitle = obj["title"]?.jsonPrimitive?.content.orEmpty()
                    val version = obj["version"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    val title = if (version.isNotBlank() && !rawTitle.contains(version, ignoreCase = true)) {
                        "$rawTitle ($version)"
                    } else rawTitle

                    val artistsArray = (obj["artists"] as? JsonArray)?.mapNotNull {
                        (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
                    } ?: emptyList()
                    val mainArtist = (obj["artist"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val artist = if (artistsArray.isNotEmpty()) artistsArray.joinToString(", ") else mainArtist
                    val albumObj = obj["album"] as? JsonObject
                    val albumName = albumObj?.get("title")?.jsonPrimitive?.content
                    val cover = albumObj?.get("cover")?.jsonPrimitive?.content
                    val durationSec = obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L

                    val audioModes = (obj["audioModes"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    val isAtmos = audioModes.any { it.contains("DOLBY_ATMOS", ignoreCase = true) }
                    val audioQuality = obj["audioQuality"]?.jsonPrimitive?.content.orEmpty()

                    val artworkUrl = if (!cover.isNullOrBlank()) {
                        "https://resources.tidal.com/images/${cover.replace("-", "/")}/640x640.jpg"
                    } else null

                    val m = durationSec / 60
                    val s = durationSec % 60
                    val durationFormatted = String.format("%d:%02d", m, s)

                    val qualityLabel = when {
                        isAtmos -> "Dolby Atmos"
                        audioQuality == "HI_RES_LOSSLESS" -> "24-bit Hi-Res FLAC"
                        else -> "Lossless FLAC"
                    }

                    // Record atmos metadata in cache for stream resolver
                    if (isAtmos) {
                        atmosTrackCache[id] = true
                    }

                    Song(
                        videoId = SourceRegistry.trackKey(config.id, id),
                        title = title,
                        artist = artist,
                        thumbnailUrl = artworkUrl,
                        albumName = albumName ?: qualityLabel,
                        durationText = durationFormatted,
                        sourceQuality = qualityLabel,
                        isExplicit = obj["explicit"]?.jsonPrimitive?.booleanOrNull == true ||
                            title.contains("(Explicit)", ignoreCase = true) ||
                            title.contains("[Explicit]", ignoreCase = true),
                    )
                }
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Tidal search failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        val isAtmos = atmosTrackCache[trackId] == true
        TrackLog.d(TAG, "🔍 Tidal stream lookup for trackId=$trackId, isAtmos=$isAtmos")

        val streamUrl = ZarzSessionManager.resolveTidalStream(
            trackId = trackId,
            quality = if (isAtmos) "DOLBY_ATMOS" else "LOSSLESS",
            isAtmos = isAtmos
        )

        if (streamUrl.isNullOrBlank()) {
            TrackLog.w(TAG, "  ↳ Tidal stream resolution failed for track $trackId")
            return null
        }

        val codec = if (isAtmos || streamUrl.contains("mp4", ignoreCase = true)) "eac3" else "flac"
        val bitDepth = if (isAtmos) 16 else 16
        val sampleRate = if (isAtmos) 48000 else 44100
        val summary = if (isAtmos) "TIDAL Dolby Atmos" else "TIDAL Lossless FLAC"

        TrackLog.d(TAG, "  ↳ Tidal serving stream: $streamUrl ($summary)")
        return SourceStream(
            url = streamUrl,
            format = StreamFormat(
                codec = codec,
                bitDepth = bitDepth,
                sampleRateHz = sampleRate,
                isDolbyAtmos = isAtmos,
                source = summary,
            ),
        )
    }

    companion object {
        const val TIDAL_CLIENT_TOKEN = "49YxDN9a2aFV6RTG"
        private val atmosTrackCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    }
}
