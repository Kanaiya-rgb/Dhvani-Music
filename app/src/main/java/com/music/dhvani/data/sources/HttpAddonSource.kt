package com.music.dhvani.data.sources

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale

private const val TAG = "HttpAddonSource"

/**
 * A [MusicSource] backed by a BitChord / Eclipse HTTP Addon (HTTP JSON v1 protocol).
 *
 * Communicates with standalone addon servers providing `manifest.json`,
 * `/search?q=...`, and `/stream/...` endpoints (e.g. BlueBit, Eljohnzc, LosslessBridge).
 */
class HttpAddonSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = config.kind
    override val displayName: String get() = config.displayName

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    var addonName: String? = null
        private set
    var addonVersion: String? = null
        private set

    /**
     * Resolves the (baseApiUrl, manifestUrl) pair from whatever URL the user entered.
     */
    fun resolveUrls(): Pair<String, String> {
        val trimmed = config.baseUrl.trim().removeSuffix("/")
        return if (trimmed.endsWith("/manifest.json", ignoreCase = true)) {
            val base = trimmed.substringBeforeLast("/manifest.json")
            base to trimmed
        } else if (trimmed.endsWith("manifest.json", ignoreCase = true)) {
            val base = trimmed.removeSuffix("manifest.json").trimEnd('/')
            base to trimmed
        } else {
            trimmed to "$trimmed/manifest.json"
        }
    }

    // -- Health ------------------------------------------------------------

    override suspend fun health(): SourceHealth = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank()) {
            return@withContext SourceHealth.Rejected("Addon URL is required")
        }
        val (base, manifestUrl) = resolveUrls()

        // Probe manifestUrl first, then base if manifestUrl fails
        val candidates = listOf(manifestUrl, base).distinct()
        for (url in candidates) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                    .get()
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return@use null

                    // If it contains "category:", it's a SpineModule index, not an HTTP addon
                    if (root.keys.any { it.startsWith("category:") }) {
                        return@withContext SourceHealth.Rejected("SpineModule format detected")
                    }

                    // Check if it's a BitChord/Eclipse Addon manifest
                    val hasResources = root.containsKey("resources") ||
                        root.containsKey("addonProtocol") ||
                        (root.containsKey("id") && root.containsKey("name"))
                    if (hasResources) {
                        val name = root["name"]?.jsonPrimitive?.contentOrNull
                            ?: root["id"]?.jsonPrimitive?.contentOrNull
                            ?: "Addon"
                        val version = root["version"]?.jsonPrimitive?.contentOrNull ?: "1.0.0"
                        addonName = name
                        addonVersion = version
                        return@withContext SourceHealth.Ok("$name v$version")
                    }
                }
            } catch (e: Exception) {
                TrackLog.w(TAG, "Health check probe failed on $url: ${e.message}")
            }
        }

        SourceHealth.Rejected("Invalid or unreachable addon manifest")
    }

    // -- Search ------------------------------------------------------------

    companion object {
        private val failureCooldown = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val (base, _) = resolveUrls()
            val now = android.os.SystemClock.elapsedRealtime()
            if ((failureCooldown[base] ?: 0L) > now) {
                TrackLog.d(TAG, "Skipping addon $base (in error cooldown)")
                return@withContext emptyList()
            }

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$base/search?q=$encodedQuery"

            TrackLog.d(TAG, "Searching HTTP Addon: $searchUrl")
            try {
                val timeout = if (waitForAll) 4_000L else 2_500L
                withTimeoutOrNull(timeout) {
                    val request = Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                        .get()
                        .build()

                    Http.client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            TrackLog.w(TAG, "Addon search HTTP ${response.code} from $searchUrl")
                            if (response.code >= 500) {
                                failureCooldown[base] = android.os.SystemClock.elapsedRealtime() + 60_000L
                            }
                            return@use emptyList()
                        }
                        val body = response.body?.string() ?: return@use emptyList()
                        parseSearchResults(body, limit)
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                TrackLog.w(TAG, "Addon search failed for '$query': ${e.message}")
                failureCooldown[base] = android.os.SystemClock.elapsedRealtime() + 30_000L
                emptyList()
            }
        }

    private fun parseSearchResults(body: String, limit: Int): List<Song> {
        return runCatching {
            val root = json.parseToJsonElement(body)
            val items: List<JsonObject> = when (root) {
                is JsonArray -> root.mapNotNull { it as? JsonObject }
                is JsonObject -> {
                    val array = root["results"] as? JsonArray
                        ?: root["tracks"] as? JsonArray
                        ?: root["items"] as? JsonArray
                        ?: root["data"] as? JsonArray
                    array?.mapNotNull { it as? JsonObject } ?: emptyList()
                }
                else -> emptyList()
            }

            items.take(limit).mapNotNull { item ->
                val id = item["id"]?.jsonPrimitive?.contentOrNull
                    ?: item["trackId"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null

                val title = item["title"]?.jsonPrimitive?.contentOrNull
                    ?: item["name"]?.jsonPrimitive?.contentOrNull
                    ?: "Unknown Title"

                val artistsRaw = item["artist"] ?: item["artistNames"] ?: item["artists"]
                val artist = when (artistsRaw) {
                    is JsonArray -> artistsRaw.mapNotNull {
                        when (it) {
                            is JsonObject -> it["name"]?.jsonPrimitive?.contentOrNull
                            else -> it.jsonPrimitive.contentOrNull
                        }
                    }.joinToString(", ")
                    is JsonElement -> artistsRaw.jsonPrimitive.contentOrNull
                    else -> "Unknown Artist"
                }.orEmpty().ifBlank { "Unknown Artist" }

                val album = item["album"]?.jsonPrimitive?.contentOrNull
                    ?: item["albumName"]?.jsonPrimitive?.contentOrNull
                    ?: "Lossless FLAC"

                val artwork = item["artworkURL"]?.jsonPrimitive?.contentOrNull
                    ?: item["artwork"]?.jsonPrimitive?.contentOrNull
                    ?: item["thumbnail"]?.jsonPrimitive?.contentOrNull
                    ?: item["cover"]?.jsonPrimitive?.contentOrNull

                val durNum = item["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val durationSec = if (durNum > 10_000) (durNum / 1000).toInt() else durNum.toInt()
                val m = durationSec / 60
                val s = durationSec % 60
                val durationFormatted = "%d:%02d".format(Locale.ROOT, m, s)

                val audioQuality = item["audioQuality"]?.jsonPrimitive?.contentOrNull
                    ?: item["quality"]?.jsonPrimitive?.contentOrNull
                    ?: ""
                val isHiRes = item["isHiRes"]?.jsonPrimitive?.booleanOrNull == true ||
                    audioQuality.contains("HI_RES", ignoreCase = true) ||
                    audioQuality.contains("HIRES", ignoreCase = true)
                val sourceQuality = if (isHiRes) "Hi-Res FLAC" else "Lossless FLAC"

                Song(
                    videoId = SourceRegistry.trackKey(config.id, id),
                    title = title,
                    artist = artist,
                    thumbnailUrl = artwork,
                    albumName = album,
                    durationText = durationFormatted,
                    sourceQuality = sourceQuality,
                )
            }
        }.getOrElse {
            TrackLog.w(TAG, "Error parsing addon search results: ${it.message}")
            emptyList()
        }
    }

    // -- Stream ------------------------------------------------------------

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? =
        withContext(Dispatchers.IO) {
            val (base, _) = resolveUrls()
            val encodedId = URLEncoder.encode(trackId, "UTF-8")

            // Try /stream/<id> first, fallback to /stream?id=<id>
            val urlsToTry = listOf(
                "$base/stream/$encodedId",
                "$base/stream?id=$encodedId"
            )

            for (url in urlsToTry) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                        .get()
                        .build()

                    Http.client.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val body = response.body?.string() ?: return@use null
                        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return@use null

                        val streamUrl = root["url"]?.jsonPrimitive?.contentOrNull ?: return@use null
                        if (streamUrl.isBlank()) return@use null

                        val codec = root["codec"]?.jsonPrimitive?.contentOrNull
                            ?: root["format"]?.jsonPrimitive?.contentOrNull
                            ?: "flac"
                        val bitDepth = root["bitDepth"]?.jsonPrimitive?.intOrNull ?: 16
                        val sampleRate = root["sampleRate"]?.jsonPrimitive?.intOrNull
                            ?: root["sampleRateHz"]?.jsonPrimitive?.intOrNull
                            ?: 44100
                        val isHiRes = root["isHiRes"]?.jsonPrimitive?.booleanOrNull
                            ?: root["resolvedHiRes"]?.jsonPrimitive?.booleanOrNull
                            ?: (bitDepth > 16 || sampleRate > 48000)
                        val sourceLabel = if (isHiRes) "$displayName Hi-Res" else "$displayName Lossless"

                        val customHeaders = mutableMapOf<String, String>()
                        (root["headers"] as? JsonObject)?.forEach { (k, v) ->
                            v.jsonPrimitive.contentOrNull?.let { customHeaders[k] = it }
                        }

                        TrackLog.d(TAG, "Addon stream resolved: $streamUrl ($codec ${bitDepth}b/${sampleRate}Hz)")
                        return@withContext SourceStream(
                            url = streamUrl,
                            format = StreamFormat(
                                codec = codec.lowercase(Locale.ROOT),
                                bitDepth = bitDepth,
                                sampleRateHz = sampleRateHz(sampleRate),
                                source = sourceLabel,
                            ),
                            headers = customHeaders,
                        )
                    }
                } catch (e: Exception) {
                    TrackLog.w(TAG, "Addon stream fetch error on $url: ${e.message}")
                }
            }

            null
        }

    private fun sampleRateHz(rate: Int): Int = if (rate < 1000) rate * 1000 else rate
}
