package com.music.dhvani.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Line-synced lyrics from Musixmatch.
 *
 * Supports:
 * 1. User token (e.g. from Musixmatch desktop/web or Spicetify) configured in AppSettings.
 * 2. Session token negotiation with mobile / desktop endpoints and browser headers.
 * 3. Fallback to PaxSenix Musixmatch proxy when configured or direct tokens are unavailable.
 * 4. Automatic filtering of Musixmatch honeypot decoy responses (Tatar gibberish / fake tracks).
 */
object Musixmatch {

    private const val BASE_DESKTOP = "https://apic-desktop.musixmatch.com/ws/1.1"
    private const val BASE_APIC = "https://apic.musixmatch.com/ws/1.1"

    private const val SIGNING_SECRET = "RJDefUswhwjkZDeM"

    private val HEADERS = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "x-mxm-token-guid" to "",
        "Connection" to "keep-alive",
        "Accept" to "application/json",
    )

    private val tokenMutex = Mutex()
    private val configuredToken = AtomicReference<String>("")
    private val cachedToken = AtomicReference<String?>(null)

    fun setUserToken(token: String) {
        configuredToken.set(token.trim())
    }

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsCleaner.cleanTitle(title, artist)
        val cleanArtist = LyricsCleaner.cleanArtist(artist)
        val seconds = (durationMs / 1000).toInt()

        // 1. Try direct Musixmatch API
        val directLyrics = tryDirectLyrics(cleanTitle, cleanArtist, seconds)
        if (!directLyrics.isNullOrEmpty()) return@withContext directLyrics

        // 2. Fallback to PaxSenix Musixmatch endpoint if available
        val paxMusix = PaxSenix.musixmatchLyrics(cleanTitle, cleanArtist, durationMs)
        if (!paxMusix.isNullOrEmpty()) return@withContext paxMusix

        null
    }

    private suspend fun tryDirectLyrics(title: String, artist: String, seconds: Int): List<LyricLine>? {
        val token = getToken()

        // Attempt via macro.subtitles.get (fast bundled call)
        if (!token.isNullOrBlank()) {
            val macroLyrics = fetchMacroLyrics(title, artist, seconds, token)
            if (!macroLyrics.isNullOrEmpty()) return macroLyrics
        }

        // Standard search + subtitle retrieval flow
        val track = bestTrack(title, artist, seconds, token) ?: return null
        val subtitle = if (track.hasSubtitles == 1) fetchSubtitle(track.trackId, token) else null
        val lrc = subtitle?.let(::subtitleToLrc)?.takeIf { it.isNotBlank() } ?: return null

        if (isDecoyHoneypot(track.trackName, title, lrc)) return null

        return LrcLib.parseLrc(lrc).takeIf { it.isNotEmpty() }
    }

    private suspend fun fetchMacroLyrics(
        title: String,
        artist: String,
        seconds: Int,
        token: String,
    ): List<LyricLine>? {
        val url = "$BASE_DESKTOP/macro.subtitles.get".toHttpUrl().newBuilder()
            .addQueryParameter("app_id", "web-desktop-app-v1.0")
            .addQueryParameter("q_track", title)
            .addQueryParameter("q_artist", artist)
            .addQueryParameter("q_duration", seconds.toString())
            .addQueryParameter("f_subtitle_length", seconds.toString())
            .addQueryParameter("namespace", "lyrics_richsynched")
            .addQueryParameter("subtitle_format", "mxm")
            .addQueryParameter("usertoken", token)
            .build()

        val response = lyricsGetWithHeaders(sign(url.toString()), HEADERS) ?: return null
        if (looksUnauthorized(response)) return null

        val root = runCatching {
            lyricsJson.decodeFromString<Envelope<MacroCallsBody>>(response)
        }.getOrNull() ?: return null

        val macro = root.message.body?.macroCalls ?: return null
        val track = macro.matcherTrack?.message?.body?.track
        val subtitleBody = macro.trackSubtitles?.message?.body?.subtitleList?.firstOrNull()?.subtitle?.subtitleBody
            ?: macro.trackSubtitle?.message?.body?.subtitle?.subtitleBody
            ?: return null

        if (track != null && isDecoyHoneypot(track.trackName, title, subtitleBody)) return null

        val lrc = subtitleToLrc(subtitleBody).takeIf { it.isNotBlank() } ?: return null
        return LrcLib.parseLrc(lrc).takeIf { it.isNotEmpty() }
    }

    private suspend fun bestTrack(title: String, artist: String, seconds: Int, token: String?): Track? {
        val tracks = searchTrack(title, artist, token) ?: return null
        val best = tracks.maxByOrNull { score(it, title, artist, seconds) } ?: return null
        return best.takeIf { score(it, title, artist, seconds) >= 50.0 }
    }

    private fun score(track: Track, title: String, artist: String, seconds: Int): Double {
        if (!LyricsCleaner.isTitleMatch(track.trackName, title)) return -1000.0
        val artistMatches = LyricsCleaner.isArtistMatch(track.artistName, artist)
        if (!artistMatches && artist.isNotBlank()) return -1000.0

        var score = 50.0
        val name = track.trackName.trim().lowercase(Locale.ROOT)
        val targetTitle = title.trim().lowercase(Locale.ROOT)
        if (name == targetTitle) score += 30.0
        if (artistMatches) {
            score += 30.0
        }
        track.trackLength?.let { length ->
            val diff = abs(length - seconds)
            score += when {
                diff <= 2 -> 20.0
                diff <= 5 -> 10.0
                diff <= 10 -> 0.0
                else -> -30.0
            }
        }
        return score
    }

    private suspend fun searchTrack(title: String, artist: String, token: String?): List<Track>? {
        val search = { currentToken: String? ->
            val urlBuilder = "$BASE_DESKTOP/track.search".toHttpUrl().newBuilder()
                .addQueryParameter("app_id", "web-desktop-app-v1.0")
                .addQueryParameter("q_track", title)
                .addQueryParameter("q_artist", artist)
                .addQueryParameter("f_has_lyrics", "1")
                .addQueryParameter("s_track_rating", "desc")
                .addQueryParameter("quorum_factor", "1")
                .addQueryParameter("page_size", "10")
                .addQueryParameter("page", "1")

            if (!currentToken.isNullOrBlank()) {
                urlBuilder.addQueryParameter("usertoken", currentToken)
            }

            lyricsGetWithHeaders(sign(urlBuilder.build().toString()), HEADERS)
        }

        var response = search(token)
        if (response != null && looksUnauthorized(response)) {
            cachedToken.set(null)
            val freshToken = getToken()
            response = search(freshToken)
        }

        val body = runCatching {
            lyricsJson.decodeFromString<Envelope<TrackSearchBody>>(response ?: return null)
        }.getOrNull() ?: return null

        return body.message.body?.trackList?.map { it.track }
    }

    private suspend fun fetchSubtitle(trackId: Long, token: String?): String? {
        val fetch = { currentToken: String? ->
            val urlBuilder = "$BASE_DESKTOP/track.subtitle.get".toHttpUrl().newBuilder()
                .addQueryParameter("app_id", "web-desktop-app-v1.0")
                .addQueryParameter("track_id", trackId.toString())
                .addQueryParameter("subtitle_format", "mxm")

            if (!currentToken.isNullOrBlank()) {
                urlBuilder.addQueryParameter("usertoken", currentToken)
            }

            lyricsGetWithHeaders(sign(urlBuilder.build().toString()), HEADERS)
        }

        var response = fetch(token)
        if (response != null && looksUnauthorized(response)) {
            cachedToken.set(null)
            val freshToken = getToken()
            response = fetch(freshToken)
        }

        return runCatching {
            lyricsJson.decodeFromString<Envelope<SubtitleBody>>(response ?: return null)
        }.getOrNull()?.message?.body?.subtitle?.subtitleBody
    }

    private fun isDecoyHoneypot(trackName: String, queryTitle: String, text: String): Boolean {
        if (text.contains("Wob gopini den", ignoreCase = true)) return true
        if (text.contains("Tefe woxica fero", ignoreCase = true)) return true
        if (trackName.equals("NOKIA", ignoreCase = true) && !queryTitle.contains("NOKIA", ignoreCase = true)) return true
        return false
    }

    /** Musixmatch's `mxm` subtitle JSON — a list of `{text, time:{total}}` — turned into LRC. */
    private fun subtitleToLrc(subtitleBody: String): String {
        // If it's already an LRC string format
        if (subtitleBody.trim().startsWith("[")) return subtitleBody.trim()

        val lines = runCatching { lyricsJson.decodeFromString<List<SubtitleLine>>(subtitleBody) }
            .getOrNull() ?: return ""
        return buildString {
            for (line in lines) {
                if (line.text.isBlank()) continue
                val totalMs = (line.time.total * 1000).toLong()
                val minutes = totalMs / 1000 / 60
                val seconds = (totalMs / 1000) % 60
                val millis = totalMs % 1000
                appendLine(
                    "[" + "%02d:%02d.%03d".format(Locale.US, minutes, seconds, millis) + "]" + line.text,
                )
            }
        }.trim()
    }

    private fun looksUnauthorized(body: String): Boolean =
        runCatching { lyricsJson.decodeFromString<Envelope<kotlinx.serialization.json.JsonElement>>(body) }
            .getOrNull()?.message?.header?.statusCode?.let { it == 401 || it == 402 } ?: false

    private suspend fun getToken(): String? {
        val userToken = configuredToken.get().ifBlank { null }
        if (userToken != null) return userToken

        return cachedToken.get() ?: tokenMutex.withLock {
            cachedToken.get() ?: fetchToken()?.also { cachedToken.set(it) }
        }
    }

    private fun fetchToken(): String? {
        // 1. Try desktop token endpoint
        val desktopUrl = "$BASE_DESKTOP/token.get".toHttpUrl().newBuilder()
            .addQueryParameter("app_id", "web-desktop-app-v1.0")
            .build()
        val desktopBody = lyricsGetWithHeaders(sign(desktopUrl.toString()), HEADERS)
        val desktopToken = desktopBody?.let {
            runCatching { lyricsJson.decodeFromString<Envelope<TokenBody>>(it) }.getOrNull()?.message?.body?.userToken
        }
        if (!desktopToken.isNullOrBlank() && !desktopToken.all { it == '0' }) {
            return desktopToken
        }

        // 2. Try mobile token endpoint
        val mobileUrl = "$BASE_APIC/token.get".toHttpUrl().newBuilder()
            .addQueryParameter("app_id", "mac-ios-v2.0")
            .build()
        val mobileBody = lyricsGetWithHeaders(mobileUrl.toString(), HEADERS)
        val mobileToken = mobileBody?.let {
            runCatching { lyricsJson.decodeFromString<Envelope<TokenBody>>(it) }.getOrNull()?.message?.body?.userToken
        }
        if (!mobileToken.isNullOrBlank() && !mobileToken.all { it == '0' }) {
            return mobileToken
        }

        return null
    }

    private fun sign(url: String): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SIGNING_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal("$url$date".toByteArray(Charsets.UTF_8))
        val signature = Base64.getEncoder().encodeToString(raw)
        return "$url&signature=${java.net.URLEncoder.encode(signature, "UTF-8")}&signature_protocol=sha256"
    }

    @Serializable
    private data class Envelope<T>(val message: Message<T>)

    @Serializable
    private data class Message<T>(val header: Header, val body: T? = null)

    @Serializable
    private data class Header(@SerialName("status_code") val statusCode: Int = 0)

    @Serializable
    private data class TokenBody(@SerialName("user_token") val userToken: String)

    @Serializable
    private data class TrackSearchBody(@SerialName("track_list") val trackList: List<TrackWrapper> = emptyList())

    @Serializable
    private data class TrackWrapper(val track: Track)

    @Serializable
    private data class Track(
        @SerialName("track_id") val trackId: Long,
        @SerialName("track_name") val trackName: String,
        @SerialName("artist_name") val artistName: String = "",
        @SerialName("track_length") val trackLength: Int? = null,
        @SerialName("has_subtitles") val hasSubtitles: Int = 0,
    )

    @Serializable
    private data class SubtitleBody(val subtitle: Subtitle? = null)

    @Serializable
    private data class Subtitle(@SerialName("subtitle_body") val subtitleBody: String)

    @Serializable
    private data class SubtitleLine(val text: String, val time: SubtitleTime)

    @Serializable
    private data class SubtitleTime(val total: Double)

    @Serializable
    private data class MacroCallsBody(
        @SerialName("macro_calls") val macroCalls: MacroCalls? = null,
    )

    @Serializable
    private data class MacroCalls(
        @SerialName("matcher.track.get") val matcherTrack: MatcherTrackWrapper? = null,
        @SerialName("track.subtitles.get") val trackSubtitles: TrackSubtitlesWrapper? = null,
        @SerialName("track.subtitle.get") val trackSubtitle: TrackSubtitleWrapper? = null,
    )

    @Serializable
    private data class MatcherTrackWrapper(val message: Message<TrackWrapper>)

    @Serializable
    private data class TrackSubtitlesWrapper(val message: Message<SubtitleListBody>)

    @Serializable
    private data class TrackSubtitleWrapper(val message: Message<SubtitleBody>)

    @Serializable
    private data class SubtitleListBody(
        @SerialName("subtitle_list") val subtitleList: List<SubtitleWrapper> = emptyList(),
    )

    @Serializable
    private data class SubtitleWrapper(val subtitle: Subtitle)
}
