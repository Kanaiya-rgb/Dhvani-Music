package com.music.dhvani.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.dhvani.R
import com.music.dhvani.data.model.HomeShelf
import com.music.dhvani.data.model.MoodGenre
import com.music.dhvani.data.model.MoodGenreSection
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.UiState
import com.music.dhvani.ui.components.MessageState
import com.music.dhvani.ui.components.PAGE_GUTTER
import com.music.dhvani.ui.components.PullToRefresh
import com.music.dhvani.ui.components.ShimmerBox
import com.music.dhvani.ui.components.feedSkeleton
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import com.music.dhvani.ui.theme.uiDesignCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: UiState<List<MoodGenreSection>>,
    listState: LazyListState,
    onCategoryClick: (MoodGenre) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.explore),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }
            when (state) {
                UiState.Loading -> item { ExploreSkeleton() }
                is UiState.Error -> item {
                    MessageState(state.message, actionLabel = stringResource(R.string.retry), onAction = onRetry)
                }
                is UiState.Success -> state.data.forEach { section ->
                    item(key = section.title) {
                        MoodGenreGrid(section = section, onCategoryClick = onCategoryClick)
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for CategoryScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    state: UiState<List<MoodGenreSection>>,
    listState: LazyListState,
    onCategoryClick: (MoodGenre) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) = ExploreScreen(
    state = state,
    listState = listState,
    onCategoryClick = onCategoryClick,
    onRetry = onRetry,
    refreshing = refreshing,
    onRefresh = onRefresh,
    pullState = pullState,
    contentPadding = contentPadding,
    modifier = modifier,
)

@Composable
private fun MoodGenreGrid(
    section: MoodGenreSection,
    onCategoryClick: (MoodGenre) -> Unit,
) {
    Column(Modifier.padding(bottom = 22.dp)) {
        SectionHeader(section.title)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - PAGE_GUTTER * 2 - 12.dp) / 2
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = PAGE_GUTTER),
            ) {
                section.items.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item ->
                            MoodGenreCard(
                                item = item,
                                onClick = { onCategoryClick(item) },
                                modifier = Modifier.width(cardWidth),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodGenreCard(
    item: MoodGenre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val color = moodColor(item.title)
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(106.dp)
            .uiDesignCard(
                shape = cardShape,
                backgroundColor = color,
                onClick = {
                    haptics.play(Haptic.Select)
                    onClick()
                },
            )
            .padding(12.dp),
    ) {
        // Tilted sleeve container in bottom-right corner with dedicated category icon
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 14.dp)
                .size(80.dp)
                .graphicsLayer { rotationZ = 16f }
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.40f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = moodGenreIcon(item.title)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(36.dp),
            )
            item.thumbnailUrl?.let { artwork ->
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(end = 46.dp),
        )
    }
}

private fun moodGenreIcon(title: String): ImageVector {
    val clean = title.trim().lowercase(Locale.ROOT)
    return when {
        clean.contains("chill") || clean.contains("relax") -> Icons.Rounded.Spa
        clean.contains("commute") || clean.contains("travel") || clean.contains("drive") -> Icons.Rounded.DirectionsCar
        clean.contains("energ") || clean.contains("power") -> Icons.Rounded.Bolt
        clean.contains("feel-good") || clean.contains("feel good") || clean.contains("happy") -> Icons.Rounded.SentimentSatisfied
        clean.contains("focus") || clean.contains("study") || clean.contains("work") -> Icons.Rounded.Psychology
        clean.contains("gaming") || clean.contains("game") -> Icons.Rounded.SportsEsports
        clean.contains("party") || clean.contains("club") || clean.contains("celebrat") -> Icons.Rounded.Celebration
        clean.contains("romance") || clean.contains("love") || clean.contains("romantic") -> Icons.Rounded.Favorite
        clean.contains("sad") || clean.contains("heartbreak") || clean.contains("cry") -> Icons.Rounded.WaterDrop
        clean.contains("sleep") || clean.contains("night") || clean.contains("bed") -> Icons.Rounded.Bedtime
        clean.contains("workout") || clean.contains("gym") || clean.contains("fitness") -> Icons.Rounded.FitnessCenter
        clean.contains("bollywood") || clean.contains("desi") || clean.contains("hindi") -> Icons.Rounded.GraphicEq
        clean.contains("punjabi") -> Icons.Rounded.MusicNote
        clean.contains("rock") || clean.contains("metal") -> Icons.Rounded.ElectricBolt
        clean.contains("pop") -> Icons.Rounded.Star
        clean.contains("hip") || clean.contains("rap") -> Icons.Rounded.Mic
        clean.contains("dance") || clean.contains("edm") || clean.contains("electronic") -> Icons.Rounded.Speaker
        clean.contains("classical") || clean.contains("raga") -> Icons.Rounded.LibraryMusic
        clean.contains("jazz") || clean.contains("blues") -> Icons.Rounded.Nightlife
        clean.contains("indie") || clean.contains("alternative") -> Icons.Rounded.Headphones
        clean.contains("folk") || clean.contains("acoustic") -> Icons.Rounded.Waves
        clean.contains("devotional") || clean.contains("bhajan") || clean.contains("spiritual") -> Icons.Rounded.SelfImprovement
        clean.contains("chart") || clean.contains("trend") || clean.contains("top") -> Icons.Rounded.TrendingUp
        clean.contains("new") || clean.contains("release") -> Icons.Rounded.NewReleases
        clean.contains("r&b") || clean.contains("soul") -> Icons.Rounded.QueueMusic
        clean.contains("instrumental") -> Icons.Rounded.Piano
        clean.contains("retro") || clean.contains("80s") || clean.contains("90s") -> Icons.Rounded.Radio
        clean.contains("kid") || clean.contains("family") -> Icons.Rounded.ChildCare
        else -> Icons.Rounded.Album
    }
}

private fun moodColor(title: String): Color = when ((title.hashCode() and Int.MAX_VALUE) % 8) {
    0 -> Color(0xFFE64A19)
    1 -> Color(0xFFEC0B65)
    2 -> Color(0xFF8664AC)
    3 -> Color(0xFF6B4EFF)
    4 -> Color(0xFFBE6100)
    5 -> Color(0xFF233C78)
    6 -> Color(0xFF4D97E5)
    else -> Color(0xFFAA267E)
}

@Composable
private fun ExploreSkeleton() {
    Column(Modifier.padding(horizontal = PAGE_GUTTER)) {
        repeat(5) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                repeat(2) {
                    Box(Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(12.dp))) {
                        ShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(12.dp))
                        ShimmerBox(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = 12.dp)
                                .size(82.dp)
                                .graphicsLayer { rotationZ = 16f },
                            RoundedCornerShape(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodGenrePlaylistsScreen(
    title: String,
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
            )
        }
        when (state) {
            UiState.Loading -> feedSkeleton()
            is UiState.Error -> item {
                MessageState(state.message, actionLabel = stringResource(R.string.retry), onAction = onRetry)
            }
            is UiState.Success -> items(state.data, key = { it.title }) { shelf ->
                Shelf(shelf = shelf, onItemClick = onItemClick)
            }
        }
    }
}
