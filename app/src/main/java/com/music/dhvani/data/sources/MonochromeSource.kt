package com.music.dhvani.data.sources

import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.monochrome.MonochromeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DhvaniMusic"

class MonochromeSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.MONOCHROME
    override val displayName: String get() = config.label.ifBlank { SourceKind.MONOCHROME.label }

    override suspend fun health(): SourceHealth {
        val ok = MonochromeService.health()
        return if (ok) {
            SourceHealth.Ok("24-bit / 96kHz FLAC • Studio Master")
        } else {
            SourceHealth.Unreachable("Server is temporarily down • Please check back in a few days")
        }
    }

    override suspend fun search(query: String, limit: Int, waitForAll: Boolean): List<Song> {
        TrackLog.d(TAG, "🔍 Monochrome search() query=\"$query\" limit=$limit")
        val results = MonochromeService.search(query)
        TrackLog.d(TAG, "  ↳ Monochrome returned ${results.size} tracks")

        return results.take(limit).map { raw ->
            val seconds = (raw.durationMs / 1000).toInt()
            val m = seconds / 60
            val s = seconds % 60
            val durationFormatted = String.format("%d:%02d", m, s)

            Song(
                videoId = SourceRegistry.trackKey(config.id, raw.id),
                title = raw.title,
                artist = raw.artistNames,
                thumbnailUrl = raw.artwork,
                albumName = "Lossless FLAC",
                durationText = durationFormatted,
                sourceQuality = "Lossless FLAC",
            )
        }
    }

    companion object {
        private val streamInfoCache = java.util.concurrent.ConcurrentHashMap<String, FlacStreamInfo>()
        private val lastRateLimitHitMs = java.util.concurrent.ConcurrentHashMap<String, Long>()

        fun isRateLimited(trackId: String): Boolean {
            val last = lastRateLimitHitMs[trackId] ?: return false
            return System.currentTimeMillis() - last < 300_000L // 5 minutes cache
        }

        fun markRateLimited(trackId: String) {
            lastRateLimitHitMs[trackId] = System.currentTimeMillis()
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        TrackLog.d(TAG, "🔍 Monochrome stream() trackId=$trackId request=$request")
        val streamUrl = MonochromeService.getStreamUrl(trackId)
        if (streamUrl.isBlank()) {
            TrackLog.w(TAG, "  ↳ Monochrome had no stream URL for $trackId")
            return null
        }

        // Fast probe to verify if stream is blocked by Cloudflare (429) or rate limited
        var responseCode = 200
        val isReachable = kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(3000L) {
                    val probeReq = okhttp3.Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Range", "bytes=0-1024")
                        .build()
                    MonochromeService.fastClient.newCall(probeReq).execute().use { resp ->
                        responseCode = resp.code
                        resp.isSuccessful || resp.code == 206 || resp.code == 200
                    }
                } ?: false
            } catch (e: Exception) {
                false
            }
        }

        if (!isReachable) {
            if (responseCode == 429) {
                TrackLog.w(TAG, "  ↳ Monochrome stream $streamUrl hit server rate limit (HTTP 429)")
                markRateLimited(trackId)
            } else {
                TrackLog.w(TAG, "  ↳ Monochrome stream $streamUrl is unreachable (HTTP $responseCode)")
            }
            return null
        }

        // Successfully reached stream: clear any previous rate-limit flag
        lastRateLimitHitMs.remove(trackId)

        val cached = streamInfoCache[streamUrl]
        val bitDepth = cached?.bitDepth ?: 24
        val sampleRateHz = cached?.sampleRateHz ?: 96000
        val isHiRes = cached?.isHiRes ?: true
        val sourceLabel = if (isHiRes) "Monochrome Hi-Res" else "Monochrome Lossless"

        if (cached == null) {
            // Asynchronously probe STREAMINFO in background without holding up ExoPlayer buffer start
            CoroutineScope(Dispatchers.IO).launch {
                FlacStreamInfoReader.inspectUrl(streamUrl)?.let { info ->
                    streamInfoCache[streamUrl] = info
                }
            }
        }

        val finalUrl = if (!streamUrl.substringAfterLast('/').contains('.')) "$streamUrl#.flac" else streamUrl
        TrackLog.d(TAG, "  ↳ Monochrome serving immediately: $finalUrl")
        return SourceStream(
            url = finalUrl,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
                "Accept" to "*/*",
            ),
            format = StreamFormat(
                codec = "flac",
                bitDepth = bitDepth,
                sampleRateHz = sampleRateHz,
                source = sourceLabel,
            ),
        )
    }
}
