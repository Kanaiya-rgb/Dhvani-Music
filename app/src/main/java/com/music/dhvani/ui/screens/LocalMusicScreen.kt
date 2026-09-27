package com.music.dhvani.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.dhvani.R
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.model.durationMillis
import com.music.dhvani.download.DownloadedCollection
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.ROW_DIVIDER_INSET
import com.music.dhvani.ui.components.SongRow
import com.music.dhvani.ui.components.thumbnailBorder
import com.music.dhvani.ui.components.TopBarContentGap
import com.music.dhvani.ui.components.topBarHeight
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.util.Locale

private const val LOCAL_TAB_SONGS = 0
private const val LOCAL_TAB_ARTISTS = 1
private const val LOCAL_TAB_ALBUMS = 2

/**
 * Local Music folder view with three tabs: Songs (default), Artists, Albums.
 *
 * Also the Downloads folder — the two are the same thing from here, a flat list
 * of tracks on this device, and they read as the same page because they are the
 * same page. What differs is only where the list came from, what to say when it
 * is empty, and whether anything knows how those tracks were *asked* for: see
 * [collections], which is the Downloads folder's alone.
 *
 * Tapping an artist or album name slides in a filtered song list inline, so
 * the tab bar stays visible and Back returns to the grid rather than leaving
 * the screen.
 */
