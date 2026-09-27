package com.music.dhvani.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import com.music.dhvani.data.playlist.PlaylistImportManager
import com.music.dhvani.data.playlist.PlaylistManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.AutoAwesome
import com.music.dhvani.data.playlist.PlaylistSyncManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.model.CARD_ART_PX
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.ui.components.thumbnailBorder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.LikeState
import com.music.dhvani.data.YtMusicRepository
import com.music.dhvani.data.model.HomeShelf
import com.music.dhvani.R
import com.music.dhvani.data.model.LibraryPage
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.UiState
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.download.Downloads
import com.music.dhvani.download.SavedCollection
import com.music.dhvani.ui.icons.DhvaniIcons
import com.music.dhvani.ui.components.LIBRARY_GRID_SPACING
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.PullToRefresh
import com.music.dhvani.ui.components.SHELF_CARD_WIDTH
import com.music.dhvani.ui.components.libraryGrid
import com.music.dhvani.ui.components.librarySkeleton
import com.music.dhvani.ui.player.MeshGradientBackground
import com.music.dhvani.ui.theme.uiDesignCard
import com.music.dhvani.ui.player.rememberArtworkColors
import com.music.dhvani.ui.replay.ReplayHeroCard
import java.util.Locale

/**
 * The signed-in library: the saved collections, as shelves of cards.
 *
 * Deliberately only the collections. This page used to end with two runs of
 * track rows — "Liked Music" and "Songs" — which are two overlapping answers
 * to the same question and read as one list that couldn't make up its mind: a
 * track that stopped being liked didn't leave the page, it moved down it, into
 * a section most people had taken for more of the same. Liked Music is a
 * playlist, and it is reached the way every other playlist here is, by opening
 * its card.
 *
 * The liked list is still fetched — it is what the rest of the app reads a
 * track's rating off (see MainViewModel's `likeStatuses`); it just isn't a
 * second place to browse it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    listState: LazyListState,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportPlaylist: (String?) -> Unit = {},
    onExportPlaylist: () -> Unit = {},
    /**
     * A shelf's "Show all" — every shelf's row here stops at five cards (see
     * [LibraryGridShelf]), so this is the only way to reach whatever didn't
     * fit.
     */
    onShowAll: (HomeShelf) -> Unit,
    /**
     * The Replay's leading card — minutes listened — or null before anything has
     * been played.
     *
     * Not drawn as a card here. This page is a list of places to go, and a card
     * is an object to look at; one sitting at the top of it read as the Replay
     * page's opening reprinted on a page about playlists and downloads. What the
     * card is used for instead is its *numbers* and its *artwork*: the button
     * below says what is behind it, and is painted in the colours of the record
     * that year was mostly spent on.
     */
    replayCard: ReplayHeroCard?,
    onOpenReplay: () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /**
     * The playlists downloaded whole, as cards behind the two device folders.
     *
     * They belong on that shelf because they are the same promise everything
     * else on it makes — here, now, without a network. Nothing is truncated:
     * the shelf is a row that scrolls, so "all of them" costs nothing.
     *
     * Downloaded *albums* are deliberately not here. An album stamps its name
     * onto each of its tracks, so the Downloads folder's Albums tab groups it
     * back up on its own and a card here would be a second door onto the same
     * list. A playlist has no tag anything can derive it from — its tracks are
     * off forty different releases — so this is the only place it can be reached
     * without going through that folder.
     */
    downloadedPlaylists: List<SavedCollection> = emptyList(),
) {
    val pinnedPlaylists by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()
    val localCustomPlaylists by AppSettings.localCustomPlaylists.collectAsStateWithLifecycle()
    val albumCustomPlaylists = remember(localCustomPlaylists) {
        localCustomPlaylists.filter { it.isAlbum }
    }
    val ytCustomPlaylists = remember(localCustomPlaylists) {
        localCustomPlaylists.filter { !it.isAlbum && it.source != "SPOTIFY" && it.source != "SPOTIFY_PROFILE" }
    }
    val spotifyCustomPlaylists = remember(localCustomPlaylists) {
        localCustomPlaylists.filter { !it.isAlbum && it.source == "SPOTIFY" }
    }
    val spotifyProfilePlaylists = remember(localCustomPlaylists) {
        localCustomPlaylists.filter { !it.isAlbum && it.source == "SPOTIFY_PROFILE" }
    }
    val albumShelfItems = remember(albumCustomPlaylists) {
        albumCustomPlaylists.map { pl ->
            val thumbs = pl.songs.mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }.distinct()
            val artistName = pl.effectiveAuthor ?: pl.songs.firstOrNull()?.artist
            val subtitle = if (!artistName.isNullOrBlank()) "$artistName • ${pl.songs.size} songs" else "${pl.songs.size} songs"
            ShelfItem(
                title = pl.displayTitle,
                subtitle = subtitle,
                thumbnailUrl = pl.coverUrl ?: thumbs.firstOrNull(),
                videoId = null,
                browseId = "local:custom:${pl.id}",
                mosaicUrls = if (pl.coverUrl.isNullOrBlank() && thumbs.size >= 4) thumbs.take(4) else emptyList(),
            )
        }
    }
    val ytShelfItems = remember(ytCustomPlaylists) {
        ytCustomPlaylists.map { pl ->
            val thumbs = pl.songs.mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }.distinct()
            ShelfItem(
                title = pl.title,
                subtitle = "${pl.songs.size} songs",
                thumbnailUrl = pl.coverUrl ?: thumbs.firstOrNull(),
                videoId = null,
                browseId = "local:custom:${pl.id}",
                mosaicUrls = if (pl.coverUrl.isNullOrBlank() && thumbs.size >= 4) thumbs.take(4) else emptyList(),
            )
        }
    }
    val spotifyShelfItems = remember(spotifyCustomPlaylists) {
        spotifyCustomPlaylists.map { pl ->
            val thumbs = pl.songs.mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }.distinct()
            ShelfItem(
                title = pl.title,
                subtitle = "${pl.songs.size} songs",
                thumbnailUrl = pl.coverUrl ?: thumbs.firstOrNull(),
                videoId = null,
                browseId = "local:custom:${pl.id}",
                mosaicUrls = if (pl.coverUrl.isNullOrBlank() && thumbs.size >= 4) thumbs.take(4) else emptyList(),
            )
        }
    }
    val spotifyProfileGroups = remember(spotifyProfilePlaylists) {
        spotifyProfilePlaylists
            .groupBy { it.effectiveAuthor ?: "Spotify User" }
            .map { (author, playlists) ->
                val allSongs = playlists.flatMap { it.songs }
                val thumbs = allSongs.mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }.distinct()
                val avatarUrl = playlists.firstNotNullOfOrNull { pl ->
                    pl.authorAvatarUrl?.takeIf { u ->
                        u.isNotBlank() && !u.contains("mosaic.scdn.co") && !pl.songs.any { s -> s.thumbnailUrl?.equals(u, ignoreCase = true) == true }
                    }
                }
                val shelfItems = playlists.map { pl ->
                    val plThumbs = pl.songs.mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }.distinct()
                    ShelfItem(
                        title = pl.displayTitle,
                        subtitle = "$author • ${pl.songs.size} songs",
                        thumbnailUrl = pl.coverUrl ?: plThumbs.firstOrNull(),
                        videoId = null,
                        browseId = "local:custom:${pl.id}",
                        mosaicUrls = if (pl.coverUrl.isNullOrBlank() && plThumbs.size >= 4) plThumbs.take(4) else emptyList(),
                    )
                }
                SpotifyProfileGroup(
                    authorName = author,
                    authorAvatarUrl = avatarUrl,
                    playlists = playlists,
                    totalSongs = allSongs.size,
                    shelfItems = shelfItems,
                    mosaicUrls = if (thumbs.size >= 4) thumbs.take(4) else emptyList(),
                )
            }
    }
    val spotifyProfileShelfItems = remember(spotifyProfileGroups) {
        spotifyProfileGroups.flatMap { it.shelfItems }
    }
    val importStatus by PlaylistImportManager.status.collectAsStateWithLifecycle()
    val localLikedSongs by LikeState.likedSongs.collectAsStateWithLifecycle()
    val likedList = if (signedIn && state is UiState.Success && state.data.likedSongs.isNotEmpty()) {
        state.data.likedSongs
    } else {
        localLikedSongs
    }
    val likedCount = likedList.size
    val latestLikedCover = likedList.firstOrNull()?.thumbnailUrl
    val likedShelfItem = ShelfItem(
        title = stringResource(R.string.liked_music),
        subtitle = if (likedCount == 1) "1 song" else "$likedCount songs",
        thumbnailUrl = latestLikedCover,
        videoId = null,
        browseId = YtMusicRepository.LIKED_MUSIC,
    )

    val context = LocalContext.current

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = {
            onRefresh()
            PlaylistSyncManager.checkAllAsync(context, force = true)
        },
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                Text(
                    text = stringResource(R.string.library),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }
            if (importStatus.isImporting) {
                item(key = "import_progress_banner") {
                    BackgroundImportBanner(
                        status = importStatus,
                        onCancel = { PlaylistImportManager.cancel() },
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                    )
                }
            }
            // Drawn whether or not anything has been played: with nothing behind
            // it the page still has to say the feature exists, or the only way
            // to discover it is to have already used it.
            item(key = "replay") { ReplayBanner(replayCard, onOpenReplay) }
            item(key = "shelf:$ON_DEVICE") {
                val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()
                val downloadsCover = remember(savedDownloads) {
                    savedDownloads.keys.firstNotNullOfOrNull { id ->
                        Downloads.localCoverUri(id)
                    }
                }
                val onDeviceShelf = HomeShelf(
                    title = ON_DEVICE,
                    items = listOf(
                        ShelfItem(
                            title = stringResource(R.string.downloads),
                            subtitle = stringResource(R.string.downloaded_songs),
                            thumbnailUrl = downloadsCover,
                            videoId = null,
                            browseId = "local:downloads",
                        ),
                        ShelfItem(
                            title = stringResource(R.string.local_music),
                            subtitle = stringResource(R.string.audio_files_on_device),
                            thumbnailUrl = null,
                            videoId = null,
                            browseId = "local:all",
                        ),
                    ) + downloadedPlaylists.map { playlist ->
                        ShelfItem(
                            title = playlist.title,
                            subtitle = playlist.subtitle.ifBlank { "Downloaded playlist" },
                            thumbnailUrl = playlist.thumbnailUrl,
                            videoId = null,
                            browseId = Downloads.pageIdFor(playlist.id),
                        )
                    },
                )
                LibraryGridShelf(
                    shelf = onDeviceShelf,
                    onItemClick = onShelfItemClick,
                    onItemLongPress = onShelfItemLongPress,
                    onShowAll = { onShowAll(onDeviceShelf) },
                )
            }
            if (!signedIn) {
                item(key = "shelf:$PLAYLISTS") {
                    val guestPlaylists = HomeShelf(PLAYLISTS, listOf(likedShelfItem))
                    PlaylistShelf(
                        shelf = guestPlaylists,
                        onItemClick = onShelfItemClick,
                        onItemLongPress = onShelfItemLongPress,
                        onNewPlaylist = onNewPlaylist,
                        onImportPlaylist = { onImportPlaylist(null) },
                        onExportPlaylist = onExportPlaylist,
                        onShowAll = { onShowAll(guestPlaylists) },
                    )
                }
                if (ytShelfItems.isNotEmpty()) {
                    item(key = "shelf:$YOUTUBE_PLAYLISTS") {
                        val ytShelf = HomeShelf(YOUTUBE_PLAYLISTS, ytShelfItems)
                        LibraryGridShelf(
                            shelf = ytShelf,
                            onItemClick = onShelfItemClick,
                            onItemLongPress = onShelfItemLongPress,
                            onShowAll = { onShowAll(ytShelf) },
                            pinnedPlaylists = pinnedPlaylists,
                            sectionIcon = DhvaniIcons.YouTube,
                        )
                    }
                }
                if (spotifyShelfItems.isNotEmpty()) {
                    item(key = "shelf:$SPOTIFY_PLAYLISTS") {
                        val spotifyShelf = HomeShelf(SPOTIFY_PLAYLISTS, spotifyShelfItems)
                        LibraryGridShelf(
                            shelf = spotifyShelf,
                            onItemClick = onShelfItemClick,
                            onItemLongPress = onShelfItemLongPress,
                            onShowAll = { onShowAll(spotifyShelf) },
                            pinnedPlaylists = pinnedPlaylists,
                            sectionIcon = DhvaniIcons.Spotify,
                        )
                    }
                }
                if (spotifyProfileGroups.isNotEmpty()) {
                    item(key = "shelf:$SPOTIFY_PROFILES") {
                        SpotifyProfileShelf(
                            profiles = spotifyProfileGroups,
                            onProfileClick = { group ->
                                onShowAll(
                                    HomeShelf(
                                        title = "${group.authorName}'s Spotify Playlists",
                                        items = group.shelfItems,
                                        subtitle = "${group.playlists.size} playlists • ${group.totalSongs} songs",
                                    )
                                )
                            },
                            onProfileDelete = { group ->
                                AppSettings.deleteLocalPlaylistsByAuthor(group.authorName)
                            },
                            sectionIcon = DhvaniIcons.Spotify,
                        )
                    }
                }
                if (albumShelfItems.isNotEmpty()) {
                    item(key = "shelf:$IMPORTED_ALBUMS") {
                        val albumsShelf = HomeShelf(IMPORTED_ALBUMS, albumShelfItems)
                        LibraryGridShelf(
                            shelf = albumsShelf,
                            onItemClick = onShelfItemClick,
                            onItemLongPress = onShelfItemLongPress,
                            onShowAll = { onShowAll(albumsShelf) },
                            pinnedPlaylists = pinnedPlaylists,
                            sectionIcon = Icons.Rounded.Album,
                        )
                    }
                }
            } else when (state) {
                is UiState.Loading -> librarySkeleton()
                is UiState.Error -> item {
                    MessageState(state.message, actionLabel = "Retry", onAction = onRetry)
                }
                is UiState.Success -> {
                    val shelves = state.data.shelves
                    if (shelves.none { it.title == PLAYLISTS }) {
                        item(key = "shelf:$PLAYLISTS") {
                            val defaultPlaylists = HomeShelf(PLAYLISTS, listOf(likedShelfItem))
                            PlaylistShelf(
                                shelf = defaultPlaylists,
                                onItemClick = onShelfItemClick,
                                onItemLongPress = onShelfItemLongPress,
                                onNewPlaylist = onNewPlaylist,
                                onImportPlaylist = { onImportPlaylist(null) },
                                onExportPlaylist = onExportPlaylist,
                                onShowAll = { onShowAll(defaultPlaylists) },
                            )
                        }
                        if (ytShelfItems.isNotEmpty()) {
                            item(key = "shelf:$YOUTUBE_PLAYLISTS") {
                                val ytShelf = HomeShelf(YOUTUBE_PLAYLISTS, ytShelfItems)
                                LibraryGridShelf(
                                    shelf = ytShelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onShowAll = { onShowAll(ytShelf) },
                                    pinnedPlaylists = pinnedPlaylists,
                                    sectionIcon = DhvaniIcons.YouTube,
                                )
                            }
                        }
                        if (spotifyShelfItems.isNotEmpty()) {
                            item(key = "shelf:$SPOTIFY_PLAYLISTS") {
                                val spotifyShelf = HomeShelf(SPOTIFY_PLAYLISTS, spotifyShelfItems)
                                LibraryGridShelf(
                                    shelf = spotifyShelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onShowAll = { onShowAll(spotifyShelf) },
                                    pinnedPlaylists = pinnedPlaylists,
                                    sectionIcon = DhvaniIcons.Spotify,
                                )
                            }
                        }
                        if (spotifyProfileGroups.isNotEmpty()) {
                            item(key = "shelf:$SPOTIFY_PROFILES") {
                                SpotifyProfileShelf(
                                    profiles = spotifyProfileGroups,
                                    onProfileClick = { group ->
                                        onShowAll(
                                            HomeShelf(
                                                title = "${group.authorName}'s Spotify Playlists",
                                                items = group.shelfItems,
                                                subtitle = "${group.playlists.size} playlists • ${group.totalSongs} songs",
                                            )
                                        )
                                    },
                                    onProfileDelete = { group ->
                                        AppSettings.deleteLocalPlaylistsByAuthor(group.authorName)
                                    },
                                    sectionIcon = DhvaniIcons.Spotify,
                                )
                            }
                        }
                        if (albumShelfItems.isNotEmpty()) {
                            item(key = "shelf:$IMPORTED_ALBUMS") {
                                val albumsShelf = HomeShelf(IMPORTED_ALBUMS, albumShelfItems)
                                LibraryGridShelf(
                                    shelf = albumsShelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onShowAll = { onShowAll(albumsShelf) },
                                    pinnedPlaylists = pinnedPlaylists,
                                    sectionIcon = Icons.Rounded.Album,
                                )
                            }
                        }
                    }
                    shelves.forEach { shelf ->
                        item(key = "shelf:${shelf.title}") {
                            if (shelf.title == PLAYLISTS) {
                                val otherItems = shelf.items.filterNot {
                                    it.browseId == YtMusicRepository.LIKED_MUSIC || it.browseId == "local:liked"
                                }
                                val withLiked = shelf.copy(items = listOf(likedShelfItem) + otherItems)
                                val pinnedFirst = withLiked.pinnedFirst(pinnedPlaylists)
                                PlaylistShelf(
                                    shelf = pinnedFirst,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onNewPlaylist = onNewPlaylist,
                                    onImportPlaylist = { onImportPlaylist(null) },
                                    onExportPlaylist = onExportPlaylist,
                                    onShowAll = { onShowAll(pinnedFirst) },
                                    pinnedPlaylists = pinnedPlaylists,
                                )
                            } else {
                                LibraryGridShelf(
                                    shelf = shelf,
                                    onItemClick = onShelfItemClick,
                                    onItemLongPress = onShelfItemLongPress,
                                    onShowAll = { onShowAll(shelf) },
                                )
                            }
                        }
                        if (shelf.title == PLAYLISTS) {
                            if (ytShelfItems.isNotEmpty()) {
                                item(key = "shelf:$YOUTUBE_PLAYLISTS") {
                                    val ytShelf = HomeShelf(YOUTUBE_PLAYLISTS, ytShelfItems)
                                    LibraryGridShelf(
                                        shelf = ytShelf,
                                        onItemClick = onShelfItemClick,
                                        onItemLongPress = onShelfItemLongPress,
                                        onShowAll = { onShowAll(ytShelf) },
                                        pinnedPlaylists = pinnedPlaylists,
                                        sectionIcon = DhvaniIcons.YouTube,
                                    )
                                }
                            }
                            if (spotifyShelfItems.isNotEmpty()) {
                                item(key = "shelf:$SPOTIFY_PLAYLISTS") {
                                    val spotifyShelf = HomeShelf(SPOTIFY_PLAYLISTS, spotifyShelfItems)
                                    LibraryGridShelf(
                                        shelf = spotifyShelf,
                                        onItemClick = onShelfItemClick,
                                        onItemLongPress = onShelfItemLongPress,
                                        onShowAll = { onShowAll(spotifyShelf) },
                                        pinnedPlaylists = pinnedPlaylists,
                                        sectionIcon = DhvaniIcons.Spotify,
                                    )
                                }
                            }
                            if (spotifyProfileGroups.isNotEmpty()) {
                                item(key = "shelf:$SPOTIFY_PROFILES") {
                                    SpotifyProfileShelf(
                                        profiles = spotifyProfileGroups,
                                        onProfileClick = { group ->
                                            onShowAll(
                                                HomeShelf(
                                                    title = "${group.authorName}'s Spotify Playlists",
                                                    items = group.shelfItems,
                                                    subtitle = "${group.playlists.size} playlists • ${group.totalSongs} songs",
                                                )
                                            )
                                        },
                                        onProfileDelete = { group ->
                                            AppSettings.deleteLocalPlaylistsByAuthor(group.authorName)
                                        },
                                        sectionIcon = DhvaniIcons.Spotify,
                                    )
                                }
                            }
                            if (albumShelfItems.isNotEmpty()) {
                                item(key = "shelf:$IMPORTED_ALBUMS") {
                                    val albumsShelf = HomeShelf(IMPORTED_ALBUMS, albumShelfItems)
                                    LibraryGridShelf(
                                        shelf = albumsShelf,
                                        onItemClick = onShelfItemClick,
                                        onItemLongPress = onShelfItemLongPress,
                                        onShowAll = { onShowAll(albumsShelf) },
                                        pinnedPlaylists = pinnedPlaylists,
                                        sectionIcon = Icons.Rounded.Album,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The way in to Replay, at the top of the page.
 *
 * On the Library tab rather than a tab of its own because that is what Replay
 * is — a view of what is already yours, alongside the playlists and the
 * downloads. A fifth tab would give a page most people open a handful of times
 * a year the same standing as Search.
 *
 * ## Why it is painted the way the cards are
 *
 * The mesh is the same one the Replay cards and the player's backdrop run —
 * sampled from the artwork of the record the period was mostly spent on, and
 * drifting rather than settling (see [MeshGradientBackground]'s `continuous`).
 * A fixed brand gradient here looked like a promo banner, which is the one thing
 * this must not be: it advertises the user's own listening, so it should be lit
 * by the user's own listening, and it should not look like anything else on the
 * page. With nothing played yet the mesh falls back to its stock colours, which
 * is a perfectly good button and still not a red rectangle.
 *
 * A single wide strip rather than a shelf of cards: there is exactly one of it,
 * and a carousel with one item in it always reads as a carousel that failed to
 * load the rest.
 */
@Composable
private fun ReplayBanner(card: ReplayHeroCard?, onClick: () -> Unit) {
    val palette = rememberArtworkColors(card?.artworkUrl)
    Box(
        Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .fillMaxWidth()
            .uiDesignCard(shape = RoundedCornerShape(18.dp), onClick = onClick),
    ) {
        // Behind the row and sized to it rather than given a height of its own,
        // so the strip is as tall as its two lines of type and no taller.
        Box(Modifier.matchParentSize()) {
            MeshGradientBackground(
                palette = palette,
                trackKey = card?.artworkUrl ?: "replay",
                continuous = false,
                // A short wide strip: at the backdrop's own radius the four
                // colours blur into one wash before they reach its ends.
                blurRadius = 28.dp,
            )
        }
        // The mesh carries a vertical scrim of its own, pitched for a full
        // screen where it has hundreds of dp to fade across; over a strip this
        // short it lands as a flat darkening of the whole thing. So this one is
        // kept deliberately light and runs the other way — just enough under the
        // words on the left, and almost nothing over the colour on the right,
        // which is the half anyone actually sees as a gradient.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Your Replay",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    // The numbers when there are any, because "5,231 minutes" is
                    // a reason to tap and a description of the feature is not.
                    text = card?.let { "${it.value} ${it.label.lowercase(Locale.ROOT)} · ${it.detail}" }
                        ?: "Top songs, artists, albums and genres — counted on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = DhvaniIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * The one shelf on this page that can be written to: it leads with the tile
 * that creates a playlist, and holding a card gets rename and delete on top of
 * the queue actions every other shelf's menu offers.
 */
@Composable
private fun PlaylistShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onShowAll: () -> Unit,
    pinnedPlaylists: List<String> = emptyList(),
) {
    LibraryGridShelf(
        shelf = shelf,
        onItemClick = onItemClick,
        onItemLongPress = onItemLongPress,
        onShowAll = onShowAll,
        pinnedPlaylists = pinnedPlaylists,
        leadingCard = {
            NewShelfCard(
                icon = DhvaniIcons.Plus,
                label = "New playlist",
                subtitle = stringResource(R.string.saved_to_youtube_music),
                onClick = onNewPlaylist,
            )
        },
        leadingCardSecond = {
            NewShelfCard(
                icon = Icons.Rounded.FileDownload,
                label = "Import",
                subtitle = "Link, album or file",
                onClick = onImportPlaylist,
            )
        },
        leadingCardThird = {
            NewShelfCard(
                icon = Icons.Rounded.FileUpload,
                label = "Export playlist",
                subtitle = "Save as M3U or JSON",
                onClick = onExportPlaylist,
            )
        },
    )
}

/** A Library shelf's preview row never swipes past this many cards. */
private const val LIBRARY_ROW_MAX_ITEMS = 5

/**
 * A Library shelf: a sideways-scrolling row of [SHELF_CARD_WIDTH] cards, the
 * same as every other shelf, but stopped at [LIBRARY_ROW_MAX_ITEMS] rather
 * than left to run the shelf's whole length — with a "Show all" beside the
 * title whenever there's more than that, opening the rest as a
 * vertically-scrolling grid instead. See [LibraryGridPage].
 *
 * [leadingCard], if given, occupies the first slot and counts against that
 * cap — see [PlaylistShelf].
 */
@Composable
internal fun LibraryGridShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onShowAll: () -> Unit,
    leadingCard: (@Composable () -> Unit)? = null,
    leadingCardSecond: (@Composable () -> Unit)? = null,
    leadingCardThird: (@Composable () -> Unit)? = null,
    pinnedPlaylists: List<String> = emptyList(),
    sectionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val leadingCount = (if (leadingCard != null) 1 else 0) +
        (if (leadingCardSecond != null) 1 else 0) +
        (if (leadingCardThird != null) 1 else 0)
    val visibleItems = shelf.items.take((LIBRARY_ROW_MAX_ITEMS - leadingCount).coerceAtLeast(0))
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle,
            onShowAll = onShowAll.takeIf { shelf.items.size + leadingCount > LIBRARY_ROW_MAX_ITEMS },
            leadingIcon = sectionIcon,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            leadingCardSecond?.let { card -> item(key = "leading_second") { card() } }
            leadingCardThird?.let { card -> item(key = "leading_third") { card() } }
            items(visibleItems) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
                )
            }
        }
    }
}

data class SpotifyProfileGroup(
    val authorName: String,
    val authorAvatarUrl: String?,
    val playlists: List<AppSettings.CustomLocalPlaylist>,
    val totalSongs: Int,
    val shelfItems: List<ShelfItem>,
    val mosaicUrls: List<String> = emptyList(),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SpotifyProfileShelf(
    profiles: List<SpotifyProfileGroup>,
    onProfileClick: (SpotifyProfileGroup) -> Unit,
    onProfileDelete: (SpotifyProfileGroup) -> Unit,
    modifier: Modifier = Modifier,
    sectionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val spotifyGreen = Color(0xFF1DB954)
    var profileToDelete by remember { mutableStateOf<SpotifyProfileGroup?>(null) }

    Column(modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = SPOTIFY_PROFILES,
            subtitle = if (profiles.size == 1) {
                "${profiles.first().authorName} • ${profiles.first().playlists.size} playlists"
            } else {
                "${profiles.size} imported profiles"
            },
            onShowAll = null,
            leadingIcon = sectionIcon,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
        ) {
            items(profiles, key = { it.authorName }) { profile ->
                SpotifyProfileCard(
                    profile = profile,
                    onClick = { onProfileClick(profile) },
                    onLongClick = { profileToDelete = profile },
                )
            }
        }
    }

    profileToDelete?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = {
                Text(
                    text = "Delete Profile Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all ${target.playlists.size} playlists imported from ${target.authorName}?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onProfileDelete(target)
                        profileToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpotifyProfileCard(
    profile: SpotifyProfileGroup,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val spotifyGreen = Color(0xFF1DB954)
    LaunchedEffect(profile.authorName) {
        if (profile.authorAvatarUrl.isNullOrBlank()) {
            val remoteAvatar = PlaylistManager.fetchSpotifyUserAvatar(profile.authorName)
            if (!remoteAvatar.isNullOrBlank()) {
                AppSettings.updateAuthorAvatar(profile.authorName, remoteAvatar)
            }
        }
    }

    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(118.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(CircleShape)
                    .border(2.dp, spotifyGreen.copy(alpha = 0.5f), CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!profile.authorAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile.authorAvatarUrl.artworkAt(ROW_ART_PX),
                        contentDescription = profile.authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = spotifyGreen,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            // Small Spotify verified badge at bottom-end
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(spotifyGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = DhvaniIcons.Spotify,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = profile.authorName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "${profile.playlists.size} playlists • ${profile.totalSongs} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BackgroundImportBanner(
    status: PlaylistImportManager.ImportStatus,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spotifyGreen = Color(0xFF1DB954)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = spotifyGreen.copy(alpha = 0.12f),
        ),
        border = BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = spotifyGreen,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (status.isBatch) {
                            "Importing ${status.title} (${status.currentBatchIndex}/${status.totalBatchCount})"
                        } else {
                            "Importing ${status.title}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (status.currentSong.isNotBlank()) {
                        Text(
                            text = "Matching: ${status.currentSong}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(status.progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = spotifyGreen,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = spotifyGreen,
                trackColor = spotifyGreen.copy(alpha = 0.2f),
            )
        }
    }
}

/**
 * Everything a Library shelf's "Show all" opens onto — the same cards, at the
 * same [libraryGrid] width, run down the screen instead of stopping at one row.
 */
@Composable
fun LibraryGridPage(
    shelf: HomeShelf,
    gridState: LazyGridState,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNewPlaylist: (() -> Unit)? = null,
    onImportPlaylist: (() -> Unit)? = null,
    onExportPlaylist: (() -> Unit)? = null,
) {
    val pinnedPlaylists by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()
    val sortedShelf = shelf.pinnedFirst(pinnedPlaylists)

    var searchQuery by rememberSaveable(shelf.title) { mutableStateOf("") }
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var sortMode by rememberSaveable { mutableStateOf(0) } // 0: Default, 1: A-Z, 2: Most Songs

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSpotifyProfile = shelf.title == SPOTIFY_PROFILES || shelf.title.endsWith("Spotify Playlists")

    val isSyncChecking by PlaylistSyncManager.isChecking.collectAsStateWithLifecycle()
    val pendingUpdates by PlaylistSyncManager.pendingUpdates.collectAsStateWithLifecycle()

    val filteredItems = remember(sortedShelf.items, searchQuery, sortMode, isSpotifyProfile) {
        var list = if (isSpotifyProfile) {
            sortedShelf.items.distinctBy { it.title.lowercase().trim() }
        } else {
            sortedShelf.items.distinctBy { it.browseId ?: it.title }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            list = list.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                    it.subtitle.lowercase(Locale.getDefault()).contains(q)
            }
        }
        when (sortMode) {
            1 -> list.sortedBy { it.title.lowercase(Locale.getDefault()) }
            2 -> list.sortedByDescending { item ->
                Regex("""(\d+)\s+songs?""").find(item.subtitle)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            else -> list
        }
    }

    val totalSongs = remember(filteredItems) {
        filteredItems.sumOf { item ->
            Regex("""(\d+)\s+songs?""").find(item.subtitle)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
    }
    val profileCreator = remember(shelf.title, sortedShelf.items) {
        if (shelf.title.endsWith("'s Spotify Playlists")) {
            shelf.title.removeSuffix("'s Spotify Playlists")
        } else {
            sortedShelf.items.firstNotNullOfOrNull { item ->
                if (item.subtitle.contains(" • ")) item.subtitle.substringBefore(" • ") else null
            }
        }
    }

    val relevantUpdates = remember(pendingUpdates, profileCreator) {
        pendingUpdates.filter { update ->
            when (update) {
                is PlaylistSyncManager.SyncUpdate.NewPlaylistsInProfile ->
                    profileCreator == null || update.profileUsername.equals(profileCreator, ignoreCase = true)
                is PlaylistSyncManager.SyncUpdate.NewTracksInPlaylist ->
                    profileCreator == null || update.author?.equals(profileCreator, ignoreCase = true) == true
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val grid = libraryGrid(maxWidth - PAGE_GUTTER * 2)
        val activeColumns = if (isGridView) grid.columns else 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(activeColumns),
            state = gridState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(if (isGridView) 18.dp else 4.dp),
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
        ) {
            // Hero Banner for Spotify Profile or Other Special Shelves
            item(key = "shelf_hero_header", span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
                    if (isSpotifyProfile) {
                        val spotifyGreen = Color(0xFF1DB954)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            spotifyGreen.copy(alpha = 0.22f),
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        )
                                    )
                                )
                                .border(1.dp, spotifyGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(18.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        val creatorAvatar = remember(profileCreator) {
                                            if (profileCreator != null) {
                                                AppSettings.localCustomPlaylists.value.firstNotNullOfOrNull { pl ->
                                                    if (pl.effectiveAuthor.equals(profileCreator, ignoreCase = true)) {
                                                        pl.authorAvatarUrl?.takeIf { u -> u.isNotBlank() && !u.contains("mosaic.scdn.co") }
                                                    } else null
                                                }
                                            } else null
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(spotifyGreen.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!creatorAvatar.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = creatorAvatar.artworkAt(ROW_ART_PX),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = DhvaniIcons.Spotify,
                                                    contentDescription = null,
                                                    tint = spotifyGreen,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (profileCreator != null) "$profileCreator's Spotify Playlists" else "Spotify Profile Playlists",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${filteredItems.size} playlists • $totalSongs songs total",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        // Sync / Check for updates button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(spotifyGreen.copy(alpha = 0.18f))
                                                .clickable(enabled = !isSyncChecking) {
                                                    scope.launch {
                                                        Toast.makeText(context, "Checking ${profileCreator ?: "Spotify"} for updates...", Toast.LENGTH_SHORT).show()
                                                        PlaylistSyncManager.checkAllAsync(context, force = true)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSyncChecking) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        strokeWidth = 2.dp,
                                                        color = spotifyGreen,
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Sync,
                                                        contentDescription = "Sync",
                                                        tint = spotifyGreen,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    text = if (isSyncChecking) "Syncing..." else "Sync",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = spotifyGreen,
                                                )
                                            }
                                        }

                                        if (onImportPlaylist != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(spotifyGreen.copy(alpha = 0.18f))
                                                    .clickable(onClick = onImportPlaylist)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.FileDownload,
                                                        contentDescription = "Import",
                                                        tint = spotifyGreen,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = "Import",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = spotifyGreen,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pending Updates Cards if updates found for this profile
                        relevantUpdates.forEach { update ->
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = spotifyGreen.copy(alpha = 0.14f)),
                                border = BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = spotifyGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        when (update) {
                                            is PlaylistSyncManager.SyncUpdate.NewPlaylistsInProfile -> {
                                                Text(
                                                    text = "New from ${update.profileUsername}!",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                )
                                                Text(
                                                    text = "${update.newPlaylists.size} new playlist(s) found on Spotify",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            is PlaylistSyncManager.SyncUpdate.NewTracksInPlaylist -> {
                                                Text(
                                                    text = "New Songs in \"${update.playlistTitle}\"",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                )
                                                Text(
                                                    text = "+${update.newTracks.size} new songs added (e.g. ${update.newTracks.first().title})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    when (update) {
                                        is PlaylistSyncManager.SyncUpdate.NewTracksInPlaylist -> {
                                            Button(
                                                onClick = {
                                                    PlaylistSyncManager.syncNewTracksToPlaylist(context, update)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp),
                                            ) {
                                                Text("Update", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        is PlaylistSyncManager.SyncUpdate.NewPlaylistsInProfile -> {
                                            if (onImportPlaylist != null) {
                                                Button(
                                                    onClick = onImportPlaylist,
                                                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp),
                                                ) {
                                                    Text("Import", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Search & Controls Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Search bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = DhvaniIcons.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search in ${shelf.title}...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            maxLines = 1,
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onBackground,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Sort toggle button
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { sortMode = (sortMode + 1) % 3 }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = "Sort",
                                    tint = if (sortMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = when (sortMode) {
                                        1 -> "A-Z"
                                        2 -> "Songs"
                                        else -> "Default"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (sortMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Grid / List toggle button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { isGridView = !isGridView },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = if (isGridView) "Switch to list" else "Switch to grid",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Leading action cards when not searching
            if (searchQuery.isEmpty()) {
                if (onNewPlaylist != null) {
                    item(key = "leading") {
                        if (isGridView) {
                            NewShelfCard(
                                icon = DhvaniIcons.Plus,
                                label = "New playlist",
                                subtitle = stringResource(R.string.saved_to_youtube_music),
                                onClick = onNewPlaylist,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onNewPlaylist)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = DhvaniIcons.Plus,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "New playlist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = stringResource(R.string.saved_to_youtube_music),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (onImportPlaylist != null && !isSpotifyProfile) {
                    item(key = "leading_second") {
                        if (isGridView) {
                            NewShelfCard(
                                icon = Icons.Rounded.FileDownload,
                                label = "Import playlist",
                                subtitle = "M3U, JSON or link",
                                onClick = onImportPlaylist,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onImportPlaylist)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Import playlist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = "M3U, JSON or link",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (onExportPlaylist != null) {
                    item(key = "leading_export") {
                        if (isGridView) {
                            NewShelfCard(
                                icon = Icons.Rounded.FileUpload,
                                label = "Export playlist",
                                subtitle = "Save as M3U or JSON",
                                onClick = onExportPlaylist,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onExportPlaylist)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Export playlist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = "Save as M3U or JSON",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty search state
            if (filteredItems.isEmpty()) {
                item(key = "empty_state", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No playlists found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Try searching for a different name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { searchQuery = "" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "Clear search",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.browseId ?: it.title }) { item ->
                    val isPinned = item.browseId != null && item.browseId in pinnedPlaylists
                    if (isGridView) {
                        ShelfCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongPress = { onItemLongPress(item) },
                            modifier = Modifier.fillMaxWidth(),
                            isPinned = isPinned,
                            titleMaxLines = 2,
                        )
                    } else {
                        PlaylistListRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongPress = { onItemLongPress(item) },
                            isPinned = isPinned,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistListRow(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail or 2x2 mosaic
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .uiDesignCard(shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.mosaicUrls.size >= 4) {
                val mosaic = item.mosaicUrls.take(4)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .thumbnailBorder(RoundedCornerShape(12.dp)),
                ) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AsyncImage(
                                model = mosaic[0].artworkAt(CARD_ART_PX / 2),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.width(1.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AsyncImage(
                                model = mosaic[1].artworkAt(CARD_ART_PX / 2),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.height(1.dp))
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AsyncImage(
                                model = mosaic[2].artworkAt(CARD_ART_PX / 2),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.width(1.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            AsyncImage(
                                model = mosaic[3].artworkAt(CARD_ART_PX / 2),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            } else if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .thumbnailBorder(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(
                        imageVector = DhvaniIcons.Pin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onLongPress,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Moves whichever of this shelf's cards are in [pinned] to the front, in the
 * order they were pinned, leaving everything else in its existing order behind
 * them.
 *
 * A no-op on any shelf that isn't Playlists: [pinned] only ever holds playlist
 * browse ids, so an album or artist shelf never has a card that matches.
 */
private fun HomeShelf.pinnedFirst(pinned: List<String>): HomeShelf {
    if (pinned.isEmpty()) return this
    val byId = items.filter { it.browseId != null }.associateBy { it.browseId }
    val pinnedItems = pinned.mapNotNull { byId[it] }
    if (pinnedItems.isEmpty()) return this
    val pinnedSet = pinnedItems.toSet()
    return copy(items = pinnedItems + items.filter { it !in pinnedSet })
}

/** The library feed whose cards are the account's own — see [PlaylistShelf]. */
private const val PLAYLISTS = YtMusicRepository.PLAYLISTS_SHELF
private const val ON_DEVICE = "On Device"
const val YOUTUBE_PLAYLISTS = "YouTube Playlists"
const val SPOTIFY_PLAYLISTS = "Spotify Playlists"
const val SPOTIFY_PROFILES = "Spotify Profile Playlists"
const val IMPORTED_ALBUMS = "Albums"
