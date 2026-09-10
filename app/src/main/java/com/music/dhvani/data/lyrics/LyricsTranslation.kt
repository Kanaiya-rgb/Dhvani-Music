package com.music.dhvani.data.lyrics

import android.util.LruCache
import com.music.dhvani.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale

/**
 * Real-time translation for lyrics using Google Translate single endpoint with in-memory caching.
 * Reconstructs lyric lines preserving original timing via [LyricLine.timingSource].
 */
object LyricsTranslation {
    private val cache = LruCache<String, List<LyricLine>>(100)

    /**
     * Translates a list of [LyricLine]s into [targetLang] (defaulting to the device locale).
     * Retains timing information and delegates character reveal and glow calculations via `timingSource`.
     */
    suspend fun translate(
        lines: List<LyricLine>,
        targetLang: String = Locale.getDefault().language
    ): Result<List<LyricLine>> = withContext(Dispatchers.IO) {
        runCatching {
            if (lines.isEmpty()) return@runCatching emptyList()

            val textKey = lines.joinToString("\n") { it.text }
            val cacheKey = "$targetLang|${textKey.hashCode()}"
            cache.get(cacheKey)?.let { return@runCatching it }

            // Extract non-empty text lines
            val validLines = lines.mapIndexed { idx, line -> idx to line.text }
                .filter { it.second.isNotBlank() }

            if (validLines.isEmpty()) return@runCatching lines

            val separator = "\n===LT_SEP===\n"
            val fullText = validLines.joinToString(separator) { it.second }

            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t"
            val formBody = FormBody.Builder()
                .add("q", fullText)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val rawJson = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Translation failed with code ${response.code}")
                response.body?.string() ?: error("Empty response body")
            }

            val jsonArray = JSONArray(rawJson)
            val sentences = jsonArray.optJSONArray(0) ?: error("Invalid translation format")
            val fullTranslatedBuilder = StringBuilder()
            for (i in 0 until sentences.length()) {
                val sentence = sentences.optJSONArray(i)
                val part = sentence?.optString(0).orEmpty()
                fullTranslatedBuilder.append(part)
            }

            val translatedParts = fullTranslatedBuilder.toString().split("===LT_SEP===")
                .map { it.trim() }

            val translatedMap = mutableMapOf<Int, String>()
            validLines.forEachIndexed { i, (lineIndex, _) ->
                if (i < translatedParts.size) {
                    translatedMap[lineIndex] = translatedParts[i]
                }
            }

            val resultLines = lines.mapIndexed { idx, origLine ->
                val translatedText = translatedMap[idx] ?: origLine.text
                origLine.copy(
                    text = translatedText,
                    timingSource = origLine
                )
            }

            cache.put(cacheKey, resultLines)
            resultLines
        }
    }

    fun clearCache() {
        cache.evictAll()
    }
}
