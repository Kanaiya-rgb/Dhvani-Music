package com.music.flux.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.music.flux.BuildConfig
import com.music.flux.auth.AuthStore
import com.music.flux.data.lyrics.LyricsSource
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stream bitrate ceiling. HIGH means "whatever the best available format is".
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps · smallest download", "29 MB/hr"),
    MEDIUM(128, "Medium", "~128 kbps · balanced", "58 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "Best available · ~171 kbps Opus", "77 MB/hr"),
}

/**
 * What to keep when a track is saved to the device.
 *
 * Deliberately not [AudioQuality]. That enum budgets a *stream*, and is priced
 * per hour because the same bytes are spent again on every replay. A download is
 * the opposite trade — paid for once, kept, played from disk forever after — so
 * the figure that decides it is what one track costs, and the rung worth
 * defaulting to is the top one rather than the cheap one.
 *
 * The rungs themselves differ too. On the YouTube path a download is
 * AAC-in-MP4 or nothing (see
 * [StreamResolver.resolveForDownload][com.music.flux.data.innertube.StreamResolver.resolveForDownload]),
 * so there is no Opus here to describe the way [AudioQuality.HIGH] does. And
 * [LOSSLESS] has no streaming counterpart at all: it is the only rung that lets
 * a configured source's bit-exact file end up as a file on disk.
 */
