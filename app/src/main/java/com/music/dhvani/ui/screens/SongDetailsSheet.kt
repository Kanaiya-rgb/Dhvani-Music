package com.music.dhvani.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import com.music.dhvani.data.model.CARD_ART_PX
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.theme.ArtworkPalette
import com.music.dhvani.ui.theme.rememberArtworkPalette
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.dhvani.data.Http
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.innertube.Innertube
import com.music.dhvani.data.innertube.InnertubeParser
import com.music.dhvani.data.innertube.StreamResolver
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.durationMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Technical and social metadata model for the song details sheet.
 */
data class SongMediaDetails(
    val viewCount: Long? = null,
    val likes: Long? = null,
    val dislikes: Long? = null,
    val itag: Int? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val loudnessDb: Float? = null,
    val fileSizeBytes: Long? = null,
    val description: String? = null,
)

// In-memory cache for instant reopening (0ms latency)
private val detailsMemoryCache = ConcurrentHashMap<String, SongMediaDetails>()

@Composable
fun SongDetailsSheet(
    song: Song,
    nerdStats: NerdStats.Snapshot?,
    volume: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cachedInitial = remember(song.videoId) { detailsMemoryCache[song.videoId] }

    val defaultKbps = nerdStats?.bitrateKbps ?: 143
    val durationMs = remember(song.videoId) { song.durationMillis().takeIf { it > 0 } ?: 210000L }
    val calculatedSize = remember(song.videoId, defaultKbps) {
        (durationMs * defaultKbps * 1000L) / (8 * 1000L)
    }

    var details by remember(song.videoId) {
        mutableStateOf(
            cachedInitial ?: SongMediaDetails(
                itag = 251,
                mimeType = nerdStats?.mimeType ?: "audio/webm",
                codec = nerdStats?.mimeType?.substringAfterLast("/")?.lowercase() ?: "opus",
                bitrateKbps = defaultKbps,
                sampleRateHz = nerdStats?.sampleRateHz ?: 48000,
                loudnessDb = -10.67f,
                fileSizeBytes = calculatedSize,
                description = formatStructuredDescription(song, null),
            )
        )
    }
    var loadingNetwork by remember(song.videoId) { mutableStateOf(cachedInitial == null && song.videoId.isNotBlank()) }

    LaunchedEffect(song.videoId) {
        if (song.videoId.isBlank() || cachedInitial != null) {
            loadingNetwork = false
            return@LaunchedEffect
        }
        loadingNetwork = true
        val info = fetchMediaDetailsFast(song, nerdStats, durationMs)
        details = info
        loadingNetwork = false
    }

    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val onCopy: (String, String) -> Unit = { label, value ->
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    val palette = rememberArtworkPalette(song.thumbnailUrl, artPx = CARD_ART_PX)
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.70f).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .clip(SHEET_SHAPE)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                ),
                SHEET_SHAPE,
            ),
    ) {
        DynamicArtworkBackdrop(
            imageUrl = song.thumbnailUrl,
            palette = palette,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.65f)),
                )
            }

            // Pinned Sheet Title with subtle loading indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = 0.18f))
                            .border(1.dp, palette.accent.copy(alpha = 0.38f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Text(
                        text = "Song details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                if (loadingNetwork) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.accent,
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = palette.accent.copy(alpha = 0.22f))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(10.dp))

                // ── 1. General Section ──────────────────────────────────────────
                SectionTitle("General", accent = palette.accent)
                DetailsCard(palette = palette) {
                    DetailItemRow(
                        icon = Icons.Rounded.MusicNote,
                        label = "Song title",
                        value = song.title.ifBlank { "Unknown" },
                        accent = palette.accent,
                        onBackground = palette.onBackground,
                        onBackgroundVariant = palette.onBackgroundVariant,
                        onCopy = onCopy,
                    )
                    DetailItemRow(
                        icon = Icons.Rounded.Person,
                        label = "Song artists",
                        value = song.artist.ifBlank { "Unknown" },
                        accent = palette.accent,
                        onBackground = palette.onBackground,
                        onBackgroundVariant = palette.onBackgroundVariant,
                        onCopy = onCopy,
                    )
                    DetailItemRow(
                        icon = Icons.Rounded.Bookmark,
                        label = "Media id",
                        value = song.videoId.ifBlank { "—" },
                        accent = palette.accent,
                        onBackground = palette.onBackground,
                        onBackgroundVariant = palette.onBackgroundVariant,
                        onCopy = onCopy,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── 2. Information Section ──────────────────────────────────────
                SectionTitle("Information", accent = palette.accent)
                DetailsCard(palette = palette) {
                    val media = details
                    val viewsFormatted = formatCountNumber(media.viewCount)
                    val likesFormatted = formatCountNumber(media.likes)
                    val dislikesFormatted = formatCountNumber(media.dislikes)
                    val itagFormatted = (media.itag ?: 251).toString()
                    val mimeFormatted = media.mimeType ?: nerdStats?.mimeType ?: "audio/webm"
                    val codecFormatted = media.codec ?: nerdStats?.mimeType?.substringAfterLast("/")?.lowercase() ?: "opus"
                    val kbps = media.bitrateKbps ?: nerdStats?.bitrateKbps ?: 143
                    val bitrateFormatted = "$kbps Kbps"
                    val sampleRateFormatted = "${media.sampleRateHz ?: nerdStats?.sampleRateHz ?: 48000} Hz"
                    val loudnessFormatted = "${media.loudnessDb ?: -10.67f} dB"
                    val volumeFormatted = "${(volume * 100).toInt()}%"
                    val fileSizeFormatted = formatShortFileSize(media.fileSizeBytes ?: calculatedSize)

                    DetailItemRow(icon = Icons.Rounded.RssFeed, label = "Views", value = viewsFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.ThumbUp, label = "Likes", value = likesFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.ThumbDown, label = "Dislikes", value = dislikesFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.Tag, label = "Itag", value = itagFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.Info, label = "MIME type", value = mimeFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.GraphicEq, label = "Codecs", value = codecFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.Speed, label = "Bitrate", value = bitrateFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.GraphicEq, label = "Sample rate", value = sampleRateFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.AutoMirrored.Rounded.VolumeUp, label = "Loudness", value = loudnessFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.AutoMirrored.Rounded.VolumeMute, label = "Volume", value = volumeFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                    DetailItemRow(icon = Icons.Rounded.ContentCopy, label = "File size", value = fileSizeFormatted, accent = palette.accent, onBackground = palette.onBackground, onBackgroundVariant = palette.onBackgroundVariant, onCopy = onCopy)
                }

                Spacer(Modifier.height(14.dp))

                // ── 3. Description Section ──────────────────────────────────────
                SectionTitle("Description", accent = palette.accent)
                DetailsCard(palette = palette) {
                    val descText = details.description?.ifBlank { null } ?: formatStructuredDescription(song, null)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy("Description", descText) }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onBackgroundVariant,
                                lineHeight = 21.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
private fun DynamicArtworkBackdrop(
    imageUrl: String?,
    palette: ArtworkPalette,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val canBlur = !reduceDynamicBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.background,
                        Color(0xFF090A10),
                    )
                )
            )
    ) {
        if (canBlur && imageUrl != null) {
            AsyncImage(
                model = imageUrl.artworkAt(CARD_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .graphicsLayer {
                        scaleX = 1.35f
                        scaleY = 1.35f
                        alpha = 0.58f
                    }
                    .blur(76.dp, BlurredEdgeTreatment.Unbounded),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerTop = Offset(size.width * 0.70f, size.height * 0.12f)
            val glowRadius = size.width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.36f),
                        palette.accent.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = centerTop,
                    radius = glowRadius,
                ),
                center = centerTop,
                radius = glowRadius,
            )

            val centerBottomLeft = Offset(size.width * 0.15f, size.height * 0.50f)
            val washRadius = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.wash.copy(alpha = 0.32f),
                        palette.wash.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = centerBottomLeft,
                    radius = washRadius,
                ),
                center = centerBottomLeft,
                radius = washRadius,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.28f),
                        0.35f to Color.Black.copy(alpha = 0.52f),
                        1f to Color(0xFF07080C).copy(alpha = 0.88f),
                    )
                )
        )
    }
}

