package com.music.dhvani.data.lyrics

import android.content.Context
import android.util.LruCache
import com.music.dhvani.data.Http
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response

/**
 * On-demand lyric romanization and phonetic pronunciation generation.
 * Transliterates non-Latin scripts (Hindi/Devanagari, Punjabi/Gurmukhi, Korean/Hangul,
 * Japanese/Kanji/Kana, Cyrillic, Chinese, Arabic, Telugu, Tamil, etc.) into Latin alphabets.
 */
object LyricsRomanization {
    sealed interface Result {
        data class Romanized(
            val lines: List<LyricLine>,
            val sourceLanguage: String,
            val fromCache: Boolean,
        ) : Result

        data class AlreadyLatin(val language: String) : Result
        data object Unavailable : Result
    }

    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val CACHE_VERSION = 1
    private const val CACHE_DIRECTORY = "lyrics_romanization_v1"
    private const val MAX_CACHE_BYTES = 2L * 1024L * 1024L
    private const val MAX_BATCH_CHARS = 3_500
    private const val MAX_PARALLEL_REQUESTS = 2
    private val json = Json { ignoreUnknownKeys = true }
    private val diskMutex = Mutex()
    private val memory = LruCache<String, CachedRomanization>(50)
    private val client by lazy {
        Http.client.newBuilder()
            .callTimeout(12, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Serializable
    private data class CachedRomanization(
        val version: Int = CACHE_VERSION,
        val sourceLanguage: String,
        val texts: List<String>,
    )

    private data class TextSlot(
        val lineIndex: Int,
        val background: Boolean,
        val text: String,
    )

    private data class Batch(
        val slots: List<TextSlot>,
        val payload: String,
    )

    private data class BatchAnswer(
        val romanizations: List<String>,
        val sourceLanguage: String,
        val hasRomanization: Boolean,
    )

    /**
     * Synchronously checks if romanized lyrics are present in memory cache.
     * Returns the reconstructed [LyricLine] list if cached, or null.
     */
    fun getCached(trackId: String, lines: List<LyricLine>): List<LyricLine>? {
        if (lines.isEmpty()) return null
        val slots = flatten(lines)
        if (slots.isEmpty()) return null
        val cacheKey = cacheKey(trackId, slots)
        val cached = memory.get(cacheKey) ?: return null
        if (cached.version == CACHE_VERSION && cached.texts.size == slots.size) {
            return rebuild(lines, slots, cached.texts)
        }
        return null
    }

    suspend fun romanize(
        context: Context,
        trackId: String,
        lines: List<LyricLine>,
    ): Result {
        if (lines.isEmpty()) return Result.Unavailable

        val slots = flatten(lines)
        if (slots.isEmpty()) return Result.Unavailable

        // If all non-blank lines already contain only Latin characters, digits, and basic punctuation
        val isAlreadyLatin = slots.all { isAsciiOrLatin(it.text) }
        if (isAlreadyLatin) {
            return Result.AlreadyLatin("en")
        }

        val cacheKey = cacheKey(trackId, slots)
        val cached = memory.get(cacheKey) ?: readCache(context, cacheKey)?.also {
            memory.put(cacheKey, it)
        }
        if (cached != null && cached.version == CACHE_VERSION && cached.texts.size == slots.size) {
            return Result.Romanized(
                lines = rebuild(lines, slots, cached.texts),
                sourceLanguage = cached.sourceLanguage,
                fromCache = true,
            )
        }

        val batches = batches(slots)
        val answers = coroutineScope {
            batches.chunked(MAX_PARALLEL_REQUESTS).flatMap { group ->
                group.map { batch -> async { requestBatch(batch) } }.awaitAll()
            }
        }
        if (answers.any { it == null }) return Result.Unavailable
        val complete = answers.filterNotNull()
        if (complete.none { it.hasRomanization }) {
            val src = complete.firstOrNull()?.sourceLanguage.orEmpty().ifBlank { "en" }
            return Result.AlreadyLatin(src)
        }

        val source = complete
            .groupBy { it.sourceLanguage }
            .maxByOrNull { it.value.size }
            ?.key
            .orEmpty()
            .ifBlank { "auto" }

        val rawRomanized = complete.flatMap { it.romanizations }
        val romanized = List(slots.size) { idx ->
            rawRomanized.getOrNull(idx)?.ifBlank { slots[idx].text } ?: slots[idx].text
        }

        val entry = CachedRomanization(
            sourceLanguage = source,
            texts = romanized,
        )
        memory.put(cacheKey, entry)
        writeCache(context, cacheKey, entry)

        return Result.Romanized(
            lines = rebuild(lines, slots, romanized),
            sourceLanguage = source,
            fromCache = false,
        )
    }

    private fun isAsciiOrLatin(text: String): Boolean {
        for (ch in text) {
            if (ch.isLetter()) {
                val script = Character.UnicodeScript.of(ch.code)
                if (script != Character.UnicodeScript.LATIN &&
                    script != Character.UnicodeScript.COMMON &&
                    script != Character.UnicodeScript.INHERITED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun flatten(lines: List<LyricLine>): List<TextSlot> = buildList {
        lines.forEachIndexed { index, line ->
            if (line.text.isNotBlank()) {
                add(TextSlot(lineIndex = index, background = false, text = line.text.trim()))
            }
            line.background?.takeIf { it.text.isNotBlank() }?.let { background ->
                add(TextSlot(lineIndex = index, background = true, text = background.text.trim()))
            }
        }
    }

    private fun batches(slots: List<TextSlot>): List<Batch> {
        val result = mutableListOf<Batch>()
        var current = mutableListOf<TextSlot>()
        var length = 0

        fun flush() {
            if (current.isEmpty()) return
            result += Batch(current.toList(), payload(current))
            current = mutableListOf()
            length = 0
        }

        slots.forEach { slot ->
            val added = slot.text.length + if (current.isEmpty()) 0 else 8
            if (current.isNotEmpty() && length + added > MAX_BATCH_CHARS) flush()
            current += slot
            length += added
        }
        flush()
        return result
    }

    private fun payload(slots: List<TextSlot>): String = buildString {
        slots.forEachIndexed { index, slot ->
            if (index > 0) append('\n').append(marker(index)).append('\n')
            append(slot.text)
        }
    }

    private fun marker(index: Int): String = "\uE000${index.toString().padStart(4, '0')}\uE001"
    private val markerRegex = Regex("""[\r\n\s]*\uE000\s*\d{4}\s*\uE001[\r\n\s]*""")

    private suspend fun requestBatch(batch: Batch): BatchAnswer? {
        val body = FormBody.Builder()
            .add("client", "gtx")
            .add("sl", "auto")
            .add("tl", "en")
            .add("dt", "t")
            .add("dt", "rm")
            .add("q", batch.payload)
            .build()
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", "Dhvani-Music/2.0.7")
            .header("Accept", "application/json")
            .post(body)
            .build()
        val response = try {
            client.newCall(request).awaitBody()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            android.util.Log.w("LyricsRomanization", "Romanization network error", e)
            return null
        }

        return runCatching {
            val root = json.parseToJsonElement(response).jsonArray
            val firstBlock = root.getOrNull(0)?.jsonArray
            val source = root.getOrNull(2)?.jsonPrimitive?.contentOrNull.orEmpty()

            // Find the transliteration element inside root[0] where index 0 is null and index 3/2 has the romanized string
            val romanizedBody = firstBlock?.firstNotNullOfOrNull { item ->
                val arr = runCatching { item.jsonArray }.getOrNull() ?: return@firstNotNullOfOrNull null
                val firstContent = runCatching { arr.getOrNull(0)?.jsonPrimitive?.contentOrNull }.getOrNull()
                if (firstContent == null) {
                    runCatching { arr.getOrNull(3)?.jsonPrimitive?.contentOrNull }.getOrNull()
                        ?: runCatching { arr.getOrNull(2)?.jsonPrimitive?.contentOrNull }.getOrNull()
                } else null
            }

            val hasRomanization = !romanizedBody.isNullOrBlank()
            val textToSplit = romanizedBody ?: batch.payload
            val parts = textToSplit.split(markerRegex).map(String::trim)
            val count = batch.slots.size
            val reconciled = if (parts.size == count) {
                parts.mapIndexed { idx, part ->
                    part.ifBlank { batch.slots[idx].text }
                }
            } else if (parts.size > count) {
                parts.take(count).mapIndexed { idx, part ->
                    part.ifBlank { batch.slots[idx].text }
                }
            } else {
                List(count) { idx ->
                    parts.getOrNull(idx)?.ifBlank { batch.slots[idx].text } ?: batch.slots[idx].text
                }
            }
            BatchAnswer(reconciled, source, hasRomanization)
        }.onFailure {
            android.util.Log.w("LyricsRomanization", "Failed to parse romanization response", it)
        }.getOrNull()
    }

    private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = if (it.isSuccessful) it.body?.string() else null
                    if (!continuation.isActive) return
                    if (body != null) continuation.resume(body)
                    else continuation.resumeWithException(IOException("Romanization HTTP ${it.code}"))
                }
            }
        })
    }

    private fun rebuild(
        original: List<LyricLine>,
        slots: List<TextSlot>,
        romanized: List<String>,
    ): List<LyricLine> {
        val byLine = slots.zip(romanized).groupBy { it.first.lineIndex }
        return original.mapIndexed { index, line ->
            val entries = byLine[index].orEmpty()
            val lead = entries.firstOrNull { !it.first.background }
            val backing = entries.firstOrNull { it.first.background }
            val romanLead = lead?.second?.takeIf { it.isNotBlank() && it != line.text }
            val romanBacking = backing?.second?.takeIf { it.isNotBlank() && it != line.background?.text }
            line.copy(
                romanizedText = romanLead,
                background = line.background?.copy(romanizedText = romanBacking),
            )
        }
    }

    private fun cacheKey(trackId: String, slots: List<TextSlot>): String {
        val source = buildString {
            append(trackId).append('\u0000').append("romanize")
            slots.forEach { append('\u0000').append(it.text) }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private suspend fun readCache(context: Context, key: String): CachedRomanization? =
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val file = File(File(context.cacheDir, CACHE_DIRECTORY), "$key.json.gz")
                if (!file.isFile) return@withLock null
                runCatching {
                    val value = GZIPInputStream(FileInputStream(file)).bufferedReader().use {
                        json.decodeFromString<CachedRomanization>(it.readText())
                    }
                    file.setLastModified(System.currentTimeMillis())
                    value.takeIf { it.version == CACHE_VERSION }
                }.getOrNull()
            }
        }

    private suspend fun writeCache(context: Context, key: String, value: CachedRomanization) =
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val directory = File(context.cacheDir, CACHE_DIRECTORY)
                if (!directory.exists() && !directory.mkdirs()) return@withLock
                val destination = File(directory, "$key.json.gz")
                val temporary = File(directory, "$key.tmp")
                runCatching {
                    GZIPOutputStream(FileOutputStream(temporary)).bufferedWriter().use {
                        it.write(json.encodeToString(value))
                    }
                    if (!temporary.renameTo(destination)) {
                        temporary.copyTo(destination, overwrite = true)
                        temporary.delete()
                    }
                    trimCache(directory)
                }.onFailure { temporary.delete() }
            }
        }

    private fun trimCache(directory: File) {
        val files = directory.listFiles { file -> file.extension == "gz" }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
        var kept = 0L
        files.forEach { file ->
            kept += file.length()
            if (kept > MAX_CACHE_BYTES) file.delete()
        }
    }
}
