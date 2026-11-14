package com.music.dhvani.ui.screens

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
import com.music.dhvani.ui.theme.uiDesignCard
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
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
import androidx.compose.ui.unit.sp
import com.music.dhvani.data.history.PlaybackHistory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.ui.icons.DhvaniIcons
import com.music.dhvani.R
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.CARD_ART_PX
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.HomeShelf
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.UiState
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.ui.components.HERO_CARD_RATIO
import java.util.Locale
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.ExplicitBadge
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.PullToRefresh
import com.music.dhvani.ui.components.SHELF_CARD_WIDTH
import com.music.dhvani.ui.components.SignInBanner
import com.music.dhvani.ui.components.feedMoreSkeleton
import com.music.dhvani.ui.components.feedSkeleton
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.music.dhvani.data.settings.AppSettings
import androidx.compose.animation.core.animateFloatAsState
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Immutable
import com.music.dhvani.ui.components.heroCardWidth
import com.music.dhvani.ui.components.thumbnailBorder
import com.music.dhvani.ui.player.MeshGradientBackground
import com.music.dhvani.ui.player.MeshPalette
import com.music.dhvani.data.YtMusicRepository

private val RowCardShape = RoundedCornerShape(10.dp)
private val RowThumbnailShape = RoundedCornerShape(8.dp)
private val ShelfCardShape = RoundedCornerShape(16.dp)
private val LibraryCardShape = RoundedCornerShape(12.dp)
private val HeroCardShape = RoundedCornerShape(22.dp)

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
    replayCard: com.music.dhvani.ui.replay.ReplayHeroCard? = null,
    onOpenReplay: (() -> Unit)? = null,
    onPlayRandom: (() -> Unit)? = null,
) {
    val visibleShelves = remember(state) {
        (state as? UiState.Success)?.data?.filterNot { YtMusicRepository.isExcludedHomeShelf(it) }.orEmpty()
    }
    val homeArtworkUrls = remember(visibleShelves, replayCard) {
        val urls = mutableListOf<String>()
        replayCard?.artworkUrl?.takeIf { it.isNotBlank() }?.let { urls.add(it) }
        for (shelf in visibleShelves) {
            for (item in shelf.items) {
                val url = item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: item.mosaicUrls.firstOrNull()
                if (url != null && !urls.contains(url)) {
                    urls.add(url)
                    if (urls.size >= 2) break
                }
            }
            if (urls.size >= 2) break
        }
        urls
    }
    val primaryArtworkUrl = homeArtworkUrls.firstOrNull()
    val secondaryArtworkUrl = homeArtworkUrls.getOrNull(1)

    Box(modifier = modifier.fillMaxSize()) {
        HomeAmbientBackdrop(
            primaryArtworkUrl = primaryArtworkUrl,
            secondaryArtworkUrl = secondaryArtworkUrl,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(),
        )

        PullToRefresh(
            refreshing = refreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                item(key = "home_title", contentType = "home_title") {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                        )
                        MoodGenreChips(
                            selectedChip = selectedCategory,
                            onChipSelect = { chip ->
                                onCategorySelected?.invoke(chip)
                            },
                        )
                    }
                }

// Replay removed from Home feed as requested

                when (state) {
                    is UiState.Loading -> feedSkeleton()
                    is UiState.Error -> item {
                        MessageState(state.message, actionLabel = "Retry", onAction = onRetry)
                    }
                    is UiState.Success -> {
                        itemsIndexedShelves(visibleShelves, onItemClick, onItemLongPress)
                        if (loadingMore) feedMoreSkeleton()
                    }
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        // Only load more when user has scrolled all the way to the last shelf (prevents network thrashing on launch)
        LaunchedEffect(listState) {
            snapshotFlow {
                val layout = listState.layoutInfo
                val last = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
                layout.totalItemsCount >= 5 && last >= layout.totalItemsCount - 1
            }.distinctUntilChanged()
                .collect { nearEnd ->
                    if (nearEnd && !loadingMore) onLoadMore()
                }
        }
    }
}

