package com.music.dhvani.data.lyrics

/**
 * The databases [LyricsRepository] can ask, in the order it asks them.
 *
 * Exposed in Settings so the user can choose which sources to query and reorder them.
 * Only verified, active, working sources are included here.
 */
enum class LyricsSource(
    val label: String,
    val detail: String,
    /** Whether it can return per-word timings, or only whole lines. */
    val wordSynced: Boolean,
) {
    BINI_LYRICS(
        label = "am-lyrics",
        detail = "Apple timings by binimum / am-lyrics, matched on recording",
        wordSynced = true,
    ),
    LRC_RED(
        label = "lrc.red",
        detail = "Apple timings by recording, syllable by syllable",
        wordSynced = true,
    ),
    LYRICS_PLUS(
        label = "LyricsPlus",
        detail = "Multi-provider syllable & word sync via binimum mirror",
        wordSynced = true,
    ),
    BETTER_LYRICS(
        label = "BetterLyrics",
        detail = "Apple Music timings, word by word",
        wordSynced = true,
    ),
    BETTER_LYRICS_PORTATO(
        label = "QQ Music",
        detail = "QQ Music word-synced karaoke & syllable timings via OIAPI",
        wordSynced = true,
    ),
    MEGALOBIZ(
        label = "Megalobiz",
        detail = "Synced community LRC with rich Hindi & regional catalog",
        wordSynced = false,
    ),
    MUSIXMATCH(
        label = "Musixmatch",
        detail = "Whole lines, from the biggest lyrics database there is",
        wordSynced = false,
    ),
    LRCLIB(
        label = "LRCLIB",
        detail = "Whole lines only, open-source community database",
        wordSynced = false,
    ),
    YOUTUBE_TRANSCRIPT(
        label = "YouTube captions",
        detail = "Timed captions matched to the exact playing video",
        wordSynced = false,
    ),
    YOUTUBE_MUSIC(
        label = "YouTube Music",
        detail = "Plain lyrics from the playing video's Lyrics tab",
        wordSynced = false,
    ),
    KUGOU(
        label = "KuGou",
        detail = "Whole lines, strong outside the English catalogue",
        wordSynced = false,
    ),
    UNISON(
        label = "Unison",
        detail = "Contributed by listeners, so it has what nobody licensed",
        wordSynced = true,
    ),
    GENIUS(
        label = "Genius",
        detail = "Synced via YouTube captions & word pacing, massive catalogue",
        wordSynced = false,
    ),
}
