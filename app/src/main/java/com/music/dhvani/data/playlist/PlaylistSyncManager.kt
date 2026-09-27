package com.music.dhvani.data.playlist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.music.dhvani.MainActivity
import com.music.dhvani.R
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Manages periodic and on-demand synchronization for imported Spotify profiles and playlists.
 * Detects new playlists created by creators (e.g. Revibe) or new tracks added to imported playlists,
 * and alerts the user with rich notifications and in-app update prompts.
 */
object PlaylistSyncManager {
    private const val PREFS_NAME = "dhvani_playlist_sync_prefs"
    private const val KEY_TRACKED_PROFILES = "tracked_profiles_json"
    private const val KEY_TRACKED_PLAYLISTS = "tracked_playlists_json"
    private const val KEY_LAST_GLOBAL_CHECK = "last_global_sync_check"
    private const val SYNC_CHANNEL_ID = "dhvani_playlist_sync_channel"
    private const val NOTIFICATION_ID_BASE = 2026300

    data class TrackedProfile(
        val userId: String,
        val username: String,
        val avatarUrl: String? = null,
        val knownPlaylistIds: Set<String> = emptySet(),
        val lastChecked: Long = 0L,
    )

    data class TrackedPlaylist(
        val playlistId: String,
        val source: String, // "SPOTIFY" or "YOUTUBE"
        val title: String,
        val author: String? = null,
        val localPlaylistId: String,
        val lastTrackCount: Int = 0,
        val lastKnownTrackTitles: List<String> = emptyList(),
        val lastChecked: Long = 0L,
    )

    sealed class SyncUpdate {
        data class NewPlaylistsInProfile(
            val profileUsername: String,
            val profileUserId: String,
            val avatarUrl: String?,
            val newPlaylists: List<PlaylistManager.SpotifyProfilePlaylist>,
        ) : SyncUpdate()

        data class NewTracksInPlaylist(
            val playlistTitle: String,
            val localPlaylistId: String,
            val source: String,
            val playlistId: String,
            val author: String?,
            val newTracks: List<PlaylistManager.ImportedTrack>,
            val currentTrackCount: Int,
            val updatedTrackCount: Int,
        ) : SyncUpdate()
    }

    private val _pendingUpdates = MutableStateFlow<List<SyncUpdate>>(emptyList())
    val pendingUpdates = _pendingUpdates.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        ensureChannel(context)
        autoSeedFromLocalPlaylists(context)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Playlist & Profile Updates",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifies when imported Spotify creators add new playlists or songs"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Auto-registers any imported Spotify profiles or playlists that exist in AppSettings.
     */
    fun autoSeedFromLocalPlaylists(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingPlaylists = AppSettings.localCustomPlaylists.value

        // Seed profiles
        val trackedProfiles = readTrackedProfiles(context).associateBy { it.userId.lowercase() }.toMutableMap()
        val spotifyProfilePlaylists = existingPlaylists.filter { it.source == "SPOTIFY_PROFILE" }
        spotifyProfilePlaylists.groupBy { it.effectiveAuthor ?: "Spotify User" }.forEach { (author, pls) ->
            if (!trackedProfiles.containsKey(author.lowercase())) {
                val avatar = pls.firstNotNullOfOrNull { it.authorAvatarUrl }
                trackedProfiles[author.lowercase()] = TrackedProfile(
                    userId = author,
                    username = author,
                    avatarUrl = avatar,
                    knownPlaylistIds = pls.mapNotNull { it.sourceId }.toSet(),
                    lastChecked = System.currentTimeMillis(),
                )
            }
        }
        persistTrackedProfiles(context, trackedProfiles.values.toList())

        // Seed playlists
        val trackedPlaylists = readTrackedPlaylists(context).associateBy { it.playlistId }.toMutableMap()
        existingPlaylists.forEach { pl ->
            val srcId = pl.sourceId
            if (!srcId.isNullOrBlank() && !trackedPlaylists.containsKey(srcId)) {
                trackedPlaylists[srcId] = TrackedPlaylist(
                    playlistId = srcId,
                    source = pl.source,
                    title = pl.displayTitle,
                    author = pl.effectiveAuthor,
                    localPlaylistId = pl.id,
                    lastTrackCount = pl.songs.size,
                    lastKnownTrackTitles = pl.songs.map { it.title },
                    lastChecked = pl.lastSyncedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                )
            }
        }
        persistTrackedPlaylists(context, trackedPlaylists.values.toList())
    }

