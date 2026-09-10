package com.music.dhvani.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.rounded.Search
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
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.UiState
import com.music.dhvani.R
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.ROW_DIVIDER_INSET
import com.music.dhvani.ui.components.SongRow
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
    suggestions: List<String>,
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
    // A non-empty suggestion list means the field is mid-edit — see
    // MainViewModel.suggestions. Nothing below it is worth showing while it is
    // up: the results are for whatever was searched before this edit began,
    // and so are the filter tabs above them.
    val suggesting = suggestions.isNotEmpty()
    Column(modifier = modifier.fillMaxSize()) {
        // Search field and filter tabs stay fixed at the top, outside the
        // scrolling list, so they're always reachable rather than scrolling
        // away with the results or recent searches beneath them.
        Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                onVoiceClick = launchVoiceSearch,
                onFocusChanged = { isSearchFocused = it },
                focusRequester = focusRequester,
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 4.dp),
            )
            // The filters only mean something once there is a result set to narrow;
            // they stay up for an empty or failed search too, or picking a filter
            // that finds nothing would take away the control needed to leave it.
            if (results != null && !suggesting && (!isSearchFocused || query.isNotEmpty())) {
                SearchFilterTabs(filter = filter, onFilterChange = onFilterChange)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            when {
                suggesting -> searchSuggestions(
                    suggestions = suggestions,
                    // Picking one is done typing, so the keyboard comes down
                    // with it and the results get the whole screen.
                    onClick = { term ->
                        onSuggestionClick(term)
                        focusManager.clearFocus()
                    },
                    onFill = onQueryChange,
                )
                // When search bar is clicked/focused or when there are no results yet, show recent searches
                (isSearchFocused && query.isBlank()) || results == null -> if (history.isEmpty()) {
                    item { MessageState(stringResource(R.string.search_empty)) }
                } else {
                    recentSearches(
                        history = history,
                        onClick = { term ->
                            onHistoryClick(term)
                            focusManager.clearFocus()
                        },
                        onRemove = onHistoryRemove,
                        onClear = onHistoryClear,
                    )
                }
                results is UiState.Loading -> songListSkeleton(circular = filter == SearchFilter.ARTISTS)
                results is UiState.Error -> item { MessageState(results.message) }
                results is UiState.Success -> {
                    // Tapping a track plays the tracks around it, not the browse rows.
                    val tracks = results.data
                        .filterIsInstance<SearchResult.Track>()
                        .map { it.song }
                    itemsIndexed(results.data) { index, row ->
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
                        if (index < results.data.lastIndex) {
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
    suggestions: List<String>,
    onClick: (String) -> Unit,
    onFill: (String) -> Unit,
) {
    itemsIndexed(suggestions, key = { _, term -> "suggest:$term" }) { index, term ->
        SuggestionRow(
            term = term,
            // The lead row *is* what's in the field, so there is nothing to
            // fill it with and the arrow would be a no-op button.
            onFill = if (index == 0) null else ({ onFill(term) }),
            onClick = { onClick(term) },
        )
    }
}

/**
 * One typeahead row: tap the text to search it, or the arrow to put it in the
 * field and carry on typing — the pair YouTube, Google and every mobile
 * keyboard's own suggestion strip use, and the reason a longer completion
 * isn't a dead end when it's only nearly right.
 */
@Composable
private fun SuggestionRow(term: String, onFill: (() -> Unit)?, onClick: () -> Unit) {
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
            // Keeps the text column the same width as the rows below, so the
            // lead row doesn't sit a touch wider than its completions.
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
                .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.clear),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
    // Both ways of saying "search this" do the same two things, so they're
    // written once here rather than twice.
    val submit = {
        onSubmit()
        focusManager.clearFocus()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Fixed height prevents the row from growing when text is entered
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            // Asymmetric: the magnifier is a button now and wants a real touch
            // target, so it's given the room by pulling the field's own start
            // padding in rather than by pushing the glyph and the text along.
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The search button. It reads as one — a magnifier at the head of a
        // text field is the search affordance on every platform — and now that
        // pressing it is the only thing that runs a search, leaving it
        // decorative would mean the keyboard's own key was the single way in.
        Icon(
            Icons.Rounded.Search,
            contentDescription = stringResource(R.string.search),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(enabled = query.isNotBlank(), onClick = submit)
                .padding(6.dp),
        )
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        onFocusChanged(state.isFocused)
                    },
            )
        }
        // Emptying the field is also how the recent searches are got back to,
        // so it needs to be one tap rather than a held backspace.
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        onQueryChange("")
                        focusManager.clearFocus()
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
            Spacer(Modifier.width(4.dp))
        }
        // Voice Search Mic Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
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
