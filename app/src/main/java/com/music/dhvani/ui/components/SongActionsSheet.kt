package com.music.dhvani.ui.components

import com.music.dhvani.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.automirrored.rounded.Undo

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.CanvasStyle
import com.music.dhvani.data.settings.DownloadNetwork
import com.music.dhvani.data.settings.DownloadQuality
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Language

import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import com.music.dhvani.ui.icons.DhvaniIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.sources.SourceKind
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.model.LikeStatus
import com.music.dhvani.data.model.ROW_ART_PX
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.model.detectSongLanguage
import com.music.dhvani.download.DownloadState
import com.music.dhvani.download.Downloads
import com.music.dhvani.playback.SleepTimer
import com.music.dhvani.ui.components.thumbnailBorder
import com.music.dhvani.ui.theme.ArtworkPalette
import com.music.dhvani.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Long-press menu for a track, in the shape music apps normally use.
 *
 * The account actions lead — rating, playlists, library — because they are
 * what the menu is opened for; the queue and navigation rows below it were
 * always the fallback for "I meant to do something with this song".
 *
 * Everything that writes to the account is hidden outright when [signedIn] is
 * false rather than shown and refused. The same goes for a track that is
 * playing from a local file or a finished download (`song.localUri != null`):
 * rating, playlists, downloading it again and sharing all assume a YouTube
 * identity the file doesn't carry, so those rows drop out regardless of
 * [signedIn].
 *
 * [showSleepTimer] and [onShare] are the player's extras: a sleep timer isn't a
 * property of some row in a list, so it only appears where it means something.
 *
 * [onDownload] is only the *start* of a download — cancelling one and deleting
 * a saved file are answered here, because neither needs anything the caller
 * has. Starting one might: below API 29 it needs a storage permission that only
 * an Activity can ask for.
 *
 * The sheet is painted in the track's own colours, the same way its album page
 * is — it is opened *from* that artwork, usually with it still on screen behind
 * the scrim, and a slab of flat grey in front of a coloured page reads as
 * something borrowed from another app. The host supplies no container colour
 * and no drag handle; both are drawn here, over the tint.
 */
