package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale

/**
 * Apple Music TTML again, from a third host - and the only one here that will
 * answer to a recording rather than to a name.
 */
object BiniLyrics {

    private const val BASE = "https://lrc.red/api/v1"
    private const val FALLBACK_BASE = "https://lyrics-api.binimum.org/"

    /** Lyrics, and the ISRC of the recording they were matched to. */
    data class Match(val isrc: String?, val lines: List<LyricLine>, val songwriters: List<String> = emptyList())

    /**
     * Which recording this is, without fetching its words.
     */
    internal suspend fun identify(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        isrc: String? = null,
    ): Hit? = withContext(Dispatchers.IO) {
        val cleanIsrc = isrc?.trim()?.replace("-", "")?.uppercase(Locale.ROOT)
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
        val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }

        fun queryEndpoint(base: String, track: String, art: String, isr: String?): List<Hit>? {
            val url = base.toHttpUrl().newBuilder()
                .apply {
                    if (!isr.isNullOrBlank()) {
                        addQueryParameter("isrc", isr)
                    } else {
                        addQueryParameter("track", track)
                        if (art.isNotBlank()) addQueryParameter("artist", art)
                        val cleanAlb = album?.let { LyricsCleaner.cleanAlbum(it) }?.takeIf { it.isNotBlank() && !it.equals(track, ignoreCase = true) }
                        if (!cleanAlb.isNullOrBlank()) addQueryParameter("album", cleanAlb)
                        val seconds = durationMs / 1000
                        if (seconds > 0) addQueryParameter("duration", seconds.toString())
                    }
                }
                .build()

            val body = lyricsGet(url.toString())
            var response = body?.let { runCatching { lyricsJson.decodeFromString<Response>(it) }.getOrNull() }
            var results = response?.results.orEmpty()

            // Fallback 1: If album caused a miss, retry with track & artist (without album restriction)
            if (results.isEmpty() && isr.isNullOrBlank() && !album.isNullOrBlank() && art.isNotBlank()) {
                val noAlbumUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("track", track)
                    .addQueryParameter("artist", art)
                    .apply {
                        val seconds = durationMs / 1000
                        if (seconds > 0) addQueryParameter("duration", seconds.toString())
                    }
                    .build()
                val aBody = lyricsGet(noAlbumUrl.toString())
                response = aBody?.let { runCatching { lyricsJson.decodeFromString<Response>(it) }.getOrNull() }
                results = response?.results.orEmpty()
            }

            if (results.isEmpty() && isr.isNullOrBlank() && art.isNotBlank() && track.isNotBlank()) {
                val titleOnlyUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("track", track)
                    .apply {
                        val seconds = durationMs / 1000
                        if (seconds > 0) addQueryParameter("duration", seconds.toString())
                    }
                    .build()
                val tBody = lyricsGet(titleOnlyUrl.toString())
                response = tBody?.let { runCatching { lyricsJson.decodeFromString<Response>(it) }.getOrNull() }
                results = response?.results.orEmpty()
            }

            if (results.isEmpty() && isr.isNullOrBlank() && track.isNotBlank()) {
                val qUrl = base.toHttpUrl().newBuilder()
                    .addQueryParameter("q", track)
                    .build()
                val qBody = lyricsGet(qUrl.toString())
                response = qBody?.let { runCatching { lyricsJson.decodeFromString<Response>(it) }.getOrNull() }
                results = response?.results.orEmpty()
            }
            return results.takeIf { it.isNotEmpty() }
        }

        val results = queryEndpoint(BASE, cleanTitle, cleanArtist, cleanIsrc)
            ?: (if (cleanTitle != title || cleanArtist != artist) queryEndpoint(BASE, title, artist, cleanIsrc) else null)
            ?: queryEndpoint(FALLBACK_BASE, cleanTitle, cleanArtist, cleanIsrc)
            ?: return@withContext null

        val seconds = (durationMs / 1000).toInt()
        if (seconds <= 0) return@withContext results.firstOrNull()
        results.filter { hit -> hit.duration == null || kotlin.math.abs(hit.duration - seconds) <= 45 }
            .minByOrNull { hit -> hit.duration?.let { kotlin.math.abs(it - seconds) } ?: 999 }
            ?: results.firstOrNull()
    }

    /** The document a search already found, fetched and parsed. */
    internal suspend fun lyricsFor(hit: Hit): Match? = withContext(Dispatchers.IO) {
        val document = hit.lyricsUrl?.takeIf { it.isNotBlank() }
            ?: hit.isrc?.let { "https://lrc.red/s/${it.replace("-", "")}.ttml" }
            ?: return@withContext null
        var content = lyricsGet(document)
        if (content.isNullOrBlank() && document.endsWith(".ttml")) {
            val lrcUrl = document.removeSuffix(".ttml") + ".lrc"
            content = lyricsGet(lrcUrl)
        }
        if (content.isNullOrBlank()) return@withContext null
        val lines = ProviderLyrics.parse(content)?.takeIf { it.isNotEmpty() } ?: return@withContext null
        Match(hit.isrc?.takeIf { it.isNotBlank() }, lines, emptyList())
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        isrc: String? = null,
    ): Match? = identify(title, artist, durationMs, album, isrc)?.let { lyricsFor(it) }

    @Serializable
    internal data class Response(
        val total: Int? = null,
        val source: String? = null,
        val results: List<Hit>? = null,
    )

    @Serializable
    internal data class Hit(
        @SerialName("track_name") val trackName: String? = null,
        @SerialName("artist_name") val artistName: String? = null,
        @SerialName("album_name") val albumName: String? = null,
        val duration: Int? = null,
        val isrc: String? = null,
        @SerialName("timing_type") val timingType: String? = null,
        val lyricsUrl: String? = null,
    )
}
