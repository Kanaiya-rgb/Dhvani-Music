package com.music.dhvani.data.canvas

import com.metrolist.spotify.SpotifyAuth
import com.music.dhvani.data.Http
import com.music.dhvani.data.lyrics.LyricsCleaner
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

/**
 * Resolves motion canvas video artwork from Spotify's internal Canvaz API.
 * Uses the configured Spotify sp_dc cookie or Bearer token from [AppSettings.spotifySpdcToken].
 */
object SpotifyCanvas {
    private const val CANVAZ_URL = "https://spclient.wg.spotify.com/canvaz-cache/v0/canvases"
    private const val SPOTIFY_SEARCH_URL = "https://api.spotify.com/v1/search"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val tokenMutex = Mutex()

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var tokenExpiryMs: Long = 0L

    /**
     * Checks if a Spotify token or sp_dc cookie is configured.
     */
    fun isConfigured(): Boolean {
        return AppSettings.spotifySpdcToken.value.isNotBlank()
    }

    suspend fun fetch(title: String, artist: String, album: String? = null): CanvasArtwork? =
        withContext(Dispatchers.IO) {
            val tokenConfig = AppSettings.spotifySpdcToken.value.trim()
            if (tokenConfig.isBlank()) return@withContext null

            val accessToken = getAccessToken(tokenConfig) ?: return@withContext null

            val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
            val cleanArtist = LyricsCleaner.cleanArtist(artist)

            val queries = listOf(
                "$cleanTitle $cleanArtist".trim(),
                cleanTitle.trim(),
                "${title.trim()} ${artist.trim()}".trim(),
                title.trim(),
            ).distinct().filter { it.isNotBlank() }

            for (query in queries) {
                val trackUri = searchSpotifyTrackUri(accessToken, query, title, artist, album)
                if (trackUri != null) {
                    val canvas = fetchCanvasForTrack(accessToken, trackUri, title, artist, album)
                    if (canvas != null) return@withContext canvas
                }
            }

            null
        }

    private suspend fun getAccessToken(rawToken: String): String? {
        val trimmed = rawToken.trim()
        if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            return trimmed.substringAfter(" ").trim()
        }
        // If it's already an OAuth access token (starts with BQ or doesn't look like sp_dc)
        if (trimmed.startsWith("BQ", ignoreCase = false) || (!trimmed.contains(";") && trimmed.length < 100)) {
            return trimmed
        }

        // Otherwise handle as sp_dc cookie via SpotifyAuth
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < tokenExpiryMs - 60_000L) {
            return cachedAccessToken
        }

        return tokenMutex.withLock {
            val lockNow = System.currentTimeMillis()
            if (cachedAccessToken != null && lockNow < tokenExpiryMs - 60_000L) {
                return@withLock cachedAccessToken
            }

            runCatching {
                val result = SpotifyAuth.fetchAccessToken(trimmed).getOrThrow()
                cachedAccessToken = result.accessToken
                tokenExpiryMs = result.accessTokenExpirationTimestampMs
                result.accessToken
            }.getOrNull() ?: trimmed // Fallback to raw value if TOTP exchange fails
        }
    }

    private suspend fun searchSpotifyTrackUri(
        accessToken: String,
        query: String,
        wantTitle: String,
        wantArtist: String,
        wantAlbum: String?,
    ): String? {
        com.metrolist.spotify.Spotify.accessToken = accessToken
        val searchResult = runCatching { com.metrolist.spotify.Spotify.search(query).getOrNull() }.getOrNull()
        val items = searchResult?.tracks?.items.orEmpty()

        for (track in items) {
            val trackTitle = track.name
            val artistName = track.artists.joinToString(", ") { it.name }
            val albumName = track.album?.name
            val uri = track.uri?.takeIf { it.isNotBlank() } ?: "spotify:track:${track.id}"

            val candidate = CanvasArtwork(
                url = "",
                title = trackTitle,
                artist = artistName,
                album = albumName,
                source = CanvasSource.SPOTIFY,
            )

            if (candidate.matches(wantTitle, wantArtist, wantAlbum)) {
                return uri
            }
        }
        return null
    }

    private fun fetchCanvasForTrack(
        accessToken: String,
        trackUri: String,
        title: String,
        artist: String,
        album: String?,
    ): CanvasArtwork? {
        val requestBytes = buildCanvasRequestProtobuf(trackUri)
        val requestBody = requestBytes.toRequestBody("application/x-protobuf".toMediaType())

        val request = Request.Builder()
            .url(CANVAZ_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", CANVAS_UA)
            .header("Content-Type", "application/x-protobuf")
            .post(requestBody)
            .build()

        return runCatching {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.body?.bytes() ?: return@use null
                val canvasUrl = extractCanvasUrl(bytes) ?: return@use null

                CanvasArtwork(
                    url = canvasUrl,
                    title = title,
                    artist = artist,
                    album = album,
                    source = CanvasSource.SPOTIFY,
                )
            }
        }.getOrNull()
    }

    /**
     * Serializes CanvasRequest protobuf message:
     * message CanvasRequest {
     *   message Track { string track_uri = 1; }
     *   repeated Track tracks = 1;
     * }
     */
    private fun buildCanvasRequestProtobuf(trackUri: String): ByteArray {
        val uriBytes = trackUri.toByteArray(Charsets.UTF_8)
        val trackStream = ByteArrayOutputStream()
        // Track.track_uri -> field 1, wire type 2 (0x0A)
        trackStream.write(0x0A)
        writeVarint(trackStream, uriBytes.size)
        trackStream.write(uriBytes)
        val trackBytes = trackStream.toByteArray()

        val reqStream = ByteArrayOutputStream()
        // CanvasRequest.tracks -> field 1, wire type 2 (0x0A)
        reqStream.write(0x0A)
        writeVarint(reqStream, trackBytes.size)
        reqStream.write(trackBytes)
        return reqStream.toByteArray()
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    /**
     * Extracts the canvas_url from Spotify's binary protobuf response.
     * Searches both for protobuf field 2 (tag 0x12) and fallback direct URL match.
     */
    private fun extractCanvasUrl(bytes: ByteArray): String? {
        val responseText = runCatching { String(bytes, Charsets.ISO_8859_1) }.getOrNull() ?: ""
        
        // Fast URL pattern match for canvaz CDN MP4 video
        val regex = Regex("""https://canvaz\.scdn\.co/upload/[^\s\"'<>\x00-\x1F\x7F-\xFF]+\.mp4""")
        val match = regex.find(responseText)
        if (match != null) {
            return match.value
        }

        // Generic https .mp4 fallback within response
        val fallbackRegex = Regex("""https://[^\s\"'<>\x00-\x1F\x7F-\xFF]+\.(?:mp4|cnvs\.mp4)""")
        return fallbackRegex.find(responseText)?.value
    }
}
