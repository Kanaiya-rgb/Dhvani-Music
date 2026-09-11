package com.music.dhvani.data.lyrics

import kotlin.math.max

/**
 * Phonetic syllable and vocal tempo estimator.
 *
 * Converts line-synced lyrics without word-level timestamps (such as standard LRC,
 * LRCLIB, or embedded tags) into natural, rhythmically weighted [LyricWord] timings.
 * This guarantees that every song in the app supports dynamic Apple Music Sing style
 * word-by-word karaoke sing-along, vocal bounce, and word tap-seeking.
 */
object SyllableEstimator {

    private val VOWEL_REGEX = Regex("""[aeiouy]+""", RegexOption.IGNORE_CASE)
    private val INDIC_VOWEL_REGEX = Regex("""[\u0904-\u0914\u093E-\u094C\u0962\u0963]""")

    /**
     * Estimates phonetic syllable count for a given word across English, Indic/Romanized
     * Hindi, and general phonetic patterns.
     */
    fun estimateSyllables(word: String): Int {
        val clean = word.filter { !it.isWhitespace() && it !in ",.;:!?-\"\'()[]{}" }.lowercase()
        if (clean.isEmpty()) return 1
        if (clean.length <= 3 && !clean.any { it in '\u0900'..'\u097F' }) return 1

        // Check for Indic characters (Devanagari matras and independent vowels)
        val indicCount = INDIC_VOWEL_REGEX.findAll(clean).count()
        if (indicCount > 0) return max(1, indicCount)

        // Western / Romanized vowel cluster matching
        var count = VOWEL_REGEX.findAll(clean).count()

        val isIndicRomanized = clean in setOf(
            "tere", "mere", "kaise", "jaise", "aate", "jaate", "tujhe", "mujhe",
            "kare", "bhare", "sake", "hoke", "aake", "chale", "mile", "khile",
            "rahe", "sahe", "kahe", "dekhe", "samjhe", "liye", "jiye", "piye", "aaye", "gaaye"
        )

        // English silent 'e' at word end (e.g. "love", "game", "time")
        if (!isIndicRomanized && clean.endsWith("e") && !clean.endsWith("le") && !clean.endsWith("ee") && clean.length > 2) {
            count--
        }

        // Silent 'ed' suffix (e.g. "walked", "jumped") unless preceded by 't' or 'd'
        if (clean.endsWith("ed") && clean.length > 3) {
            val beforeEd = clean[clean.length - 3]
            if (beforeEd != 't' && beforeEd != 'd') {
                count--
            }
        }

        // Common suffix adjustments
        if (clean.endsWith("ing") || clean.endsWith("tion") || clean.endsWith("sion") || clean.endsWith("able")) {
            count = max(count, 2)
        }

        return max(1, count)
    }

    /**
     * Generates a rhythmically spaced list of [LyricWord]s from a line's text and duration.
     *
     * @param text The full line text.
     * @param startMs The line start timestamp in milliseconds.
     * @param endMs The line end timestamp in milliseconds.
     * @return List of [LyricWord] items spanning the duration with natural vocal pacing.
     */
    fun estimateWords(text: String, startMs: Long, endMs: Long): List<LyricWord> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val rawSpan = endMs - startMs
        val minSpanNeeded = tokens.size * 180L
        val effectiveSpan = if (rawSpan >= minSpanNeeded) rawSpan else (tokens.size * 350L)

        // Musical lead-in and tail margins so the line doesn't abruptly start or cut off
        val leadMargin = if (effectiveSpan > 2_000L) minOf(200L, effectiveSpan / 10) else 0L
        val tailMargin = if (effectiveSpan > 2_000L) minOf(300L, effectiveSpan / 8) else 50L
        val singingSpan = (effectiveSpan - leadMargin - tailMargin).coerceAtLeast(tokens.size * 150L)

        // Calculate rhythmic weight for each word
        val weights = tokens.map { token ->
            val syllables = estimateSyllables(token)
            val lengthFactor = token.length.coerceIn(1, 15) * 0.12f
            val punctuationBonus = when {
                token.endsWith("...") || token.endsWith("—") -> 0.65f
                token.endsWith(",") || token.endsWith(";") -> 0.35f
                token.endsWith(".") || token.endsWith("!") || token.endsWith("?") -> 0.45f
                else -> 0.0f
            }
            syllables + lengthFactor + punctuationBonus
        }

        val totalWeight = weights.sum().coerceAtLeast(1f)
        val msPerWeight = singingSpan.toFloat() / totalWeight

        val result = mutableListOf<LyricWord>()
        var currentMs = startMs + leadMargin

        for (i in tokens.indices) {
            val token = tokens[i]
            val weight = weights[i]
            val duration = (weight * msPerWeight).toLong().coerceAtLeast(120L)
            val wordStart = currentMs
            val wordEnd = wordStart + duration

            result.add(LyricWord(startMs = wordStart, endMs = wordEnd, text = token))
            // Brief inter-word vocal breath before the next word
            val interWordPause = if (token.any { it in ",.;:!?-—" }) 60L else 20L
            currentMs = wordEnd + interWordPause
        }

        return result
    }
}
