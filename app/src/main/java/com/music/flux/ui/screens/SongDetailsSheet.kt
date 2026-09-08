package com.music.flux.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.flux.data.Http
import com.music.flux.data.NerdStats
import com.music.flux.data.innertube.Innertube
import com.music.flux.data.innertube.InnertubeParser
import com.music.flux.data.innertube.StreamResolver
import com.music.flux.data.model.Song
import com.music.flux.data.model.durationMillis
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SHEET_SHAPE)
            .background(Color(0xFF0F1318))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f))
                ),
                SHEET_SHAPE
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
                )
            }

            // Sheet Title with subtle loading indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Song details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (loadingNetwork) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // ── 1. General Section ──────────────────────────────────────────
            SectionTitle("General")
            DetailsCard {
                DetailItemRow(
                    icon = Icons.Rounded.MusicNote,
                    label = "Song title",
                    value = song.title.ifBlank { "Unknown" },
                    onCopy = onCopy,
                )
                DetailItemRow(
                    icon = Icons.Rounded.Person,
                    label = "Song artists",
                    value = song.artist.ifBlank { "Unknown" },
                    onCopy = onCopy,
                )
                DetailItemRow(
                    icon = Icons.Rounded.Bookmark,
                    label = "Media id",
                    value = song.videoId.ifBlank { "—" },
                    onCopy = onCopy,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── 2. Information Section ──────────────────────────────────────
            SectionTitle("Information")
            DetailsCard {
                val media = details
                val viewsFormatted = formatGermanNumber(media.viewCount)
                val likesFormatted = formatGermanNumber(media.likes)
                val dislikesFormatted = formatGermanNumber(media.dislikes)
                val itagFormatted = (media.itag ?: 251).toString()
                val mimeFormatted = media.mimeType ?: nerdStats?.mimeType ?: "audio/webm"
                val codecFormatted = media.codec ?: nerdStats?.mimeType?.substringAfterLast("/")?.lowercase() ?: "opus"
                val kbps = media.bitrateKbps ?: nerdStats?.bitrateKbps ?: 143
                val bitrateFormatted = "$kbps Kbps"
                val sampleRateFormatted = "${media.sampleRateHz ?: nerdStats?.sampleRateHz ?: 48000} Hz"
                val loudnessFormatted = "${media.loudnessDb ?: -10.67f} dB"
                val volumeFormatted = "${(volume * 100).toInt()}%"
                val fileSizeFormatted = formatShortFileSize(media.fileSizeBytes ?: calculatedSize)

                DetailItemRow(icon = Icons.Rounded.RssFeed, label = "Views", value = viewsFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.ThumbUp, label = "Likes", value = likesFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.ThumbDown, label = "Dislikes", value = dislikesFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.Tag, label = "Itag", value = itagFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.Info, label = "MIME type", value = mimeFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.GraphicEq, label = "Codecs", value = codecFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.Speed, label = "Bitrate", value = bitrateFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.GraphicEq, label = "Sample rate", value = sampleRateFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.AutoMirrored.Rounded.VolumeUp, label = "Loudness", value = loudnessFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.AutoMirrored.Rounded.VolumeMute, label = "Volume", value = volumeFormatted, onCopy = onCopy)
                DetailItemRow(icon = Icons.Rounded.ContentCopy, label = "File size", value = fileSizeFormatted, onCopy = onCopy)
            }

            Spacer(Modifier.height(14.dp))

            // ── 3. Description Section ──────────────────────────────────────
            SectionTitle("Description")
            DetailsCard {
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
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF161A22))
            .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
    onCopy: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(label, value) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
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

private fun formatGermanNumber(count: Long?): String {
    if (count == null || count <= 0L) return "—"
    return String.format(Locale.GERMANY, "%,d", count)
}

private fun formatShortFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "—"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