@Composable
fun LocalMusicScreen(
    title: String = "Downloads",
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    contentPadding: PaddingValues,
    /**
     * Shown in place of the tab content when there are no songs at all — the
     * reason there are none, which "0 songs" on its own doesn't give.
     */
    emptyMessage: String? = null,
    /**
     * One of the Artists / Albums groupings, held rather than tapped — the
     * album/playlist menu, with the rows it covers already in hand. Nothing
     * here has a browse id to fetch, so this is the only way these get one.
     */
    onCollectionLongPress: ((String, List<Song>) -> Unit)? = null,
    /**
     * The albums and playlists that were downloaded *as* albums and playlists.
     *
     * They lead the Albums tab, because they are the only entries on it that the
     * user actually asked for by name — the rest are groupings this screen
     * derived from whatever album tag each file happens to carry, which is a
     * good guess and nothing more. A playlist cannot be derived that way at all:
     * its tracks are off forty different releases and no tag on any of them says
     * which playlist they were pulled from, so without this a downloaded
     * playlist simply scattered.
     *
     * Empty for Local Music, where nothing was asked for through this app and
     * the tags are all there is.
     */
    collections: List<DownloadedCollection> = emptyList(),
    onExplore: (() -> Unit)? = null,
    onImport: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Which top-level tab is selected.
    var selectedTab by rememberSaveable { mutableIntStateOf(LOCAL_TAB_SONGS) }

    // Narrows whichever tab is showing — songs by title/artist/album, artists
    // and albums by name. Not saved across process death: a filter left on a
    // folder that was never reopened is more surprising than one that reset.
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // When non-null, we are showing a drill-down list for that artist or album.
    var drillDownLabel by remember { mutableStateOf<String?>(null) }
    var drillDownSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    // The release's own cover, for the drill-down header. Only a downloaded
    // album or playlist has one worth showing — a tag-derived grouping's
    // "artwork" is just whichever of its rows happened to be first.
    var drillDownArt by remember { mutableStateOf<String?>(null) }

    val inDrillDown = drillDownLabel != null

    val leaveDrillDown = {
        drillDownLabel = null
        drillDownSongs = emptyList()
        drillDownArt = null
    }

    BackHandler(enabled = inDrillDown) { leaveDrillDown() }

    // The tab row is fixed above the scrolling content, so its own top
    // padding has to clear the frosted top bar / status bar that the
    // LazyColumns beneath it would otherwise scroll under.
    val bodyContentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())

    // contentPadding.top carries extra breathing room meant for scrolling
    // content resting under the glass bar; the tab row is fixed and sits
    // right below the bar, so it only needs to clear the bar itself.
    val barHeight = topBarHeight()

    val songsCount = songs.size
    val artistsCount = remember(songs) { songs.map { it.artist }.filter { it.isNotBlank() }.distinct().size }
    val albumsList = remember(songs, collections) { albumEntries(songs, collections) }
    val albumsCount = albumsList.size

    Column(modifier = modifier.fillMaxSize()) {
        // ── Hero Header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = barHeight + TopBarContentGap,
                    start = PAGE_GUTTER,
                    end = PAGE_GUTTER,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (songs.isNotEmpty()) Color(0xFF0F3824)
                                else Color(0xFF282A2E)
                            )
                            .border(
                                1.dp,
                                if (songs.isNotEmpty()) Color(0xFF59DDA9).copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 9.dp, vertical = 3.5.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (songs.isNotEmpty()) Color(0xFF59DDA9) else Color(0xFFFF8A3D)),
                            )
                            Text(
                                text = if (songs.isNotEmpty()) "OFFLINE READY" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                ),
                                color = if (songs.isNotEmpty()) Color(0xFF59DDA9) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (songs.isNotEmpty()) {
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"} saved on device",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        ),
                    )
                }
            }
        }

        // ── Search Field ───────────────────────────────────────────────────────
        LocalSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .padding(horizontal = PAGE_GUTTER)
                .padding(bottom = 10.dp),
        )

        // ── Modern Segmented Pill Tabs ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModernPillTab(
                icon = Icons.Rounded.MusicNote,
                label = stringResource(R.string.songs),
                count = if (songsCount > 0) songsCount else null,
                selected = selectedTab == LOCAL_TAB_SONGS,
                onClick = {
                    selectedTab = LOCAL_TAB_SONGS
                    leaveDrillDown()
                },
                modifier = Modifier.weight(1f),
            )
            ModernPillTab(
                icon = Icons.Rounded.Person,
                label = stringResource(R.string.artists),
                count = if (artistsCount > 0) artistsCount else null,
                selected = selectedTab == LOCAL_TAB_ARTISTS,
                onClick = {
                    selectedTab = LOCAL_TAB_ARTISTS
                    leaveDrillDown()
                },
                modifier = Modifier.weight(1f),
            )
            ModernPillTab(
                icon = Icons.Rounded.Album,
                label = stringResource(R.string.albums),
                count = if (albumsCount > 0) albumsCount else null,
                selected = selectedTab == LOCAL_TAB_ALBUMS,
                onClick = {
                    selectedTab = LOCAL_TAB_ALBUMS
                    leaveDrillDown()
                },
                modifier = Modifier.weight(1f),
            )
        }

        // ── Content ──────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = if (inDrillDown) "drill:$drillDownLabel" else "tab:$selectedTab",
            transitionSpec = {
                if (targetState.startsWith("drill:")) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "local_music_content",
            modifier = Modifier.fillMaxSize(),
        ) { key ->
            when {
                key.startsWith("drill:") -> {
                    // Drill-down song list for artist / album
                    DrillDownSongList(
                        label = drillDownLabel ?: "",
                        artworkUrl = drillDownArt,
                        songs = drillDownSongs,
                        onSongClick = onSongClick,
                        onSongLongPress = onSongLongPress,
                        onSongSwipe = onSongSwipe,
                        onShuffle = onShuffle,
                        onMore = onCollectionLongPress?.let { more ->
                            { more(drillDownLabel ?: "", drillDownSongs) }
                        },
                        onBack = leaveDrillDown,
                        contentPadding = bodyContentPadding,
                    )
                }

                key == "tab:$LOCAL_TAB_SONGS" -> {
                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bodyContentPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            DhvaniOfflineBanner(
                                songCount = 0,
                                totalDurationMs = 0L,
                                onExplore = onExplore ?: {},
                                onImport = onImport,
                                onBack = onBack ?: {},
                            )
                        }
                    } else {
                        val filteredSongs = remember(songs, searchQuery) {
                            if (searchQuery.isBlank()) songs
                            else songs.filter { it.matchesSearch(searchQuery) }
                        }
                        if (searchQuery.isNotBlank() && filteredSongs.isEmpty()) {
                            EmptySearchResult(
                                query = searchQuery,
                                onClear = { searchQuery = "" },
                                modifier = Modifier.padding(bodyContentPadding),
                            )
                        } else {
                            SongsTab(
                                songs = filteredSongs,
                                onSongClick = onSongClick,
                                onSongLongPress = onSongLongPress,
                                onSongSwipe = onSongSwipe,
                                onShuffle = onShuffle,
                                contentPadding = bodyContentPadding,
                            )
                        }
                    }
                }

                key == "tab:$LOCAL_TAB_ARTISTS" -> {
                    val artists = remember(songs, searchQuery) {
                        songs.groupBy { it.artist }
                            .entries
                            .filter { searchQuery.isBlank() || it.key.contains(searchQuery, ignoreCase = true) }
                            .sortedBy { it.key.lowercase(Locale.ROOT) }
                    }
                    if (songs.isEmpty() || (searchQuery.isBlank() && artists.isEmpty())) {
                        OfflineTabEmptyState(
                            icon = Icons.Rounded.Person,
                            accentColor = Color(0xFF59DDA9),
                            title = "No Offline Artists",
                            subtitle = "Artists of your downloaded songs will appear here. Download songs to listen to your favorite artists offline anytime.",
                            onExplore = onExplore ?: {},
                            onImport = onImport,
                            onBack = onBack ?: {},
                            modifier = Modifier.padding(bodyContentPadding),
                        )
                    } else if (searchQuery.isNotBlank() && artists.isEmpty()) {
                        EmptySearchResult(
                            query = searchQuery,
                            onClear = { searchQuery = "" },
                            modifier = Modifier.padding(bodyContentPadding),
                        )
                    } else {
                        ArtistsTab(
                            artists = artists,
                            onArtistClick = { artist, artistSongs ->
                                drillDownLabel = artist
                                drillDownSongs = artistSongs
                                drillDownArt = null
                            },
                            onArtistLongPress = onCollectionLongPress,
                            contentPadding = bodyContentPadding,
                        )
                    }
                }

                else -> {
                    // LOCAL_TAB_ALBUMS
                    val albums = remember(songs, collections, searchQuery) {
                        albumEntries(songs, collections).filter {
                            searchQuery.isBlank() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    if (songs.isEmpty() || (searchQuery.isBlank() && albums.isEmpty())) {
                        OfflineTabEmptyState(
                            icon = Icons.Rounded.Album,
                            accentColor = Color(0xFFFF848E),
                            title = "No Offline Albums",
                            subtitle = "Downloaded albums and offline playlists will appear here. Save complete albums to listen anytime offline without internet.",
                            onExplore = onExplore ?: {},
                            onImport = onImport,
                            onBack = onBack ?: {},
                            modifier = Modifier.padding(bodyContentPadding),
                        )
                    } else if (searchQuery.isNotBlank() && albums.isEmpty()) {
                        EmptySearchResult(
                            query = searchQuery,
                            onClear = { searchQuery = "" },
                            modifier = Modifier.padding(bodyContentPadding),
                        )
                    } else {
                        AlbumsTab(
                            albums = albums,
                            onAlbumClick = { entry ->
                                drillDownLabel = entry.title
                                drillDownSongs = entry.songs
                                drillDownArt = entry.thumbnailUrl
                            },
                            onAlbumLongPress = onCollectionLongPress,
                            contentPadding = bodyContentPadding,
                        )
                    }
                }
            }
        }
    }
}

