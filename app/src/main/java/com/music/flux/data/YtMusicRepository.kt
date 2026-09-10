package com.music.flux.data

import com.music.flux.data.DebugLog as Log
import com.music.flux.data.innertube.Innertube
import com.music.flux.data.innertube.InnertubeParser
import com.music.flux.data.model.Account
import com.music.flux.data.model.ArtistPage
import com.music.flux.data.model.HomeFeed
import com.music.flux.data.model.HomeShelf
import com.music.flux.data.model.LibraryPage
import com.music.flux.data.model.LibraryState
import com.music.flux.data.model.LikeStatus
import com.music.flux.data.model.PlaylistPrivacy
import com.music.flux.data.model.SearchFilter
import com.music.flux.data.model.SearchResult
import com.music.flux.data.model.ShelfItem
import com.music.flux.data.model.Song
import com.music.flux.data.model.SongMenu
import com.music.flux.data.model.UserPlaylist
import com.music.flux.data.history.PlaybackHistory
import com.music.flux.data.settings.AppSettings
import com.music.flux.data.settings.SearchHistory
import com.music.flux.data.LikeState
import com.music.flux.data.sources.TrackMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/** Suspend API over Innertube. Every call returns a Result so the UI can show a real error. */
object YtMusicRepository {

    private const val TAG = "FluxMusic"

