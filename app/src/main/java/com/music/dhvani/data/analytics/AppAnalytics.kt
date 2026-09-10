package com.music.dhvani.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.music.dhvani.data.TrackLog

/**
 * Lightweight, privacy-first anonymous analytics helper using Firebase Analytics.
 *
 * Tracks:
 * - Anonymous song plays (song title & artist only, no personal user data or IP)
 * - Automatic app sessions, retention, and DAU/MAU
 */
object AppAnalytics {

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        runCatching {
            analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            TrackLog.d("AppAnalytics", "Firebase Analytics initialized successfully")
        }.onFailure {
            TrackLog.w("AppAnalytics", "Failed to init Firebase Analytics: ${it.message}")
        }
    }

    /**
     * Log an anonymous song play event.
     * Only logs song name and artist - never personal user details.
     */
    fun logSongPlayed(title: String, artist: String) {
        val safeTitle = title.take(100)
        val safeArtist = artist.take(100)
        TrackLog.d("AppAnalytics", "Song played: $safeTitle by $safeArtist")

        runCatching {
            val bundle = Bundle().apply {
                putString("song_title", safeTitle)
                putString("artist", safeArtist)
            }
            analytics?.logEvent("song_played", bundle)
        }
    }
}
