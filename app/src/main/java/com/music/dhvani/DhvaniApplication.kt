package com.music.dhvani

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.music.dhvani.auth.AuthStore
import com.music.dhvani.playback.AudioCache
import com.music.dhvani.playback.LastPlayed
import com.music.dhvani.data.innertube.Innertube
import com.music.dhvani.data.LikeState
import com.music.dhvani.data.scrobbling.LastFM
import com.music.dhvani.data.AppUpdateChecker
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.history.PlaybackHistory
import com.music.dhvani.data.settings.SearchHistory
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.stats.ArtistFacts
import com.music.dhvani.data.stats.ListeningStats
import com.music.dhvani.download.Downloads
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DhvaniApplication : Application(), SingletonImageLoader.Factory {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // Early setups needed immediately
        AppSettings.init(this)
        authStore = AuthStore(this)
        Innertube.cookie = authStore.cookie

        // Offload non-blocking disk stores, encrypted keystores, and network observers
        // to a dedicated background startup thread so the main UI thread renders instantly at 120 FPS
        kotlin.concurrent.thread(name = "dhvani-startup-init") {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            runCatching {
                FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
            }
            AppUpdateChecker.schedulePeriodicCheck(this@DhvaniApplication)
            AppUpdateChecker.startNetworkUpdateObserver(this@DhvaniApplication)

            if (authStore.cookie != null) {
                CoroutineScope(Dispatchers.IO).launch { Innertube.ensureSessionScope() }
            }

            SourceRegistry.init(this@DhvaniApplication)
            SearchHistory.init(this@DhvaniApplication)
            PlaybackHistory.init(this@DhvaniApplication)
            LikeState.init(this@DhvaniApplication)
            LastPlayed.init(this@DhvaniApplication)
            Downloads.init(this@DhvaniApplication)
            com.music.dhvani.data.analytics.AppAnalytics.init(this@DhvaniApplication)
            com.music.dhvani.data.telemetry.TelemetryManager.init(this@DhvaniApplication)
            ListeningStats.init(this@DhvaniApplication)
            ArtistFacts.init(this@DhvaniApplication)
            AudioCache.init(this@DhvaniApplication)
            com.music.dhvani.playback.OriginalVersion.init(this@DhvaniApplication)
            com.music.dhvani.data.canvas.CanvasCache.init(this@DhvaniApplication)
            com.music.dhvani.data.canvas.CanvasRepository.init(this@DhvaniApplication)
            com.music.dhvani.listentogether.ListenTogetherManager.init(this@DhvaniApplication)
            initLastfm()

            if (AppSettings.consumeVersionUpdate(BuildConfig.VERSION_CODE)) {
                AudioCache.clear()
                SingletonImageLoader.get(this@DhvaniApplication).let { loader ->
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
            }
        }
    }

    /**
     * Artwork loading, which was previously left entirely on Coil's defaults.
     *
     * The defaults aren't unreasonable, but the disk cache is sized at 2% of
     * free space — which on a full phone is the 10MB floor, a few screens of
     * covers, and covers are exactly the thing worth still having tomorrow.
     * Naming a directory alongside it keeps that cache somewhere identifiable
     * rather than in the process's temp dir.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true || Runtime.getRuntime().maxMemory() < 192L * 1024 * 1024
        val memoryPercent = if (isLowRam) 0.12 else 0.25
        val maxDiskBytes = if (isLowRam) 120L * 1024 * 1024 else 250L * 1024 * 1024

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryPercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(maxDiskBytes)
                    .build()
            }
            // Instant image pop-in without triggering concurrent alpha animation loops on scroll
            .crossfade(false)
            .build()
    }

    private fun initLastfm() {
        val sessionKey = AppSettings.lastfmSessionKey.value
        if (sessionKey.isBlank()) return
        val endpoint = AppSettings.lastfmEndpoint.value.ifBlank { LastFM.DEFAULT_API_ENDPOINT }
        val apiKey = AppSettings.lastfmApiKey.value.trim()
        val secret = AppSettings.lastfmSecret.value.trim()
        if (apiKey.isBlank() || secret.isBlank()) return
        LastFM.configure(
            endpoint = endpoint,
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
        )
    }

    companion object {
        lateinit var authStore: AuthStore
            private set
    }
}
