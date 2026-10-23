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
        return if (ok) SourceHealth.Ok() else SourceHealth.Unreachable("Monochrome endpoint unreachable")
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
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        TrackLog.d(TAG, "🔍 Monochrome stream() trackId=$trackId request=$request")
        val streamUrl = MonochromeService.getStreamUrl(trackId)
        if (streamUrl.isBlank()) {
            TrackLog.w(TAG, "  ↳ Monochrome had no stream URL for $trackId")
            return null
        }

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
            format = StreamFormat(
                codec = "flac",
                bitDepth = bitDepth,
                sampleRateHz = sampleRateHz,
                source = sourceLabel,
            ),
        )
    }
}
