package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Where the player gets its lyrics.
 *
 * Sixteen sources, tried in [order] — the user's own priority list in Settings,
 * defaulting to [LyricsSource.entries]:
 *
 *  - [BetterLyrics], [PaxSenix] and [BiniLyrics] — Apple Music TTML,
 *    per-syllable, from three independent hosts so one having a bad day
 *    doesn't cost the timing.
 *  - [LyricsPlus] — the YouLy+ backend; finest timing of the lot, flakiest hosting.
 *  - [SimpMusicLyrics] — keyed on the video id, so it can't fetch the wrong edit.
 *  - [Unison] — contributed rather than licensed; thin, and occasionally the
 *    only place a track exists.
 *  - [LrcLib], [Musixmatch], [KuGou] — line-synced only, but between them
 *    almost always up, and [KuGou] carries a lot that the others don't.
 *
 * Every enabled source is asked *at the same time*, but their answers are
 * taken in [order]: the loop awaits them one at a time in that sequence, so a
 * lower-priority source finishing first never preempts one still pending
 * ahead of it. Asked one after another instead, a miss on each source would
 * cost its own round trip before the next was even tried, and a track with no
 * lyrics anywhere would spend the best part of a minute finding that out with
 * eight of them. Run together, a miss costs whatever the slowest one needed
 * to still be waited on took.
 *
 * A word-timed answer wins outright. Failing that, a line-timed one is taken
 * from the highest-priority source that had it — better a whole line lighting
 * up in sync than the right animation on lyrics that don't exist.
 *
 * ### Matching on the recording
 *
 * Everything above matches a *name*, and a name is ambiguous in a way that
 * matters: a single and its album cut share a title, an artist and very nearly
 * a length, and routinely differ in the words. An ISRC names one recording and
 * settles it. Two sources accept one — [BiniLyrics] and [LyricsPlus] — and
 * [BiniLyrics] is also the one that hands them out, reporting the ISRC of
 * whatever its own search matched.
 *
 * So the recording is settled *before* anybody is asked for words: one small
 * search against [BiniLyrics], whose answer is a name every other source can
 * use. It costs a round trip at the head of the lookup, which is why it is
 * capped at [IDENTIFY_TIMEOUT_MS] and why its result is kept against the video
 * id — a track asked about twice pays for this once. A downloaded or local file
 * can skip it entirely: its own tags name the recording, which is what the
 * [isrc] parameter is for.
 *
 * Only run when the user has [LyricsSource.BINI_LYRICS] enabled. It is a
 * request to a third party like any other, and a source somebody has turned off
 * is a source this app does not contact — not even for something it would only
 * use to help the sources they left on.
 */
object LyricsRepository {

    /** Lyrics, and which source they turned out to come from. */
    data class Result(val source: LyricsSource, val lines: List<LyricLine>, val songwriters: List<String> = emptyList())

