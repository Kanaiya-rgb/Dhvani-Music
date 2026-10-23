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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.sources.MusicSource
import com.music.dhvani.data.sources.SourceConfig
import com.music.dhvani.data.sources.SourceKind
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.sources.SourceResolver
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import kotlinx.coroutines.Dispatchers
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
        title = "High Quality AAC",
        subtitle = "320 kbps AAC stream",
        badge = "320 kbps",
        sourceName = "High Quality AAC",
    ),
    ORIGINAL_YT(
        id = "YOUTUBE",
        title = "Standard Opus",
        subtitle = "Native Opus stream up to 171 kbps",
        badge = "Opus",
        sourceName = "Standard Opus",
    ),
    LOSSLESS(
        id = "LOSSLESS",
        title = "Hi-Res Lossless",
        subtitle = "Monochrome / Studio Master • Bit-exact FLAC up to 96 kHz",
        badge = "Hi-Res",
        sourceName = "Lossless",
    );
}

/**
 * Real-time availability state of a song on a specific audio source.
 */
sealed class SourceAvailability {
    object Checking : SourceAvailability()
    data class Available(val detail: String, val isLossless: Boolean = false) : SourceAvailability()
    data class Unavailable(val message: String = "Not available for this track") : SourceAvailability()
}

/**
 * Representation of an individual streaming source option available to the user.
 */
data class PlaybackSourceOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val badgeText: String,
    val icon: ImageVector,
    val isLossless: Boolean = false,
    val isRecommended: Boolean = false,
    val sourceConfig: SourceConfig? = null,
)

