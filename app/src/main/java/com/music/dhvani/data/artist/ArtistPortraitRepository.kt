package com.music.dhvani.data.artist

import android.util.Log
import com.music.dhvani.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves verified high-definition artist studio portrait photographs.
 *
 * Strict 4-Tier Resolution Hierarchy:
 * 1. Apple Music -> Official artist catalog artwork from Apple Music CDN (mzstatic.com, 1000x1000 HD).
 * 2. Deezer -> 1000x1000 Ultra HD verified artist studio shots.
 * 3. JioSaavn -> Curated 500x500 HD studio portraits (/artists/ directory only).
 * 4. YouTube -> Authentic channel circular profile avatar upgraded to s500 HD.
 */
object ArtistPortraitRepository {

    private const val TAG = "ArtistPortrait"
    private const val NO_PORTRAIT_SENTINEL = "__NO_PORTRAIT__"

    /** In-memory cache of resolved artist portrait image URLs. */
    private val portraitCache = ConcurrentHashMap<String, String>()

    /**
     * Synchronously returns a previously resolved portrait URL from memory cache,
     * or null if not yet queried or absent.
     */
    fun getCachedPortrait(artistName: String): String? {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return null
        val cached = portraitCache[cleanName.lowercase()] ?: return null
        return if (cached == NO_PORTRAIT_SENTINEL) null else cached
    }

    /**
     * Resolves a verified HD portrait image URL for [artistName].
     *
     * @param artistName The name of the artist to resolve.
     * @param currentThumbnail The thumbnail already provided by YouTube Music, if any.
     * @return Verified portrait URL, or null if the caller should retain its existing thumbnail.
     */
    suspend fun getPortrait(artistName: String, currentThumbnail: String? = null): String? = withContext(Dispatchers.IO) {
        val cleanName = cleanArtistName(artistName)
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = cleanName.lowercase()
        portraitCache[cacheKey]?.let { cached ->
            return@withContext if (cached == NO_PORTRAIT_SENTINEL) null else cached
        }

        Log.d(TAG, "Resolving portrait for '$cleanName' across sources...")
        val variants = generateArtistVariants(cleanName)

        // 1. Priority 1: Apple Music (Official Artist Catalog Artwork)
        val appleUrl = resolveFromAppleMusic(variants)
        if (appleUrl != null) {
            Log.i(TAG, "SUCCESS: '$cleanName' resolved from Apple Music (Priority 1) -> $appleUrl")
            portraitCache[cacheKey] = appleUrl
            return@withContext appleUrl
        }

        // 2. Priority 2: Deezer (1000x1000 Ultra HD Studio Portraits)
        val deezerUrl = resolveFromDeezer(variants)
        if (deezerUrl != null) {
            Log.i(TAG, "SUCCESS: '$cleanName' resolved from Deezer (Priority 2) -> $deezerUrl")
            portraitCache[cacheKey] = deezerUrl
            return@withContext deezerUrl
        }

        // 3. Priority 3: JioSaavn (Curated Studio CDN /artists/ 500x500 HD)
        val jioUrl = resolveFromJioSaavn(variants)
        if (jioUrl != null) {
            Log.i(TAG, "SUCCESS: '$cleanName' resolved from JioSaavn (Priority 3) -> $jioUrl")
            portraitCache[cacheKey] = jioUrl
            return@withContext jioUrl
        }

        // 4. Priority 4: YouTube (Only if Apple, Deezer, and Jio all return null)
        if (!currentThumbnail.isNullOrBlank() && isAuthenticYtAvatar(currentThumbnail)) {
            val hdAvatar = upgradeYtAvatarToHd(currentThumbnail)
            Log.i(TAG, "FALLBACK: '$cleanName' resolved from YouTube Avatar (Priority 4) -> $hdAvatar")
            portraitCache[cacheKey] = hdAvatar
            return@withContext hdAvatar
        }

        val ytChannelAvatar = resolveFromYouTubeChannel(variants)
        if (ytChannelAvatar != null) {
            Log.i(TAG, "FALLBACK: '$cleanName' resolved from YouTube Channel search (Priority 4) -> $ytChannelAvatar")
            portraitCache[cacheKey] = ytChannelAvatar
            return@withContext ytChannelAvatar
        }

        Log.w(TAG, "NO_PORTRAIT found for '$cleanName' across all 4 sources.")
        portraitCache[cacheKey] = NO_PORTRAIT_SENTINEL
        null
    }

