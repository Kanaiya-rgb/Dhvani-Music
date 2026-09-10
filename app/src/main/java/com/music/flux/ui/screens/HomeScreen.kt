package com.music.flux.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.flux.ui.haptics.Haptic
import com.music.flux.ui.haptics.rememberHaptics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.unit.sp
import com.music.flux.data.history.PlaybackHistory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.flux.ui.icons.FluxIcons
import com.music.flux.R
import coil3.compose.AsyncImage
import com.music.flux.data.model.CARD_ART_PX
import com.music.flux.data.model.HEADER_ART_PX
import com.music.flux.data.model.ROW_ART_PX
import com.music.flux.data.model.HomeShelf
import com.music.flux.data.model.ShelfItem
import com.music.flux.data.model.UiState
import com.music.flux.data.model.artworkAt
import com.music.flux.ui.components.HERO_CARD_RATIO
import java.util.Locale
import com.music.flux.ui.components.MessageState
import com.music.flux.ui.components.PAGE_GUTTER
import com.music.flux.ui.components.PullToRefresh
import com.music.flux.ui.components.SHELF_CARD_WIDTH
import com.music.flux.ui.components.SignInBanner
import com.music.flux.ui.components.feedMoreSkeleton
import com.music.flux.ui.components.feedSkeleton
import com.music.flux.ui.components.heroCardWidth
import com.music.flux.ui.components.thumbnailBorder
import com.music.flux.ui.player.MeshGradientBackground
import com.music.flux.ui.player.MeshPalette
import com.music.flux.data.YtMusicRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    title: String = "Listen Now",
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    /**
     * Holding a card rather than tapping it — the album/playlist menu. Only the
     * cards that point at a collection have one; a card that is a single track
     * is a track, and its own menu lives on the rows in the lists below.
     */
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    // Explore doesn't page — only Home has a continuation worth following.
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    selectedCategory: String = "All Rhythms",
    onCategorySelected: ((String) -> Unit)? = null,
) {
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "home_title") {
                Column {
                    // ── Dhvani Stitch Home: Greeting + Language Filter Rail ──────────────
                    DhvaniHomeHeader()
                    MoodGenreChips(
                        selectedChip = selectedCategory,
                        onChipSelect = { chip ->
                            onCategorySelected?.invoke(chip)
                        },
                    )
                }
            }

            when (state) {
                is UiState.Loading -> feedSkeleton()
                is UiState.Error -> item {
                    MessageState(state.message, actionLabel = "Retry", onAction = onRetry)
                }
                is UiState.Success -> {
                    itemsIndexedShelves(state.data, onItemClick, onItemLongPress)
                    if (loadingMore) feedMoreSkeleton()
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        // Fires again each time the tail end of the list comes back into
        // view — appending shelves doesn't reset it, only leaving the
        // bottom and scrolling back down does, which is exactly when
        // another page is worth asking for.
        val nearEnd by remember {
            derivedStateOf {
                val layout = listState.layoutInfo
                val last = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                layout.totalItemsCount > 0 && last >= layout.totalItemsCount - 4
            }
        }
        LaunchedEffect(nearEnd, listState.firstVisibleItemIndex) {
            if (nearEnd && !loadingMore) onLoadMore()
        }
    }
}

/**
 * Dhvani Stitch Home Header: Time-aware greeting (Namaste) + animated context badge.
 * Matches "Dhvani - Home" screen (projects/6466663551718142121/screens/9afd8dc206ae4127bdcf40f2a709775e)
 */
