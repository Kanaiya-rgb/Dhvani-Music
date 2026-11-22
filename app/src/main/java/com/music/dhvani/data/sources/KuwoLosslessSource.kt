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

private const val TAG = "KuwoLosslessSource"

/**
 * Native Kuwo Music Lossless source for Dhvani Music.
 * Fetches direct 2000 kbps CD-Quality 16/24-bit FLAC streams and 320kbps MP3s via Kuwo CDN.
 */
class KuwoLosslessSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.KUWO
    override val displayName: String get() = config.label.ifBlank { SourceKind.KUWO.label }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun baseUrl(): String {
        val configured = config.baseUrl.trim().removeSuffix("/")
        return if (configured.isNotBlank()) configured else DEFAULT_API_BASE
    }

    override suspend fun health(): SourceHealth = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl()}?msg=test&page=1&limit=1"
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
                    val code = root?.get("code")?.jsonPrimitive?.intOrNull ?: 0
                    if (code == 1 || root?.containsKey("data") == true) {
                        SourceHealth.Ok("Ready • 2000kbps FLAC")
                    } else {
                        SourceHealth.Unreachable("API response code: $code")
                    }
                } else {
                    SourceHealth.Unreachable("HTTP ${resp.code}")
                }
            }
        } catch (e: Exception) {
            SourceHealth.Unreachable(e.message ?: "Kuwo network error")
        }
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val cacheKey = cleanQuery.lowercase(Locale.ROOT)
        searchCache[cacheKey]?.let {
            TrackLog.d(TAG, "Kuwo search cache hit for '$cleanQuery'")
            return@withContext it
        }

        val encoded = runCatching { URLEncoder.encode(cleanQuery, "UTF-8") }.getOrDefault(cleanQuery)
        val url = "${baseUrl()}?msg=$encoded&page=1&limit=$limit"

        TrackLog.d(TAG, "Searching Kuwo: $url")
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    TrackLog.w(TAG, "Kuwo search HTTP ${resp.code}")
                    return@use emptyList()
                }
                val body = resp.body?.string() ?: return@use emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = (root["data"] as? JsonArray)
                    ?: (root["data"] as? JsonObject)?.get("items") as? JsonArray
                    ?: return@use emptyList()

                val songs = data.mapIndexedNotNull { idx, el ->
                    val obj = el as? JsonObject ?: return@mapIndexedNotNull null
                    val songTitle = obj["song"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?.replace(Regex("""^\d+\.\s*"""), "")
                        ?.ifBlank { null } ?: return@mapIndexedNotNull null
                    val singer = obj["singer"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
                    val album = obj["album"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Kuwo FLAC" } ?: "Kuwo FLAC"
                    val rawPic = obj["picture"]?.jsonPrimitive?.contentOrNull?.trim()
                    val picture = if (rawPic?.startsWith("http://", ignoreCase = true) == true) "https://" + rawPic.substring(7) else rawPic
                    val timeSec = obj["time"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                    val rid = obj["rid"]?.jsonPrimitive?.contentOrNull?.trim()?.removePrefix("MUSIC_") ?: ""

                    val types = obj["types"] as? JsonArray
                    val hasFlac = types?.any { t ->
                        val tObj = t as? JsonObject
                        tObj?.get("format")?.jsonPrimitive?.contentOrNull?.contains("flac", ignoreCase = true) == true
                    } ?: true

                    val durationFormatted = "%d:%02d".format(Locale.ROOT, timeSec / 60, timeSec % 60)
                    val quality = if (hasFlac) "2000kbps FLAC" else "320kbps MP3"

                    // Encode sanitized query, 1-based index, rid, and title so stream resolver can locate exact match
                    val sanitizedQuery = cleanQuery.replace("::", " ").trim()
                    val sanitizedTitle = songTitle.replace("::", " ").trim()
                    val trackIdentifier = "$sanitizedQuery::${idx + 1}::$rid::$sanitizedTitle"

                    Song(
                        videoId = SourceRegistry.trackKey(config.id, trackIdentifier),
                        title = songTitle,
                        artist = singer,
                        thumbnailUrl = picture,
                        albumName = album,
                        durationText = durationFormatted,
                        sourceQuality = quality,
                        isExplicit = false,
                    )
                }
                searchCache[cacheKey] = songs
                songs
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Kuwo search failed for '$query': ${e.message}")
            emptyList()
        }
    }

    private fun fetchKuwoStreamObject(url: String): JsonObject? {
        try {
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
                return root["data"] as? JsonObject
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun fetchKuwoSearchList(url: String): List<JsonObject> {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = (root["data"] as? JsonArray)
                    ?: (root["data"] as? JsonObject)?.get("items") as? JsonArray
                    ?: return emptyList()
                return data.mapNotNull { it as? JsonObject }
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? = withContext(Dispatchers.IO) {
        streamCache[trackId]?.let {
            TrackLog.d(TAG, "Kuwo stream cache hit for trackId=$trackId")
            return@withContext it
        }

        TrackLog.d(TAG, "🔍 Kuwo stream lookup for trackId=$trackId")

        // Parse query, index, targetRid, and title from trackId
        val parts = trackId.split("::")
        val (query, index, targetRid, title) = when {
            parts.size >= 4 -> {
                val q = parts[0]
                val idx = parts[1].toIntOrNull() ?: 1
                val r = parts[2].removePrefix("MUSIC_")
                val t = parts.subList(3, parts.size).joinToString("::")
                Tuples4(q, idx, r, t)
            }
            parts.size == 3 -> {
                val idx = parts[0].toIntOrNull() ?: 1
                val r = parts[1].removePrefix("MUSIC_")
                val t = parts[2]
                Tuples4(t, idx, r, t)
            }
            parts.size == 2 -> {
                val idx = parts[0].toIntOrNull() ?: 1
                val t = parts[1]
                Tuples4(t, idx, "", t)
            }
            else -> Tuples4(trackId, 1, "", trackId)
        }

        var streamData: JsonObject? = null

        // 1. Direct hit: Use the exact query (e.g. "Banjaare - Bairan") and its 1-based index from the search
        val primaryQuery = query.ifBlank { title }.trim()
        if (primaryQuery.isNotBlank()) {
            val encodedQuery = runCatching { URLEncoder.encode(primaryQuery, "UTF-8") }.getOrDefault(primaryQuery)
            val url = "${baseUrl()}?msg=$encodedQuery&n=$index&br=1"
            val directObj = fetchKuwoStreamObject(url)
            if (directObj != null) {
                val rawUrl = directObj["url"]?.jsonPrimitive?.contentOrNull?.trim()
                if (!rawUrl.isNullOrBlank()) {
                    val streamRid = directObj["rid"]?.jsonPrimitive?.contentOrNull?.trim()?.removePrefix("MUSIC_")
                        ?: directObj["id"]?.jsonPrimitive?.contentOrNull?.trim()?.removePrefix("MUSIC_")
                    if (targetRid.isBlank() || streamRid == null || streamRid == targetRid) {
                        streamData = directObj
                    } else {
                        TrackLog.w(TAG, "Kuwo direct hit rid mismatch ($streamRid vs expected $targetRid) — falling back to search")
                    }
                }
            }
        }

        // 2. Fallback search: If direct hit failed or mismatched rid, search Kuwo by title (and complete query)
        // and find the candidate whose rid exactly matches targetRid
        if (streamData == null) {
            val searchTerms = listOfNotNull(
                title.takeIf { it.isNotBlank() },
                query.takeIf { it.isNotBlank() && it != title },
            ).distinct()

            for (term in searchTerms) {
                val encodedTerm = runCatching { URLEncoder.encode(term, "UTF-8") }.getOrDefault(term)
                val searchUrl = "${baseUrl()}?msg=$encodedTerm&page=1&limit=10"
                val candidates = fetchKuwoSearchList(searchUrl)
                if (candidates.isEmpty()) continue

                var matchedIndex = 1
                if (targetRid.isNotBlank()) {
                    val foundIdx = candidates.indexOfFirst {
                        it["rid"]?.jsonPrimitive?.contentOrNull?.trim()?.removePrefix("MUSIC_") == targetRid
                    }
                    if (foundIdx >= 0) {
                        matchedIndex = foundIdx + 1
                    }
                }

                val resolveUrl = "${baseUrl()}?msg=$encodedTerm&n=$matchedIndex&br=1"
                val obj = fetchKuwoStreamObject(resolveUrl)
                if (obj != null && !obj["url"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
                    streamData = obj
                    break
                }
            }
        }

        val data = streamData ?: run {
            TrackLog.w(TAG, "Kuwo stream could not be resolved for '$title'")
            return@withContext null
        }

        val rawStreamUrl = data["url"]?.jsonPrimitive?.contentOrNull?.trim()
        if (rawStreamUrl.isNullOrBlank()) {
            TrackLog.w(TAG, "Kuwo stream URL is blank for '$title'")
            return@withContext null
        }
        val streamUrl = if (rawStreamUrl.startsWith("http://", ignoreCase = true)) {
            "https://" + rawStreamUrl.substring(7)
        } else {
            rawStreamUrl
        }

        val formatStr = data["format"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        val bitrateStr = data["bitrate"]?.jsonPrimitive?.contentOrNull
        val isFlac = streamUrl.contains(".flac", ignoreCase = true) || formatStr.contains("flac")
        val timeSec = data["time"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val sizeStr = data["size"]?.jsonPrimitive?.contentOrNull
        val parsedSizeBytes = parseSizeToBytes(sizeStr)

        val codec = if (isFlac) "flac" else "mp3"
        val bitDepth = if (isFlac) 16 else null

        // Calculate mathematically exact dynamic bitrate from file size and song duration
        val actualKbps = if (parsedSizeBytes != null && timeSec != null && timeSec > 0) {
            val calc = ((parsedSizeBytes * 8L) / (timeSec * 1000L)).toInt()
            if (calc in 300..15000) calc else if (isFlac) 950 else 320
        } else {
            bitrateStr?.toIntOrNull()?.takeIf { it in 100..1800 } ?: if (isFlac) 950 else 320
        }

        val summary = if (isFlac) "Kuwo FLAC Master" else "Kuwo 320kbps MP3"

        TrackLog.d(TAG, "  ↳ Kuwo serving stream: $streamUrl ($summary, ${actualKbps}kbps, ${parsedSizeBytes ?: 0} bytes)")
        val stream = SourceStream(
            url = streamUrl,
            format = StreamFormat(
                codec = codec,
                bitDepth = bitDepth,
                sampleRateHz = 44100,
                kbps = actualKbps,
                source = summary,
            ),
            durationSec = timeSec,
            contentLength = parsedSizeBytes,
        )
        streamCache[trackId] = stream
        stream
    }

    private data class Tuples4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        const val DEFAULT_API_BASE = "https://oiapi.net/api/Kuwo"
        private val searchCache = java.util.concurrent.ConcurrentHashMap<String, List<Song>>()
        private val streamCache = java.util.concurrent.ConcurrentHashMap<String, SourceStream>()

        private fun parseSizeToBytes(sizeStr: String?): Long? {
            if (sizeStr.isNullOrBlank()) return null
            val clean = sizeStr.trim()
            return try {
                when {
                    clean.endsWith("gb", ignoreCase = true) -> {
                        val num = clean.substring(0, clean.length - 2).trim().toDouble()
                        (num * 1024 * 1024 * 1024).toLong()
                    }
                    clean.endsWith("mb", ignoreCase = true) -> {
                        val num = clean.substring(0, clean.length - 2).trim().toDouble()
                        (num * 1024 * 1024).toLong()
                    }
                    clean.endsWith("kb", ignoreCase = true) -> {
                        val num = clean.substring(0, clean.length - 2).trim().toDouble()
                        (num * 1024).toLong()
                    }
                    clean.endsWith("b", ignoreCase = true) -> {
                        clean.substring(0, clean.length - 1).trim().toLong()
                    }
                    else -> clean.toLongOrNull()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