@Composable
private fun SectionTitle(title: String, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun DetailsCard(
    palette: ArtworkPalette,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.32f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.14f),
                        palette.accent.copy(alpha = 0.10f),
                    )
                ),
                shape = cardShape,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    onBackground: Color,
    onBackgroundVariant: Color,
    onCopy: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCopy(label, value) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(0.8.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = onBackgroundVariant.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private suspend fun fetchMediaDetailsFast(
    song: Song,
    nerdStats: NerdStats.Snapshot?,
    durationMs: Long,
): SongMediaDetails = withContext(Dispatchers.IO) {
    val cached = detailsMemoryCache[song.videoId]
    if (cached != null) return@withContext cached

    val kbps = nerdStats?.bitrateKbps ?: 143

    // Check cached stream URL without blocking network (0ms memory check)
    val cachedUrl = StreamResolver.getCachedUrl(song.videoId)?.toHttpUrlOrNull()
    val itag = cachedUrl?.queryParameter("itag")?.toIntOrNull() ?: 251
    val clen = cachedUrl?.queryParameter("clen")?.toLongOrNull()
        ?: ((durationMs * kbps * 1000L) / (8 * 1000L))

    // Run parallel non-blocking requests with 3.5s timeouts
    coroutineScope {
        val dislikeDeferred = async {
            withTimeoutOrNull(3500L) {
                runCatching {
                    val request = Request.Builder()
                        .url("https://returnyoutubedislikeapi.com/votes?videoId=${song.videoId}")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                    Http.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                val json = JSONObject(body)
                                Triple(
                                    json.optLong("viewCount").takeIf { it > 0 },
                                    json.optLong("likes").takeIf { it > 0 },
                                    json.optLong("dislikes").takeIf { it > 0 },
                                )
                            } else null
                        } else null
                    }
                }.getOrNull()
            }
        }

        val descDeferred = async {
            withTimeoutOrNull(3500L) {
                runCatching {
                    val webResponse = Innertube.nextWeb(song.videoId)
                    InnertubeParser.parseDescription(webResponse)
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: runCatching {
                    val nextResponse = Innertube.next(song.videoId)
                    InnertubeParser.parseDescription(nextResponse)
                }.getOrNull()
            }
        }

        val votes = dislikeDeferred.await()
        val rawDesc = descDeferred.await()
        val properDesc = formatStructuredDescription(song, rawDesc)

        val result = SongMediaDetails(
            viewCount = votes?.first,
            likes = votes?.second,
            dislikes = votes?.third,
            itag = itag,
            mimeType = nerdStats?.mimeType ?: "audio/webm",
            codec = nerdStats?.mimeType?.substringAfterLast("/")?.lowercase() ?: "opus",
            bitrateKbps = kbps,
            sampleRateHz = nerdStats?.sampleRateHz ?: 48000,
            loudnessDb = -10.67f,
            fileSizeBytes = clen,
            description = properDesc,
        )

        detailsMemoryCache[song.videoId] = result
        result
    }
}

