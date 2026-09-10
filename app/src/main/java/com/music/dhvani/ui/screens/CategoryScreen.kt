package com.music.dhvani.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.dhvani.data.YtMusicRepository
import com.music.dhvani.data.model.BrowseType
import com.music.dhvani.data.model.HomeShelf
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.Song
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import com.music.dhvani.ui.icons.DhvaniIcons
import kotlinx.coroutines.launch
import java.util.Locale

enum class CategoryGroup {
    ALL, MOOD, WORKOUT, GENRE, VIBE
}

data class MusicCategory(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val emoji: String,
    val description: String,
    val gradient: List<Color>,
    val group: CategoryGroup,
)

val AllMusicCategories = listOf(
    // ── Moods ──
    MusicCategory(
        id = "sad",
        title = "Sad & Heartbreak",
        hindiTitle = "दर्द भरे गीत",
        emoji = "💔",
        description = "Melancholy, emotional tears & healing",
        gradient = listOf(Color(0xFF1E3C72), Color(0xFF2A5298)),
        group = CategoryGroup.MOOD,
    ),
    MusicCategory(
        id = "happy",
        title = "Happy & Joy",
        hindiTitle = "खुशी और मुस्कान",
        emoji = "✨",
        description = "Feel good, upbeat positivity & smiles",
        gradient = listOf(Color(0xFFFF8008), Color(0xFFFFC837)),
        group = CategoryGroup.MOOD,
    ),
    MusicCategory(
        id = "romantic",
        title = "Romance & Love",
        hindiTitle = "रोमांटिक धुनें",
        emoji = "💖",
        description = "Endless romance, duets & love ballads",
        gradient = listOf(Color(0xFFE91E63), Color(0xFFFF5252)),
        group = CategoryGroup.MOOD,
    ),
    MusicCategory(
        id = "chill",
        title = "Chill & Relax",
        hindiTitle = "सुकून और शांति",
        emoji = "☕",
        description = "Cozy acoustic, sunset calm & peaceful vibes",
        gradient = listOf(Color(0xFF007991), Color(0xFF78FFD6)),
        group = CategoryGroup.MOOD,
    ),

    // ── Energy & Workout ──
    MusicCategory(
        id = "gym",
        title = "Gym & Workout",
        hindiTitle = "जिम और वर्कआउट",
        emoji = "🏋️",
        description = "Beast mode, heavy lifting & high cardio",
        gradient = listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)),
        group = CategoryGroup.WORKOUT,
    ),
    MusicCategory(
        id = "power",
        title = "Power & Motivation",
        hindiTitle = "जोश और ताक़त",
        emoji = "⚡",
        description = "Adrenaline rush, warrior spirit & hype",
        gradient = listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
        group = CategoryGroup.WORKOUT,
    ),
    MusicCategory(
        id = "party",
        title = "Party & Dance",
        hindiTitle = "पार्टी और डांस",
        emoji = "🎉",
        description = "Club bangers, DJ remixes & high bass",
        gradient = listOf(Color(0xFF8A2387), Color(0xFFE94057)),
        group = CategoryGroup.WORKOUT,
    ),

    // ── Genres ──
    MusicCategory(
        id = "rock",
        title = "Rock & Metal",
        hindiTitle = "रॉक और गिटार",
        emoji = "🎸",
        description = "Heavy riffs, desi rock & electric thunder",
        gradient = listOf(Color(0xFF232526), Color(0xFF414345)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "pop",
        title = "Pop & Bops",
        hindiTitle = "पॉप संगीत",
        emoji = "🎤",
        description = "Global chartbusters, desi pop & dance hits",
        gradient = listOf(Color(0xFFDA22FF), Color(0xFF9733EE)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "hiphop",
        title = "Hip-Hop & Rap",
        hindiTitle = "हिप-हॉप और रैप",
        emoji = "🔥",
        description = "Gully rap, heavy bars & street flow",
        gradient = listOf(Color(0xFF0F2027), Color(0xFF203A43)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "punjabi",
        title = "Punjabi Hits",
        hindiTitle = "पंजाबी बीट्स",
        emoji = "🪘",
        description = "Dhol, bhangra, swagger & chartbusters",
        gradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "bollywood",
        title = "Bollywood Magic",
        hindiTitle = "बॉलीवुड सदाबहार",
        emoji = "🎬",
        description = "Cinematic magic, 90s gold & modern blockbusters",
        gradient = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "indie",
        title = "Indie Acoustic",
        hindiTitle = "इंडि धुनें",
        emoji = "🌿",
        description = "Soulful poetry, acoustic guitar & indie scene",
        gradient = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "sufi",
        title = "Sufi & Ghazal",
        hindiTitle = "सूफ़ी और ग़ज़ल",
        emoji = "🕯️",
        description = "Timeless poetry, qawwali & spiritual peace",
        gradient = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
        group = CategoryGroup.GENRE,
    ),
    MusicCategory(
        id = "classical",
        title = "Classical Ragas",
        hindiTitle = "शास्त्रीय राग",
        emoji = "🪕",
        description = "Sitar, tabla maestros & meditative harmony",
        gradient = listOf(Color(0xFF614385), Color(0xFF516395)),
        group = CategoryGroup.GENRE,
    ),

    // ── Vibes & Activity ──
    MusicCategory(
        id = "lofi",
        title = "Desi Lo-Fi",
        hindiTitle = "लो-फाई मूड",
        emoji = "🌙",
        description = "Slowed & reverb, nostalgic rainy evening beats",
        gradient = listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)),
        group = CategoryGroup.VIBE,
    ),
    MusicCategory(
        id = "focus",
        title = "Focus & Study",
        hindiTitle = "स्टडी और एकाग्रता",
        emoji = "📚",
        description = "Deep work, alpha waves & calm instrumental",
        gradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        group = CategoryGroup.VIBE,
    ),
    MusicCategory(
        id = "travel",
        title = "Road Trip & Drive",
        hindiTitle = "सफ़र के साथी",
        emoji = "🚗",
        description = "Open highways, scenic vistas & wanderlust",
        gradient = listOf(Color(0xFF396AFC), Color(0xFF2948FF)),
        group = CategoryGroup.VIBE,
    ),
    MusicCategory(
        id = "bhakti",
        title = "Bhakti & Devotion",
        hindiTitle = "भक्ति और शांति",
        emoji = "🕉️",
        description = "Morning aartis, peaceful mantras & divine chants",
        gradient = listOf(Color(0xFFFF6A00), Color(0xFFEE0979)),
        group = CategoryGroup.VIBE,
    ),
)