@Composable
fun SongActionsSheet(
    song: Song,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onDownload: () -> Unit,
    onToggleLike: (() -> Unit)? = null,
    onToggleDislike: (() -> Unit)? = null,
    onDislikeAndSkip: (() -> Unit)? = null,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    showSleepTimer: Boolean = false,
    /** Opens the bottom sheet to select alternate versions or languages for this track. */
    onOpenVersions: (() -> Unit)? = null,
    onOpenAudioQuality: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenAudioEffects: (() -> Unit)? = null,
    onOpenLyricsOffset: (() -> Unit)? = null,
    lyricsOffsetMs: Long = 0L,
    onEnterAmbientCinemaMode: (() -> Unit)? = null,

    onSetAsRingtone: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onShareLyrics: (() -> Unit)? = null,
    /**
     * Copies what the app logged while starting this track. Null everywhere
     * except the player, where "this track" means something.
     */
    onCopyLog: (() -> Unit)? = null,
    /**
     * Opens a full-detail sheet for the current track — title, artist, media
     * id and technical stream info (codec, bitrate, sample rate). Only ever
     * supplied by the player; everywhere else the sheet is about a row, not
     * a live stream, so some fields would be empty or misleading.
     */
    onOpenDetails: (() -> Unit)? = null,
    /**
     * True while a lookup for this track's album/artist ids is still in
     * flight, so it isn't yet known whether "Open album" and "Open artist"
     * belong on this sheet at all. Only the player ever opens a sheet before
     * it knows; everywhere else this is simply false, and the two rows behave
     * as before — present when the id is there, absent when it never was.
     */
    resolvingLinks: Boolean = false,
) {
    var pickingSleepTimer by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    // Read from the thumbnail the row that opened this sheet was already
    // showing, not a larger copy of it: the tint is a blur and a handful of
    // swatches, neither of which a bigger image improves, and going back for
    // one is what had the sheet opening grey and colouring in afterwards.
    val palette = rememberArtworkPalette(song.thumbnailUrl, artPx = ROW_ART_PX)
    val liked = likeStatus == LikeStatus.LIKE
    val disliked = likeStatus == LikeStatus.DISLIKE
    // A local file or a finished download has no YouTube identity behind it to
    // rate, save, queue into a playlist, fetch again, or share a link for.
    val isOffline = song.localUri != null

    val currentNerdStats by NerdStats.current.collectAsStateWithLifecycle()
    val preferredPlaybackSource by AppSettings.preferredPlaybackSource.collectAsStateWithLifecycle()
    val sourceConfigs by SourceRegistry.configs.collectAsStateWithLifecycle()

    val audioSourceCompactValue = remember(currentNerdStats, preferredPlaybackSource, sourceConfigs) {
        val activeSource = when {
            currentNerdStats?.isLossless == true || currentNerdStats?.mimeType?.contains("flac", ignoreCase = true) == true -> "Lossless"
            currentNerdStats?.isHiQuality == true ||
                currentNerdStats?.sourceName?.contains("saavn", ignoreCase = true) == true ||
                currentNerdStats?.sourceName?.contains("aac", ignoreCase = true) == true -> "High Quality AAC"
            else -> "Standard Opus"
        }
        val losslessCount = sourceConfigs.count { it.kind.canServeLossless && it.isComplete && it.enabled }.coerceAtLeast(1)
        val jioCount = if (sourceConfigs.none { it.kind == SourceKind.JIOSAAVN && !it.enabled }) 1 else 0
        val ytCount = 1
        val totalSources = losslessCount + jioCount + ytCount
        val otherSources = (totalSources - 1).coerceAtLeast(0)
        if (otherSources > 0) "$activeSource +$otherSources" else activeSource
    }

    TintedSheet(palette = palette, imageUrl = song.thumbnailUrl, modifier = modifier) {
        if (pickingSleepTimer) {
            SleepTimerPicker(palette = palette, onBack = { pickingSleepTimer = false })
            return@TintedSheet
        }

        // Centered Share action header
        if (!isOffline && (onShare != null || onShareLyrics != null)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (onShareLyrics != null && onShare != null) {
                                showShareOptionsDialog = true
                            } else if (onShareLyrics != null) {
                                onShareLyrics()
                            } else {
                                onShare?.invoke()
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = DhvaniIcons.ShareBox,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }

        if (showShareOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showShareOptionsDialog = false },
                title = {
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Option 1: Share song link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    showShareOptionsDialog = false
                                    onShare?.invoke()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share song",
                                tint = palette.accent,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share song",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Share YouTube Music link",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }

                        // Option 2: Share lyrics card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    showShareOptionsDialog = false
                                    onShareLyrics?.invoke()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Share lyrics",
                                tint = palette.accent,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share lyrics",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Create and share lyric card poster",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showShareOptionsDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1E1E22),
                shape = RoundedCornerShape(20.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            val hasGroup1 = (onOpenAudioQuality != null && !isOffline) ||
                (onOpenVersions != null && !isOffline) ||
                (onOpenLyricsOffset != null) ||
                (onSetAsRingtone != null)

            // Group 1: Quality & Audio Settings
            if (onOpenAudioQuality != null && !isOffline) {
                ActionRow(
                    icon = Icons.Rounded.GraphicEq,
                    label = "Audio source",
                    value = audioSourceCompactValue,
                    accent = palette.accent,
                    onClick = onOpenAudioQuality,
                )
            }
            if (onOpenVersions != null && !isOffline) {
                val currentLang = remember(song.title, song.artist, song.albumName) {
                    detectSongLanguage(song.title, song.artist, song.albumName) ?: "Original"
                }
                ActionRow(
                    icon = Icons.Rounded.Language,
                    label = "Audio track",
                    value = currentLang,
                    accent = palette.accent,
                    onClick = onOpenVersions,
                )
            }
            if (onOpenLyricsOffset != null) {
                val offsetSec = lyricsOffsetMs / 1000f
                val offsetFormatted = String.format(Locale.ROOT, "%.1f", offsetSec)
                val offsetValue = if (lyricsOffsetMs > 0L) "+$offsetFormatted s" else if (lyricsOffsetMs < 0L) "$offsetFormatted s" else "0.0 s"
                ActionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Lyrics offset",
                    value = offsetValue,
                    accent = palette.accent,
                    onClick = onOpenLyricsOffset,
                )
            }

            onSetAsRingtone?.let { setRingtone ->
                ActionRow(
                    icon = DhvaniIcons.Ringtone,
                    label = "Set as Ringtone",
                    accent = palette.accent,
                    onClick = setRingtone,
                )
            }

            if (hasGroup1) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Group 2: Playback & Library Actions
            var hasGroup2 = false
            if (!isOffline) {

                if (onToggleDislike != null || onDislikeAndSkip != null) {
                    hasGroup2 = true
                    ActionRow(
                        icon = if (disliked) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt,
                        label = if (disliked) "Undo dislike" else if (onDislikeAndSkip != null) "Dislike & skip" else "Dislike",
                        tint = if (disliked) palette.accent else null,
                        accent = palette.accent,
                        onClick = {
                            if (!disliked && onDislikeAndSkip != null) {
                                onDislikeAndSkip()
                            } else {
                                onToggleDislike?.invoke()
                            }
                        },
                    )
                }
                onRemoveFromPlaylist?.let {
                    hasGroup2 = true
                    ActionRow(
                        icon = Icons.Rounded.PlaylistRemove,
                        label = "Remove from this playlist",
                        accent = palette.accent,
                        onClick = it,
                    )
                }
            }

            if (onStartRadio != null) {
                hasGroup2 = true
                ActionRow(
                    icon = Icons.Rounded.Radio,
                    label = "Start radio",
                    accent = palette.accent,
                    onClick = onStartRadio,
                )
            }
            if (onPlayNext != null) {
                hasGroup2 = true
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    label = "Play next",
                    accent = palette.accent,
                    onClick = onPlayNext,
                )
            }
            if (onAddToQueue != null) {
                hasGroup2 = true
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Add to queue",
                    accent = palette.accent,
                    onClick = onAddToQueue,
                )
            }
            if (!isOffline) {
                hasGroup2 = true
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    label = "Add to playlist",
                    accent = palette.accent,
                    onClick = onAddToPlaylist,
                )
            }

            hasGroup2 = true
            DownloadRow(song, palette, isOffline, onDownload)

            if (hasGroup2) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Group 3: Navigation & Info (Microphone for artist + artist subtitle, Album with album subtitle, Info)
            var hasGroup3 = false
            when (val id = song.artistId) {
                null -> if (resolvingLinks) {
                    hasGroup3 = true
                    LoadingActionRow(DhvaniIcons.Mic, "Open artist", palette)
                }
                else -> {
                    hasGroup3 = true
                    ActionRow(
                        icon = DhvaniIcons.Mic,
                        label = "Open artist",
                        subtitle = song.artist.takeIf { it.isNotBlank() },
                        accent = palette.accent,
                    ) { onOpenArtist(id) }
                }
            }
            when (val id = song.albumId) {
                null -> if (resolvingLinks) {
                    hasGroup3 = true
                    LoadingActionRow(DhvaniIcons.Album, "Open album", palette)
                }
                else -> {
                    hasGroup3 = true
                    ActionRow(
                        icon = DhvaniIcons.Album,
                        label = "Open album",
                        subtitle = (song.albumName ?: song.title).takeIf { it.isNotBlank() },
                        accent = palette.accent,
                    ) { onOpenAlbum(id) }
                }
            }
            if (onOpenDetails != null) {
                hasGroup3 = true
                ActionRow(
                    icon = Icons.Rounded.Info,
                    label = "Song info",
                    accent = palette.accent,
                    onClick = onOpenDetails,
                )
            }

            val hasGroup4 = showSleepTimer || onOpenEqualizer != null || onOpenAudioEffects != null || onCopyLog != null
            if (hasGroup3 && hasGroup4) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Group 4: Tools & Utilities
            if (showSleepTimer) {
                val sleepStatus = sleepTimerStatus()
                ActionRow(
                    icon = DhvaniIcons.SleepTimer,
                    label = "Sleep timer",
                    value = sleepStatus,
                    accent = palette.accent,
                ) { pickingSleepTimer = true }
            }
            onOpenEqualizer?.let { openEq ->
                ActionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Equalizer",
                    accent = palette.accent,
                    onClick = openEq,
                )
            }
            onOpenAudioEffects?.let { openEffects ->
                ActionRow(
                    icon = DhvaniIcons.SpeedDial,
                    label = "Slowed / Nightcore Mode",
                    accent = palette.accent,
                    onClick = openEffects,
                )
            }
            onCopyLog?.let {
                ActionRow(
                    icon = Icons.Rounded.BugReport,
                    label = "Copy log",
                    accent = palette.accent,
                    onClick = it,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * A bottom sheet wearing the artwork's colours: the tint and its blurred wash
 * behind, the rounded top corners and the drag handle drawn over it.
 *
 * The corners and the handle are this composable's job rather than
 * `ModalBottomSheet`'s because the host has to pass a transparent container for
 * the wash to be visible at all — and a transparent container has nothing left
 * to clip or to hang a handle on.
 */
private val SHEET_SHAPE = RoundedCornerShape(32.dp)

/**
 * Floating rounded sheet in Dhvani aesthetic:
 * 32dp continuous rounding, horizontal margins, dark translucent slate background,
 * subtle 1px border, and top drag handle.
 */
@Composable
private fun TintedSheet(
    palette: ArtworkPalette,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.96f).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .heightIn(max = maxSheetHeight)
                .clip(SHEET_SHAPE)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E2028).copy(alpha = 0.82f),
                            Color(0xFF121317).copy(alpha = 0.80f),
                        )
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    SHEET_SHAPE,
                ),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f)),
                    )
                }
                content()
            }
        }
    }
}

