package com.music.dhvani.data.lyrics

/**
 * One word of a line, with the stretch of the song it is sung over.
 *
 * Apple's TTML splits long words into syllables; those are merged back into
 * whole words on the way in, so [startMs] is the first syllable's start and
 * [endMs] the last one's end. Whole words are what the sweep needs — a
 * highlight that ran across "e" and "nough" separately reads as a stutter.
 */
data class LyricWord(val startMs: Long, val endMs: Long, val text: String)

/**
 * One synced line. [timeMs] is when it starts; a blank [text] is an
 * instrumental stretch — LRC files mark those with a bare timestamp.
 *
 * [words] is populated only by the providers that carry word-level timing
 * (BetterLyrics, LyricsPlus, SimpMusic's rich sync). LRCLIB has none, so a
 * line from there highlights whole; see [isWordSynced].
 *
 * [sungUntilMs] is the line's own end where a line-synced provider states one,
 * which is what lets an interlude be told apart from a slowly sung line.
 *
 * [background] is the answering vocal — the "(ooh)" or the echoed half-phrase
 * a second voice sings over the lead. It is a line in its own right, with its
 * own stamp and its own words, because that is what it is: it starts partway
 * through the line it answers and routinely runs past the *next* line's stamp.
 * Run into [text] it dragged the sweep along with it, and the cursor — which
 * takes the last line whose stamp has passed — moved on before the bracket had
 * been sung, so the tail of the line was skipped. Kept apart it draws
 * underneath the lead on its own clock. Never nested: a background line's own
 * [background] is always null.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: LyricLine? = null,
    val timingSource: LyricLine? = null,
    val isEstimatedTiming: Boolean = false,
) {
    val isGap: Boolean get() = text.isEmpty()

    val isWordSynced: Boolean get() = timingSource?.isWordSynced ?: words.isNotEmpty()

    /**
     * Whether anything actually told us when the singing stops, rather than
     * only when it starts. Word timings carry it, and so does a provider that
     * stamps the line's own end ([sungUntilMs]).
     *
     * The distance to the next line's stamp is *not* evidence of an end: that
     * distance is the line's own slot, and on a line-synced source it is
     * routinely ten seconds for a line sung over all ten of them.
     */
    val hasKnownEnd: Boolean get() = timingSource?.hasKnownEnd ?: (words.isNotEmpty() || sungUntilMs != null)

    /**
     * When the last word finishes — or the line's own end where the provider
     * gave one, or [timeMs] when nothing did. Check [hasKnownEnd] before
     * reading a silence out of this.
     *
     * The answering vocal counts: it is still this line being sung, and it
     * regularly holds a note past the lead's last word. Measured without it, a
     * break would be found in the middle of a line that is still going.
     */
    val endMs: Long
        get() {
            if (timingSource != null) return timingSource.endMs
            val lead = words.lastOrNull()?.endMs ?: sungUntilMs ?: timeMs
            return maxOf(lead, background?.endMs ?: lead)
        }

    /**
     * How far through the line the singing has got, 0..1, as a fractional
     * index into [text]. The sweep reveals up to this character.
     *
     * Within a word it interpolates across that word's own span, so a held
     * note draws slowly and a rattled-off one snaps. Whitespace between two
     * words is credited to the gap between them: it fills as the singer moves
     * on rather than jumping ahead of the next word's first letter.
     */
    fun revealedChars(positionMs: Long, lineEndMs: Long = endMs): Float {
        if (timingSource != null) {
            val srcLen = timingSource.text.length.coerceAtLeast(1).toFloat()
            val ratio = (timingSource.revealedChars(positionMs, lineEndMs) / srcLen).coerceIn(0f, 1f)
            return ratio * text.length.toFloat()
        }
        if (words.isEmpty()) {
            if (positionMs < timeMs) return 0f
            val effectiveEnd = if (lineEndMs > timeMs) lineEndMs else sungUntilMs ?: timeMs
            if (effectiveEnd <= timeMs || positionMs >= effectiveEnd) return text.length.toFloat()
            val fraction = ((positionMs - timeMs).toFloat() / (effectiveEnd - timeMs)).coerceIn(0f, 1f)
            return fraction * text.length.toFloat()
        }
        var offset = 0
        words.forEachIndexed { index, word ->
            // Where this word sits in [text]. Built by walking rather than
            // searching, so a word repeated in the line still lines up.
            val start = text.indexOf(word.text, offset).takeIf { it >= 0 } ?: offset
            val end = start + word.text.length
            if (positionMs < word.startMs) return start.toFloat()
            if (positionMs < word.endMs) {
                val span = (word.endMs - word.startMs).coerceAtLeast(1L)
                val through = (positionMs - word.startMs).toFloat() / span
                return start + through * word.text.length
            }
            // Past this word: the trailing space fills over the pause before
            // the next one, so the highlight keeps creeping instead of resting
            // on the word's last letter.
            val next = words.getOrNull(index + 1)
            if (next != null && positionMs < next.startMs) {
                val gapStart = text.indexOf(next.text, end).takeIf { it >= 0 } ?: end
                val pause = (next.startMs - word.endMs).coerceAtLeast(1L)
                val through = (positionMs - word.endMs).toFloat() / pause
                return end + through * (gapStart - end)
            }
            offset = end
        }
        return text.length.toFloat()
    }

    /**
     * How much bloom the word being sung has earned, 0..1.
     *
     * Two things decide it. How long the word is held sets the ceiling — a
     * note carried for a second swells, a word rattled off in a tenth of one
     * barely registers, which is the difference between a glow that belongs to
     * the singing and a lamp dragged along under the text. Then an envelope
     * across the word's own span rises as it lands and eases off as it goes,
     * so each word blooms and lets go rather than the light being on
     * throughout and stepping between brightnesses at every boundary.
     *
     * Zero between words and after the last one, which is what keeps the
     * pauses dark and costs nothing to draw.
     */
    fun glowIntensity(positionMs: Long, lineEndMs: Long = endMs): Float {
        if (timingSource != null) return timingSource.glowIntensity(positionMs, lineEndMs)
        if (words.isEmpty()) {
            val effectiveEnd = if (lineEndMs > timeMs) lineEndMs else sungUntilMs ?: timeMs
            if (positionMs !in timeMs..effectiveEnd || effectiveEnd <= timeMs) return 0f
            val held = (effectiveEnd - timeMs).coerceAtLeast(1L)
            val through = ((positionMs - timeMs).toFloat() / held).coerceIn(0f, 1f)
            val envelope = when {
                through < GLOW_ATTACK -> through / GLOW_ATTACK
                through > 1f - GLOW_RELEASE -> (1f - through) / GLOW_RELEASE
                else -> 1f
            }
            return (GLOW_FLOOR + (1f - GLOW_FLOOR) * 0.7f) * envelope.coerceIn(0f, 1f)
        }
        val word = words.firstOrNull { positionMs < it.endMs } ?: return 0f
        if (positionMs < word.startMs) return 0f

        val held = (word.endMs - word.startMs).coerceAtLeast(1L)
        val through = ((positionMs - word.startMs).toFloat() / held).coerceIn(0f, 1f)
        val envelope = when {
            through < GLOW_ATTACK -> through / GLOW_ATTACK
            through > 1f - GLOW_RELEASE -> (1f - through) / GLOW_RELEASE
            else -> 1f
        }
        val pace = ((held - GLOW_FAST_MS).toFloat() / (GLOW_SLOW_MS - GLOW_FAST_MS))
            .coerceIn(0f, 1f)
        return (GLOW_FLOOR + (1f - GLOW_FLOOR) * pace) * envelope.coerceIn(0f, 1f)
    }

    /**
     * Returns a copy of this line with syllable-estimated [LyricWord] timings
     * if the line does not already carry word-level timings from a provider.
     */
    fun withEstimatedWords(lineEndMs: Long): LyricLine {
        if (words.isNotEmpty() || text.isBlank()) return this
        val effectiveEnd = if (lineEndMs > timeMs) lineEndMs else sungUntilMs ?: (timeMs + text.length * 80L)
        val estimated = SyllableEstimator.estimateWords(text, timeMs, effectiveEnd)
        return copy(words = estimated)
    }
}

