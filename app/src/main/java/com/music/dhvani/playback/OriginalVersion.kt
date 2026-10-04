package com.music.dhvani.playback

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the songs the listener has pinned to YouTube's original stream or allowed for high quality upgrades.
 */
object OriginalVersion {

    private var prefs: SharedPreferences? = null

    private val _pinned = MutableStateFlow<Set<String>>(emptySet())

    val pinned: StateFlow<Set<String>> = _pinned.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _pinned.value = sp.getString(KEY_PINNED, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    fun isPinned(videoId: String?): Boolean = videoId != null && videoId in _pinned.value

    /** Keeps [videoId] on YouTube's own upload. */
    fun pin(videoId: String) {
        if (videoId.isBlank() || isPinned(videoId)) return
        var next = _pinned.value + videoId
        while (next.size > MAX_PINNED) next = next - next.first()
        write(next)
    }

    /** Lets [videoId] be substituted and upgraded to high quality (JioSaavn) again. */
    fun unpin(videoId: String) {
        if (!isPinned(videoId)) return
        write(_pinned.value - videoId)
    }

    private fun write(ids: Set<String>) {
        _pinned.value = ids
        prefs?.edit()?.putString(KEY_PINNED, ids.joinToString("\n"))?.apply()
    }

    private const val MAX_PINNED = 500
    private const val PREFS_NAME = "dhvani_original_versions"
    private const val KEY_PINNED = "pinned"
}