    fun trackProfile(
        context: Context,
        userId: String,
        username: String,
        avatarUrl: String?,
        playlistIds: Set<String>,
    ) {
        val list = readTrackedProfiles(context).toMutableList()
        list.removeAll { it.userId.equals(userId, ignoreCase = true) || it.username.equals(username, ignoreCase = true) }
        list.add(
            TrackedProfile(
                userId = userId,
                username = username,
                avatarUrl = avatarUrl,
                knownPlaylistIds = playlistIds,
                lastChecked = System.currentTimeMillis(),
            ),
        )
        persistTrackedProfiles(context, list)
    }

    fun trackPlaylist(
        context: Context,
        playlistId: String,
        source: String,
        title: String,
        author: String?,
        localPlaylistId: String,
        trackCount: Int,
        trackTitles: List<String>,
    ) {
        val list = readTrackedPlaylists(context).toMutableList()
        list.removeAll { it.playlistId.equals(playlistId, ignoreCase = true) }
        list.add(
            TrackedPlaylist(
                playlistId = playlistId,
                source = source,
                title = title,
                author = author,
                localPlaylistId = localPlaylistId,
                lastTrackCount = trackCount,
                lastKnownTrackTitles = trackTitles,
                lastChecked = System.currentTimeMillis(),
            ),
        )
        persistTrackedPlaylists(context, list)
    }

    /**
     * Checks all tracked profiles and playlists for updates asynchronously.
     */
    fun checkAllAsync(context: Context, force: Boolean = false) {
        syncScope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_GLOBAL_CHECK, 0L)
            val now = System.currentTimeMillis()
            // Throttle to max once per 30 minutes unless forced
            if (!force && now - lastCheck < 30 * 60 * 1000L) {
                return@launch
            }
            prefs.edit().putLong(KEY_LAST_GLOBAL_CHECK, now).apply()