enum class DownloadQuality(
    /** Ceiling for the AAC ladder. [Int.MAX_VALUE] means "whichever rung is best". */
    val maxKbps: Int,
    val label: String,
    val detail: String,
    /** Roughly what one four-minute track costs at this rung, sans unit context. */
    val perTrack: String,
    /** Whether a source's bit-exact file is worth keeping, or a transcode will do. */
    val keepsLossless: Boolean,
) {
    STANDARD(128, "Standard", "~128 kbps AAC · fits more on the device", "~4 MB", false),
    HIGH(Int.MAX_VALUE, "High", "Best AAC on offer, usually ~256 kbps", "~8 MB", false),
    LOSSLESS(
        Int.MAX_VALUE,
        "Lossless",
        "Bit-exact if a source has it, best AAC if not",
        "~35 MB",
        true,
    ),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

enum class SliderStyle(val label: String) {
    DEFAULT("Default"),
    CAPSULE("Capsule"),
    MATERIAL("Material"),
    WAVY("Wavy"),
    SQUIGGLY("Squiggly"),
    SLIM("Slim"),
}

enum class PlayerBackgroundStyle(val label: String) {
    DEFAULT("Follow Theme"),
    GRADIENT("Gradient"),
    BLUR("Blur"),
}

enum class MiniPlayerBackgroundStyle(val label: String) {
    DEFAULT("Follow Theme"),
    TRANSPARENT("Transparent"),
    BLUR("Blur"),
    GRADIENT("Gradient"),
    PURE_BLACK("Pure Black"),
}

enum class PlayerButtonsStyle(val label: String) {
    DEFAULT("Default"),
    PRIMARY("Primary Color"),
    TERTIARY("Tertiary Color"),
}

enum class LyricsPosition(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
}

enum class LyricsAnimationStyle(val label: String) {
    NONE("None"),
    FADE("Fade"),
    GLOW("Glow"),
    SLIDE("Slide"),
    KARAOKE("Karaoke"),
    APPLE("Apple Music Style"),
}

enum class DensityScale(val value: Float, val label: String) {
    NATIVE(1.0f, "Native (100%)"),
    SLIGHTLY_COMPACT(0.85f, "Slightly Compact (85%)"),
    COMPACT(0.75f, "Compact (75%)"),
    VERY_COMPACT(0.65f, "Very Compact (65%)"),
    ULTRA_COMPACT(0.55f, "Ultra Compact (55%)"),
    ;

    companion object {
        fun fromValue(value: Float): DensityScale = entries.find { kotlin.math.abs(it.value - value) < 0.01f } ?: NATIVE
    }
}

enum class GridItemSize(val label: String) {
    SMALL("Small"),
    BIG("Big"),
}

/**
 * App settings, backed by SharedPreferences and exposed as flows.
 *
 * PlaybackService runs in the same process as the UI, so it observes these
 * same flows and applies changes to the live ExoPlayer instance immediately —
 * no restart, no rebinding.
 */
object AppSettings {

    private lateinit var prefs: SharedPreferences

    /** Only for the Discord token — everything else on here is plain prefs. */
    private lateinit var authStore: AuthStore

    /**
     * Quality ceilings, one per kind of connection — the point of the split is
     * that Wi-Fi can stay on High while mobile data is capped. Both default to
     * High; the mobile plan is the user's to budget, not ours to assume.
     */
    val audioQualityWifi = MutableStateFlow(AudioQuality.HIGH)
    val audioQualityCellular = MutableStateFlow(AudioQuality.HIGH)

    /**
     * What a saved file should be, answered on its own terms.
     *
     * Kept apart from the two ceilings above on purpose. Those are about what
     * this minute's connection costs, and a download outlives the minute it was
     * started in — capping a permanent file at whichever network happened to be
     * in hand bakes a temporary decision into a lasting artefact, and the
     * reverse (a High ceiling on Wi-Fi implying 35MB FLACs of everything) is
     * just as wrong in the other direction.
     *
     * Data spend on a download is [wifiOnlyDownloads]' problem, not this
     * setting's, which is what lets this one be purely about the file.
     *
     * Defaults to [DownloadQuality.LOSSLESS] because that is what the download
     * path already did on an uncapped connection, and [migrateDownloadQuality]
     * keeps it that way for the people it didn't.
     */
    val downloadQuality = MutableStateFlow(DownloadQuality.LOSSLESS)

    /**
     * Refuse to start a download while the connection charges for data.
     *
     * Metered rather than literally-Wi-Fi, the same test [effectiveAudioQuality]
     * makes, because the thing worth protecting is the bill and not the radio: a
     * tethered hotspot is Wi-Fi that costs money, and an unmetered home
     * connection is worth using whether or not it arrives over Wi-Fi.
     *
     * On by default, and that is a deliberate change of behaviour for anyone
     * updating. [downloadQuality] defaulting to Lossless means a tap that used
     * to spend four megabytes of mobile data can now spend thirty-five, and of
     * the two ways to get that wrong — silently overspending a data plan, or
     * refusing with a sentence naming the switch that would allow it — only the
     * second is recoverable by the person it happens to.
     */
    val wifiOnlyDownloads = MutableStateFlow(true)

    /** Whether the active network charges for data. `null` while offline. */
    val meteredConnection = MutableStateFlow<Boolean?>(null)

    // `losslessAudio` used to live here, behind a "Prefer lossless" switch on
    // the Sources screen. It is gone: sources are asked for their best and each
    // degrades on its own terms, so the switch's only real effect was to ask a
    // module for a worse file than it was holding. See
    // [SourceResolver.requestForNow][com.music.flux.data.sources.SourceResolver.requestForNow],
    // which now reads [effectiveAudioQuality] and nothing else.

    val crossfadeSeconds = MutableStateFlow(0)

    /**
     * Lets Automix's analyzer decide the transition's timing and length
     * from each track's tempo, energy and structure, replacing the fixed
     * [crossfadeSeconds] window rather than needing it set to anything first
     * — [crossfadeSeconds] only matters here as a fallback while a pair is
     * still being analysed. Off by default: analysis costs a background
     * decode per track.
     *
     * See [com.music.flux.playback.smart.TransitionPlanner].
     */
    val smartFadeEnabled = MutableStateFlow(false)
    val skipSilence = MutableStateFlow(false)

    /**
     * Widens stereo output via [com.music.flux.playback.SpatialAudioProcessor],
     * a stereo widening + cross-feed effect running inside ExoPlayer's own
     * pipeline. Not true object-based spatial audio — YouTube only ever hands
     * us a stereo stream, so there's no Atmos-style source to render.
     */
    val spatialAudio = MutableStateFlow(false)
    val playbackSpeed = MutableStateFlow(1.0f)
    val themeMode = MutableStateFlow(ThemeMode.DARK)

    /** Keep playing similar music once the queue runs out. */
    val autoplay = MutableStateFlow(true)

    /** Put the playing track's codec, bitrate and sample rate on the player. */
    val showNerdStats = MutableStateFlow(false)

    /** Freezes the main player's mesh gradient instead of letting it drift/crossfade. */
    val reduceAnimation = MutableStateFlow(false)

    // ── Appearance & Theming (Meld features) ───────────────────────────
    val dynamicTheme = MutableStateFlow(true)
    val enableHighRefreshRate = MutableStateFlow(true)
    val densityScale = MutableStateFlow(1.0f)
    val sliderStyle = MutableStateFlow(SliderStyle.DEFAULT)
    val squigglySlider = MutableStateFlow(false)
    val playerBackgroundStyle = MutableStateFlow(PlayerBackgroundStyle.DEFAULT)
    val miniPlayerBackgroundStyle = MutableStateFlow(MiniPlayerBackgroundStyle.DEFAULT)
    val playerButtonsStyle = MutableStateFlow(PlayerButtonsStyle.DEFAULT)
    val hidePlayerThumbnail = MutableStateFlow(false)
    val cropAlbumArt = MutableStateFlow(false)
    val hideStatusBarOnFullscreen = MutableStateFlow(false)
    val swipeThumbnail = MutableStateFlow(true)
    val swipeSensitivity = MutableStateFlow(0.73f)
    val lyricsPosition = MutableStateFlow(LyricsPosition.CENTER)
    val lyricsAnimationStyle = MutableStateFlow(LyricsAnimationStyle.FADE)
    val lyricsGlowEffect = MutableStateFlow(false)
    val lyricsTextSize = MutableStateFlow(24f)
    val lyricsLineSpacing = MutableStateFlow(1.2f)
    val lyricsClickSeek = MutableStateFlow(true)
    val lyricsAutoScroll = MutableStateFlow(true)
    val respectAgentPositioning = MutableStateFlow(true)
    val defaultOpenTab = MutableStateFlow(0)
    val gridItemSize = MutableStateFlow(GridItemSize.SMALL)
    val slimNavBar = MutableStateFlow(false)
    val showRecognizeButton = MutableStateFlow(true)
    val showPlayRandomButton = MutableStateFlow(true)
    val showLikedPlaylist = MutableStateFlow(true)
    val showDownloadedPlaylist = MutableStateFlow(true)
    val showTopPlaylist = MutableStateFlow(true)
    val showCachedPlaylist = MutableStateFlow(true)
    val showUploadedPlaylist = MutableStateFlow(true)

    /** Stop playback when the app is swiped away from the recent apps screen. */
    val stopOnTaskRemoved = MutableStateFlow(false)

    /** Hides the volume slider on the main player, leaving the rest of the layout to reflow. */
    val hideVolumeBar = MutableStateFlow(false)

    /** Swiping a song row plays it next instead of adding it to the end of the queue. */
    val swipeToPlayNext = MutableStateFlow(false)

    /** Automatically resume playback when Bluetooth headphones or devices connect. */
    val resumeOnBluetooth = MutableStateFlow(false)

    /** Once a song has been suggested or played this session, AutoPlay won't offer it again. */
    val dontRepeatSuggestions = MutableStateFlow(false)

    /**
     * Leaves a music-video upload as itself instead of swapping it for its
     * catalogue audio release. See
     * [YtMusicRepository.resolveAudio][com.music.flux.data.YtMusicRepository.resolveAudio],
     * which checks this before ever running the swap.
     */
    val convertVideoToAudio = MutableStateFlow(true)

    /** Drops haze blur (status bar, mini player, bottom fade, lyrics focus) for a solid-fill look. */
    val reduceDynamicBlur = MutableStateFlow(false)

    /**
     * Plays a looping video behind the cover art on the player when one is
     * published for the track — Spotify's Canvas, Apple's motion artwork.
     *
     * Costs a video stream on top of the audio one and reaches three
     * services that have nothing to do with playback, so it stays a switch —
     * but it is the better default, and most tracks resolve to no canvas at
     * all. See [CanvasRepository][com.music.flux.data.canvas.CanvasRepository].
     */
    val animatedCanvas = MutableStateFlow(true)

    /**
     * Whether [animatedCanvas] is allowed to actually stream on a metered
     * connection, as distinct from the switch that turns the feature off
     * altogether.
     *
     * Off by default. A canvas clip loops for as long as its track plays,
     * and every loop past the first re-fetches the same few seconds of video
     * — see [CanvasCache][com.music.flux.data.canvas.CanvasCache] for why
     * that costs network at all rather than being answered from a buffer —
     * so a few-second clip behind a four-minute track on cellular is not a
     * flat video cost, it is that cost repeated dozens of times per song.
     * That is the shape of the reported 8GB day: still art costs nothing
     * here and stays up regardless of this setting.
     */
    val canvasOverCellular = MutableStateFlow(false)

    /**
     * Blows the player's cover art out to a full-bleed banner running off the
     * top of the screen, rather than sitting it in a square card.
     *
     * The treatment motion artwork has always had, applied to still sleeves too.
     * Off restores the card: the sleeve keeps its corners, its shadow and its
     * shrink-while-paused, and only a clip goes full-bleed. Phones only either
     * way — see the hero notes in
     * [NowPlayingScreen][com.music.flux.ui.player.NowPlayingScreen].
     */
    val fullBleedArtwork = MutableStateFlow(true)

    /**
     * Time-synced lyrics on the player, lit up as they are sung.
     *
     * On by default — it is most of the point of the player screen — but it
     * reaches third-party lyric databases for every track played, so it stays
     * a switch, and [lyricsSources] narrows which of them get asked.
     */
    val syncedLyrics = MutableStateFlow(true)

    /** The databases [syncedLyrics] may ask. Empty is the same as off. */
    val lyricsSources = MutableStateFlow(LyricsSource.entries.toSet())

    /**
     * The order [lyricsSources] are asked in — see [LyricsRepository][com.music.flux.data.lyrics.LyricsRepository]:
     * every enabled source is asked at once, but a higher-priority one still
     * pending is never preempted by a lower one that happened to answer first.
     * Reordered from Settings, so this is a full permutation of
     * [LyricsSource.entries] rather than a subset — enabling and ordering are
     * independent choices.
     */
    val lyricsSourceOrder = MutableStateFlow<List<LyricsSource>>(LyricsSource.entries)

    /**
     * Off, the highest-priority source to answer at all is taken as the
     * lyrics, word-synced or not. On, a merely line-synced answer is held as
     * a fallback while the rest of [lyricsSourceOrder] is still checked for a
     * word-synced one — worth the extra network calls to some, not to others,
     * which is why it defaults off rather than being how [LyricsRepository]
     * always behaved.
     */
    val prioritizeSyllableSync = MutableStateFlow(false)

    /** Disk budget for cached audio. [AudioCache][com.music.flux.playback.AudioCache] evicts past it. */
    val audioCacheLimitBytes = MutableStateFlow(DEFAULT_CACHE_LIMIT_BYTES)

    // ── Replay ──────────────────────────────────────────────────────────────

    /**
     * Whether Replay may work out a genre chart.
     *
     * Its own switch because it is the one part of Replay that isn't purely
     * local: everything else on that page is counted on this device and never
     * leaves it, while a genre has to be looked up by artist name — see
     * [ArtistFacts][com.music.flux.data.stats.ArtistFacts]. On by default,
     * since it sends a name and nothing else and the answer is what makes a
     * quarter of the page exist; off, the genre chart simply isn't drawn.
     */
    val replayGenres = MutableStateFlow(true)

    // ── Library ─────────────────────────────────────────────────────────────

    /**
     * Browse ids of the playlists pinned to the top of the Library tab, in the
     * order they were pinned.
     *
     * A [List] rather than a [Set]: pin order is part of what a pin means here —
     * the whole point is a small, hand-picked front row, and a set would leave
     * that order to hash iteration. Capped at [MAX_PINNED_PLAYLISTS] by
     * [togglePinnedPlaylist], the only way this is ever written.
     */
    val pinnedPlaylists = MutableStateFlow<List<String>>(emptyList())

    /** How many playlists [pinnedPlaylists] can hold at once. */
    const val MAX_PINNED_PLAYLISTS = 5

    // ── Scrobbling ──────────────────────────────────────────────────────

    /** One release gate shared by the settings UI and the playback service. */
    val scrobblingAvailable = true

    val lastfmEnabled = MutableStateFlow(false)
    val lastfmUsername = MutableStateFlow("")
    val lastfmSessionKey = MutableStateFlow("")
    val lastfmApiKey = MutableStateFlow("")
    val lastfmSecret = MutableStateFlow("")
    val lastfmEndpoint = MutableStateFlow("")
    val lastfmScrobbleEnabled = MutableStateFlow(false)
    val lastfmNowPlaying = MutableStateFlow(false)
    val scrobbleMinDuration = MutableStateFlow(30)
    val scrobbleDelayPercent = MutableStateFlow(0.5f)
    val scrobbleDelaySeconds = MutableStateFlow(180)
    val listenBrainzEnabled = MutableStateFlow(false)
    val listenBrainzToken = MutableStateFlow("")
    val spotifySpdcToken = MutableStateFlow("")

    // ── Discord Rich Presence ───────────────────────────────────────────

    /**
     * The connected Discord account's token, mirrored out of [AuthStore] so
     * [PlaybackService][com.music.flux.playback.PlaybackService] can pick
     * up a login without polling for one. Empty means not connected.
     *
     * Only the mirror is here — the persisted copy is encrypted, because unlike
     * a scrobbler key this one is the account itself.
     */
    val discordToken = MutableStateFlow("")

    /**
     * Who the token belongs to, cached at login. Kept so the settings screen
     * can show the account without a round trip every time it opens, and can
     * still show it offline.
     */
    val discordUsername = MutableStateFlow("")
    val discordName = MutableStateFlow("")
    val discordAvatar = MutableStateFlow("")

    val discordRpcEnabled = MutableStateFlow(true)

    /** Put the track title on the bold profile line, in place of the artist. */
    val discordUseDetails = MutableStateFlow(false)

    /** Reveals the presence-shape controls: status, activity type/name, buttons. */
    val discordAdvancedMode = MutableStateFlow(false)

    val discordStatus = MutableStateFlow("online")
    val discordActivityType = MutableStateFlow("listening")

    /** Overrides the "Listening to ___" line; empty means the app's own name. */
    val discordActivityName = MutableStateFlow("")

    val discordButton1Text = MutableStateFlow("")
    val discordButton1Visible = MutableStateFlow(true)
    val discordButton2Text = MutableStateFlow("")
    val discordButton2Visible = MutableStateFlow(true)

    // ── Listen Together ─────────────────────────────────────────────────
    val listenTogetherUsername = MutableStateFlow("Listener")
    val listenTogetherServerUrl = MutableStateFlow("wss://metroserverx.meowery.eu/ws")
    val listenTogetherAutoApproval = MutableStateFlow(false)
    val listenTogetherAutoApproveSuggestions = MutableStateFlow(false)
    val listenTogetherSyncVolume = MutableStateFlow(false)
    val listenTogetherSessionToken = MutableStateFlow("")
    val listenTogetherRoomCode = MutableStateFlow("")
    val listenTogetherUserId = MutableStateFlow("")
    val listenTogetherIsHost = MutableStateFlow(false)
    val listenTogetherSessionTimestamp = MutableStateFlow(0L)

    /** The notice about what connecting an account actually does has been read. */
    val discordInfoDismissed = MutableStateFlow(false)

    /** Published by PlaybackService so the UI can open the system equalizer. */
    val audioSessionId = MutableStateFlow(0)

    /**
     * True only while a Automix transition that is actually *mixing* is
     * audible — one that beat-matched, cued the incoming track into its
     * arrangement, or rode a filter.
     *
     * Deliberately not "a crossfade is running". The fallback case, where
     * neither track was analysed in time and the incoming one starts from 0:00
     * under a plain equal-power fade, is exactly what this must stay dark for:
     * the whole point is that seeing it means the analysis landed and did
     * something a plain crossfade could not.
     */
    val smartMixInProgress = MutableStateFlow(false)

    /**
     * How much of the *upcoming* transition has been analysed, for stats for
     * nerds. Published by the crossfade controller, which is the only thing
     * that knows which two tracks the next transition is between.
     */
    val smartAnalysis = MutableStateFlow(SmartAnalysis())

    /**
     * Where on the *playing* track the next transition is planned to happen, as
     * fractions of its duration, or null when there is nothing worth drawing.
     *
     * Only published once both tracks are measured. Before that the planner is
     * still working from a fallback window that moves as evidence arrives, and
     * a marker that slides around the bar would be worse than no marker.
     */
    val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)

    /** The ceiling that applies to a stream started right now. */
    val effectiveAudioQuality: AudioQuality
        get() = if (meteredConnection.value == true) {
            audioQualityCellular.value
        } else {
            audioQualityWifi.value
        }

    /**
     * Whether a download may start on the connection in hand.
     *
     * A null [meteredConnection] means there is no active network, and that is
     * deliberately allowed through: a download with nothing to download over
     * fails on the network and says so, which is true, where refusing it here
     * would blame a Wi-Fi setting for an outage.
     */
    val downloadsAllowedNow: Boolean
        get() = !wifiOnlyDownloads.value || meteredConnection.value != true

    fun init(context: Context) {
        prefs = context.getSharedPreferences("flux_settings", Context.MODE_PRIVATE)
        authStore = AuthStore(context)
        readAll()
        watchConnection(context)
    }

    /**
     * Re-reads every setting off disk.
     *
     * The one caller is an import ([Backup][com.music.flux.data.stats.Backup]),
     * which writes the whole preference file underneath these flows. Nothing
     * else in the app changes a preference without going through the setter
     * beside it, so nothing else has a reason to ask.
     *
     * Deliberately not re-registering the network callback: that watches the
     * device, not the preferences, and a second one would have both firing.
     */
    fun reload() {
        if (!this::prefs.isInitialized) return
        readAll()
    }

    private fun readAll() {
        migrateSingleQuality()
        audioQualityWifi.value = readQuality(KEY_QUALITY_WIFI)
        audioQualityCellular.value = readQuality(KEY_QUALITY_CELLULAR)
        migrateDownloadQuality()
        downloadQuality.value = readDownloadQuality()
        wifiOnlyDownloads.value = prefs.getBoolean(KEY_WIFI_ONLY_DOWNLOADS, true)
        crossfadeSeconds.value = prefs.getInt(KEY_CROSSFADE, 0)
        smartFadeEnabled.value = prefs.getBoolean(KEY_SMART_FADE, false)
        skipSilence.value = prefs.getBoolean(KEY_SKIP_SILENCE, false)
        spatialAudio.value = prefs.getBoolean(KEY_SPATIAL_AUDIO, false)
        playbackSpeed.value = prefs.getFloat(KEY_SPEED, 1.0f)
        themeMode.value = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "DARK")
        }.getOrDefault(ThemeMode.DARK)
        autoplay.value = prefs.getBoolean(KEY_AUTOPLAY, true)
        showNerdStats.value = prefs.getBoolean(KEY_NERD_STATS, false)
        reduceAnimation.value = prefs.getBoolean(KEY_REDUCE_ANIMATION, false)
        stopOnTaskRemoved.value = prefs.getBoolean(KEY_STOP_ON_TASK_REMOVED, false)
        hideVolumeBar.value = prefs.getBoolean(KEY_HIDE_VOLUME_BAR, false)
        swipeToPlayNext.value = prefs.getBoolean(KEY_SWIPE_TO_PLAY_NEXT, false)
        resumeOnBluetooth.value = prefs.getBoolean(KEY_RESUME_ON_BLUETOOTH, false)
        dontRepeatSuggestions.value = prefs.getBoolean(KEY_DONT_REPEAT_SUGGESTIONS, false)
        convertVideoToAudio.value = prefs.getBoolean(KEY_CONVERT_VIDEO_TO_AUDIO, true)
        reduceDynamicBlur.value = prefs.getBoolean(KEY_REDUCE_BLUR, false)
        dynamicTheme.value = prefs.getBoolean(KEY_DYNAMIC_THEME, true)
        enableHighRefreshRate.value = prefs.getBoolean(KEY_ENABLE_HIGH_REFRESH_RATE, true)
        densityScale.value = prefs.getFloat(KEY_DENSITY_SCALE, 1.0f)
        sliderStyle.value = runCatching {
            SliderStyle.valueOf(prefs.getString(KEY_SLIDER_STYLE, null) ?: "DEFAULT")
        }.getOrDefault(SliderStyle.DEFAULT)
        squigglySlider.value = prefs.getBoolean(KEY_SQUIGGLY_SLIDER, false)
        playerBackgroundStyle.value = runCatching {
            PlayerBackgroundStyle.valueOf(prefs.getString(KEY_PLAYER_BACKGROUND_STYLE, null) ?: "DEFAULT")
        }.getOrDefault(PlayerBackgroundStyle.DEFAULT)
        miniPlayerBackgroundStyle.value = runCatching {
            MiniPlayerBackgroundStyle.valueOf(prefs.getString(KEY_MINI_PLAYER_BACKGROUND_STYLE, null) ?: "DEFAULT")
        }.getOrDefault(MiniPlayerBackgroundStyle.DEFAULT)
        playerButtonsStyle.value = runCatching {
            PlayerButtonsStyle.valueOf(prefs.getString(KEY_PLAYER_BUTTONS_STYLE, null) ?: "DEFAULT")
        }.getOrDefault(PlayerButtonsStyle.DEFAULT)
        hidePlayerThumbnail.value = prefs.getBoolean(KEY_HIDE_PLAYER_THUMBNAIL, false)
        cropAlbumArt.value = prefs.getBoolean(KEY_CROP_ALBUM_ART, false)
        hideStatusBarOnFullscreen.value = prefs.getBoolean(KEY_HIDE_STATUS_BAR_ON_FULLSCREEN, false)
        swipeThumbnail.value = prefs.getBoolean(KEY_SWIPE_THUMBNAIL, true)
        swipeSensitivity.value = prefs.getFloat(KEY_SWIPE_SENSITIVITY, 0.73f)
        lyricsPosition.value = runCatching {
            LyricsPosition.valueOf(prefs.getString(KEY_LYRICS_POSITION, null) ?: "CENTER")
        }.getOrDefault(LyricsPosition.CENTER)
        lyricsAnimationStyle.value = runCatching {
            LyricsAnimationStyle.valueOf(prefs.getString(KEY_LYRICS_ANIMATION_STYLE, null) ?: "FADE")
        }.getOrDefault(LyricsAnimationStyle.FADE)
        lyricsGlowEffect.value = prefs.getBoolean(KEY_LYRICS_GLOW_EFFECT, false)
        lyricsTextSize.value = prefs.getFloat(KEY_LYRICS_TEXT_SIZE, 24f)
        lyricsLineSpacing.value = prefs.getFloat(KEY_LYRICS_LINE_SPACING, 1.2f)
        lyricsClickSeek.value = prefs.getBoolean(KEY_LYRICS_CLICK_SEEK, true)
        lyricsAutoScroll.value = prefs.getBoolean(KEY_LYRICS_AUTO_SCROLL, true)
        respectAgentPositioning.value = prefs.getBoolean(KEY_RESPECT_AGENT_POSITIONING, true)
        defaultOpenTab.value = prefs.getInt(KEY_DEFAULT_OPEN_TAB, 0)
        gridItemSize.value = runCatching {
            GridItemSize.valueOf(prefs.getString(KEY_GRID_ITEM_SIZE, null) ?: "SMALL")
        }.getOrDefault(GridItemSize.SMALL)
        slimNavBar.value = prefs.getBoolean(KEY_SLIM_NAV_BAR, false)
        showRecognizeButton.value = prefs.getBoolean(KEY_SHOW_RECOGNIZE_BUTTON, true)
        showPlayRandomButton.value = prefs.getBoolean(KEY_SHOW_PLAY_RANDOM_BUTTON, true)
        showLikedPlaylist.value = prefs.getBoolean(KEY_SHOW_LIKED_PLAYLIST, true)
        showDownloadedPlaylist.value = prefs.getBoolean(KEY_SHOW_DOWNLOADED_PLAYLIST, true)
        showTopPlaylist.value = prefs.getBoolean(KEY_SHOW_TOP_PLAYLIST, true)
        showCachedPlaylist.value = prefs.getBoolean(KEY_SHOW_CACHED_PLAYLIST, true)
        showUploadedPlaylist.value = prefs.getBoolean(KEY_SHOW_UPLOADED_PLAYLIST, true)
        animatedCanvas.value = prefs.getBoolean(KEY_ANIMATED_CANVAS, true)
        canvasOverCellular.value = prefs.getBoolean(KEY_CANVAS_OVER_CELLULAR, false)
        fullBleedArtwork.value = prefs.getBoolean(KEY_FULL_BLEED_ARTWORK, true)
        syncedLyrics.value = prefs.getBoolean(KEY_SYNCED_LYRICS, true)
        lyricsSources.value = readLyricsSources()
        lyricsSourceOrder.value = readLyricsSourceOrder()
        prioritizeSyllableSync.value = prefs.getBoolean(KEY_PRIORITIZE_SYLLABLE_SYNC, false)
        audioCacheLimitBytes.value = prefs.getLong(KEY_CACHE_LIMIT, DEFAULT_CACHE_LIMIT_BYTES)
            .coerceIn(DEFAULT_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        lastfmEnabled.value = prefs.getBoolean(KEY_LASTFM_ENABLED, false)
        lastfmUsername.value = prefs.getString(KEY_LASTFM_USERNAME, "").orEmpty()
        lastfmSessionKey.value = prefs.getString(KEY_LASTFM_SESSION_KEY, "").orEmpty()
        lastfmApiKey.value = prefs.getString(KEY_LASTFM_API_KEY, "").orEmpty().ifBlank { BuildConfig.LASTFM_API_KEY }
        lastfmSecret.value = prefs.getString(KEY_LASTFM_SECRET, "").orEmpty().ifBlank { BuildConfig.LASTFM_SECRET }
        lastfmEndpoint.value = prefs.getString(KEY_LASTFM_ENDPOINT, "").orEmpty()
        lastfmScrobbleEnabled.value = prefs.getBoolean(KEY_LASTFM_SCROBBLE_ENABLED, false)
        lastfmNowPlaying.value = prefs.getBoolean(KEY_LASTFM_NOW_PLAYING, false) && lastfmScrobbleEnabled.value
        scrobbleMinDuration.value = prefs.getInt(KEY_SCROBBLE_MIN_DURATION, 30)
        scrobbleDelayPercent.value = prefs.getFloat(KEY_SCROBBLE_DELAY_PERCENT, 0.5f)
        scrobbleDelaySeconds.value = prefs.getInt(KEY_SCROBBLE_DELAY_SECONDS, 180)
        listenBrainzEnabled.value = prefs.getBoolean(KEY_LISTENBRAINZ_ENABLED, false)
        listenBrainzToken.value = prefs.getString(KEY_LISTENBRAINZ_TOKEN, "").orEmpty()
        spotifySpdcToken.value = prefs.getString(KEY_SPOTIFY_SPDC_TOKEN, "").orEmpty()
        replayGenres.value = prefs.getBoolean(KEY_REPLAY_GENRES, true)
        pinnedPlaylists.value = readPinnedPlaylists()
        discordToken.value = authStore.discordToken.orEmpty()
        discordUsername.value = prefs.getString(KEY_DISCORD_USERNAME, "").orEmpty()
        discordName.value = prefs.getString(KEY_DISCORD_NAME, "").orEmpty()
        discordAvatar.value = prefs.getString(KEY_DISCORD_AVATAR, "").orEmpty()
        discordRpcEnabled.value = prefs.getBoolean(KEY_DISCORD_RPC_ENABLED, true)
        discordUseDetails.value = prefs.getBoolean(KEY_DISCORD_USE_DETAILS, false)
        discordAdvancedMode.value = prefs.getBoolean(KEY_DISCORD_ADVANCED_MODE, false)
        discordStatus.value = prefs.getString(KEY_DISCORD_STATUS, "online").orEmpty()
        discordActivityType.value = prefs.getString(KEY_DISCORD_ACTIVITY_TYPE, "listening").orEmpty()
        discordActivityName.value = prefs.getString(KEY_DISCORD_ACTIVITY_NAME, "").orEmpty()
        discordButton1Text.value = prefs.getString(KEY_DISCORD_BUTTON_1_TEXT, "").orEmpty()
        discordButton1Visible.value = prefs.getBoolean(KEY_DISCORD_BUTTON_1_VISIBLE, true)
        discordButton2Text.value = prefs.getString(KEY_DISCORD_BUTTON_2_TEXT, "").orEmpty()
        discordButton2Visible.value = prefs.getBoolean(KEY_DISCORD_BUTTON_2_VISIBLE, true)
        discordInfoDismissed.value = prefs.getBoolean(KEY_DISCORD_INFO_DISMISSED, false)
        listenTogetherUsername.value = prefs.getString(KEY_LISTEN_TOGETHER_USERNAME, null) ?: "Listener"
        listenTogetherServerUrl.value = prefs.getString(KEY_LISTEN_TOGETHER_SERVER_URL, null) ?: "wss://metroserverx.meowery.eu/ws"
        listenTogetherAutoApproval.value = prefs.getBoolean(KEY_LISTEN_TOGETHER_AUTO_APPROVAL, false)
        listenTogetherAutoApproveSuggestions.value = prefs.getBoolean(KEY_LISTEN_TOGETHER_AUTO_APPROVE_SUGGESTIONS, false)
        listenTogetherSyncVolume.value = prefs.getBoolean(KEY_LISTEN_TOGETHER_SYNC_VOLUME, false)
        listenTogetherSessionToken.value = prefs.getString(KEY_LISTEN_TOGETHER_SESSION_TOKEN, null) ?: ""
        listenTogetherRoomCode.value = prefs.getString(KEY_LISTEN_TOGETHER_ROOM_CODE, null) ?: ""
        listenTogetherUserId.value = prefs.getString(KEY_LISTEN_TOGETHER_USER_ID, null) ?: ""
        listenTogetherIsHost.value = prefs.getBoolean(KEY_LISTEN_TOGETHER_IS_HOST, false)
        listenTogetherSessionTimestamp.value = prefs.getLong(KEY_LISTEN_TOGETHER_SESSION_TIMESTAMP, 0L)
    }

    /**
     * True the first time this is called after [currentVersionCode] rises above
     * whatever was last recorded — i.e. once per update, on the first launch
     * after it installs. A fresh install has nothing to compare against, so
     * the very first call seeds the stored value from [currentVersionCode]
     * rather than reporting an update.
     *
     * BitChord ships sideloaded (see [com.music.flux.data.AppUpdateChecker]),
     * so installing a new APK over the old one is the only "update" there is —
     * app data, this pref included, survives it exactly like a Play Store
     * update. Call once per process start, before anything reads a cache that
     * an update should invalidate.
     */
    fun consumeVersionUpdate(currentVersionCode: Int): Boolean {
        val last = prefs.getInt(KEY_LAST_VERSION_CODE, currentVersionCode)
        if (last != currentVersionCode) {
            prefs.edit().putInt(KEY_LAST_VERSION_CODE, currentVersionCode).apply()
        }
        return currentVersionCode > last
    }

    /**
     * A ceiling saved when there was only one applies to both connections.
     * Someone who picked Low to protect a data plan would not thank us for
     * quietly putting Wi-Fi *and* mobile back on High.
     */
    private fun migrateSingleQuality() {
        val legacy = prefs.getString(KEY_QUALITY_LEGACY, null) ?: return
        prefs.edit()
            .putString(KEY_QUALITY_WIFI, legacy)
            .putString(KEY_QUALITY_CELLULAR, legacy)
            .remove(KEY_QUALITY_LEGACY)
            .apply()
    }

    private fun readQuality(key: String): AudioQuality {
        val stored = prefs.getString(key, null) ?: return AudioQuality.HIGH
        return runCatching { AudioQuality.valueOf(stored) }.getOrDefault(AudioQuality.HIGH)
    }

    /**
     * Write down what the download path was already doing, before it starts
     * being asked instead.
     *
     * Download quality used to be derived rather than chosen: a lossless copy
     * was kept when `SourceResolver.requestForNow()` said Lossless, which meant
     * a download quietly turned on the lossless preference and off again with
     * it. Someone who switched that off on the Sources screen was getting AAC
     * downloads on purpose, and defaulting them to Lossless now would answer a
     * question they had already answered — with thirty-five megabytes a track.
     *
     * The ceilings are deliberately *not* consulted. They were only in that
     * derivation because there was nowhere else to say "not on mobile data",
     * and [wifiOnlyDownloads] is now where that is said.
     */
    private fun migrateDownloadQuality() {
        if (prefs.contains(KEY_QUALITY_DOWNLOAD)) return
        // Was derived from the old `losslessAudio` switch, which defaulted to
        // on; LOSSLESS is what that produced for all but the few installs that
        // had turned it off, and is the default a fresh install gets anyway.
        prefs.edit().putString(KEY_QUALITY_DOWNLOAD, DownloadQuality.LOSSLESS.name).apply()
    }

    private fun readDownloadQuality(): DownloadQuality {
        val stored = prefs.getString(KEY_QUALITY_DOWNLOAD, null) ?: return DownloadQuality.LOSSLESS
        return runCatching { DownloadQuality.valueOf(stored) }.getOrDefault(DownloadQuality.LOSSLESS)
    }

    /**
     * Track the active network so [effectiveAudioQuality] can answer without
     * touching ConnectivityManager. Stream resolution happens off the main
     * thread mid-playback; a callback keeps that lookup off the hot path and
     * lets the settings page show which ceiling is currently in force.
     */
    private fun watchConnection(context: Context) {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val refresh = {
            meteredConnection.value = runCatching {
                if (manager.activeNetwork == null) null else manager.isActiveNetworkMetered
            }.getOrNull()
        }
        refresh()
        runCatching {
            manager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = refresh()
                    override fun onLost(network: Network) = refresh()
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) = refresh()
                },
            )
        }
    }

    fun setAutoplay(value: Boolean) {
        autoplay.value = value
        prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
    }

    fun setAudioQualityWifi(value: AudioQuality) {
        audioQualityWifi.value = value
        prefs.edit().putString(KEY_QUALITY_WIFI, value.name).apply()
    }

    fun setAudioQualityCellular(value: AudioQuality) {
        audioQualityCellular.value = value
        prefs.edit().putString(KEY_QUALITY_CELLULAR, value.name).apply()
    }

    fun setDownloadQuality(value: DownloadQuality) {
        downloadQuality.value = value
        prefs.edit().putString(KEY_QUALITY_DOWNLOAD, value.name).apply()
    }

    fun setWifiOnlyDownloads(value: Boolean) {
        wifiOnlyDownloads.value = value
        prefs.edit().putBoolean(KEY_WIFI_ONLY_DOWNLOADS, value).apply()
    }

    fun setCrossfadeSeconds(value: Int) {
        crossfadeSeconds.value = value
        prefs.edit().putInt(KEY_CROSSFADE, value).apply()
    }

    fun setSmartFadeEnabled(value: Boolean) {
        smartFadeEnabled.value = value
        prefs.edit().putBoolean(KEY_SMART_FADE, value).apply()
    }

    fun setSkipSilence(value: Boolean) {
        skipSilence.value = value
        prefs.edit().putBoolean(KEY_SKIP_SILENCE, value).apply()
    }

    fun setSpatialAudio(value: Boolean) {
        spatialAudio.value = value
        prefs.edit().putBoolean(KEY_SPATIAL_AUDIO, value).apply()
    }

    fun setPlaybackSpeed(value: Float) {
        playbackSpeed.value = value
        prefs.edit().putFloat(KEY_SPEED, value).apply()
    }

    fun setShowNerdStats(value: Boolean) {
        showNerdStats.value = value
        prefs.edit().putBoolean(KEY_NERD_STATS, value).apply()
    }

    fun setThemeMode(value: ThemeMode) {
        themeMode.value = value
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    fun setReduceAnimation(value: Boolean) {
        reduceAnimation.value = value
        prefs.edit().putBoolean(KEY_REDUCE_ANIMATION, value).apply()
    }

    fun setStopOnTaskRemoved(value: Boolean) {
        stopOnTaskRemoved.value = value
        prefs.edit().putBoolean(KEY_STOP_ON_TASK_REMOVED, value).apply()
    }

    fun setHideVolumeBar(value: Boolean) {
        hideVolumeBar.value = value
        prefs.edit().putBoolean(KEY_HIDE_VOLUME_BAR, value).apply()
    }

    fun setSwipeToPlayNext(value: Boolean) {
        swipeToPlayNext.value = value
        prefs.edit().putBoolean(KEY_SWIPE_TO_PLAY_NEXT, value).apply()
    }

    fun setResumeOnBluetooth(value: Boolean) {
        resumeOnBluetooth.value = value
        prefs.edit().putBoolean(KEY_RESUME_ON_BLUETOOTH, value).apply()
    }

    fun setDontRepeatSuggestions(value: Boolean) {
        dontRepeatSuggestions.value = value
        prefs.edit().putBoolean(KEY_DONT_REPEAT_SUGGESTIONS, value).apply()
    }

    fun setConvertVideoToAudio(value: Boolean) {
        convertVideoToAudio.value = value
        prefs.edit().putBoolean(KEY_CONVERT_VIDEO_TO_AUDIO, value).apply()
    }

    fun setReduceDynamicBlur(value: Boolean) {
        reduceDynamicBlur.value = value
        prefs.edit().putBoolean(KEY_REDUCE_BLUR, value).apply()
    }

    fun setDynamicTheme(value: Boolean) {
        dynamicTheme.value = value
        prefs.edit().putBoolean(KEY_DYNAMIC_THEME, value).apply()
    }

    fun setEnableHighRefreshRate(value: Boolean) {
        enableHighRefreshRate.value = value
        prefs.edit().putBoolean(KEY_ENABLE_HIGH_REFRESH_RATE, value).apply()
    }

    fun setDensityScale(value: Float) {
        densityScale.value = value
        prefs.edit().putFloat(KEY_DENSITY_SCALE, value).apply()
    }

    fun setSliderStyle(value: SliderStyle) {
        sliderStyle.value = value
        prefs.edit().putString(KEY_SLIDER_STYLE, value.name).apply()
    }

    fun setSquigglySlider(value: Boolean) {
        squigglySlider.value = value
        prefs.edit().putBoolean(KEY_SQUIGGLY_SLIDER, value).apply()
    }

    fun setPlayerBackgroundStyle(value: PlayerBackgroundStyle) {
        playerBackgroundStyle.value = value
        prefs.edit().putString(KEY_PLAYER_BACKGROUND_STYLE, value.name).apply()
    }

    fun setMiniPlayerBackgroundStyle(value: MiniPlayerBackgroundStyle) {
        miniPlayerBackgroundStyle.value = value
        prefs.edit().putString(KEY_MINI_PLAYER_BACKGROUND_STYLE, value.name).apply()
    }

    fun setPlayerButtonsStyle(value: PlayerButtonsStyle) {
        playerButtonsStyle.value = value
        prefs.edit().putString(KEY_PLAYER_BUTTONS_STYLE, value.name).apply()
    }

    fun setHidePlayerThumbnail(value: Boolean) {
        hidePlayerThumbnail.value = value
        prefs.edit().putBoolean(KEY_HIDE_PLAYER_THUMBNAIL, value).apply()
    }

    fun setCropAlbumArt(value: Boolean) {
        cropAlbumArt.value = value
        prefs.edit().putBoolean(KEY_CROP_ALBUM_ART, value).apply()
    }

    fun setHideStatusBarOnFullscreen(value: Boolean) {
        hideStatusBarOnFullscreen.value = value
        prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR_ON_FULLSCREEN, value).apply()
    }

    fun setSwipeThumbnail(value: Boolean) {
        swipeThumbnail.value = value
        prefs.edit().putBoolean(KEY_SWIPE_THUMBNAIL, value).apply()
    }

    fun setSwipeSensitivity(value: Float) {
        swipeSensitivity.value = value
        prefs.edit().putFloat(KEY_SWIPE_SENSITIVITY, value).apply()
    }

    fun setLyricsPosition(value: LyricsPosition) {
        lyricsPosition.value = value
        prefs.edit().putString(KEY_LYRICS_POSITION, value.name).apply()
    }

    fun setLyricsAnimationStyle(value: LyricsAnimationStyle) {
        lyricsAnimationStyle.value = value
        prefs.edit().putString(KEY_LYRICS_ANIMATION_STYLE, value.name).apply()
    }

    fun setLyricsGlowEffect(value: Boolean) {
        lyricsGlowEffect.value = value
        prefs.edit().putBoolean(KEY_LYRICS_GLOW_EFFECT, value).apply()
    }

    fun setLyricsTextSize(value: Float) {
        lyricsTextSize.value = value
        prefs.edit().putFloat(KEY_LYRICS_TEXT_SIZE, value).apply()
    }

    fun setLyricsLineSpacing(value: Float) {
        lyricsLineSpacing.value = value
        prefs.edit().putFloat(KEY_LYRICS_LINE_SPACING, value).apply()
    }

    fun setLyricsClickSeek(value: Boolean) {
        lyricsClickSeek.value = value
        prefs.edit().putBoolean(KEY_LYRICS_CLICK_SEEK, value).apply()
    }

    fun setLyricsAutoScroll(value: Boolean) {
        lyricsAutoScroll.value = value
        prefs.edit().putBoolean(KEY_LYRICS_AUTO_SCROLL, value).apply()
    }

    fun setRespectAgentPositioning(value: Boolean) {
        respectAgentPositioning.value = value
        prefs.edit().putBoolean(KEY_RESPECT_AGENT_POSITIONING, value).apply()
    }

    fun setDefaultOpenTab(value: Int) {
        defaultOpenTab.value = value
        prefs.edit().putInt(KEY_DEFAULT_OPEN_TAB, value).apply()
    }

    fun setGridItemSize(value: GridItemSize) {
        gridItemSize.value = value
        prefs.edit().putString(KEY_GRID_ITEM_SIZE, value.name).apply()
    }

    fun setSlimNavBar(value: Boolean) {
        slimNavBar.value = value
        prefs.edit().putBoolean(KEY_SLIM_NAV_BAR, value).apply()
    }

    fun setShowRecognizeButton(value: Boolean) {
        showRecognizeButton.value = value
        prefs.edit().putBoolean(KEY_SHOW_RECOGNIZE_BUTTON, value).apply()
    }

    fun setShowPlayRandomButton(value: Boolean) {
        showPlayRandomButton.value = value
        prefs.edit().putBoolean(KEY_SHOW_PLAY_RANDOM_BUTTON, value).apply()
    }

    fun setShowLikedPlaylist(value: Boolean) {
        showLikedPlaylist.value = value
        prefs.edit().putBoolean(KEY_SHOW_LIKED_PLAYLIST, value).apply()
    }

    fun setShowDownloadedPlaylist(value: Boolean) {
        showDownloadedPlaylist.value = value
        prefs.edit().putBoolean(KEY_SHOW_DOWNLOADED_PLAYLIST, value).apply()
    }

    fun setShowTopPlaylist(value: Boolean) {
        showTopPlaylist.value = value
        prefs.edit().putBoolean(KEY_SHOW_TOP_PLAYLIST, value).apply()
    }

    fun setShowCachedPlaylist(value: Boolean) {
        showCachedPlaylist.value = value
        prefs.edit().putBoolean(KEY_SHOW_CACHED_PLAYLIST, value).apply()
    }

    fun setShowUploadedPlaylist(value: Boolean) {
        showUploadedPlaylist.value = value
        prefs.edit().putBoolean(KEY_SHOW_UPLOADED_PLAYLIST, value).apply()
    }

    fun setSyncedLyrics(value: Boolean) {
        syncedLyrics.value = value
        prefs.edit().putBoolean(KEY_SYNCED_LYRICS, value).apply()
    }

    fun setLyricsSources(value: Set<LyricsSource>) {
        lyricsSources.value = value
        prefs.edit().putString(KEY_LYRICS_SOURCES, value.joinToString(",") { it.name }).apply()
    }

    /**
     * Stored as a joined list of names rather than a string set: a name that
     * no longer exists — a source dropped in a later build — has to fall out
     * quietly, and the default when nothing has been saved is "all of them",
     * which a missing key and an empty set would otherwise be unable to tell
     * apart.
     */
    private fun readLyricsSources(): Set<LyricsSource> {
        val stored = prefs.getString(KEY_LYRICS_SOURCES, null)
            ?: return LyricsSource.entries.toSet()
        return stored.split(",")
            .mapNotNull { name -> LyricsSource.entries.firstOrNull { it.name == name } }
            .toSet()
    }

    fun setLyricsSourceOrder(value: List<LyricsSource>) {
        lyricsSourceOrder.value = value
        prefs.edit().putString(KEY_LYRICS_SOURCE_ORDER, value.joinToString(",") { it.name }).apply()
    }

    /**
     * A named source dropped from the stored order — an upgrade reordered
     * since it was saved — falls out on read; one added since is appended, in
     * [LyricsSource]'s own declared order, so a fresh install and an upgraded
     * one agree on where a new source lands until the user says otherwise.
     */
    private fun readLyricsSourceOrder(): List<LyricsSource> {
        val stored = prefs.getString(KEY_LYRICS_SOURCE_ORDER, null)
            ?: return LyricsSource.entries
        val saved = stored.split(",")
            .mapNotNull { name -> LyricsSource.entries.firstOrNull { it.name == name } }
        return saved + LyricsSource.entries.filter { it !in saved }
    }

    fun setPrioritizeSyllableSync(value: Boolean) {
        prioritizeSyllableSync.value = value
        prefs.edit().putBoolean(KEY_PRIORITIZE_SYLLABLE_SYNC, value).apply()
    }

    /**
     * Puts the source list, its order and [prioritizeSyllableSync] back the
     * way a fresh install finds them. [syncedLyrics] itself is left alone —
     * this is "start over on *which* lyrics", not "turn lyrics off".
     */
    fun resetLyricsSourceSettings() {
        setLyricsSources(LyricsSource.entries.toSet())
        setLyricsSourceOrder(LyricsSource.entries)
        setPrioritizeSyllableSync(false)
    }

    fun setAnimatedCanvas(value: Boolean) {
        animatedCanvas.value = value
        prefs.edit().putBoolean(KEY_ANIMATED_CANVAS, value).apply()
    }

    fun setCanvasOverCellular(value: Boolean) {
        canvasOverCellular.value = value
        prefs.edit().putBoolean(KEY_CANVAS_OVER_CELLULAR, value).apply()
    }

    fun setFullBleedArtwork(value: Boolean) {
        fullBleedArtwork.value = value
        prefs.edit().putBoolean(KEY_FULL_BLEED_ARTWORK, value).apply()
    }

    /** Clamped to [DEFAULT_CACHE_LIMIT_BYTES]..[MAX_CACHE_LIMIT_BYTES] — the floor is the default, not zero. */
    fun setAudioCacheLimitBytes(value: Long) {
        val clamped = value.coerceIn(DEFAULT_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        audioCacheLimitBytes.value = clamped
        prefs.edit().putLong(KEY_CACHE_LIMIT, clamped).apply()
    }

    fun setLastfmEnabled(value: Boolean) {
        lastfmEnabled.value = value
        prefs.edit().putBoolean(KEY_LASTFM_ENABLED, value).apply()
    }

    fun setLastfmUsername(value: String) {
        lastfmUsername.value = value
        prefs.edit().putString(KEY_LASTFM_USERNAME, value).apply()
    }

    fun setLastfmSessionKey(value: String) {
        lastfmSessionKey.value = value
        prefs.edit().putString(KEY_LASTFM_SESSION_KEY, value).apply()
    }

    fun setLastfmApiKey(value: String) {
        lastfmApiKey.value = value
        prefs.edit().putString(KEY_LASTFM_API_KEY, value).apply()
    }

    fun setLastfmSecret(value: String) {
        lastfmSecret.value = value
        prefs.edit().putString(KEY_LASTFM_SECRET, value).apply()
    }

    fun setLastfmEndpoint(value: String) {
        lastfmEndpoint.value = value
        prefs.edit().putString(KEY_LASTFM_ENDPOINT, value).apply()
    }

    fun setSpotifySpdcToken(value: String) {
        spotifySpdcToken.value = value
        prefs.edit().putString(KEY_SPOTIFY_SPDC_TOKEN, value).apply()
    }

    fun setLastfmScrobbleEnabled(value: Boolean) {
        lastfmScrobbleEnabled.value = value
        if (!value) lastfmNowPlaying.value = false
        prefs.edit()
            .putBoolean(KEY_LASTFM_SCROBBLE_ENABLED, value)
            .putBoolean(KEY_LASTFM_NOW_PLAYING, if (value) lastfmNowPlaying.value else false)
            .apply()
    }

    fun setLastfmNowPlaying(value: Boolean) {
        if (!lastfmScrobbleEnabled.value && value) return
        lastfmNowPlaying.value = value
        prefs.edit().putBoolean(KEY_LASTFM_NOW_PLAYING, value).apply()
    }

    fun setScrobbleMinDuration(value: Int) {
        scrobbleMinDuration.value = value
        prefs.edit().putInt(KEY_SCROBBLE_MIN_DURATION, value).apply()
    }

    fun setScrobbleDelayPercent(value: Float) {
        scrobbleDelayPercent.value = value
        prefs.edit().putFloat(KEY_SCROBBLE_DELAY_PERCENT, value).apply()
    }

    fun setScrobbleDelaySeconds(value: Int) {
        scrobbleDelaySeconds.value = value
        prefs.edit().putInt(KEY_SCROBBLE_DELAY_SECONDS, value).apply()
    }

    fun setListenBrainzEnabled(value: Boolean) {
        listenBrainzEnabled.value = value
        prefs.edit().putBoolean(KEY_LISTENBRAINZ_ENABLED, value).apply()
    }

    fun setListenBrainzToken(value: String) {
        listenBrainzToken.value = value
        prefs.edit().putString(KEY_LISTENBRAINZ_TOKEN, value).apply()
    }

    /** Writes through to the encrypted store; pass "" to disconnect. */
    fun setDiscordToken(value: String) {
        discordToken.value = value
        authStore.discordToken = value.ifEmpty { null }
    }

    fun setDiscordAccount(username: String, name: String, avatar: String?) {
        discordUsername.value = username
        discordName.value = name
        discordAvatar.value = avatar.orEmpty()
        prefs.edit()
            .putString(KEY_DISCORD_USERNAME, username)
            .putString(KEY_DISCORD_NAME, name)
            .putString(KEY_DISCORD_AVATAR, avatar.orEmpty())
            .apply()
    }

    fun setDiscordRpcEnabled(value: Boolean) {
        discordRpcEnabled.value = value
        prefs.edit().putBoolean(KEY_DISCORD_RPC_ENABLED, value).apply()
    }

    fun setDiscordUseDetails(value: Boolean) {
        discordUseDetails.value = value
        prefs.edit().putBoolean(KEY_DISCORD_USE_DETAILS, value).apply()
    }

    fun setDiscordAdvancedMode(value: Boolean) {
        discordAdvancedMode.value = value
        prefs.edit().putBoolean(KEY_DISCORD_ADVANCED_MODE, value).apply()
    }

    fun setDiscordStatus(value: String) {
        discordStatus.value = value
        prefs.edit().putString(KEY_DISCORD_STATUS, value).apply()
    }

    fun setDiscordActivityType(value: String) {
        discordActivityType.value = value
        prefs.edit().putString(KEY_DISCORD_ACTIVITY_TYPE, value).apply()
    }

    fun setDiscordActivityName(value: String) {
        discordActivityName.value = value
        prefs.edit().putString(KEY_DISCORD_ACTIVITY_NAME, value).apply()
    }

    fun setDiscordButton1Text(value: String) {
        discordButton1Text.value = value
        prefs.edit().putString(KEY_DISCORD_BUTTON_1_TEXT, value).apply()
    }

    fun setDiscordButton1Visible(value: Boolean) {
        discordButton1Visible.value = value
        prefs.edit().putBoolean(KEY_DISCORD_BUTTON_1_VISIBLE, value).apply()
    }

    fun setDiscordButton2Text(value: String) {
        discordButton2Text.value = value
        prefs.edit().putString(KEY_DISCORD_BUTTON_2_TEXT, value).apply()
    }

    fun setDiscordButton2Visible(value: Boolean) {
        discordButton2Visible.value = value
        prefs.edit().putBoolean(KEY_DISCORD_BUTTON_2_VISIBLE, value).apply()
    }

    fun setDiscordInfoDismissed(value: Boolean) {
        discordInfoDismissed.value = value
        prefs.edit().putBoolean(KEY_DISCORD_INFO_DISMISSED, value).apply()
    }

    fun setReplayGenres(value: Boolean) {
        replayGenres.value = value
        prefs.edit().putBoolean(KEY_REPLAY_GENRES, value).apply()
    }

    /**
     * Pins or unpins [browseId], returning whether it is pinned afterwards.
     *
     * Pinning past [MAX_PINNED_PLAYLISTS] is refused rather than evicting the
     * oldest pin: a silent swap would mean a playlist someone pinned on purpose
     * disappears from the row without them ever having touched it, the moment
     * they pin a sixth. Unpinning always succeeds.
     */
    fun togglePinnedPlaylist(browseId: String): Boolean {
        val current = pinnedPlaylists.value
        val updated = when {
            browseId in current -> current - browseId
            current.size >= MAX_PINNED_PLAYLISTS -> return false
            else -> current + browseId
        }
        pinnedPlaylists.value = updated
        prefs.edit().putString(KEY_PINNED_PLAYLISTS, updated.joinToString(",")).apply()
        return browseId in updated
    }

    private fun readPinnedPlaylists(): List<String> {
        val stored = prefs.getString(KEY_PINNED_PLAYLISTS, null) ?: return emptyList()
        return stored.split(",").filter { it.isNotBlank() }
    }

    /** Forgets the account: token and cached profile. */
    fun clearDiscordAccount() {
        setDiscordToken("")
        setDiscordAccount("", "", null)
    }

    // ── Backup ──────────────────────────────────────────────────────────────

    /**
     * Every stored preference, for an export.
     *
     * Read off the preference file wholesale rather than assembled from the
     * flows above, so a setting added in a later build is in the backup the day
     * it is added instead of the day somebody remembers to list it here. What is
     * *left out* is therefore the part worth stating explicitly, and it is
     * [SECRETS]: an export is a file the user is about to put in Drive or a
     * chat, and a scrobbler session key or an API secret in it is a credential
     * that has left the device in plain text. Signing back in after a restore is
     * a minute; a leaked session key is not recoverable at all.
     *
     * The Discord token is not here for the same reason and one more: it never
     * reaches this file. It lives in the encrypted store — see [AuthStore] — and
     * so does the YouTube cookie, which means neither can be exported by
     * accident.
     */
    fun exportPrefs(): Map<String, Any?> {
        if (!this::prefs.isInitialized) return emptyMap()
        return prefs.all.filterKeys { it !in SECRETS && it !in DEVICE_LOCAL }
    }

    fun setListenTogetherUsername(value: String) {
        listenTogetherUsername.value = value
        prefs.edit().putString(KEY_LISTEN_TOGETHER_USERNAME, value).apply()
    }

    fun setListenTogetherServerUrl(value: String) {
        listenTogetherServerUrl.value = value
        prefs.edit().putString(KEY_LISTEN_TOGETHER_SERVER_URL, value).apply()
    }

    fun setListenTogetherAutoApproval(value: Boolean) {
        listenTogetherAutoApproval.value = value
        prefs.edit().putBoolean(KEY_LISTEN_TOGETHER_AUTO_APPROVAL, value).apply()
    }

    fun setListenTogetherAutoApproveSuggestions(value: Boolean) {
        listenTogetherAutoApproveSuggestions.value = value
        prefs.edit().putBoolean(KEY_LISTEN_TOGETHER_AUTO_APPROVE_SUGGESTIONS, value).apply()
    }

    fun setListenTogetherSyncVolume(value: Boolean) {
        listenTogetherSyncVolume.value = value
        prefs.edit().putBoolean(KEY_LISTEN_TOGETHER_SYNC_VOLUME, value).apply()
    }

    fun setListenTogetherSession(token: String, roomCode: String, userId: String, isHost: Boolean, timestamp: Long) {
        listenTogetherSessionToken.value = token
        listenTogetherRoomCode.value = roomCode
        listenTogetherUserId.value = userId
        listenTogetherIsHost.value = isHost
        listenTogetherSessionTimestamp.value = timestamp
        prefs.edit()
            .putString(KEY_LISTEN_TOGETHER_SESSION_TOKEN, token)
            .putString(KEY_LISTEN_TOGETHER_ROOM_CODE, roomCode)
            .putString(KEY_LISTEN_TOGETHER_USER_ID, userId)
            .putBoolean(KEY_LISTEN_TOGETHER_IS_HOST, isHost)
            .putLong(KEY_LISTEN_TOGETHER_SESSION_TIMESTAMP, timestamp)
            .apply()
    }

    fun clearListenTogetherSession() {
        setListenTogetherSession("", "", "", false, 0L)
    }

    /**
     * Replaces the preference file with [values] and re-reads it.
     *
     * A replace, not a merge: a partial restore leaves a device holding half of
     * one configuration and half of another, which is the one outcome nobody
     * asked for. Keys in [SECRETS] survive untouched — they were never in the
     * file being restored from, and clearing them would sign the user out of
     * services the backup has nothing to say about.
     */
    fun importPrefs(values: Map<String, Any?>) {
        if (!this::prefs.isInitialized) return
        val kept = prefs.all.filterKeys { it in SECRETS || it in DEVICE_LOCAL }
        prefs.edit().apply {
            clear()
            val incoming = values.filterKeys { it !in SECRETS && it !in DEVICE_LOCAL }
            (kept + incoming).forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    else -> Unit
                }
            }
        }.apply()
        reload()
    }

    /**
     * Preferences an export must not carry — credentials, not configuration.
     * See [exportPrefs].
     */
    private val SECRETS = setOf(
        KEY_LASTFM_SESSION_KEY,
        KEY_LASTFM_API_KEY,
        KEY_LASTFM_SECRET,
        KEY_LISTENBRAINZ_TOKEN,
    )

    /**
     * Preferences that describe *this device* rather than this configuration,
     * and so are neither exported nor overwritten by an import.
     *
     * [Downloads][com.music.flux.download.Downloads] keeps its record of
     * what is saved in this same preference file, and that record is a list of
     * files on this phone's storage. Carrying it into a backup would restore a
     * folder full of tracks that are not here; clearing it on import would leave
     * the files on disk with nothing pointing at them, which is worse — the
     * Downloads page would read as empty while the space stayed used.
     */
    private val DEVICE_LOCAL = setOf(
        "downloaded_tracks",
        "downloaded_tracks_metadata",
        "downloaded_collections",
        KEY_LAST_VERSION_CODE,
    )

    const val DEFAULT_CACHE_LIMIT_BYTES = 512L * 1024 * 1024
    const val MAX_CACHE_LIMIT_BYTES = 10L * 1024 * 1024 * 1024

    private const val KEY_QUALITY_LEGACY = "audio_quality"
    private const val KEY_QUALITY_WIFI = "audio_quality_wifi"
    private const val KEY_QUALITY_CELLULAR = "audio_quality_cellular"
    private const val KEY_QUALITY_DOWNLOAD = "audio_quality_download"
    private const val KEY_WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"
    private const val KEY_LOSSLESS = "lossless_audio"
    private const val KEY_CROSSFADE = "crossfade_seconds"
    private const val KEY_SMART_FADE = "smart_fade_enabled"
    private const val KEY_SKIP_SILENCE = "skip_silence"
    private const val KEY_SPATIAL_AUDIO = "spatial_audio"
    private const val KEY_SPEED = "playback_speed"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_AUTOPLAY = "autoplay"
    private const val KEY_NERD_STATS = "show_nerd_stats"
    private const val KEY_CACHE_LIMIT = "audio_cache_limit_bytes"
    private const val KEY_REDUCE_ANIMATION = "reduce_animation"
    private const val KEY_STOP_ON_TASK_REMOVED = "stop_on_task_removed"
    private const val KEY_HIDE_VOLUME_BAR = "hide_volume_bar"
    private const val KEY_SWIPE_TO_PLAY_NEXT = "swipe_to_play_next"
    private const val KEY_RESUME_ON_BLUETOOTH = "resume_on_bluetooth"
    private const val KEY_DONT_REPEAT_SUGGESTIONS = "dont_repeat_suggestions"
    private const val KEY_CONVERT_VIDEO_TO_AUDIO = "convert_video_to_audio"
    private const val KEY_REDUCE_BLUR = "reduce_dynamic_blur"
    private const val KEY_ANIMATED_CANVAS = "animated_canvas"
    private const val KEY_CANVAS_OVER_CELLULAR = "canvas_over_cellular"
    private const val KEY_FULL_BLEED_ARTWORK = "full_bleed_artwork"
    private const val KEY_SYNCED_LYRICS = "synced_lyrics"
    private const val KEY_LYRICS_SOURCES = "lyrics_sources"
    private const val KEY_LYRICS_SOURCE_ORDER = "lyrics_source_order"
    private const val KEY_PRIORITIZE_SYLLABLE_SYNC = "prioritize_syllable_sync"
    private const val KEY_REPLAY_GENRES = "replay_genres"
    private const val KEY_PINNED_PLAYLISTS = "pinned_playlists"

    private const val KEY_DYNAMIC_THEME = "dynamic_theme"
    private const val KEY_ENABLE_HIGH_REFRESH_RATE = "enable_high_refresh_rate"
    private const val KEY_DENSITY_SCALE = "density_scale_factor"
    private const val KEY_SLIDER_STYLE = "slider_style"
    private const val KEY_SQUIGGLY_SLIDER = "squiggly_slider"
    private const val KEY_PLAYER_BACKGROUND_STYLE = "player_background_style"
    private const val KEY_MINI_PLAYER_BACKGROUND_STYLE = "mini_player_background_style"
    private const val KEY_PLAYER_BUTTONS_STYLE = "player_buttons_style"
    private const val KEY_HIDE_PLAYER_THUMBNAIL = "hide_player_thumbnail"
    private const val KEY_CROP_ALBUM_ART = "crop_album_art"
    private const val KEY_HIDE_STATUS_BAR_ON_FULLSCREEN = "hide_status_bar_on_fullscreen"
    private const val KEY_SWIPE_THUMBNAIL = "swipe_thumbnail"
    private const val KEY_SWIPE_SENSITIVITY = "swipe_sensitivity"
    private const val KEY_LYRICS_POSITION = "lyrics_position"
    private const val KEY_LYRICS_ANIMATION_STYLE = "lyrics_animation_style"
    private const val KEY_LYRICS_GLOW_EFFECT = "lyrics_glow_effect"
    private const val KEY_LYRICS_TEXT_SIZE = "lyrics_text_size"
    private const val KEY_LYRICS_LINE_SPACING = "lyrics_line_spacing"
    private const val KEY_LYRICS_CLICK_SEEK = "lyrics_click_seek"
    private const val KEY_LYRICS_AUTO_SCROLL = "lyrics_auto_scroll"
    private const val KEY_RESPECT_AGENT_POSITIONING = "respect_agent_positioning"
    private const val KEY_DEFAULT_OPEN_TAB = "default_open_tab"
    private const val KEY_GRID_ITEM_SIZE = "grid_item_size"
    private const val KEY_SLIM_NAV_BAR = "slim_nav_bar"
    private const val KEY_SHOW_RECOGNIZE_BUTTON = "show_recognize_button"
    private const val KEY_SHOW_PLAY_RANDOM_BUTTON = "show_play_random_button"
    private const val KEY_SHOW_LIKED_PLAYLIST = "show_liked_playlist"
    private const val KEY_SHOW_DOWNLOADED_PLAYLIST = "show_downloaded_playlist"
    private const val KEY_SHOW_TOP_PLAYLIST = "show_top_playlist"
    private const val KEY_SHOW_CACHED_PLAYLIST = "show_cached_playlist"
    private const val KEY_SHOW_UPLOADED_PLAYLIST = "show_uploaded_playlist"

    private const val KEY_LASTFM_ENABLED = "lastfm_enabled"
    private const val KEY_LASTFM_USERNAME = "lastfm_username"
    private const val KEY_LASTFM_SESSION_KEY = "lastfm_session_key"
    private const val KEY_LASTFM_API_KEY = "lastfm_api_key"
    private const val KEY_LASTFM_SECRET = "lastfm_secret"
    private const val KEY_LASTFM_ENDPOINT = "lastfm_endpoint"
    private const val KEY_LASTFM_SCROBBLE_ENABLED = "lastfm_scrobble_enabled"
    private const val KEY_LASTFM_NOW_PLAYING = "lastfm_now_playing"
    private const val KEY_SCROBBLE_MIN_DURATION = "scrobble_min_duration"
    private const val KEY_SCROBBLE_DELAY_PERCENT = "scrobble_delay_percent"
    private const val KEY_SCROBBLE_DELAY_SECONDS = "scrobble_delay_seconds"
    private const val KEY_LISTENBRAINZ_ENABLED = "listenbrainz_enabled"
    private const val KEY_LISTENBRAINZ_TOKEN = "listenbrainz_token"
    private const val KEY_SPOTIFY_SPDC_TOKEN = "spotify_spdc_token"

    private const val KEY_DISCORD_USERNAME = "discord_username"
    private const val KEY_DISCORD_NAME = "discord_name"
    private const val KEY_DISCORD_AVATAR = "discord_avatar"
    private const val KEY_DISCORD_RPC_ENABLED = "discord_rpc_enabled"
    private const val KEY_DISCORD_USE_DETAILS = "discord_use_details"
    private const val KEY_DISCORD_ADVANCED_MODE = "discord_advanced_mode"
    private const val KEY_DISCORD_STATUS = "discord_status"
    private const val KEY_DISCORD_ACTIVITY_TYPE = "discord_activity_type"
    private const val KEY_DISCORD_ACTIVITY_NAME = "discord_activity_name"
    private const val KEY_DISCORD_BUTTON_1_TEXT = "discord_button_1_text"
    private const val KEY_DISCORD_BUTTON_1_VISIBLE = "discord_button_1_visible"
    private const val KEY_DISCORD_BUTTON_2_TEXT = "discord_button_2_text"
    private const val KEY_DISCORD_BUTTON_2_VISIBLE = "discord_button_2_visible"
    private const val KEY_DISCORD_INFO_DISMISSED = "discord_info_dismissed"
    private const val KEY_LISTEN_TOGETHER_USERNAME = "listen_together_username"
    private const val KEY_LISTEN_TOGETHER_SERVER_URL = "listen_together_server_url"
    private const val KEY_LISTEN_TOGETHER_AUTO_APPROVAL = "listen_together_auto_approval"
    private const val KEY_LISTEN_TOGETHER_AUTO_APPROVE_SUGGESTIONS = "listen_together_auto_approve_suggestions"
    private const val KEY_LISTEN_TOGETHER_SYNC_VOLUME = "listen_together_sync_volume"
    private const val KEY_LISTEN_TOGETHER_SESSION_TOKEN = "listen_together_session_token"
    private const val KEY_LISTEN_TOGETHER_ROOM_CODE = "listen_together_room_code"
    private const val KEY_LISTEN_TOGETHER_USER_ID = "listen_together_user_id"
    private const val KEY_LISTEN_TOGETHER_IS_HOST = "listen_together_is_host"
    private const val KEY_LISTEN_TOGETHER_SESSION_TIMESTAMP = "listen_together_session_timestamp"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"
}