@Composable
fun CategoryScreen(
    listState: LazyListState,
    contentPadding: PaddingValues,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (id: String, title: String, subtitle: String?, thumbnail: String?, type: BrowseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<MusicCategory?>(null) }
    var selectedGroup by remember { mutableStateOf(CategoryGroup.ALL) }
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    AnimatedContent(
        targetState = selectedCategory,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "categoryScreenNav",
        modifier = modifier.fillMaxSize(),
    ) { category ->
        if (category == null) {
            // ── Main Category Catalog Grid ──
            CategoryCatalogView(
                listState = listState,
                contentPadding = contentPadding,
                selectedGroup = selectedGroup,
                onSelectGroup = { selectedGroup = it },
                onSelectCategory = { cat ->
                    haptics.play(Haptic.Select)
                    selectedCategory = cat
                },
                onQuickPlayCategory = { cat ->
                    haptics.play(Haptic.Select)
                    scope.launch {
                        YtMusicRepository.categoryShelves(cat.title).onSuccess { shelves ->
                            val songs = shelves.flatMap { it.items }
                                .filter { it.videoId != null }
                                .map { it.toSong() }
                            if (songs.isNotEmpty()) {
                                onPlaySongs(songs, 0)
                            }
                        }
                    }
                },
            )
        } else {
            // ── Category Detail / Music Player Feed ──
            CategoryDetailView(
                category = category,
                contentPadding = contentPadding,
                onBack = {
                    haptics.play(Haptic.Select)
                    selectedCategory = null
                },
                onPlaySongs = onPlaySongs,
                onOpenDetail = onOpenDetail,
            )
        }
    }
}

