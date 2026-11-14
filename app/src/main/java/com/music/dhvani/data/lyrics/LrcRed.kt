package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * lrc.red — Apple Music TTML, per-syllable, filed by ISRC.
 *
 * Every document lives at `https://lrc.red/s/{ISRC}.ttml`, so a track whose
 * recording is already known costs one request and cannot come back as the
 * wrong edit.
 *
 * Uses lrc.red's official REST API v1 (`/api/v1?track=&artist=&duration=`)
 * as the primary search engine, falling back to `/api/v1?q=` and
 * free-text `/search.json?q=`.
 */
object LrcRed {

    private const val BASE = "https://lrc.red/"

    /**
     * How far a hit's length may be from the playing track's. YouTube music
     * videos frequently include spoken dialogue, intro cuts, or outro scenes
     * that make the video 20-40 seconds longer than the studio audio cut.
     */
    private const val DURATION_TOLERANCE_SECONDS = 45.0

    /** `CC-XXX-YY-NNNNN` without the dashes — and nothing else goes into a path. */
    private val ISRC = Regex("""[A-Z]{2}[A-Z0-9]{3}\d{7}""")

    fun cleanIsrc(isrc: String): String =
        isrc.trim().replace("-", "").uppercase(Locale.ROOT)

    fun documentUrl(isrc: String): String? =
        cleanIsrc(isrc).takeIf { ISRC.matches(it) }?.let { "${BASE}s/$it.ttml" }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String? = null,
        get: suspend (String) -> String? = { lyricsGet(it) },
        onFoundIsrc: ((String) -> Unit)? = null,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cleanedIsrc = isrc?.let(::cleanIsrc)?.takeIf { ISRC.matches(it) }
        val known = cleanedIsrc?.let { "${BASE}s/$it.ttml" }
        if (known != null) {
            document(known, get)?.let {
                onFoundIsrc?.invoke(cleanedIsrc)
                return@withContext it
            }
        }

