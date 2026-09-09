package com.music.flux.data.analytics

import android.content.Context
import android.os.Bundle
import com.music.flux.data.TrackLog

/**
 * Lightweight, privacy-first anonymous analytics helper.
 *
 * Tracks:
 * - Anonymous song plays (song title & artist only, no personal user data or IP)
 * - App sessions / active users
 *
 * When Firebase Analytics is added (via google-services.json), events are automatically
 * forwarded via reflection without hard-crashing if Firebase SDK isn't present yet.
 */
object AppAnalytics {

    private var firebaseAnalyticsInstance: Any? = null
    private var logEventMethod: java.lang.reflect.Method? = null

    fun init(context: Context) {
        runCatching {
            val clazz = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            val getInstanceMethod = clazz.getMethod("getInstance", Context::class.java)
            firebaseAnalyticsInstance = getInstanceMethod.invoke(null, context.applicationContext)
            logEventMethod = clazz.getMethod("logEvent", String::class.java, Bundle::class.java)
            TrackLog.d("AppAnalytics", "Firebase Analytics initialized successfully")
        }.onFailure {
            TrackLog.d("AppAnalytics", "Firebase Analytics SDK not linked yet (offline mode active)")
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

        val instance = firebaseAnalyticsInstance ?: return
        val method = logEventMethod ?: return

        runCatching {
            val bundle = Bundle().apply {
                putString("song_title", safeTitle)
                putString("artist", safeArtist)
            }
            method.invoke(instance, "song_played", bundle)
        }
    }
}