/**
 * Enhances a list of [LyricLine]s with syllable and word estimation so every track
 * supports live syllable/word-by-word sing-along, vocal tempo bounce, and word seeking.
 */
fun List<LyricLine>.withKaraokeSyllables(): List<LyricLine> {
    if (isEmpty()) return this
    if (any { it.words.isNotEmpty() }) return this
    return mapIndexed { index, line ->
        val nextStart = getOrNull(index + 1)?.timeMs
        val effectiveEnd = nextStart ?: (line.timeMs + (line.text.length * 120L).coerceAtLeast(1_500L))
        line.withEstimatedWords(effectiveEnd)
    }
}

/** A word this short is patter; it gets [GLOW_FLOOR] and no more. */
private const val GLOW_FAST_MS = 130L

/** A word held this long gets the full bloom. */
private const val GLOW_SLOW_MS = 800L

/** What the quickest words still get, so patter doesn't go completely flat. */
private const val GLOW_FLOOR = 0.22f

/** Share of a word's span spent coming up, and going back down. */
private const val GLOW_ATTACK = 0.18f
private const val GLOW_RELEASE = 0.38f

/**
 * Turns plain (unsynced) lyrics into paced [LyricLine]s spanning the song's duration.
 * This guarantees every track has readable lyrics that follow along with playback.
 */
fun plainToLyricLines(plainText: String, durationMs: Long): List<LyricLine> {
    val cleanLines = plainText.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { line ->
            line.startsWith("Source:", ignoreCase = true) ||
                line.startsWith("Lyrics licensed", ignoreCase = true) ||
                line.startsWith("Written by:", ignoreCase = true)
        }
        .toList()
    if (cleanLines.isEmpty()) return emptyList()

    val count = cleanLines.size
    val validDuration = if (durationMs > 10_000L) durationMs else (count * 4_000L)
    val startMs = if (validDuration > 20_000L) minOf(6_000L, validDuration / 12) else 0L
    val endMs = if (validDuration > 30_000L) maxOf(validDuration - 8_000L, (validDuration * 0.92).toLong()) else validDuration
    val availableMs = (endMs - startMs).coerceAtLeast(count * 1_000L)
    val stepMs = availableMs / count

    return cleanLines.mapIndexed { index, text ->
        val lineStart = startMs + index * stepMs
        val lineEnd = lineStart + stepMs
        LyricLine(
            timeMs = lineStart,
            text = text,
            sungUntilMs = lineEnd,
            isEstimatedTiming = true,
        )
    }
}
