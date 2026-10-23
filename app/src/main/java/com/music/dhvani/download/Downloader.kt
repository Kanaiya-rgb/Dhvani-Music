package com.music.dhvani.download

import android.os.SystemClock
import com.music.dhvani.data.DebugLog as Log
import com.music.dhvani.data.Http
import com.music.dhvani.data.innertube.PlayerClient
import com.music.dhvani.data.innertube.StreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/**
 * Pulls a resolved stream onto disk.
 *
 * Two things here are not obvious and both are load-bearing:
 *
 *  - **Bounded ranges, not one long GET.** googlevideo paces a continuous
 *    response down to roughly playback speed  about 15kB/s, against 5.7MB/s
 *    for the same bytes asked for as ranges. That is the difference between a
 *    four-minute track saving in a second and saving in four minutes, and it is
 *    the same finding [ChunkedDataSource][com.music.dhvani.playback.ChunkedDataSource]
 *    exists for.
 *  - **The client's own headers.** googlevideo bakes the identity that minted a
 *    URL into it as `c=`/`cver=` and compares that against the headers of the
 *    request that comes back for the bytes. Fetching with anything else is a
 *    403  so the fetch is dressed as whatever the URL says made it, which
 *    [PlayerClient.forStreamUrl] can recover from the URL alone.
 *
 * Sequential rather than parallel. Ranges are served at line rate, and a
 * typical AAC track is four megabytes: splitting that across connections buys
 * nothing a user could perceive and costs the temp files and reassembly that a
 * cancelled download would then have to clean up.
 *
 * None of the above is true of a stream from a configured source rather than
 * from YouTube, which is what [fetchDirect] is for.
 */
object Downloader {

    private const val TAG = "DhvaniMusic"

    /** Matches the range size read-ahead settled on; large enough to amortise, small enough to cancel promptly. */
    private const val CHUNK_BYTES = 2L * 1024 * 1024

    /** 256KB buffer sized for high-throughput lossless streaming on modern connections. */
    private const val BUFFER_BYTES = 256 * 1024
    /**
     * Fetch all of [stream] into [destination], resuming from any existing bytes.
     *
     * @param maxKbps the ceiling [stream] was resolved under, needed again for
     *   the re-resolve below. Resolving at a different one would pick a
     *   different rung of the AAC ladder, and the length check that guards the
     *   resume would then fail a retry that had nothing wrong with it.
     * @param onProgress called as bytes land, with the running total, the full size,
     *   live bytes-per-second transfer speed, and estimated seconds remaining (or null if unknown).
     * @return how many bytes were written.
     */
    suspend fun fetch(
        videoId: String,
        stream: StreamResolver.Stream,
        maxKbps: Int,
        destination: DownloadStore.Pending,
        onProgress: (written: Long, total: Long, bytesPerSec: Long, etaSeconds: Long?) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        var url = stream.url
        val total = contentLength(url) ?: error("Track unavailable: no length to fetch")

        val startOffset = destination.existingBytes.coerceAtMost(total)
        var position = startOffset
        var reresolved = false
        val buffer = ByteArray(BUFFER_BYTES)
        val sink = destination.openStream(append = position > 0)

        var lastReportTime = SystemClock.elapsedRealtime()
        var lastReportBytes = position
        var smoothedSpeed = 0.0

        sink.use { output ->
            while (position < total) {
                coroutineContext.ensureActive()
                val length = minOf(CHUNK_BYTES, total - position)

                val response = try {
                    open(url, position, length)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "range at $position failed for $videoId: ${e.message}")
                    throw e
                }

                // A URL that served its opening and then refuses is the one failure
                // worth a second attempt: it means the identity behind it has been
                // stood down mid-download, not that the track is gone. Telling the
                // resolver is what stops the next track failing the same way.
                if (response.code in REFUSAL_CODES) {
                    response.close()
                    StreamResolver.onPlaybackRefused(url, response.code)
                    if (reresolved) error("Download refused after ${position}B (HTTP ${response.code})")
                    reresolved = true
                    Log.w(TAG, "re-resolving $videoId after HTTP ${response.code} at $position")
                    url = StreamResolver.resolveForDownload(videoId, maxKbps).url
                    // Resolving again re-runs the whole client walk, and a
                    // different client can answer with a different format. Resuming
                    // one stream into the middle of another produces a file that is
                    // the right length and unplayable, so a length that has moved
                    // is a failure rather than something to work around.
                    if (contentLength(url) != total) error("The stream changed mid-download — try again")
                    continue
                }

                response.use {
                    if (it.code !in 200..299) error("Download failed (HTTP ${it.code})")
                    val body = it.body ?: error("Download failed: empty response")
                    val source = body.byteStream()
                    var readForChunk = 0L
                    while (readForChunk < length) {
                        coroutineContext.ensureActive()
                        val wanted = minOf(buffer.size.toLong(), length - readForChunk).toInt()
                        val read = source.read(buffer, 0, wanted)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        readForChunk += read
                        position += read

                        val now = SystemClock.elapsedRealtime()
                        val timeDelta = now - lastReportTime
                        if (timeDelta >= 150L || position >= total) {
                            val bytesDelta = position - lastReportBytes
                            val instantSpeed = if (timeDelta > 0) (bytesDelta * 1000.0) / timeDelta else 0.0
                            smoothedSpeed = if (smoothedSpeed <= 0.0) instantSpeed else (0.35 * instantSpeed + 0.65 * smoothedSpeed)
                            val speedBps = smoothedSpeed.toLong().coerceAtLeast(0L)
                            val eta = if (total > position && speedBps > 0) (total - position) / speedBps else null

                            onProgress(position, total, speedBps, eta)
                            lastReportTime = now
                            lastReportBytes = position
                        }
                    }
                    // A range that stops short is not fatal on its own — the next
                    // pass simply asks for what is left — but one that yields
                    // nothing at all would loop here forever.
                    if (readForChunk == 0L) error("Download stalled at ${position}B")
                }
            }
            output.flush()
        }

