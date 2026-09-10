package com.music.flux.data.lyrics

import com.music.flux.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import kotlin.math.abs

/**
 * Lyrics from LRCLIB — a free, key-less, community lyrics database.
 *
 * Two calls: an exact `get` keyed on artist + title + duration, and a fuzzy
 * `search` when that misses (YouTube Music titles carry "(From ...)" and
 * "| Official Video" noise that the exact endpoint won't match). Results are
 * line-synced `[mm:ss.xx]` LRC — LRCLIB has no word-level timing, so
 * highlighting is per line.
 */
object LrcLib {

    private const val BASE = "https://lrclib.net/api"
    private const val AGENT = "Flux Music"

    private val json = Json { ignoreUnknownKeys = true }

    private data class Hit(val synced: String?, val plain: String?)

    /** Synced or plain lyrics for a track, or null when nothing usable is published. */
    suspend fun lyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? =
        withContext(Dispatchers.IO) {
            val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
            val cleanArtist = LyricsCleaner.cleanArtist(artist)
            val allArtists = LyricsCleaner.allArtists(artist)
            val seconds = (durationMs / 1000).toInt()

            var bestHit: Hit? = null

            // 1. Try exact match with primary clean artist
            bestHit = runCatching { exactMatch(cleanTitle, cleanArtist, seconds) }.getOrNull()

            // 2. Try exact match with any other credited artists
            if (bestHit?.synced.isNullOrBlank() && allArtists.size > 1) {
                for (otherArtist in allArtists.drop(1)) {
                    val candidate = runCatching { exactMatch(cleanTitle, otherArtist, seconds) }.getOrNull()
                    if (!candidate?.synced.isNullOrBlank()) {
                        bestHit = candidate
                        break
                    } else if (bestHit == null && !candidate?.plain.isNullOrBlank()) {
                        bestHit = candidate
                    }
                }
            }

            // 3. Search with cleanTitle and cleanArtist, strictly verifying candidate
            if (bestHit?.synced.isNullOrBlank()) {
                val candidate = runCatching { bestSearchHit(cleanTitle, cleanArtist, allArtists, seconds) }.getOrNull()
                if (!candidate?.synced.isNullOrBlank()) {
                    bestHit = candidate
                } else if (bestHit == null && !candidate?.plain.isNullOrBlank()) {
                    bestHit = candidate
                }
            }

            // 4. If still missing, search with cleanTitle alone, verifying title & duration
            if (bestHit?.synced.isNullOrBlank()) {
                val candidate = runCatching { searchByTitle(cleanTitle, allArtists, seconds) }.getOrNull()
                if (!candidate?.synced.isNullOrBlank()) {
                    bestHit = candidate
                } else if (bestHit == null && !candidate?.plain.isNullOrBlank()) {
                    bestHit = candidate
                }
            }

            // Only return synced lyrics (proper sync)
            bestHit?.synced?.takeIf { it.isNotBlank() }?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
        }

    private fun exactMatch(title: String, artist: String, seconds: Int): Hit? {
        val url = "$BASE/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("duration", seconds.toString())
            .build()
        val body = get(url.toString()) ?: return null
        val obj = json.parseToJsonElement(body) as? JsonObject ?: return null
        val synced = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull
        val plain = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull
        if (synced.isNullOrBlank() && plain.isNullOrBlank()) return null
        return Hit(synced, plain)
    }

