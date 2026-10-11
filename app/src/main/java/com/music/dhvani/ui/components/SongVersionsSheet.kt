package com.music.dhvani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.SongLanguage
import com.music.dhvani.data.model.detectSongLanguage
import com.music.dhvani.data.sources.TrackMatcher
import com.music.dhvani.ui.theme.rememberArtworkPalette
import java.util.Locale

private val SHEET_SHAPE = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

data class AudioLanguageOption(
    val language: String,
    val song: Song,
    val isCurrent: Boolean,
    val subtitle: String,
)

@Composable
fun SongVersionsSheet(
    song: Song,
    versions: List<Song>,
    isLoading: Boolean,
    onSelectVersion: (Song) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.70f).dp

    // Build distinct audio language options (ONLY valid different languages of the SAME song!)
    val options = remember(versions, song.videoId) {
        val list = mutableListOf<AudioLanguageOption>()
        val seenLangs = mutableSetOf<String>()
        val currentLang = detectSongLanguage(song.title, song.artist, song.albumName) ?: "Original"

        // Ensure current song is represented
        list.add(
            AudioLanguageOption(
                language = currentLang,
                song = song,
                isCurrent = true,
                subtitle = song.title,
            )
        )
        seenLangs.add(currentLang.lowercase(Locale.ROOT))

        val origClean = TrackMatcher.searchableTitle(song.title, "").lowercase(Locale.ROOT)
        val family = SongLanguage.getCrossLanguageFamily(song.title)

        for (v in versions) {
            if (v.videoId == song.videoId) continue

            // 1. MUST have a recognized language! NEVER use raw song title as language!
            val detected = detectSongLanguage(v.title, v.artist, v.albumName) ?: continue
            val langKey = detected.lowercase(Locale.ROOT)

            // MUST be a DIFFERENT language than what is already present!
            if (seenLangs.contains(langKey)) continue

            // 2. Candidate MUST be the SAME song (family match or title match), NOT an unrelated song!
            val candClean = TrackMatcher.searchableTitle(v.title, "").lowercase(Locale.ROOT)
            val isFamilyMatch = family != null && family.any { candClean.contains(it) }
            val isTitleMatch = candClean.startsWith(origClean) || candClean.contains(origClean)

            if (!isFamilyMatch && !isTitleMatch) continue

            // 3. Duration sanity check: audio tracks of the same song have similar duration (within 45s)
            val origSec = TrackMatcher.secondsOf(song.durationText)
            val candSec = TrackMatcher.secondsOf(v.durationText)
            if (origSec != null && candSec != null && Math.abs(origSec - candSec) > 45) continue

            seenLangs.add(langKey)
            list.add(
                AudioLanguageOption(
                    language = detected,
                    song = v,
                    isCurrent = false,
                    subtitle = v.title,
                )
            )
        }
        list
    }

    val palette = rememberArtworkPalette(song.thumbnailUrl, artPx = ROW_ART_PX)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .clip(SHEET_SHAPE)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent,
                    )
                ),
                SHEET_SHAPE,
            ),
    ) {
        ArtworkBackdrop(
            palette = palette,
            imageUrl = song.thumbnailUrl,
            modifier = Modifier.matchParentSize(),
            washFraction = 0.75f,
            artPx = ROW_ART_PX,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            }

            // Header Row (Audio track / Language)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Audio track",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            ),
                            color = palette.onBackground,
                        )
                        Text(
                            text = "Change audio language without restarting",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = palette.onBackgroundVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = palette.onBackgroundVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = palette.divider,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            if (isLoading && versions.size <= 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = palette.accent,
                            strokeWidth = 2.5.dp,
                        )
                        Text(
                            text = "Checking available audio tracks…",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = palette.onBackgroundVariant,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(options, key = { it.song.videoId }) { opt ->
                    val isSelected = opt.isCurrent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) Modifier.background(palette.accent.copy(alpha = 0.16f))
                                else Modifier
                            )
                            .clickable { onSelectVersion(opt.song) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        // Language name and details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = opt.language,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                    ),
                                    color = if (isSelected) palette.accent else palette.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                if (opt.song.sourceQuality == "HIGH") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.accent.copy(alpha = 0.20f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            text = "320k",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = palette.accent,
                                        )
                                    }
                                }
                            }

                            if (opt.subtitle.isNotBlank() && !opt.subtitle.equals(opt.language, ignoreCase = true)) {
                                Text(
                                    text = opt.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = palette.onBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Active",
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (!isLoading && options.size <= 1) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Only one audio track is available for this song.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = palette.onBackgroundVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
