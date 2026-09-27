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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.data.lyrics.LyricsSource
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Half-screen bottom sheet for picking a lyrics provider.
 * Clean design: no left icon circles, thin color accent bar instead.
 */
@Composable
fun LyricsSourcePickerSheet(
    currentSource: LyricsSource?,
    availableSources: Map<LyricsSource, Boolean>,
    onSelectSource: (LyricsSource) -> Unit,
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
            targetValue = if (isVisible) 0.60f else 0f,
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
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f)
                        .widthIn(max = 540.dp)
                        .clickable(enabled = false) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                        )

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Lyrics Provider",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Choose your preferred lyrics source",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 2.dp),
                        )

                        Spacer(Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            LyricsSource.entries.forEach { source ->
                                val isSelected = currentSource == source
                                val isAvailable = availableSources[source] ?: false
                                val hasProbed = source in availableSources
                                val isNotAvailable = hasProbed && !isAvailable && !isSelected

                                ProviderItemCard(
                                    source = source,
                                    isSelected = isSelected,
                                    isAvailable = isAvailable,
                                    isNotAvailable = isNotAvailable,
                                    onClick = {
                                        haptics.play(Haptic.Tap)
                                        scope.launch {
                                            isVisible = false
                                            delay(180)
                                            onSelectSource(source)
                                        }
                                    },
                                )
                            }

                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderItemCard(
    source: LyricsSource,
    isSelected: Boolean,
    isAvailable: Boolean,
    isNotAvailable: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isAvailable -> Color(0xFF38BDF8)
        isNotAvailable -> Color(0xFFEF4444).copy(alpha = 0.60f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    val cardBg = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        isAvailable -> Color(0xFF38BDF8).copy(alpha = 0.06f)
        else -> Color(0xFF181B24)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(
            0.8.dp,
            when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                isAvailable -> Color(0xFF38BDF8).copy(alpha = 0.20f)
                else -> Color.White.copy(alpha = 0.06f)
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(0.55f)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(accentColor),
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isNotAvailable) Color.White.copy(alpha = 0.50f) else Color.White,
                )
                Text(
                    text = when {
                        isSelected -> "Current provider"
                        isAvailable -> "Available"
                        isNotAvailable -> "Not available"
                        else -> "Tap to check"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        isAvailable -> Color(0xFF38BDF8).copy(alpha = 0.85f)
                        isNotAvailable -> Color(0xFFEF4444).copy(alpha = 0.65f)
                        else -> Color.White.copy(alpha = 0.35f)
                    },
                )
            }

            when {
                isSelected -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                        modifier = Modifier.padding(end = 14.dp),
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                isAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.30f)),
                        modifier = Modifier.padding(end = 14.dp),
                    ) {
                        Text(
                            text = "Available",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                isNotAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.10f),
                        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.20f)),
                        modifier = Modifier.padding(end = 14.dp),
                    ) {
                        Text(
                            text = "Not available",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color(0xFFEF4444).copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