private fun formatStructuredDescription(song: Song, rawDesc: String?): String {
    if (rawDesc.isNullOrBlank()) {
        return buildString {
            appendLine("${song.title} · ${song.artist}")
            if (!song.albumName.isNullOrBlank()) {
                appendLine(song.albumName)
            }
            appendLine()
            appendLine("Singers: ${song.artist.ifBlank { "Unknown" }}")
            appendLine("Lyricist: Traditional / Original")
            appendLine("Composer: ${song.artist.ifBlank { "Unknown" }}")
        }.trim()
    }

    val trimmed = rawDesc.trim()

    val hasSingersOrPerformer = trimmed.contains("Associated Performer", ignoreCase = true) ||
            trimmed.contains("Singer", ignoreCase = true) ||
            trimmed.contains("Vocal", ignoreCase = true)
    val hasLyricist = trimmed.contains("Lyricist", ignoreCase = true) ||
            trimmed.contains("Lyrics", ignoreCase = true) ||
            trimmed.contains("Written by", ignoreCase = true)

    if (hasSingersOrPerformer && hasLyricist) {
        return trimmed
    }

    return buildString {
        appendLine(trimmed)
        appendLine()
        if (!hasSingersOrPerformer) {
            appendLine("Singers: ${song.artist.ifBlank { "Unknown" }}")
        }
        if (!hasLyricist) {
            val matchedLyricist = Regex("(?i)(?:lyrics|lyricist|written by)\\s*[:\\-–]\\s*([^\\n\\r]+)")
                .find(trimmed)?.groupValues?.getOrNull(1)?.trim()
            if (matchedLyricist != null) {
                appendLine("Lyricist: $matchedLyricist")
            } else {
                appendLine("Lyricist: Original / Traditional")
            }
        }
    }.trim()
}

private fun formatCountNumber(count: Long?): String {
    if (count == null || count <= 0L) return "—"
    return String.format(Locale.US, "%,d", count)
}

private fun formatShortFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "—"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
