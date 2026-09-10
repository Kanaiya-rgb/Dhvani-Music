package com.music.flux.data.playlist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.music.flux.data.YtMusicRepository
import com.music.flux.data.model.SearchFilter
import com.music.flux.data.model.SearchResult
import com.music.flux.data.model.Song
import com.music.flux.data.model.durationMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object PlaylistManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    data class ImportedTrack(
        val videoId: String? = null,
        val title: String,
        val artist: String = "",
        val durationSeconds: Long = 0,
    )

    data class ParsedPlaylist(
        val title: String,
        val tracks: List<ImportedTrack>,
    )

    @Serializable
    private data class JsonPlaylist(
        val title: String,
        val app: String = "FluxMusic",
        val version: Int = 1,
        val tracks: List<JsonTrack>,
    )

    @Serializable
    private data class JsonTrack(
        val videoId: String,
        val title: String,
        val artist: String,
        val duration: String? = null,
        val thumbnailUrl: String? = null,
    )

    fun buildM3u(title: String, songs: List<Song>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:${title.trim()}")
        for (song in songs) {
            val seconds = song.durationMillis() / 1000
            val artist = song.artist.ifBlank { "Various Artists" }
            val songTitle = song.title.ifBlank { "Track" }
            sb.appendLine("#EXTINF:$seconds,$artist - $songTitle")
            if (song.videoId.isNotBlank()) {
                sb.appendLine("https://music.youtube.com/watch?v=${song.videoId}")
            } else if (!song.localUri.isNullOrBlank()) {
                sb.appendLine(song.localUri)
            } else {
                sb.appendLine("$artist - $songTitle")
            }
        }
        return sb.toString()
    }

    fun buildJson(title: String, songs: List<Song>): String {
        val payload = JsonPlaylist(
            title = title,
            tracks = songs.map { song ->
                JsonTrack(
                    videoId = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    duration = song.durationText,
                    thumbnailUrl = song.thumbnailUrl,
                )
            },
        )
        return json.encodeToString(payload)
    }

    fun exportAndShare(
        context: Context,
        title: String,
        songs: List<Song>,
        asJson: Boolean = false,
    ) {
        if (songs.isEmpty()) {
            Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val extension = if (asJson) "json" else "m3u8"
        val mimeType = if (asJson) "application/json" else "audio/x-mpegurl"
        val content = if (asJson) buildJson(title, songs) else buildM3u(title, songs)
        val safeName = sanitizeFileName(title.ifBlank { "Playlist" })

        try {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(sharedDir, "$safeName.$extension")
            file.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title.$extension")
                putExtra(Intent.EXTRA_TEXT, "Exported playlist: $title (${songs.size} tracks)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export \"$title\"")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun saveToDownloads(
        context: Context,
        title: String,
        songs: List<Song>,
        asJson: Boolean = false,
    ): File? {
        if (songs.isEmpty()) {
            Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return null
        }

        val extension = if (asJson) "json" else "m3u8"
        val content = if (asJson) buildJson(title, songs) else buildM3u(title, songs)
        val safeName = sanitizeFileName(title.ifBlank { "Playlist" })

        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fluxFolder = File(downloadsDir, "FluxMusic").apply { mkdirs() }
            val targetFile = File(fluxFolder, "$safeName.$extension")
            targetFile.writeText(content, Charsets.UTF_8)
            Toast.makeText(context, "Saved to Downloads/FluxMusic/${targetFile.name}", Toast.LENGTH_LONG).show()
            targetFile
        } catch (e: Exception) {
            val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$safeName.$extension")
            fallback.writeText(content, Charsets.UTF_8)
            Toast.makeText(context, "Saved to ${fallback.name}", Toast.LENGTH_LONG).show()
            fallback
        }
    }

    fun parseText(text: String, fallbackTitle: String = "Imported Playlist"): ParsedPlaylist {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            runCatching {
                val element = json.parseToJsonElement(trimmed) as? JsonObject
                val title = element?.get("title")?.jsonPrimitive?.content ?: fallbackTitle
                val tracksArray = element?.get("tracks")?.jsonArray
                if (tracksArray != null) {
                    val tracks = tracksArray.mapNotNull { item ->
                        val obj = item.jsonObject
                        val videoId = obj["videoId"]?.jsonPrimitive?.content
                        val songTitle = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val artist = obj["artist"]?.jsonPrimitive?.content.orEmpty()
                        ImportedTrack(videoId = videoId, title = songTitle, artist = artist)
                    }
                    if (tracks.isNotEmpty()) return ParsedPlaylist(title, tracks)
                }
            }
        }

        var detectedTitle = fallbackTitle
        val tracks = mutableListOf<ImportedTrack>()
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }

        var currentExtInfTitle = ""
        var currentExtInfArtist = ""
        var currentDuration: Long = 0

        for (line in lines) {
            if (line.startsWith("#PLAYLIST:", ignoreCase = true)) {
                detectedTitle = line.substringAfter(":").trim().ifBlank { detectedTitle }
                continue
            }
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val rawMeta = line.substringAfter(":")
                val durationPart = rawMeta.substringBefore(",").trim().toLongOrNull() ?: 0L
                currentDuration = durationPart
                val titlePart = rawMeta.substringAfter(",").trim()
                if (titlePart.contains(" - ")) {
                    currentExtInfArtist = titlePart.substringBefore(" - ").trim()
                    currentExtInfTitle = titlePart.substringAfter(" - ").trim()
                } else {
                    currentExtInfTitle = titlePart
                    currentExtInfArtist = ""
                }
                continue
            }
            if (line.startsWith("#")) continue

            // Line is a link, videoId, or song entry
            val videoId = extractVideoId(line)
            if (videoId != null) {
                tracks.add(
                    ImportedTrack(
                        videoId = videoId,
                        title = currentExtInfTitle.ifBlank { "Track ${tracks.size + 1}" },
                        artist = currentExtInfArtist,
                        durationSeconds = currentDuration,
                    ),
                )
            } else if (line.contains(" - ")) {
                val artist = line.substringBefore(" - ").trim()
                val songTitle = line.substringAfter(" - ").trim()
                tracks.add(ImportedTrack(videoId = null, title = songTitle, artist = artist))
            } else if (line.isNotBlank()) {
                tracks.add(ImportedTrack(videoId = null, title = line, artist = currentExtInfArtist))
            }

            currentExtInfTitle = ""
            currentExtInfArtist = ""
            currentDuration = 0
        }

        return ParsedPlaylist(detectedTitle, tracks)
    }

    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("PL") || trimmed.startsWith("VLPL") || trimmed.startsWith("RDAMPL")) {
            return trimmed.removePrefix("VL")
        }
        val match = Regex("""[?&]list=([a-zA-Z0-9_-]+)""").find(trimmed)
        return match?.groupValues?.get(1)
    }

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("""[a-zA-Z0-9_-]{11}"""))) {
            return trimmed
        }
        val ytMatch = Regex("""(?:v=|youtu\.be/|embed/|shorts/|watch\?v=)([a-zA-Z0-9_-]{11})""").find(trimmed)
        return ytMatch?.groupValues?.get(1)
    }

    suspend fun resolveTracksToSongs(
        tracks: List<ImportedTrack>,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): List<Song> = withContext(Dispatchers.IO) {
        val resolved = mutableListOf<Song>()
        tracks.forEachIndexed { index, track ->
            onProgress(index + 1, tracks.size)
            if (!track.videoId.isNullOrBlank()) {
                resolved.add(
                    Song(
                        videoId = track.videoId,
                        title = track.title,
                        artist = track.artist.ifBlank { "Various Artists" },
                        thumbnailUrl = "https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg",
                    ),
                )
            } else {
                val query = listOf(track.title, track.artist).filter { it.isNotBlank() }.joinToString(" ")
                val searchHit = runCatching {
                    YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
                }.getOrNull()?.filterIsInstance<SearchResult.Track>()?.firstOrNull()?.song

                if (searchHit != null) {
                    resolved.add(searchHit)
                } else {
                    resolved.add(
                        Song(
                            videoId = "",
                            title = track.title,
                            artist = track.artist,
                            thumbnailUrl = null,
                        ),
                    )
                }
            }
        }
        resolved
    }

    suspend fun fetchYoutubePlaylist(
        playlistId: String,
    ): Pair<String, List<Song>>? = withContext(Dispatchers.IO) {
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        val result = YtMusicRepository.browseSongs(browseId).getOrNull() ?: return@withContext null
        val title = result.header?.title ?: "Imported Playlist"
        title to result.songs
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").replace(Regex("""\s+"""), "_").trim('_')
    }
}