    /**
     * Fuzzy fallback. Must strictly match the title and be close in length.
     */
    private fun bestSearchHit(title: String, artist: String, allArtists: List<String>, seconds: Int): Hit? {
        val url = "$BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .build()
        val body = get(url.toString()) ?: return null
        val hits = json.parseToJsonElement(body) as? JsonArray ?: return null
        val candidates = hits.mapNotNull { it as? JsonObject }
            .filter { hit ->
                val trackName = hit["trackName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val hasAnyLyrics = hit["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true ||
                    hit["plainLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
                val d = hit["duration"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0
                hasAnyLyrics && LyricsCleaner.isTitleMatch(trackName, title) && LyricsCleaner.isDurationMatch(d, seconds, 15)
            }

        val best = candidates.filter { hit -> hit["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true }
            .minByOrNull { hit ->
                val d = hit["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                abs(d - seconds)
            } ?: candidates.minByOrNull { hit ->
                val d = hit["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                abs(d - seconds)
            } ?: return null

        return Hit(
            synced = best["syncedLyrics"]?.jsonPrimitive?.contentOrNull,
            plain = best["plainLyrics"]?.jsonPrimitive?.contentOrNull,
        )
    }

    /**
     * Title-only search fallback for tracks with multiple or alternate artist spellings.
     */
    private fun searchByTitle(title: String, allArtists: List<String>, seconds: Int): Hit? {
        val url = "$BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .build()
        val body = get(url.toString()) ?: return null
        val hits = json.parseToJsonElement(body) as? JsonArray ?: return null
        val candidates = hits.mapNotNull { it as? JsonObject }
            .filter { hit ->
                val trackName = hit["trackName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val hasAnyLyrics = hit["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true ||
                    hit["plainLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
                val d = hit["duration"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0
                hasAnyLyrics && LyricsCleaner.isTitleMatch(trackName, title) && (seconds <= 0 || abs(d - seconds) <= 18)
            }

        fun scoreHit(hit: JsonObject): Double {
            val artistName = hit["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val d = hit["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            var score = 50.0
            if (hit["syncedLyrics"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true) score += 40.0
            if (allArtists.any { LyricsCleaner.isArtistMatch(artistName, it) }) score += 50.0
            if (seconds > 0) score -= abs(d - seconds) * 3.0
            return score
        }

        val best = candidates.maxByOrNull { scoreHit(it) } ?: return null
        return Hit(
            synced = best["syncedLyrics"]?.jsonPrimitive?.contentOrNull,
            plain = best["plainLyrics"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun get(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", AGENT).build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    /**
     * `[mm:ss.xx] words`. Metadata tags carry no timestamp and fall out on
     * their own. A stamp with no words marks an instrumental break; those are
     * kept, but only where the silence is long enough to be worth showing —
     * otherwise the line would blink out between two sung phrases. A stamp
     * with nothing after it closes the final line, so it always survives.
     */
    internal fun parseLrc(lrc: String): List<LyricLine> {
        val all = lrc.lineSequence().mapNotNull { line ->
            val match = STAMP.find(line) ?: return@mapNotNull null
            val (minutes, seconds, fraction) = match.destructured
            // Two digits mean centiseconds, three mean milliseconds.
            val fractionMs = when (fraction.length) {
                2 -> fraction.toLong() * 10
                3 -> fraction.toLong()
                else -> 0L
            }
            val body = line.substring(match.range.last + 1)
            LyricLine(
                timeMs = minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fractionMs,
                // Stripped rather than rebuilt from the runs below: the spacing
                // and punctuation between two words belong to the line, and
                // re-joining the words with single spaces would quietly rewrite
                // a line that never had them.
                text = body.replace(WORD_STAMP, "").trim(),
                words = parseWordRuns(body),
            )
        }.sortedBy { it.timeMs }.toList()

        val kept = all.filterIndexed { index, line ->
            if (!line.isGap) return@filterIndexed true
            // A trailing stamp closes off the last line — that's the outro.
            val next = all.getOrNull(index + 1) ?: return@filterIndexed true
            next.timeMs - line.timeMs >= MIN_GAP_MS
        }

        // Nothing stands for the intro — LRC files start at the first sung
        // word — so give the run-up its own break when it's long enough.
        val first = kept.firstOrNull() ?: return kept
        return if (!first.isGap && first.timeMs >= MIN_GAP_MS) {
            listOf(LyricLine(0L, "")) + kept
        } else {
            kept
        }
    }

    /**
     * YouTube Music titles are noisy — "(From "Raees")", "| Official Video",
     * "(Lyrical)" — and LRCLIB matches on the plain song name.
     */
    private fun String.clean(): String = this
        .replace(NOISE, " ")
        .substringBefore(" | ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { this }

    /**
     * The `<mm:ss.xx>` runs of an "enhanced" A2 line, as words.
     *
     * Each run ends where the next one starts, which is why a line written by
     * [toEnhancedLrc] closes with a bare stamp: that last one names no word, it
     * just states where the previous one stopped. A run with no text is
     * therefore a terminator rather than a word, here and in the files other
     * A2 writers produce.
     *
     * Empty for a plain line, which is what keeps [LyricLine.isWordSynced]
     * honest — a line-synced source stays line-synced through this.
     */
    private fun parseWordRuns(body: String): List<LyricWord> {
        val marks = WORD_STAMP.findAll(body).toList()
        if (marks.isEmpty()) return emptyList()
        val runs = marks.mapIndexed { index, mark ->
            val until = marks.getOrNull(index + 1)?.range?.first ?: body.length
            msOf(mark) to body.substring(mark.range.last + 1, until)
        }
        return runs.mapIndexedNotNull { index, (startMs, text) ->
            if (text.isBlank()) return@mapIndexedNotNull null
            // The next run's stamp is this word's end — including when that run
            // is the closing terminator, which is the only thing that gives the
            // last word of a line an end at all.
            val endMs = runs.getOrNull(index + 1)?.first ?: startMs
            LyricWord(startMs = startMs, endMs = maxOf(endMs, startMs), text = text.trim())
        }
    }

    private fun msOf(mark: MatchResult): Long {
        val (minutes, seconds, fraction) = mark.destructured
        val fractionMs = when (fraction.length) {
            2 -> fraction.toLong() * 10
            3 -> fraction.toLong()
            else -> 0L
        }
        return minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fractionMs
    }

    private val STAMP = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})]""")
    private val WORD_STAMP = Regex("""<(\d{1,3}):(\d{2})[.:](\d{2,3})>""")
    private val NOISE = Regex(
        """\((?:from|feat\.?|official|lyrical|video|audio|remix)[^)]*\)|\[[^]]*]|""" +
            """\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b""",
        RegexOption.IGNORE_CASE,
    )
}
