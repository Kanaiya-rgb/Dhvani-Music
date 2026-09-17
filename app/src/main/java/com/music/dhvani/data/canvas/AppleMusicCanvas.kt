package com.music.dhvani.data.canvas

import android.util.Log
import com.music.dhvani.data.Http
import com.music.dhvani.data.lyrics.LyricsCleaner
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.Base64
import java.util.Locale

/**
 * Apple Music catalog motion artwork engine.
 * Resolves both track-level and album-level HLS/MP4 motion canvases (`editorialVideo`).
 */
object AppleMusicCanvas {
    private const val TAG = "AppleMusicCanvas"
    private const val AMP = "https://amp-api.music.apple.com/v1/catalog"
    private const val WEB_PLAYER = "https://music.apple.com/us/browse"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val storefront: String by lazy {
        Locale.getDefault().country.takeIf { it.length == 2 }?.lowercase(Locale.ROOT) ?: "us"
    }

    suspend fun fetch(title: String, artist: String, album: String? = null): CanvasArtwork? =
        withContext(Dispatchers.IO) {
            val bearer = token() ?: return@withContext null

            val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
            val cleanArtist = LyricsCleaner.cleanArtist(artist)

            searchAppleMusic(cleanTitle, cleanArtist, album, bearer)
                ?: if (cleanTitle != title || cleanArtist != artist) {
                    searchAppleMusic(title, artist, album, bearer)
                } else null
        }

    private fun searchAppleMusic(
        title: String,
        artist: String,
        album: String?,
        bearer: String,
    ): CanvasArtwork? {
        val term = buildString {
            if (!title.contains(artist, ignoreCase = true)) append(artist).append(' ')
            append(title)
            if (!album.isNullOrBlank() && !title.contains(album, ignoreCase = true)) {
                append(' ').append(album)
            }
        }.trim()

        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "songs")
            .addQueryParameter("limit", "10")
            .addQueryParameter("extend", "editorialVideo")
            .addQueryParameter("include", "albums")
            .build()
            .toString()

        val body = get(url, bearer) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val hits = root["results"]?.jsonObject
            ?.get("songs")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return null

        val ranked = hits.mapNotNull { hit ->
            val song = hit as? JsonObject ?: return@mapNotNull null
            val score = score(song, title, artist, album) ?: return@mapNotNull null
            score to song
        }.sortedByDescending { it.first }

