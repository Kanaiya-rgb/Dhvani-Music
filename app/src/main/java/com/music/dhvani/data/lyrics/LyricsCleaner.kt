package com.music.dhvani.data.lyrics

import java.util.Locale
import kotlin.math.abs

/**
 * Normalizes song titles and artist names from noisy streaming metadata (YouTube Music,
 * upload labels, movie mentions) and provides strict verification matching so lyrics
 * are never mistakenly loaded for the wrong song.
 */
object LyricsCleaner {

    private val BRACKETED = Regex("""[(\[]([^()\[\]]*)[)\]]""")

    private val NOISE_PATTERNS = listOf(
        Regex("""\b(?:official\s+(?:music\s+)?video|official\s+audio|audio\s+song|video\s+song|lyrical\s+(?:video|song)?|full\s+(?:song|video|audio)|visualizer|teaser|trailer|4k\s+video|hd\s+video|motion\s+poster|promo)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:t-series|zee\s+music(?:\s+company)?|yrf|sony\s+music(?:\s+india)?|saregama|tips\s+official|speed\s+records|geet\s+mp3)\b""", RegexOption.IGNORE_CASE),
    )

    private val ASIDE_NOISE = Regex(
        """\b(?:from|feat\.?|ft\.?|featuring|with|official|video|audio|lyric|lyrics|lyrical|visualizer|full\s+song|teaser|trailer|4k|hd|hq|remaster\w*|live|slowed|reverb|version|album\s+version|original\s+version|hindi|punjabi|telugu|tamil)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val ARTIST_SPLIT = Regex("""\s*(?:[,&;/·|]|\band\b|\bx\b|\bvs\.?\b|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b)\s*""", RegexOption.IGNORE_CASE)

    /**
     * Cleans a noisy title (e.g. from YouTube Music) into its canonical song title.
     * Examples:
     * - "Kesariya (From \"Brahmastra\") | Official Music Video | Ranbir | Alia | Pritam | Arijit Singh" -> "Kesariya"
     * - "Pehle Bhi Main - Animal | Ranbir Kapoor | Vishal Mishra" -> "Pehle Bhi Main"
     * - "Tauba Tauba (From \"Bad Newz\")" -> "Tauba Tauba"
     * - "Chaleya (Hindi)" -> "Chaleya"
     * - "Arijit Singh - O Maahi (Official Audio)" -> "O Maahi"
     */
    fun cleanTitle(rawTitle: String, rawArtist: String = ""): String {
        if (rawTitle.isBlank()) return rawTitle
        var text = rawTitle.trim()

        // 1. YouTube metadata after pipe '|' is promotional credits / channel tags
        if (text.contains("|")) {
            text = text.substringBefore("|").trim()
        }

        // 2. Handle "Artist - Title" vs "Title - Movie/Subtitle" upload conventions
        if (text.contains(" - ")) {
            val parts = text.split(" - ", limit = 2)
            val head = parts[0].trim()
            val tail = parts[1].trim()
            val artistLower = rawArtist.lowercase(Locale.ROOT)
            val headLower = head.lowercase(Locale.ROOT)
            val tailLower = tail.lowercase(Locale.ROOT)

            val isHeadArtist = (artistLower.isNotBlank() && (artistLower.contains(headLower) || headLower.contains(artistLower))) ||
                isRecordLabelOrChannel(artistLower)

            val isTailMovieOrExtra = tailLower.contains("from ") ||
                tailLower.startsWith("movie") ||
                tailLower.contains("film") ||
                tailLower.contains("soundtrack") ||
                tailLower.contains("ost") ||
                tailLower.contains("album")

            text = when {
                isHeadArtist && !isTailMovieOrExtra -> tail
                isTailMovieOrExtra -> head
                isRecordLabelOrChannel(artistLower) -> tail
                else -> head
            }
        }

        // 3. Remove bracketed noise: (From "Movie"), [Official Audio], (Lyrical), (Hindi), etc.
        text = BRACKETED.replace(text) { match ->
            val inner = match.groupValues[1].trim()
            if (ASIDE_NOISE.containsMatchIn(inner) || inner.length > 25) {
                " "
            } else {
                // If it's a version tag like (Acoustic) or (Remix), keep it
                val lower = inner.lowercase(Locale.ROOT)
                if (lower.contains("remix") || lower.contains("acoustic") || lower.contains("unplugged") || lower.contains("live")) {
                    "($inner)"
                } else {
                    // Movie name or secondary note: drop it from title
                    " "
                }
            }
        }

        // 4. Remove unbracketed noise patterns
        for (pattern in NOISE_PATTERNS) {
            text = pattern.replace(text, " ")
        }

        // 5. Remove trailing noise words
        text = text.replace(Regex("""\s+\b(song|songs|video|audio|lyrics?|lyrical|full|original)\b\s*$""", RegexOption.IGNORE_CASE), "")

        // 6. Clean whitespace
        text = text.replace(Regex("""\s+"""), " ").trim()

        return text.ifBlank { rawTitle.trim() }
    }

    /**
     * Extracts primary artist and strips featuring / collaboration junk.
     * "Pritam, Arijit Singh, Amitabh Bhattacharya" -> "Pritam"
     */
    fun cleanArtist(rawArtist: String): String {
        if (rawArtist.isBlank()) return ""
        val first = rawArtist.split(ARTIST_SPLIT).firstOrNull()?.trim().orEmpty()
        return first.replace(Regex("""\s+"""), " ")
    }

    /**
     * Returns all individual artists credited on the track.
     */
    fun allArtists(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return emptyList()
        return rawArtist.split(ARTIST_SPLIT)
            .map { it.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.isNotBlank() }
    }

    /**
     * Cleans album title if provided.
     */
    fun cleanAlbum(rawAlbum: String): String {
        return cleanTitle(rawAlbum)
    }

    /**
     * Normalizes a title for strict equality checking.
     * Keeps unicode characters (Latin, Devanagari, Gurmukhi, etc.).
     */
    fun normalize(str: String): String {
        return str.lowercase(Locale.ROOT)
            .replace(Regex("""\s*[(\[](?:from|feat\.?|ft\.?|official|audio|video|lyrics?|lyrical|version|mix)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[^\p{L}\p{N}]"""), "")
    }

    /**
     * Strict verification: does the candidate title truly match the target song?
     * Rejects unrelated songs completely to prevent wrong lyrics.
     */
    fun isTitleMatch(candidateTitle: String, targetTitle: String): Boolean {
        val candNorm = normalize(candidateTitle)
        val targetNorm = normalize(targetTitle)
        if (candNorm.isEmpty() || targetNorm.isEmpty()) return false
        if (candNorm == targetNorm) return true

        // Substring check with strict length ratio
        if (candNorm.contains(targetNorm) || targetNorm.contains(candNorm)) {
            val minLen = minOf(candNorm.length, targetNorm.length)
            val maxLen = maxOf(candNorm.length, targetNorm.length)
            if (minLen >= 4 && minLen.toDouble() / maxLen.toDouble() >= 0.60) {
                return true
            }
        }

        // Word-level token match
        val candWords = candidateTitle.lowercase(Locale.ROOT)
            .split(Regex("""[\s\-–—|_.,'"]+"""))
            .map { it.replace(Regex("""[^\p{L}\p{N}]"""), "") }
            .filter { it.length > 1 }
        val targetWords = targetTitle.lowercase(Locale.ROOT)
            .split(Regex("""[\s\-–—|_.,'"]+"""))
            .map { it.replace(Regex("""[^\p{L}\p{N}]"""), "") }
            .filter { it.length > 1 }

        if (targetWords.isNotEmpty() && targetWords.all { it in candWords }) {
            return true
        }
        if (candWords.isNotEmpty() && candWords.all { it in targetWords }) {
            return true
        }

        return false
    }

    /**
     * Checks if candidate artist overlaps with any credited artist.
     */
    fun isArtistMatch(candidateArtist: String, targetArtist: String): Boolean {
        if (candidateArtist.isBlank() || targetArtist.isBlank()) return true
        val candNorm = normalize(candidateArtist)
        val targetNorm = normalize(targetArtist)
        if (candNorm.contains(targetNorm) || targetNorm.contains(candNorm)) return true

        val allTarget = allArtists(targetArtist).map { normalize(it) }.filter { it.isNotEmpty() }
        val allCand = allArtists(candidateArtist).map { normalize(it) }.filter { it.isNotEmpty() }
        return allTarget.any { t -> allCand.any { c -> t.contains(c) || c.contains(t) } }
    }

    /**
     * Checks whether candidate duration matches target within reasonable bounds.
     */
    fun isDurationMatch(candidateSec: Int?, targetSec: Int?, toleranceSec: Int = 10): Boolean {
        if (candidateSec == null || targetSec == null || candidateSec <= 0 || targetSec <= 0) return true
        return abs(candidateSec - targetSec) <= toleranceSec
    }

    private fun isRecordLabelOrChannel(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("records") || lower.contains("music") || lower.contains("series") ||
            lower.contains("company") || lower.contains("entertainment") || lower.contains("films") ||
            lower.contains("studios") || lower.contains("vevo") || lower.contains("channel") ||
            lower.contains("topic") || lower.contains("media") || lower == "ncs"
    }
}
