package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.math.abs

/**
 * Word-synced and syllable-timed lyrics from QQ Music via OIAPI.
 *
 * Provides high-precision QRC karaoke timings ([8220,1158]First (8220,192)things (8412,470)first...)
 * parsed by [KaraokeLrc], with automatic fallback to line-synced LRC.
 */
object QQMusicLyrics {

    private const val SEARCH_ENDPOINT = "https://oiapi.net/api/QQMusicLyric"
    private const val DURATION_TOLERANCE_SECONDS = 8

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)
        val query = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
        val trackSeconds = (durationMs / 1000).toInt()

        val searchUrl = SEARCH_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("keyword", query)
            .addQueryParameter("limit", "5")
            .build()

        val searchBody = lyricsGet(searchUrl.toString()) ?: return@withContext null
        val root = runCatching { lyricsJson.parseToJsonElement(searchBody).jsonObject }.getOrNull() ?: return@withContext null
        val data = root["data"] as? JsonArray ?: return@withContext null
        if (data.isEmpty()) return@withContext null

        // Score and find the best matching candidate
        data class Candidate(val numericId: String, val mid: String, val score: Int)

        val candidates = data.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val songName = obj["name"]?.jsonPrimitive?.content ?: ""
            val singers = obj["singer"]?.let { el ->
                if (el is JsonArray) el.mapNotNull { it.jsonPrimitive.content }
                else listOf(el.jsonPrimitive.content)
            } ?: emptyList()
            val mid = obj["mid"]?.jsonPrimitive?.content?.trim().orEmpty()
            val id = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
            val dur = obj["duration"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0

            if (mid.isBlank() && id.isBlank()) return@mapNotNull null

            var score = 0
            if (LyricsCleaner.isTitleMatch(songName, cleanTitle)) score += 50
            if (singers.any { LyricsCleaner.isArtistMatch(it, cleanArtist) }) score += 30
            if (trackSeconds > 0 && dur > 0) {
                val diff = abs(dur - trackSeconds)
                if (diff <= 3) score += 20
                else if (diff <= DURATION_TOLERANCE_SECONDS) score += 10
                else if (diff > 20) score -= 30
            }
            Candidate(numericId = id, mid = mid, score = score)
        }.sortedByDescending { it.score }

        val best = candidates.firstOrNull() ?: return@withContext null

        val fetchFormat = { idParam: String, format: String ->
            if (idParam.isNotBlank()) {
                val url = SEARCH_ENDPOINT.toHttpUrl().newBuilder()
                    .addQueryParameter("id", idParam)
                    .addQueryParameter("format", format)
                    .build()
                val body = lyricsGet(url.toString())
                if (!body.isNullOrBlank()) {
                    ProviderLyrics.parse(body)?.takeIf { lines -> lines.any { it.text.isNotBlank() } }
                } else null
            } else null
        }

        // 1. High-precision QRC Syllable/Karaoke sync (numeric id preferred by OIAPI)
        fetchFormat(best.numericId, "1")?.let { return@withContext it }
        fetchFormat(best.mid, "1")?.let { return@withContext it }

        // 2. Line-synced LRC fallback
        fetchFormat(best.numericId, "0")?.let { return@withContext it }
        fetchFormat(best.mid, "0")?.let { return@withContext it }

        null
    }
}