@Composable
private fun DhvaniHomeHeader() {
    val currentLocale = try {
        val firstLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
        firstLocale?.language ?: java.util.Locale.getDefault().language
    } catch (_: Throwable) {
        java.util.Locale.getDefault().language
    }
    val isHindi = currentLocale.startsWith("hi", ignoreCase = true)

    val currentHour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    val timeLabel = remember(currentHour, isHindi) {
        if (isHindi) {
            when (currentHour) {
                in 5..11  -> "सुबह की धुनें"
                in 12..16 -> "दोपहर की धुन"
                in 17..20 -> "शाम की महफ़िल"
                else      -> "रात का सुकून"
            }
        } else {
            when (currentHour) {
                in 5..11  -> "MORNING VIBES"
                in 12..16 -> "AFTERNOON ACOUSTIC"
                in 17..20 -> "EVENING MELODIES"
                else      -> "NIGHT CHILL"
            }
        }
    }

    val timeSubtitle = remember(currentHour, isHindi) {
        if (isHindi) {
            when (currentHour) {
                in 5..11  -> "आज क्या सुनेंगे? सुबह के राग या शांत लो-फ़ाई धुनें?"
                in 12..16 -> "दोपहर की ताज़गी — बॉलीवुड और इंडी संगीत"
                in 17..20 -> "शाम का सुकून — ग़ज़ल, सूफ़ी या मधुर धुनें"
                else      -> "रात की शांति — एकांत संगीत और सुकून"
            }
        } else {
            when (currentHour) {
                in 5..11  -> "Start your day with morning acoustic and calming tunes"
                in 12..16 -> "Energize your afternoon with fresh popular hits"
                in 17..20 -> "Unwind with cozy evening melodies and warmth"
                else      -> "Relax into the night with soothing lo-fi and ambient sounds"
            }
        }
    }

    val greetingTitle = remember(currentHour, isHindi) {
        if (isHindi) {
            when (currentHour) {
                in 5..11  -> "शुभ प्रभात"
                in 12..16 -> "शुभ दोपहर"
                in 17..21 -> "शुभ संध्या"
                else      -> "शुभ रात्रि"
            }
        } else {
            when (currentHour) {
                in 5..11  -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..21 -> "Good Evening"
                else      -> "Good Night"
            }
        }
    }

    val listenerLabel = if (isHindi) "संगीत प्रेमी" else "Music Lover"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER)
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        // Top Pill Badge: e.g. MORNING VIBES / सुबह की धुनें
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF2A1C14)) // Warm dark amber/saffron tint
                .border(0.5.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = timeLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(14.dp))

        // Greeting headline: Good Evening, Music Lover / शुभ संध्या, संगीत प्रेमी
        Text(
            text = "$greetingTitle, $listenerLabel",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))

        Text(
            text = timeSubtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Stitch Dhvani Mood & Genre Filter Chips:
 * Bilingual Indian language + genre tags matching the Stitch Home screen language rail.
 * Selected chip: Saffron fill. Unselected: surface-container with subtle border.
 */
@Composable
private fun MoodGenreChips(
    selectedChip: String,
    onChipSelect: (String) -> Unit,
) {
    val currentLocale = try {
        val firstLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
        firstLocale?.language ?: java.util.Locale.getDefault().language
    } catch (_: Throwable) {
        java.util.Locale.getDefault().language
    }
    val isHindi = currentLocale.startsWith("hi", ignoreCase = true)

    val chips = remember(isHindi) {
        if (isHindi) {
            listOf(
                "सभी",
                "नया संगीत",
                "बॉलीवुड",
                "इंडी हिंदी",
                "ग़ज़ल व सूफ़ी",
                "शास्त्रीय राग",
                "पंजाबी पॉप",
                "तमिल",
                "तेलुगु",
            )
        } else {
            listOf(
                "All",
                "New Releases",
                "Bollywood",
                "Indie Hindi",
                "Ghazal & Sufi",
                "Classical Ragas",
                "Punjabi Pop",
                "Tamil Hits",
                "Telugu Beats",
            )
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    ) {
        items(chips) { chip ->
            val isSelected = chip == selectedChip
            val haptics = rememberHaptics()
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.08f),
                        CircleShape,
                    )
                    .clickable {
                        haptics.play(Haptic.Select)
                        onChipSelect(chip)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (chip == "All Rhythms") {
                        Icon(
                            imageVector = FluxIcons.Infinity,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.W500,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * Detects whether a shelf represents a song-oriented feed (like Quick picks,
 * Top songs this week, Trending tracks) that YouTube Music displays as a
 * multi-row compact track list rather than full square album cards.
 */
internal fun HomeShelf.isSongShelf(): Boolean {
    if (items.isEmpty()) return false
    val songCount = items.count { it.videoId != null && it.browseId == null }
    val titleLower = title.lowercase(Locale.ROOT)
    val titleMatch = titleLower.contains("pick") ||
        titleLower.contains("song") ||
        titleLower.contains("track") ||
        titleLower.contains("chart") ||
        titleLower.contains("trending") ||
        titleLower.contains("top")
    return (songCount >= items.size * 0.75) || (songCount >= 3 && titleMatch)
}

/**
 * Shelves are rendered with dedicated layouts matching YouTube Music:
 * - Quick picks, Top songs, Trending -> 4-row compact song columns (SongShelf)
 * - Lead collection shelf -> Apple-style wide cards (HeroShelf)
 * - Other albums/playlists -> standard square cards (Shelf)
 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedShelves(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    shelves.forEachIndexed { index, shelf ->
        item(key = shelf.title + index) {
            when {
                shelf.isSongShelf() -> {
                    SongShelf(shelf = shelf, onItemClick = onItemClick, onItemLongPress = onItemLongPress)
                }
                index == 0 -> {
                    HeroShelf(shelf = shelf, onItemClick = onItemClick, onItemLongPress = onItemLongPress)
                }
                else -> {
                    Shelf(shelf = shelf, onItemClick = onItemClick, onItemLongPress = onItemLongPress)
                }
            }
        }
    }
}

/**
 * YouTube Music style Quick Picks / Top songs horizontal grid:
 * 4 rows of tracks per column, allowing users to quickly scan and play individual tracks.
 */
@Composable
private fun SongShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    Column(Modifier.padding(bottom = 24.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        val rowsPerColumn = 4
        val columns = remember(shelf.items) { shelf.items.chunked(rowsPerColumn) }
        BoxWithConstraints {
            val columnWidth = if (maxWidth > 600.dp) 340.dp else maxWidth * 0.88f
            LazyRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(columns) { columnItems ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        columnItems.forEach { item ->
                            QuickPickSongRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongPress = onItemLongPress?.let { { it(item) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Compact track item for YouTube Music style Quick Picks and Top Songs shelves. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickSongRow(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .thumbnailBorder(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onLongPress != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLongPress),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Shared by the home feed, Explore and Library so headings line up across tabs.
 * Dhvani Stitch style: headlineSmall title + saffron "See All" — matching editorial
 * shelf categorization from the "Midnight Raag" design system.
 *
 * [onShowAll] is only ever set on Library, whose rows stop at five cards
 * rather than running the shelf's whole length — see [LibraryGridShelf].
 */
@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER)
            .padding(top = 14.dp, bottom = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    // Dhvani: headline-sm (17sp / 600 weight) for shelf categorizations
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.contains("Made for You", ignoreCase = true) || title.contains("Daily Discover", ignoreCase = true) || title.contains("Quick Picks", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2A1C14))
                            .border(0.5.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language?.startsWith("hi") == true) "रोज़ाना नया" else "FOR YOU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = stringResource(R.string.show_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun HeroShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        // Measured rather than taken as a share of the parent, because the card
        // has a ceiling as well as a fraction — see [heroCardWidth]. A fixed
        // width is also the only one of the two the aspect ratio below can turn
        // into a height, so the card keeps its shape however it was arrived at.
        BoxWithConstraints {
            val cardWidth = heroCardWidth(maxWidth)
            LazyRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(shelf.items) { item ->
                    HeroCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        modifier = Modifier.width(cardWidth),
                    )
                }
            }
        }
    }
}

/**
 * Stitch Dhvani Featured Hero Card:
 * Atmospheric background image with dark gradient overlay,
 * Curated Raga & Mood badge, Dolby Atmos 24-bit Hi-Res badge,
 * Large title, artists, poetic mood caption, and primary "▶ Play Daily Mix" action button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = modifier
            .aspectRatio(0.95f) // Generous atmospheric card ratio matching Stitch
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF16181D))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(HEADER_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay: transparent to deep midnight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f),
                            Color(0xFF0F1115).copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Badges row: [CURATED RAGA & MOOD] + [Dolby Atmos • 24-bit Hi-Res]
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF2E2420).copy(alpha = 0.85f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "CURATED RAGA & MOOD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.6.sp,
                        ),
                        color = Color(0xFFFFB68D),
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0B251D).copy(alpha = 0.85f))
                        .border(0.5.dp, Color(0xFF00A878).copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF59DDA9),
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "Dolby Atmos • 24-bit Hi-Res",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                        ),
                        color = Color(0xFF59DDA9),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main Title (e.g. Shaam-e-Mehfil / Featured Mix)
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Artists line
            if (item.subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(5.dp))

            // Mood description
            Text(
                text = "Intimate semi-classical guitar arpeggios woven with soulful vocals, tuned to Raag Yaman for sunset...",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(14.dp))

            // Action Buttons Row: [▶ Play Daily Mix] + [+] + [Share]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Primary Saffron "Play Daily Mix" button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary,
                                ),
                            ),
                        )
                        .clickable {
                            haptics.play(Haptic.Resume)
                            onClick()
                        }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play Daily Mix",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // Add to Playlist button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            haptics.play(Haptic.Select)
                            onLongPress?.invoke()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }

                // Share button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            haptics.play(Haptic.Select)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

/**
 * [leadingCard] rides at the head of the row, ahead of the content — the
 * Library tab's "New playlist" tile, which belongs among the playlists rather
 * than in a bar somewhere above them. [onItemLongPress] opens the album /
 * playlist menu, and is null only where a card points at something with no
 * track list behind it to act on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(shelf.items) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                )
            }
        }
    }
}

/**
 * A card that isn't a thing yet — the dashed "New playlist" tile at the head
 * of the Library's playlist row, sized to sit in line with the covers beside
 * it rather than as a button bolted above them.
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** Set on a Library playlist card that's in [AppSettings.pinnedPlaylists][com.music.flux.data.settings.AppSettings.pinnedPlaylists]. */
    isPinned: Boolean = false,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        when (item.browseId) {
            YtMusicRepository.LIKED_MUSIC, "local:liked" -> {
                // Dhvani: Bollywood Crimson → Saffron (tertiary → primary)
                val palette = remember { MeshPalette(listOf(Color(0xFFFF848E), Color(0xFFFF8A3D), Color(0xFFFFB68D))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.thumbnailUrl != null) {
                        AsyncImage(
                            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .thumbnailBorder(RoundedCornerShape(12.dp)),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                    )
                                ),
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Icon(
                                imageVector = FluxIcons.HeartFilled,
                                contentDescription = null,
                                tint = Color(0xFFFFB68D), // Dhvani saffron-dim
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp),
                            )
                        }
                    } else {
                        MeshGradientBackground(
                            palette = palette,
                            trackKey = "liked_music_card",
                            continuous = false,
                            blurRadius = 24.dp,
                        )
                        Icon(
                            imageVector = FluxIcons.HeartFilled,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
            }
            "local:downloads" -> {
                // Dhvani: Midnight deep blue → surface-bright
                val palette = remember { MeshPalette(listOf(Color(0xFF111318), Color(0xFF37393E), Color(0xFF1E2024))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:downloads",
                        continuous = false,
                        blurRadius = 24.dp,
                    )
                    Icon(
                        imageVector = FluxIcons.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            "local:all" -> {
                // Dhvani: Peacock Emerald (secondary) gradient
                val palette = remember { MeshPalette(listOf(Color(0xFF003826), Color(0xFF00A878), Color(0xFF59DDA9))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:all",
                        continuous = false,
                        blurRadius = 24.dp,
                    )
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            else -> {
                AsyncImage(
                    model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .thumbnailBorder(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPinned) {
                Icon(
                    imageVector = FluxIcons.Pin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
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
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