    /**
     * The personalised feed, led by what was actually just played and padded
     * out with new releases.
     *
     * FEmusic_home alone is thin when signed out (three shelves), so extra
     * rows are pulled from FEmusic_new_releases, which carries genuinely
     * different content. Charts (Daily/Weekly, Trending) live under Explore
     * in the real app — see [explore] — not here. Titles are de-duped in
     * case the home feed already surfaced the same shelf.
     *
     * FEmusic_home's own continuation token comes back, for [moreHome] —
     * signed in, it keeps paging into mood mixes and more personalised
     * shelves the same way the official app does as you scroll; signed out
     * it's empty and there's nothing more to fetch.
     */
    private fun Song.toShelfItem(): ShelfItem = ShelfItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = null,
    )

    suspend fun home(): Result<HomeFeed> = call("home") {
        coroutineScope {
            val dynamicShelvesAsync = async { runCatching { buildMeldStyleDynamicShelves() }.getOrDefault(emptyList()) }
            val homeRaw = async { Innertube.browse("FEmusic_home") }
            val chartsRaw = async { runCatching { Innertube.browse("FEmusic_charts") }.getOrNull() }
            val newReleases = async { runCatching { shelvesOf("FEmusic_new_releases") }.getOrDefault(emptyList()) }

            val dynamicShelves = dynamicShelvesAsync.await()
            val home = homeRaw.await()
            val charts = chartsRaw.await()
            val homeShelves = InnertubeParser.parseHome(home)
            val chartShelves = charts?.let { InnertubeParser.parseHome(it) }.orEmpty()

            val shelves = dynamicShelves +
                homeShelves +
                chartShelves +
                newReleases.await()

            val token = InnertubeParser.continuationToken(home)
                ?: charts?.let { InnertubeParser.continuationToken(it) }
            HomeFeed(shelves, token)
        }
    }

    /**
     * Builds dynamic, continuously changing home shelves inspired by Meld:
     * - Shuffled Quick Picks with seed expansion
     * - Shuffled Daily Discover from random liked/history seeds
     * - Random "Similar to" recommendations (different artists/songs each refresh)
     * - Rotated Indian genres & trending hits
     * - Dynamic shelf ordering so content feels fresh on every refresh
     */
    private suspend fun buildMeldStyleDynamicShelves(): List<HomeShelf> = coroutineScope {
        val shelves = mutableListOf<HomeShelf>()
        val history = PlaybackHistory.recent.value
        val likedIds = LikeState.getLikedVideoIds()

        // 1. Quick Picks (Meld style: history + related + shuffled)
        val quickPicksAsync = async {
            val historySongs = history.shuffled().take(8).map { it.toSong() }
            val seed = history.shuffled().firstOrNull()?.videoId
            val relatedSongs = if (seed != null) {
                radio(seed).getOrNull().orEmpty().shuffled().take(10)
            } else emptyList()
            val fallback = if (historySongs.isEmpty() && relatedSongs.isEmpty()) {
                search("Top Hits India 2025", SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()?.map { it.song }.orEmpty().shuffled().take(16)
            } else emptyList()

            val combined = (historySongs + relatedSongs + fallback)
                .distinctBy { it.videoId }
                .shuffled()
                .take(16)

            if (combined.isNotEmpty()) {
                HomeShelf(
                    title = "Quick Picks",
                    subtitle = "Fresh mix based on your taste",
                    items = combined.map { it.toShelfItem() },
                )
            } else null
        }

        // 2. Daily Discover (Meld style: 3 random seeds -> related recommendations)
        val dailyDiscoverAsync = async {
            val candidateSeeds = (likedIds + history.map { it.videoId })
                .distinct()
                .shuffled()
                .take(3)
            val discoverSongs = mutableListOf<Song>()
            candidateSeeds.forEach { seedId ->
                val related = radio(seedId).getOrNull().orEmpty()
                    .filterNot { it.videoId == seedId }
                    .shuffled()
                    .take(5)
                discoverSongs.addAll(related)
            }
            if (discoverSongs.isEmpty()) {
                val fresh = search("New Hindi Songs 2025", SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()?.map { it.song }.orEmpty().shuffled().take(12)
                discoverSongs.addAll(fresh)
            }
            val deduped = discoverSongs.distinctBy { it.videoId }.shuffled().take(12)
            if (deduped.isNotEmpty()) {
                HomeShelf(
                    title = "Daily Discover",
                    subtitle = "New sounds you haven't heard yet",
                    items = deduped.map { it.toShelfItem() },
                )
            } else null
        }

        // 3. Dynamic "Similar to" & "Because you listen to" shelves (Random seeds each refresh)
        val similarShelvesAsync = async {
            val result = mutableListOf<HomeShelf>()
            val randomHistorySeeds = history.shuffled().distinctBy { it.artist }.take(2)
            randomHistorySeeds.forEach { item ->
                val related = radio(item.videoId).getOrNull().orEmpty()
                    .filterNot { it.videoId == item.videoId }
                    .shuffled()
                    .take(12)
                if (related.isNotEmpty()) {
                    result.add(
                        HomeShelf(
                            title = "Similar to ${item.title}",
                            subtitle = "Because you listened to ${item.artist}",
                            items = related.map { it.toShelfItem() },
                        )
                    )
                }
            }
            result
        }

        // 4. Rotating Indian Trending & Regional Hits (Pick 2 random categories every refresh!)
        val indianTrendingAsync = async {
            val categories = listOf(
                "Trending India Songs" to "Trending in India",
                "Latest Hindi Hits 2025" to "Bollywood Chartbusters",
                "Hot Punjabi Hits 2025" to "Punjabi Wave",
                "Indian Indie Songs" to "Indie & Chill India",
                "Romantic Hindi Songs" to "Desi Romance",
                "Desi Hip Hop Hits" to "Desi Hip Hop",
                "South Indian Hindi Hits" to "South Indian Blockbusters",
                "Arijit Singh Best Songs" to "Melodies & Soul",
            ).shuffled().take(2)

            categories.mapNotNull { (query, shelfTitle) ->
                val tracks = search(query, SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()
                    ?.map { it.song }
                    ?.shuffled()
                    ?.take(12)
                if (!tracks.isNullOrEmpty()) {
                    HomeShelf(
                        title = shelfTitle,
                        subtitle = "Updated for today",
                        items = tracks.map { it.toShelfItem() },
                    )
                } else null
            }
        }

        // 5. Search-based shelf if user recently searched
        val searchShelfAsync = async {
            val lastSearch = SearchHistory.recent.value.shuffled().firstOrNull()
            if (!lastSearch.isNullOrBlank()) {
                val tracks = search(lastSearch, SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()
                    ?.map { it.song }
                    ?.shuffled()
                    ?.take(10)
                if (!tracks.isNullOrEmpty()) {
                    HomeShelf(
                        title = "More like \"$lastSearch\"",
                        subtitle = "From your recent search",
                        items = tracks.map { it.toShelfItem() },
                    )
                } else null
            } else null
        }

        // 6. Listen Again shelf (from local history)
        val listenAgainAsync = async {
            if (history.isNotEmpty()) {
                HomeShelf(
                    title = "Listen Again",
                    subtitle = "Jump back in",
                    items = history.take(15).map { it.toSong().toShelfItem() },
                )
            } else null
        }

        val qp = quickPicksAsync.await()
        val dd = dailyDiscoverAsync.await()
        val sim = similarShelvesAsync.await()
        val trend = indianTrendingAsync.await()
        val sSearch = searchShelfAsync.await()
        val la = listenAgainAsync.await()

        // Listen Again (Recent play content) placed at the very top:
        val top = listOfNotNull(la)
        // Lead dynamic discovery: Quick Picks & Daily Discover (shuffled)
        val lead = listOfNotNull(qp, dd).shuffled()
        // Mid sections: Similar to, Indian Trending, Searches (shuffled dynamically)
        val mid = (sim + trend + listOfNotNull(sSearch)).shuffled()

        shelves.addAll(top)
        shelves.addAll(lead)
        shelves.addAll(mid)
        shelves
    }

    /**
     * More Home shelves past [home]'s first page, following FEmusic_home's
     * own continuation — the lever the official app pulls as you scroll
     * rather than a fixed one-shot page.
     */
    suspend fun moreHome(token: String): Result<HomeFeed> = call("home:more") {
        val response = Innertube.browseContinuation(token)
        HomeFeed(
            shelves = InnertubeParser.parseHomeContinuation(response),
            continuation = InnertubeParser.continuationToken(response),
        )
    }

    /**
     * Extra shelves loaded dynamically when continuation runs out or for guest mode,
     * ensuring the home feed continues loading rich content as the user scrolls.
     */
    suspend fun fallbackHomeShelves(page: Int): List<HomeShelf> = withContext(Dispatchers.IO) {
        runCatching {
            when (page) {
                1 -> shelvesOf("FEmusic_explore")
                2 -> shelvesOf("FEmusic_moods_and_genres")
                3 -> shelvesOf("FEmusic_new_releases_albums")
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Fetches dedicated shelves for a selected Indian mood or genre filter:
     * - Top Tracks (songs)
     * - Popular Playlists / Albums
     * - Trending Artists
     */
    suspend fun categoryShelves(category: String): Result<List<HomeShelf>> = call("category:$category") {
        coroutineScope {
            val queries = when {
                category.contains("Hindi", ignoreCase = true) -> listOf(
                    "Trending Hindi Songs" to "Trending in Hindi",
                    "Top Bollywood Hits" to "Bollywood Chartbusters",
                    "Best of Arijit Singh" to "Romantic Melodies",
                    "Hindi Indie Songs" to "Indie Hindi Vibe",
                )
                category.contains("Punjabi", ignoreCase = true) -> listOf(
                    "Top Punjabi Songs 2025" to "Punjabi Chartbusters",
                    "Punjabi Hip Hop Hits" to "Punjabi Beats & Bass",
                    "Romantic Punjabi Songs" to "Sufi & Romance Punjabi",
                    "Diljit Dosanjh Hits" to "Artist Spotlight: Diljit",
                )
                category.contains("Tamil", ignoreCase = true) -> listOf(
                    "Top Tamil Songs 2025" to "Kollywood Hits",
                    "Anirudh Ravichander Best Songs" to "Anirudh Wave",
                    "Tamil Melody Songs" to "Soul of Tamil",
                )
                category.contains("Telugu", ignoreCase = true) -> listOf(
                    "Top Telugu Songs 2025" to "Tollywood Blockbusters",
                    "Telugu Melody Hits" to "Heart of Telugu",
                    "SS Thaman Best Songs" to "Mass & Beats",
                )
                category.contains("Indie", ignoreCase = true) -> listOf(
                    "Indian Indie Hits" to "Indie Spotlight",
                    "Prateek Kuhad, Anuv Jain Best" to "Acoustic Poetry",
                    "Desi Lo-Fi Songs" to "Late Night Indie",
                )
                category.contains("Bollywood", ignoreCase = true) -> listOf(
                    "Bollywood Classics 90s 2000s" to "Golden Bollywood Era",
                    "Latest Bollywood Blockbusters" to "Current Hindi Hits",
                    "Bollywood Party Dance Songs" to "Party Rhythms",
                )
                category.contains("Ghazal", ignoreCase = true) || category.contains("Sufi", ignoreCase = true) -> listOf(
                    "Jagjit Singh Best Ghazals" to "Timeless Ghazals",
                    "Nusrat Fateh Ali Khan Sufi" to "Sufiana Baithak",
                    "Rahat Fateh Ali Khan Hits" to "Soul of Sufism",
                )
                category.contains("Classical", ignoreCase = true) || category.contains("Raga", ignoreCase = true) -> listOf(
                    "Indian Classical Raag Yaman Bhairavi" to "Raag & Meditation",
                    "Pandit Ravi Shankar Sitar" to "Sitar & Sarod Maestros",
                    "Zakir Hussain Classical Tabla" to "Rhythm of Raag",
                )
                else -> listOf(
                    "$category Hits 2025" to "Featured $category",
                    "$category Popular Songs" to "Top Picks for You",
                )
            }

            val shelfDeferreds = queries.map { (query, shelfTitle) ->
                async {
                    val tracks = search(query, SearchFilter.SONGS).getOrNull()
                        ?.filterIsInstance<SearchResult.Track>()
                        ?.map { it.song }
                        ?.shuffled()
                        ?.take(16)
                    if (!tracks.isNullOrEmpty()) {
                        HomeShelf(
                            title = shelfTitle,
                            subtitle = "Curated $category",
                            items = tracks.map { it.toShelfItem() },
                        )
                    } else null
                }
            }

            shelfDeferreds.mapNotNull { it.await() }
        }
    }

    /**
     * The lead shelf: the account's listening history, newest first.
     * Falls back to local PlaybackHistory when not signed in.
     */
    private suspend fun recentlyPlayed(): HomeShelf? {
        val songs = if (Innertube.cookie != null) {
            fetchHistory().take(RECENT_LIMIT)
        } else {
            PlaybackHistory.recent.value.take(RECENT_LIMIT).map { it.toSong() }
        }
        if (songs.isEmpty()) return null
        return HomeShelf(
            title = RECENT_TITLE,
            items = songs.map {
                ShelfItem(
                    title = it.title,
                    subtitle = it.artist,
                    thumbnailUrl = it.thumbnailUrl,
                    videoId = it.videoId,
                    browseId = null,
                )
            },
        )
    }

    /**
     * The raw fetch behind both [recentlyPlayed] and [history]: the account's
     * listening history, newest first, one row per play collapsed to one row
     * per track.
     *
     * A track played three times today is three rows in the feed — what that
     * dedupe costs is the times, which is fine for "what you have been
     * listening to" but would matter for a log. YouTube's own page groups them
     * under Today and Yesterday headings that the shelf parser doesn't carry
     * through either.
     */
    private suspend fun fetchHistory(): List<Song> =
        InnertubeParser.collectSongsDeep(Innertube.browse(HISTORY)).distinctBy { it.videoId }

    /**
     * The account's listening history, in the order YouTube Music keeps it.
     *
     * The same feed [recentlyPlayed] reads, without the truncation: that one is
     * a shelf on the home page and stops at [RECENT_LIMIT] so it stays a shelf,
     * whereas this is the page you open when twenty is not enough.
     */
    suspend fun history(): Result<List<Song>> = call("history") { fetchHistory() }

    private const val HISTORY = "FEmusic_history"
    private const val RECENT_TITLE = "Recently played"

    /** Enough to scroll through, short of turning the shelf into the history page. */
    private const val RECENT_LIMIT = 20

    private suspend fun shelvesOf(browseId: String): List<HomeShelf> =
        InnertubeParser.parseHome(Innertube.browse(browseId))

    /**
     * Explore: moods & genres from FEmusic_explore, plus the Daily/Weekly/
     * Trending charts, which YouTube Music serves from a separate browse id
     * and surfaces under Explore rather than Home.
     */
    suspend fun explore(): Result<List<HomeShelf>> = call("explore") {
        coroutineScope {
            val feeds = listOf("FEmusic_explore", "FEmusic_charts")
                .map { id -> async { runCatching { shelvesOf(id) }.getOrDefault(emptyList()) } }
                .awaitAll()
            val seen = mutableSetOf<String>()
            feeds.flatten().filter { seen.add(it.title.lowercase(Locale.ROOT)) }
        }
    }

    suspend fun search(query: String, filter: SearchFilter): Result<List<SearchResult>> =
        call("search:${filter.name}") {
            InnertubeParser.parseSearch(Innertube.search(query, filter.params))
        }

    /**
     * What YouTube Music would suggest completing [input] to, for the search
     * field's typeahead. Unfiltered on purpose: a suggestion is a query, and
     * which tab it is then run against is the user's to pick afterwards.
     */
    suspend fun searchSuggestions(input: String): Result<List<String>> =
        call("suggest") {
            InnertubeParser.parseSearchSuggestions(Innertube.searchSuggestions(input))
        }

    /**
     * The catalogue (audio-only) release of a music-video upload, found the
     * same way the "Switch to audio" toggle in the real app would land on
     * it: searching the title and artist and taking the closest song match.
     * Called before a video-tagged [Song] ever reaches the queue, so
     * playback, the mini player/notification, and YouTube's own history all
     * see the audio track — never the video upload's title, art or id.
     *
     * Matched through [TrackMatcher] rather than a bare title compare, for
     * the same reason [SourceResolver][com.music.flux.data.sources.SourceResolver]
     * does: a query for a niche title can come back with nothing that is
     * really the recording, and taking the first row regardless was landing
     * on a same-language, wrong-song hit — a Telugu folk video resolving to
     * an unrelated devotional track was reported from exactly this path.
     * [TrackMatcher.best] returning null is a normal answer, not a failure to
     * work around.
     *
     * Returns [song] unchanged when it isn't a video, or when nothing better
     * turns up — playing the video's own audio track beats guessing at a
     * substitute, and [song] is what a queue restore or offline retry falls
     * back to as well. Also unchanged when
     * [AppSettings.convertVideoToAudio][com.music.flux.data.settings.AppSettings.convertVideoToAudio]
     * is off — the listener has asked to keep video uploads as themselves.
     *
     * [search] already drops video rows from its results (see
     * [InnertubeParser.parseSearch]), so every candidate here is audio-only
     * without a second check.
     */
    suspend fun resolveAudio(song: Song): Song {
        if (!song.isVideo || !AppSettings.convertVideoToAudio.value) return song
        val target = TrackMatcher.targetOf(song)
        for (query in TrackMatcher.queries(target)) {
            val candidates = search(query, SearchFilter.SONGS)
                .getOrNull()
                ?.filterIsInstance<SearchResult.Track>()
                ?.map { it.song }
                .orEmpty()
            TrackMatcher.best(candidates, target)?.let { return it }
        }
        return song
    }

    /** Signed-in profile for the settings header. Null when signed out. */
    suspend fun account(): Result<Account> = call("account") {
        InnertubeParser.parseAccount(Innertube.accountMenu())
            ?: error("No account details")
    }

    /**
     * The whole library in one shot — requires a signed-in session.
     *
     * YouTube Music has no single "my library" feed: Liked Music is the `LM`
     * auto-playlist, the songs added to the library are a separate feed, and
     * every saved collection has its own browse id. They're fetched in
     * parallel and a feed that fails or is simply empty (a fresh account has
     * no saved albums) is dropped rather than failing the whole page.
     */
    suspend fun library(): Result<LibraryPage> = call("library") {
        coroutineScope {
            val liked = async { runCatching { songsPaged(LIKED_MUSIC) }.getOrDefault(emptyList()) }
            val added = async { runCatching { songsPaged(LIBRARY_SONGS) }.getOrDefault(emptyList()) }
            val shelves = LIBRARY_FEEDS
                .map { (title, browseId) ->
                    async {
                        val items = runCatching {
                            InnertubeParser.parseLibraryItems(Innertube.browse(browseId))
                        }.getOrDefault(emptyList())
                        HomeShelf(title, items)
                    }
                }
                .awaitAll()
                .filter { it.items.isNotEmpty() }

            val likedSongs = liked.await()
            val likedIds = likedSongs.mapTo(HashSet()) { it.videoId }
            LikeState.seedLiked(likedIds)
            LibraryPage(
                likedSongs = likedSongs,
                // Thumbs-up'd tracks are also in the library feed; only what
                // the "Liked Music" list doesn't already cover is worth a
                // second section.
                librarySongs = added.await().filterNot { it.videoId in likedIds },
                shelves = shelves,
            )
        }
    }

    /**
     * What YouTube Music would play on after [videoId]. Feeds AutoPlay; the
     * seed track itself comes back first, so callers filter what they have.
     */
    suspend fun radio(videoId: String): Result<List<Song>> = call("radio:$videoId") {
        InnertubeParser.parseWatchQueue(Innertube.next(videoId))
    }

    /**
     * The artist and album pages a track links out to.
     *
     * Search rows carry them, but home cards and anything already sitting in a
     * queue often don't — and the credits in the player have to lead somewhere
     * either way. A track's own watch queue entry always names both.
     */
    suspend fun trackLinks(videoId: String): Result<Song> = call("links:$videoId") {
        InnertubeParser.parseWatchQueue(Innertube.next(videoId))
            .firstOrNull { it.videoId == videoId }
            ?: error("no watch entry for $videoId")
    }

    /**
     * One page of a browse feed's tracks, and the token for the page after
     * it — null once there is nothing more. [suggested] is only ever
     * non-empty for a playlist page — see [InnertubeParser.parsePlaylistShelf].
     */
    data class SongPage(
        val songs: List<Song>,
        val continuation: String?,
        val suggested: List<Song> = emptyList(),
        /**
         * Whether the release this page describes is in the library. Only the
         * first page can answer — a continuation carries rows and nothing else
         * — so it is null from [moreSongs] and must not overwrite what
         * [browseSongs] already established.
         */
        val library: LibraryState? = null,
        /**
         * Whether this page's playlist is one the account made rather than one
         * it saved — see [InnertubeParser.parsePlaylistOwned]. Null for an
         * album, a continuation, or a page that doesn't say.
         */
        val owned: Boolean? = null,
        /**
         * What the page calls itself — only needed by callers that opened it
         * with nothing but a browse id, i.e. a tapped link. Null on a
         * continuation, which carries rows and no header.
         */
        val header: InnertubeParser.BrowseHeader? = null,
        /**
         * The editorial blurb YouTube Music writes for the release, when it
         * has one — see [InnertubeParser.parseDescription]. Null from a
         * continuation, same as [header].
         */
        val description: String? = null,
    )

    /**
     * The first page of an album/playlist's tracks, and nothing more.
     *
     * Deliberately not the whole list. Following every continuation before
     * returning meant a long playlist spent up to ten round trips showing a
     * spinner, when every row needed to fill the first screenful was in the
     * first response. The rest arrives behind a page that is by then already
     * being read — see [moreSongs].
     */
    suspend fun browseSongs(browseId: String): Result<SongPage> = call("browse:$browseId") {
        val response = Innertube.browse(browseId)
        val page = pageOf(response)
        // Only a playlist has an owner in the sense that matters — see
        // parsePlaylistOwned — and only its own first response can be asked.
        if (!browseId.startsWith("VL")) page
        else page.copy(owned = InnertubeParser.parsePlaylistOwned(response))
    }

    /** The page [SongPage.continuation] points at. */
    suspend fun moreSongs(token: String): Result<SongPage> = call("browse:more") {
        pageOf(Innertube.browseContinuation(token))
    }

    /**
     * Whether [browseId] is a playlist the account made — see
     * [InnertubeParser.parsePlaylistOwned].
     *
     * The same question [browseSongs] answers on the way past, asked on its own
     * by whatever needs it without a page open: holding a playlist card offers
     * Rename and Delete, and the card itself cannot say whether either applies.
     * The rows it fetches are thrown away, which is the price of one request for
     * a menu that would otherwise have to guess.
     */
    suspend fun playlistOwned(browseId: String): Result<Boolean?> = call("owner:$browseId") {
        InnertubeParser.parsePlaylistOwned(Innertube.browse(browseId))
    }

    private fun pageOf(response: JsonObject): SongPage {
        val library = InnertubeParser.parseLibraryState(response)
        val header = InnertubeParser.parseBrowseHeader(response)
        // A playlist page is scoped to its own shelf so its "Suggested
        // tracks" never read as songs the user added — see
        // parsePlaylistShelf. Anything else (album, library, history) has no
        // such shelf, and falls back to the layout-agnostic walk.
        InnertubeParser.parsePlaylistShelf(response)?.let { shelf ->
            return SongPage(shelf.songs, shelf.continuation, shelf.suggested, library, header = header)
        }
        return SongPage(
            // One response can name the same track twice — an album page that
            // also carries a "you might also like" shelf, say. Collecting into a
            // map used to take care of that; paging by hand means saying so.
            songs = InnertubeParser.collectSongsDeep(response).distinctBy { it.videoId },
            continuation = InnertubeParser.continuationToken(response),
            library = library,
            header = header,
            description = InnertubeParser.parseDescription(response),
        )
    }

    /**
     * The complete track listing behind an album or playlist browse id.
     *
     * The whole list rather than [browseSongs]' first page, because the callers
     * are the ones that act on all of it at once — "Add to queue" on a card
     * whose page was never opened. Queueing the first hundred rows of a
     * three-hundred-track playlist and calling it the playlist would be a
     * quieter kind of wrong than failing outright.
     *
     * Takes as long as the list is long — see [songsPaged].
     */
    suspend fun allSongs(browseId: String): Result<List<Song>> = call("all:$browseId") {
        songsPaged(browseId).ifEmpty { error("No tracks here") }
    }

    /**
     * Every track behind a browse id, following continuations.
     *
     * A playlist page returns its first ~100 rows and a token for the rest, so
     * a long list otherwise arrives silently truncated. Capped at
     * [MAX_PAGES] so a runaway feed can't hold the UI open forever, and a
     * failed page keeps whatever was already collected.
     *
     * Holds its caller until the last page lands, so it belongs behind things
     * nobody is watching — the library sync, an artist's back catalogue. For
     * anything a screen is waiting on, use [browseSongs] and [moreSongs].
     */
    suspend fun syncLikedMusic(maxPages: Int = 25): List<Song> {
        val songs = songsPaged(LIKED_MUSIC, maxPages)
        val likedIds = songs.mapTo(HashSet()) { it.videoId }
        LikeState.seedLiked(likedIds)
        return songs
    }

    private suspend fun songsPaged(browseId: String, maxPages: Int = MAX_PAGES): List<Song> {
        val out = LinkedHashMap<String, Song>()
        var response = Innertube.browse(browseId)
        var page = 1
        while (true) {
            // Same shelf-scoping as pageOf: a playlist (Liked Music and the
            // Library Songs auto-playlist included) is read from its own
            // shelf so a trailing "Suggested tracks" shelf never joins in.
            val shelf = InnertubeParser.parsePlaylistShelf(response)
            (shelf?.songs ?: InnertubeParser.collectSongsDeep(response)).forEach { out[it.videoId] = it }
            val token = shelf?.continuation ?: InnertubeParser.continuationToken(response)
            if (token == null || page++ >= maxPages) break
            response = runCatching { Innertube.browseContinuation(token) }.getOrNull() ?: break
        }
        return out.values.toList()
    }

    const val MAX_PAGES = 25

    /**
     * Liked Music: the `LM` auto-playlist, addressed as a playlist browse id.
     * Public because it is also the page a track has to disappear from the
     * moment it stops being liked — see MainViewModel's `dropFromLikedLists`.
     */
    const val LIKED_MUSIC = "VLLM"

    /** Songs explicitly added to the library — distinct from Liked Music. */
    private const val LIBRARY_SONGS = "FEmusic_liked_videos"

    /** Saved and own playlists; also what the "add to playlist" picker lists. */
    private const val LIBRARY_PLAYLISTS = "FEmusic_liked_playlists"

    /**
     * What the playlists shelf is called in a [LibraryPage].
     *
     * Coined here, and named here rather than spelt out at each use, because it
     * is the only shelf in the library anything else looks for by name: it is
     * the one the create tile leads (see LibraryScreen) and the one a rename or
     * a delete has to reach into (see MainViewModel's `editPlaylistShelf`).
     * Three copies of a bare "Playlists" is three places a retitling silently
     * turns those features off.
     */
    const val PLAYLISTS_SHELF = "Playlists"

    private val LIBRARY_FEEDS = listOf(
        PLAYLISTS_SHELF to LIBRARY_PLAYLISTS,
        "Albums" to "FEmusic_liked_albums",
        "Artists" to "FEmusic_library_corpus_track_artists",
        "Subscriptions" to "FEmusic_library_corpus_artists",
        "Podcasts" to "FEmusic_library_non_music_audio_list",
    )

    // ---- Writes -------------------------------------------------------------

    /**
     * The account's own state for one track — rating and library membership.
     *
     * Deliberately a lookup rather than something cached with the [Song]: a
     * track reaching the player through the queue has been round-tripped
     * through a MediaItem, which carries an id and little else, and the
     * feedback tokens are per-row anyway. Fetched when a menu is opened, which
     * is the only moment the answer is looked at.
     */
    suspend fun songMenu(videoId: String): Result<SongMenu> = call("menu:$videoId") {
        InnertubeParser.parseSongMenu(Innertube.next(videoId), videoId)
            ?: error("no menu for $videoId")
    }

    suspend fun rate(videoId: String, status: LikeStatus): Result<Unit> =
        call("rate:$videoId") { Innertube.rate(videoId, status) }

    /** Adds or removes a track from the library; [token] says which. */
    suspend fun setLibraryStatus(token: String): Result<Unit> =
        call("library:feedback") { Innertube.sendFeedback(token) }

    /**
     * Saves an album or playlist to the library, or removes it. [playlistId] is
     * the one the page named — see [LibraryState].
     */
    suspend fun setSaved(playlistId: String, saved: Boolean): Result<Unit> =
        call("library:$playlistId") { Innertube.ratePlaylist(playlistId, saved) }

    /**
     * The playlists a track can be added to. Not paged: an account with more
     * than one page of playlists is rare, and the picker is a list to scroll
     * rather than a feed to follow.
     */
    suspend fun userPlaylists(): Result<List<UserPlaylist>> = call("playlists") {
        InnertubeParser.parseUserPlaylists(Innertube.browse(LIBRARY_PLAYLISTS))
    }

    /** Creates a playlist, optionally seeded with [videoIds]; returns its id. */
    suspend fun createPlaylist(
        title: String,
        privacy: PlaylistPrivacy,
        videoIds: List<String> = emptyList(),
    ): Result<String> = call("playlist:create") {
        Innertube.createPlaylist(title, privacy, videoIds = videoIds)
    }

    /**
     * Adds tracks to a playlist. Succeeds with the per-entry ids YouTube minted
     * for them — see [Innertube.addToPlaylist]. A caller with nothing on screen
     * to update can ignore the map; one splicing the row into a playlist it is
     * looking at needs it for the row's "remove".
     */
    suspend fun addToPlaylist(
        playlistId: String,
        videoIds: List<String>,
    ): Result<Map<String, String>> =
        call("playlist:add") { Innertube.addToPlaylist(playlistId, videoIds) }

    /** [entries] are (setVideoId, videoId) pairs — see [Song.setVideoId]. */
    suspend fun removeFromPlaylist(
        playlistId: String,
        entries: List<Pair<String, String>>,
    ): Result<Unit> = call("playlist:remove") {
        Innertube.removeFromPlaylist(playlistId, entries)
    }

    suspend fun renamePlaylist(playlistId: String, title: String): Result<Unit> =
        call("playlist:rename") { Innertube.renamePlaylist(playlistId, title) }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> =
        call("playlist:delete") { Innertube.deletePlaylist(playlistId) }

    /**
     * Artist page. The landing page only lists ~5 songs, so the linked
     * "Top songs" playlist is fetched to fill the list out.
     */
    suspend fun artistPage(browseId: String): Result<ArtistPage> = call("artist:$browseId") {
        val page = InnertubeParser.parseArtistPage(Innertube.browse(browseId))
        val fullSongs = page.moreSongsBrowseId?.let { playlistId ->
            runCatching { songsPaged(playlistId) }.getOrNull()
        }
        if (!fullSongs.isNullOrEmpty()) page.copy(songs = fullSongs) else page
    }

    private suspend fun <T> call(label: String, block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }
                // runCatching catches Throwable, cancellation included, which
                // would turn "the user typed another letter" into a failed
                // Result and put the abandoned request's error on screen.
                // Cancellation isn't this call's to answer for.
                .onFailure { if (it is CancellationException) throw it }
                .onSuccess { Log.d(TAG, "$label ok") }
                .onFailure { Log.w(TAG, "$label failed: ${it.message}") }
        }
}