@Composable
private fun CategoryCatalogView(
    listState: LazyListState,
    contentPadding: PaddingValues,
    selectedGroup: CategoryGroup,
    onSelectGroup: (CategoryGroup) -> Unit,
    onSelectCategory: (MusicCategory) -> Unit,
    onQuickPlayCategory: (MusicCategory) -> Unit,
) {
    val currentLocale = try {
        val first = AppCompatDelegate.getApplicationLocales().get(0)
        first?.toLanguageTag() ?: first?.language
    } catch (_: Throwable) {
        null
    } ?: Locale.getDefault().toLanguageTag()

    val isHindi = currentLocale.startsWith("hi", ignoreCase = true)

    val filteredCategories = remember(selectedGroup) {
        if (selectedGroup == CategoryGroup.ALL) AllMusicCategories
        else AllMusicCategories.filter { it.group == selectedGroup }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "cat_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .padding(top = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = if (isHindi) "श्रेणियाँ और मूड्स" else "Categories & Moods",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "अपनी पसंद की कैटेगरी चुनें और बेहतरीन संगीत सुनें"
                    else "Choose your favorite category, mood, or workout vibe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Filter Chips ──
        item(key = "cat_filters") {
            val chips = listOf(
                CategoryGroup.ALL to (if (isHindi) "सभी" else "All"),
                CategoryGroup.MOOD to (if (isHindi) "मूड्स" else "Moods"),
                CategoryGroup.WORKOUT to (if (isHindi) "जिम और पावर" else "Workout & Power"),
                CategoryGroup.GENRE to (if (isHindi) "संगीत शैलियाँ" else "Genres"),
                CategoryGroup.VIBE to (if (isHindi) "वाइब्स" else "Vibes"),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                items(chips) { (group, label) ->
                    val isSelected = selectedGroup == group
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
                            .clickable { onSelectGroup(group) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── Grid of Categories in pairs ──
        items(filteredCategories.chunked(2)) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryCard(
                    category = pair[0],
                    isHindi = isHindi,
                    onClick = { onSelectCategory(pair[0]) },
                    onPlay = { onQuickPlayCategory(pair[0]) },
                    modifier = Modifier.weight(1f),
                )
                if (pair.size > 1) {
                    CategoryCard(
                        category = pair[1],
                        isHindi = isHindi,
                        onClick = { onSelectCategory(pair[1]) },
                        onPlay = { onQuickPlayCategory(pair[1]) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    category: MusicCategory,
    isHindi: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = category.gradient,
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f),
                    )
                ),
                RoundedCornerShape(18.dp),
            )
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        // Top row: Emoji & Play Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = category.emoji,
                fontSize = 28.sp,
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable {
                        haptics.play(Haptic.Select)
                        onPlay()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play ${category.title}",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Bottom column: Title & Hindi Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (isHindi) category.hindiTitle else category.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryDetailView(
    category: MusicCategory,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (id: String, title: String, subtitle: String?, thumbnail: String?, type: BrowseType) -> Unit,
) {
    var shelves by remember(category.id) { mutableStateOf<List<HomeShelf>?>(null) }
    var loading by remember(category.id) { mutableStateOf(true) }
    var error by remember(category.id) { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()

    LaunchedEffect(category.id) {
        loading = true
        error = null
        YtMusicRepository.categoryShelves(category.title).fold(
            onSuccess = {
                shelves = it
                loading = false
            },
            onFailure = {
                error = it.message ?: "Failed to load ${category.title}"
                loading = false
            },
        )
    }

    val allSongs = remember(shelves) {
        shelves?.flatMap { it.items }
            ?.filter { it.videoId != null }
            ?.map { it.toSong() }
            .orEmpty()
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── Header Banner ──
        item(key = "detail_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(category.gradient))
                    .padding(18.dp),
            ) {
                Column {
                    // Back button & Emoji
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable { onBack() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                        Text(
                            text = category.emoji,
                            fontSize = 32.sp,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${category.hindiTitle} • ${category.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )

                    Spacer(Modifier.height(16.dp))

                    // Action Buttons: Play All & Shuffle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Play All Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (allSongs.isNotEmpty()) {
                                        haptics.play(Haptic.Select)
                                        onPlaySongs(allSongs, 0)
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Play Mix",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.Black,
                                )
                            }
                        }

                        // Shuffle Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    if (allSongs.isNotEmpty()) {
                                        haptics.play(Haptic.Select)
                                        onPlaySongs(allSongs.shuffled(), 0)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        } else if (error != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            // Render Shelves
            shelves?.forEach { shelf ->
                item(key = "shelf_${shelf.title}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            text = shelf.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                        )
                        if (shelf.subtitle != null) {
                            Text(
                                text = shelf.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(shelf.items) { idx, item ->
                                CategoryTrackCard(
                                    item = item,
                                    onClick = {
                                        if (item.videoId != null) {
                                            haptics.play(Haptic.Select)
                                            val songs = shelf.items.map { it.toSong() }
                                            onPlaySongs(songs, idx)
                                        } else if (item.browseId != null) {
                                            onOpenDetail(
                                                item.browseId,
                                                item.title,
                                                item.subtitle,
                                                item.thumbnailUrl,
                                                BrowseType.PLAYLIST,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CategoryTrackCard(
    item: ShelfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ShelfItem.toSong(): Song = Song(
    videoId = videoId ?: "",
    title = title,
    artist = subtitle ?: "",
    thumbnailUrl = thumbnailUrl,
)
