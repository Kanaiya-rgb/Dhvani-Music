package com.music.dhvani.data.sources

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Metadata extracted directly from a FLAC container's STREAMINFO block.
 */
data class FlacStreamInfo(
    val sampleRateHz: Int,
    val channels: Int,
    val bitDepth: Int,
    val totalSamples: Long,
) {
    /** True only if bit depth > 16 bits or sample rate > 48 kHz. */
    val isHiRes: Boolean get() = bitDepth > 16 || sampleRateHz > 48000
}

/**
 * Validates and extracts true FLAC container audio properties by reading the
 * STREAMINFO header from stream bytes or via a minimal HTTP range request.
 *
 * Never trusts provider labels: bit depth and sample rate are verified against
 * the genuine FLAC bitstream structure.
 */
object FlacStreamInfoReader {
    private const val TAG = "DhvaniMusic"

    /**
     * Parses the FLAC STREAMINFO metadata block from raw header bytes.
     * The FLAC format specification requires:
     * - Bytes 0..3: magic 'fLaC' (0x66, 0x4C, 0x61, 0x43)
     * - Byte 4: block header flags & block type (0 = STREAMINFO)
     * - Bytes 5..7: block length (34 bytes for STREAMINFO)
     * - Bytes 8..41: STREAMINFO payload:
     *     - Byte 18: sample rate [19..12]
     *     - Byte 19: sample rate [11..4]
     *     - Byte 20: sample rate [3..0] (high 4) | channels - 1 (3 bits) | bit depth - 1 [4] (1 bit)
     *     - Byte 21: bit depth - 1 [3..0] (high 4) | total samples [35..32]
     */
    fun parse(bytes: ByteArray): FlacStreamInfo? {
        if (bytes.size < 26) return null
        // Check magic 'fLaC'
        if (bytes[0] != 0x66.toByte() || bytes[1] != 0x4C.toByte() ||
            bytes[2] != 0x61.toByte() || bytes[3] != 0x43.toByte()
        ) {
            return null
        }
        // First block must be STREAMINFO (type 0)
        val blockHeader = bytes[4].toInt() and 0xFF
        val blockType = blockHeader and 0x7F
        if (blockType != 0) return null

        val b18 = bytes[18].toInt() and 0xFF
        val b19 = bytes[19].toInt() and 0xFF
        val b20 = bytes[20].toInt() and 0xFF
        val b21 = bytes[21].toInt() and 0xFF

        val sampleRate = (b18 shl 12) or (b19 shl 4) or ((b20 and 0xF0) ushr 4)
        val channels = ((b20 and 0x0E) ushr 1) + 1
        val bitDepth = (((b20 and 0x01) shl 4) or ((b21 and 0xF0) ushr 4)) + 1
        val totalSamples = ((b21.toLong() and 0x0F) shl 32) or
            ((bytes[22].toLong() and 0xFF) shl 24) or
            ((bytes[23].toLong() and 0xFF) shl 16) or
            ((bytes[24].toLong() and 0xFF) shl 8) or
            (bytes[25].toLong() and 0xFF)

        if (sampleRate <= 0 || bitDepth <= 0 || channels <= 0) return null
        return FlacStreamInfo(
            sampleRateHz = sampleRate,
            channels = channels,
            bitDepth = bitDepth,
            totalSamples = totalSamples,
        )
    }

    /**
     * Inspects a FLAC stream URL by requesting only the first 128 bytes via HTTP Range.
     * Fast, lightweight (~5ms) and does not buffer audio payload.
     */
    suspend fun inspectUrl(url: String): FlacStreamInfo? = kotlinx.coroutines.withTimeoutOrNull(2500L) {
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-127")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0")
                .get()
                .build()
            Http.client.newCall(req).execute().use { response ->
                if (!response.isSuccessful && response.code !in 200..206) return@withContext null
                val body = response.body ?: return@withContext null
                val bytes = body.byteStream().use { input ->
                    val buffer = ByteArray(64)
                    var read = 0
                    while (read < 26) {
                        val count = input.read(buffer, read, 26 - read)
                        if (count < 0) break
                        read += count
                    }
                    if (read >= 26) buffer else null
                } ?: return@withContext null
                parse(bytes)
            }
            } catch (e: Exception) {
                TrackLog.w(TAG, "FlacStreamInfoReader failed for $url: ${e.message}")
                null
            }
        }
    }
}