/**
 * One row carrying the whole life of a download: start it, watch it, cancel it,
 * and delete what it produced.
 *
 * A row rather than a screen because that is the size of the decision. The
 * files land in the device's own Music folder, which already has a manager
 * — the Files app — and building a second one inside this app would be
 * duplicating it in a worse place. What this app uniquely knows is which *song*
 * a file belongs to, and that is exactly what this row says.
 *
 * The state comes straight from [Downloads] rather than through the caller: it
 * changes while the sheet is open, and threading a flow through the sheet's
 * arrangement the sleep timer row already uses.
 */
@Composable
private fun DownloadRow(song: Song, palette: ArtworkPalette, isOffline: Boolean, onDownload: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active by Downloads.active.collectAsStateWithLifecycle()
    val saved by Downloads.saved.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val downloadNetwork by AppSettings.downloadNetwork.collectAsStateWithLifecycle()
    val alwaysAskDownloadOptions by AppSettings.alwaysAskDownloadOptions.collectAsStateWithLifecycle()
    var showDownloadOptionsDialog by remember { mutableStateOf(false) }

    // The record is a claim about a folder the user manages themselves, so it
    // is checked against the disk rather than trusted - re-checked whenever the
    // record for this track changes, which is what makes the row settle onto
    // "Saved" the moment a download finishes.
    //
    // Seeded with the record rather than with null so that moment isn't a
    // flicker: a finished download clears the running state and writes the
    // record in the same breath, and starting pessimistic would show "Download"
    // again for as long as the check off the main thread takes.
    val recorded = saved[song.videoId]
    val file by produceState(recorded?.let(Uri::parse), song.videoId, recorded) {
        value = Downloads.savedUri(context, song.videoId)
    }

    // A failure is worth stating once. Leaving it set would have the row still
    // reporting last week's dead connection the next time the sheet is opened.
    DisposableEffect(song.videoId) {
        onDispose { Downloads.dismissFailure(song.videoId) }
    }

    when (val state = active[song.videoId]) {
        is DownloadState.Queued -> ActionRow(
            icon = Icons.Rounded.Downloading,
            label = "Queued",
            value = "Cancel",
            accent = palette.accent,
        ) { Downloads.cancel(song.videoId) }

        is DownloadState.Running -> {
            val progressText = com.music.dhvani.download.DownloadFormatters.formatRunningProgress(
                fraction = state.fraction,
                bytesDownloaded = state.bytesDownloaded,
                totalBytes = state.totalBytes,
                bytesPerSecond = state.bytesPerSecond,
                etaSeconds = state.etaSeconds,
                isFinalizing = state.isFinalizing,
            )
            val percentLabel = when {
                state.isFinalizing -> "100%"
                state.fraction > 0f -> "${(state.fraction * 100).toInt()}%"
                else -> null
            }
            ActionRow(
                icon = Icons.Rounded.Downloading,
                label = if (state.isFinalizing) "Finalizing download" else "Downloading",
                subtitle = if (state.isFinalizing) "Embedding tags & saving…" else progressText,
                value = percentLabel,
                tint = palette.accent,
                accent = palette.accent,
            ) { Downloads.cancel(song.videoId) }
        }

        is DownloadState.Failed -> ActionRow(
            icon = Icons.Rounded.ErrorOutline,
            label = state.reason,
            value = "Retry",
            // Not the artwork's colour: a failure has to stay legible as a
            // failure whatever the sleeve happens to be tinted.
            tint = MaterialTheme.colorScheme.error,
            accent = MaterialTheme.colorScheme.error,
            onClick = onDownload,
        )

        null -> if (file != null) {
            ActionRow(
                icon = Icons.Rounded.DownloadDone,
                label = "Saved to Downloads",
                value = "Delete",
                tint = palette.accent,
                accent = palette.accent,
            ) { scope.launch { Downloads.delete(context, song.videoId) } }
        } else if (!isOffline) {
            ActionRow(
                icon = Icons.Rounded.Download,
                label = "Download",
                accent = palette.accent,
                onClick = {
                    if (alwaysAskDownloadOptions) {
                        showDownloadOptionsDialog = true
                    } else {
                        onDownload()
                    }
                },
            )
        }
    }

    if (showDownloadOptionsDialog) {
        var selectedQuality by remember { mutableStateOf(downloadQuality) }
        var selectedNetwork by remember { mutableStateOf(downloadNetwork) }

        AlertDialog(
            onDismissRequest = { showDownloadOptionsDialog = false },
            title = {
                Text(
                    text = "Download Options",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Audio Quality",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    DownloadQuality.entries.forEach { quality ->
                        val selected = quality == selectedQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { selectedQuality = quality }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = quality.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    Text(
                        text = "Network Policy",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    DownloadNetwork.entries.forEach { network ->
                        val selected = network == selectedNetwork
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { selectedNetwork = network }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (network) {
                                        DownloadNetwork.BOTH -> "Mobile Data & Wi-Fi"
                                        DownloadNetwork.WIFI_ONLY -> "Wi-Fi Only"
                                        DownloadNetwork.CELLULAR_ONLY -> "Mobile Data Only"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = when (network) {
                                        DownloadNetwork.BOTH -> "Download over any connection"
                                        DownloadNetwork.WIFI_ONLY -> "Only download on unmetered Wi-Fi"
                                        DownloadNetwork.CELLULAR_ONLY -> "Only download using cellular data"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSettings.setDownloadQuality(selectedQuality)
                        AppSettings.setDownloadNetwork(selectedNetwork)
                        showDownloadOptionsDialog = false
                        onDownload()
                    },
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadOptionsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}


/** End of track or a duration, plus a way out once one is running. */
@Composable
private fun ColumnScope.SleepTimerPicker(palette: ArtworkPalette, onBack: () -> Unit) {
    val chosen by SleepTimer.minutes.collectAsStateWithLifecycle()
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    val countdown = sleepTimerCountdown()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        countdown != null -> "$countdown until playback pauses"
                        afterTrack -> "Pausing when this song ends"
                        else -> "Pause playback after a while"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)

        // Finishing the song is the one people reach for at the end of a
        // listen, so it leads rather than sitting under the durations.
        SleepOption(label = "After this song", selected = afterTrack, accent = palette.accent) {
            SleepTimer.startAfterTrack()
            onBack()
        }
        SleepTimer.PRESETS.forEach { minutes ->
            SleepOption(
                label = if (minutes == 60) "1 hour" else "$minutes minutes",
                selected = minutes == chosen,
                accent = palette.accent,
            ) {
                SleepTimer.start(minutes)
                onBack()
            }
        }
        if (chosen != null || afterTrack) {
            ActionRow(Icons.Rounded.Close, "Turn off timer", accent = palette.accent) {
                SleepTimer.cancel()
                onBack()
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}


@Composable
private fun SleepOption(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Running",
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** What the sleep timer row shows on the right, or null when none is armed. */
@Composable
private fun sleepTimerStatus(): String? {
    val afterTrack by SleepTimer.afterTrack.collectAsStateWithLifecycle()
    return sleepTimerCountdown() ?: "After this song".takeIf { afterTrack }
}

/** Live "m:ss" until the sleep timer fires, or null when none is running. */
@Composable
private fun sleepTimerCountdown(): String? {
    val deadline by SleepTimer.deadline.collectAsStateWithLifecycle()
    val remaining by produceState<Long?>(initialValue = SleepTimer.remainingMs(), deadline) {
        while (deadline != null) {
            value = SleepTimer.remainingMs()
            delay(1_000)
        }
        value = null
    }
    return remaining?.let {
        val seconds = it / 1000
        "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }
}

/**
 * One line of a bottom sheet's menu. Shared with the playlist picker so the
 * two sheets read as the same control rather than as two lists that happen to
 * look alike.
 *
 * [tint] is for rows whose icon carries state — a filled heart on a liked
 * track — and is otherwise the ordinary foreground. [accent] colours the
 * trailing [value], and defaults to the app's own red: a sheet tinted from
 * artwork passes the artwork's accent instead, so the row belongs to the sheet
 * it is drawn on.
 */
@Composable
internal fun ActionRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    subtitle: String? = null,
    tint: Color? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ActionRow(
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = (tint ?: Color.White.copy(alpha = 0.95f)).copy(
                    alpha = if (enabled) 1f else 0.35f,
                ),
                modifier = Modifier.size(22.dp),
            )
        },
        label = label,
        value = value,
        subtitle = subtitle,
        accent = accent,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
internal fun ActionRow(
    leading: @Composable () -> Unit,
    label: String,
    value: String? = null,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(18.dp))
        if (subtitle != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = Color.White.copy(
                        alpha = if (enabled) 1f else 0.35f,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(
                        alpha = if (enabled) 0.55f else 0.25f,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = Color.White.copy(
                    alpha = if (enabled) 1f else 0.35f,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = accent.copy(alpha = if (enabled) 1f else 0.4f),
                maxLines = 1,
            )
        }
    }
}

/**
 * Stands in for [ActionRow] while whether it belongs on the sheet at all is
 * still unknown.
 */
@Composable
private fun LoadingActionRow(icon: ImageVector, label: String, palette: ArtworkPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        )
        CircularProgressIndicator(
            color = Color.White.copy(alpha = 0.6f),
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The track a sheet is about, drawn at its head. Shared by the actions menu
 * and the playlist picker, which is the same track two taps later.
 *
 * [subtitleColor] exists because the two sheets stand on different ground: the
 * picker's is the flat theme background, where the usual dim grey is right,
 * while the actions sheet is tinted from this very artwork and needs the
 * credit brighter to stay off the wash.
 */
@Composable
internal fun SheetTrackHeader(
    song: Song,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .thumbnailBorder(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The heading over a sheet's second half — "Add to playlist". */
@Composable
internal fun SheetHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 4.dp),
    )
}