        for ((score, song) in ranked) {
            if (score < MIN_SCORE) break
            val attributes = song["attributes"]?.jsonObject ?: continue
            val songName = attributes["name"]?.jsonPrimitive?.contentOrNull
            val songArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull
            val albumName = attributes["albumName"]?.jsonPrimitive?.contentOrNull

            // 1. Inline editorial video on song
            attributes["editorialVideo"]?.jsonObject?.let { video ->
                motionUrls(video)?.let { (primary, alternate) ->
                    Log.d(TAG, "inline motion artwork for '$songName'")
                    return CanvasArtwork(
                        url = primary,
                        fallbackUrl = alternate,
                        title = songName,
                        artist = songArtist,
                        album = albumName,
                        source = CanvasSource.APPLE_MUSIC,
                    )
                }
            }

            // 2. Resolve album editorial video
            val albumId = albumId(song) ?: continue
            fetchAlbum(albumId, bearer, songName, songArtist)?.let { return it }
        }
        return null
    }

    fun searchAlbum(album: String, artist: String): CanvasArtwork? {
        val bearer = token() ?: return null
        val term = if (album.contains(artist, ignoreCase = true)) album else "$artist $album"

        val url = "$AMP/$storefront/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "albums")
            .addQueryParameter("limit", "10")
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(url, bearer) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val hits = root["results"]?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray
            ?: return null

        val ranked = hits.mapNotNull { hit ->
            val record = hit as? JsonObject ?: return@mapNotNull null
            val score = score(record, album, artist, album, albumIsSelf = true)
                ?: return@mapNotNull null
            score to record
        }.sortedByDescending { it.first }

        for ((score, record) in ranked) {
            if (score < MIN_SCORE) break
            val attributes = record["attributes"]?.jsonObject ?: continue
            val name = attributes["name"]?.jsonPrimitive?.contentOrNull
            if (name != null && isCompilation(name)) continue
            val video = attributes["editorialVideo"]?.jsonObject ?: continue
            val (primary, alternate) = motionUrls(video) ?: continue

            return CanvasArtwork(
                url = primary,
                fallbackUrl = alternate,
                title = name,
                artist = attributes["artistName"]?.jsonPrimitive?.contentOrNull,
                album = name,
                source = CanvasSource.APPLE_MUSIC,
            )
        }
        return null
    }

    private const val MIN_SCORE = 12

    private fun score(
        song: JsonObject,
        title: String,
        artist: String,
        album: String?,
        albumIsSelf: Boolean = false,
    ): Int? {
        val attributes = song["attributes"]?.jsonObject ?: return null
        val hitName = attributes["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val hitArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val hitAlbum = if (albumIsSelf) {
            hitName
        } else {
            attributes["albumName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }

        if (isCompilation(hitName) || isCompilation(hitAlbum)) return null

        val wanted = splitArtists(artist)
        val credited = splitArtists(hitArtist)
        if (wanted.isEmpty() || credited.isEmpty()) return null
        val artistMatches = LyricsCleaner.isArtistMatch(hitArtist, artist) ||
            wanted.any { want: String -> credited.any { cred: String -> cred == want || cred.contains(want) || want.contains(cred) } }
        if (!artistMatches) return null

        var score = 10

        val wantTitle = CanvasArtwork.normalize(LyricsCleaner.cleanTitle(title, artist))
        val hitTitle = CanvasArtwork.normalize(hitName)
        score += when {
            hitTitle == wantTitle -> 15
            LyricsCleaner.isTitleMatch(hitName, title) -> 12
            hitTitle.contains(wantTitle) || wantTitle.contains(hitTitle) -> 7
            else -> -10
        }

        if (!album.isNullOrBlank() && hitAlbum.isNotBlank()) {
            val wantAlbum = CanvasArtwork.normalize(album)
            val gotAlbum = CanvasArtwork.normalize(hitAlbum)
            score += when {
                gotAlbum == wantAlbum -> 20
                gotAlbum.contains(wantAlbum) || wantAlbum.contains(gotAlbum) -> 10
                else -> 0
            }
        }

        for (word in EDITION_WORDS) {
            val inWanted = title.contains(word, ignoreCase = true)
            val inHit = hitName.contains(word, ignoreCase = true)
            if (inWanted && inHit) score += 5 else if (inHit) score -= 3
        }

        return score
    }

    private val EDITION_WORDS =
        listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")

    private fun isCompilation(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return COMPILATION_MARKERS.any { lower.contains(it) }
    }

    private val COMPILATION_MARKERS = listOf(
        "playlist", "set list", "essentials", "dj mix", "mixed",
        "apple music", "today's hits", "session",
    )

    private fun albumId(song: JsonObject): String? {
        val fromRelationship = song["relationships"]?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        if (fromRelationship != null) return fromRelationship.takeUnless { it.startsWith("pl.") }

        val url = song["attributes"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            ?: return null
        return url.substringAfter("/album/", "")
            .substringBefore("?")
            .substringAfterLast("/")
            .takeIf { it.isNotBlank() && it.all(Char::isDigit) }
    }

    private fun fetchAlbum(
        albumId: String,
        bearer: String,
        songTitle: String?,
        songArtist: String?,
    ): CanvasArtwork? {
        val url = "$AMP/$storefront/albums/$albumId".toHttpUrl().newBuilder()
            .addQueryParameter("extend", "editorialVideo")
            .build()
            .toString()

        val body = get(url, bearer) ?: return null
        val album = runCatching {
            json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
        }.getOrNull() ?: return null

        val attributes = album["attributes"]?.jsonObject ?: return null
        val albumName = attributes["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (isCompilation(albumName)) return null

        val video = attributes["editorialVideo"]?.jsonObject ?: return null
        val (primary, alternate) = motionUrls(video) ?: return null

        Log.d(TAG, "motion artwork on album '$albumName' ($albumId)")
        return CanvasArtwork(
            url = primary,
            fallbackUrl = alternate,
            title = songTitle,
            artist = songArtist ?: attributes["artistName"]?.jsonPrimitive?.contentOrNull,
            album = albumName,
            source = CanvasSource.APPLE_MUSIC,
        )
    }

    private fun motionUrls(video: JsonObject): Pair<String, String?>? {
        fun link(key: String): String? = video[key]?.jsonObject?.let { asset ->
            asset["video"]?.jsonPrimitive?.contentOrNull
                ?: asset["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["url"]?.jsonPrimitive?.contentOrNull
        }?.takeIf { it.isNotBlank() }

        val square = link("motionDetailSquare") ?: link("motionSquareVideo1x1")
        val raw = link("motionDetailRaw")
        val tall = link("motionDetailTall") ?: link("motionTallVideo3x4")
        val primary = square ?: raw ?: tall ?: return null
        val alternate = listOfNotNull(square, raw, tall).firstOrNull { it != primary }
        return primary to alternate
    }

    // ---- Token ---------------------------------------------------------

    private var cachedToken: String = AppSettings.DEFAULT_APPLE_MUSIC_DEV_TOKEN
    private var tokenExpiresAtMs = 0L
    private var retryTokenAfterMs = 0L
    private val rejected = mutableSetOf<String>()

    @Synchronized
    private fun token(): String? {
        val now = System.currentTimeMillis()
        if (cachedToken.isNotBlank() && cachedToken != AppSettings.DEFAULT_APPLE_MUSIC_DEV_TOKEN && now < tokenExpiresAtMs - 60_000) {
            return cachedToken
        }
        if (now < retryTokenAfterMs) return cachedToken.ifBlank { AppSettings.DEFAULT_APPLE_MUSIC_DEV_TOKEN }

        val html = canvasGet(WEB_PLAYER, mapOf("User-Agent" to CANVAS_UA))
        val scripts = html?.let {
            Regex("""/assets/index(?:-legacy)?[~-][A-Za-z0-9_-]+\.js""")
                .findAll(it).map(MatchResult::value).distinct().toList()
        }.orEmpty()

        for (path in scripts) {
            val script = canvasGet("https://music.apple.com$path", mapOf("User-Agent" to CANVAS_UA))
                ?: continue
            val candidates = Regex("""ey[A-Za-z0-9_-]+\.ey[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
                .findAll(script)
                .map(MatchResult::value)
                .distinct()
                .filter { it !in rejected }
                .mapNotNull { jwt -> expiry(jwt)?.let { jwt to it } }
                .filter { it.second > now }
                .toList()
            if (candidates.isEmpty()) continue

            val (jwt, expiresAt) = candidates.firstOrNull { isWebPlayerToken(it.first) }
                ?: candidates.first()
            cachedToken = jwt
            tokenExpiresAtMs = expiresAt
            Log.d(TAG, "web player token good until ${java.util.Date(expiresAt)}")
            return jwt
        }

        retryTokenAfterMs = now + (30L * 60 * 1000)
        return cachedToken.ifBlank { AppSettings.DEFAULT_APPLE_MUSIC_DEV_TOKEN }
    }

    private fun get(url: String, bearer: String): String? {
        val request = Request.Builder().url(url).apply {
            authHeaders(bearer).forEach { (name, value) -> header(name, value) }
        }.build()
        return runCatching {
            Http.client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> response.body?.string()
                    response.code == 401 -> {
                        Log.w(TAG, "token rejected by the catalog API; will re-scrape")
                        synchronized(this) {
                            rejected += bearer
                            if (cachedToken == bearer) {
                                cachedToken = AppSettings.DEFAULT_APPLE_MUSIC_DEV_TOKEN
                                tokenExpiresAtMs = 0L
                            }
                        }
                        null
                    }
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun isWebPlayerToken(jwt: String): Boolean = runCatching {
        val parts = jwt.split(".")
        val header = String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        header.contains("WebPlayKid") || payload.contains("AMPWebPlay")
    }.getOrDefault(false)

    private fun expiry(jwt: String): Long? = runCatching {
        val payload = String(
            Base64.getUrlDecoder().decode(jwt.split(".")[1]),
            Charsets.UTF_8,
        )
        val seconds = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payload)?.groupValues?.get(1)
        seconds?.toLong()?.times(1000)
    }.getOrDefault(null)

    private fun authHeaders(bearer: String) = mapOf(
        "Authorization" to "Bearer $bearer",
        "Origin" to "https://music.apple.com",
        "Referer" to "https://music.apple.com/",
        "User-Agent" to CANVAS_UA,
    )
}
