package com.music.dhvani.data.lyrics

import com.music.dhvani.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Web scraper for Genius.com lyrics.
 *
 * Used strictly as a last resort fallback when none of the time-synced providers
 * (LRCLIB, Musixmatch, BetterLyrics, KuGou, etc.) have lyrics for a track.
 *
 * Genius does not offer timestamps, but carries an immense catalogue. The scraper:
 *  1. Searches Genius's open multi-search endpoint (no API key required).
 *  2. Matches the best candidate song and grabs its webpage URL.
 *  3. Fetches the page HTML and extracts lyrics from `data-lyrics-container` tags via Jsoup.
 *  4. Cleans out Genius metadata (headers, translation links, "You might also like", "Embed").
 *  5. Retains section headers (e.g. "[Verse 1]", "[Chorus]") for beautiful formatted display.
 */
object Genius {

    private const val BROWSER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    private val json by lazy { Json { ignoreUnknownKeys = true; isLenient = true } }

    private val httpClient by lazy {
        Http.client.newBuilder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        videoId: String? = null,
        durationMs: Long = 0L,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val raw = runCatching {
            scrapeLyrics(title, artist)
        }.getOrNull() ?: return@withContext null

        if (videoId.isNullOrBlank()) return@withContext raw

        // Jugaad 1: Fetch YouTube Transcript cues to auto-sync Genius lyrics with playback
        val cues = runCatching { YouTubeTranscriptLyrics.lyrics(videoId) }.getOrNull()
        alignLyrics(raw, cues, durationMs)
    }

    private fun scrapeLyrics(title: String, artist: String): List<LyricLine>? {
        val cleanTitle = cleanQuery(title)
        val cleanArtist = cleanQuery(artist)

        // Primary artist (first artist before comma, &, feat, ft, etc.)
        val primaryArtist = cleanArtist.split(MULTI_ARTIST_SEPARATOR).firstOrNull()?.trim().orEmpty()

        // If the title is in "Artist - Title" format, extract both parts
        val titleParts = if (cleanTitle.contains(TITLE_SEPARATOR)) {
            cleanTitle.split(TITLE_SEPARATOR, limit = 2)
        } else null

        val extractedTitle = when {
            titleParts != null && titleParts[0].trim().equals(cleanArtist, ignoreCase = true) -> titleParts[1].trim()
            titleParts != null && titleParts[1].trim().equals(cleanArtist, ignoreCase = true) -> titleParts[0].trim()
            titleParts != null && titleParts[0].isNotBlank() && titleParts[1].isNotBlank() -> titleParts[1].trim()
            else -> cleanTitle
        }

        val extractedArtist = when {
            titleParts != null && titleParts[0].trim().equals(cleanArtist, ignoreCase = true) -> cleanArtist
            titleParts != null && titleParts[1].trim().equals(cleanArtist, ignoreCase = true) -> cleanArtist
            titleParts != null && cleanArtist.isBlank() -> titleParts[0].trim()
            else -> cleanArtist
        }

        val titleWithoutBrackets = extractedTitle
            .replace(BRACKETED_CONTENT, " ")
            .replace(NON_ALPHANUMERIC, " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val searchAttempts = mutableListOf<SearchAttempt>()

        // 1. Primary artist + clean title (e.g. "Banjaare Barsaat", "Anuv Jain Arz Kiya Hai", "Arijit Singh Apna Bana Le")
        if (primaryArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt("$primaryArtist $cleanTitle".trim(), cleanTitle, cleanArtist))
        }

        // 2. Clean title + primary artist (e.g. "Barsaat Banjaare")
        if (primaryArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt("$cleanTitle $primaryArtist".trim(), cleanTitle, cleanArtist))
        }

