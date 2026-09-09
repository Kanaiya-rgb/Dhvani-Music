package com.music.flux.data

import android.content.Context
import android.content.SharedPreferences
import com.music.flux.data.model.LikeStatus
import com.music.flux.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
data class LikedSongItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val artistId: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val sourceQuality: String? = null,
    val likedAt: Long = System.currentTimeMillis(),
) {
    fun toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationText = durationText,
        artistId = artistId,
        albumId = albumId,
        albumName = albumName,
        sourceQuality = sourceQuality,
    )

    companion object {
        fun fromSong(song: Song, likedAt: Long = System.currentTimeMillis()): LikedSongItem =
            LikedSongItem(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationText = song.durationText,
                artistId = song.artistId,
                albumId = song.albumId,
                albumName = song.albumName,
                sourceQuality = song.sourceQuality,
                likedAt = likedAt,
            )
    }
}

/**
 * Ratings changed during this app session, shared by the UI and playback service.
 *
 * Persisted locally in SharedPreferences so ratings and liked songs remain intact across sessions
 * even in guest mode without Google sign-in.
 */
object LikeState {
    private const val PREFS_NAME = "flux_likes"
    private const val KEY_LIKES = "likes_map"
    private const val KEY_LIKED_SONGS = "liked_songs_list"
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private val songsSerializer = ListSerializer(LikedSongItem.serializer())

    private val _overrides = MutableStateFlow<Map<String, LikeStatus>>(emptyMap())
    val overrides: StateFlow<Map<String, LikeStatus>> = _overrides.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawLikes = prefs.getString(KEY_LIKES, null)
        if (rawLikes != null) {
            runCatching {
                val map = json.decodeFromString(serializer, rawLikes)
                _overrides.value = map.mapNotNull { (k, v) ->
                    runCatching { LikeStatus.valueOf(v) }.getOrNull()?.let { k to it }
                }.toMap()
            }
        }

        val rawSongs = prefs.getString(KEY_LIKED_SONGS, null)
        if (rawSongs != null) {
            runCatching {
                val items = json.decodeFromString(songsSerializer, rawSongs)
                _likedSongs.value = items.map { it.toSong() }
            }
        }
    }

    fun set(videoId: String, status: LikeStatus) {
        val next = _overrides.value + (videoId to status)
        _overrides.value = next
        persist(next)
        if (status != LikeStatus.LIKE) {
            val updatedSongs = _likedSongs.value.filterNot { it.videoId == videoId }
            if (updatedSongs != _likedSongs.value) {
                _likedSongs.value = updatedSongs
                persistSongs(updatedSongs)
            }
        }
    }

    fun addLiked(song: Song) {
        set(song.videoId, LikeStatus.LIKE)
        val current = _likedSongs.value.filterNot { it.videoId == song.videoId }
        val next = listOf(song) + current
        _likedSongs.value = next
        persistSongs(next)
    }

    fun removeLiked(videoId: String) {
        set(videoId, LikeStatus.INDIFFERENT)
        val next = _likedSongs.value.filterNot { it.videoId == videoId }
        _likedSongs.value = next
        persistSongs(next)
    }

    fun seedLikedSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val current = _likedSongs.value
        val currentIds = current.mapTo(HashSet()) { it.videoId }
        val newOnes = songs.filterNot { it.videoId in currentIds }
        if (newOnes.isEmpty()) return
        val next = current + newOnes
        _likedSongs.value = next
        persistSongs(next)
    }

    fun getSong(videoId: String): Song? =
        _likedSongs.value.firstOrNull { it.videoId == videoId }

    fun rememberStated(videoId: String, status: LikeStatus) = set(videoId, status)

    /** Seeds only ratings not already changed explicitly during this session. */
    fun seedLiked(videoIds: Set<String>) {
        if (videoIds.isEmpty()) return
        val next = _overrides.value.toMutableMap()
        videoIds.forEach { next.putIfAbsent(it, LikeStatus.LIKE) }
        if (next != _overrides.value) {
            _overrides.value = next
            persist(next)
        }
    }

    fun clear() {
        _overrides.value = emptyMap()
        _likedSongs.value = emptyList()
        if (this::prefs.isInitialized) {
            prefs.edit().remove(KEY_LIKES).remove(KEY_LIKED_SONGS).apply()
        }
    }

    private fun persist(map: Map<String, LikeStatus>) {
        if (!this::prefs.isInitialized) return
        val stringMap = map.mapValues { it.value.name }
        prefs.edit().putString(KEY_LIKES, json.encodeToString(serializer, stringMap)).apply()
    }

    private fun persistSongs(songs: List<Song>) {
        if (!this::prefs.isInitialized) return
        runCatching {
            val items = songs.map { LikedSongItem.fromSong(it) }
            prefs.edit().putString(KEY_LIKED_SONGS, json.encodeToString(songsSerializer, items)).apply()
        }
    }

    fun getLikedVideoIds(): List<String> =
        _overrides.value.filter { it.value == LikeStatus.LIKE }.keys.toList()
}