        // A known recording lrc.red doesn't have — a local file's tag naming a
        // release it never indexed — still gets a search: the same song is
        // often catalogued under a sibling release's code.
        val hit = search(title, artist, durationMs) ?: return@withContext null
        val hitIsrc = hit.isrc?.let(::cleanIsrc)?.takeIf { ISRC.matches(it) } ?: return@withContext null
        val found = "${BASE}s/$hitIsrc.ttml".takeIf { it != known } ?: return@withContext null
        val lines = document(found, get)
        if (!lines.isNullOrEmpty()) {
            onFoundIsrc?.invoke(hitIsrc)
        }
        lines
    }

    suspend fun findHit(title: String, artist: String, durationMs: Long): Hit? = withContext(Dispatchers.IO) {
        search(title, artist, durationMs)
    }

    private suspend fun document(url: String, get: suspend (String) -> String?): List<LyricLine>? {
        val content = get(url)
        if (!content.isNullOrBlank()) {
            val lines = ProviderLyrics.parse(content)?.takeIf { it.isNotEmpty() }
            if (!lines.isNullOrEmpty()) {
                android.util.Log.d("LrcRed", "Loaded ${lines.size} lines from $url")
                return lines
            }
        }
        // Fallback: if .ttml failed, try .lrc (contains per-word <00:23.77> timings)
        if (url.endsWith(".ttml")) {
            val lrcUrl = url.removeSuffix(".ttml") + ".lrc"
            val lrcContent = get(lrcUrl)
            if (!lrcContent.isNullOrBlank()) {
                val lines = ProviderLyrics.parse(lrcContent)?.takeIf { it.isNotEmpty() }
                if (!lines.isNullOrEmpty()) {
                    android.util.Log.d("LrcRed", "Loaded ${lines.size} lines from fallback $lrcUrl")
                    return lines
                }
            }
        }
        return null
    }

    private fun search(title: String, artist: String, durationMs: Long): Hit? {
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
        val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }
        val seconds = if (durationMs > 0) (durationMs / 1000).toInt() else 0

        // 1. Primary choice: lrc.red fast free-text search (/search.json?q=) - native frontend endpoint (~480ms)
        val freeTextHit = searchFreeText(cleanTitle, cleanArtist, durationMs)
            ?: searchFreeText(cleanTitle, "", durationMs)
            ?: if (cleanTitle != title) searchFreeText(title, "", durationMs) else null
        if (freeTextHit != null) {
            android.util.Log.d("LrcRed", "Found hit via search.json: ${freeTextHit.isrc} - ${freeTextHit.title}")
            return freeTextHit
        }

        // 2. Secondary choice: lrc.red official REST API v1 (?track=&artist=&duration=)
        val apiHit = searchApiV1(cleanTitle, cleanArtist, seconds)
            ?: if (cleanTitle != title || cleanArtist != artist) searchApiV1(title, artist, seconds) else null
        if (apiHit != null) {
            android.util.Log.d("LrcRed", "Found hit via api/v1: ${apiHit.isrc} - ${apiHit.title}")
            return apiHit
        }
        return null
    }

    private fun searchApiV1(title: String, artist: String, seconds: Int): Hit? {
        val builder = "${BASE}api/v1".toHttpUrl().newBuilder()
            .addQueryParameter("track", title)
        if (artist.isNotBlank()) builder.addQueryParameter("artist", artist)
        if (seconds > 0) builder.addQueryParameter("duration", seconds.toString())

        var body = lyricsGet(builder.build().toString())
        var resp = body?.let { runCatching { lyricsJson.decodeFromString<ApiV1Response>(it) }.getOrNull() }
        var results = resp?.results.orEmpty()

        // Fallback 1: Query by track title only (without artist restriction)
        if (results.isEmpty() && artist.isNotBlank() && title.isNotBlank()) {
            val titleOnlyUrl = "${BASE}api/v1".toHttpUrl().newBuilder()
                .addQueryParameter("track", title)
                .apply { if (seconds > 0) addQueryParameter("duration", seconds.toString()) }
                .build()
            body = lyricsGet(titleOnlyUrl.toString())
            resp = body?.let { runCatching { lyricsJson.decodeFromString<ApiV1Response>(it) }.getOrNull() }
            results = resp?.results.orEmpty()
        }

        // Fallback 2: Free-text search
        if (results.isEmpty() && title.isNotBlank()) {
            val qUrl = "${BASE}api/v1".toHttpUrl().newBuilder()
                .addQueryParameter("q", title)
                .build()
            body = lyricsGet(qUrl.toString())
            resp = body?.let { runCatching { lyricsJson.decodeFromString<ApiV1Response>(it) }.getOrNull() }
            results = resp?.results.orEmpty()
        }

        if (results.isEmpty()) return null

        val valid = results.filter { it.isrc?.let(::cleanIsrc)?.let(ISRC::matches) == true }
        if (valid.isEmpty()) return null

        val bestResult = if (seconds > 0) {
            valid.filter { it.duration == null || abs(it.duration - seconds) <= 45 }
                .minByOrNull { it.duration?.let { d -> abs(d - seconds) } ?: 999 }
                ?: valid.first()
        } else {
            valid.first()
        }

        return Hit(
            isrc = bestResult.isrc,
            title = bestResult.trackName,
            artist = bestResult.artistName,
            album = bestResult.albumName,
            duration = bestResult.duration?.toDouble(),
        )
    }

    private fun searchFreeText(title: String, artist: String, durationMs: Long): Hit? {
        val q = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()
        val url = "${BASE}search.json".toHttpUrl().newBuilder()
            .addQueryParameter("q", q)
            .build()
        val body = lyricsGet(url.toString()) ?: return null
        val hits = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()?.hits
            ?: return null
        return best(hits, title, artist, durationMs)
    }

    /**
     * The hit that is this recording, or null if none of them is sure to be.
     */
    fun best(hits: List<Hit>, title: String, artist: String, durationMs: Long): Hit? {
        val wantedTitle = coreOf(title)
        val wantedVersion = versionOf(title)
        val wantedArtists = artistsOf(artist)
        val seconds = durationMs / 1000.0

        fun distance(hit: Hit): Double =
            if (durationMs > 0 && hit.duration != null) abs(hit.duration - seconds) else 0.0

        return hits
            .filter { hit ->
                val name = hit.title ?: return@filter false
                val dist = distance(hit)
                val hitArtists = artistsOf(hit.artist.orEmpty())
                val artistsMatch = wantedArtists.isEmpty() ||
                    hitArtists.any { it in wantedArtists } ||
                    wantedArtists.any { it in hitArtists }

                hit.isrc?.let(::cleanIsrc)?.let(ISRC::matches) == true &&
                    coreOf(name) == wantedTitle &&
                    versionOf(name) == wantedVersion &&
                    (artistsMatch || dist <= 8.0) &&
                    dist <= DURATION_TOLERANCE_SECONDS
            }
            .minByOrNull(::distance)
    }

    /** "Song (Live) - 2011 Remaster" → "song". */
    private fun coreOf(title: String): String =
        normalized(title.replace(BRACKETED, " ").substringBefore(" - "))
            .ifEmpty { normalized(title) }

    /** The version words in the parts [coreOf] throws away. */
    private fun versionOf(title: String): Set<String> {
        val extras = BRACKETED.findAll(title).joinToString(" ") { it.value } +
            " " + title.substringAfter(" - ", "")
        return normalized(extras).split(' ').filter { it in VERSION_WORDS }.toSet()
    }

    private fun artistsOf(artist: String): Set<String> =
        artist.split(ARTIST_SEPARATORS).map(::normalized).filter { it.isNotEmpty() }.toSet()

    /** Lower case, accents off ("ROSÉ" is "rose"), and only letters and digits. */
    private fun normalized(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()

    private val BRACKETED = Regex("""[(\[][^)\]]*[)\]]""")
    private val COMBINING_MARKS = Regex("""\p{Mn}+""")
    private val WHITESPACE = Regex("""\s+""")
    private val ARTIST_SEPARATORS = Regex(
        """\s*(?:,|&|;|/|\s+and\s+|\s+x\s+|\s+with\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""",
        RegexOption.IGNORE_CASE,
    )

    /** Bracket words that make it a different recording, not a different label. */
    private val VERSION_WORDS = setOf(
        "live", "remix", "remixed", "mix", "acoustic", "unplugged", "instrumental",
        "karaoke", "cappella", "acapella", "demo", "edit", "version", "cover",
        "sped", "slowed", "reverb", "nightcore", "lofi", "orchestral", "extended",
    )

    @Serializable
    data class Response(
        val hits: List<Hit>? = null,
        /** Paging cursor. The first page is all a match is looked for in. */
        val next: String? = null,
    )

    @Serializable
    internal data class ApiV1Response(
        val results: List<ApiV1Result>? = null,
    )

    @Serializable
    internal data class ApiV1Result(
        @SerialName("track_name") val trackName: String? = null,
        @SerialName("artist_name") val artistName: String? = null,
        @SerialName("album_name") val albumName: String? = null,
        val duration: Int? = null,
        val isrc: String? = null,
        val lyricsUrl: String? = null,
        @SerialName("timing_type") val timingType: String? = null,
    )

    @Serializable
    data class Hit(
        val isrc: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val year: Int? = null,
        /** Seconds, fractional. */
        val duration: Double? = null,
    )
}
