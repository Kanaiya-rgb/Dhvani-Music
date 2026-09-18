package com.music.dhvani.data.canvas

import com.music.dhvani.data.lyrics.LyricsCleaner
import java.text.Normalizer
import java.util.Locale

enum class CanvasSource(val displayName: String) {
    SPOTIFY("Spotify"),
    APPLE_MUSIC("Apple Music"),
    TIDAL("Tidal"),
    COMMUNITY("Community"),
}

/**
 * Model representing motion canvas video artwork from Apple Music, Tidal, or Community.
 */
data class CanvasArtwork(
    val url: String,
    val fallbackUrl: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val source: CanvasSource,
) {
    /**
     * Checks if this candidate matches the desired track strictly and fuzzily.
     * Integrates [LyricsCleaner] for stripping YouTube Music noise ("Official Video",
     * movie mentions, collaborations) and validates track & artist identity.
     */
    fun matches(wantTitle: String, wantArtist: String, wantAlbum: String? = null): Boolean {
        val candTitle = title.orEmpty().trim()
        val candArtist = artist.orEmpty().trim()

        if (candTitle.isBlank() || wantTitle.isBlank()) return false

        val cleanWantTitle = LyricsCleaner.cleanTitle(wantTitle, wantArtist)
        val cleanCandidateTitle = LyricsCleaner.cleanTitle(candTitle, candArtist)

        val normWantTitle = normalize(cleanWantTitle).ifBlank { normalize(wantTitle) }
        val normCandidateTitle = normalize(cleanCandidateTitle).ifBlank { normalize(candTitle) }

        // 1. Check title match via LyricsCleaner and normalized tokens
        val titleMatches = LyricsCleaner.isTitleMatch(candTitle, wantTitle) ||
            LyricsCleaner.isTitleMatch(cleanCandidateTitle, cleanWantTitle) ||
            normCandidateTitle == normWantTitle ||
            normCandidateTitle.contains(normWantTitle) ||
            normWantTitle.contains(normCandidateTitle) ||
            shareSignificantTokens(normWantTitle, normCandidateTitle)

        if (!titleMatches) return false

        // 2. Check artist match
        if (wantArtist.isNotBlank() && candArtist.isNotBlank()) {
            val cleanWantArtist = LyricsCleaner.cleanArtist(wantArtist)
            val cleanCandidateArtist = LyricsCleaner.cleanArtist(candArtist)

            val normWantArtist = normalize(cleanWantArtist).ifBlank { normalize(wantArtist) }
            val normCandidateArtist = normalize(cleanCandidateArtist).ifBlank { normalize(candArtist) }

            val artistMatches = LyricsCleaner.isArtistMatch(candArtist, wantArtist) ||
                LyricsCleaner.isArtistMatch(cleanCandidateArtist, cleanWantArtist) ||
                normCandidateArtist == normWantArtist ||
                normCandidateArtist.contains(normWantArtist) ||
                normWantArtist.contains(normCandidateArtist) ||
                shareSignificantTokens(normWantArtist, normCandidateArtist)

            if (!artistMatches) return false
        }

        // 3. Album match (bonus validation if specified)
        if (!wantAlbum.isNullOrBlank() && !album.isNullOrBlank()) {
            val normWantAlbum = normalize(LyricsCleaner.cleanAlbum(wantAlbum))
            val normCandidateAlbum = normalize(LyricsCleaner.cleanAlbum(album))
            if (normWantAlbum.isNotEmpty() && normCandidateAlbum.isNotEmpty()) {
                // If title and artist match cleanly, album mismatch won't veto singles or deluxes
            }
        }

        return true
    }

    companion object {
        private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val BRACKETED = Regex("""[(\[].*?[)\]]""")
        private val NON_ALPHANUM = Regex("[^a-z0-9\\s]")
        private val EXTRA_WHITESPACE = Regex("\\s+")

        /**
         * Cleans a string by:
         * 1. Removing bracketed content e.g. (Official Video), [Remastered]
         * 2. Decomposing unicode and stripping accents / diacritical marks
         * 3. Lowercasing
         * 4. Replacing punctuation with spaces
         * 5. Collapsing consecutive whitespace
         */
        fun normalize(text: String): String {
            val withoutBrackets = BRACKETED.replace(text, " ")
            val decomposed = Normalizer.normalize(withoutBrackets, Normalizer.Form.NFD)
            val withoutDiacritics = DIACRITICS.replace(decomposed, "")
            val lower = withoutDiacritics.lowercase(Locale.ROOT)
            val cleanPunctuation = NON_ALPHANUM.replace(lower, " ")
            return EXTRA_WHITESPACE.replace(cleanPunctuation, " ").trim()
        }

        private fun shareSignificantTokens(a: String, b: String): Boolean {
            val tokensA = a.split(" ").filter { it.length > 2 }
            val tokensB = b.split(" ").filter { it.length > 2 }
            if (tokensA.isEmpty() || tokensB.isEmpty()) return false
            return tokensA.any { it in tokensB }
        }
    }
}

internal fun splitArtists(raw: String): List<String> =
    raw.split(ARTIST_SEPARATORS)
        .map { CanvasArtwork.normalize(it) }
        .filter { it.isNotBlank() }

private val ARTIST_SEPARATORS = Regex(
    "(?:\\s*,\\s*|\\s*&\\s*|\\s+·\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
    RegexOption.IGNORE_CASE,
)

internal fun canvasGet(url: String, headers: Map<String, String> = emptyMap()): String? {
    val request = okhttp3.Request.Builder().url(url).apply {
        headers.forEach { (name, value) -> header(name, value) }
    }.build()
    return runCatching {
        com.music.dhvani.data.Http.client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()
}

internal const val CANVAS_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/122.0.0.0 Safari/537.36"

