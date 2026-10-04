package com.music.dhvani.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.model.Song
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Audio quality mode choices for streaming playback.
 */
enum class PlaybackQualityMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val sourceName: String,
) {
    HIGH_QUALITY(
        id = "JIOSAAVN",
        title = "High Quality",
        subtitle = "JioSaavn • 320 kbps High Fidelity AAC",
        badge = "320 kbps",
        sourceName = "JioSaavn",
    ),
    ORIGINAL_YT(
        id = "YOUTUBE",
        title = "Original Quality",
        subtitle = "YouTube Music • Native Opus / AAC stream",
        badge = "Original",
        sourceName = "YouTube Music",
    ),
    LOSSLESS(
        id = "LOSSLESS",
        title = "Hi-Res Lossless",
        subtitle = "Studio Master • Bit-exact FLAC / ALAC",
        badge = "Lossless",
        sourceName = "Module / Lossless",
    );
}

/**
 * Sleek bottom sheet for switching between audio quality & streaming sources.
 * Offers High Quality (JioSaavn 320kbps), Original Quality (YouTube Music),
 * and Hi-Res Lossless (if configured module is present).
 */
@Composable
fun AudioQualityPickerSheet(
    song: Song?,
    nerdStats: NerdStats.Snapshot?,
    preferredSource: String,
    hasLosslessModule: Boolean = false,
    onSelectQuality: (PlaybackQualityMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissSmoothly = {
        if (isVisible) {
            isVisible = false
            scope.launch {
                delay(220)
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    Dialog(
        onDismissRequest = { dismissSmoothly() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isVisible) 0.65f else 0f,
            animationSpec = tween(durationMillis = 240),
            label = "scrim",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable { dismissSmoothly() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(160)),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color(0xFF0F1118),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .clickable(enabled = false) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )

                        Spacer(Modifier.height(16.dp))

                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                Text(
                                    text = "Audio Quality & Source",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Switch playback stream quality & provider",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            // Live Active Source Pill
                            val liveSourceLabel = when {
                                nerdStats?.isLossless == true -> "Lossless FLAC"
                                nerdStats?.isHiQuality == true || nerdStats?.sourceName?.contains("saavn", ignoreCase = true) == true -> "JioSaavn 320k"
                                else -> "YouTube Opus"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = liveSourceLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        // Current Song Info Snippet
                        if (song != null) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161922),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color.White.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Quality Options
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val activeModeId = when {
                                nerdStats?.isLossless == true -> "LOSSLESS"
                                nerdStats?.isHiQuality == true || nerdStats?.sourceName?.contains("saavn", ignoreCase = true) == true -> "JIOSAAVN"
                                nerdStats?.sourceName?.contains("youtube", ignoreCase = true) == true -> "YOUTUBE"
                                else -> preferredSource.ifBlank { "JIOSAAVN" }
                            }
                            val effectiveSelected = preferredSource.ifBlank { activeModeId }

                            // 1. High Quality (JioSaavn)
                            val isJioSelected = effectiveSelected.equals("JIOSAAVN", ignoreCase = true)
                            QualityOptionCard(
                                title = "High Quality",
                                subtitle = "JioSaavn • Crystal clear 320 kbps AAC audio",
                                badgeText = "320 kbps",
                                icon = Icons.Rounded.HighQuality,
                                isSelected = isJioSelected,
                                isRecommended = true,
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    scope.launch {
                                        isVisible = false
                                        delay(180)
                                        onSelectQuality(PlaybackQualityMode.HIGH_QUALITY)
                                    }
                                },
                            )

                            // 2. Original Quality (YouTube Music)
                            val isYtSelected = effectiveSelected.equals("YOUTUBE", ignoreCase = true)
                            QualityOptionCard(
                                title = "Original Quality",
                                subtitle = "YouTube Music • Native Opus / AAC ~160 kbps",
                                badgeText = "Original",
                                icon = Icons.Rounded.MusicNote,
                                isSelected = isYtSelected,
                                isRecommended = false,
                                onClick = {
                                    haptics.play(Haptic.Tap)
                                    scope.launch {
                                        isVisible = false
                                        delay(180)
                                        onSelectQuality(PlaybackQualityMode.ORIGINAL_YT)
                                    }
                                },
                            )

                            // 3. Hi-Res Lossless (if available or configured)
                            if (hasLosslessModule) {
                                val isLosslessSelected = effectiveSelected.equals("LOSSLESS", ignoreCase = true)
                                QualityOptionCard(
                                    title = "Hi-Res Lossless",
                                    subtitle = "Tidal / Studio Master • Bit-exact FLAC audio",
                                    badgeText = "FLAC",
                                    icon = Icons.Rounded.GraphicEq,
                                    isSelected = isLosslessSelected,
                                    isRecommended = false,
                                    onClick = {
                                        haptics.play(Haptic.Tap)
                                        scope.launch {
                                            isVisible = false
                                            delay(180)
                                            onSelectQuality(PlaybackQualityMode.LOSSLESS)
                                        }
                                    },
                                )
                            }



                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardBg = if (isSelected) {
        primaryColor.copy(alpha = 0.12f)
    } else {
        Color(0xFF161923)
    }

    val borderColor = if (isSelected) {
        primaryColor.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon container with soft glow
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) primaryColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) primaryColor else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isSelected) primaryColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    if (isRecommended && !isSelected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "HQ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF38BDF8),
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(10.dp))

            // Check / Radio indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                )
            }
        }
    }
}
