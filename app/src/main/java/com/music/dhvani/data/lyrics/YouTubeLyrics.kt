package com.music.dhvani.data.lyrics

import com.music.dhvani.data.YtMusicRepository
import com.music.dhvani.data.innertube.Innertube
import com.music.dhvani.data.model.SearchFilter
import com.music.dhvani.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Lyrics directly from YouTube Music's official catalogue.
 *
 * YouTube Music licenses official lyrics from LyricFind and Musixmatch across
 * hundreds of thousands of songs (especially Bollywood, Indian regional tracks,
 * Western pop, and international releases) that community LRC databases lack.
 *
 * If the currently playing track is a music video or user upload whose lyrics
 * tab is unselectable, this automatically searches for the corresponding official
 * audio track on YouTube Music and retrieves its lyrics instead.
 */
object YouTubeLyrics {

    suspend fun lyrics(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)

        // 1. Try currently playing videoId directly
        if (videoId.isNotBlank()) {
            val directBrowseId = runCatching {
                val nextResp = Innertube.next(videoId)
                extractLyricsBrowseId(nextResp)
            }.getOrNull()

            if (!directBrowseId.isNullOrBlank()) {
                fetchLyricsFromBrowse(directBrowseId, durationMs)?.let { return@withContext it }
            }
        }

        // 2. If missing or unselectable (music video / user upload), search official audio track
        val query = listOf(cleanTitle, cleanArtist).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isNotBlank()) {
            val searchResults = runCatching {
                YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
            }.getOrNull().orEmpty()

            val topSong = searchResults.filterIsInstance<SearchResult.Track>()
                .firstOrNull { candidate ->
                    candidate.song.videoId != videoId &&
                        LyricsCleaner.isTitleMatch(candidate.song.title, cleanTitle)
                }

            if (topSong != null) {
                val altBrowseId = runCatching {
                    val altNext = Innertube.next(topSong.song.videoId)
                    extractLyricsBrowseId(altNext)
                }.getOrNull()

                if (!altBrowseId.isNullOrBlank()) {
                    fetchLyricsFromBrowse(altBrowseId, durationMs)?.let { return@withContext it }
                }
            }
        }

        null
    }

    private suspend fun fetchLyricsFromBrowse(browseId: String, durationMs: Long): List<LyricLine>? {
        val browseResp = runCatching { Innertube.browse(browseId) }.getOrNull() ?: return null
        val timedData = findTimedLyricsData(browseResp)
        if (timedData != null && timedData.isNotEmpty()) {
            val lines = parseTimedLyricsData(timedData)
            if (lines.isNotEmpty()) return lines
        }
        return null
    }

    private fun findTimedLyricsData(element: JsonElement?): JsonArray? {
        if (element is JsonObject) {
            val direct = element["timedLyricsData"] as? JsonArray
            if (!direct.isNullOrEmpty()) return direct
            for ((_, child) in element) {
                val found = findTimedLyricsData(child)
                if (found != null) return found
            }
        } else if (element is JsonArray) {
            for (child in element) {
                val found = findTimedLyricsData(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun extractLyricsBrowseId(nextResponse: JsonObject): String? {
        val tabs = nextResponse.o("contents")
            ?.o("singleColumnMusicWatchNextResultsRenderer")
            ?.o("tabbedRenderer")
            ?.o("watchNextTabbedResultsRenderer")
            ?.a("tabs") ?: return null

        for (element in tabs) {
            val tabRenderer = element.o("tabRenderer") ?: continue
            val title = tabRenderer.o("title")?.firstRunText()
                ?: tabRenderer.s("title").orEmpty()
            val pageType = tabRenderer.o("endpoint")
                ?.o("browseEndpoint")
                ?.o("browseEndpointContextSupportedConfigs")
                ?.o("browseEndpointContextMusicConfig")
                ?.s("pageType")

            val isLyricsTab = pageType == "MUSIC_PAGE_TYPE_TRACK_LYRICS" ||
                title.contains("lyrics", ignoreCase = true)
            val isUnselectable = tabRenderer.s("unselectable")?.toBooleanStrictOrNull() == true

            if (isLyricsTab && !isUnselectable) {
                val browseId = tabRenderer.o("endpoint")?.o("browseEndpoint")?.s("browseId")
                if (!browseId.isNullOrBlank()) return browseId
            }
        }
        return null
    }

    private fun parseTimedLyricsData(timedData: JsonArray): List<LyricLine> {
        return timedData.mapNotNull { item ->
            val text = item.s("lyricLine")?.trim()
                ?: item.o("lyricLine")?.runs()?.trim()
                ?: item.firstRunText()?.trim()
                ?: return@mapNotNull null
            val cue = item.o("cueRange")
            val startMs = cue?.s("startTimeMilliseconds")?.toLongOrNull() ?: return@mapNotNull null
            val endMs = cue.s("endTimeMilliseconds")?.toLongOrNull()
            LyricLine(
                timeMs = startMs,
                text = text,
                sungUntilMs = endMs,
                isEstimatedTiming = false,
            )
        }.sortedBy { it.timeMs }
    }

    // JSON parsing helpers
    private fun JsonElement?.o(key: String): JsonObject? =
        (this as? JsonObject)?.get(key) as? JsonObject

    private fun JsonElement?.a(key: String): JsonArray? =
        (this as? JsonObject)?.get(key) as? JsonArray

    private fun JsonElement?.s(key: String): String? =
        ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonElement?.firstRunText(): String? =
        this.a("runs")?.firstOrNull().s("text")

    private fun JsonElement?.runs(): String =
        this.a("runs")?.joinToString("") { it.s("text").orEmpty() }.orEmpty()
}