    private fun cleanArtistName(name: String): String {
        return name
            .replace(Regex("(?i)\\s*-\\s*topic$"), "")
            .replace(Regex("(?i)\\s*official$"), "")
            .replace(Regex("(?i)\\s*vevo$"), "")
            .replace(Regex("(?i)\\s*\\(singer\\)$"), "")
            .replace(Regex("(?i)\\s*\\(composer\\)$"), "")
            .trim()
    }

    /**
     * Generates normalized candidate search queries including compound name splitting
     * and alternate honorific/spacing spellings.
     */
    private fun generateArtistVariants(cleanName: String): List<String> {
        val variants = LinkedHashSet<String>()
        variants.add(cleanName)

        // Split compound credits ("Roop Kumar Rathod, Sadhana Sargam" -> "Roop Kumar Rathod")
        for (sep in listOf(",", "&", " feat. ", " ft. ", " / ")) {
            if (cleanName.contains(sep)) {
                val first = cleanName.substringBefore(sep).trim()
                if (first.isNotBlank()) variants.add(first)
            }
        }

        val currentList = variants.toList()
        for (v in currentList) {
            // Honorific variations: Mohd. <-> Mohammed
            if (v.contains("Mohd.", ignoreCase = true) || v.contains("Mohd ", ignoreCase = true)) {
                variants.add(v.replace(Regex("(?i)Mohd\\.?"), "Mohammed").trim())
            } else if (v.contains("Mohammed", ignoreCase = true)) {
                variants.add(v.replace(Regex("(?i)Mohammed"), "Mohd.").trim())
            }

            // Spacing variations: "Roop Kumar Rathod" <-> "Roopkumar Rathod"
            val words = v.split(" ").filter { it.isNotBlank() }
            if (words.size == 3) {
                variants.add("${words[0]}${words[1]} ${words[2]}")
            }

            // Punctuation variants: "A.R. Rahman" <-> "AR Rahman"
            if (v.contains(".")) {
                variants.add(v.replace(".", "").trim())
            } else {
                if (v.startsWith("AR ", ignoreCase = true)) {
                    variants.add("A.R. " + v.substring(3))
                }
                if (v.equals("KK", ignoreCase = true)) {
                    variants.add("K.K.")
                }
            }
        }

        return variants.filter { it.isNotBlank() }
    }

    /**
     * Priority 1: Resolves official artist catalog artwork from Apple Music / iTunes.
     */
    private fun resolveFromAppleMusic(variants: List<String>): String? {
        val ogImageRegex = Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']|<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE)
        val browserUa = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        for (variant in variants) {
            val result = runCatching {
                val encoded = URLEncoder.encode(variant, "UTF-8")
                val searchUrl = "https://itunes.apple.com/search?term=$encoded&entity=musicArtist&limit=3"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", browserUa)
                    .build()

                val searchResponse = Http.client.newCall(searchRequest).execute()
                if (!searchResponse.isSuccessful) return@runCatching null
                val bodyStr = searchResponse.body?.string() ?: return@runCatching null
                val json = JSONObject(bodyStr)
                val results = json.optJSONArray("results") ?: return@runCatching null

                for (i in 0 until results.length()) {
                    val r = results.optJSONObject(i) ?: continue
                    val artistName = r.optString("artistName", "").trim()
                    val linkUrl = r.optString("artistLinkUrl", "").trim()
                    if (linkUrl.isBlank()) continue

                    val vClean = variant.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                    val aClean = artistName.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                    val isMatch = vClean == aClean || vClean in aClean || aClean in vClean

                    if (isMatch) {
                        val pageReq = Request.Builder()
                            .url(linkUrl)
                            .header("User-Agent", browserUa)
                            .build()
                        val pageResp = Http.client.newCall(pageReq).execute()
                        if (!pageResp.isSuccessful) continue
                        val source = pageResp.body?.source() ?: continue
                        val buffer = okio.Buffer()
                        // Only read first 64KB where <head> and og:image reside (lightning fast)
                        source.read(buffer, 64 * 1024L)
                        val headHtml = buffer.readUtf8()
                        pageResp.close()

                        val match = ogImageRegex.find(headHtml)
                        var imgUrl = match?.let { m ->
                            m.groupValues.getOrNull(1)?.takeIf { g -> g.isNotBlank() } ?: m.groupValues.getOrNull(2)
                        }

                        if (!imgUrl.isNullOrBlank() && imgUrl.contains("mzstatic.com")) {
                            // Convert to maximum 4K Ultra HD (4160x4160bb.jpg) so the image never pixelates or blurs even when zoomed in
                            imgUrl = imgUrl.replace(Regex("""/\d+x\d+.*"""), "/4160x4160bb.jpg")
                            Log.d(TAG, "Priority 1 (Apple Music 4K) matched for '$variant' -> $imgUrl")
                            return@runCatching imgUrl
                        }
                    }
                }
                null
            }.onFailure { Log.e(TAG, "Apple Music resolve error for '$variant'", it) }.getOrNull()

            if (result != null) return result
        }
        return null
    }

