package com.music.dhvani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.playback.PlaybackPosition
import com.music.dhvani.ui.theme.ArtworkPalette
import com.music.dhvani.ui.theme.rememberArtworkPalette

/**
 * iPhone-style Dynamic Island floating notch player.
 * Appears dynamically when music is playing, with bouncy spring animations,
 * kinetic audio equalizer bars, and expandable quick playback controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandPlayer(
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    visible: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    position: PlaybackPosition? = null,
    durationMs: Long = 0L,
) {
    val haptics = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    // If song changes or becomes invisible, reset expanded state
    if (!visible || song == null) {
        expanded = false
    }

    AnimatedVisibility(
        visible = visible && song != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ) { -it * 2 } + fadeIn(tween(250)) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        ),
        exit = slideOutVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        ) { -it * 2 } + fadeOut(tween(180)) + scaleOut(
            targetScale = 0.7f,
            animationSpec = tween(200),
        ),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 4.dp),
    ) {
        if (song == null) return@AnimatedVisibility

        val palette = rememberArtworkPalette(
            imageUrl = song.artworkAt(200) ?: song.thumbnailUrl,
            dark = true,
        )

        val cornerRadius by animateDpAsState(
            targetValue = if (expanded) 24.dp else 20.dp,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 450f,
            ),
            label = "inAppCornerRadius",
        )
        val shape = RoundedCornerShape(cornerRadius)

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.85f),
                    spotColor = palette.accent.copy(alpha = 0.50f),
                )
                .clip(shape)
                .background(Color(0xFF000000))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = shape,
                )
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 450f,
                    ),
                )
                .combinedClickable(
                    onClick = {
                        onOpenFullPlayer()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        expanded = !expanded
                    },
                ),
        ) {
            Crossfade(
                targetState = expanded,
                animationSpec = tween(140, easing = FastOutSlowInEasing),
                label = "InAppDynamicIslandCrossfade",
            ) { isExp ->
                if (isExp) {
                    ExpandedIslandContent(
                        song = song,
                        palette = palette,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        position = position,
                        durationMs = durationMs,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onCollapse = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            expanded = false
                        },
                    )
                } else {
                    CompactIslandContent(
                        song = song,
                        palette = palette,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                    )
                }
            }
        }
    }
}

/**
 * Compact pill showing mini thumbnail, marquee-styled title, and audio equalizer bars.
 */
@Composable
private fun CompactIslandContent(
    song: Song,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    isLoading: Boolean,
) {
    // Subtle rotation for artwork when playing
    val infiniteTransition = rememberInfiniteTransition(label = "compactArtSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "artRotation",
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mini Album Art
        AsyncImage(
            model = song.artworkAt(100) ?: song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .then(if (isPlaying) Modifier.rotate(rotation) else Modifier),
        )

        // Song title and artist
        Column(
            modifier = Modifier
                .widthIn(min = 70.dp, max = 150.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Live Audio Equalizer Bars or Loading Spinner
        if (isLoading) {
            CircularProgressIndicator(
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp),
            )
        } else {
            DynamicIslandEqualizer(
                isPlaying = isPlaying,
                color = palette.accent,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

/**
 * Expanded island card providing playback controls, larger artwork, and progress bar.
 */
@Composable
private fun ExpandedIslandContent(
    song: Song,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition?,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
) {
    val progress = if (durationMs > 0L && position != null) {
        (position.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    val accentColor = palette.accent

    val playButtonBg = remember(accentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.70f).coerceIn(0.35f, 0.75f)
        hsv[2] = 0.42f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    val sideButtonBg = remember(accentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.30f).coerceIn(0.08f, 0.25f)
        hsv[2] = 0.22f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Column(
        modifier = Modifier
            .width(295.dp)
            .padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top section: Artwork, Track Info, and Collapse button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = song.artworkAt(200) ?: song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quick collapse icon
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Collapse Island",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Mini seek progress bar tinted with artwork accent
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.22f),
            strokeCap = StrokeCap.Round,
        )

        // Bottom Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Live status tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DynamicIslandEqualizer(
                    isPlaying = isPlaying,
                    barCount = 3,
                    color = accentColor,
                )
                Text(
                    text = if (isPlaying) "Playing" else "Paused",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accentColor,
                )
            }

            // Playback buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(sideButtonBg)
                        .clickable(onClick = onPrevious),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(playButtonBg)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(sideButtonBg)
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * 4-bar kinetic audio equalizer visualizer.
 * Dances lively when [isPlaying] is true, resting at subtle heights when paused.
 */
@Composable
private fun DynamicIslandEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "islandEq")

    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val h2 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(330, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val h3 by transition.animateFloat(
        initialValue = 3f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq3",
    )
    val h4 by transition.animateFloat(
        initialValue = 5f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq4",
    )

    val heights = listOf(
        if (isPlaying) h1 else 4f,
        if (isPlaying) h2 else 6f,
        if (isPlaying) h3 else 3f,
        if (isPlaying) h4 else 4f,
    )

    Row(
        modifier = modifier.height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until barCount.coerceAtMost(4)) {
            val barHeight by animateDpAsState(
                targetValue = heights[i].dp,
                label = "barHeight$i",
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
