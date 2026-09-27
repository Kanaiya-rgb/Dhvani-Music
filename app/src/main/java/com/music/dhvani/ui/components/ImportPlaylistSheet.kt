package com.music.dhvani.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.CloudDownload
import com.music.dhvani.data.playlist.PlaylistImportManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.music.dhvani.data.settings.AppSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.playlist.PlaylistManager
import com.music.dhvani.ui.icons.DhvaniIcons
import kotlinx.coroutines.launch

private val YoutubeRed = Color(0xFFFF0033)
private val YoutubeRedDark = Color(0xFFCC0029)
private val YoutubeGradient = Brush.horizontalGradient(listOf(Color(0xFFFF0033), Color(0xFFFF416C)))

private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyGreenDark = Color(0xFF1AA34A)
private val SpotifyGradient = Brush.horizontalGradient(listOf(Color(0xFF1DB954), Color(0xFF1ED760)))

private val FileBlue = Color(0xFF388AF6)
private val FileGradient = Brush.horizontalGradient(listOf(Color(0xFF388AF6), Color(0xFF5B9DFF)))

@Composable
fun ImportPlaylistSheet(
    onImportSuccess: (title: String, songs: List<Song>, source: String, author: String?, authorAvatarUrl: String?, coverUrl: String?, isAlbum: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialSource: String? = null,
    initialUrl: String? = null,
    modifier: Modifier = Modifier,
    onPlayNow: ((Song) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAlbum by remember(initialSource) { mutableStateOf(initialSource == "ALBUM") }

    var selectedSource by remember(initialSource) {
        mutableStateOf(
            when (initialSource) {
                "SPOTIFY" -> "SPOTIFY"
                "FILE" -> "FILE"
                "YOUTUBE" -> "YOUTUBE"
                else -> "UNIVERSAL"
            },
        )
    }

    var playlistTitle by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf(initialUrl.orEmpty()) }
    var parsedTracks by remember { mutableStateOf<List<PlaylistManager.ImportedTrack>>(emptyList()) }
    var directSongs by remember { mutableStateOf<List<Song>?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var currentResolvingSong by remember { mutableStateOf("") }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var showProgressPercent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Success State Modal
    var importedSuccessResult by remember { mutableStateOf<Triple<String, List<Song>, String>?>(null) }

    var spotifyProfilePlaylists by remember { mutableStateOf<List<PlaylistManager.SpotifyProfilePlaylist>>(emptyList()) }
    var spotifyProfileUsername by remember { mutableStateOf<String?>(null) }
    var spotifyProfileAvatar by remember { mutableStateOf<String?>(null) }
    var playlistCoverUrl by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isBatchImporting by remember { mutableStateOf(false) }

    val activeBrandColor = when (selectedSource) {
        "SPOTIFY" -> SpotifyGreen
        "FILE" -> FileBlue
        "YOUTUBE" -> YoutubeRed
        else -> MaterialTheme.colorScheme.primary
    }

    val resolveInput: (String) -> Unit = { text ->
        val trimmed = text.trim()
        errorMessage = null
        val spotifyUserId = PlaylistManager.extractSpotifyUserId(trimmed)
        val spotifyAlbumId = PlaylistManager.extractSpotifyAlbumId(trimmed)
        val spotifyPlId = PlaylistManager.extractSpotifyPlaylistId(trimmed)
        val plId = PlaylistManager.extractPlaylistId(trimmed)
        val videoId = PlaylistManager.extractVideoId(trimmed)

        if (spotifyUserId != null) {
            isAlbum = false
            selectedSource = "SPOTIFY"
            scope.launch {
                isResolving = true
                showProgressPercent = false
                currentResolvingSong = ""
                progressText = "Loading Spotify profile & public playlists..."
                val profileResult = PlaylistManager.fetchSpotifyUserPlaylists(spotifyUserId)
                isResolving = false
                if (profileResult != null && profileResult.playlists.isNotEmpty()) {
                    spotifyProfileUsername = profileResult.username
                    spotifyProfileAvatar = profileResult.avatarUrl
                    spotifyProfilePlaylists = profileResult.playlists
                    selectedPlaylistIds = profileResult.playlists.map { it.id }.toSet()
                    parsedTracks = emptyList()
                    directSongs = null
                } else {
                    errorMessage = "Could not find public playlists on this Spotify profile. Ensure the profile has public playlists."
                }
            }
        } else if (spotifyAlbumId != null) {
            isAlbum = true
            spotifyProfilePlaylists = emptyList()
            spotifyProfileUsername = null
            spotifyProfileAvatar = null
            playlistCoverUrl = null
            selectedSource = "SPOTIFY"
            scope.launch {
                isResolving = true
                showProgressPercent = false
                currentResolvingSong = ""
                progressText = "Fetching Spotify album metadata & tracks..."
                val result = PlaylistManager.fetchSpotifyAlbum(spotifyAlbumId)
                isResolving = false
                if (result != null && result.second.isNotEmpty()) {
                    playlistTitle = result.first
                    parsedTracks = result.second
                    playlistCoverUrl = result.third
                    directSongs = null
                } else {
                    errorMessage = "Could not load Spotify album. Please ensure it is a public album."
                }
            }
        } else if (spotifyPlId != null) {
            isAlbum = false
            spotifyProfilePlaylists = emptyList()
            spotifyProfileUsername = null
            spotifyProfileAvatar = null
            playlistCoverUrl = null
            selectedSource = "SPOTIFY"
            scope.launch {
                isResolving = true
                showProgressPercent = false
                currentResolvingSong = ""
                progressText = "Fetching Spotify playlist metadata..."
                val result = PlaylistManager.fetchSpotifyPlaylist(spotifyPlId)
                isResolving = false
                if (result != null && result.second.isNotEmpty()) {
                    playlistTitle = result.first
                    parsedTracks = result.second
                    playlistCoverUrl = result.third
                    if (result.isAlbum) {
                        isAlbum = true
                    }
                    directSongs = null
                } else {
                    errorMessage = "Could not load Spotify playlist. Please ensure it is a public playlist."
                }
            }
        } else if (plId != null) {
            isAlbum = PlaylistManager.isAlbumId(plId)
            spotifyProfilePlaylists = emptyList()
            spotifyProfileUsername = null
            spotifyProfileAvatar = null
            playlistCoverUrl = null
            selectedSource = "YOUTUBE"
            scope.launch {
                isResolving = true
                showProgressPercent = false
                currentResolvingSong = ""
                progressText = if (isAlbum) "Loading YouTube Music album..." else "Loading YouTube playlist..."
                val result = PlaylistManager.fetchYoutubePlaylist(plId)
                isResolving = false
                if (result != null && result.second.isNotEmpty()) {
                    playlistTitle = result.first
                    directSongs = result.second
                    playlistCoverUrl = result.second.firstOrNull()?.thumbnailUrl
                    parsedTracks = emptyList()
                } else {
                    errorMessage = "Could not load playlist. Please ensure the playlist is public or unlisted."
                }
            }
        } else if (videoId != null) {
            isAlbum = false
            selectedSource = "YOUTUBE"
            playlistCoverUrl = null
            scope.launch {
                isResolving = true
                showProgressPercent = false
                currentResolvingSong = ""
                progressText = "Loading YouTube song..."
                val song = PlaylistManager.fetchYoutubeSingleVideo(videoId)
                isResolving = false
                if (song != null) {
                    playlistTitle = song.title.ifBlank { "Imported Track" }
                    directSongs = listOf(song)
                    playlistCoverUrl = song.thumbnailUrl
                    parsedTracks = emptyList()
                } else {
                    errorMessage = "Could not load song details from link."
                }
            }
        } else if (trimmed.contains("#EXTINF") || trimmed.contains("{")) {
            val parsed = PlaylistManager.parseText(trimmed, "Imported Playlist")
            if (parsed.tracks.isNotEmpty()) {
                selectedSource = "FILE"
                playlistTitle = parsed.title
                parsedTracks = parsed.tracks
                directSongs = null
            } else {
                errorMessage = "No valid tracks found in pasted text."
            }
        } else if (trimmed.isNotBlank()) {
            errorMessage = "Invalid link. Please paste a valid YouTube song/playlist, or Spotify URL."
        }
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            resolveInput(initialUrl)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }.orEmpty()

            if (content.isNotBlank()) {
                val fallback = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".")
                    ?: "Imported Playlist"
                val parsed = PlaylistManager.parseText(content, fallback)
                playlistTitle = parsed.title
                parsedTracks = parsed.tracks
                directSongs = null
                errorMessage = null
                selectedSource = "FILE"
                if (parsed.tracks.isEmpty()) {
                    errorMessage = "No tracks found in selected file."
                }
            }
        }.onFailure {
            errorMessage = "Failed to read file: ${it.message}"
        }
    }

    val totalTracks = directSongs?.size ?: parsedTracks.size
    val firstSongThumbnail = directSongs?.firstOrNull()?.thumbnailUrl
        ?: parsedTracks.firstOrNull()?.videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

    val scrollState = rememberScrollState()

    // ==========================================
    // 1. SUCCESS SCREEN (PROFESSIONAL COMPLETION DIALOG)
    // ==========================================
    if (importedSuccessResult != null) {
        val (finalTitle, importedSongs, source) = importedSuccessResult!!
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Success Animated Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen.copy(alpha = 0.16f))
                    .border(2.dp, SpotifyGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(38.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Playlist Imported!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Successfully added to your library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // Playlist Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val cover = importedSongs.firstOrNull()?.thumbnailUrl
                    if (!cover.isNullOrBlank()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = finalTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpotifyGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = finalTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (source == "SPOTIFY" || source == "SPOTIFY_PROFILE") DhvaniIcons.Spotify else DhvaniIcons.YouTube,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "${importedSongs.size} tracks • Ready to play",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = "Done", fontWeight = FontWeight.Bold)
                }

                if (importedSongs.isNotEmpty() && onPlayNow != null) {
                    Button(
                        onClick = {
                            onPlayNow(importedSongs.first())
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (source == "SPOTIFY" || source == "SPOTIFY_PROFILE") SpotifyGreen else YoutubeRed,
                            contentColor = if (source == "SPOTIFY" || source == "SPOTIFY_PROFILE") Color.Black else Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Play Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        return
    }

    // ==========================================
    // 2. MAIN SHEET CONTENT
    // ==========================================
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        // ---- Top Header Row ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeBrandColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isAlbum -> Icons.Rounded.Album
                        selectedSource == "SPOTIFY" -> DhvaniIcons.Spotify
                        selectedSource == "FILE" -> Icons.Rounded.FolderOpen
                        selectedSource == "YOUTUBE" -> DhvaniIcons.YouTube
                        else -> Icons.Rounded.AutoAwesome
                    },
                    contentDescription = null,
                    tint = if (selectedSource == "FILE") FileBlue else if (selectedSource == "UNIVERSAL" || isAlbum) activeBrandColor else Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isAlbum -> "Import Album"
                        spotifyProfilePlaylists.isNotEmpty() -> "Import Spotify Profile"
                        selectedSource == "SPOTIFY" -> "Import Spotify Playlist"
                        selectedSource == "YOUTUBE" -> "Import YouTube Playlist"
                        selectedSource == "FILE" -> "Import Local Playlist"
                        else -> "Universal Import"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        isAlbum -> "Detected album from ${if (selectedSource == "SPOTIFY") "Spotify" else "YouTube Music"}"
                        spotifyProfilePlaylists.isNotEmpty() -> "Sync Spotify public profile playlists"
                        selectedSource == "SPOTIFY" -> "Sync Spotify playlists or albums"
                        selectedSource == "FILE" -> "Import from local .m3u, .m3u8, or .json files"
                        selectedSource == "YOUTUBE" -> "Import YouTube / YouTube Music playlists & tracks"
                        else -> "Paste any Spotify or YouTube link — playlist, album, or profile"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ==========================================
        // PINNED PROGRESS CARD AT TOP (WHEN RESOLVING)
        // ==========================================
        if (isResolving) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = activeBrandColor.copy(alpha = 0.12f),
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = activeBrandColor,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = progressText.ifBlank { "Importing songs..." },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (currentResolvingSong.isNotBlank()) {
                                Text(
                                    text = "Matching: $currentResolvingSong",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (showProgressPercent) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = activeBrandColor,
                            )
                        }
                    }

                    if (showProgressPercent) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = activeBrandColor,
                            trackColor = activeBrandColor.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "High-speed 16x parallel matching",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Button(
                                onClick = {
                                    if (isBatchImporting) {
                                        val toImport = spotifyProfilePlaylists.filter { it.id in selectedPlaylistIds }
                                        PlaylistImportManager.startBatchProfileImport(
                                            context = context,
                                            username = spotifyProfileUsername ?: "Spotify",
                                            playlists = toImport,
                                            authorAvatarUrl = spotifyProfileAvatar,
                                        )
                                    } else if (parsedTracks.isNotEmpty()) {
                                        val finalTitle = playlistTitle.trim().ifBlank {
                                            if (isAlbum) "Spotify Album" else if (selectedSource == "SPOTIFY") "Spotify Playlist" else "Imported Playlist"
                                        }
                                        PlaylistImportManager.startSinglePlaylistImport(
                                            context = context,
                                            playlistTitle = finalTitle,
                                            tracks = parsedTracks,
                                            source = selectedSource,
                                            coverUrl = playlistCoverUrl,
                                            isAlbum = isAlbum,
                                        )
                                    }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeBrandColor.copy(alpha = 0.2f),
                                    contentColor = activeBrandColor,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Run in Background",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = activeBrandColor,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Supported Platforms & File Picker Chips ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Spotify chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedSource == "SPOTIFY") SpotifyGreen.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .border(
                        1.dp,
                        if (selectedSource == "SPOTIFY") SpotifyGreen.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = DhvaniIcons.Spotify,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Spotify",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedSource == "SPOTIFY") FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (selectedSource == "SPOTIFY") SpotifyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // YouTube chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedSource == "YOUTUBE") YoutubeRed.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .border(
                        1.dp,
                        if (selectedSource == "YOUTUBE") YoutubeRed.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = DhvaniIcons.YouTube,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedSource == "YOUTUBE") FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (selectedSource == "YOUTUBE") YoutubeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Local File (Clickable to browse files directly!)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedSource == "FILE") FileBlue.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .border(
                        1.dp,
                        if (selectedSource == "FILE") FileBlue.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { filePickerLauncher.launch("*/*") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = FileBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Local File",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedSource == "FILE") FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (selectedSource == "FILE") FileBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Sleek Universal Link Input Box
        OutlinedTextField(
            value = inputText,
            onValueChange = { input ->
                val prev = inputText
                inputText = input
                errorMessage = null
                if (input.isBlank()) {
                    selectedSource = "UNIVERSAL"
                    isAlbum = false
                    parsedTracks = emptyList()
                    directSongs = null
                    spotifyProfilePlaylists = emptyList()
                    spotifyProfileUsername = null
                    spotifyProfileAvatar = null
                    playlistCoverUrl = null
                } else if (input.isNotBlank() && (
                    input.startsWith("http") ||
                    input.contains("spotify.com") ||
                    input.contains("youtu") ||
                    input.startsWith("spotify:") ||
                    input.length - prev.length > 5
                )) {
                    resolveInput(input)
                }
            },
            placeholder = {
                Text(
                    text = "Paste any Spotify or YouTube link (Playlist, Album, Profile)...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = when {
                        isAlbum -> Icons.Rounded.Album
                        selectedSource == "SPOTIFY" -> DhvaniIcons.Spotify
                        selectedSource == "YOUTUBE" -> DhvaniIcons.YouTube
                        selectedSource == "FILE" -> Icons.Rounded.FolderOpen
                        else -> Icons.Rounded.Link
                    },
                    contentDescription = null,
                    tint = if (selectedSource == "FILE") FileBlue else if (selectedSource == "UNIVERSAL" || isAlbum) activeBrandColor else Color.Unspecified,
                    modifier = Modifier.size(22.dp),
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            inputText = ""
                            errorMessage = null
                            selectedSource = "UNIVERSAL"
                            isAlbum = false
                            spotifyProfilePlaylists = emptyList()
                            spotifyProfileUsername = null
                            spotifyProfileAvatar = null
                            playlistCoverUrl = null
                            parsedTracks = emptyList()
                            directSongs = null
                        }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { resolveInput(inputText) }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(activeBrandColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Load",
                                    tint = if (selectedSource == "SPOTIFY") Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        // Direct 1-tap paste icon button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                if (clip.isNotBlank()) {
                                    inputText = clip
                                    resolveInput(clip)
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(activeBrandColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { resolveInput(inputText) }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeBrandColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Helpful 1-tap quick action chip below input when empty
        if (inputText.isBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(activeBrandColor.copy(alpha = 0.12f))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (clip.isNotBlank()) {
                                inputText = clip
                                resolveInput(clip)
                            } else {
                                Toast.makeText(context, "No link copied on clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = null,
                        tint = activeBrandColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Paste link from clipboard",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = activeBrandColor,
                    )
                }

                Text(
                    text = "Auto-detects format",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---- Spotify Profile Multi-Playlist Section ----
        if (spotifyProfilePlaylists.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Profile Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!spotifyProfileAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = spotifyProfileAvatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${spotifyProfileUsername ?: "Spotify User"}'s Playlists",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${spotifyProfilePlaylists.size} public playlists found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Select All Toggle
                        val allSelected = selectedPlaylistIds.size == spotifyProfilePlaylists.size && spotifyProfilePlaylists.isNotEmpty()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyGreen.copy(alpha = 0.15f))
                                .clickable {
                                    selectedPlaylistIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        spotifyProfilePlaylists.map { it.id }.toSet()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SpotifyGreen,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Batch Import Action Buttons (Now & Background)
                    AnimatedVisibility(visible = selectedPlaylistIds.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        val toImport = spotifyProfilePlaylists.filter { it.id in selectedPlaylistIds }
                                        scope.launch {
                                            isResolving = true
                                            isBatchImporting = true
                                            showProgressPercent = true
                                            val userPrefix = spotifyProfileUsername ?: "Spotify"
                                            for ((idx, pl) in toImport.withIndex()) {
                                                progressFraction = (idx.toFloat() / toImport.size)
                                                progressText = "Importing (${idx + 1}/${toImport.size}): ${pl.title}..."
                                                val result = PlaylistManager.fetchSpotifyPlaylist(pl.id)
                                                if (result != null && result.second.isNotEmpty()) {
                                                    val songs = PlaylistManager.resolveTracksToSongs(result.second) { cur, tot, songTitle ->
                                                        progressFraction = ((idx + (cur.toFloat() / tot)) / toImport.size)
                                                        progressText = "Matching (${idx + 1}/${toImport.size}) [${cur}/$tot]..."
                                                        currentResolvingSong = songTitle
                                                    }
                                                    val finalTitle = "$userPrefix • ${result.first}"
                                                    onImportSuccess(finalTitle, songs, "SPOTIFY_PROFILE", userPrefix, spotifyProfileAvatar, pl.imageUrl, false)
                                                }
                                            }
                                            isBatchImporting = false
                                            isResolving = false
                                            importedSuccessResult = Triple(
                                                "${toImport.size} Spotify Playlists",
                                                emptyList(),
                                                "SPOTIFY_PROFILE",
                                            )
                                        }
                                    },
                                    enabled = !isResolving,
                                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileDownload,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Import (${selectedPlaylistIds.size})",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.Black,
                                        maxLines = 1,
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val toImport = spotifyProfilePlaylists.filter { it.id in selectedPlaylistIds }
                                        PlaylistImportManager.startBatchProfileImport(
                                            context = context,
                                            username = spotifyProfileUsername ?: "Spotify",
                                            playlists = toImport,
                                            authorAvatarUrl = spotifyProfileAvatar,
                                        )
                                        onDismiss()
                                    },
                                    enabled = !isResolving,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, SpotifyGreen),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Background",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = SpotifyGreen,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    // List of Playlists
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        spotifyProfilePlaylists.forEach { pl ->
                            val isChecked = pl.id in selectedPlaylistIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isChecked) SpotifyGreen.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    )
                                    .border(
                                        1.dp,
                                        if (isChecked) SpotifyGreen.copy(alpha = 0.4f) else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        resolveInput("https://open.spotify.com/playlist/${pl.id}")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedPlaylistIds = if (checked) {
                                            selectedPlaylistIds + pl.id
                                        } else {
                                            selectedPlaylistIds - pl.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SpotifyGreen,
                                        checkmarkColor = Color.Black,
                                    ),
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                if (!pl.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = pl.imageUrl,
                                        contentDescription = pl.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SpotifyGreen.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null,
                                            tint = SpotifyGreen,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                val existingInLib = remember(pl.title, pl.id) {
                                    AppSettings.findDuplicatePlaylist(
                                        title = pl.title,
                                        author = spotifyProfileUsername,
                                        sourceId = pl.id,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (existingInLib != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = SpotifyGreen,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "In Library (${existingInLib.songs.size} songs)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = SpotifyGreen,
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Tap to load preview",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Load",
                                    tint = SpotifyGreen,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Resolved Playlist / Track Hero Preview Card ----
        if (totalTracks > 0) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Hero Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Thumbnail with platform watermark badge
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(activeBrandColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val displayThumb = playlistCoverUrl ?: firstSongThumbnail
                            if (!displayThumb.isNullOrBlank()) {
                                AsyncImage(
                                    model = displayThumb,
                                    contentDescription = playlistTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(74.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            // Platform Mini-Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = when (selectedSource) {
                                        "SPOTIFY" -> DhvaniIcons.Spotify
                                        "FILE" -> Icons.Rounded.FolderOpen
                                        else -> DhvaniIcons.YouTube
                                    },
                                    contentDescription = null,
                                    tint = if (selectedSource == "FILE") FileBlue else Color.Unspecified,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        // Playlist Metadata & Title Editor
                        Column(modifier = Modifier.weight(1f)) {
                            // Source & Track Count Badges
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(activeBrandColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = when (selectedSource) {
                                            "SPOTIFY" -> "SPOTIFY"
                                            "FILE" -> "LOCAL FILE"
                                            else -> "YOUTUBE"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = activeBrandColor,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "$totalTracks ${if (totalTracks == 1) "track" else "tracks"}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isAlbum) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(activeBrandColor.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Album,
                                                contentDescription = null,
                                                tint = activeBrandColor,
                                                modifier = Modifier.size(11.dp),
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "ALBUM",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp,
                                                ),
                                                color = activeBrandColor,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Editable Title Field
                            OutlinedTextField(
                                value = playlistTitle,
                                onValueChange = { playlistTitle = it },
                                label = { Text(if (isAlbum) "Album Name" else "Playlist Name") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit name",
                                        tint = activeBrandColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeBrandColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(10.dp))

                    // Tracklist Preview Accordion
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Tracks Preview",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Showing up to 6 songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Track Item Rows
                    val previewItems: List<Pair<String, String>> = if (directSongs != null) {
                        directSongs!!.take(6).map { it.title to it.artist }
                    } else {
                        parsedTracks.take(6).map { it.title to it.artist }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        previewItems.forEachIndexed { index, (title, artist) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = activeBrandColor,
                                    modifier = Modifier.width(22.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (artist.isNotBlank()) {
                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = activeBrandColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        if (totalTracks > 6) {
                            Text(
                                text = "+ ${totalTracks - 6} more tracks will be imported",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Bottom Action Buttons ----
        if (!isResolving) {
            val isSingleSong = totalTracks == 1 && directSongs?.isNotEmpty() == true
            if (isSingleSong && onPlayNow != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val song = directSongs!!.first()
                            onPlayNow(song)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Play Now", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val finalTitle = playlistTitle.trim().ifBlank { "Imported Track" }
                            onImportSuccess(finalTitle, directSongs!!, selectedSource, null, null, playlistCoverUrl, false)
                            importedSuccessResult = Triple(finalTitle, directSongs!!, selectedSource)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeBrandColor,
                            contentColor = if (selectedSource == "SPOTIFY") Color.Black else Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Save Track", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (parsedTracks.isNotEmpty()) {
                val existingMatch = remember(playlistTitle) {
                    if (playlistTitle.isNotBlank()) AppSettings.findDuplicatePlaylist(playlistTitle) else null
                }

                if (existingMatch != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = activeBrandColor.copy(alpha = 0.12f),
                        ),
                        border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = activeBrandColor,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Already in Library (${existingMatch.songs.size} songs)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val diff = totalTracks - existingMatch.songs.size
                                Text(
                                    text = if (diff > 0) "+$diff new songs found! Importing will update without duplicate."
                                    else "Importing will refresh tracks safely without duplicating.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val effectiveSource = if (selectedSource == "UNIVERSAL") "SPOTIFY" else selectedSource
                            val finalTitle = playlistTitle.trim().ifBlank {
                                if (isAlbum) (if (effectiveSource == "SPOTIFY") "Spotify Album" else "Album")
                                else if (effectiveSource == "SPOTIFY") "Spotify Playlist" else "Imported Playlist"
                            }
                            PlaylistImportManager.startSinglePlaylistImport(
                                context = context,
                                playlistTitle = finalTitle,
                                tracks = parsedTracks,
                                source = effectiveSource,
                                coverUrl = playlistCoverUrl,
                                isAlbum = isAlbum,
                            )
                            Toast.makeText(context, "Updating \"$finalTitle\" in background...", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(0.42f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = activeBrandColor,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (existingMatch != null) "Bg Update" else "Background",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }

                    Button(
                        onClick = {
                            val effectiveSource = if (selectedSource == "UNIVERSAL") "SPOTIFY" else selectedSource
                            val finalTitle = playlistTitle.trim().ifBlank {
                                if (isAlbum) (if (effectiveSource == "SPOTIFY") "Spotify Album" else "Album")
                                else if (effectiveSource == "SPOTIFY") "Spotify Playlist" else "Imported Playlist"
                            }
                            scope.launch {
                                isResolving = true
                                showProgressPercent = true
                                progressFraction = 0f
                                progressText = "Matching tracks (0/${parsedTracks.size})..."
                                currentResolvingSong = ""
                                val songs = PlaylistManager.resolveTracksToSongs(parsedTracks) { current, total, songTitle ->
                                    progressFraction = current.toFloat() / total.coerceAtLeast(1)
                                    progressText = "Matching audio ($current/$total)..."
                                    currentResolvingSong = songTitle
                                }
                                isResolving = false
                                onImportSuccess(finalTitle, songs, effectiveSource, null, null, playlistCoverUrl, isAlbum)
                                importedSuccessResult = Triple(finalTitle, songs, effectiveSource)
                            }
                        },
                        modifier = Modifier
                            .weight(0.58f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeBrandColor,
                            contentColor = if (selectedSource == "SPOTIFY") Color.Black else Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = when (selectedSource) {
                                "SPOTIFY" -> DhvaniIcons.Spotify
                                "FILE" -> Icons.Rounded.FileDownload
                                else -> DhvaniIcons.YouTube
                            },
                            contentDescription = null,
                            tint = if (selectedSource == "FILE") Color.White else Color.Unspecified,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (existingMatch != null) "Update ($totalTracks)" else "Import ($totalTracks)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        val effectiveSource = if (selectedSource == "UNIVERSAL") "YOUTUBE" else selectedSource
                        val finalTitle = playlistTitle.trim().ifBlank {
                            if (isAlbum) (if (effectiveSource == "SPOTIFY") "Spotify Album" else "Album")
                            else if (effectiveSource == "SPOTIFY") "Spotify Playlist" else "Imported Playlist"
                        }
                        if (directSongs != null) {
                            onImportSuccess(finalTitle, directSongs!!, effectiveSource, null, null, playlistCoverUrl, isAlbum)
                            importedSuccessResult = Triple(finalTitle, directSongs!!, effectiveSource)
                        } else {
                            Toast.makeText(context, "Please paste a playlist link or choose a file first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = totalTracks > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeBrandColor,
                        contentColor = if (selectedSource == "SPOTIFY") Color.Black else Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(
                        imageVector = when (selectedSource) {
                            "SPOTIFY" -> DhvaniIcons.Spotify
                            "FILE" -> Icons.Rounded.FileDownload
                            else -> DhvaniIcons.YouTube
                        },
                        contentDescription = null,
                        tint = if (selectedSource == "FILE") Color.White else Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (totalTracks > 0) {
                            if (isAlbum) {
                                "Import $totalTracks Album Tracks"
                            } else when (selectedSource) {
                                "SPOTIFY" -> "Match & Import $totalTracks Spotify Songs"
                                "FILE" -> "Import $totalTracks Tracks to Library"
                                else -> "Import $totalTracks Songs to Library"
                            }
                        } else {
                            if (isAlbum) {
                                "Import Album"
                            } else when (selectedSource) {
                                "SPOTIFY" -> "Import Spotify Playlist"
                                "FILE" -> "Choose File to Import"
                                else -> "Import Playlist"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
