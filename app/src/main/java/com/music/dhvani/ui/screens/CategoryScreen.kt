package com.music.dhvani.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
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
import com.music.dhvani.data.model.SearchResult
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.Song
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import com.music.dhvani.ui.icons.DhvaniIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale

enum class ExploreTab {
    MOODS_GENRES, NEW_RELEASES, CHARTS, PODCASTS
}

enum class MoodGroup {
    FOR_YOU, MOODS_MOMENTS, GENRES
}

data class MusicCategory(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val emoji: String,
    val description: String,
    val gradient: List<Color>,
    val group: MoodGroup,
)

val ForYouCategories = listOf(
    MusicCategory("romance", "Romance", "रोमांटिक धुनें", "💖", "Bollywood love ballads & duets", listOf(Color(0xFFE91E63), Color(0xFFFF5252)), MoodGroup.FOR_YOU),
    MusicCategory("hindi", "Hindi", "बॉलीवुड सदाबहार", "🎬", "Golden melodies & blockbuster hits", listOf(Color(0xFFF7971E), Color(0xFFFFD200)), MoodGroup.FOR_YOU),
    MusicCategory("feelgood", "Feel good", "खुशी और मुस्कान", "✨", "Uplifting feel-good anthems", listOf(Color(0xFFFF8008), Color(0xFFFFC837)), MoodGroup.FOR_YOU),
    MusicCategory("desihiphop", "Desi hip-hop", "देसी हिप-हॉप", "🔥", "Gully rap, street flow & heavy bars", listOf(Color(0xFF0F2027), Color(0xFF203A43)), MoodGroup.FOR_YOU),
    MusicCategory("hiphop", "Hip-hop", "ग्लोबल हिप-हॉप", "🎧", "Global trap, 808s & lyrical beats", listOf(Color(0xFF141E30), Color(0xFF243B55)), MoodGroup.FOR_YOU),
    MusicCategory("devotional", "Devotional", "भक्ति और शांति", "🕉️", "Aartis, bhajans & divine mantras", listOf(Color(0xFFFF6A00), Color(0xFFEE0979)), MoodGroup.FOR_YOU),
)

