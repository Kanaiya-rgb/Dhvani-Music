package com.music.flux.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.music.flux.data.model.Song
import com.music.flux.data.playlist.PlaylistManager
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistSheet(
    onImportSuccess: (String, List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playlistTitle by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var parsedTracks by remember { mutableStateOf<List<PlaylistManager.ImportedTrack>>(emptyList()) }
    var directSongs by remember { mutableStateOf<List<Song>?>(null) }
    var isResolving by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }

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
                if (parsed.tracks.isEmpty()) {
                    Toast.makeText(context, "No tracks found in file", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Found ${parsed.tracks.size} tracks", Toast.LENGTH_SHORT).show()
                }
            }
        }.onFailure {
            Toast.makeText(context, "Failed to read file: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                    text = "Import from M3U / JSON files, or YouTube links",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
                    imageVector = Icons.Rounded.FileUpload,
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
                inputText = input
                val plId = PlaylistManager.extractPlaylistId(input)
                if (plId != null) {
                    // It's a YouTube link
                    scope.launch {
                        isResolving = true
                        progressText = "Loading YouTube playlist..."
                        val result = PlaylistManager.fetchYoutubePlaylist(plId)
                        isResolving = false
                        if (result != null) {
                            playlistTitle = result.first
                            directSongs = result.second
                            parsedTracks = emptyList()
                            Toast.makeText(context, "Loaded \"${result.first}\" (${result.second.size} tracks)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not load playlist from link", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (input.contains("#EXTINF") || input.contains("{")) {
                    val parsed = PlaylistManager.parseText(input, "Imported Playlist")
                    if (parsed.tracks.isNotEmpty()) {
                        playlistTitle = parsed.title
                        parsedTracks = parsed.tracks
                        directSongs = null
                    }
                }
            },
            placeholder = { Text("Paste YouTube playlist link or M3U text") },
            trailingIcon = {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    if (clip.isNotBlank()) {
                        inputText = clip
                        val plId = PlaylistManager.extractPlaylistId(clip)
                        if (plId != null) {
                            scope.launch {
                                isResolving = true
                                progressText = "Loading YouTube playlist..."
                                val result = PlaylistManager.fetchYoutubePlaylist(plId)
                                isResolving = false
                                if (result != null) {
                                    playlistTitle = result.first
                                    directSongs = result.second
                                    parsedTracks = emptyList()
                                    Toast.makeText(context, "Loaded \"${result.first}\" (${result.second.size} tracks)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val parsed = PlaylistManager.parseText(clip, "Imported Playlist")
                            if (parsed.tracks.isNotEmpty()) {
                                playlistTitle = parsed.title
                                parsedTracks = parsed.tracks
                                directSongs = null
                            }
                        }
                    }
                }) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

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
            Button(
                onClick = {
                    val finalTitle = playlistTitle.trim().ifBlank { "Imported Playlist" }
                    if (directSongs != null) {
                        onImportSuccess(finalTitle, directSongs!!)
                        onDismiss()
                    } else if (parsedTracks.isNotEmpty()) {
                        scope.launch {
                            isResolving = true
                            progressText = "Resolving tracks (0/${parsedTracks.size})..."
                            val songs = PlaylistManager.resolveTracksToSongs(parsedTracks) { current, total ->
                                progressText = "Resolving tracks ($current/$total)..."
                            }
                            isResolving = false
                            onImportSuccess(finalTitle, songs)
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

        Spacer(Modifier.height(16.dp))
    }
}
