package com.music.dhvani.playback

import android.media.MediaCodecList

object AudioDecoderResolver {

    /**
     * Resolves the real system audio decoder for a given format / MIME type.
     * Guards against stale decoders (e.g. an Opus decoder left over from YouTube when playing FLAC).
     */
    fun resolveDecoderName(
        currentDecoder: String?,
        mimeType: String?,
        isLossless: Boolean,
    ): String {
        val isOpus = mimeType?.contains("opus", ignoreCase = true) == true
        val isAac = mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("aac", ignoreCase = true) == true
        val isFlac = !isOpus && !isAac && (isLossless || mimeType?.contains("flac", ignoreCase = true) == true || mimeType?.contains("raw", ignoreCase = true) == true)

        if (!currentDecoder.isNullOrBlank()) {
            val lower = currentDecoder.lowercase()
            val matchesFlac = isFlac && (lower.contains("flac") || (!lower.contains("opus") && !lower.contains("aac")))
            val matchesAac = isAac && (lower.contains("aac") || lower.contains("mp4"))
            val matchesOpus = isOpus && lower.contains("opus")

            if (matchesFlac || matchesAac || matchesOpus) {
                return currentDecoder
            }
        }

        val targetMime = when {
            isOpus -> "audio/opus"
            isAac -> "audio/mp4a-latm"
            isFlac -> "audio/flac"
            mimeType != null -> mimeType
            else -> "audio/opus"
        }

        val platformDecoder = runCatching {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            codecList.codecInfos.firstOrNull { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(targetMime, ignoreCase = true) }
            }?.name
        }.getOrNull()

        if (!platformDecoder.isNullOrBlank()) {
            return platformDecoder
        }

        return when {
            isFlac -> "c2.android.flac.decoder"
            isAac -> "c2.android.aac.decoder"
            else -> "c2.android.opus.decoder"
        }
    }
}
