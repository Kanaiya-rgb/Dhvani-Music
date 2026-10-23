package com.music.dhvani.data.recognition

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.mrsep.musicrecognizer.core.recognition.shazam.SongRecSignature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt
import kotlin.random.Random

sealed interface ShazamResult {
    data class Success(
        val title: String,
        val artist: String,
        val artworkUrl: String? = null,
        val key: String? = null
    ) : ShazamResult

    data object NoMatch : ShazamResult
    data class Error(val message: String) : ShazamResult
}

object ShazamRecognitionClient {
    private const val TAG = "ShazamClient"
    private const val SAMPLE_RATE = 16_000
    private const val RECORD_DURATION_MS = 5000L // 5 seconds is optimal for Shazam fingerprinting
    private const val SAMPLES_COUNT = (SAMPLE_RATE * RECORD_DURATION_MS / 1000).toInt() // 80,000 samples

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val USER_AGENTS = listOf(
        "Dalvik/2.1.0 (Linux; U; Android 14; Pixel 8 Build/UD1A.230803.041)",
        "Dalvik/2.1.0 (Linux; U; Android 13; SM-S918B Build/TP1A.220624.014)",
        "Dalvik/2.1.0 (Linux; U; Android 12; SM-G998B Build/SP1A.210812.016)",
        "Dalvik/2.1.0 (Linux; U; Android 11; Pixel 5 Build/RQ3A.211001.001)"
    )

    private val TIMEZONES = listOf(
        "America/New_York",
        "Europe/London",
        "Asia/Kolkata",
        "Europe/Paris",
        "Asia/Tokyo"
    )

    /**
     * Records audio from the microphone for [RECORD_DURATION_MS] while feeding
     * real-time amplitude values to [onAudioLevel] (range 0.0f..1.0f).
     * Then fingerprints with SongRec and sends to Shazam API.
     */
    @SuppressLint("MissingPermission")
    suspend fun recognizeFromMic(
        onAudioLevel: (Float) -> Unit,
        onProgress: (Float) -> Unit
    ): ShazamResult = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, 4096)
        var audioRecord: AudioRecord? = null

        val pcmData = ShortArray(SAMPLES_COUNT)
        var totalRecorded = 0

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return@withContext ShazamResult.Error("Could not initialize microphone recorder.")
            }

            audioRecord.startRecording()
            Log.d(TAG, "Started recording 16kHz audio for Shazam fingerprinting...")

            val readChunk = ShortArray(1024)
            val startTime = System.currentTimeMillis()

            while (isActive && totalRecorded < SAMPLES_COUNT) {
                val toRead = minOf(readChunk.size, SAMPLES_COUNT - totalRecorded)
                val readResult = audioRecord.read(readChunk, 0, toRead)

                if (readResult > 0) {
                    System.arraycopy(readChunk, 0, pcmData, totalRecorded, readResult)
                    totalRecorded += readResult

                    // Calculate RMS amplitude for real-time visualizer
                    var sumSq = 0.0
                    for (i in 0 until readResult) {
                        val s = readChunk[i].toDouble()
                        sumSq += s * s
                    }
                    val rms = sqrt(sumSq / readResult)
                    // Normalize RMS (typical speech/music RMS is 500..8000)
                    val normalizedLevel = (rms / 3500.0).toFloat().coerceIn(0.05f, 1.0f)
                    onAudioLevel(normalizedLevel)

                    val progress = totalRecorded.toFloat() / SAMPLES_COUNT.toFloat()
                    onProgress(progress)
                } else if (readResult < 0) {
                    Log.e(TAG, "AudioRecord read error: $readResult")
                    break
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Recording exception", e)
            return@withContext ShazamResult.Error("Recording error: ${e.message}")
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {}
            onAudioLevel(0.05f)
        }

        if (totalRecorded < SAMPLE_RATE * 2) {
            return@withContext ShazamResult.Error("Not enough audio captured. Please try again.")
        }

        // Generate Shazam acoustic signature using SongRec JNI
        Log.d(TAG, "Generating acoustic signature from $totalRecorded PCM samples...")
        val signatureUri: String = try {
            val trimmedSamples = if (totalRecorded == pcmData.size) pcmData else pcmData.copyOf(totalRecorded)
            SongRecSignature.fromPcm16Mono16kHz(trimmedSamples)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to compute SongRec fingerprint", e)
            return@withContext ShazamResult.Error("Acoustic fingerprinting failed: ${e.message}")
        }

        if (!signatureUri.startsWith("data:audio/vnd.shazam.sig;base64,")) {
            Log.e(TAG, "Invalid signature URI format: $signatureUri")
            return@withContext ShazamResult.Error("Invalid acoustic signature format.")
        }

        // Send query to Shazam API
        queryShazamApi(signatureUri, (totalRecorded * 1000L / SAMPLE_RATE))
    }

    private fun queryShazamApi(signatureUri: String, sampleDurationMs: Long): ShazamResult {
        return try {
            val timestamp = Instant.now().epochSecond
            val uuid1 = UUID.randomUUID().toString().uppercase()
            val uuid2 = UUID.randomUUID().toString()

            val requestJson = JSONObject().apply {
                put("geolocation", JSONObject().apply {
                    put("altitude", Random.nextDouble() * 300 + 100)
                    put("latitude", Random.nextDouble() * 140 - 70)
                    put("longitude", Random.nextDouble() * 360 - 180)
                })
                put("signature", JSONObject().apply {
                    put("samplems", sampleDurationMs)
                    put("timestamp", timestamp)
                    put("uri", signatureUri)
                })
                put("timestamp", timestamp)
                put("timezone", TIMEZONES.random())
            }

            val url = "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$uuid1/$uuid2" +
                    "?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3"

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENTS.random())
                .header("Content-Language", "en_US")
                .header("Accept", "application/json")
                .post(body)
                .build()

            Log.d(TAG, "Posting signature to Shazam tag endpoint...")
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w(TAG, "Shazam API response unsuccessful: code=${response.code}")
                return ShazamResult.Error("Shazam server returned ${response.code}")
            }

            parseShazamResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Shazam network query failed", e)
            ShazamResult.Error("Connection failed: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun parseShazamResponse(jsonString: String): ShazamResult {
        return try {
            val root = JSONObject(jsonString)
            val track = root.optJSONObject("track")

            if (track == null) {
                val matches = root.optJSONArray("matches")
                if (matches == null || matches.length() == 0) {
                    Log.d(TAG, "No match found by Shazam")
                    return ShazamResult.NoMatch
                }
                return ShazamResult.NoMatch
            }

            val title = track.optString("title", "").trim()
            val subtitle = track.optString("subtitle", "").trim()
            val key = if (track.has("key")) track.optString("key") else null

            if (title.isEmpty() || subtitle.isEmpty()) {
                return ShazamResult.NoMatch
            }

            // Extract artwork
            val images = track.optJSONObject("images")
            val artworkUrl = images?.optString("coverarthq")?.takeIf { it.isNotEmpty() }
                ?: images?.optString("coverart")?.takeIf { it.isNotEmpty() }

            Log.d(TAG, "Identified track: $title by $subtitle (key: $key)")
            ShazamResult.Success(
                title = title,
                artist = subtitle,
                artworkUrl = artworkUrl,
                key = key
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Shazam response", e)
            ShazamResult.Error("Failed to parse recognition results.")
        }
    }
}