        val finalSpeed = smoothedSpeed.toLong().coerceAtLeast(0L)
        onProgress(position, total, finalSpeed, 0L)
        position
    }

    /** Overload for raw OutputStream sinks (e.g. temporary ringtone caching). */
    suspend fun fetch(
        videoId: String,
        stream: StreamResolver.Stream,
        maxKbps: Int,
        sink: OutputStream,
        onProgress: (written: Long, total: Long) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        var url = stream.url
        val total = contentLength(url) ?: error("Track unavailable: no length to fetch")
        var position = 0L
        var reresolved = false
        val buffer = ByteArray(BUFFER_BYTES)

        while (position < total) {
            coroutineContext.ensureActive()
            val length = minOf(CHUNK_BYTES, total - position)
            val response = try {
                open(url, position, length)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "range at $position failed for $videoId: ${e.message}")
                throw e
            }
            if (response.code in REFUSAL_CODES) {
                response.close()
                StreamResolver.onPlaybackRefused(url, response.code)
                if (reresolved) error("Download refused after ${position}B (HTTP ${response.code})")
                reresolved = true
                url = StreamResolver.resolveForDownload(videoId, maxKbps).url
                if (contentLength(url) != total) error("The stream changed mid-download — try again")
                continue
            }
            response.use {
                if (it.code !in 200..299) error("Download failed (HTTP ${it.code})")
                val body = it.body ?: error("Download failed: empty response")
                val source = body.byteStream()
                var readForChunk = 0L
                while (readForChunk < length) {
                    coroutineContext.ensureActive()
                    val wanted = minOf(buffer.size.toLong(), length - readForChunk).toInt()
                    val read = source.read(buffer, 0, wanted)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                    readForChunk += read
                    position += read
                    onProgress(position, total)
                }
                if (readForChunk == 0L) error("Download stalled at ${position}B")
            }
        }
        sink.flush()
        position
    }

    /**
     * Fetch all of [url] into [sink], for a stream that isn't googlevideo's.
     *
     * Deliberately not a parameterised [fetch], on two counts that both matter:
     *
     *  - **One GET, not ranges.** The chunk loop above exists to defeat
     *    googlevideo's pacing, and nothing else paces bytes that way. Worse, a
     *    server that ignores `Range` answers 200 with the whole file rather
     *    than 206 with the slice asked for  and that loop would then write the
     *    opening of the file into the middle of the output and commit something
     *    the right length and unplayable.
     *  - **The source's own headers.** A source hands them over alongside the
     *    URL  see [SourceStream.headers][com.music.dhvani.data.sources.SourceStream.headers]
     *     and they are whatever that server binds its links to. The
     *    [PlayerClient] identity [fetch] recovers from a googlevideo URL means
     *    nothing here.
     *
     * There is no re-resolve on a refusal either. [fetch] has one because a
     * googlevideo URL is minted against a session that can be stood down
     * mid-download; a source's URL that stops working has a server behind it
     * with its own reasons, and the caller falling back to YouTube is a better
     * answer than asking again.
     *
     * @param onProgress called as bytes land, and only when the response stated
     *   a length to measure against  an unstated one leaves the progress
     * @return how many bytes were written.
     */
    suspend fun fetchDirect(
        url: String,
        headers: Map<String, String>,
        destination: DownloadStore.Pending,
        onProgress: (written: Long, total: Long, bytesPerSec: Long, etaSeconds: Long?) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        val startOffset = destination.existingBytes
        val requestBuilder = Request.Builder().url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }

        if (startOffset > 0) {
            requestBuilder.header("Range", "bytes=$startOffset-")
            Log.d(TAG, "attempting resume from offset ${startOffset}B for $url")
        }

        var response = Http.client.newCall(requestBuilder.build()).execute()

        // If range was requested but server replied with 416 (Range Not Satisfiable),
        // file length may have changed or offset was at/beyond EOF. Fall back to clean fetch.
        if (startOffset > 0 && response.code == 416) {
            response.close()
            Log.w(TAG, "range $startOffset rejected with 416; restarting from 0")
            destination.truncate()
            val freshRequest = Request.Builder().url(url)
                .apply { headers.forEach { (name, value) -> header(name, value) } }
                .build()
            response = Http.client.newCall(freshRequest).execute()
        }

        response.use { resp ->
            if (resp.code !in 200..299) error("Download failed (HTTP ${resp.code})")
            val body = resp.body ?: error("Download failed: empty response")

            val isResumed = resp.code == 206 && startOffset > 0
            if (!isResumed && startOffset > 0) {
                Log.w(TAG, "server returned HTTP ${resp.code} instead of 206 for resumed download at $startOffset; resetting destination to 0")
                destination.truncate()
            }
            val total = if (isResumed) {
                // Header format: Content-Range: bytes 30000-59999/60000
                resp.header("Content-Range")?.substringAfterLast('/')?.trim()?.toLongOrNull()
                    ?: (startOffset + body.contentLength()).takeIf { it > startOffset }
            } else {
                resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0 }
            }

            var written = if (isResumed) startOffset else 0L
            val sink = destination.openStream(append = isResumed)

            sink.use { output ->
                val source = body.byteStream()
                val buffer = ByteArray(BUFFER_BYTES)

                var lastReportTime = SystemClock.elapsedRealtime()
                var lastReportBytes = written
                var smoothedSpeed = 0.0

                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    written += read

                    val now = SystemClock.elapsedRealtime()
                    val timeDelta = now - lastReportTime
                    if (timeDelta >= 150L || (total != null && written >= total)) {
                        val bytesDelta = written - lastReportBytes
                        val instantSpeed = if (timeDelta > 0) (bytesDelta * 1000.0) / timeDelta else 0.0
                        smoothedSpeed = if (smoothedSpeed <= 0.0) instantSpeed else (0.35 * instantSpeed + 0.65 * smoothedSpeed)
                        val speedBps = smoothedSpeed.toLong().coerceAtLeast(0L)
                        val eta = if (total != null && total > written && speedBps > 0) {
                            (total - written) / speedBps
                        } else null

                        onProgress(written, total ?: -1L, speedBps, eta)
                        lastReportTime = now
                        lastReportBytes = written
                    }
                }
                output.flush()

                val finalSpeed = smoothedSpeed.toLong().coerceAtLeast(0L)
                onProgress(written, total ?: written, finalSpeed, 0L)
            }

            // A stated length that didn't all arrive is the one failure the
            // pending-row dance cannot catch on its own: nothing threw, so
            // committing would publish a file that looks whole and stops
            // halfway through the song.
            if (total != null && written < total) {
                error("Download stopped at ${written}B of $total — try again")
            }
            if (written == 0L) error("Download failed: nothing was sent")
            Log.d(TAG, "fetched ${written}B directly (resumed: $isResumed, from: ${startOffset}B)")
            written
        }
    }

    /** Overload for raw OutputStream sinks (e.g. temporary ringtone caching). */
    suspend fun fetchDirect(
        url: String,
        headers: Map<String, String>,
        sink: OutputStream,
        onProgress: (written: Long, total: Long) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        Http.client.newCall(request).execute().use { resp ->
            if (resp.code !in 200..299) error("Download failed (HTTP ${resp.code})")
            val body = resp.body ?: error("Download failed: empty response")
            val total = resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0 } ?: -1L
            val source = body.byteStream()
            val buffer = ByteArray(BUFFER_BYTES)
            var written = 0L

            while (true) {
                coroutineContext.ensureActive()
                val read = source.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                written += read
                onProgress(written, total)
            }
            sink.flush()
            if (total > 0 && written < total) {
                error("Download stopped at ${written}B of $total")
            }
            if (written == 0L) error("Download failed: nothing was sent")
            written
        }
    }

    private fun open(url: String, position: Long, length: Long) = Http.client
        .newCall(
            Request.Builder()
                .url(url)
                .header("Range", "bytes=$position-${position + length - 1}")
                .apply {
                    PlayerClient.forStreamUrl(url).mediaHeaders()
                        .forEach { (name, value) -> header(name, value) }
                }
                .build(),
        )
        .execute()

    /**
     * How many bytes the whole track is.
     *
     * Every progressive googlevideo URL carries it as `clen`, which costs no
     * request at all. The `bytes=0-0` probe behind it is for the URLs that
     * don't  the extraction failsafe can produce one  and reads the total out
     * of the `Content-Range` header of a one-byte response.
     */
    private fun contentLength(url: String): Long? {
        url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { return it }

        return runCatching {
            open(url, 0, 1).use { response ->
                response.header("Content-Range")
                    ?.substringAfter('/', "")
                    ?.toLongOrNull()
                    ?.takeIf { it > 0 }
            }
        }.onFailure { Log.w(TAG, "could not measure the track: ${it.message}") }.getOrNull()
    }

    private val REFUSAL_CODES = setOf(403, 404, 410)
}
