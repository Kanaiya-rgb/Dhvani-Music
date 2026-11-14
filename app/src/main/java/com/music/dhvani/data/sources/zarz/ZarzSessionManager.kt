package com.music.dhvani.data.sources.zarz
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
private const val TAG = "DhvaniMusic"
/**
 * Manages signed sessions and HMAC authentication for Zarz / Mornye backend APIs
 * (https://api.zarz.moe), powering high-resolution Tidal, Qobuz, and Amazon Music playback.
 */
object ZarzSessionManager {
    private const val BASE_URL = "https://api.zarz.moe"
    private const val PREFS_NAME = "zarz_session_store"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_SECRET = "session_secret"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_CHALLENGE_ID = "challenge_id"
    private const val KEY_TURNSTILE_SITE_KEY = "turnstile_site_key"
    private const val SCHEME_LABEL = "ZARZ-HMAC-V1"
    private const val APP_VERSION = "tidal-web@1.2.5"
    private const val PLATFORM = "extension"
    private const val HEADER_PREFIX = "X-Zarz-"
    private const val TIME_WINDOW_SECONDS = 300L
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val random = SecureRandom()
    private var prefs: SharedPreferences? = null
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (getInstallId().isBlank()) {
                val newId = UUID.randomUUID().toString()
                prefs?.edit()?.putString(KEY_INSTALL_ID, newId)?.apply()
            }
        }
    }
    fun getInstallId(): String = prefs?.getString(KEY_INSTALL_ID, "") ?: ""
    fun getSessionId(): String = prefs?.getString(KEY_SESSION_ID, "") ?: ""
    private fun getSessionSecret(): String = prefs?.getString(KEY_SESSION_SECRET, "") ?: ""
    private fun getExpiresAt(): Long = prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L
    fun getTurnstileSiteKey(): String = prefs?.getString(KEY_TURNSTILE_SITE_KEY, "0x4AAAAAADIYiNDcnB6HDKKi") ?: "0x4AAAAAADIYiNDcnB6HDKKi"
    fun getPendingChallengeId(): String = prefs?.getString(KEY_CHALLENGE_ID, "") ?: ""
    fun isSessionValid(): Boolean {
        val sId = getSessionId()
        val sSecret = getSessionSecret()
        val exp = getExpiresAt()
        return sId.isNotBlank() && sSecret.isNotBlank() && System.currentTimeMillis() < exp
    }
    /**
     * Bootstraps session with the gateway server.
     * Returns true if session is ready and usable, false if verification challenge is required.
     */
    suspend fun bootstrap(): Boolean = withContext(Dispatchers.IO) {
        if (isSessionValid()) return@withContext true
        val installId = getInstallId().ifBlank { UUID.randomUUID().toString() }
        val url = "$BASE_URL/v2/bootstrap?app_version=$APP_VERSION&install_id=$installId"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .get()
                .build()
            Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    TrackLog.w(TAG, "Zarz bootstrap returned HTTP ${resp.code}")
                    return@withContext false
                }
                val body = resp.body?.string().orEmpty()
                val parsed = json.parseToJsonElement(body) as? JsonObject ?: return@withContext false
                val grant = parsed["grant"]?.jsonPrimitive?.content
                if (!grant.isNullOrBlank()) {
                    TrackLog.d(TAG, "Zarz bootstrap returned direct grant, exchanging...")
                    return@withContext exchangeGrant(grant)
                }
                val sessionId = parsed["session_id"]?.jsonPrimitive?.content
                val sessionSecret = parsed["session_secret"]?.jsonPrimitive?.content
                val challengeId = parsed["challenge_id"]?.jsonPrimitive?.content
                val siteKey = parsed["turnstile_site_key"]?.jsonPrimitive?.content
                if (!sessionId.isNullOrBlank() && !sessionSecret.isNullOrBlank()) {
                    val exp = System.currentTimeMillis() + 86400_000L * 7 // 7 days
                    saveSession(sessionId, sessionSecret, exp)
                    TrackLog.d(TAG, "Zarz bootstrap succeeded directly with session $sessionId")
                    return@withContext true
                }
                if (!challengeId.isNullOrBlank()) {
                    prefs?.edit()?.apply {
                        putString(KEY_CHALLENGE_ID, challengeId)
                        if (!siteKey.isNullOrBlank()) putString(KEY_TURNSTILE_SITE_KEY, siteKey)
                        apply()
                    }
                    TrackLog.d(TAG, "Zarz bootstrap returned challenge $challengeId, siteKey=$siteKey")
                }
                false
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Zarz bootstrap failed: ${e.message}")
            false
        }
    }
    /**
     * Exchanges a completed Turnstile grant token for an active HMAC session.
     */
    suspend fun exchangeGrant(grant: String): Boolean = withContext(Dispatchers.IO) {
        val installId = getInstallId().ifBlank { UUID.randomUUID().toString() }
        val challengeId = getPendingChallengeId()
        val payload = buildJsonObject {
            put("grant", grant)
            put("install_id", installId)
            if (challengeId.isNotBlank()) {
                put("challenge_id", challengeId)
            }
            put("app_version", APP_VERSION)
            put("platform", PLATFORM)
        }.toString()
        val req = Request.Builder()
            .url("$BASE_URL/v2/session/exchange")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            Http.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    TrackLog.w(TAG, "Zarz session exchange returned HTTP ${resp.code}: $body")
                    return@withContext false
                }
                val parsed = json.parseToJsonElement(body) as? JsonObject ?: return@withContext false
                val sId = parsed["session_id"]?.jsonPrimitive?.content.orEmpty()
                val sSecret = parsed["session_secret"]?.jsonPrimitive?.content.orEmpty()
                if (sId.isNotBlank() && sSecret.isNotBlank()) {
                    val exp = System.currentTimeMillis() + 86400_000L * 7
                    saveSession(sId, sSecret, exp)
                    TrackLog.d(TAG, "Zarz session exchange successful! Active session: $sId")
                    return@withContext true
                }
                false
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Zarz session exchange failed: ${e.message}")
            false
        }
    }
    private fun saveSession(sessionId: String, sessionSecret: String, expiresAt: Long) {
        prefs?.edit()?.apply {
            putString(KEY_SESSION_ID, sessionId)
            putString(KEY_SESSION_SECRET, sessionSecret)
            putLong(KEY_EXPIRES_AT, expiresAt)
            remove(KEY_CHALLENGE_ID)
            apply()
        }
    }
    fun clearSession() {
        prefs?.edit()?.apply {
            remove(KEY_SESSION_ID)
            remove(KEY_SESSION_SECRET)
            remove(KEY_EXPIRES_AT)
            apply()
        }
    }
    /**
     * Builds standard ZARZ-HMAC-V1 signed HTTP request headers.
     */
    fun signHeaders(method: String, path: String, body: ByteArray = ByteArray(0)): Map<String, String> {
        val sessionId = getSessionId()
        val sessionSecret = getSessionSecret()
        if (sessionId.isBlank() || sessionSecret.isBlank()) return emptyMap()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date(now))
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(body).joinToString("") { "%02x".format(it) }
        val nonceBytes = ByteArray(12)
        random.nextBytes(nonceBytes)
        val nonce = nonceBytes.joinToString("") { "%02x".format(it) }
        val window = (now / 1000L) / TIME_WINDOW_SECONDS
        val rollingKeyMsg = "$window:$sessionId".toByteArray(StandardCharsets.UTF_8)
        val rollingKey = hmacSha256(sessionSecret.toByteArray(StandardCharsets.UTF_8), rollingKeyMsg)
        val signingString = listOf(
            SCHEME_LABEL,
            method.uppercase(),
            path,
            "",
            hash,
            timestamp,
            nonce,
            sessionId,
            APP_VERSION,
            PLATFORM
        ).joinToString("\n")
        val signature = hmacSha256(rollingKey.toByteArray(StandardCharsets.UTF_8), signingString.toByteArray(StandardCharsets.UTF_8))
        return mapOf(
            "${HEADER_PREFIX}Session" to sessionId,
            "${HEADER_PREFIX}Timestamp" to timestamp,
            "${HEADER_PREFIX}Nonce" to nonce,
            "${HEADER_PREFIX}Body-SHA256" to hash,
            "${HEADER_PREFIX}Signature" to signature,
            "${HEADER_PREFIX}App-Version" to APP_VERSION,
            "${HEADER_PREFIX}Platform" to PLATFORM,
        ).mapKeys { it.key.lowercase(Locale.ROOT) }
    }
    private fun hmacSha256(key: ByteArray, message: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val raw = mac.doFinal(message)
        return Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
    suspend fun getDownloadTicket(trackId: String): String? = withContext(Dispatchers.IO) {
        if (!isSessionValid()) return@withContext null
        try {
            val md = MessageDigest.getInstance("SHA-256")
            val resHash = md.digest("tid:track:$trackId".toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val payload = buildJsonObject {
                put("capability", "download_ticket")
                put("provider", "tid")
                put("resource_hash", resHash)
            }.toString()
            val path = "/v2/tickets"
            val bodyBytes = payload.toByteArray(StandardCharsets.UTF_8)
            val signed = signHeaders("POST", path, bodyBytes)
            val reqBuilder = Request.Builder()
                .url("$BASE_URL$path")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(bodyBytes.toRequestBody("application/json".toMediaType()))
            signed.forEach { (k, v) -> reqBuilder.header(k, v) }
            Http.client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val parsed = json.parseToJsonElement(body) as? JsonObject
                    val ticket = parsed?.get("ticket_id")?.jsonPrimitive?.content
                        ?: parsed?.get("ticket")?.jsonPrimitive?.content
                    return@withContext ticket
                }
            }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Zarz ticket request failed: ${e.message}")
        }
        null
    }

    /**
     * Resolves a TIDAL track download/stream payload from Zarz or community fallback mirrors.
     */
    suspend fun resolveTidalStream(trackId: String, quality: String = "LOSSLESS", isAtmos: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!isSessionValid()) {
            bootstrap()
        }
        val ticket = getDownloadTicket(trackId)
        val path = "/v1/dl/tid2"
        val payloadObj = if (isAtmos) {
            buildJsonObject {
                put("id", trackId)
                put("endpoint", "manifests")
                put("formats", buildJsonObject { put("0", "EAC3_JOC") })
            }
        } else {
            buildJsonObject {
                put("id", trackId)
                put("quality", quality)
            }
        }
        val bodyBytes = payloadObj.toString().toByteArray(StandardCharsets.UTF_8)
        if (!ticket.isNullOrBlank()) {
            val pathsToTry = listOf("/v1/dl/tid", "/v1/dl/tid2")
            for (p in pathsToTry) {
                val signed = signHeaders("POST", p, bodyBytes)
                val reqBuilder = Request.Builder()
                    .url("$BASE_URL$p")
                    .header("Content-Type", "application/json")
                    .header("X-Zarz-Ticket", ticket)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(bodyBytes.toRequestBody("application/json".toMediaType()))
                signed.forEach { (k, v) -> reqBuilder.header(k, v) }

                try {
                    Http.client.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val parsed = json.parseToJsonElement(body) as? JsonObject
                            val data = parsed?.get("data") as? JsonObject
                            val directUrl = data?.get("directURL")?.jsonPrimitive?.content
                                ?: data?.get("url")?.jsonPrimitive?.content
                                ?: data?.get("uri")?.jsonPrimitive?.content
                                ?: parsed?.get("url")?.jsonPrimitive?.content
                            if (!directUrl.isNullOrBlank()) {
                                TrackLog.d(TAG, "Zarz resolved Tidal stream for $trackId via $p")
                                return@withContext directUrl
                            }
                        } else if (resp.code == 401 || resp.code == 403) {
                            TrackLog.w(TAG, "Zarz session returned ${resp.code}; clearing invalid session")
                            clearSession()
                        }
                    }
                } catch (e: Exception) {
                    TrackLog.w(TAG, "Zarz Tidal stream lookup failed for $p: ${e.message}")
                }
            }
        }
        // 2. Fallback to open Tidal FLAC community mirrors (tight 1200ms timeout)
        val communityMirrors = listOf(
            "https://tidal.qqdl.site",
            "https://tracks.monochrome.st"
        )
        val mirrorClient = Http.client.newBuilder()
            .connectTimeout(1200, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        for (mirror in communityMirrors) {
            try {
                val mirrorUrl = "$mirror/track/?id=$trackId&quality=$quality"
                val mirrorReq = Request.Builder()
                    .url(mirrorUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
                    .build()
                mirrorClient.newCall(mirrorReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val respBody = resp.body?.string().orEmpty()
                        val parsed = runCatching { json.parseToJsonElement(respBody) as? JsonObject }.getOrNull()
                        val mirrorStream = parsed?.get("url")?.jsonPrimitive?.content
                            ?: parsed?.get("streamUrl")?.jsonPrimitive?.content
                        if (!mirrorStream.isNullOrBlank()) {
                            TrackLog.d(TAG, "Tidal community mirror ($mirror) resolved stream for $trackId")
                            return@withContext mirrorStream
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }
}