    /**
     * [sources] is the user's pick from Settings; anything not in it is not
     * contacted at all. An empty set means no lyrics, which is the same answer
     * as switching the feature off. [order] is tried first-to-last; a source
     * missing from it (an upgrade that added one after the order was last
     * saved) falls in after everything named, in [LyricsSource]'s own order.
     *
     * [prioritizeSyllableSync] decides what happens once *something* has come
     * back: off, the highest-priority source's own answer is taken as-is,
     * word-synced or not — priority is priority, and second-guessing it with
     * more network calls after it has already answered is not what "first"
     * was supposed to mean. On, a merely line-synced answer is kept only as a
     * fallback, and the search keeps going through the rest of [order] for a
     * word-synced one, taking the top-priority source that has one.
     */
    suspend fun lyrics(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        sources: Set<LyricsSource> = LyricsSource.entries.toSet(),
        order: List<LyricsSource> = LyricsSource.entries,
        prioritizeSyllableSync: Boolean = false,
        isrc: String? = null,
    ): LyricsRepository.Result? = coroutineScope {
        if (videoId.isNotBlank()) {
            lyricsCache[videoId]?.let { cached ->
                return@coroutineScope cached.result
            }
        }

        val sequence = order.filter { it in sources } +
            LyricsSource.entries.filter { it in sources && it !in order }

        // Every source but [SimpMusicLyrics] is asked for a name, and
        // YouTube's is not the name anyone catalogued. Cleaned canonically
        // using LyricsCleaner so noise like '(From "Movie")' and '- Topic'
        // are stripped upfront across all providers.
        val searchTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title.forLyricsSearch() }
        val searchArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist.artistForLyricsSearch() }

        // Settled before anyone is asked for words, so every source that can
        // name the recording does. What the caller knows beats what we worked
        // out last time, and both beat asking again.
        val known = isrc?.takeIf { it.isNotBlank() } ?: isrcs[videoId]
        val hit = if (known == null) {
            identify(videoId, searchTitle, searchArtist, durationMs, album, sequence)
        } else {
            null
        }
        val recording = known ?: hit?.isrc?.takeIf { it.isNotBlank() }

        val racing: List<Pair<LyricsSource, Deferred<List<LyricLine>?>>> = sequence.map { source ->
            source to async(Dispatchers.IO) {
                runCatching { fetch(source, videoId, searchTitle, searchArtist, durationMs, album, recording, hit) }
                    .getOrNull()
            }
        }

        try {
            var lineSyncedResult: LyricsRepository.Result? = null
            var estimatedResult: LyricsRepository.Result? = null
            var plainResult: LyricsRepository.Result? = null

            for ((source, job) in racing) {
                val lines = runCatching { job.await() }.getOrNull() ?: continue
                if (lines.isEmpty()) continue

                val hasWordSync = lines.any { it.isWordSynced }
                val hasRealLineSync = lines.any { it.timeMs > 0 && !it.isEstimatedTiming }
                val hasEstimatedTiming = lines.any { it.isEstimatedTiming }

                val res = result(source, lines)

                if (hasWordSync) {
                    if (videoId.isNotBlank()) lyricsCache[videoId] = CacheEntry(res)
                    return@coroutineScope res
                }

                if (hasRealLineSync) {
                    if (!prioritizeSyllableSync) {
                        if (videoId.isNotBlank()) lyricsCache[videoId] = CacheEntry(res)
                        return@coroutineScope res
                    }
                    if (lineSyncedResult == null) {
                        lineSyncedResult = res
                    }
                } else if (hasEstimatedTiming) {
                    if (estimatedResult == null) {
                        estimatedResult = res
                    }
                } else {
                    // Plain / unsynced lyrics fallback only
                    if (plainResult == null) {
                        plainResult = res
                    }
                }
            }

            val best = lineSyncedResult ?: estimatedResult ?: plainResult
            if (best != null && videoId.isNotBlank()) {
                lyricsCache[videoId] = CacheEntry(best)
            }
            best
        } finally {
            // Whoever lost the race is no longer worth waiting on, and
            // coroutineScope will not return while they are still running.
            racing.forEach { it.second.cancel() }
        }
    }

    private suspend fun fetch(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        isrc: String?,
        /** What [identify] already found, where it ran; saves a second search. */
        hit: BiniLyrics.Hit?,
    ): List<LyricLine>? {
        val found = when (source) {
            LyricsSource.BINI_LYRICS ->
                (hit?.let { BiniLyrics.lyricsFor(it) }
                    ?: BiniLyrics.lyrics(title, artist, durationMs, album, isrc))
                    ?.also { remember(videoId, it.isrc) }
                    ?.lines
            LyricsSource.LRC_RED -> LrcRed.lyrics(title, artist, durationMs, isrc, onFoundIsrc = { foundIsrc ->
                remember(videoId, foundIsrc)
            })
            LyricsSource.LYRICS_PLUS -> LyricsPlus.lyrics(title, artist, durationMs, album, isrc)
            LyricsSource.MEGALOBIZ -> Megalobiz.lyrics(title, artist, durationMs)
            LyricsSource.MUSIXMATCH -> Musixmatch.lyrics(title, artist, durationMs)
            LyricsSource.YOUTUBE_TRANSCRIPT -> YouTubeTranscriptLyrics.lyrics(videoId)
            LyricsSource.YOUTUBE_MUSIC -> YouTubeMusicLyrics.lyrics(videoId)
            LyricsSource.LRCLIB -> LrcLib.lyrics(title, artist, durationMs)
            LyricsSource.KUGOU -> KuGou.lyrics(title, artist, durationMs, album)
            LyricsSource.BETTER_LYRICS -> BetterLyrics.lyrics(title, artist, durationMs, album, isrc)
            LyricsSource.BETTER_LYRICS_PORTATO -> BetterLyrics.portato(title, artist, durationMs, album)
            LyricsSource.UNISON -> Unison.lyrics(title, artist, durationMs, album, videoId)
            LyricsSource.GENIUS -> Genius.lyrics(title, artist, videoId, durationMs)
        }
        return found
    }

    /**
     * Whichever source won, its lines get the same last pass: the answering
     * vocal split off the lead so it can be drawn under it. Done here rather
     * than in each parser because most of them write it as a bracket and only
     * [TtmlLyrics] knows it structurally — [withBackgroundVocals] leaves that
     * one's own split alone.
     */
    private fun result(source: LyricsSource, lines: List<LyricLine>, songwriters: List<String> = emptyList()) =
        Result(source, lines.withBackgroundVocals(), songwriters.ifEmpty { TtmlLyrics.songwritersFor(lines) })

    /**
     * Longest the lookup will wait to find out which recording this is.
     *
     * Short on purpose. Knowing the recording makes every match better, but not
     * knowing it only puts things back where they were a release ago, and a
     * lyrics panel sitting empty because one host is having a slow morning is a
     * worse failure than a fuzzy match.
     */
    private const val IDENTIFY_TIMEOUT_MS = 3_500L

    /**
     * The one request made before the race: which recording is this?
     *
     * Skipped entirely when the source that answers it is switched off, and
     * given up on rather than waited out — see [IDENTIFY_TIMEOUT_MS]. What it
     * finds is remembered, so a track pays for this once rather than once per
     * lookup, and the hit is handed back so [BiniLyrics] need not search twice.
     */
    private suspend fun identify(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        sequence: List<LyricsSource>,
    ): BiniLyrics.Hit? {
        val wantsIdentify = LyricsSource.BINI_LYRICS in sequence ||
            LyricsSource.LRC_RED in sequence ||
            sequence.any { it.wordSynced }
        if (!wantsIdentify) return null

        val hit = withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
            runCatching { BiniLyrics.identify(title, artist, durationMs, album) }.getOrNull()
        } ?: withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
            val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
            val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }
            if (cleanTitle != title) {
                runCatching { BiniLyrics.identify(cleanTitle, cleanArtist, durationMs, album) }.getOrNull()
            } else null
        } ?: withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
            val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
            val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }
            runCatching { LrcRed.findHit(cleanTitle, cleanArtist, durationMs) }.getOrNull()?.let { h ->
                BiniLyrics.Hit(
                    trackName = h.title,
                    artistName = h.artist,
                    albumName = h.album,
                    duration = h.duration?.toInt(),
                    isrc = h.isrc,
                    lyricsUrl = h.isrc?.let { LrcRed.documentUrl(it) },
                )
            }
        }
        if (hit == null) return null
        remember(videoId, hit.isrc)
        return hit
    }

    /** How many recordings to keep in hand; see [isrcs]. */
    private const val REMEMBERED = 100

    /**
     * The recording behind a video id, once something has worked it out.
     *
     * Bounded, least-recently-used, and in memory only. This is a shortcut,
     * not a store: losing it costs one fuzzy match, which is what every lookup
     * did before any of this, and keeping it on disk would mean keeping a
     * wrong answer on disk too.
     */
    private val isrcs: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(REMEMBERED, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > REMEMBERED
        },
    )

    private fun remember(videoId: String, isrc: String?) {
        if (isrc.isNullOrBlank() || videoId.isEmpty()) return
        isrcs.put(videoId, isrc)
    }

    private data class CacheEntry(val result: LyricsRepository.Result?)

    /**
     * In-memory LRU cache of resolved lyrics by videoId with negative caching.
     * Guarantees instant retrieval on song transition and eliminates duplicate network hits.
     */
    private val lyricsCache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(REMEMBERED, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>) = size > REMEMBERED
        },
    )

    fun hasCached(videoId: String): Boolean = videoId.isNotBlank() && lyricsCache.containsKey(videoId)

    fun getCached(videoId: String): LyricsRepository.Result? = if (videoId.isBlank()) null else lyricsCache[videoId]?.result

    fun clearCache(videoId: String) {
        if (videoId.isNotBlank()) {
            lyricsCache.remove(videoId)
        }
    }

    /**
     * Asynchronously probes which lyrics providers return results for the active song.
     * Uses a lightweight parallel check with a short timeout.
     */
    suspend fun probeAvailableSources(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        videoId: String = "",
    ): Map<LyricsSource, Boolean> = coroutineScope {
        val searchTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title.forLyricsSearch() }
        val searchArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist.artistForLyricsSearch() }
        val results = ConcurrentHashMap<LyricsSource, Boolean>()

        val activeCachedSource = getCached(videoId)?.source
        if (activeCachedSource != null) {
            results[activeCachedSource] = true
        }

        val candidateSources = LyricsSource.entries

        val knownIsrc = if (videoId.isNotBlank()) isrcs[videoId] else null
        val hit = if (knownIsrc == null && (LyricsSource.BINI_LYRICS in candidateSources || LyricsSource.LRC_RED in candidateSources || candidateSources.any { it.wordSynced })) {
            runCatching {
                identify(videoId, searchTitle, searchArtist, durationMs, album, candidateSources)
            }.getOrNull()
        } else null
        val effectiveIsrc = knownIsrc ?: hit?.isrc?.takeIf { it.isNotBlank() }
        if (effectiveIsrc != null) {
            results[LyricsSource.LRC_RED] = true
            results[LyricsSource.BINI_LYRICS] = true
        }

        candidateSources.map { source ->
            async(Dispatchers.IO) {
                if (source == activeCachedSource) return@async
                if (effectiveIsrc != null && (source == LyricsSource.LRC_RED || source == LyricsSource.BINI_LYRICS)) {
                    results[source] = true
                    return@async
                }
                val found = runCatching {
                    withTimeoutOrNull(8500L) {
                        var lines = fetch(
                            source = source,
                            videoId = videoId,
                            title = searchTitle,
                            artist = searchArtist,
                            durationMs = durationMs,
                            album = album,
                            isrc = effectiveIsrc,
                            hit = if (source == LyricsSource.BINI_LYRICS) hit else null,
                        )
                        if (lines.isNullOrEmpty() && (searchTitle != title || searchArtist != artist)) {
                            lines = fetch(
                                source = source,
                                videoId = videoId,
                                title = title,
                                artist = artist,
                                durationMs = durationMs,
                                album = album,
                                isrc = effectiveIsrc,
                                hit = if (source == LyricsSource.BINI_LYRICS) hit else null,
                            )
                        }
                        if (lines.isNullOrEmpty()) {
                            val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
                            val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }
                            if (cleanTitle != searchTitle && cleanTitle != title) {
                                lines = fetch(
                                    source = source,
                                    videoId = videoId,
                                    title = cleanTitle,
                                    artist = cleanArtist,
                                    durationMs = durationMs,
                                    album = album,
                                    isrc = effectiveIsrc,
                                    hit = if (source == LyricsSource.BINI_LYRICS) hit else null,
                                )
                            }
                        }
                        !lines.isNullOrEmpty()
                    } ?: false
                }.getOrDefault(false)
                results[source] = found
            }
        }.awaitAll()

        results
    }

    /**
     * Allows on-the-fly manual selection of a specific lyrics provider without restarting playback.
     */
    suspend fun fetchLyricsFromExplicitSource(
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
    ): Result? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val searchTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title.forLyricsSearch() }
        val searchArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist.artistForLyricsSearch() }
        val known = isrcs[videoId]
        val hit = if (known == null && (source == LyricsSource.BINI_LYRICS || source == LyricsSource.LRC_RED || source.wordSynced)) {
            runCatching {
                identify(videoId, searchTitle, searchArtist, durationMs, album, listOf(source, LyricsSource.BINI_LYRICS, LyricsSource.LRC_RED))
            }.getOrNull()
        } else null
        val recording = known ?: hit?.isrc?.takeIf { it.isNotBlank() }

        var lines = runCatching {
            fetch(
                source = source,
                videoId = videoId,
                title = searchTitle,
                artist = searchArtist,
                durationMs = durationMs,
                album = album,
                isrc = recording,
                hit = hit,
            )
        }.getOrNull()

        // If cleaned title missed, try with original title & artist
        if (lines.isNullOrEmpty() && (searchTitle != title || searchArtist != artist)) {
            lines = runCatching {
                fetch(
                    source = source,
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    album = album,
                    isrc = recording,
                    hit = hit,
                )
            }.getOrNull()
        }

        // Also try with fully stripped clean title
        if (lines.isNullOrEmpty()) {
            val cleanTitle = LyricsCleaner.cleanTitle(title, artist).ifBlank { title }
            val cleanArtist = LyricsCleaner.cleanArtist(artist).ifBlank { artist }
            if (cleanTitle != searchTitle && cleanTitle != title) {
                lines = runCatching {
                    fetch(
                        source = source,
                        videoId = videoId,
                        title = cleanTitle,
                        artist = cleanArtist,
                        durationMs = durationMs,
                        album = album,
                        isrc = recording,
                        hit = hit,
                    )
                }.getOrNull()
            }
        }

        if (lines.isNullOrEmpty()) return@withContext null
        val res = result(source, lines)
        if (videoId.isNotBlank()) {
            lyricsCache[videoId] = CacheEntry(res)
        }
        res
    }
}
