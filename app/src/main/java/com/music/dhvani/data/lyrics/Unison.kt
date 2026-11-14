package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * A community-submitted lyrics database from BetterLyrics.
 *
 * Querying supports:
 * 1. Exact YouTube videoId (?v=...) - instant 1-call exact match for playing tracks.
 * 2. Song title & artist (?song=...&artist=...) without rigid duration constraints.
 *
 * Production host: https://unison.betterlyrics.org/lyrics (fallback: https://unison.boidu.dev/lyrics).
 */
object Unison {

    private const val BASE = "https://unison.betterlyrics.org/lyrics"
    private const val FALLBACK_BASE = "https://unison.boidu.dev/lyrics"

    private val YOUTUBE_ID = Regex("""[A-Za-z0-9_-]{11}""")

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L,
        album: String? = null,
        videoId: String? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        // 1. Try exact video ID match first if playing a YouTube track
        if (!videoId.isNullOrBlank() && YOUTUBE_ID.matches(videoId)) {
            val byVideo = fetch("$BASE?v=$videoId") ?: fetch("$FALLBACK_BASE?v=$videoId")
            if (byVideo != null) return@withContext byVideo
        }

        // 2. Query by cleaned song + artist name without strict duration filter
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
        val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }

        val searchUrl = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("song", cleanTitle)
            .addQueryParameter("artist", cleanArtist)
            .build()

        fetch(searchUrl.toString()) ?: run {
            val fallbackUrl = FALLBACK_BASE.toHttpUrl().newBuilder()
                .addQueryParameter("song", cleanTitle)
                .addQueryParameter("artist", cleanArtist)
                .build()
            fetch(fallbackUrl.toString())
        }
    }

    private fun fetch(url: String): List<LyricLine>? {
        val body = lyricsGet(url) ?: return null
        val response = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()
            ?: return null
        if (response.success != true) return null
        return linesOf(response.data ?: return null)
    }

    /** The three shapes, told apart by what the entry says it is. */
    internal fun linesOf(data: Entry): List<LyricLine>? {
        val text = data.lyrics?.takeIf { it.isNotBlank() } ?: return null
        val lines = when {
            data.format.equals(TTML, ignoreCase = true) || text.contains("<tt", ignoreCase = true) ->
                TtmlLyrics.parse(text)
            data.syncType.equals(PLAIN, ignoreCase = true) -> plain(text)
            // Word stamps first: the format field says only "lrc", and an
            // enhanced file is still one. [EnhancedLrc] comes back empty when
            // there are no word stamps in it, which is the signal to fall
            // through rather than a failure.
            else -> EnhancedLrc.parse(text).takeIf { it.isNotEmpty() } ?: LrcLib.parseLrc(text)
        }
        return lines.takeIf { it.isNotEmpty() }
    }

    /**
     * Lyrics with no timing: one line each, all stamped zero, which is how
     * every unsynced source here states the same thing — the panel reads it as
     * words to be scrolled by hand rather than followed.
     */
    private fun plain(text: String): List<LyricLine> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { LyricLine(timeMs = 0, text = it) }
        .toList()

    private const val TTML = "ttml"
    private const val PLAIN = "plain"

    @Serializable
    internal data class Response(
        val success: Boolean? = null,
        val data: Entry? = null,
    )

    @Serializable
    internal data class Entry(
        val song: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        /** `ttml` or `lrc`. */
        val format: String? = null,
        /** `wordsync`, `linesync` or `plain`. */
        val syncType: String? = null,
        val confidence: String? = null,
        val voteCount: Int? = null,
    )
}