    /**
     * Priority 2: Resolves an official studio portrait from Deezer API (1000x1000 Ultra HD).
     * Works for artists worldwide (including Pakistani, Indian, Global).
     */
    private fun resolveFromDeezer(variants: List<String>): String? {
        for (variant in variants) {
            val result = runCatching {
                val encoded = URLEncoder.encode(variant, "UTF-8")
                val url = "https://api.deezer.com/search/artist?q=$encoded&limit=5"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Dhvani/1.0")
                    .build()

                val response = Http.client.newCall(request).execute()
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string() ?: return@runCatching null
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@runCatching null

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val artistName = item.optString("name", "").trim()
                    if (artistName.isBlank()) continue

                    val vClean = variant.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                    val aClean = artistName.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                    val isMatch = vClean == aClean || vClean in aClean || aClean in vClean

                    if (isMatch) {
                        val pictureXl = item.optString("picture_xl", "").takeIf { it.isNotBlank() }
                        val pictureBig = item.optString("picture_big", "").takeIf { it.isNotBlank() }
                        val pictureMed = item.optString("picture_medium", "").takeIf { it.isNotBlank() }
                        val candidate = pictureXl ?: pictureBig ?: pictureMed

                        if (candidate != null && !candidate.contains("/artist//") && !candidate.contains("default")) {
                            Log.d(TAG, "Found Deezer 1000x1000 HD portrait for '$variant': $candidate")
                            return@runCatching candidate
                        }
                    }
                }
                null
            }.getOrNull()

            if (result != null) return result
        }
        return null
    }

    /**
     * Priority 3: Resolves an official studio portrait from JioSaavn.
     * CRITICAL: Strictly requires the image URL to reside in `https://c.saavncdn.com/artists/`.
     * Any image outside `/artists/` is a song/album cover and is immediately rejected.
     */
    private fun resolveFromJioSaavn(variants: List<String>): String? {
        for (variant in variants) {
            // 1. Dedicated artist search endpoint
            runCatching {
                val encoded = URLEncoder.encode(variant, "UTF-8")
                val url = "https://www.jiosaavn.com/api.php?__call=search.getArtistResults&q=$encoded&_format=json&_marker=0&ctx=android&n=5"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Dhvani/1.0")
                    .build()

                val response = Http.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results")
                        if (results != null) {
                            for (i in 0 until results.length()) {
                                val item = results.optJSONObject(i) ?: continue
                                val name = item.optString("name", "").trim()
                                val img = item.optString("image", "").trim()

                                // STRICT CHECK: Must be in /artists/ directory and not default icon
                                if (img.contains("/artists/") && !img.contains("default")) {
                                    val vClean = variant.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                                    val tClean = name.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                                    if (vClean == tClean || vClean in tClean || tClean in vClean) {
                                        val hd = toJioSaavnHd(img)
                                        Log.d(TAG, "Found JioSaavn /artists/ HD portrait for '$variant': $hd")
                                        return hd
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Autocomplete fallback endpoint
            runCatching {
                val encoded = URLEncoder.encode(variant, "UTF-8")
                val url = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&ctx=android"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Dhvani/1.0")
                    .build()

                val response = Http.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val candidates = mutableListOf<JSONObject>()
                        json.optJSONObject("topquery")?.optJSONArray("data")?.let { arr ->
                            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { candidates.add(it) }
                        }
                        json.optJSONObject("artists")?.optJSONArray("data")?.let { arr ->
                            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { candidates.add(it) }
                        }

                        for (item in candidates) {
                            val title = item.optString("title", "").trim()
                            val img = item.optString("image", "").trim()

                            // STRICT CHECK: Must be in /artists/ directory
                            if (img.contains("/artists/") && !img.contains("default")) {
                                val vClean = variant.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                                val tClean = title.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                                if (vClean == tClean || vClean in tClean || tClean in vClean) {
                                    val hd = toJioSaavnHd(img)
                                    Log.d(TAG, "Found JioSaavn autocomplete /artists/ HD portrait for '$variant': $hd")
                                    return hd
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun toJioSaavnHd(img: String): String {
        return img
            .replace("50x50.webp", "500x500.jpg")
            .replace("50x50.jpg", "500x500.jpg")
            .replace("150x150.webp", "500x500.jpg")
            .replace("150x150.jpg", "500x500.jpg")
    }

    private fun isAuthenticYtAvatar(url: String): Boolean {
        val isYtDomain = url.contains("yt3.googleusercontent.com") || url.contains("yt3.ggpht.com")
        val isNotDefault = !url.contains("default-user")
        val isNotBanner = !url.contains("w1440") && !url.contains("w1080") && !url.contains("w1920")
        return isYtDomain && isNotDefault && isNotBanner
    }

    private fun upgradeYtAvatarToHd(url: String): String {
        return if (url.contains("=")) {
            url.substringBefore("=") + "=s500-c-k-c0x00ffffff-no-rj"
        } else {
            url
        }
    }

    /**
     * Priority 4: Resolves an official artist channel circular profile avatar from YouTube.
     * Uses YouTube WEB client to query the artist channel and extract the verified avatar.
     */
    private fun resolveFromYouTubeChannel(variants: List<String>): String? {
        for (variant in variants) {
            val result = runCatching {
                val jsonPayload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20240101.00.00")
                            put("hl", "en")
                        })
                    })
                    put("query", "$variant channel")
                }

                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/search")
                    .post(body)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Dhvani/1.0")
                    .build()

                val response = Http.client.newCall(request).execute()
                if (!response.isSuccessful) return@runCatching null
                val respStr = response.body?.string() ?: return@runCatching null
                val root = JSONObject(respStr)

                val sections = root.optJSONObject("contents")
                    ?.optJSONObject("twoColumnSearchResultsRenderer")
                    ?.optJSONObject("primaryContents")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: return@runCatching null

                for (sIdx in 0 until sections.length()) {
                    val section = sections.optJSONObject(sIdx) ?: continue
                    val items = section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue
                    for (iIdx in 0 until items.length()) {
                        val item = items.optJSONObject(iIdx) ?: continue
                        val cr = item.optJSONObject("channelRenderer") ?: continue
                        val title = cr.optJSONObject("title")?.optString("simpleText", "") ?: ""

                        val vClean = variant.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                        val cClean = title.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                        if (vClean.isNotBlank() && (vClean == cClean || vClean in cClean || cClean in vClean)) {
                            val thumbs = cr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            if (thumbs != null && thumbs.length() > 0) {
                                val lastThumb = thumbs.getJSONObject(thumbs.length() - 1)
                                var u = lastThumb.optString("url", "")
                                if (u.isNotBlank()) {
                                    if (u.startsWith("//")) u = "https:$u"
                                    val hd = upgradeYtAvatarToHd(u)
                                    Log.d(TAG, "Found YouTube channel avatar for '$variant' ($title): $hd")
                                    return@runCatching hd
                                }
                            }
                        }
                    }
                }
                null
            }.getOrNull()

            if (result != null) return result
        }
        return null
    }
}
