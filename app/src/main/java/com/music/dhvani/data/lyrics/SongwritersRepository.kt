package com.music.dhvani.data.lyrics

import com.music.dhvani.data.Http
import com.music.dhvani.data.innertube.Innertube
import com.music.dhvani.data.innertube.InnertubeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for fetching authentic song writers, lyricists, and composers
 * from live metadata APIs (JioSaavn, YouTube Video Description, iTunes)
 * when lyrics providers (e.g. LRCLIB, LRC) do not provide explicit TTML agent tags.
 */
object SongwritersRepository {
    private val memoryCache = ConcurrentHashMap<String, List<String>>()

    fun getCached(videoId: String): List<String>? = memoryCache[videoId]

    suspend fun fetchSongwriters(
        videoId: String,
        title: String,
        artist: String,
        currentKnownWriters: List<String> = emptyList(),
    ): List<String> = withContext(Dispatchers.IO) {
        if (currentKnownWriters.isNotEmpty()) {
            memoryCache[videoId] = currentKnownWriters
            return@withContext currentKnownWriters
        }

        memoryCache[videoId]?.let { return@withContext it }

        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)

        // 1. Try JioSaavn details API (contains real composer & lyricist credits)
        val fromJio = runCatching {
            withTimeoutOrNull(3000L) {
                queryJioSaavnSongwriters(cleanTitle, cleanArtist)
            }
        }.getOrNull()

        if (!fromJio.isNullOrEmpty()) {
            memoryCache[videoId] = fromJio
            return@withContext fromJio
        }

        // 2. Try YouTube Video Description via InnerTube ("Lyrics: ...", "Written by: ...", "Composer: ...")
        if (videoId.isNotBlank()) {
            val fromYt = runCatching {
                withTimeoutOrNull(3000L) {
                    queryYouTubeDescription(videoId)
                }
            }.getOrNull()

            if (!fromYt.isNullOrEmpty()) {
                memoryCache[videoId] = fromYt
                return@withContext fromYt
            }
        }

        // 3. Try iTunes Search API
        val fromITunes = runCatching {
            withTimeoutOrNull(3000L) {
                queryITunesSongwriters(cleanTitle, cleanArtist)
            }
        }.getOrNull()

        if (!fromITunes.isNullOrEmpty()) {
            memoryCache[videoId] = fromITunes
            return@withContext fromITunes
        }

        // 4. Fallback to artist list if no external metadata found
        val fallback = if (artist.isNotBlank() && artist != "Unknown") {
            artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.", " / ").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        if (fallback.isNotEmpty()) {
            memoryCache[videoId] = fallback
        }
        fallback
    }

    private fun queryJioSaavnSongwriters(title: String, artist: String): List<String>? {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        val searchUrl = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$query"
        val req = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
        val body = Http.client.newCall(req).execute().use { it.body?.string() } ?: return null
        val json = JSONObject(body)
        val songsData = json.optJSONObject("songs")?.optJSONArray("data") ?: return null
        if (songsData.length() == 0) return null
        val songId = songsData.getJSONObject(0).optString("id")
        if (songId.isNullOrBlank()) return null

        val detailUrl = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$songId&_format=json"
        val req2 = Request.Builder()
            .url(detailUrl)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        val detailBody = Http.client.newCall(req2).execute().use { it.body?.string() } ?: return null
        val dJson = JSONObject(detailBody).optJSONObject(songId) ?: return null

        val writers = LinkedHashSet<String>()
        val starringRaw = dJson.optString("starring")
        val starringList = starringRaw.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }

        val singersRaw = dJson.optString("singers")
        val singersList = singersRaw.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }

        val music = dJson.optString("music").trim()
        if (music.isNotBlank() && !music.equals("null", ignoreCase = true)) {
            music.split(",").forEach {
                val cleaned = it.trim()
                val lower = cleaned.lowercase(Locale.ROOT)
                if (cleaned.isNotBlank() && !starringList.any { lower.contains(it) || it.contains(lower) }) {
                    writers.add(cleaned)
                }
            }
        }
        val artistMap = dJson.optJSONObject("artistMap")
        if (artistMap != null) {
            val keys = artistMap.keys()
            while (keys.hasNext()) {
                val name = keys.next().trim()
                val lower = name.lowercase(Locale.ROOT)
                // Filter out actors/hero/heroine (starring) and singers
                val isActor = starringList.any { lower.contains(it) || it.contains(lower) }
                val isSinger = singersList.any { lower.contains(it) || it.contains(lower) }
                if (name.isNotBlank() && !isActor && !isSinger) {
                    writers.add(name)
                }
            }
        }
        return writers.toList().takeIf { it.isNotEmpty() }
    }

    private suspend fun queryYouTubeDescription(videoId: String): List<String>? {
        val nextJson = Innertube.nextWeb(videoId)
        val desc = InnertubeParser.parseDescription(nextJson) ?: return null
        val regex = Regex("(?i)(?:lyrics|lyricist|written by|songwriters?|composer)\\s*[:\\-–]\\s*([^\\n\\r|,]+)")
        val match = regex.find(desc) ?: return null
        val writer = match.groupValues.getOrNull(1)?.trim() ?: return null
        if (writer.length in 2..50) {
            return listOf(writer)
        }
        return null
    }

    private fun queryITunesSongwriters(title: String, artist: String): List<String>? {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        val body = Http.client.newCall(req).execute().use { it.body?.string() } ?: return null
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val item = results.getJSONObject(0)
        val artistName = item.optString("artistName").trim()
        if (artistName.isNotBlank()) {
            return artistName.split(",", "&", "feat.", "ft.", "Feat.", "Ft.", " / ")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        return null
    }
}

