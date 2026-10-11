package com.music.dhvani.ringtone

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.music.dhvani.data.Http
import com.music.dhvani.data.innertube.StreamResolver
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.durationMillis
import com.music.dhvani.download.Downloader
import com.music.dhvani.download.Downloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

object RingtoneHelper {
    private const val TAG = "RingtoneHelper"

    /** Checks if app has permission to write system settings (required to set ringtone). */
    fun canSetRingtone(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /** Opens Android System Settings page to allow WRITE_SETTINGS permission. */
    fun requestWriteSettingsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Prepares the source audio file for trimming.
     * Uses local downloaded audio if present, or downloads audio into cache with progress reporting.
     */
    suspend fun prepareSourceAudioFile(
        context: Context,
        song: Song,
        onProgress: (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        // 1. Check if user already downloaded this song or has local file
        val localUriString = song.localUri ?: Downloads.verifiedSavedUri(song.videoId)
        if (!localUriString.isNullOrBlank()) {
            val localUri = Uri.parse(localUriString)
            if (localUri.scheme == "file") {
                val f = File(localUri.path ?: "")
                if (f.exists() && f.length() > 50_000) return@withContext f
            } else if (localUri.scheme == "content") {
                val cached = File(context.cacheDir, "ringtone_src_${song.videoId}.m4a")
                if (cached.exists() && cached.length() > 50_000) return@withContext cached
                try {
                    context.contentResolver.openInputStream(localUri)?.use { input ->
                        FileOutputStream(cached).use { out -> input.copyTo(out) }
                    }
                    if (cached.exists() && cached.length() > 50_000) return@withContext cached
                } catch (e: Exception) {
                    Log.w(TAG, "Failed reading local content URI: ${e.message}")
                }
            }
        }

        // 2. Check if already cached in ringtone cache
        val cacheFile = File(context.cacheDir, "ringtone_src_${song.videoId}.m4a")
        if (cacheFile.exists() && cacheFile.length() > 100_000) {
            return@withContext cacheFile
        }

        // 3. Resolve audio stream
        val stream = try {
            StreamResolver.resolveForDownload(song.videoId, 256)
        } catch (e: Exception) {
            Log.w(TAG, "StreamResolver.resolveForDownload failed: ${e.message}, trying quick resolve")
            try {
                val direct = StreamResolver.resolve(song.videoId)
                StreamResolver.Stream(direct, 160, "audio/mp4", song.durationMillis())
            } catch (e2: Exception) {
                Log.e(TAG, "StreamResolver.resolve failed: ${e2.message}")
                null
            }
        } ?: return@withContext null

        // 4. Download audio using high-speed 2MB bounded chunks (Downloader)
        // Bypasses YouTube's 15kB/s single-GET pacing, fetching at line rate (~5.7 MB/s)
        try {
            cacheFile.parentFile?.mkdirs()
            FileOutputStream(cacheFile).use { out ->
                Downloader.fetch(
                    videoId = song.videoId,
                    stream = stream,
                    maxKbps = 256,
                    sink = out,
                ) { written, total ->
                    if (total > 0L) {
                        onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 50_000) {
                return@withContext cacheFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Downloader.fetch failed: ${e.message}, trying direct stream fetch")
            runCatching {
                FileOutputStream(cacheFile).use { out ->
                    Downloader.fetchDirect(
                        url = stream.url,
                        headers = emptyMap(),
                        sink = out,
                    ) { written, total ->
                        if (total > 0L) {
                            onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 50_000) {
                return@withContext cacheFile
            }
        }

        null
    }

    /**
     * Trims an audio file losslessly between [startMs] and [endMs] using MediaExtractor and MediaMuxer.
     * Takes ~100-200ms and preserves 100% original audio fidelity without re-encoding artifacts.
     */
    suspend fun trimAudio(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(sourceFile.absolutePath)
            }

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in source file")
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            // Ensure destination parent directory exists
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = runCatching {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            }.getOrDefault(256 * 1024)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming audio: ${e.message}", e)
            false
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }

    /**
     * Saves trimmed audio to Android's Ringtone MediaStore and sets it as the default phone ringtone.
     */
    suspend fun saveAndSetRingtone(
        context: Context,
        trimmedFile: File,
        song: Song,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val fileName = "${song.title.replace(Regex("[^a-zA-Z0-9._ -]"), "_")}_Ringtone.m4a"

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, "${song.title} (Ringtone)")
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.ARTIST, song.artist)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val ringtoneUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false

            contentResolver.openOutputStream(ringtoneUri)?.use { out ->
                FileInputStream(trimmedFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(ringtoneUri, values, null, null)
            }

            // Set as default system ringtone
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                ringtoneUri
            )

            Log.d(TAG, "Successfully set ringtone: $ringtoneUri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save and set ringtone: ${e.message}", e)
            false
        }
    }
}
