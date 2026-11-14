package com.music.dhvani.data

/**
 * Universal Explicit Content Advisory ([E]) Detector.
 *
 * Checks platform advisory signals, metadata tags, title keywords, and
 * parental advisory album releases (such as The Weeknd's Starboy, Tame Impala's Currents,
 * hip-hop/R&B mature albums, etc.) so mature tracks consistently display the official
 * [E] badge across Search, Home, Library, Mini Player, and Now Playing screens.
 */
object ExplicitDetector {

    private val EXPLICIT_TITLE_KEYWORDS = listOf(
        "(explicit)",
        "[explicit]",
        "explicit version",
        "explicit",
        "parental advisory",
    )

    private val KNOWN_EXPLICIT_TRACKS = listOf(
        "starboy",
        "die for you",
        "the hills",
        "often",
        "reminder",
        "party monster",
        "can't feel my face",
        "heartless",
        "yes i'm changing",
        "the less i know the better",
        "sicko mode",
        "rockstar",
        "goosebumps",
        "humble",
        "dna",
        "money trees",
        "swimming pools",
        "without me",
        "lose yourself",
        "the real slim shady",
        "stan",
        "mockingbird",
        "godzilla",
        "rap god",
    )

    private val KNOWN_EXPLICIT_ALBUMS = listOf(
        "starboy",
        "after hours",
        "beauty behind the madness",
        "my dear melancholy",
        "dawn fm",
        "currents",
        "my beautiful dark twisted fantasy",
        "damn.",
        "good kid, m.a.a.d city",
        "to pimp a butterfly",
        "mr. morale",
        "astroworld",
        "utopia",
        "rodeo",
        "birds in the trap",
        "views",
        "scorpion",
        "certified lover boy",
        "her loss",
        "for all the dogs",
        "take care",
        "nothing was the same",
        "igor",
        "call me if you get lost",
        "flower boy",
        "donda",
        "yeezus",
        "the marshall mathers lp",
        "the eminem show",
        "recovery",
        "kamikaze",
        "music to be murdered by",
        "graduation",
        "late registration",
        "the college dropout",
        "savage mode",
        "heroes & villains",
    )

    /**
     * Determines whether a track is mature/explicit.
     *
     * @param isExplicitFlag Explicit status already resolved from InnerTube/Tidal/Addons.
     * @param title Track title.
     * @param artist Track artist.
     * @param albumName Track album name (if known).
     */
    fun isExplicit(
        isExplicitFlag: Boolean = false,
        title: String = "",
        artist: String = "",
        albumName: String? = null,
    ): Boolean {
        if (isExplicitFlag) return true

        val titleLower = title.lowercase().trim()
        if (titleLower.isBlank()) return false

        // Clean / radio edits explicitly opt out
        if (titleLower.contains("(clean)") ||
            titleLower.contains("[clean]") ||
            titleLower.contains("clean version") ||
            titleLower.contains("radio edit") ||
            titleLower.contains("clean edit")
        ) {
            return false
        }

        // Direct title keywords
        for (kw in EXPLICIT_TITLE_KEYWORDS) {
            if (titleLower.contains(kw)) return true
        }

        // Known explicit songs
        for (track in KNOWN_EXPLICIT_TRACKS) {
            if (titleLower == track || titleLower.startsWith("$track ") || titleLower.contains(" $track ") || titleLower.contains("($track") || titleLower.contains("-$track")) {
                return true
            }
        }

        // Parental Advisory explicit albums
        if (!albumName.isNullOrBlank()) {
            val albumLower = albumName.lowercase().trim()
            if (albumLower.contains("(explicit)") || albumLower.contains("[explicit]")) {
                return true
            }
            for (album in KNOWN_EXPLICIT_ALBUMS) {
                if (albumLower == album || albumLower.startsWith("$album ") || albumLower.contains(album)) {
                    return true
                }
            }
        }

        return false
    }
}

