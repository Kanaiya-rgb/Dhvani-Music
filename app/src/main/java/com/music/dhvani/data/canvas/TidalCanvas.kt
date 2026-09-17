package com.music.dhvani.data.canvas

import com.music.dhvani.data.Http
import com.music.dhvani.data.lyrics.LyricsCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder

/**
 * Resolves motion canvas video artwork from Tidal's public API.
 * Uses public embed token `vNVdglQOjFJJGG2U` and maps `videoCover` UUIDs
 * from tracks and albums to direct CDN video URLs.
 */
object TidalCanvas {
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"
    private const val API_SEARCH_URL = "https://api.tidal.com/v1/search"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetch(title: String, artist: String, album: String? = null): CanvasArtwork? =
        withContext(Dispatchers.IO) {
            val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
            val cleanArtist = LyricsCleaner.cleanArtist(artist)

            val queries = listOf(
                "$cleanTitle $cleanArtist".trim(),
                cleanTitle.trim(),
                "${title.trim()} ${artist.trim()}".trim(),
                title.trim(),
            ).distinct().filter { it.isNotBlank() }

            for (query in queries) {
                val hit = searchTidal(query, title, artist, album)
                if (hit != null) return@withContext hit
            }

            null
        }

    private fun searchTidal(
        query: String,
        wantTitle: String,
        wantArtist: String,
        wantAlbum: String?,
    ): CanvasArtwork? {
        val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrNull() ?: return null
        val url = "$API_SEARCH_URL?query=$encoded&types=TRACKS,ALBUMS&limit=10&countryCode=US"

        val request = Request.Builder()
            .url(url)
            .header("x-tidal-token", TIDAL_TOKEN)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .build()

        return runCatching {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                parseSearchResponse(body, wantTitle, wantArtist, wantAlbum)
            }
        }.getOrNull()
    }

    private fun parseSearchResponse(
        jsonString: String,
        wantTitle: String,
        wantArtist: String,
        wantAlbum: String?,
    ): CanvasArtwork? {
        return runCatching {
            val root = json.parseToJsonElement(jsonString).jsonObject

            // 1. Check tracks
            val tracksObj = root["tracks"]?.jsonObject
            val trackItems = tracksObj?.get("items")?.jsonArray
            if (trackItems != null) {
                for (elem in trackItems) {
                    val trackObj = elem.jsonObject
                    val trackTitle = trackObj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val artistsArray = trackObj["artists"]?.jsonArray
                    val artistName = if (artistsArray != null && artistsArray.isNotEmpty()) {
                        artistsArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.joinToString(", ")
                    } else {
                        trackObj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    }

                    val albumObj = trackObj["album"]?.jsonObject
                    val albumName = albumObj?.get("title")?.jsonPrimitive?.contentOrNull

                    val candidate = CanvasArtwork(
                        url = "",
                        title = trackTitle,
                        artist = artistName,
                        album = albumName,
                        source = CanvasSource.TIDAL,
                    )

                    if (!candidate.matches(wantTitle, wantArtist, wantAlbum)) continue

                    // Check track videoCover, then album videoCover
                    val videoCover = trackObj["videoCover"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
                        ?: albumObj?.get("videoCover")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
                        ?: continue

                    val uuidPath = videoCover.replace("-", "/")
                    val videoUrl = "https://resources.tidal.com/videos/$uuidPath/1280x1280.mp4"

                    return CanvasArtwork(
                        url = videoUrl,
                        fallbackUrl = null,
                        title = trackTitle,
                        artist = artistName,
                        album = albumName,
                        source = CanvasSource.TIDAL,
                    )
                }
            }

            // 2. Check albums
            val albumsObj = root["albums"]?.jsonObject
            val albumItems = albumsObj?.get("items")?.jsonArray
            if (albumItems != null) {
                for (elem in albumItems) {
                    val albumObj = elem.jsonObject
                    val albumTitle = albumObj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val artistsArray = albumObj["artists"]?.jsonArray
                    val artistName = if (artistsArray != null && artistsArray.isNotEmpty()) {
                        artistsArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.joinToString(", ")
                    } else {
                        albumObj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    }

                    val candidate = CanvasArtwork(
                        url = "",
                        title = albumTitle,
                        artist = artistName,
                        album = albumTitle,
                        source = CanvasSource.TIDAL,
                    )

                    if (!candidate.matches(wantTitle, wantArtist, wantAlbum)) continue

                    val videoCover = albumObj["videoCover"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
                        ?: continue

                    val uuidPath = videoCover.replace("-", "/")
                    val videoUrl = "https://resources.tidal.com/videos/$uuidPath/1280x1280.mp4"

                    return CanvasArtwork(
                        url = videoUrl,
                        fallbackUrl = null,
                        title = albumTitle,
                        artist = artistName,
                        album = albumTitle,
                        source = CanvasSource.TIDAL,
                    )
                }
            }

            null
        }.getOrNull()
    }
}