// ── Songs tab ─────────────────────────────────────────────────────────────────

private enum class LocalSortOrder(val label: String) {
    RECENT("Recent"),
    TITLE("A-Z"),
    ARTIST("Artist"),
    DURATION("Duration"),
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    contentPadding: PaddingValues,
) {
    var sortOrder by rememberSaveable { mutableStateOf(LocalSortOrder.RECENT) }
    val sortedSongs = remember(songs, sortOrder) {
        when (sortOrder) {
            LocalSortOrder.RECENT -> songs
            LocalSortOrder.TITLE -> songs.sortedBy { it.title.lowercase(Locale.ROOT) }
            LocalSortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase(Locale.ROOT) }
            LocalSortOrder.DURATION -> songs.sortedByDescending { it.durationMillis() }
        }
    }

    val listState = rememberLazyListState()
    val totalPlaybackMs = remember(songs) {
        songs.sumOf { it.durationMillis() }
    }
    val haptics = rememberHaptics()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            OfflineStatsSummaryCard(
                songCount = songs.size,
                totalDurationMs = totalPlaybackMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Play All
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF8A3D), Color(0xFFFF5E00))
                            )
                        )
                        .clickable { if (sortedSongs.isNotEmpty()) onSongClick(sortedSongs, 0) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                        ),
                    )
                }
                // Shuffle
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1E2024))
                        .border(1.dp, Color(0xFF2E323A), RoundedCornerShape(50))
                        .clickable { if (sortedSongs.isNotEmpty()) onShuffle(sortedSongs) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = Color(0xFF59DDA9),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E2E8),
                        ),
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                LocalSortOrder.entries.forEach { order ->
                    val isSelected = order == sortOrder
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color(0xFFFF8A3D).copy(alpha = 0.18f)
                                else Color(0xFF16181C)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF8A3D).copy(alpha = 0.6f)
                                else Color(0xFF282A2E),
                                RoundedCornerShape(50),
                            )
                            .clickable {
                                if (!isSelected) {
                                    haptics.play(Haptic.Select)
                                    sortOrder = order
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                            ),
                            color = if (isSelected) Color(0xFFFF8A3D) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(
                icon = Icons.Rounded.LibraryMusic,
                title = formatSongsCount(sortedSongs.size),
            )
        }
        itemsIndexed(sortedSongs, key = { index, s -> if (s.videoId.isNotBlank()) "${s.videoId}_$index" else "local_$index" }) { index, song ->
            SongRow(
                song = song,
                onClick = { onSongClick(sortedSongs, index) },
                onLongPress = { onSongLongPress(song) },
                onSwipeToQueue = { onSongSwipe(song) },
            )
            if (index < sortedSongs.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── Artists tab ───────────────────────────────────────────────────────────────

@Composable
private fun ArtistsTab(
    artists: List<Map.Entry<String, List<Song>>>,
    onArtistClick: (String, List<Song>) -> Unit,
    onArtistLongPress: ((String, List<Song>) -> Unit)?,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SectionHeader(
                icon = Icons.Rounded.Person,
                title = "${artists.size} ${if (artists.size == 1) "artist" else "artists"}",
            )
        }
        items(artists) { (artist, artistSongs) ->
            ArtistRow(
                name = artist,
                artistSongs = artistSongs,
                songCount = artistSongs.size,
                onClick = { onArtistClick(artist, artistSongs) },
                onLongPress = onArtistLongPress?.let { { it(artist, artistSongs) } },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistRow(
    name: String,
    artistSongs: List<Song>,
    songCount: Int,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar circle with subtle outline border
        val avatarUrl = artistSongs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "ARTIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                Text(
                    text = "$songCount ${if (songCount == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Albums tab ────────────────────────────────────────────────────────────────

/**
 * One row of the Albums tab, whichever of the two things it came from.
 *
 * The tab used to be a `Map.Entry<String, List<Song>>` straight off a `groupBy`,
 * which was exactly as much as a tag grouping can say. A downloaded release
 * knows three more things — its own cover, whether it is a playlist rather than
 * an album, and the order its tracks go in — and none of those has anywhere to
 * live in a map entry.
 */
private class AlbumEntry(
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    /** Billed as a playlist rather than by artist; see [AlbumRow]. */
    val playlist: Boolean,
    /** Kept in the order it was downloaded in, which is the release's own. */
    val songs: List<Song>,
    /** Whether this is a release the user asked for, or a grouping inferred. */
    val asked: Boolean,
    /**
     * What the list keys this row by — the release's own id where it has one.
     *
     * Not the title: an album and a playlist can be called the same thing (a
     * self-titled record and its "This is …" mix, say), and two rows sharing a
     * key is a crash out of `LazyColumn` rather than a cosmetic clash.
     */
    val key: String,
)

/**
 * The Albums tab's rows: the releases downloaded whole, then whatever else the
 * files' own album tags group up.
 *
 * The two are merged rather than shown as separate sections because they are the
 * same kind of thing to whoever is looking for one — a folder of songs with a
 * name they remember. What matters is only that the *named* ones win a collision:
 * an album downloaded whole also stamps its name onto each of its tracks (see
 * `withAlbum` in MainActivity), so without this every one of them would appear
 * twice, once with its cover and once without.
 *
 * Releases lead within their own alphabetical run rather than being sorted
 * together, because a tag grouping is a guess and a recorded release is not.
 */
private fun albumEntries(
    songs: List<Song>,
    collections: List<DownloadedCollection>,
): List<AlbumEntry> {
    val asked = collections.map { collection ->
        AlbumEntry(
            title = collection.title,
            artist = collection.subtitle.ifBlank {
                collection.songs.firstOrNull()?.artist.orEmpty()
            },
            thumbnailUrl = collection.thumbnailUrl,
            playlist = collection.playlist,
            songs = collection.songs,
            asked = true,
            key = "asked:${collection.id}",
        )
    }
    val claimed = asked.mapTo(HashSet()) { it.title.lowercase(Locale.ROOT) }
    val derived = songs
        .groupBy { it.albumName }
        .mapNotNull { (name, group) ->
            // Null is every track that never said what release it was off, and
            // there is no row to draw for "no album" — those are the Songs tab's
            // and nothing else's.
            if (name == null || name.lowercase(Locale.ROOT) in claimed) return@mapNotNull null
            AlbumEntry(
                title = name,
                artist = group.firstOrNull()?.artist.orEmpty(),
                thumbnailUrl = group.firstNotNullOfOrNull { it.thumbnailUrl },
                playlist = false,
                songs = group,
                asked = false,
                key = "tagged:$name",
            )
        }
    return (asked + derived).sortedWith(
        compareByDescending<AlbumEntry> { it.asked }.thenBy { it.title.lowercase(Locale.ROOT) },
    )
}

@Composable
private fun AlbumsTab(
    albums: List<AlbumEntry>,
    onAlbumClick: (AlbumEntry) -> Unit,
    onAlbumLongPress: ((String, List<Song>) -> Unit)?,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SectionHeader(
                icon = Icons.Rounded.Album,
                title = "${albums.size} ${if (albums.size == 1) "album / playlist" else "albums & playlists"}",
            )
        }
        items(albums, key = { it.key }) { entry ->
            AlbumRow(
                entry = entry,
                onClick = { onAlbumClick(entry) },
                onLongPress = onAlbumLongPress?.let { { it(entry.title, entry.songs) } },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumRow(
    entry: AlbumEntry,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (entry.playlist) Icons.AutoMirrored.Rounded.QueueMusic else Icons.Rounded.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            if (!entry.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.thumbnailUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(shape),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (entry.playlist) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = if (entry.playlist) "PLAYLIST" else "ALBUM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.playlist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                val subtext = buildString {
                    if (!entry.playlist && entry.artist.isNotBlank() && entry.artist != entry.title) {
                        append("${entry.artist} · ")
                    }
                    append("${entry.songs.size} ${if (entry.songs.size == 1) "track" else "tracks"}")
                }
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * A release's cover, with a glyph standing in when there isn't one.
 *
 * The placeholder is not a fallback so much as the common case for anything
 * grouped off tags: those files' artwork is whatever the media scanner extracted,
 * which for a `.m4a` this app wrote is frequently nothing at all. Drawn behind
 * the image rather than instead of it, so a cover that loads late replaces the
 * glyph without the row changing size under it.
 */
@Composable
private fun CollectionArtwork(url: String?, playlist: Boolean, size: Dp) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playlist) Icons.AutoMirrored.Rounded.QueueMusic else Icons.Rounded.Album,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size * 0.54f),
        )
        if (url != null) {
            AsyncImage(
                model = url.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .thumbnailBorder(shape),
            )
        }
    }
}

// ── Drill-down song list ───────────────────────────────────────────────────────

@Composable
private fun DrillDownSongList(
    label: String,
    /** The release's cover, where it has one — see [CollectionArtwork]. */
    artworkUrl: String?,
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onMore: (() -> Unit)?,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        // Back + title header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = PAGE_GUTTER, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.width(4.dp))
                // Only where there is a real cover to show. An artist grouping
                // has none, and a square of placeholder glyph next to the name
                // would be decoration standing in for information.
                if (artworkUrl != null) {
                    CollectionArtwork(url = artworkUrl, playlist = false, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // The same menu holding the row in the grid behind this opens.
                // Reachable from here too because this is where someone ends up
                // who wanted the whole album and tapped instead of held.
                onMore?.let { more ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = more),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Play / Shuffle action row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Play button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { if (songs.isNotEmpty()) onSongClick(songs, 0) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                // Shuffle button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { if (songs.isNotEmpty()) onShuffle(songs) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Song rows
        itemsIndexed(songs) { index, song ->
            SongRow(
                song = song,
                onClick = { onSongClick(songs, index) },
                onLongPress = { onSongLongPress(song) },
                onSwipeToQueue = { onSongSwipe(song) },
            )
            if (index < songs.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

/** Whether this track is a hit for a query typed into [LocalSearchField]. */
private fun Song.matchesSearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        albumName?.contains(query, ignoreCase = true) == true

/**
 * The filter box above the tab row.
 *
 * Live rather than submit-on-enter: there is no network round trip behind it,
 * only a list already in memory, so narrowing it on every keystroke costs
 * nothing and a submit action would just be a tap this screen doesn't need.
 */
@Composable
private fun LocalSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search this folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ModernPillTab(
    icon: ImageVector,
    label: String,
    count: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val shape = RoundedCornerShape(50)
    val saffronGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFF8A3D), Color(0xFFFF5E00))
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(saffronGradient)
                } else {
                    Modifier
                        .background(Color(0xFF16181C))
                        .border(1.dp, Color(0xFF282A2E), shape)
                }
            )
            .clickable {
                if (!selected) haptics.play(Haptic.Select)
                onClick()
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                ),
                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (count != null && count > 0) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) Color.Black.copy(alpha = 0.2f)
                            else Color(0xFF282A2E)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (selected) Color.Black else Color(0xFFFF7A29),
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineTabEmptyState(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    onExplore: () -> Unit,
    onImport: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg = Color(0xFF16181C)
    val cardBorder = Color(0xFF2E323A)

    val infiniteTransition = rememberInfiniteTransition(label = "empty_tab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Glowing orbital circle with dynamic pulse
        Box(
            modifier = Modifier
                .size(112.dp * pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f * pulseAlpha),
                            Color(0xFF16181C).copy(alpha = 0.5f),
                        )
                    )
                )
                .border(1.5.dp, accentColor.copy(alpha = 0.5f * pulseAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2024))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(26.dp))

        // Explore button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF8A3D), Color(0xFFFF5E00))
                    )
                )
                .clickable(onClick = onExplore),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Explore & Download Music",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                    ),
                )
            }
        }

        if (onImport != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1A261F))
                    .border(1.dp, Color(0xFF59DDA9).copy(alpha = 0.4f), RoundedCornerShape(50))
                    .clickable(onClick = onImport),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF59DDA9),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Import Playlists / Albums",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF59DDA9),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Go Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(50))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(50))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.offline_go_back),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Nothing matching \"$query\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClear)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Clear search",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Formats continuous playback duration into localized string (e.g. 0 Mins, 45 Mins, 2.4 Hours). */
