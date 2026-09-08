package com.music.flux.data.history

import android.content.Context
import android.content.SharedPreferences
import com.music.flux.data.TrackLog
import com.music.flux.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Calendar

@Serializable
data class HistoryItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val albumName: String? = null,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1,
    val sourceQuality: String? = null,
) {
    fun toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationText = durationText,
        albumName = albumName,
        sourceQuality = sourceQuality,
    )

    companion object {
        fun fromSong(song: Song, playedAt: Long = System.currentTimeMillis(), playCount: Int = 1): HistoryItem =
            HistoryItem(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationText = song.durationText,
                albumName = song.albumName,
                playedAt = playedAt,
                playCount = playCount,
                sourceQuality = song.sourceQuality,
            )
    }
}

/**
 * Meld-style local playback history.
 *
 * Saves every song played on device across YouTube, JioSaavn, Tidal, and local media.
 * Works completely offline without requiring Google sign-in.
 */
object PlaybackHistory {
    private const val TAG = "FluxMusic"
    private const val PREFS_NAME = "flux_playback_history"
    private const val KEY_ITEMS = "history_items"
    private const val MAX_ENTRIES = 500

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val serializer = ListSerializer(HistoryItem.serializer())

    private val _recent = MutableStateFlow<List<HistoryItem>>(emptyList())
    val recent: StateFlow<List<HistoryItem>> = _recent.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        reload()
    }

    fun reload() {
        if (!this::prefs.isInitialized) return
        val raw = prefs.getString(KEY_ITEMS, null) ?: "[]"
        _recent.value = runCatching {
            json.decodeFromString(serializer, raw)
        }.getOrDefault(emptyList())
    }

    /**
     * Records a track playback. Moves it to the top with current timestamp
     * and increments its local playCount.
     */
    fun record(song: Song) {
        if (song.videoId.isBlank() || song.title.isBlank()) return
        scope.launch {
            val now = System.currentTimeMillis()
            val current = _recent.value
            val existing = current.firstOrNull { it.videoId == song.videoId }
            val playCount = (existing?.playCount ?: 0) + 1

            val updatedItem = HistoryItem.fromSong(song, playedAt = now, playCount = playCount)
            val filtered = current.filterNot { it.videoId == song.videoId }
            val next = (listOf(updatedItem) + filtered).take(MAX_ENTRIES)

            _recent.value = next
            saveToDisk(next)
            TrackLog.d(TAG, "Recorded to local history: '${song.title}' by '${song.artist}' (plays: $playCount)")
        }
    }

    fun remove(videoId: String) {
        scope.launch {
            val next = _recent.value.filterNot { it.videoId == videoId }
            _recent.value = next
            saveToDisk(next)
        }
    }

    fun clear() {
        scope.launch {
            _recent.value = emptyList()
            saveToDisk(emptyList())
            TrackLog.d(TAG, "Local playback history cleared")
        }
    }

    private fun saveToDisk(items: List<HistoryItem>) {
        if (!this::prefs.isInitialized) return
        runCatching {
            val encoded = json.encodeToString(serializer, items)
            prefs.edit().putString(KEY_ITEMS, encoded).apply()
        }.onFailure {
            TrackLog.w(TAG, "Failed to persist playback history: ${it.message}")
        }
    }

    /** Groups history items into chronological sections: "Today", "Yesterday", "Earlier". */
    fun groupChronologically(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
        val thisWeekStart = todayStart - (7 * 24 * 60 * 60 * 1000L)

        val grouped = LinkedHashMap<String, MutableList<HistoryItem>>()

        for (item in items) {
            val groupTitle = when {
                item.playedAt >= todayStart -> "Today"
                item.playedAt >= yesterdayStart -> "Yesterday"
                item.playedAt >= thisWeekStart -> "This Week"
                else -> "Earlier"
            }
            grouped.getOrPut(groupTitle) { mutableListOf() }.add(item)
        }
        return grouped
    }
}