/**
 * Sleek bottom sheet for switching between audio quality & streaming sources.
 * Dynamically displays all configured & enabled sources (Monochrome Hi-Res, JioSaavn, YouTube Music)
 * and allows instant stream switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQualityPickerSheet(
    song: Song?,
    nerdStats: NerdStats.Snapshot?,
    preferredSource: String,
    sourceConfigs: List<SourceConfig> = emptyList(),
    onSelectSource: (PlaybackSourceOption) -> Unit,
    onConfigureSources: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Build available options from the configured sources
    val options = remember(sourceConfigs) {
        val list = mutableListOf<PlaybackSourceOption>()

        // 1. Configured Lossless / Module sources (Monochrome, Modular plugins)
        val losslessConfigs = sourceConfigs.filter { it.kind.canServeLossless && it.isComplete }
        losslessConfigs.forEachIndexed { idx, cfg ->
            val isMonochrome = cfg.kind == SourceKind.MONOCHROME
            val title = when {
                isMonochrome -> "Monochrome Lossless FLAC"
                else -> cfg.displayName.ifBlank { "Custom Module" }
            }
            val badgeText = when {
                isMonochrome -> "FLAC"
                else -> "FLAC"
            }
            val icon = when {
                isMonochrome -> Icons.Rounded.Waves
                else -> Icons.Rounded.Extension
            }
            list.add(
                PlaybackSourceOption(
                    id = cfg.id,
                    title = title,
                    subtitle = "Bit-exact studio FLAC",
                    badgeText = badgeText,
                    icon = icon,
                    isLossless = true,
                    isRecommended = (idx == 0),
                    sourceConfig = cfg,
                ),
            )
        }

        // 2. JioSaavn (High Quality 320 kbps AAC)
        val jioConfig = sourceConfigs.firstOrNull { it.kind == SourceKind.JIOSAAVN }
        val hasJio = jioConfig?.enabled ?: true
        if (hasJio || sourceConfigs.isEmpty()) {
            list.add(
                PlaybackSourceOption(
                    id = "JIOSAAVN",
                    title = "High Quality AAC",
                    subtitle = "320 kbps AAC stream",
                    badgeText = "320 kbps",
                    icon = Icons.Rounded.HighQuality,
                    isLossless = false,
                    isRecommended = losslessConfigs.isEmpty(),
                    sourceConfig = jioConfig,
                ),
            )
        }

        // 3. YouTube Music (Standard Opus)
        list.add(
            PlaybackSourceOption(
                id = "YOUTUBE",
                title = "Standard Opus",
                subtitle = "Native Opus stream up to 171 kbps",
                badgeText = "Opus",
                icon = Icons.Rounded.PlayCircle,
                isLossless = false,
                isRecommended = false,
            ),
        )

        list
    }

    val availabilityMap = remember { mutableStateMapOf<String, SourceAvailability>() }

    LaunchedEffect(song?.videoId, options) {
        if (song == null) return@LaunchedEffect
        for (opt in options) {
            if (opt.id == "YOUTUBE") {
                availabilityMap[opt.id] = SourceAvailability.Available("Full catalogue Opus")
                continue
            }
            availabilityMap[opt.id] = SourceAvailability.Checking
            launch(Dispatchers.IO) {
                val candidateSource: MusicSource? = when {
                    opt.sourceConfig != null -> SourceRegistry.instance(opt.sourceConfig.id)
                    opt.id == "JIOSAAVN" -> SourceRegistry.active().firstOrNull { it.kind == SourceKind.JIOSAAVN }
                        ?: SourceRegistry.configs.value.firstOrNull { it.kind == SourceKind.JIOSAAVN }?.let { SourceRegistry.instance(it.id) }
                    else -> SourceRegistry.instance(opt.id)
                }
                if (candidateSource != null) {
                    val (isAvail, details) = SourceResolver.probeAvailability(candidateSource, song)
                    availabilityMap[opt.id] = if (isAvail) {
                        SourceAvailability.Available(details, isLossless = opt.isLossless)
                    } else {
                        SourceAvailability.Unavailable(details)
                    }
                } else {
                    availabilityMap[opt.id] = SourceAvailability.Unavailable("Source not configured")
                }
            }
        }
    }

    val liveSourceLabel = when {
        nerdStats?.isLossless == true || nerdStats?.mimeType?.contains("flac", ignoreCase = true) == true ->
            if ((nerdStats.bitDepth ?: 16) > 16 || (nerdStats.sampleRateHz ?: 44100) > 48000) "Hi-Res FLAC" else "Lossless FLAC"
        nerdStats?.isHiQuality == true || nerdStats?.sourceName?.contains("saavn", ignoreCase = true) == true ->
            "320k AAC"
        nerdStats?.sourceName?.contains("youtube", ignoreCase = true) == true ->
            "Standard Opus"
        preferredSource == "LOSSLESS" -> "Lossless FLAC"
        preferredSource.contains("saavn", ignoreCase = true) -> "320k AAC"
        preferredSource.equals("YOUTUBE", ignoreCase = true) -> "Standard Opus"
        else -> "Standard Opus"
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = Color(0xFF0F1118),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.22f),
                width = 36.dp,
                height = 4.dp,
            )
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.82f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header with title & current live format summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Choose Audio Stream",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.width(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = liveSourceLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                val subtitleText = song?.title?.let { "Track: $it" } ?: "Select preferred streaming audio source"
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Color.White.copy(alpha = 0.52f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Dynamic Source Stream Options
            val activeSourceName = nerdStats?.sourceName.orEmpty()
            val claimedSource = nerdStats?.claimed?.source.orEmpty()
            val isCurrentStreamLossless = nerdStats?.isLossless == true || nerdStats?.mimeType?.contains("flac", ignoreCase = true) == true
            val isCurrentStreamAac = !isCurrentStreamLossless && (nerdStats?.isHiQuality == true ||
                activeSourceName.contains("saavn", ignoreCase = true) ||
                activeSourceName.contains("aac", ignoreCase = true) ||
                claimedSource.contains("saavn", ignoreCase = true) ||
                claimedSource.contains("aac", ignoreCase = true) ||
                nerdStats?.mimeType?.contains("mp4a", ignoreCase = true) == true)
            val isCurrentStreamOpus = !isCurrentStreamLossless && !isCurrentStreamAac && (
                activeSourceName.contains("youtube", ignoreCase = true) ||
                activeSourceName.contains("opus", ignoreCase = true) ||
                claimedSource.contains("youtube", ignoreCase = true) ||
                claimedSource.contains("opus", ignoreCase = true) ||
                nerdStats?.mimeType?.contains("opus", ignoreCase = true) == true)

            // Determine the single active/selected option ID
            val activeOptionId: String = when {
                isCurrentStreamLossless -> {
                    // Find which lossless option matches activeSourceName or claimed source
                    options.firstOrNull { opt ->
                        opt.isLossless && (
                            (activeSourceName.isNotBlank() && (opt.title.contains(activeSourceName, ignoreCase = true) || activeSourceName.contains(opt.title, ignoreCase = true))) ||
                            (claimedSource.isNotBlank() && (opt.title.contains(claimedSource, ignoreCase = true) || claimedSource.contains(opt.title, ignoreCase = true))) ||
                            (opt.sourceConfig?.displayName?.isNotBlank() == true && (activeSourceName.contains(opt.sourceConfig.displayName, ignoreCase = true) || claimedSource.contains(opt.sourceConfig.displayName, ignoreCase = true))) ||
                            (opt.sourceConfig?.id?.isNotBlank() == true && (activeSourceName.contains(opt.sourceConfig.id, ignoreCase = true) || claimedSource.contains(opt.sourceConfig.id, ignoreCase = true))) ||
                            (activeSourceName.contains("monochrome", ignoreCase = true) && (opt.id.contains("monochrome", ignoreCase = true) || opt.sourceConfig?.kind == SourceKind.MONOCHROME)) ||
                            (claimedSource.contains("monochrome", ignoreCase = true) && (opt.id.contains("monochrome", ignoreCase = true) || opt.sourceConfig?.kind == SourceKind.MONOCHROME))
                        )
                    }?.id
                        ?: options.firstOrNull { it.isLossless && preferredSource == it.id }?.id
                        ?: options.firstOrNull { it.isLossless && it.isRecommended }?.id
                        ?: options.firstOrNull { it.isLossless }?.id
                        ?: "YOUTUBE"
                }
                isCurrentStreamAac -> "JIOSAAVN"
                isCurrentStreamOpus -> "YOUTUBE"
                else -> when {
                    preferredSource == "LOSSLESS" -> options.firstOrNull { it.isLossless }?.id ?: "YOUTUBE"
                    preferredSource == "JIOSAAVN" -> "JIOSAAVN"
                    preferredSource == "YOUTUBE" -> "YOUTUBE"
                    options.any { it.id == preferredSource } -> preferredSource
                    else -> "YOUTUBE"
                }
            }

            for (opt in options) {
                val isSelected = opt.id == activeOptionId

                QualityOptionCard(
                    title = opt.title,
                    subtitle = opt.subtitle,
                    badgeText = opt.badgeText,
                    icon = opt.icon,
                    isSelected = isSelected,
                    availability = availabilityMap[opt.id],
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            sheetState.hide()
                            onSelectSource(opt)
                            onDismiss()
                        }
                    },
                )
            }

            // Option to Add / Manage custom module or addon
            if (onConfigureSources != null) {
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.035f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.07f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.play(Haptic.Tap)
                            scope.launch {
                                sheetState.hide()
                                onConfigureSources()
                                onDismiss()
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Manage or Add Streaming Addons",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QualityOptionCard(
    title: String,
    badgeText: String,
    icon: ImageVector,
    isSelected: Boolean,
    subtitle: String? = null,
    availability: SourceAvailability? = null,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isUnavailable = availability is SourceAvailability.Unavailable

    val cardBg = when {
        isSelected -> primaryColor.copy(alpha = 0.09f)
        isUnavailable -> Color(0xFF101218)
        else -> Color(0xFF151822)
    }

    val borderColor = when {
        isSelected -> primaryColor.copy(alpha = 0.38f)
        isUnavailable -> Color.White.copy(alpha = 0.04f)
        else -> Color.White.copy(alpha = 0.07f)
    }

    val iconBg = when {
        isSelected -> primaryColor.copy(alpha = 0.18f)
        isUnavailable -> Color.White.copy(alpha = 0.03f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    val iconTint = when {
        isSelected -> primaryColor
        isUnavailable -> Color.White.copy(alpha = 0.30f)
        else -> Color.White.copy(alpha = 0.70f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = !isUnavailable || isSelected,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(13.dp))

            // Title & Status details column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (isUnavailable && !isSelected) Color.White.copy(alpha = 0.38f) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(3.dp))

                // Short status / detail line
                when (availability) {
                    is SourceAvailability.Available -> {
                        val displayText = if (availability.detail.startsWith("Available", ignoreCase = true)) {
                            availability.detail
                        } else {
                            "Available • ${availability.detail}"
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = if (isSelected) primaryColor.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    is SourceAvailability.Unavailable -> {
                        Text(
                            text = availability.message.ifBlank { "Not available for this track" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Color(0xFFF87171).copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    is SourceAvailability.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.4.dp,
                                color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.50f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Checking availability…",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = Color.White.copy(alpha = 0.45f),
                                maxLines = 1,
                            )
                        }
                    }
                    null -> {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = Color.White.copy(alpha = 0.45f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Codec / Quality pill
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(0.6.dp, if (isSelected) primaryColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.09f)),
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isSelected) primaryColor else if (isUnavailable) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                )
            }

            // Checkmark indicator for selected state
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = primaryColor,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}