            _isChecking.value = true
            try {
                val updates = mutableListOf<SyncUpdate>()

                // 1. Check Profiles
                val profiles = readTrackedProfiles(context)
                for (profile in profiles) {
                    val result = checkProfileForUpdates(context, profile)
                    if (result != null) {
                        updates.add(result)
                    }
                }

                // 2. Check Playlists
                val playlists = readTrackedPlaylists(context)
                for (pl in playlists) {
                    val result = checkPlaylistForUpdates(context, pl)
                    if (result != null) {
                        updates.add(result)
                    }
                }

                _pendingUpdates.value = updates

                // Post system notifications for detected updates
                updates.forEachIndexed { idx, update ->
                    postUpdateNotification(context, update, idx)
                }
            } catch (_: Exception) {
            } finally {
                _isChecking.value = false
            }
        }
    }

    suspend fun checkProfileForUpdates(context: Context, profile: TrackedProfile): SyncUpdate.NewPlaylistsInProfile? {
        val result = PlaylistManager.fetchSpotifyUserPlaylists(profile.userId) ?: return null
        val remotePlaylists = result.playlists
        val newOnes = remotePlaylists.filter { it.id !in profile.knownPlaylistIds }

        // Update known IDs
        val allIds = (profile.knownPlaylistIds + remotePlaylists.map { it.id }).toSet()
        val updatedProfile = profile.copy(
            knownPlaylistIds = allIds,
            avatarUrl = result.avatarUrl ?: profile.avatarUrl,
            lastChecked = System.currentTimeMillis(),
        )
        updateTrackedProfile(context, updatedProfile)

        return if (newOnes.isNotEmpty()) {
            SyncUpdate.NewPlaylistsInProfile(
                profileUsername = profile.username,
                profileUserId = profile.userId,
                avatarUrl = result.avatarUrl ?: profile.avatarUrl,
                newPlaylists = newOnes,
            )
        } else null
    }

    suspend fun checkPlaylistForUpdates(context: Context, pl: TrackedPlaylist): SyncUpdate.NewTracksInPlaylist? {
        val remoteTracks: List<PlaylistManager.ImportedTrack> = when (pl.source) {
            "SPOTIFY" -> PlaylistManager.fetchSpotifyPlaylist(pl.playlistId)?.second ?: return null
            "YOUTUBE" -> {
                val res = PlaylistManager.fetchYoutubePlaylist(pl.playlistId) ?: return null
                res.second.map { PlaylistManager.ImportedTrack(title = it.title, artist = it.artist) }
            }
            else -> return null
        }

        val knownTitlesSet = pl.lastKnownTrackTitles.map { it.lowercase().trim() }.toSet()
        val newTracks = remoteTracks.filter { it.title.lowercase().trim() !in knownTitlesSet }

        // Update tracked playlist
        val updatedPl = pl.copy(
            lastTrackCount = remoteTracks.size,
            lastKnownTrackTitles = remoteTracks.map { it.title },
            lastChecked = System.currentTimeMillis(),
        )
        updateTrackedPlaylist(context, updatedPl)

        return if (newTracks.isNotEmpty()) {
            SyncUpdate.NewTracksInPlaylist(
                playlistTitle = pl.title,
                localPlaylistId = pl.localPlaylistId,
                source = pl.source,
                playlistId = pl.playlistId,
                author = pl.author,
                newTracks = newTracks,
                currentTrackCount = pl.lastTrackCount,
                updatedTrackCount = remoteTracks.size,
            )
        } else null
    }

    /**
     * Resolves and adds the new tracks directly to the existing local playlist without duplicates.
     */
    fun syncNewTracksToPlaylist(
        context: Context,
        update: SyncUpdate.NewTracksInPlaylist,
        onComplete: (() -> Unit)? = null,
    ) {
        syncScope.launch {
            val existing = AppSettings.getLocalPlaylist(update.localPlaylistId)
                ?: AppSettings.findDuplicatePlaylist(update.playlistTitle, update.author)
                ?: return@launch

            val newSongs = PlaylistManager.resolveTracksToSongs(update.newTracks)
            if (newSongs.isNotEmpty()) {
                val merged = (existing.songs + newSongs).distinctBy { s ->
                    if (s.videoId.isNotBlank()) s.videoId else "${s.title.lowercase()}:${s.artist.lowercase()}"
                }
                AppSettings.saveLocalPlaylist(
                    title = existing.title,
                    songs = merged,
                    source = existing.source,
                    author = existing.author,
                    authorAvatarUrl = existing.authorAvatarUrl,
                    sourceId = update.playlistId,
                    replaceExisting = true,
                )
                // Remove from pending updates
                _pendingUpdates.value = _pendingUpdates.value.filterNot { it == update }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Added ${newSongs.size} new songs to \"${existing.displayTitle}\"",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            onComplete?.invoke()
        }
    }

    private fun postUpdateNotification(context: Context, update: SyncUpdate, index: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_LIBRARY", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, content, subtext) = when (update) {
            is SyncUpdate.NewPlaylistsInProfile -> {
                val count = update.newPlaylists.size
                Triple(
                    "New from ${update.profileUsername}",
                    "Added ${if (count == 1) "\"${update.newPlaylists.first().title}\"" else "$count new playlists"} on Spotify! Tap to view.",
                    "Spotify Profile Update",
                )
            }
            is SyncUpdate.NewTracksInPlaylist -> {
                val count = update.newTracks.size
                val sample = update.newTracks.firstOrNull()?.title?.let { " (e.g. $it)" }.orEmpty()
                Triple(
                    "New Songs in \"${update.playlistTitle}\"",
                    "+$count new song${if (count > 1) "s" else ""}$sample added. Tap to update.",
                    "Playlist Update Available",
                )
            }
        }

        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(0xFF1DB954.toInt()) // Spotify Green
            .setSubText(subtext)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + index, notification)
        }
    }

    private fun readTrackedProfiles(context: Context): List<TrackedProfile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRACKED_PROFILES, null) ?: return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            root.mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val uId = (obj["userId"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                val uName = (obj["username"] as? JsonPrimitive)?.content ?: uId
                val av = (obj["avatarUrl"] as? JsonPrimitive)?.content
                val knownArr = obj["knownPlaylistIds"] as? JsonArray
                val known = knownArr?.mapNotNull { (it as? JsonPrimitive)?.content }?.toSet().orEmpty()
                val last = (obj["lastChecked"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                TrackedProfile(uId, uName, av, known, last)
            }
        }.getOrDefault(emptyList())
    }

    private fun persistTrackedProfiles(context: Context, list: List<TrackedProfile>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = buildJsonArray {
            list.forEach { p ->
                add(
                    buildJsonObject {
                        put("userId", JsonPrimitive(p.userId))
                        put("username", JsonPrimitive(p.username))
                        p.avatarUrl?.let { put("avatarUrl", JsonPrimitive(it)) }
                        put(
                            "knownPlaylistIds",
                            buildJsonArray { p.knownPlaylistIds.forEach { add(JsonPrimitive(it)) } },
                        )
                        put("lastChecked", JsonPrimitive(p.lastChecked))
                    },
                )
            }
        }.toString()
        prefs.edit().putString(KEY_TRACKED_PROFILES, array).apply()
    }

    private fun updateTrackedProfile(context: Context, profile: TrackedProfile) {
        val list = readTrackedProfiles(context).toMutableList()
        val idx = list.indexOfFirst { it.userId.equals(profile.userId, ignoreCase = true) }
        if (idx >= 0) list[idx] = profile else list.add(profile)
        persistTrackedProfiles(context, list)
    }

    private fun readTrackedPlaylists(context: Context): List<TrackedPlaylist> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRACKED_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            root.mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val plId = (obj["playlistId"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                val src = (obj["source"] as? JsonPrimitive)?.content ?: "SPOTIFY"
                val title = (obj["title"] as? JsonPrimitive)?.content ?: ""
                val author = (obj["author"] as? JsonPrimitive)?.content
                val localId = (obj["localPlaylistId"] as? JsonPrimitive)?.content ?: ""
                val count = (obj["lastTrackCount"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                val last = (obj["lastChecked"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                TrackedPlaylist(plId, src, title, author, localId, count, emptyList(), last)
            }
        }.getOrDefault(emptyList())
    }

    private fun persistTrackedPlaylists(context: Context, list: List<TrackedPlaylist>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = buildJsonArray {
            list.forEach { pl ->
                add(
                    buildJsonObject {
                        put("playlistId", JsonPrimitive(pl.playlistId))
                        put("source", JsonPrimitive(pl.source))
                        put("title", JsonPrimitive(pl.title))
                        pl.author?.let { put("author", JsonPrimitive(it)) }
                        put("localPlaylistId", JsonPrimitive(pl.localPlaylistId))
                        put("lastTrackCount", JsonPrimitive(pl.lastTrackCount))
                        put("lastChecked", JsonPrimitive(pl.lastChecked))
                    },
                )
            }
        }.toString()
        prefs.edit().putString(KEY_TRACKED_PLAYLISTS, array).apply()
    }

    private fun updateTrackedPlaylist(context: Context, pl: TrackedPlaylist) {
        val list = readTrackedPlaylists(context).toMutableList()
        val idx = list.indexOfFirst { it.playlistId.equals(pl.playlistId, ignoreCase = true) }
        if (idx >= 0) list[idx] = pl else list.add(pl)
        persistTrackedPlaylists(context, list)
    }
}