@Immutable
private data class HomeAmbientPalette(
    val dominant: Color,
    val vibrant: Color,
    val secondary: Color,
)

/**
 * Extracts real dominant & vibrant ambient colors directly from the visible Home feed album artwork.
 * Decodes off-thread on Dispatchers.IO via Coil and AndroidX Palette.
 *
 * Remains completely stable across scroll events because URLs are anchored to the top feed items.
 * Returns null during loading or error states so the existing dark theme background remains fallback.
 */
@Composable
private fun rememberHomeAmbientColors(
    primaryUrl: String?,
    secondaryUrl: String? = null,
): HomeAmbientPalette? {
    val context = LocalContext.current
    var ambientPalette by remember(primaryUrl, secondaryUrl) {
        mutableStateOf<HomeAmbientPalette?>(null)
    }

    LaunchedEffect(primaryUrl, secondaryUrl) {
        if (primaryUrl.isNullOrBlank()) {
            ambientPalette = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = SingletonImageLoader.get(context)
                val request = ImageRequest.Builder(context)
                    .data(primaryUrl.artworkAt(ROW_ART_PX))
                    .size(96)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.image?.toBitmap()
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                    val swatches = palette.swatches.sortedByDescending { it.population }

                    val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
                        ?: swatches.firstOrNull()?.rgb?.let { Color(it) }

                    val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
                        ?: palette.lightVibrantSwatch?.rgb?.let { Color(it) }
                        ?: palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                        ?: swatches.getOrNull(1)?.rgb?.let { Color(it) }
                        ?: dominant

                    var secondary = palette.mutedSwatch?.rgb?.let { Color(it) }
                        ?: palette.darkMutedSwatch?.rgb?.let { Color(it) }
                        ?: swatches.getOrNull(2)?.rgb?.let { Color(it) }
                        ?: vibrant

                    if (secondaryUrl != null) {
                        try {
                            val secRequest = ImageRequest.Builder(context)
                                .data(secondaryUrl.artworkAt(ROW_ART_PX))
                                .size(96)
                                .allowHardware(false)
                                .build()
                            val secResult = imageLoader.execute(secRequest)
                            val secBitmap = (secResult as? SuccessResult)?.image?.toBitmap()
                            if (secBitmap != null) {
                                val secPalette = Palette.from(secBitmap).maximumColorCount(16).generate()
                                val secDom = secPalette.dominantSwatch?.rgb?.let { Color(it) }
                                    ?: secPalette.vibrantSwatch?.rgb?.let { Color(it) }
                                if (secDom != null) {
                                    secondary = secDom
                                }
                            }
                        } catch (_: Throwable) {}
                    }

                    if (dominant != null) {
                        ambientPalette = HomeAmbientPalette(
                            dominant = dominant,
                            vibrant = vibrant ?: dominant,
                            secondary = secondary ?: vibrant ?: dominant,
                        )
                    }
                }
            } catch (_: Throwable) {
                // Keep null on error to smoothly fallback to dark theme background
            }
        }
    }
    return ambientPalette
}

/**
 * Subtle, blurred ambient gradient background for the Home screen.
 * Extracts dominant and vibrant colors directly from the visible Home feed artwork.
 *
 * Features:
 * - Smooth one-time transition when artwork loads (zero per-scroll-frame or per-item jumps).
 * - Clean fallback to current dark theme background during loading, error, or missing artwork.
 * - Multi-point radial Gaussian-like falloff creating an authentic blurred ambient bloom.
 * - Protective contrast scrim guaranteeing 100% legibility of foreground cards and text.
 */
