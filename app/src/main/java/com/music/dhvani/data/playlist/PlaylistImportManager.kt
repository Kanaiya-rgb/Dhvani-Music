package com.music.dhvani.data.playlist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.music.dhvani.R
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles background importing of playlists and multi-playlist Spotify profiles.
 * Allows users to navigate freely, play songs, or exit to other apps while import
 * runs reliably with live notifications and progress state.
 */
object PlaylistImportManager {
    private const val CHANNEL_ID = "dhvani_playlist_import_channel"
    private const val NOTIFICATION_ID = 2026101

    data class ImportStatus(
        val isImporting: Boolean = false,
        val title: String = "",
        val currentSong: String = "",
        val currentTrack: Int = 0,
        val totalTracks: Int = 0,
        val progressFraction: Float = 0f,
        val isBatch: Boolean = false,
        val currentBatchIndex: Int = 0,
        val totalBatchCount: Int = 0,
        val completedTitle: String? = null,
        val error: String? = null,
    )

    private val _status = MutableStateFlow(ImportStatus())
    val status = _status.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playlist Import",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress of background playlist imports"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun postNotification(
        context: Context,
        title: String,
        content: String,
        progress: Int = 0,
        max: Int = 0,
        indeterminate: Boolean = false,
        ongoing: Boolean = true,
    ) {
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (max > 0 || indeterminate) {
            builder.setProgress(max, progress, indeterminate)
        }

        runCatching {
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun clearNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            manager.cancel(NOTIFICATION_ID)
        }
    }