@Composable
fun formatPlaybackDuration(totalMs: Long): String {
    if (totalMs <= 0L) {
        return stringResource(R.string.playback_mins_format, 0)
    }
    val totalMinutes = totalMs / 60_000L
    return if (totalMinutes < 60) {
        val mins = totalMinutes.toInt().coerceAtLeast(1)
        stringResource(R.string.playback_mins_format, mins)
    } else {
        val hours = totalMs / 3_600_000f
        stringResource(R.string.playback_hours_format, hours)
    }
}

/** Formats songs count into localized string (e.g. 1 Song, 12 Songs). */
@Composable
fun formatSongsCount(count: Int): String {
    return if (count == 1) {
        stringResource(R.string.song_count_format_singular)
    } else {
        stringResource(R.string.songs_count_format, count)
    }
}

/**
 * Modern Stats summary card showing real downloaded songs count and continuous playback duration.
 */
@Composable
fun OfflineStatsSummaryCard(
    songCount: Int,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    val saffron = Color(0xFFFF7A29)
    val emerald = Color(0xFF59DDA9)
    val cardBg = Color(0xFF16181C)
    val cardBorder = Color(0xFF2E323A)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Songs Count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(saffron.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = saffron,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = formatSongsCount(songCount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
                Text(
                    text = "Saved Offline",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.55f),
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(cardBorder),
        )

        // Continuous Playback
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(emerald.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = emerald,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column {
                Text(
                    text = formatPlaybackDuration(totalDurationMs),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = emerald,
                    ),
                )
                Text(
                    text = "Continuous Play",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.55f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun OfflineBenefitRow(
    emoji: String,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 15.sp,
                ),
            )
        }
    }
}

