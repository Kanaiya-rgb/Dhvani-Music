package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Line-synced community LRC via Megalobiz's official Free Public REST API v1.
 * Supports direct searching by title/artist/duration and instant plain .lrc downloading.
 */
object Megalobiz {
    private const val API_BASE = "https://api.megalobiz.com/api/public/v1"

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
        val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }

        val searchUrl = "$API_BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("title", cleanTitle)
            .addQueryParameter("artist", cleanArtist)
            .apply {
                val sec = durationMs / 1000
                if (sec > 0) addQueryParameter("duration", sec.toString())
                addQueryParameter("limit", "10")
            }
            .build()

        var body = lyricsGet(searchUrl.toString())

        // Fallback to free-text query 'q' if title+artist search gave no results
        if (body == null || body.contains("\"results\":[]")) {
            val qUrl = "$API_BASE/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", "$cleanTitle $cleanArtist".trim())
                .apply {
                    val sec = durationMs / 1000
                    if (sec > 0) addQueryParameter("duration", sec.toString())
                    addQueryParameter("limit", "10")
                }
                .build()
            body = lyricsGet(qUrl.toString()) ?: return@withContext null
        }

        val parsed = runCatching { lyricsJson.decodeFromString<SearchResponse>(body) }.getOrNull()
        val results = parsed?.results.orEmpty()
        if (results.isEmpty()) return@withContext null

        // Strictly verify that the result title matches the song to prevent false matches
        val cleanTitleLower = cleanTitle.lowercase()
        val matchingResults = results.filter { r ->
            val rTitle = (r.title ?: "").lowercase()
            rTitle.contains(cleanTitleLower) || cleanTitleLower.contains(rTitle) ||
                LyricsCleaner.isTitleMatch(r.title ?: "", cleanTitle)
        }
        if (matchingResults.isEmpty()) return@withContext null

        // Pick best matching result from verified title candidates
        val best = if (durationMs > 0) {
            val targetSec = durationMs / 1000.0
            matchingResults.minByOrNull { kotlin.math.abs((it.duration ?: 0.0) - targetSec) } ?: matchingResults.first()
        } else {
            matchingResults.first()
        }

        // Fetch raw LRC text
        val lrcUrl = best.lrcUrl ?: "$API_BASE/lrc/${best.id}?format=lrc"
        val rawLrc = lyricsGet(lrcUrl) ?: return@withContext null

        LrcLib.parseLrc(rawLrc).takeIf { lines -> lines.any { it.text.isNotBlank() } }
    }

    @Serializable
    private data class SearchResponse(
        val count: Int? = null,
        val results: List<SearchResult>? = null,
    )

    @Serializable
    private data class SearchResult(
        val id: Long,
        val title: String? = null,
        val artist: String? = null,
        val duration: Double? = null,
        @SerialName("lrc_url") val lrcUrl: String? = null,
    )
}