        // 3. Full artist + clean title (if different from primary artist)
        if (cleanArtist != primaryArtist && cleanArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt("$cleanArtist $cleanTitle".trim(), cleanTitle, cleanArtist))
        }

        // 4. Extracted artist + extracted title (for "Artist - Title" video labels)
        if (extractedArtist != primaryArtist && extractedArtist.isNotBlank() && extractedTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt("$extractedArtist $extractedTitle".trim(), extractedTitle, extractedArtist))
        }

        // 5. Standalone clean title (crucial fallback when uploader names differ, e.g. "Woh Lamhe Woh Baatein", "Barsaat")
        if (cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt(cleanTitle, cleanTitle, cleanArtist))
        }

        // 6. Standalone title without brackets
        if (titleWithoutBrackets.isNotBlank() && titleWithoutBrackets != cleanTitle) {
            searchAttempts.add(SearchAttempt(titleWithoutBrackets, titleWithoutBrackets, cleanArtist))
        }

        val distinctAttempts = searchAttempts.distinctBy { it.query }

        val songUrl = distinctAttempts.asSequence()
            .mapNotNull { attempt -> searchSongUrl(attempt.query, attempt.title, attempt.artist) }
            .firstOrNull() ?: return null

        val html = fetchHtml(songUrl)
        if (html.isNullOrBlank()) return null

        val lines = parseHtml(html)
        if (lines.isNullOrEmpty()) return null
        return lines
    }

    /**
     * Searches Genius for the track and returns the song's page URL.
     */
    internal fun searchSongUrl(cleanTitle: String, cleanArtist: String): String? {
        return searchSongUrl("$cleanArtist $cleanTitle".trim(), cleanTitle, cleanArtist)
    }

    private fun searchSongUrl(query: String, targetTitle: String, targetArtist: String): String? {
        val url = "https://genius.com/api/search/multi?q=${URLEncoder.encode(query, "UTF-8")}"
        val responseBody = httpGet(url) ?: return null

        return runCatching {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val responseObj = root["response"]?.jsonObject ?: return null
            val sections = responseObj["sections"]?.jsonArray ?: return null

            val songSection = sections.firstOrNull {
                (it as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull == "song"
            }?.jsonObject ?: return null

            val hits = songSection["hits"]?.jsonArray ?: return null
            val candidates = hits.mapNotNull { (it as? JsonObject)?.get("result")?.jsonObject }

            val best = bestMatch(candidates, targetTitle, targetArtist)
            best?.get("url")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun bestMatch(
        candidates: List<JsonObject>,
        targetTitle: String,
        targetArtist: String,
    ): JsonObject? {
        if (candidates.isEmpty()) return null
        val normTitle = targetTitle.lowercase(Locale.ROOT)
        val normArtist = targetArtist.lowercase(Locale.ROOT)
        val primaryArtist = targetArtist.split(MULTI_ARTIST_SEPARATOR).firstOrNull()?.trim()?.lowercase(Locale.ROOT).orEmpty()

        val scored = candidates.mapNotNull { item ->
            val title = item["title"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: ""
            val artist = item["artist_names"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: ""
            val path = item["path"]?.jsonPrimitive?.contentOrNull ?: ""

            val titleExact = normTitle.isNotBlank() && (title == normTitle)
            val titleMatches = titleExact || (normTitle.isNotBlank() && (title.contains(normTitle) || normTitle.contains(title)))
            val artistMatches = normArtist.isNotBlank() && (
                artist == normArtist || artist.contains(normArtist) || normArtist.contains(artist) ||
                (primaryArtist.isNotBlank() && (artist.contains(primaryArtist) || title.contains(primaryArtist)))
            )

            if (!titleMatches && !artistMatches) return@mapNotNull null

            var score = 0
            if (titleExact) score += 60
            else if (titleMatches) score += 30

            if (artistMatches) {
                if (artist == normArtist) score += 40
                else score += 25
            }

            // Penalize translations / instrumentals / reviews unless specifically requested
            if (path.contains("translation", ignoreCase = true) && !normTitle.contains("translation")) score -= 30
            if (path.contains("türkçe", ignoreCase = true) || path.contains("polskie-tlumaczenie", ignoreCase = true)) score -= 40
            if (path.contains("tracklist", ignoreCase = true) || path.contains("album-art", ignoreCase = true)) score -= 50

            if (score <= 0) return@mapNotNull null

            Pair(item, score)
        }
        return scored.maxByOrNull { it.second }?.first
    }

    /**
     * Parses the Genius lyrics HTML page using Jsoup, extracting text while preserving
     * stanzas and section headers.
     */
    internal fun parseHtml(html: String): List<LyricLine>? {
        return runCatching {
            parseHtmlUnsafe(html)
        }.getOrNull()
    }

    private fun parseHtmlUnsafe(html: String): List<LyricLine>? {
        val doc = Jsoup.parse(html)

        // Modern Genius uses data-lyrics-container="true", older pages use div.lyrics
        var containers = doc.select("div[data-lyrics-container=true]")
        if (containers.isEmpty()) {
            containers = doc.select("div.lyrics")
        }
        if (containers.isEmpty()) return null

        val fullTextBuilder = StringBuilder()

        for (container in containers) {
            // Remove headers, contributors, translation links, buttons, ads
            container.select(
                "[data-exclude-from-selection=true], " +
                    "[class*='LyricsHeader'], " +
                    "[class*='SongBioPreview'], " +
                    "[class*='InreadAd'], " +
                    "[class*='ContributorsCreditSong'], " +
                    "button, " +
                    "script, " +
                    "style",
            ).remove()

            // Turn <br> into explicit newlines before extracting text
            container.select("br").forEach { it.replaceWith(TextNode("\n")) }
            container.select("p").forEach { it.prepend("\n") }

            val text = container.wholeText()
            if (text.isNotBlank()) {
                fullTextBuilder.append(text).append("\n")
            }
        }

        val rawLyrics = fullTextBuilder.toString()
        if (rawLyrics.isBlank()) return null

        val cleaned = stripArtifacts(rawLyrics)
        return textToLyricLines(cleaned).takeIf { it.isNotEmpty() }
    }

    /**
     * Cleans common web scraper artifacts found on Genius:
     * - "You might also like" insertions
     * - Trailing "Embed" or "[0-9]+Embed"
     * - Non-breaking or zero-width unicode spaces
     */
    internal fun stripArtifacts(raw: String): String {
        return raw
            // Replace non-breaking / special unicode spaces with normal space
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace('\uFEFF', ' ')
            // Remove "You might also like" mid-text recommendations
            .replace(YOU_MIGHT_ALSO_LIKE, "")
            .trim()
            // Remove trailing "123Embed" or "Embed" at the very end
            .replace(TRAILING_EMBED, "")
            .trim()
    }

    /**
     * Converts clean multi-line lyrics text into structured [LyricLine]s.
     */
    internal fun textToLyricLines(text: String): List<LyricLine> {
        val lines = text.lines()
        val result = mutableListOf<LyricLine>()
        var lastWasGap = false

        for (rawLine in lines) {
            val line = rawLine.trim().replace(TRAILING_EMBED, "").trim()
            if (isSectionHeader(line)) {
                // Drop [Verse 1: ...], [Chorus], etc. so headers don't clutter lyrics
                continue
            }
            if (line.isEmpty()) {
                if (!lastWasGap && result.isNotEmpty()) {
                    result.add(LyricLine(timeMs = 0L, text = ""))
                    lastWasGap = true
                }
            } else {
                result.add(LyricLine(timeMs = 0L, text = line))
                lastWasGap = false
            }
        }

        // Drop leading and trailing gaps
        while (result.isNotEmpty() && result.first().isGap) result.removeAt(0)
        while (result.isNotEmpty() && result.last().isGap) result.removeAt(result.lastIndex)

        return result
    }

    /**
     * Checks if a line is a section header like "[Verse 1]", "[Chorus]", "[Bridge]".
     */
    /**
     * Aligns plain Genius lyrics with YouTube transcript cues (Jugaad 1).
     * If transcript cues exist, uses fuzzy word matching to transfer real timestamps
     * to the clean Genius text lines.
     * If no transcript cues exist, uses word-paced estimation across song duration.
     */
    internal fun alignLyrics(
        geniusLines: List<LyricLine>,
        transcriptCues: List<LyricLine>?,
        durationMs: Long,
    ): List<LyricLine> {
        val filtered = geniusLines.filter { it.text.isNotBlank() && !isSectionHeader(it.text) }
        if (filtered.isEmpty()) return geniusLines

        if (transcriptCues.isNullOrEmpty()) {
            return paceAcrossDuration(filtered, durationMs)
        }

        // 1. Anchor matching: find reliable timestamp anchors from transcript cues
        val anchors = mutableMapOf<Int, Long>()
        var cueIdx = 0

        for (gIdx in filtered.indices) {
            val gLine = filtered[gIdx]
            val gWords = gLine.text.lowercase(Locale.ROOT)
                .split(Regex("""\s+"""))
                .map { it.replace(Regex("""[^\p{L}\p{N}]"""), "") }
                .filter { it.length > 2 }
                .toSet()
            if (gWords.isEmpty()) continue

            var bestTime: Long? = null
            var bestOverlap = 0
            var bestCuePos = cueIdx

            val windowEnd = (cueIdx + 8).coerceAtMost(transcriptCues.size)
            for (i in cueIdx until windowEnd) {
                val cue = transcriptCues[i]
                val cWords = cue.text.lowercase(Locale.ROOT)
                    .split(Regex("""\s+"""))
                    .map { it.replace(Regex("""[^\p{L}\p{N}]"""), "") }
                    .filter { it.length > 2 }
                    .toSet()

                val overlap = gWords.intersect(cWords).size
                if (overlap > bestOverlap) {
                    bestOverlap = overlap
                    bestTime = cue.timeMs
                    bestCuePos = i
                }
            }

            if (bestTime != null && (bestOverlap >= 2 || (bestOverlap >= 1 && gWords.size <= 2))) {
                anchors[gIdx] = bestTime
                cueIdx = bestCuePos + 1
            }
        }

        // If less than 20% matched, YouTube captions likely don't match the lyrics language/content.
        // Fallback to word-weighted smooth duration pacing.
        if (anchors.size < (filtered.size * 0.20).toInt()) {
            return paceAcrossDuration(filtered, durationMs)
        }

        // 2. Smooth linear interpolation between matched anchors
        val validDuration = if (durationMs > 10_000L) durationMs else (filtered.size * 3_500L)
        val defaultIntro = (validDuration * 0.08).toLong().coerceIn(3_000L, 10_000L)
        val defaultEnd = (validDuration - 6_000L).coerceAtLeast(defaultIntro + 5_000L)

        val timestamps = LongArray(filtered.size)
        var lastAnchorIdx = -1
        var lastAnchorTime = defaultIntro

        for (i in filtered.indices) {
            val anchorTime = anchors[i]
            if (anchorTime != null) {
                timestamps[i] = maxOf(lastAnchorTime, anchorTime)
                // Interpolate any gap before this anchor
                if (lastAnchorIdx < i - 1) {
                    val gap = i - lastAnchorIdx
                    val timeSpan = timestamps[i] - lastAnchorTime
                    for (k in 1 until gap) {
                        val subIdx = lastAnchorIdx + k
                        if (subIdx >= 0) {
                            timestamps[subIdx] = lastAnchorTime + (timeSpan * k) / gap
                        }
                    }
                }
                lastAnchorIdx = i
                lastAnchorTime = timestamps[i]
            }
        }

        // Handle trailing lines after the last anchor
        if (lastAnchorIdx < filtered.size - 1) {
            val remaining = filtered.size - 1 - lastAnchorIdx
            val timeSpan = (defaultEnd - lastAnchorTime).coerceAtLeast(remaining * 2_000L)
            for (k in 1..remaining) {
                val subIdx = lastAnchorIdx + k
                timestamps[subIdx] = lastAnchorTime + (timeSpan * k) / (remaining + 1)
            }
        }

        return filtered.mapIndexed { index, line ->
            val start = timestamps[index]
            val nextStart = timestamps.getOrNull(index + 1) ?: (start + 3_000L)
            LyricLine(
                timeMs = start,
                text = line.text,
                sungUntilMs = nextStart,
                isEstimatedTiming = false,
            )
        }
    }

    private fun paceAcrossDuration(lines: List<LyricLine>, durationMs: Long): List<LyricLine> {
        val validDuration = if (durationMs > 10_000L) durationMs else (lines.size * 3_500L)
        val introMs = (validDuration * 0.08).toLong().coerceIn(3_000L, 10_000L)
        val availableMs = (validDuration - introMs - 6_000L).coerceAtLeast(lines.size * 1_800L)

        val weights = lines.map { line ->
            line.text.split(Regex("""\s+""")).size.coerceAtLeast(1)
        }
        val totalWeight = weights.sum().coerceAtLeast(1)

        var currentTime = introMs
        return lines.mapIndexed { index, line ->
            val lineTime = currentTime
            val step = ((weights[index].toDouble() / totalWeight) * availableMs).toLong().coerceAtLeast(1_500L)
            currentTime += step
            LyricLine(
                timeMs = lineTime,
                text = line.text,
                sungUntilMs = lineTime + step,
                isEstimatedTiming = true,
            )
        }
    }

    fun isSectionHeader(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 3) return false
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length <= 80) return true
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim().lowercase(Locale.ROOT)
            return inner.startsWith("verse") || inner.startsWith("chorus") || inner.startsWith("pre-chorus") ||
                inner.startsWith("bridge") || inner.startsWith("intro") || inner.startsWith("outro") ||
                inner.startsWith("hook") || inner.startsWith("refrain") || inner.startsWith("solo") ||
                inner.startsWith("instrumental")
        }
        return false
    }

    private fun cleanQuery(text: String): String {
        var cleaned = text
            .replace(DECORATIVE_CHARS, " ")
            .replace(NOISE, " ")
            .replace(PRODUCER_TAGS, " ")
            .substringBefore(" | ")
            .replace(TRENDING_SUFFIX, "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { text.trim() }
    }

    private val MULTI_ARTIST_SEPARATOR by lazy {
        Regex("""[,&/]|(?i)\b(?:feat\.?|ft\.?|featuring|with|and)\b""")
    }
    private val TRENDING_SUFFIX by lazy {
        Regex("""\s*[-–—:]\s*(?:Trending|trending|Viral|viral)\b""", RegexOption.IGNORE_CASE)
    }

    private data class SearchAttempt(
        val query: String,
        val title: String,
        val artist: String,
    )

    private fun httpGet(url: String): String? = runCatching {
        val isApi = url.contains("/api/")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_AGENT)
            .header("Accept", if (isApi) "application/json, text/plain, */*" else "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Priority", "u=0, i")
            .header("sec-ch-ua", "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-platform", "\"Windows\"")
            .header("sec-fetch-dest", if (isApi) "empty" else "document")
            .header("sec-fetch-mode", if (isApi) "cors" else "navigate")
            .header("sec-fetch-site", if (isApi) "same-origin" else "none")
            .header("sec-fetch-user", "?1")
            .header("upgrade-insecure-requests", "1")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    private fun fetchHtml(url: String): String? = httpGet(url)

    private val TITLE_SEPARATOR by lazy { Regex("""\s*[-–—:]\s*""") }
    private val DECORATIVE_CHARS by lazy { Regex("""[♪♫★☆【】《》「」~_]""") }
    private val PRODUCER_TAGS by lazy {
        Regex("""(?i)\b(?:prod(?:uced)?\.?(?:\s+by)?)\s+.*$""")
    }
    private val NOISE by lazy {
        Regex(
            """\s*[(\[]\s*(?:from|feat\.?|ft\.?|featuring|with|prod\.?|produced by|official|lyrical|video|audio|remix|music video|visualizer|mv|hd|4k|hq|full song)[^)\]]*[)\]]|""" +
                """\s*\b(?:official\s+(?:music\s+)?(?:video|audio)|lyrical(?:\s+video)?|full\s+song|4k\s+video|hd\s+video|music\s+video)\b""",
            RegexOption.IGNORE_CASE,
        )
    }
    private val BRACKETED_CONTENT by lazy { Regex("""\s*[\(\[].*?[\)\]]""") }
    private val NON_ALPHANUMERIC by lazy { Regex("[^\\p{L}\\p{N}\\s]") }
    private val YOU_MIGHT_ALSO_LIKE by lazy { Regex("""\d*You might also like""", RegexOption.IGNORE_CASE) }
    private val TRAILING_EMBED by lazy { Regex("""\d*Embed\s*$""", RegexOption.IGNORE_CASE) }
}
