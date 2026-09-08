package com.music.flux.data

import android.content.Context
import android.content.SharedPreferences
import com.music.flux.data.model.LikeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Ratings changed during this app session, shared by the UI and playback service.
 *
 * Persisted locally in SharedPreferences so ratings remain intact across sessions
 * even in guest mode without Google sign-in.
 */
object LikeState {
    private const val PREFS_NAME = "flux_likes"
    private const val KEY_LIKES = "likes_map"
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    private val _overrides = MutableStateFlow<Map<String, LikeStatus>>(emptyMap())
    val overrides: StateFlow<Map<String, LikeStatus>> = _overrides.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIKES, null) ?: return
        runCatching {
            val map = json.decodeFromString(serializer, raw)
            _overrides.value = map.mapNotNull { (k, v) ->
                runCatching { LikeStatus.valueOf(v) }.getOrNull()?.let { k to it }
            }.toMap()
        }
    }

    fun set(videoId: String, status: LikeStatus) {
        val next = _overrides.value + (videoId to status)
        _overrides.value = next
        persist(next)
    }

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
        if (this::prefs.isInitialized) {
            prefs.edit().remove(KEY_LIKES).apply()
        }
    }

    private fun persist(map: Map<String, LikeStatus>) {
        if (!this::prefs.isInitialized) return
        val stringMap = map.mapValues { it.value.name }
        prefs.edit().putString(KEY_LIKES, json.encodeToString(serializer, stringMap)).apply()
    }

    fun getLikedVideoIds(): List<String> =
        _overrides.value.filter { it.value == LikeStatus.LIKE }.keys.toList()
}

