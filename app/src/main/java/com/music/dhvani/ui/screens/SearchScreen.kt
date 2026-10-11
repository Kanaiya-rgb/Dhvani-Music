package com.music.dhvani.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.BrowseItem
import com.music.dhvani.data.model.BrowseType
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.SearchFilter
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.model.SearchResult
import com.music.dhvani.data.model.SearchSuggestionItem
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.UiState
import com.music.dhvani.R
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.ROW_DIVIDER_INSET
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import com.music.dhvani.ui.components.SongRow
import com.music.dhvani.data.model.detectSongLanguage
import com.music.dhvani.ui.components.LanguageBadgePill
import com.music.dhvani.ui.components.thumbnailBorder
import com.music.dhvani.ui.components.songListSkeleton
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.util.Locale

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    results: UiState<List<SearchResult>>?,
    listState: LazyListState,
    focusTrigger: Int = 0,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onBrowseClick: (BrowseItem) -> Unit,
    /**
     * Holding an album or playlist hit rather than tapping it — the same menu
     * the shelves open, so a release found by searching can go on the queue
     * without a trip through its page.
     */
    onBrowseLongPress: ((BrowseItem) -> Unit)? = null,
    history: List<String>,
    suggestions: List<SearchSuggestionItem>,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var isSearchFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchFocused || query.isNotEmpty()) {
        if (query.isNotEmpty()) {
            onQueryChange("")
        }
        focusManager.clearFocus()
    }

    // Speech-to-text Voice Search Launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onQueryChange(spokenText)
                onSuggestionClick(spokenText)
                focusManager.clearFocus()
            }
        }
    }

    val launchVoiceSearch = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_search))
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Re-tapping the search tab from the nav bar increments focusTrigger;
    // respond by focusing the field and opening the keyboard.
    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) focusRequester.requestFocus()
    }

    // When search is focused or mid-query, Back clears input/focus and smoothly returns to the clean discovery landing page
    BackHandler(enabled = isSearchFocused || query.isNotEmpty()) {
        if (query.isNotEmpty()) {
            onQueryChange("")
        }
        focusManager.clearFocus()
    }
    // A non-empty suggestion list means the field is mid-edit — see
    // MainViewModel.suggestions. Nothing below it is worth showing while it is
    // up: the results are for whatever was searched before this edit began,
    // and so are the filter tabs above them.
    val suggesting = suggestions.isNotEmpty()
    var selectedLangFilter by rememberSaveable(query) { mutableStateOf<String?>(null) }
    val detectedLanguages = remember(results, filter) {
        if (results is UiState.Success && filter == SearchFilter.SONGS) {
            results.data
                .filterIsInstance<SearchResult.Track>()
                .mapNotNull { detectSongLanguage(it.song.title, it.song.artist, it.song.albumName) }
                .distinct()
        } else {
            emptyList()
        }
    }

    val displayData = remember(results, selectedLangFilter, filter) {
        val successData = (results as? UiState.Success)?.data ?: return@remember emptyList()
        if (selectedLangFilter == null || filter != SearchFilter.SONGS) {
            successData
        } else {
            val filtered = successData.filter { row ->
                when (row) {
                    is SearchResult.Track -> {
                        val l = detectSongLanguage(row.song.title, row.song.artist, row.song.albumName)
                        l.equals(selectedLangFilter, ignoreCase = true)
                    }
                    else -> true
                }
            }
            if (filtered.isNotEmpty()) filtered else successData
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search field and filter tabs stay fixed at the top, outside the
        // scrolling list, so they're always reachable rather than scrolling
        // away with the results or recent searches beneath them.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                onVoiceClick = launchVoiceSearch,
                onFocusChanged = { isSearchFocused = it },
                focusRequester = focusRequester,
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 6.dp),
            )
            // The filters only mean something once there is a result set to narrow;
            // they stay up for an empty or failed search too, or picking a filter
            // that finds nothing would take away the control needed to leave it.
            if (results != null && !suggesting && (!isSearchFocused || query.isNotEmpty())) {
                SearchFilterTabs(filter = filter, onFilterChange = onFilterChange)
                if (detectedLanguages.size >= 2) {
                    SearchLanguageTabs(
                        languages = detectedLanguages,
                        selected = selectedLangFilter,
                        onSelect = { selectedLangFilter = it },
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 6.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            when {
                suggesting -> searchSuggestions(
                    suggestions = suggestions,
                    currentQuery = query,
                    onTrackClick = { song ->
                        com.music.dhvani.data.settings.SearchHistory.record(song.title)
                        focusManager.clearFocus()
                        onSongClick(listOf(song), 0)
                        onQueryChange("")
                    },
                    onQueryClick = { term ->
                        onSuggestionClick(term)
                        focusManager.clearFocus()
                    },
                    onFill = onQueryChange,
                )
                query.isBlank() -> {
                    if (isSearchFocused) {
                        // User clicked the top search bar: show recent searches history
                        if (history.isNotEmpty()) {
                            recentSearches(
                                history = history,
                                onClick = { term ->
                                    onHistoryClick(term)
                                    focusManager.clearFocus()
                                },
                                onRemove = onHistoryRemove,
                                onClear = onHistoryClear,
                            )
                        } else {
                            item(key = "empty_history") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp, bottom = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(46.dp),
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Search for artists, songs, lyrics, or albums",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Clean landing page when not focused: discovery content without search history clutter
                        searchLandingContent(
                            onTagClick = { tag ->
                                onQueryChange(tag)
                                onSuggestionClick(tag)
                                focusManager.clearFocus()
                            },
                            onArtistClick = { artist ->
                                onQueryChange(artist)
                                onSuggestionClick(artist)
                                focusManager.clearFocus()
                            },
                        )
                    }
                }
                results is UiState.Loading -> songListSkeleton(circular = filter == SearchFilter.ARTISTS)
                results is UiState.Error -> item { MessageState(results.message) }
                results is UiState.Success -> {
                    // Tapping a track plays the tracks around it, not the browse rows.
                    val tracks = displayData
                        .filterIsInstance<SearchResult.Track>()
                        .map { it.song }
                    itemsIndexed(
                        items = displayData,
                        key = { _, row ->
                            when (row) {
                                is SearchResult.Track -> "track:${row.song.videoId}"
                                is SearchResult.Browse -> "browse:${row.item.browseId}"
                            }
                        },
                        contentType = { _, row ->
                            when (row) {
                                is SearchResult.Track -> "track"
                                is SearchResult.Browse -> "browse"
                            }
                        },
                    ) { index, row ->
                        when (row) {
                            is SearchResult.Track -> SongRow(
                                song = row.song,
                                onClick = {
                                    onSongClick(tracks, tracks.indexOf(row.song).coerceAtLeast(0))
                                },
                                onLongPress = { onSongLongPress(row.song) },
                                onSwipeToQueue = { onSongSwipe(row.song) },
                            )
                            is SearchResult.Browse -> BrowseRow(
                                item = row.item,
                                onClick = { onBrowseClick(row.item) },
                                onLongPress = onBrowseLongPress?.let { { it(row.item) } },
                            )
                        }
                        if (index < displayData.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * What YouTube would complete the half-typed query to, in place of the results
 * while it is being typed.
 *
 * The first row is the text as typed, put there by the view model rather than
 * taken from YouTube's answer, so running exactly what was asked for is always
 * the nearest row to the keyboard rather than something the thumb has to aim
 * past.
 */
private fun LazyListScope.searchSuggestions(
    suggestions: List<SearchSuggestionItem>,
    currentQuery: String,
    onTrackClick: (Song) -> Unit,
    onQueryClick: (String) -> Unit,
    onFill: (String) -> Unit,
) {
    val textItems = suggestions.filterIsInstance<SearchSuggestionItem.Text>()
    val trackItems = suggestions.filterIsInstance<SearchSuggestionItem.Track>()

    val topText = textItems.take(4)
    val remainingText = textItems.drop(4)

    // 1. Top 3-4 text suggestions on top
    items(
        items = topText,
        key = { "suggest:top:${it.query}" },
        contentType = { "text_suggestion" },
    ) { item ->
        val isFirst = topText.firstOrNull() == item
        QuerySuggestionRow(
            term = item.query,
            onFill = if (isFirst && item.query.equals(currentQuery, ignoreCase = true)) null else ({ onFill(item.query) }),
            onClick = { onQueryClick(item.query) },
        )
    }

    // 2. Song suggestions with artwork covers directly below the top text suggestions
    if (trackItems.isNotEmpty()) {
        items(
            items = trackItems,
            key = { "suggest:track:${it.song.videoId}" },
            contentType = { "track_suggestion" },
        ) { item ->
            TrackSuggestionRow(
                song = item.song,
                onClick = { onTrackClick(item.song) },
                onFill = { onFill(item.song.title) },
            )
        }
    }

    // 3. Remaining text suggestions at the bottom
    if (remainingText.isNotEmpty()) {
        items(
            items = remainingText,
            key = { "suggest:more:${it.query}" },
            contentType = { "text_suggestion" },
        ) { item ->
            QuerySuggestionRow(
                term = item.query,
                onFill = { onFill(item.query) },
                onClick = { onQueryClick(item.query) },
            )
        }
    }
}

@Composable
private fun TrackSuggestionRow(
    song: Song,
    onClick: () -> Unit,
    onFill: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .thumbnailBorder(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            val detectedLang = remember(song.title, song.artist, song.albumName) {
                detectSongLanguage(song.title, song.artist, song.albumName)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (detectedLang != null) {
                    LanguageBadgePill(detectedLang)
                }
            }
            Spacer(Modifier.height(2.dp))
            val subtitle = remember(song.artist, song.albumName, song.isVideo) {
                val type = if (song.isVideo) "Video" else "Song"
                val album = song.albumName?.trim()
                if (!album.isNullOrBlank() && !album.equals(song.title.trim(), ignoreCase = true)) {
                    "$type • ${song.artist} • $album"
                } else {
                    "$type • ${song.artist}"
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onFill != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NorthWest,
                    contentDescription = stringResource(R.string.recent_search_edit, song.title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun QuerySuggestionRow(
    term: String,
    onFill: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = term,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onFill != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NorthWest,
                    contentDescription = stringResource(R.string.recent_search_edit, term),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

/**
 * What was searched for before, shown in place of the results while the field
 * is empty — the same spot Spotify and Apple Music put it, and the reason the
 * blank search page isn't just a sentence any more.
 */
private fun LazyListScope.recentSearches(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "recent:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.clear),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    items(history, key = { "recent:$it" }) { term ->
        RecentSearchRow(
            term = term,
            onClick = { onClick(term) },
            onRemove = { onRemove(term) },
        )
    }
}

@Composable
private fun RecentSearchRow(term: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = term,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.recent_search_remove, term),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class QuickSearchTag(
    val emoji: String,
    val title: String,
    val gradient: List<Color>,
)

private val QuickTags = listOf(
    QuickSearchTag("🔥", "Bollywood Hits", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
    QuickSearchTag("💖", "Arijit Singh", listOf(Color(0xFFE91E63), Color(0xFFFF5252))),
    QuickSearchTag("🎧", "Desi Hip Hop", listOf(Color(0xFF1F1C18), Color(0xFF8E0E00))),
    QuickSearchTag("☕", "Chill Lo-Fi", listOf(Color(0xFF007991), Color(0xFF78FFD6))),
    QuickSearchTag("🕉️", "Bhakti & Aarti", listOf(Color(0xFFFF6A00), Color(0xFFEE0979))),
    QuickSearchTag("⚡", "Punjabi Bops", listOf(Color(0xFFF7971E), Color(0xFFFFD200))),
    QuickSearchTag("🌿", "Indian Indie", listOf(Color(0xFF134E5E), Color(0xFF71B280))),
    QuickSearchTag("🏋️", "Workout Pump", listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))),
    QuickSearchTag("🌙", "Late Night", listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))),
    QuickSearchTag("✨", "90s Hits", listOf(Color(0xFF8A2387), Color(0xFFE94057))),
)

private data class QuickArtist(
    val name: String,
    val initial: String,
    val imageUrl: String,
    val gradient: List<Color>,
)

private val PopularArtists = listOf(
    QuickArtist(
        name = "Arijit Singh",
        initial = "AS",
        imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg",
        gradient = listOf(Color(0xFFE52D27), Color(0xFFB31217)),
    ),
    QuickArtist(
        name = "Sidhu Moose Wala",
        initial = "SM",
        imageUrl = "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_004_20250617183705_500x500.jpg",
        gradient = listOf(Color(0xFFFF8008), Color(0xFFFFC837)),
    ),
    QuickArtist(
        name = "Shreya Ghoshal",
        initial = "SG",
        imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
        gradient = listOf(Color(0xFFE91E63), Color(0xFFFF4081)),
    ),
    QuickArtist(
        name = "Karan Aujla",
        initial = "KA",
        imageUrl = "https://c.saavncdn.com/artists/Karan_Aujla_005_20260925061936_500x500.jpg",
        gradient = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
    ),
    QuickArtist(
        name = "Diljit Dosanjh",
        initial = "DD",
        imageUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg",
        gradient = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    ),
    QuickArtist(
        name = "Anuv Jain",
        initial = "AJ",
        imageUrl = "https://c.saavncdn.com/artists/Anuv_Jain_001_20231206073013_500x500.jpg",
        gradient = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
    ),
    QuickArtist(
        name = "Atif Aslam",
        initial = "AA",
        imageUrl = "https://yt3.googleusercontent.com/ykJkyILKum4B2oudDxjnf5WNenWWZAp-WEz0_CHp4cu0VnqB2-uaNDylItqC68WLXV62rdHDun-ahbg=w500-h500-p-l90-rj",
        gradient = listOf(Color(0xFF0052D4), Color(0xFF4364F7)),
    ),
    QuickArtist(
        name = "AP Dhillon",
        initial = "AP",
        imageUrl = "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg",
        gradient = listOf(Color(0xFF8A2387), Color(0xFFE94057)),
    ),
    QuickArtist(
        name = "Badshah",
        initial = "B",
        imageUrl = "https://c.saavncdn.com/artists/Badshah_006_20241118064015_500x500.jpg",
        gradient = listOf(Color(0xFFEB3349), Color(0xFFF45C43)),
    ),
    QuickArtist(
        name = "Kishore Kumar",
        initial = "KK",
        imageUrl = "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg",
        gradient = listOf(Color(0xFF614385), Color(0xFF516395)),
    ),
)

private fun LazyListScope.searchLandingContent(
    onTagClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    // Top Artists (with real artist photo thumbnails)
    item(key = "landing:artists:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Top Artists",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
    item(key = "landing:artists:row") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PopularArtists.forEach { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onArtistClick(artist.name) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(artist.gradient))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = artist.initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        AsyncImage(
                            model = artist.imageUrl,
                            contentDescription = artist.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 3. Trending & Moods (Vertical 2-column cards layout)
    item(key = "landing:trending:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 22.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Trending & Moods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
    val chunkedTags = QuickTags.chunked(2)
    chunkedTags.forEachIndexed { rowIndex, rowTags ->
        item(key = "landing:trending:row:$rowIndex") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        tag.gradient[0].copy(alpha = 0.18f),
                                        tag.gradient[1].copy(alpha = 0.08f),
                                    ),
                                ),
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        tag.gradient[0].copy(alpha = 0.40f),
                                        tag.gradient[1].copy(alpha = 0.20f),
                                    ),
                                ),
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onTagClick(tag.title) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = tag.emoji, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tag.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowTags.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    item(key = "landing:bottom:space") {
        Spacer(Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowseRow(item: BrowseItem, onClick: () -> Unit, onLongPress: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(
                    if (item.type == BrowseType.ARTIST) CircleShape
                    else RoundedCornerShape(8.dp),
                )
                .thumbnailBorder(
                    if (item.type == BrowseType.ARTIST) CircleShape
                    else RoundedCornerShape(8.dp),
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle.ifBlank { item.type.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) } },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Filter pills rather than a tab row: squarish rounded rectangles, the selected
 * one inverted. They scroll horizontally so a long label set never squeezes the
 * text, and the gutter padding sits inside the scroll so it scrolls with them.
 */
@Composable
private fun SearchFilterTabs(filter: SearchFilter, onFilterChange: (SearchFilter) -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchFilter.entries.forEach { entry ->
            val selected = entry == filter
            Box(
                modifier = Modifier
                    .clip(FILTER_PILL_SHAPE)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .border(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.08f),
                        FILTER_PILL_SHAPE,
                    )
                    // Only the pill that isn't already selected has anything to
                    // report — re-tapping the current filter changes nothing, so
                    // buzzing for it would be feedback for a no-op.
                    .clickable {
                        if (!selected) haptics.play(Haptic.Select)
                        onFilterChange(entry)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SearchLanguageTabs(
    languages: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PAGE_GUTTER, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isAllSelected = selected == null
        Box(
            modifier = Modifier
                .clip(FILTER_PILL_SHAPE)
                .background(
                    if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                )
                .border(
                    1.dp,
                    if (isAllSelected) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.08f),
                    FILTER_PILL_SHAPE,
                )
                .clickable {
                    if (!isAllSelected) {
                        haptics.play(Haptic.Select)
                        onSelect(null)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = "All",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isAllSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        languages.forEach { lang ->
            val isSelected = selected.equals(lang, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(FILTER_PILL_SHAPE)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.08f),
                        FILTER_PILL_SHAPE,
                    )
                    .clickable {
                        haptics.play(Haptic.Select)
                        onSelect(if (isSelected) null else lang)
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = lang,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Stitch Dhvani: Pill shaped genre/search filter tokens */
private val FILTER_PILL_SHAPE = CircleShape

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val submit = {
        onSubmit()
        focusManager.clearFocus()
    }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        label = "searchBorder",
    )
    val cardShape = CircleShape

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, cardShape)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                focusRequester.requestFocus()
            }
            .padding(start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isFocused) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { focusManager.clearFocus() }
                    .padding(6.dp),
            )
        } else {
            Icon(
                Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search),
                tint = if (query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(enabled = query.isNotBlank(), onClick = submit)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        onFocusChanged(state.isFocused)
                    },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        onQueryChange("")
                        focusRequester.requestFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        // Voice Search Mic Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onVoiceClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Mic,
                contentDescription = stringResource(R.string.voice_search),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
