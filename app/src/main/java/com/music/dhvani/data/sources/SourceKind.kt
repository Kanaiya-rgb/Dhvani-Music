package com.music.dhvani.data.sources

/**
 * The kinds of source this build knows how to talk to.
 *
 * Fixed and small on purpose. **Declaration order here is the order sources
 * are tried** — see `SourceRegistry.active()`, which sorts on `kind.ordinal` —
 * so a custom module comes before the built-in one, then JioSaavn, then
 * YouTube Music. Adding a source means adding a [MusicSource] implementation
 * and an entry here, which is the point — every protocol the app speaks is one
 * someone can read in this repo, and a source can't teach the app a new way to
 * behave after it ships.
 *
 * What varies per *instance* — which index, whose module — is [SourceConfig].
 */
enum class SourceKind(
    val label: String,
    val detail: String,
    /** The chips under the name on the sources screen. */
    val labels: List<String>,
    /** Whether an instance needs a URL before it can do anything — and so needs an editor. */
    val needsServer: Boolean,
    /** Whether this kind can serve bit-exact audio when asked. */
    val canServeLossless: Boolean,
    /**
     * Whether this kind answers quickly enough to be worth asking *before* a
     * track is played, so its copy can be pinned and cached ahead of time.
     */
    val worthPrefetching: Boolean = false,
) {
    /**
     * A module index the user pointed at themselves, tried ahead of the one
     * baked into the build.
     */
    CUSTOM_MODULE(
        label = "Custom module",
        detail = "Your own compatible module index. Tried before the built-in one.",
        labels = listOf("FLAC", "Lossless", "Hi-Res", "Plugins"),
        needsServer = true,
        canServeLossless = true,
        worthPrefetching = false,
    ),

    /**
     * A URL to a compatible module-index JSON.
     */
    MODULE(
        label = "Module source",
        detail = "A URL to a compatible module index. Modules are JS plugins that " +
            "can search and stream from services like Qobuz, Apple Music and more.",
        labels = listOf("FLAC", "Lossless", "Hi-Res", "Plugins"),
        needsServer = true,
        canServeLossless = true,
        worthPrefetching = false,
    ),

    TIDAL(
        label = "TIDAL HiFi",
        detail = "Studio Master & CD-quality FLAC (16-bit / 44.1 kHz) and Dolby Atmos spatial audio.",
        labels = listOf("FLAC", "Lossless", "Dolby Atmos", "16/24-bit"),
        needsServer = false,
        canServeLossless = true,
        worthPrefetching = true,
    ),

    MONOCHROME(
        label = "Monochrome Hi-Res",
        detail = "Direct studio master FLAC streams up to 24-bit / 96 kHz.",
        labels = listOf("FLAC", "24-bit · 96kHz", "Lossless"),
        needsServer = false,
        canServeLossless = true,
        worthPrefetching = true,
    ),

    KUWO(
        label = "Kuwo FLAC Master",
        detail = "Direct CD-Quality 16/24-bit Lossless FLAC streams via Kuwo CDN.",
        labels = listOf("FLAC", "Lossless", "2000kbps"),
        needsServer = true,
        canServeLossless = true,
        worthPrefetching = true,
    ),

    NETEASE(
        label = "NetEase Cloud Music",
        detail = "High-quality 320kbps MP3 audio streams via NetEase CDN.",
        labels = listOf("High Quality", "320kbps", "NetEase"),
        needsServer = true,
        canServeLossless = false,
        worthPrefetching = true,
    ),

    JIOSAAVN(
        label = "High Quality AAC",
        detail = "High-quality streams up to 320kbps AAC/MP4. A lossy fallback, tried before standard Opus.",
        labels = listOf("High Quality", "320kbps"),
        needsServer = false,
        canServeLossless = false,
        worthPrefetching = true,
    ),

    /**
     * The source the app was built on, listed here so it always has a fixed
     * place: second, behind the module source. It cannot be removed.
     */
    YOUTUBE(
        label = "Standard Opus",
        detail = "The full catalogue, at Opus up to about 171 kbps. Lossy — there is no " +
            "lossless rendition to ask for.",
        labels = listOf("Lossy", "Full catalogue", "Radio"),
        needsServer = false,
        canServeLossless = false,
    ),
}