@Composable
private fun HomeAmbientBackdrop(
    primaryArtworkUrl: String?,
    secondaryArtworkUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val themeBg = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()

    val colors = rememberHomeAmbientColors(primaryArtworkUrl, secondaryArtworkUrl)

    // Smooth one-time fade transition when artwork colors are extracted
    val transitionProgress by animateFloatAsState(
        targetValue = if (colors != null) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "homeAmbientFade",
    )

    val animatedDominant by animateColorAsState(
        targetValue = colors?.dominant ?: themeBg,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "homeAmbientDominant",
    )
    val animatedVibrant by animateColorAsState(
        targetValue = colors?.vibrant ?: themeBg,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "homeAmbientVibrant",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colors?.secondary ?: themeBg,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "homeAmbientSecondary",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBg),
    ) {
        if (transitionProgress > 0.005f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // 1. Dominant artwork bloom (top-left) - soft Gaussian-like multi-stop falloff
                val center1 = Offset(width * 0.12f, height * 0.10f)
                val radius1 = width * 1.10f
                val alpha1 = (if (isDark) 0.38f else 0.22f) * transitionProgress
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedDominant.copy(alpha = alpha1),
                        0.40f to animatedDominant.copy(alpha = alpha1 * 0.55f),
                        0.72f to animatedDominant.copy(alpha = alpha1 * 0.16f),
                        1.0f to Color.Transparent,
                        center = center1,
                        radius = radius1,
                    ),
                    radius = radius1,
                    center = center1,
                )

                // 2. Vibrant artwork bloom (top-right / mid-right) - dual-color ambient atmosphere
                val center2 = Offset(width * 0.90f, height * 0.24f)
                val radius2 = width * 0.95f
                val alpha2 = (if (isDark) 0.32f else 0.18f) * transitionProgress
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedVibrant.copy(alpha = alpha2),
                        0.38f to animatedVibrant.copy(alpha = alpha2 * 0.50f),
                        0.72f to animatedVibrant.copy(alpha = alpha2 * 0.14f),
                        1.0f to Color.Transparent,
                        center = center2,
                        radius = radius2,
                    ),
                    radius = radius2,
                    center = center2,
                )

                // 3. Secondary artwork bloom (center / lower-mid) - prevents harsh gradients, softens blend
                val center3 = Offset(width * 0.45f, height * 0.38f)
                val radius3 = width * 0.85f
                val alpha3 = (if (isDark) 0.24f else 0.12f) * transitionProgress
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to animatedSecondary.copy(alpha = alpha3),
                        0.45f to animatedSecondary.copy(alpha = alpha3 * 0.40f),
                        1.0f to Color.Transparent,
                        center = center3,
                        radius = radius3,
                    ),
                    radius = radius3,
                    center = center3,
                )

                // 4. Subtle top wash matching the dominant hue
                val washAlpha = (if (isDark) 0.24f else 0.12f) * transitionProgress
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to animatedDominant.copy(alpha = washAlpha),
                        0.32f to animatedVibrant.copy(alpha = washAlpha * 0.45f),
                        0.60f to Color.Transparent,
                    ),
                )

                // 5. Protective contrast scrim: keeps cards, titles, and text 100% legible
                // and softly settles into the dark theme background
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to themeBg.copy(alpha = if (isDark) 0.15f else 0.05f),
                        0.25f to themeBg.copy(alpha = if (isDark) 0.35f else 0.15f),
                        0.55f to themeBg.copy(alpha = if (isDark) 0.68f else 0.42f),
                        0.80f to themeBg,
                        1.0f to themeBg,
                    ),
                )
            }
        }
    }
}

/**
 * Dhvani Stitch Home Header: Time-aware greeting (Namaste) + animated context badge.
 * Matches "Dhvani - Home" screen (projects/6466663551718142121/screens/9afd8dc206ae4127bdcf40f2a709775e)
 */
