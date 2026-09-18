package com.music.dhvani.data.playlist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.music.dhvani.data.YtMusicRepository
import com.music.dhvani.data.model.SearchFilter
import com.music.dhvani.data.model.SearchResult
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.durationMillis
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
import java.net.HttpURLConnection
import java.net.URL

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
        val app: String = "DhvaniMusic",
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
            val dhvaniFolder = File(downloadsDir, "DhvaniMusic").apply { mkdirs() }
            val targetFile = File(dhvaniFolder, "$safeName.$extension")
            targetFile.writeText(content, Charsets.UTF_8)
            Toast.makeText(context, "Saved to Downloads/DhvaniMusic/${targetFile.name}", Toast.LENGTH_LONG).show()
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
        if (trimmed.isBlank()) return null
        val listMatch = Regex("""[?&]list=([a-zA-Z0-9_-]+)""").find(trimmed)
        if (listMatch != null) {
            return listMatch.groupValues[1].removePrefix("VL")
        }
        val browseMatch = Regex("""/browse/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (browseMatch != null) {
            return browseMatch.groupValues[1].removePrefix("VL")
        }
        if (trimmed.startsWith("VL") && trimmed.length > 4) {
            return trimmed.removePrefix("VL")
        }
        if (trimmed.startsWith("PL") || trimmed.startsWith("RD") || trimmed.startsWith("OLAK5uy") || trimmed.startsWith("MPREb_")) {
            return trimmed
        }
        return null
    }

    fun extractSpotifyPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val urlMatch = Regex("""open\.spotify\.com/(?:intl-[a-zA-Z]{2}/)?playlist/([a-zA-Z0-9]+)""").find(trimmed)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }
        val uriMatch = Regex("""spotify:playlist:([a-zA-Z0-9]+)""").find(trimmed)
        if (uriMatch != null) {
            return uriMatch.groupValues[1]
        }
        if (trimmed.matches(Regex("""^[a-zA-Z0-9]{22}$"""))) {
            return trimmed
        }
        return null
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
        val cleanId = playlistId.removePrefix("VL")
        val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"

        // 1. Try browse with VL prefix
        var songPage = YtMusicRepository.browseSongs(browseId).getOrNull()

        // 2. Fallback to browse without VL prefix if empty
        if (songPage == null || songPage.songs.isEmpty()) {
            val directPage = YtMusicRepository.browseSongs(cleanId).getOrNull()
            if (directPage != null && directPage.songs.isNotEmpty()) {
                songPage = directPage
            }
        }

        // 3. Fetch full songs if multiple pages exist or fallback to allSongs
        val songs = if (songPage != null && songPage.songs.isNotEmpty()) {
            if (songPage.continuation != null) {
                YtMusicRepository.allSongs(browseId).getOrNull()
                    ?: YtMusicRepository.allSongs(cleanId).getOrNull()
                    ?: songPage.songs
            } else {
                songPage.songs
            }
        } else {
            YtMusicRepository.allSongs(browseId).getOrNull()
                ?: YtMusicRepository.allSongs(cleanId).getOrNull()
                ?: emptyList()
        }

        if (songs.isEmpty()) return@withContext null
        val title = songPage?.header?.title?.ifBlank { "YouTube Playlist" } ?: "YouTube Playlist"
        title to songs
    }

    suspend fun fetchYoutubeSingleVideo(
        videoId: String,
    ): Song? = withContext(Dispatchers.IO) {
        val hit = runCatching {
            YtMusicRepository.trackLinks(videoId).getOrNull()
        }.getOrNull()
        if (hit != null && hit.title.isNotBlank()) {
            return@withContext hit.copy(
                thumbnailUrl = hit.thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            )
        }
        // Fallback: search by videoId
        val searchHit = runCatching {
            YtMusicRepository.search(videoId, SearchFilter.SONGS).getOrNull()
        }.getOrNull()?.filterIsInstance<SearchResult.Track>()?.firstOrNull()?.song
        if (searchHit != null) {
            return@withContext searchHit
        }
        // Final fallback with videoId
        Song(
            videoId = videoId,
            title = "YouTube Video",
            artist = "YouTube",
            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        )
    }

    suspend fun fetchSpotifyPlaylist(
        playlistId: String,
    ): Pair<String, List<ImportedTrack>>? = withContext(Dispatchers.IO) {
        val cleanId = playlistId.trim().removePrefix("spotify:playlist:")
        val url = "https://open.spotify.com/embed/playlist/$cleanId"
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val nextDataIdx = html.indexOf("__NEXT_DATA__")
            if (nextDataIdx == -1) return@withContext null
            val start = html.indexOf('>', nextDataIdx) + 1
            val end = html.indexOf("</script>", start)
            if (start <= 0 || end <= start) return@withContext null

            val jsonRaw = html.substring(start, end)
            val root = json.parseToJsonElement(jsonRaw).jsonObject
            val props = root["props"]?.jsonObject ?: return@withContext null
            val pageProps = props["pageProps"]?.jsonObject ?: return@withContext null
            val state = pageProps["state"]?.jsonObject ?: return@withContext null
            val data = state["data"]?.jsonObject ?: return@withContext null
            val entity = data["entity"]?.jsonObject ?: return@withContext null

            val title = entity["title"]?.jsonPrimitive?.content
                ?: entity["name"]?.jsonPrimitive?.content
                ?: "Spotify Playlist"

            val trackList = entity["trackList"]?.jsonArray.orEmpty()
            val tracks = trackList.mapNotNull { item ->
                val trackObj = item.jsonObject
                val trackTitle = trackObj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val artist = trackObj["subtitle"]?.jsonPrimitive?.content.orEmpty()
                val durationMs = trackObj["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                ImportedTrack(
                    videoId = null,
                    title = trackTitle,
                    artist = artist,
                    durationSeconds = durationMs / 1000,
                )
            }

            if (tracks.isEmpty()) null else title to tracks
        } catch (e: Exception) {
            null
        }
    }

    fun buildSpotifyExportText(title: String, songs: List<Song>): String {
        val sb = StringBuilder()
        sb.appendLine("# Playlist: $title")
        sb.appendLine("# Total Tracks: ${songs.size}")
        sb.appendLine("# Format: Title - Artist")
        sb.appendLine()
        songs.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. ${s.title} - ${s.artist}")
        }
        return sb.toString()
    }

    fun exportAndShareSpotifyText(context: Context, title: String, songs: List<Song>) {
        if (songs.isEmpty()) {
            Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val safeName = sanitizeFileName(title.ifBlank { "Playlist" })
        try {
            val content = buildSpotifyExportText(title, songs)
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(sharedDir, "${safeName}_spotify_export.txt")
            file.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title - Spotify Tracklist")
                putExtra(Intent.EXTRA_TEXT, "Tracklist for Spotify: $title (${songs.size} tracks)\n\n$content")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export tracklist for Spotify")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").replace(Regex("""\s+"""), "_").trim('_')
    }
}