/**
 * Offline Mode Banner with accurate dynamic stats, localized texts, interactive launch action cards,
 * animated breathing aura, and responsive actions.
 */
@Composable
fun DhvaniOfflineBanner(
    songCount: Int,
    totalDurationMs: Long,
    onExplore: () -> Unit,
    onImport: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saffron = Color(0xFFFF7A29)
    val emerald = Color(0xFF59DDA9)
    val cardBg = Color(0xFF16181C)
    val cardBorder = Color(0xFF2E323A)

    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Glowing Art Graphic Centerpiece with Ambient Breathing Glow
        Box(
            modifier = Modifier
                .size(126.dp * pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            saffron.copy(alpha = 0.28f * pulseAlpha),
                            Color(0xFF1B1612).copy(alpha = 0.4f),
                        )
                    )
                )
                .border(1.5.dp, saffron.copy(alpha = 0.45f * pulseAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF261A12))
                    .border(1.5.dp, saffron.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = saffron,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // Feature Pill Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF221710))
                .border(1.dp, saffron.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(emerald),
                )
                Text(
                    text = "ZERO CELLULAR DATA • OFFLINE VAULT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    color = saffron,
                )
            }
        }

        // Headline & Narrative
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Music That Never Stops",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Download your favorite tracks, albums, or playlists to listen anytime, anywhere — even without an internet connection or cellular data.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // ── Interactive Quick Launch Cards ───────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Card 1: Discover & Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, saffron.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onExplore)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(saffron.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = saffron,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Discover & Download Songs",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Text(
                        text = "Search trending charts, artists, and download tracks in 1-tap",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(saffron.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Search →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = saffron,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }

            // Card 2: Import Playlists & Albums
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, emerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { onImport?.invoke() ?: onExplore() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(emerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Playlists & Albums",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Text(
                        text = "Paste Spotify or YouTube links to sync and download directly",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(emerald.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Import →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = emerald,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }




        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Saffron Gradient Primary: Explore & Download Music
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF8A3D), Color(0xFFFF5E00))
                        )
                    )
                    .clickable(onClick = onExplore),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        text = "Explore & Download Music",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 15.sp,
                        ),
                    )
                }
            }

            // Dark Secondary: Go Back
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(50))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.offline_go_back),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
    }
}