@Composable
private fun DhvaniHomeHeader(
    onPlayRandom: (() -> Unit)? = null,
) {
    val isHindi = remember {
        try {
            val firstLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
            (firstLocale?.language ?: java.util.Locale.getDefault().language).startsWith("hi", ignoreCase = true)
        } catch (_: Throwable) {
            java.util.Locale.getDefault().language.startsWith("hi", ignoreCase = true)
        }
    }

    val currentHour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    val listenerName by com.music.dhvani.data.settings.AppSettings.userName.collectAsStateWithLifecycle()
    val displayName = listenerName.trim().ifBlank { if (isHindi) "संगीत प्रेमी" else "Music Lover" }

    val timeEmoji = when (currentHour) {
        in 5..11  -> "☀️"
        in 12..16 -> "🌤️"
        in 17..21 -> "✨"
        else      -> "🌙"
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER)
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        // Personalized greeting headline: Good Morning, Rahul ☀️ / शुभ प्रभात, Rahul ☀️
        Text(
            text = "$greetingTitle, $displayName $timeEmoji",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
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
    val isHindi = remember {
        try {
            val firstLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
            (firstLocale?.language ?: java.util.Locale.getDefault().language).startsWith("hi", ignoreCase = true)
        } catch (_: Throwable) {
            java.util.Locale.getDefault().language.startsWith("hi", ignoreCase = true)
        }
    }

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
    val haptics = rememberHaptics()
    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    ) {
        items(
            items = chips,
            key = { it },
            contentType = { "chip" },
        ) { chip ->
            val isSelected = chip == selectedChip
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
                            imageVector = DhvaniIcons.Infinity,
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
internal fun HomeShelf.isSongShelf(): Boolean = this.isSongShelf

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
    items(
        count = shelves.size,
        key = { index ->
            val shelf = shelves[index]
            val firstId = shelf.items.firstOrNull()?.videoId
                ?: shelf.items.firstOrNull()?.browseId
                ?: index
            "shelf_${shelf.title}_$firstId"
        },
        contentType = { index ->
            when {
                shelves[index].isSongShelf -> "song_shelf"
                index == 0 -> "hero_shelf"
                else -> "shelf"
            }
        },
    ) { index ->
        val shelf = shelves[index]
        when {
            shelf.isSongShelf -> {
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
        SectionHeader(shelf.title)
        val rowsPerColumn = 4
        val columns = remember(shelf.items) { shelf.items.chunked(rowsPerColumn) }
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val columnWidth = remember(screenWidth) { if (screenWidth > 600.dp) 340.dp else screenWidth * 0.88f }
        val shelfState = rememberSaveable(shelf.title, saver = LazyListState.Saver) { LazyListState() }
        LazyRow(
            state = shelfState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = columns,
                key = { col -> col.firstOrNull()?.videoId ?: col.firstOrNull()?.title ?: col.hashCode() },
                contentType = { "song_column" },
            ) { columnItems ->
                Column(
                    modifier = Modifier.width(columnWidth),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    columnItems.forEach { item ->
                        QuickPickSongRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongPress = onItemLongPress?.let { lp -> { lp(item) } },
                        )
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
            .clip(RowCardShape)
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
                .clip(RowThumbnailShape)
                .thumbnailBorder(RowThumbnailShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (item.isExplicit) {
                    ExplicitBadge()
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
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
internal fun SectionHeader(
    title: String,
    subtitle: String = "",
    onShowAll: (() -> Unit)? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val isHindi = remember {
        try {
            androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language?.startsWith("hi") == true
        } catch (_: Throwable) {
            false
        }
    }
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
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                            text = if (isHindi) "रोज़ाना नया" else "FOR YOU",
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
        SectionHeader(shelf.title)
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val cardWidth = remember(screenWidth) { heroCardWidth(screenWidth) }
        val shelfState = rememberSaveable(shelf.title, saver = LazyListState.Saver) { LazyListState() }
        LazyRow(
            state = shelfState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = shelf.items,
                key = { it.videoId ?: (it.browseId ?: "${it.title}_${it.subtitle}") },
                contentType = { "hero_card" },
            ) { item ->
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
            .uiDesignCard(shape = HeroCardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        val gradientBrush = remember {
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.15f),
                    Color.Black.copy(alpha = 0.55f),
                    Color(0xFF0F1115).copy(alpha = 0.95f),
                ),
            )
        }

        val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
        val primaryColor = MaterialTheme.colorScheme.primary
        val buttonGradient = remember(primaryContainerColor, primaryColor) {
            Brush.horizontalGradient(
                listOf(primaryContainerColor, primaryColor),
            )
        }

        // Gradient overlay: transparent to deep midnight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
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
                        .background(buttonGradient)
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
        SectionHeader(shelf.title)
        val shelfState = rememberSaveable(shelf.title, saver = LazyListState.Saver) { LazyListState() }
        LazyRow(
            state = shelfState,
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading", contentType = "leading_card") { card() } }
            items(
                items = shelf.items,
                key = { it.videoId ?: (it.browseId ?: "${it.title}_${it.subtitle}") },
                contentType = { "shelf_card" },
            ) { item ->
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** Set on a Library playlist card that's in [AppSettings.pinnedPlaylists][com.music.dhvani.data.settings.AppSettings.pinnedPlaylists]. */
    isPinned: Boolean = false,
    titleMaxLines: Int = 1,
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
                        .clip(LibraryCardShape),
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
                                .clip(LibraryCardShape)
                                .thumbnailBorder(LibraryCardShape),
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
                                imageVector = DhvaniIcons.HeartFilled,
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
                            animated = false,
                        )
                        Icon(
                            imageVector = DhvaniIcons.HeartFilled,
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
                        .clip(LibraryCardShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:downloads",
                        continuous = false,
                        blurRadius = 24.dp,
                        animated = false,
                    )
                    Icon(
                        imageVector = DhvaniIcons.Download,
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
                        .clip(LibraryCardShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:all",
                        continuous = false,
                        blurRadius = 24.dp,
                        animated = false,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(ShelfCardShape)
                        .thumbnailBorder(ShelfCardShape),
                ) {
                    if (item.mosaicUrls.size >= 4) {
                        // 2×2 mosaic collage (Spotify-style playlist cover)
                        val mosaic = item.mosaicUrls
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    AsyncImage(
                                        model = mosaic[0].artworkAt(CARD_ART_PX / 2),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Spacer(Modifier.width(1.5.dp))
                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    AsyncImage(
                                        model = mosaic[1].artworkAt(CARD_ART_PX / 2),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(1.5.dp))
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    AsyncImage(
                                        model = mosaic[2].artworkAt(CARD_ART_PX / 2),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Spacer(Modifier.width(1.5.dp))
                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    AsyncImage(
                                        model = mosaic[3].artworkAt(CARD_ART_PX / 2),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    } else if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(ShelfCardShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isPinned) {
                Icon(
                    imageVector = DhvaniIcons.Pin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (item.isExplicit) {
                ExplicitBadge()
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeReplayBanner(
    card: com.music.dhvani.ui.replay.ReplayHeroCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = com.music.dhvani.ui.player.rememberArtworkColors(card.artworkUrl)
    val accentGold = Color(0xFFFFD54F)
    val accentCoral = Color(0xFFFF7043)
    val accentViolet = Color(0xFFAB47BC)

    Box(
        modifier = modifier
            .padding(horizontal = com.music.dhvani.ui.components.PAGE_GUTTER, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentGold.copy(alpha = 0.7f),
                        accentCoral.copy(alpha = 0.5f),
                        accentViolet.copy(alpha = 0.6f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onClick),
    ) {
        // Vibrant mesh background (settles to save battery & GPU frames)
        Box(Modifier.matchParentSize()) {
            com.music.dhvani.ui.player.MeshGradientBackground(
                palette = palette,
                trackKey = card.artworkUrl ?: "home_replay",
                continuous = false,
                blurRadius = 24.dp,
                animated = false,
            )
        }
        // Sophisticated frosted glass overlay
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Top Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "✨ REPLAY • WRAPPED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = accentGold,
                    )
                }

                // Action Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(accentGold.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "View Story",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Icon(
                            imageVector = com.music.dhvani.ui.icons.DhvaniIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Your Listening Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${card.value} ${card.label.lowercase(java.util.Locale.ROOT)} · ${card.detail}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