val MoodsMomentsCategories = listOf(
    MusicCategory("chill", "Chill", "सुकून और शांति", "☕", "Cozy acoustic, sunset calm & peaceful vibes", listOf(Color(0xFF007991), Color(0xFF78FFD6)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("commute", "Commute", "सफ़र और ड्राइव", "🚗", "City drives, traffic chill & open road", listOf(Color(0xFF396AFC), Color(0xFF2948FF)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("energize", "Energize", "जोश और ताक़त", "⚡", "Adrenaline rush, power & unstoppable vibe", listOf(Color(0xFFFF512F), Color(0xFFDD2476)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("feelgood_m", "Feel good", "अच्छी तरंगें", "🌟", "Smiles, sunshine & positive frequencies", listOf(Color(0xFFF3904F), Color(0xFF3B4371)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("focus", "Focus", "स्टडी और एकाग्रता", "📚", "Deep study lofi, alpha waves & calm focus", listOf(Color(0xFF11998E), Color(0xFF38EF7D)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("gaming", "Gaming", "गेमिंग बीट्स", "🎮", "High BPM trap, synthwave & victory tunes", listOf(Color(0xFF8A2387), Color(0xFFE94057)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("party", "Party", "पार्टी और डांस", "🎉", "Club bangers, DJ drops & dhol beats", listOf(Color(0xFFDA22FF), Color(0xFF9733EE)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("romance_m", "Romance", "प्यार के पल", "💖", "Soulful duets, acoustic warmth & hearts", listOf(Color(0xFFE91E63), Color(0xFFFF5252)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("sad", "Sad", "दर्द भरे गीत", "💔", "Melancholy, emotional tears & healing notes", listOf(Color(0xFF1E3C72), Color(0xFF2A5298)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("sleep", "Sleep", "गहरी नींद और सुकून", "🌙", "Delta sleep waves, gentle ambient rain", listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)), MoodGroup.MOODS_MOMENTS),
    MusicCategory("workout", "Workout", "जिम और कसरत", "🏋️", "Heavy lifting, cardio pump & beast mode", listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)), MoodGroup.MOODS_MOMENTS),
)

val GenreCategories = listOf(
    MusicCategory("african", "African", "अफ्रीकन बीट्स", "🌍", "Afrobeats, Amapiano & African rhythm", listOf(Color(0xFFB993D6), Color(0xFF8CA6DB)), MoodGroup.GENRES),
    MusicCategory("arabic", "Arabic", "अरबी धुनें", "🏜️", "Oud melodies, Arabic pop & Middle East", listOf(Color(0xFFD38312), Color(0xFFA83279)), MoodGroup.GENRES),
    MusicCategory("bengali", "Bengali", "বাংলা গান", "🪕", "Modern Bengali pop, Rabindra & folk", listOf(Color(0xFF2C3E50), Color(0xFFBDC3C7)), MoodGroup.GENRES),
    MusicCategory("bhojpuri", "Bhojpuri", "भोजपुरी धमाका", "🌾", "High bass Bhojpuri bangers & masti", listOf(Color(0xFFF7971E), Color(0xFFFFD200)), MoodGroup.GENRES),
    MusicCategory("carnatic", "Carnatic classical", "कर्नाटक संगीत", "🎻", "Divine ragas, veena & violin mastery", listOf(Color(0xFF614385), Color(0xFF516395)), MoodGroup.GENRES),
    MusicCategory("classical", "Classical", "शास्त्रीy संगीत", "🪕", "Sitar, sarod, flute & tabla maestros", listOf(Color(0xFF3A1C71), Color(0xFFD76D77)), MoodGroup.GENRES),
    MusicCategory("country", "Country & Americana", "कंट्री संगीत", "🤠", "Acoustic storytelling & southern roots", listOf(Color(0xFF4B6CB7), Color(0xFF182848)), MoodGroup.GENRES),
    MusicCategory("dance", "Dance & electronic", "ईडीएम और डांस", "🪩", "EDM, house, club anthems & bass drops", listOf(Color(0xFF7F00FF), Color(0xFFE100FF)), MoodGroup.GENRES),
    MusicCategory("decades", "Decades", "दशकों का संगीत", "⏳", "Golden 60s, 70s, 80s, 90s & 2000s classics", listOf(Color(0xFF000000), Color(0xFF434343)), MoodGroup.GENRES),
    MusicCategory("desihiphop_g", "Desi hip-hop", "देसी रैप और ट्रैप", "🎤", "Street lyricism, gully rap & beats", listOf(Color(0xFF1F1C18), Color(0xFF8E0E00)), MoodGroup.GENRES),
    MusicCategory("devotional_g", "Devotional", "भजन और कीर्तन", "🕉️", "Spiritual awakening, peaceful chants", listOf(Color(0xFFFF512F), Color(0xFFF09819)), MoodGroup.GENRES),
    MusicCategory("family", "Family", "पारिवारिक गीत", "👨‍👩‍👧‍👦", "Celebrations, wedding songs & nostalgia", listOf(Color(0xFF56AB2F), Color(0xFFA8E063)), MoodGroup.GENRES),
    MusicCategory("folk", "Folk & acoustic", "लोक संगीत", "🪘", "Traditional roots, dholak & desi soul", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), MoodGroup.GENRES),
    MusicCategory("ghazal", "Ghazal/sufi", "ग़ज़ल और सूफ़ी", "🕯️", "Soul of Sufism, Mehfil & timeless poetry", listOf(Color(0xFF200122), Color(0xFF6F0000)), MoodGroup.GENRES),
    MusicCategory("gujarati", "Gujarati", "ગુજરાતી ગીતો", "🎭", "Garba, Gujarati folk & modern pop", listOf(Color(0xFFFF8008), Color(0xFFFFC837)), MoodGroup.GENRES),
    MusicCategory("haryanvi", "Haryanvi", "हरियाणवी बीट्स", "🚜", "High-energy Haryanvi power anthems", listOf(Color(0xFFB71C1C), Color(0xFFE53935)), MoodGroup.GENRES),
    MusicCategory("hindi_g", "Hindi", "बॉलीवुड हिट्स", "🎬", "Bollywood chartbusters & soundtrack magic", listOf(Color(0xFFF27121), Color(0xFFE94057)), MoodGroup.GENRES),
    MusicCategory("hindustani", "Hindustani classical", "हिंदुस्तानी शास्त्रीय", "🪕", "Khayal, thumri & classical traditions", listOf(Color(0xFF3E5151), Color(0xFFDECBA4)), MoodGroup.GENRES),
    MusicCategory("hiphop_g", "Hip-hop", "हिप-हॉप", "🔥", "Boom-bap, modern trap & drill", listOf(Color(0xFF232526), Color(0xFF414345)), MoodGroup.GENRES),
    MusicCategory("indianindie", "Indian indie", "भारतीय इंडी", "🌿", "Independent acoustic, indie poetry & chill", listOf(Color(0xFF134E5E), Color(0xFF71B280)), MoodGroup.GENRES),
    MusicCategory("indianpop", "Indian pop", "आई-पॉप", "🌟", "Modern viral desi pop & dance hits", listOf(Color(0xFFDA22FF), Color(0xFF9733EE)), MoodGroup.GENRES),
    MusicCategory("indiealt", "Indie & alternative", "अल्टरनेटिव रॉक", "🎸", "Alternative rock, indie folk & grunge", listOf(Color(0xFF0F2027), Color(0xFF2C5364)), MoodGroup.GENRES),
    MusicCategory("jpop", "J-Pop", "जापानी पॉप", "🌸", "Tokyo pop bops, city pop & anime OST", listOf(Color(0xFFEC008C), Color(0xFFFC6767)), MoodGroup.GENRES),
    MusicCategory("jazz", "Jazz", "जैज़ और सैक्सोफोन", "🎷", "Velvet saxophone, blue notes & smooth piano", listOf(Color(0xFF1A2980), Color(0xFF26D0CE)), MoodGroup.GENRES),
    MusicCategory("kpop", "K-Pop", "के-पॉप", "💜", "Korean chartbusters, dance & idol anthems", listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)), MoodGroup.GENRES),
    MusicCategory("kannada", "Kannada", "ಕನ್ನಡ ಹಾಡುಗಳು", "🌴", "Sandalwood hits, melodies & rhythm", listOf(Color(0xFFC33764), Color(0xFF1D2671)), MoodGroup.GENRES),
    MusicCategory("latin", "Latin", "लैटिन संगीत", "💃", "Reggaeton, tropical bachata & fiesta", listOf(Color(0xFFFF512F), Color(0xFFDD2476)), MoodGroup.GENRES),
    MusicCategory("malayalam", "Malayalam", "മലയാളം ഗാനങ്ങൾ", "🛶", "God's own melodies, soothing acoustic", listOf(Color(0xFF005AA7), Color(0xFFFFFDE4)), MoodGroup.GENRES),
    MusicCategory("marathi", "Marathi", "मराठी गाणी", "🚩", "Dhol tasha, bhavgeete & modern Marathi", listOf(Color(0xFFFF8008), Color(0xFFFFC837)), MoodGroup.GENRES),
    MusicCategory("metal", "Metal", "मेटल और गिटार", "⚡", "Heavy distortion, thunder & double bass", listOf(Color(0xFF000000), Color(0xFF53346C)), MoodGroup.GENRES),
    MusicCategory("monsoon", "Monsoon", "बारिश और सुकून", "🌧️", "Rain therapy, chai & romantic showers", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)), MoodGroup.GENRES),
    MusicCategory("pop", "Pop", "पॉप संगीत", "🎤", "Global chart-toppers & sparkling melodies", listOf(Color(0xFFE55D87), Color(0xFF5FC3E4)), MoodGroup.GENRES),
    MusicCategory("punjabi", "Punjabi", "ਪੰਜਾਬੀ ਗੀਤ", "🪘", "Bhangra swagger, beats & Punjabi wave", listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), MoodGroup.GENRES),
    MusicCategory("rnb", "R&B & soul", "आर एंड बी", "🎙️", "Smooth groove, velvet vocals & deep soul", listOf(Color(0xFF3E5151), Color(0xFFDECBA4)), MoodGroup.GENRES),
    MusicCategory("reggae", "Reggae & caribbean", "रेगे बीट्स", "🏝️", "Island rhythm, dub & tropical sun", listOf(Color(0xFF56AB2F), Color(0xFFA8E063)), MoodGroup.GENRES),
    MusicCategory("rock", "Rock", "रॉक संगीत", "🎸", "Electric riffs, desi rock & stadium energy", listOf(Color(0xFF232526), Color(0xFF414345)), MoodGroup.GENRES),
    MusicCategory("tamil", "Tamil", "தமிழ் பாடல்கள்", "🛕", "Kollywood magic, Anirudh wave & melodies", listOf(Color(0xFFFF416C), Color(0xFF8A2387)), MoodGroup.GENRES),
    MusicCategory("telugu", "Telugu", "తెలుగు పాటలు", "🏹", "Tollywood blockbusters & mass beats", listOf(Color(0xFFF7971E), Color(0xFFFFD200)), MoodGroup.GENRES),
)

val AllMusicCategories = ForYouCategories + MoodsMomentsCategories + GenreCategories

/**
 * Main Explore & Moods & Genres Screen
 */
@Composable
fun ExploreScreen(
    listState: LazyListState,
    contentPadding: PaddingValues,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (id: String, title: String, subtitle: String?, thumbnail: String?, type: BrowseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<MusicCategory?>(null) }
    var selectedTab by remember { mutableStateOf(ExploreTab.MOODS_GENRES) }
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
        label = "exploreScreenNav",
        modifier = modifier.fillMaxSize(),
    ) { category ->
        if (category == null) {
            ExploreMainView(
                listState = listState,
                contentPadding = contentPadding,
                selectedTab = selectedTab,
                onSelectTab = {
                    haptics.play(Haptic.Select)
                    selectedTab = it
                },
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
                onPlaySongs = onPlaySongs,
                onOpenDetail = onOpenDetail,
            )
        } else {
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

/**
 * Backward compatibility alias for CategoryScreen
 */
@Composable
fun CategoryScreen(
    listState: LazyListState,
    contentPadding: PaddingValues,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (id: String, title: String, subtitle: String?, thumbnail: String?, type: BrowseType) -> Unit,
    modifier: Modifier = Modifier,
) = ExploreScreen(
    listState = listState,
    contentPadding = contentPadding,
    onPlaySongs = onPlaySongs,
    onOpenDetail = onOpenDetail,
    modifier = modifier,
)

@Composable
private fun ExploreMainView(
    listState: LazyListState,
    contentPadding: PaddingValues,
    selectedTab: ExploreTab,
    onSelectTab: (ExploreTab) -> Unit,
    onSelectCategory: (MusicCategory) -> Unit,
    onQuickPlayCategory: (MusicCategory) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (id: String, title: String, subtitle: String?, thumbnail: String?, type: BrowseType) -> Unit,
) {
    val currentLocale = try {
        val first = AppCompatDelegate.getApplicationLocales().get(0)
        first?.toLanguageTag() ?: first?.language
    } catch (_: Throwable) {
        null
    } ?: Locale.getDefault().toLanguageTag()

    val isHindi = currentLocale.startsWith("hi", ignoreCase = true)

    // Dedicated state for Explore live feeds (New Releases, Charts, Podcasts)
    var feedShelves by remember(selectedTab) { mutableStateOf<List<HomeShelf>?>(null) }
    var feedLoading by remember(selectedTab) { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != ExploreTab.MOODS_GENRES) {
            feedLoading = true
            feedShelves = null
            when (selectedTab) {
                ExploreTab.NEW_RELEASES -> {
                    val official = YtMusicRepository.fallbackHomeShelves(3)
                    val fresh = YtMusicRepository.categoryShelves("New Releases").getOrDefault(emptyList())
                    feedShelves = (fresh + official).distinctBy { it.title }
                }
                ExploreTab.CHARTS -> {
                    feedShelves = YtMusicRepository.explore().getOrDefault(emptyList())
                }
                ExploreTab.PODCASTS -> {
                    val podcasts = YtMusicRepository.search("Popular Hindi Podcasts BBC Dinbhar", com.music.dhvani.data.model.SearchFilter.PLAYLISTS)
                        .getOrNull()
                        ?.filterIsInstance<SearchResult.Browse>()
                        ?.map { it.item }
                        ?.take(16)
                    val longListens = YtMusicRepository.search("Bollywood Nonstop 2 Hour Jukebox", com.music.dhvani.data.model.SearchFilter.SONGS)
                        .getOrNull()
                        ?.filterIsInstance<SearchResult.Track>()
                        ?.map { it.song }
                        ?.take(16)

                    val shelves = mutableListOf<HomeShelf>()
                    podcasts?.let {
                        if (it.isNotEmpty()) {
                            shelves.add(
                                HomeShelf(
                                    title = if (isHindi) "लोकप्रिय पॉडकास्ट और शो" else "Popular Podcasts & Shows",
                                    subtitle = "BBC, Geopolitics, News & Stories",
                                    items = it.map { p -> ShelfItem(p.title, p.subtitle, p.thumbnailUrl, null, p.browseId) },
                                )
                            )
                        }
                    }
                    longListens?.let {
                        if (it.isNotEmpty()) {
                            shelves.add(
                                HomeShelf(
                                    title = if (isHindi) "लंबे संगीत सत्र (Long Listens)" else "Long Listens & Jukeboxes",
                                    subtitle = "1 to 2 Hour Non-Stop Listening",
                                    items = it.map { s -> ShelfItem(s.title, s.artist, s.thumbnailUrl, s.videoId, null) },
                                )
                            )
                        }
                    }
                    feedShelves = shelves
                }
                else -> Unit
            }
            feedLoading = false
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── Main Header: Explore / एक्सप्लोर ──
        item(key = "explore_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .padding(top = 16.dp, bottom = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = DhvaniIcons.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = if (isHindi) "एक्सप्लोर" else "Explore",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "मूड्स, शैलियाँ, चार्ट्स और नए एल्बम का संसार"
                    else "Discover Moods & genres, New releases, Charts & Podcasts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Top 4 Explore Nav Pills (YouTube Music matching: Moods & genres, New releases, Charts, Podcasts) ──
        item(key = "explore_tabs") {
            val pills = listOf(
                ExploreTab.MOODS_GENRES to (if (isHindi) "मूड्स और शैलियाँ" else "Moods & genres"),
                ExploreTab.NEW_RELEASES to (if (isHindi) "नए रिलीज़" else "New releases"),
                ExploreTab.CHARTS to (if (isHindi) "चार्ट्स और ट्रेंडिंग" else "Charts"),
                ExploreTab.PODCASTS to (if (isHindi) "पॉडकास्ट" else "Podcasts"),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(pills) { (tab, label) ->
                    val isSelected = selectedTab == tab
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
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
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

        // ── Content Switcher based on Selected Top Pill ──
        if (selectedTab == ExploreTab.MOODS_GENRES) {
            // ── Section 1: For you ──
            item(key = "section_for_you_hdr") {
                SectionHeaderRow(
                    title = if (isHindi) "खास आपके लिए (For you)" else "For you",
                    count = ForYouCategories.size,
                )
            }
            items(ForYouCategories.chunked(2)) { pair ->
                CategoryGridRow(
                    pair = pair,
                    isHindi = isHindi,
                    onSelect = onSelectCategory,
                    onPlay = onQuickPlayCategory,
                )
            }

            // ── Section 2: Moods & moments ──
            item(key = "section_moods_hdr") {
                Spacer(Modifier.height(16.dp))
                SectionHeaderRow(
                    title = if (isHindi) "मूड्स और पल (Moods & moments)" else "Moods & moments",
                    count = MoodsMomentsCategories.size,
                )
            }
            items(MoodsMomentsCategories.chunked(2)) { pair ->
                CategoryGridRow(
                    pair = pair,
                    isHindi = isHindi,
                    onSelect = onSelectCategory,
                    onPlay = onQuickPlayCategory,
                )
            }

            // ── Section 3: Genres ──
            item(key = "section_genres_hdr") {
                Spacer(Modifier.height(16.dp))
                SectionHeaderRow(
                    title = if (isHindi) "सभी संगीत शैलियाँ (Genres)" else "Genres",
                    count = GenreCategories.size,
                )
            }
            items(GenreCategories.chunked(2)) { pair ->
                CategoryGridRow(
                    pair = pair,
                    isHindi = isHindi,
                    onSelect = onSelectCategory,
                    onPlay = onQuickPlayCategory,
                )
            }
        } else {
            // ── Feed View (New Releases / Charts / Podcasts) ──
            if (feedLoading) {
                item(key = "feed_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (feedShelves.isNullOrEmpty()) {
                item(key = "feed_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isHindi) "कोई परिणाम नहीं मिला" else "No content available right now",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                feedShelves?.forEach { shelf ->
                    item(key = "feed_shelf_${shelf.title}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PAGE_GUTTER),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = shelf.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
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
                                        rankNumber = if (selectedTab == ExploreTab.CHARTS) idx + 1 else null,
                                        onClick = {
                                            if (item.videoId != null) {
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
        }

        item {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionHeaderRow(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CategoryGridRow(
    pair: List<MusicCategory>,
    isHindi: Boolean,
    onSelect: (MusicCategory) -> Unit,
    onPlay: (MusicCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryCard(
            category = pair[0],
            isHindi = isHindi,
            onClick = { onSelect(pair[0]) },
            onPlay = { onPlay(pair[0]) },
            modifier = Modifier.weight(1f),
        )
        if (pair.size > 1) {
            CategoryCard(
                category = pair[1],
                isHindi = isHindi,
                onClick = { onSelect(pair[1]) },
                onPlay = { onPlay(pair[1]) },
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
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
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(category.gradient))
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = category.emoji,
                fontSize = 26.sp,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
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
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
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
                                text = "Explore",
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
                    Text(
                        text = "${category.hindiTitle} • ${category.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (allSongs.isNotEmpty()) {
                                        haptics.play(Haptic.Select)
                                        onPlaySongs(allSongs, 0)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play All",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Play All",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.Black,
                            )
                        }

                        Row(
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (loading) {
            item(key = "detail_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (error != null) {
            item(key = "detail_error") {
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
    rankNumber: Int? = null,
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
            if (rankNumber != null) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$rankNumber",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                    )
                }
            }
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
