package com.music.dhvani.data.canvas

import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Manages dynamic, remote retrieval of Spotify sp_dc session tokens from a GitHub Gist or Raw URL.
 *
 * This allows all users to access Spotify Canvas video artworks automatically without needing
 * to log in. When the token rotates or expires, it can be updated in the remote Gist without
 * requiring a new APK release.
 */
object SpotifyRemoteToken {
    private const val TAG = "SpotifyRemoteToken"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val fetchMutex = Mutex()

    @Volatile
    private var lastFetchAttemptTimeMs = 0L
    private val MIN_FETCH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5)

    fun hasToken(): Boolean {
        return getEffectiveToken().isNotBlank()
    }

    /**
     * Resolves the token that should be used for Spotify requests:
     * 1. User custom token (if explicitly configured)
     * 2. Persisted remote token from GitHub Gist
     * 3. Built-in BuildConfig token
     */
    fun getEffectiveToken(): String {
        if (AppSettings.isCustomSpotifyToken.value && AppSettings.spotifySpdcToken.value.isNotBlank()) {
            return AppSettings.spotifySpdcToken.value.trim()
        }

        val remote = AppSettings.remoteSpotifyToken.value.trim()
        if (remote.isNotBlank()) {
            return remote
        }

        val defaultToken = AppSettings.DEFAULT_SPOTIFY_SPDC_TOKEN.trim()
        if (defaultToken.isNotBlank()) {
            return defaultToken
        }

        val currentVal = AppSettings.spotifySpdcToken.value.trim()
        if (currentVal.isNotBlank()) {
            return currentVal
        }

        return ""
    }

    /**
     * Fetches the latest token from the remote GitHub Gist or configured URL.
     */
    suspend fun fetchOrRefresh(force: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        fetchMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastFetchAttemptTimeMs < MIN_FETCH_INTERVAL_MS) {
                val current = getEffectiveToken()
                if (current.isNotBlank()) return@withLock Result.success(current)
            }
            lastFetchAttemptTimeMs = now

            var targetUrl = AppSettings.spotifyRemoteTokenUrl.value.trim()
                .ifBlank { AppSettings.DEFAULT_SPOTIFY_REMOTE_TOKEN_URL }

            if (targetUrl.contains("gist.githubusercontent.com") && targetUrl.contains("/raw/")) {
                val regex = Regex("""(gist\.githubusercontent\.com/[^/]+/[^/]+/raw)/[a-f0-9]{40}/(.*)""")
                targetUrl = targetUrl.replace(regex, "$1/$2")
            }

            if (targetUrl.isBlank()) {
                return@withLock Result.failure(IllegalStateException("No remote Spotify token URL configured"))
            }

            TrackLog.d(TAG, "Fetching remote Spotify token from: $targetUrl")
            runCatching {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "DhvaniMusic/2.5.1")
                    .header("Cache-Control", "no-cache")
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code} from remote token URL")
                    }
                    val body = response.body?.string().orEmpty().trim()
                    val parsedToken = parseTokenFromBody(body)
                    if (parsedToken.isBlank()) {
                        throw IllegalStateException("No valid sp_dc cookie found in response")
                    }

                    TrackLog.d(TAG, "Successfully acquired remote Spotify token (${parsedToken.take(12)}...)")
                    AppSettings.setRemoteSpotifyToken(parsedToken)
                    parsedToken
                }
            }.onFailure {
                TrackLog.w(TAG, "Failed to fetch remote Spotify token: ${it.message}")
            }
        }
    }

    /**
     * Parses the sp_dc token from either a raw text body or a JSON response
     * (such as GitHub Gist API or a custom key-value JSON).
     */
    private fun parseTokenFromBody(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        // Try JSON parsing
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val token = runCatching {
                val root = json.parseToJsonElement(trimmed).jsonObject
                // Direct keys
                root["sp_dc"]?.jsonPrimitive?.contentOrNull
                    ?: root["token"]?.jsonPrimitive?.contentOrNull
                    ?: root["spotify_spdc"]?.jsonPrimitive?.contentOrNull
                    ?: root["spotify_sp_dc"]?.jsonPrimitive?.contentOrNull
                    // GitHub Gist API response structure: root["files"][filename]["content"]
                    ?: root["files"]?.jsonObject?.values?.firstOrNull()?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull?.let { parseTokenFromBody(it) }
            }.getOrNull()
            if (!token.isNullOrBlank()) return cleanTokenString(token)
        }

        // Plain text fallback
        return cleanTokenString(trimmed)
    }

    private fun cleanTokenString(input: String): String {
        var str = input.trim().removeSurrounding("\"").removeSurrounding("'")
        if (str.startsWith("sp_dc=", ignoreCase = true)) {
            str = str.substringAfter("=").trim()
        }
        if (str.contains(";")) {
            str = str.substringBefore(";").trim()
        }
        return str
    }
}