/**
 * Where one track stands in Automix's analysis.
 *
 * The three no-result states are kept apart because they call for different
 * reactions: [WAITING] resolves itself once bytes arrive, [ANALYSING] resolves
 * itself in a few seconds, and [FAILED] never resolves at all. From outside
 * they look identical, which is precisely why the line has to say which.
 */
enum class TrackAnalysisState {
    /** Nothing in flight and no result — usually waiting on bytes to arrive. */
    WAITING,

    /** Decode and inference running now; a result is a few seconds away. */
    ANALYSING,

    /** Measured, with a tempo the planner can actually use. */
    ANALYSED,

    /**
     * Measured off the track's opening, with the whole-track pass running now to
     * replace those numbers with better ones.
     *
     * Its own state rather than either neighbour, because it is genuinely both:
     * reporting [ANALYSING] made a track that was already usable look like it
     * had gone backwards, and reporting [ANALYSED] would hide that the cue and
     * the tempo are about to move.
     */
    REFINING,

    /**
     * Tried and came back with nothing usable — a decode error, or audio that
     * yielded no tempo. Distinct from [WAITING] because nothing further will
     * happen on its own: waiting is a matter of time, this is not.
     */
    FAILED,
}

/**
 * Both sides of the next transition, for stats for nerds.
 *
 * A transition needs *both* tracks measured before it can beat-match or cue the
 * incoming one into its arrangement, so reporting them separately is what makes
 * a plain crossfade explicable rather than mysterious.
 */
data class SmartAnalysis(
    val current: TrackAnalysisState = TrackAnalysisState.WAITING,
    val next: TrackAnalysisState = TrackAnalysisState.WAITING,
)

/**
 * A span of the playing track, in fractions of its duration, that the next
 * transition is planned to occupy.
 */
data class TransitionWindow(val start: Float, val end: Float)
