package com.music.dhvani.data.lyrics

import android.util.Log
import com.music.dhvani.data.Http
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "LyricsHttp"
private const val LYRICS_TIMEOUT_SECONDS = 12L

internal const val LYRICS_AGENT = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

internal val lyricsJson = Json { ignoreUnknownKeys = true; isLenient = true }

private val client by lazy {
    // Derived from the shared client, so the connection pool and DNS stay
    // common — only the deadline differs.
    Http.client.newBuilder()
        .callTimeout(LYRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}

/** Body of a successful GET, or null for any failure at all. */
internal fun lyricsGet(url: String): String? = runCatching {
    val request = Request.Builder().url(url)
        .header("User-Agent", LYRICS_AGENT)
        .header("Accept", "application/json, text/plain, application/xml, */*")
        .build()
    client.newCall(request).execute().use { response ->
        val code = response.code
        val body = response.body?.string()
        if (response.isSuccessful) {
            Log.d(TAG, "SUCCESS $code for $url (${body?.length ?: 0} bytes)")
            body
        } else {
            Log.w(TAG, "FAIL $code ${response.message} for $url")
            null
        }
    }
}.onFailure { e ->
    Log.e(TAG, "EXCEPTION for $url: ${e.javaClass.simpleName} - ${e.message}")
}.getOrNull()

/** Body of a GET with custom headers (e.g. for Musixmatch browser/client emulation). */
internal fun lyricsGetWithHeaders(url: String, headers: Map<String, String>): String? = runCatching {
    val requestBuilder = Request.Builder().url(url)
    headers.forEach { (k, v) -> requestBuilder.header(k, v) }
    client.newCall(requestBuilder.build()).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
    }
}.getOrNull()

private val authenticatedClient by lazy {
    client.newBuilder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
}

/** Body of an authenticated provider GET without service-specific browser headers. */
internal fun lyricsGetBearer(url: String, bearer: String): String? = runCatching {
    if (bearer.isBlank()) return null
    val request = Request.Builder().url(url)
        .header("User-Agent", LYRICS_AGENT)
        .header("Accept", "application/json, text/plain, */*")
        .header("Authorization", "Bearer $bearer")
        .build()
    authenticatedClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
    }
}.getOrNull()

/**
 * [lyricsGet], with a bearer token and the headers Apple's own web player
 * sends alongside one — `amp-api.music.apple.com` answers a token with no
 * `Origin` at all the same way it answers a wrong one, with a 403.
 */
internal fun lyricsGetAuthorized(url: String, bearer: String): String? = runCatching {
    val request = Request.Builder().url(url)
        .header("User-Agent", LYRICS_AGENT)
        .header("Accept", "application/json")
        .header("Authorization", "Bearer $bearer")
        .header("Origin", "https://music.apple.com")
        .header("Referer", "https://music.apple.com/")
        .build()
    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
    }
}.getOrNull()
