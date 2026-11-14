package com.music.dhvani.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.model.durationMillis
import com.music.dhvani.download.Downloads
import com.music.dhvani.ui.components.CoverPlaceholder
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.ROW_DIVIDER_INSET
import com.music.dhvani.ui.components.UserAvatar
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.util.Locale

private const val TAB_SONGS = 0
private const val TAB_ARTISTS = 1
private const val TAB_ALBUMS = 2

private enum class CachedSortOption(val label: String) {
    ALPHABETICAL_AZ("Alphabetical (A to Z)"),
    ALPHABETICAL_ZA("Alphabetical (Z to A)"),
    DATE_ADDED("Date added (newest first)"),
    DATE_MODIFIED("Date modified (newest first)"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CachedSongsScreen(
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_SONGS) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(CachedSortOption.DATE_ADDED) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    // Drill-down state for artist / album
    var drillDownLabel by remember { mutableStateOf<String?>(null) }
    var drillDownSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var drillDownArt by remember { mutableStateOf<String?>(null) }
    var drillDownIsArtist by remember { mutableStateOf(false) }

    val inDrillDown = drillDownLabel != null
    val leaveDrillDown = {
        drillDownLabel = null
        drillDownSongs = emptyList()
        drillDownArt = null
    }

    BackHandler(enabled = inDrillDown) { leaveDrillDown() }
    BackHandler(enabled = searchOpen && !inDrillDown) {
        searchOpen = false
        searchQuery = ""
    }

    // Sort songs
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            CachedSortOption.ALPHABETICAL_AZ -> songs.sortedBy { it.title.lowercase(Locale.ROOT) }
            CachedSortOption.ALPHABETICAL_ZA -> songs.sortedByDescending { it.title.lowercase(Locale.ROOT) }
            CachedSortOption.DATE_ADDED -> songs
            CachedSortOption.DATE_MODIFIED -> songs.reversed()
        }
    }

    // Filtered songs
    val filteredSongs = remember(sortedSongs, searchQuery) {
        if (searchQuery.isBlank()) sortedSongs
        else sortedSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                (it.albumName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    // Artists grouping
    val artists = remember(sortedSongs, searchQuery, sortOption) {
        val grouped = sortedSongs.groupBy { it.artist.trim().ifBlank { "Unknown Artist" } }.entries
        val filtered = if (searchQuery.isBlank()) grouped else grouped.filter { it.key.contains(searchQuery, ignoreCase = true) }
        when (sortOption) {
            CachedSortOption.ALPHABETICAL_AZ -> filtered.sortedBy { it.key.lowercase(Locale.ROOT) }
            CachedSortOption.ALPHABETICAL_ZA -> filtered.sortedByDescending { it.key.lowercase(Locale.ROOT) }
            else -> filtered.toList()
        }
    }

    // Albums grouping
    val albums = remember(sortedSongs, searchQuery, sortOption) {
        val grouped = sortedSongs.groupBy {
            val album = it.albumName?.trim()
            if (!album.isNullOrBlank()) album else it.title.trim()
        }.entries
        val filtered = if (searchQuery.isBlank()) grouped else grouped.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
                it.value.firstOrNull()?.artist?.contains(searchQuery, ignoreCase = true) == true
        }
        when (sortOption) {
            CachedSortOption.ALPHABETICAL_AZ -> filtered.sortedBy { it.key.lowercase(Locale.ROOT) }
            CachedSortOption.ALPHABETICAL_ZA -> filtered.sortedByDescending { it.key.lowercase(Locale.ROOT) }
            else -> filtered.toList()
        }
    }

    // Total play time
    val totalDurationMs = remember(songs) { songs.sumOf { it.durationMillis() } }
    val totalDurationMinutes = remember(totalDurationMs) { (totalDurationMs / 60000L).coerceAtLeast(1L) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0F0F12))) {
        // Main list content
        if (inDrillDown) {
            // Drill Down View
            DrillDownView(
                label = drillDownLabel.orEmpty(),
                artworkUrl = drillDownArt,
                isArtist = drillDownIsArtist,
                songs = drillDownSongs,
                onSongClick = onSongClick,
                onSongLongPress = onSongLongPress,
                onSongSwipe = onSongSwipe,
                onShuffle = onShuffle,
                onBack = leaveDrillDown,
                savedDownloads = savedDownloads,
                contentPadding = contentPadding,
            )
        } else {
            // Unified LazyColumn - Header never re-renders or refreshes when changing tabs!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = maxOf(contentPadding.calculateTopPadding(), 96.dp) + 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                // Hero Header item
                item(key = "hero_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PAGE_GUTTER),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(18.dp))

                        // Cover Artwork Sleeve
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF222328))
                                .border(1.dp, Color(0xFF2E2F35), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CoverPlaceholder(
                                icon = Icons.Rounded.Storage,
                                base = Color(0xFF222328),
                                tint = Color(0xFF8E8E95),
                                iconFraction = 0.36f,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // Title
                        Text(
                            text = "Cached songs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(4.dp))

                        // Subtitle
                        Text(
                            text = "Songs from YouTube and JioSaavn in the song cache",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9E9EA5),
                                fontSize = 13.5.sp,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                        Spacer(Modifier.height(4.dp))

                        // Stats: "14 Tracks · 48 min"
                        Text(
                            text = "${songs.size} ${if (songs.size == 1) "Track" else "Tracks"} · $totalDurationMinutes min",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF8E8E95),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(18.dp))

                        // Action Buttons: [ ▶ Play ] [ 🔀 Shuffle ] [ ⬇ ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Play Pill (Solid white)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .clickable {
                                        if (filteredSongs.isNotEmpty()) {
                                            haptics.play(Haptic.Resume)
                                            onSongClick(filteredSongs, 0)
                                        }
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.Black,
                                )
                            }

                            // Shuffle Pill (Dark translucent)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF222328))
                                    .border(1.dp, Color(0xFF32333A), RoundedCornerShape(50))
                                    .clickable {
                                        if (filteredSongs.isNotEmpty()) {
                                            haptics.play(Haptic.Resume)
                                            onShuffle(filteredSongs)
                                        }
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.White,
                                )
                            }

                            // Download All Circular Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222328))
                                    .border(1.dp, Color(0xFF32333A), CircleShape)
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            haptics.play(Haptic.ToggleOn)
                                            songs.forEach { song ->
                                                Downloads.enqueue(context, song, "Cached songs")
                                            }
                                            Toast.makeText(
                                                context,
                                                "Saving ${songs.size} cached tracks to Downloads…",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = "Download all to storage",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Segmented Tab Bar: [ Songs | Artists | Albums ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1B1B20))
                                .border(1.dp, Color(0xFF2A2B32), RoundedCornerShape(50))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val tabs = listOf("Songs", "Artists", "Albums")
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) Color(0xFF55555C) else Color.Transparent)
                                        .clickable {
                                            if (selectedTab != index) {
                                                haptics.play(Haptic.Select)
                                                selectedTab = index
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFF9E9EA5),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                    }
                }

                // Content according to selected tab - ONLY this part changes, header stays completely static!
                when (selectedTab) {
                    TAB_SONGS -> {
                        if (filteredSongs.isEmpty()) {
                            item(key = "songs_empty") {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank()) "No cached tracks match \"$searchQuery\""
                                    else "No songs cached yet. Stream any track to automatically cache it here.",
                                )
                            }
                        } else if (isGridView) {
                            // Grid view for songs (2-column cards)
                            val chunked = filteredSongs.chunked(2)
                            itemsIndexed(chunked, key = { index, _ -> "song_grid_row_$index" }) { _, rowSongs ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowSongs.forEach { song ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CachedSongGridCard(
                                                song = song,
                                                onClick = {
                                                    val idx = filteredSongs.indexOf(song)
                                                    if (idx >= 0) onSongClick(filteredSongs, idx)
                                                },
                                                onLongPress = { onSongLongPress(song) },
                                            )
                                        }
                                    }
                                    if (rowSongs.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            // List view for songs
                            itemsIndexed(
                                items = filteredSongs,
                                key = { index, s -> "${s.videoId}_$index" },
                            ) { index, song ->
                                val isDownloaded = song.videoId in savedDownloads
                                CachedSongRow(
                                    song = song,
                                    isDownloaded = isDownloaded,
                                    onClick = { onSongClick(filteredSongs, index) },
                                    onLongPress = { onSongLongPress(song) },
                                    onDownloadClick = {
                                        Downloads.enqueue(context, song, "Cached songs")
                                        haptics.play(Haptic.ToggleOn)
                                        Toast.makeText(context, "Saving \"${song.title}\" to Downloads…", Toast.LENGTH_SHORT).show()
                                    },
                                )
                                if (index < filteredSongs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                        thickness = 0.5.dp,
                                        color = Color(0xFF222328),
                                    )
                                }
                            }
                        }
                    }

                    TAB_ARTISTS -> {
                        if (artists.isEmpty()) {
                            item(key = "artists_empty") {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank()) "No artists match \"$searchQuery\""
                                    else "No cached artists found.",
                                )
                            }
                        } else if (isGridView) {
                            // Grid view for artists
                            val chunked = artists.chunked(2)
                            itemsIndexed(chunked, key = { index, _ -> "artist_grid_row_$index" }) { _, rowArtists ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowArtists.forEach { (artist, artistSongs) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CachedArtistGridCard(
                                                name = artist,
                                                songs = artistSongs,
                                                onClick = {
                                                    drillDownLabel = artist
                                                    drillDownSongs = artistSongs
                                                    drillDownArt = artistSongs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                                                    drillDownIsArtist = true
                                                },
                                            )
                                        }
                                    }
                                    if (rowArtists.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            // List view for artists
                            items(artists, key = { it.key }) { (artist, artistSongs) ->
                                CachedArtistRow(
                                    name = artist,
                                    songs = artistSongs,
                                    onClick = {
                                        drillDownLabel = artist
                                        drillDownSongs = artistSongs
                                        drillDownArt = artistSongs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                                        drillDownIsArtist = true
                                    },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = Color(0xFF222328),
                                )
                            }
                        }
                    }

                    TAB_ALBUMS -> {
                        if (albums.isEmpty()) {
                            item(key = "albums_empty") {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank()) "No albums match \"$searchQuery\""
                                    else "No cached albums found.",
                                )
                            }
                        } else if (isGridView) {
                            // Grid view for albums
                            val chunked = albums.chunked(2)
                            itemsIndexed(chunked, key = { index, _ -> "album_grid_row_$index" }) { _, rowAlbums ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowAlbums.forEach { (albumName, albumSongs) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CachedAlbumGridCard(
                                                title = albumName,
                                                artist = albumSongs.firstOrNull()?.artist.orEmpty(),
                                                songs = albumSongs,
                                                onClick = {
                                                    drillDownLabel = albumName
                                                    drillDownSongs = albumSongs
                                                    drillDownArt = albumSongs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                                                    drillDownIsArtist = false
                                                },
                                            )
                                        }
                                    }
                                    if (rowAlbums.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            // List view for albums
                            items(albums, key = { it.key }) { (albumName, albumSongs) ->
                                CachedAlbumRow(
                                    title = albumName,
                                    artist = albumSongs.firstOrNull()?.artist.orEmpty(),
                                    songs = albumSongs,
                                    onClick = {
                                        drillDownLabel = albumName
                                        drillDownSongs = albumSongs
                                        drillDownArt = albumSongs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                                        drillDownIsArtist = false
                                    },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = Color(0xFF222328),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Bar: Back circle on left, Pill (Search, Sort, Profile) on right
        // When searchOpen == true: search bar is right in the top bar!
        if (searchOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back circular button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1F24).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF2E2F35), CircleShape)
                        .clickable {
                            searchOpen = false
                            searchQuery = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Close search",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Full search bar capsule in top bar!
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1E1F24).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF2E2F35), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF9E9EA5),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontSize = 14.5.sp,
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search cached tracks, artists…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF6E6E75),
                                        fontSize = 14.5.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF9E9EA5),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                        )
                    }
                }

                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            searchOpen = false
                            searchQuery = ""
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular Back button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1F24).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF2E2F35), CircleShape)
                        .clickable {
                            if (inDrillDown) leaveDrillDown() else onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Right Action Capsule: [ Search | Sort/View | UserAvatar ]
                Box {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1E1F24).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF2E2F35), RoundedCornerShape(50))
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Search icon
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search cached tracks",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { searchOpen = true },
                        )

                        // Sort / View mode icon
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "View and sort options",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { sortMenuOpen = true },
                        )

                        // User profile avatar with larger size & custom / preset avatar support
                        UserAvatar(
                            size = 32.dp,
                            borderWidth = 1.5.dp,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            onClick = onProfileClick,
                        )
                    }

                    // Sort & View Dropdown Menu (Matches Reference Screenshots)
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                        offset = DpOffset(x = 0.dp, y = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color(0xFF1E1F24),
                        border = BorderStroke(1.dp, Color(0xFF2E2F35)),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp,
                    ) {
                        Spacer(Modifier.height(4.dp))
                        // 1. Switch to grid / list view
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isGridView) "Switch to list view" else "Switch to grid view",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            onClick = {
                                isGridView = !isGridView
                                sortMenuOpen = false
                                haptics.play(Haptic.Select)
                            },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFF2E2F35),
                        )

                        // 2. Sort Options: A to Z, Z to A, Date added, Date modified
                        CachedSortOption.entries.forEach { option ->
                            val isSelected = option == sortOption
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        ),
                                        color = Color.White,
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                } else null,
                                onClick = {
                                    sortOption = option
                                    sortMenuOpen = false
                                    haptics.play(Haptic.Select)
                                },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CachedSongRow(
    song: Song,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222328)),
        )

        Spacer(Modifier.width(14.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                ),
                color = Color(0xFF9E9EA5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Duration text
        song.durationText?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                ),
                color = Color(0xFF8E8E95),
            )
        }

        Spacer(Modifier.width(6.dp))

        // Download button
        if (isDownloaded) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Saved offline",
                    tint = Color(0xFF59DDA9),
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = "Save to downloads",
                    tint = Color(0xFF9E9EA5),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // 3-dots More options
        IconButton(
            onClick = onLongPress,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF9E9EA5),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CachedSongGridCard(
    song: Song,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF18181C))
            .border(0.5.dp, Color(0xFF26262C), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Play circle badge overlay on bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
            // Duration pill on bottom-right
            song.durationText?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Color(0xFF9E9EA5),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CachedArtistRow(
    name: String,
    songs: List<Song>,
    onClick: () -> Unit,
) {
    val avatarUrl = remember(songs) { songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circular Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF222328))
                .border(1.dp, Color(0xFF2E2F35), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color(0xFF8E8E95),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                ),
                color = Color(0xFF9E9EA5),
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF6E6E75),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CachedArtistGridCard(
    name: String,
    songs: List<Song>,
    onClick: () -> Unit,
) {
    val avatarUrl = remember(songs) { songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular Avatar only (NO square background card, NO square border)
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(Color(0xFF222328))
                .border(1.dp, Color(0xFF2E2F35), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color(0xFF8E8E95),
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = Color(0xFF9E9EA5),
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CachedAlbumRow(
    title: String,
    artist: String,
    songs: List<Song>,
    onClick: () -> Unit,
) {
    val artworkUrl = remember(songs) { songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rounded Square Album Cover
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF222328))
                .border(1.dp, Color(0xFF2E2F35), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = Color(0xFF8E8E95),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val sub = if (artist.isNotBlank() && artist != title) "$artist · ${songs.size} ${if (songs.size == 1) "song" else "songs"}"
            else "${songs.size} ${if (songs.size == 1) "song" else "songs"}"
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                ),
                color = Color(0xFF9E9EA5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF6E6E75),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CachedAlbumGridCard(
    title: String,
    artist: String,
    songs: List<Song>,
    onClick: () -> Unit,
) {
    val artworkUrl = remember(songs) { songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222328))
                .border(1.dp, Color(0xFF2E2F35), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl.artworkAt(ROW_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = Color(0xFF8E8E95),
                    modifier = Modifier.size(44.dp),
                )
            }

            // Distinct ALBUM Badge in top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "ALBUM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.6.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        if (artist.isNotBlank() && artist != title) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF9E9EA5),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = null,
                tint = Color(0xFF8E8E95),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    color = Color(0xFF8E8E95),
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun DrillDownView(
    label: String,
    artworkUrl: String?,
    isArtist: Boolean,
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onBack: () -> Unit,
    savedDownloads: Map<String, String>,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val shape = if (isArtist) CircleShape else RoundedCornerShape(12.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = maxOf(contentPadding.calculateTopPadding(), 96.dp) + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(18.dp))
                // Header Artwork
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(shape)
                        .background(Color(0xFF222328))
                        .border(1.dp, Color(0xFF2E2F35), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = artworkUrl.artworkAt(ROW_ART_PX),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(shape),
                        )
                    } else {
                        Icon(
                            imageVector = if (isArtist) Icons.Rounded.Person else Icons.Rounded.Album,
                            contentDescription = null,
                            tint = Color(0xFF8E8E95),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9EA5),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(14.dp))

                // Play / Shuffle pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable {
                                if (songs.isNotEmpty()) {
                                    haptics.play(Haptic.Resume)
                                    onSongClick(songs, 0)
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF222328))
                            .border(1.dp, Color(0xFF32333A), RoundedCornerShape(50))
                            .clickable {
                                if (songs.isNotEmpty()) {
                                    haptics.play(Haptic.Resume)
                                    onShuffle(songs)
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }

        itemsIndexed(songs, key = { index, s -> "${s.videoId}_$index" }) { index, song ->
            val isDownloaded = song.videoId in savedDownloads
            CachedSongRow(
                song = song,
                isDownloaded = isDownloaded,
                onClick = { onSongClick(songs, index) },
                onLongPress = { onSongLongPress(song) },
                onDownloadClick = {
                    Downloads.enqueue(context, song, "Cached songs")
                    haptics.play(Haptic.ToggleOn)
                    Toast.makeText(context, "Saving \"${song.title}\" to Downloads…", Toast.LENGTH_SHORT).show()
                },
            )
            if (index < songs.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                    thickness = 0.5.dp,
                    color = Color(0xFF222328),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PAGE_GUTTER * 2)
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E95),
            textAlign = TextAlign.Center,
        )
    }
}