    fun cancel(context: Context? = null) {
        currentJob?.cancel()
        currentJob = null
        _status.value = ImportStatus()
        context?.let { clearNotification(it) }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Start background import of a single playlist from parsed tracks.
     */
    fun startSinglePlaylistImport(
        context: Context,
        playlistTitle: String,
        tracks: List<PlaylistManager.ImportedTrack>,
        source: String = "SPOTIFY",
        author: String? = null,
        authorAvatarUrl: String? = null,
        coverUrl: String? = null,
        isAlbum: Boolean = false,
        onFinished: (() -> Unit)? = null,
    ) {
        currentJob?.cancel()
        currentJob = managerScope.launch {
            val total = tracks.size
            _status.value = ImportStatus(
                isImporting = true,
                title = playlistTitle,
                totalTracks = total,
                isBatch = false,
            )

            postNotification(
                context = context,
                title = "Importing $playlistTitle",
                content = "Starting track matching (0/$total)...",
                progress = 0,
                max = total,
                ongoing = true,
            )

            val songs = PlaylistManager.resolveTracksToSongs(tracks) { current, count, songTitle ->
                val fraction = current.toFloat() / count.coerceAtLeast(1)
                _status.value = _status.value.copy(
                    currentSong = songTitle,
                    currentTrack = current,
                    totalTracks = count,
                    progressFraction = fraction,
                )
                postNotification(
                    context = context,
                    title = "Importing $playlistTitle (${(fraction * 100).toInt()}%)",
                    content = "Matching ($current/$count): $songTitle",
                    progress = current,
                    max = count,
                    ongoing = true,
                )
            }

            if (songs.isNotEmpty()) {
                val finalTitle = if (author != null && !playlistTitle.contains(" • ")) {
                    "$author • $playlistTitle"
                } else {
                    playlistTitle
                }
                val savedId = AppSettings.saveLocalPlaylist(
                    title = finalTitle,
                    songs = songs,
                    source = source,
                    author = author,
                    authorAvatarUrl = authorAvatarUrl,
                    coverUrl = coverUrl,
                    isAlbum = isAlbum,
                )

                PlaylistSyncManager.trackPlaylist(
                    context = context,
                    playlistId = finalTitle,
                    source = source,
                    title = playlistTitle,
                    author = author,
                    localPlaylistId = savedId,
                    trackCount = songs.size,
                    trackTitles = songs.map { it.title },
                )

                _status.value = ImportStatus(
                    isImporting = false,
                    completedTitle = playlistTitle,
                )

                postNotification(
                    context = context,
                    title = "Playlist Imported",
                    content = "Saved \"$playlistTitle\" with ${songs.size} songs.",
                    ongoing = false,
                )
                showToast(context, "Successfully imported \"$playlistTitle\" (${songs.size} songs)!")
            } else {
                _status.value = ImportStatus(
                    isImporting = false,
                    error = "Could not match any tracks for $playlistTitle",
                )
                clearNotification(context)
                showToast(context, "Failed to import tracks for \"$playlistTitle\".")
            }

            onFinished?.invoke()
        }
    }

    /**
     * Start background import of multiple playlists selected from a Spotify profile.
     */
    fun startBatchProfileImport(
        context: Context,
        username: String,
        playlists: List<PlaylistManager.SpotifyProfilePlaylist>,
        authorAvatarUrl: String? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        currentJob?.cancel()
        currentJob = managerScope.launch {
            val batchCount = playlists.size
            _status.value = ImportStatus(
                isImporting = true,
                title = "$username's Playlists",
                isBatch = true,
                totalBatchCount = batchCount,
            )

            var importedCount = 0

            for ((idx, pl) in playlists.withIndex()) {
                _status.value = _status.value.copy(
                    title = pl.title,
                    currentBatchIndex = idx + 1,
                    totalBatchCount = batchCount,
                    progressFraction = idx.toFloat() / batchCount,
                )

                postNotification(
                    context = context,
                    title = "Importing profile ($idx/$batchCount)",
                    content = "Fetching playlist: ${pl.title}...",
                    progress = idx,
                    max = batchCount,
                    ongoing = true,
                )

                val result = PlaylistManager.fetchSpotifyPlaylist(pl.id)
                if (result != null && result.second.isNotEmpty()) {
                    val songs = PlaylistManager.resolveTracksToSongs(result.second) { current, total, songTitle ->
                        val batchFraction = (idx + (current.toFloat() / total)) / batchCount
                        _status.value = _status.value.copy(
                            currentSong = songTitle,
                            currentTrack = current,
                            totalTracks = total,
                            progressFraction = batchFraction,
                        )
                        postNotification(
                            context = context,
                            title = "Importing ${pl.title} (${idx + 1}/$batchCount)",
                            content = "Matching ($current/$total): $songTitle",
                            progress = current,
                            max = total,
                            ongoing = true,
                        )
                    }

                    val finalTitle = "$username • ${result.first}"
                    val savedId = AppSettings.saveLocalPlaylist(
                        title = finalTitle,
                        songs = songs,
                        source = "SPOTIFY_PROFILE",
                        author = username,
                        authorAvatarUrl = authorAvatarUrl,
                        sourceId = pl.id,
                        coverUrl = pl.imageUrl,
                    )
                    PlaylistSyncManager.trackPlaylist(
                        context = context,
                        playlistId = pl.id,
                        source = "SPOTIFY",
                        title = pl.title,
                        author = username,
                        localPlaylistId = savedId,
                        trackCount = songs.size,
                        trackTitles = songs.map { it.title },
                    )
                    importedCount++
                }
            }

            PlaylistSyncManager.trackProfile(
                context = context,
                userId = username,
                username = username,
                avatarUrl = authorAvatarUrl,
                playlistIds = playlists.map { it.id }.toSet(),
            )

            _status.value = ImportStatus(
                isImporting = false,
                completedTitle = "$importedCount playlists from $username",
            )

            postNotification(
                context = context,
                title = "Profile Import Complete",
                content = "Imported $importedCount playlists from $username to your Library.",
                ongoing = false,
            )
            showToast(context, "Imported $importedCount playlists from $username!")
            onFinished?.invoke()
        }
    }

    /**
     * Start background save of direct songs (e.g. YouTube playlist).
     */
    fun startDirectSongsImport(
        context: Context,
        playlistTitle: String,
        songs: List<Song>,
        source: String = "YOUTUBE",
        author: String? = null,
        authorAvatarUrl: String? = null,
    ) {
        managerScope.launch {
            _status.value = ImportStatus(
                isImporting = true,
                title = playlistTitle,
                totalTracks = songs.size,
                currentTrack = songs.size,
                progressFraction = 1f,
            )
            val finalTitle = if (author != null && !playlistTitle.contains(" • ")) {
                "$author • $playlistTitle"
            } else {
                playlistTitle
            }
            AppSettings.saveLocalPlaylist(
                title = finalTitle,
                songs = songs,
                source = source,
                author = author,
                authorAvatarUrl = authorAvatarUrl,
            )
            _status.value = ImportStatus(
                isImporting = false,
                completedTitle = playlistTitle,
            )
            showToast(context, "Saved \"$playlistTitle\" (${songs.size} songs) to Library!")
        }
    }

    fun cancelImport(context: Context? = null) {
        currentJob?.cancel()
        currentJob = null
        _status.value = ImportStatus(isImporting = false)
        context?.let { clearNotification(it) }
    }
}
