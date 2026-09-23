package com.music.dhvani.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.playlist.PlaylistManager
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistSheet(
    onImportSuccess: (String, List<Song>, String) -> Unit,
    onDismiss: () -> Unit,
    initialSource: String? = null,
    initialUrl: String? = null,
    modifier: Modifier = Modifier,
    onPlayNow: ((Song) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playlistTitle by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf(initialUrl.orEmpty()) }
    var parsedTracks by remember { mutableStateOf<List<PlaylistManager.ImportedTrack>>(emptyList()) }
    var directSongs by remember { mutableStateOf<List<Song>?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSource by remember(initialSource) { mutableStateOf(initialSource ?: "YOUTUBE") }

    var spotifyProfilePlaylists by remember { mutableStateOf<List<PlaylistManager.SpotifyProfilePlaylist>>(emptyList()) }
    var spotifyProfileUsername by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isBatchImporting by remember { mutableStateOf(false) }

    val resolveInput: (String) -> Unit = { text ->
        val trimmed = text.trim()
        errorMessage = null
        val spotifyUserId = PlaylistManager.extractSpotifyUserId(trimmed)
        val spotifyPlId = PlaylistManager.extractSpotifyPlaylistId(trimmed)
        val plId = PlaylistManager.extractPlaylistId(trimmed)
        val videoId = PlaylistManager.extractVideoId(trimmed)

        if (spotifyUserId != null) {
            selectedSource = "SPOTIFY"
            scope.launch {
                isResolving = true
                progressText = "Loading Spotify profile & public playlists..."
                val profileResult = PlaylistManager.fetchSpotifyUserPlaylists(spotifyUserId)
                isResolving = false
                if (profileResult != null && profileResult.playlists.isNotEmpty()) {
                    spotifyProfileUsername = profileResult.username
                    spotifyProfilePlaylists = profileResult.playlists
                    parsedTracks = emptyList()
                    directSongs = null
                    Toast.makeText(context, "Found ${profileResult.playlists.size} playlists from ${profileResult.username}", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Could not find public playlists on this Spotify profile. Please ensure the profile has public playlists."
                    Toast.makeText(context, "No public playlists found", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (spotifyPlId != null) {
            spotifyProfilePlaylists = emptyList()
            spotifyProfileUsername = null
            selectedSource = "SPOTIFY"
            scope.launch {
                isResolving = true
                progressText = "Loading Spotify playlist..."
                val result = PlaylistManager.fetchSpotifyPlaylist(spotifyPlId)
                isResolving = false
                if (result != null && result.second.isNotEmpty()) {
                    playlistTitle = result.first
                    parsedTracks = result.second
                    directSongs = null
                    Toast.makeText(context, "Loaded \"${result.first}\" (${result.second.size} Spotify tracks)", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Could not load Spotify playlist. Please ensure it is a public playlist."
                    Toast.makeText(context, "Could not load Spotify playlist", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (plId != null) {
            spotifyProfilePlaylists = emptyList()
            spotifyProfileUsername = null
            selectedSource = "YOUTUBE"
            scope.launch {
                isResolving = true
                progressText = "Loading YouTube playlist..."
                val result = PlaylistManager.fetchYoutubePlaylist(plId)
                isResolving = false
                if (result != null && result.second.isNotEmpty()) {
                    playlistTitle = result.first
                    directSongs = result.second
                    parsedTracks = emptyList()
                    Toast.makeText(context, "Loaded \"${result.first}\" (${result.second.size} tracks)", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Could not load playlist. Please ensure the playlist is public or unlisted."
                    Toast.makeText(context, "Could not load playlist from link", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (videoId != null) {
            selectedSource = "YOUTUBE"
            scope.launch {
                isResolving = true
                progressText = "Loading YouTube song..."
                val song = PlaylistManager.fetchYoutubeSingleVideo(videoId)
                isResolving = false
                if (song != null) {
                    playlistTitle = song.title.ifBlank { "Imported Track" }
                    directSongs = listOf(song)
                    parsedTracks = emptyList()
                    Toast.makeText(context, "Found song: \"${song.title}\"", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Could not load song details from link."
                }
            }
        } else if (trimmed.contains("#EXTINF") || trimmed.contains("{")) {
            val parsed = PlaylistManager.parseText(trimmed, "Imported Playlist")
            if (parsed.tracks.isNotEmpty()) {
                playlistTitle = parsed.title
                parsedTracks = parsed.tracks
                directSongs = null
                Toast.makeText(context, "Found ${parsed.tracks.size} tracks", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "No valid tracks found in pasted text."
            }
        } else if (trimmed.isNotBlank()) {
            errorMessage = "Invalid link. Please paste a valid YouTube song, playlist, or Spotify URL."
        }
    }

    androidx.compose.runtime.LaunchedEffect(initialUrl) {
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
                if (parsed.tracks.isEmpty()) {
                    errorMessage = "No tracks found in selected file."
                    Toast.makeText(context, "No tracks found in file", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Found ${parsed.tracks.size} tracks", Toast.LENGTH_SHORT).show()
                }
            }
        }.onFailure {
            errorMessage = "Failed to read file: ${it.message}"
            Toast.makeText(context, "Failed to read file: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Import Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Import from M3U / JSON files, YouTube, or Spotify links",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Source selector pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                "YOUTUBE" to "YouTube Playlist",
                "SPOTIFY" to "Spotify Playlist",
            ).forEach { (src, label) ->
                val isSelected = selectedSource == src
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        )
                        .clickable { selectedSource = src }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // File picker row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .clickable { filePickerLauncher.launch("*/*") }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Choose Playlist File",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Select .m3u, .m3u8, or .json file from your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // URL or text input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { input ->
                val prev = inputText
                inputText = input
                errorMessage = null
                // Auto-resolve if pasted a full link or batch text
                if (input.length - prev.length > 8 || (input.startsWith("http") && (input.contains("list=") || input.contains("spotify.com")))) {
                    resolveInput(input)
                }
            },
            placeholder = { Text("Paste YouTube/Spotify link or User Profile link") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            inputText = ""
                            errorMessage = null
                            spotifyProfilePlaylists = emptyList()
                            spotifyProfileUsername = null
                            parsedTracks = emptyList()
                            directSongs = null
                        }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { resolveInput(inputText) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Load",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (clip.isNotBlank()) {
                                inputText = clip
                                resolveInput(clip)
                            }
                        }) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Spotify Profile Playlists Selection Section
        if (spotifyProfilePlaylists.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${spotifyProfileUsername ?: "User"}'s Playlists (${spotifyProfilePlaylists.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // Select All / Deselect All Toggle Button
                val allSelected = selectedPlaylistIds.size == spotifyProfilePlaylists.size && spotifyProfilePlaylists.isNotEmpty()
                Text(
                    text = if (allSelected) "Deselect All" else "Select All",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            selectedPlaylistIds = if (allSelected) {
                                emptySet()
                            } else {
                                spotifyProfilePlaylists.map { it.id }.toSet()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action row if playlists are selected
            AnimatedVisibility(visible = selectedPlaylistIds.isNotEmpty()) {
                Column {
                    Button(
                        onClick = {
                            val toImport = spotifyProfilePlaylists.filter { it.id in selectedPlaylistIds }
                            scope.launch {
                                isResolving = true
                                isBatchImporting = true
                                val userPrefix = spotifyProfileUsername ?: "Spotify"
                                for ((idx, pl) in toImport.withIndex()) {
                                    progressText = "Importing (${idx + 1}/${toImport.size}): ${pl.title}..."
                                    val result = PlaylistManager.fetchSpotifyPlaylist(pl.id)
                                    if (result != null && result.second.isNotEmpty()) {
                                        val songs = PlaylistManager.resolveTracksToSongs(result.second) { cur, tot ->
                                            progressText = "Resolving tracks ($cur/$tot) for ${pl.title}..."
                                        }
                                        val finalTitle = "${userPrefix} • ${result.first}"
                                        onImportSuccess(finalTitle, songs, "SPOTIFY_PROFILE")
                                    }
                                }
                                isBatchImporting = false
                                isResolving = false
                                Toast.makeText(context, "Successfully imported ${toImport.size} playlists!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        enabled = !isResolving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Import Selected (${selectedPlaylistIds.size} Playlists)",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Text(
                text = "Select playlists to batch import or tap individual to load:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                spotifyProfilePlaylists.forEach { pl ->
                    val isChecked = pl.id in selectedPlaylistIds
                    Card(
                        onClick = {
                            resolveInput("https://open.spotify.com/playlist/${pl.id}")
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
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
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            if (!pl.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = pl.imageUrl,
                                    contentDescription = pl.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Load",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        val totalTracks = directSongs?.size ?: parsedTracks.size
        if (totalTracks > 0) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = playlistTitle,
                onValueChange = { playlistTitle = it },
                label = { Text("Playlist Name") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$totalTracks songs ready to import",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isResolving) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = progressText.ifBlank { "Importing songs..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = "Play Now", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val finalTitle = playlistTitle.trim().ifBlank { "Imported Track" }
                            onImportSuccess(finalTitle, directSongs!!, selectedSource)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = "Save as Playlist", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        val finalTitle = playlistTitle.trim().ifBlank { "Imported Playlist" }
                        if (directSongs != null) {
                            onImportSuccess(finalTitle, directSongs!!, selectedSource)
                            onDismiss()
                        } else if (parsedTracks.isNotEmpty()) {
                            scope.launch {
                                isResolving = true
                                progressText = "Resolving tracks (0/${parsedTracks.size})..."
                                val songs = PlaylistManager.resolveTracksToSongs(parsedTracks) { current, total ->
                                    progressText = "Resolving tracks ($current/$total)..."
                                }
                                isResolving = false
                                onImportSuccess(finalTitle, songs, selectedSource)
                                onDismiss()
                            }
                        } else {
                            Toast.makeText(context, "Please pick a file or paste a playlist link first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = totalTracks > 0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (totalTracks > 0) "Import $totalTracks Songs" else "Import Playlist",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
