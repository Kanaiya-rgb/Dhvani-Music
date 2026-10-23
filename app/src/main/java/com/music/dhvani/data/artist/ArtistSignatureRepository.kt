package com.music.dhvani.data.artist

import android.net.Uri
import android.util.Log
import com.music.dhvani.data.Http
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves handwritten artist signatures / autographs for artist profiles.
 *
 * Supports 3 resolution layers:
 * 1. User-configured Custom Signature API endpoint (BitChord compatible)
 * 2. Instant curated vector signatures for top world & Indian musicians
 * 3. Open Wikidata SPARQL service (Property P109) covering hundreds of thousands of artists worldwide
 */
object ArtistSignatureRepository {

    private const val TAG = "ArtistSignature"
    private const val WIKIDATA_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql"
    private const val NO_SIGNATURE_SENTINEL = "__NO_SIGNATURE__"

    /** In-memory cache of resolved signature URLs (sentinel indicates verified absent). */
    private val signatureCache = ConcurrentHashMap<String, String>()

    /** Curated dictionary of verified vector signatures on Wikimedia Commons for instant zero-latency loading. */
    private val CURATED_SIGNATURES = mapOf(
        "atif aslam" to "https://upload.wikimedia.org/wikipedia/commons/4/4a/Atif-aslam--transparenet-signature.png",
        "a. r. rahman" to "https://upload.wikimedia.org/wikipedia/en/1/1b/A_R_Rahman_signature.svg",
        "ar rahman" to "https://upload.wikimedia.org/wikipedia/en/1/1b/A_R_Rahman_signature.svg",
        "shreya ghoshal" to "https://upload.wikimedia.org/wikipedia/en/f/f0/Shreya_Ghoshal_signature.svg",
        "lata mangeshkar" to "https://upload.wikimedia.org/wikipedia/commons/6/6c/Signature_of_Lata_Mangeshkar.svg",
        "rosé" to "https://upload.wikimedia.org/wikipedia/commons/b/bf/Signature_of_Ros%C3%A9.svg",
        "rose" to "https://upload.wikimedia.org/wikipedia/commons/b/bf/Signature_of_Ros%C3%A9.svg",
        "roseanne park" to "https://upload.wikimedia.org/wikipedia/commons/b/bf/Signature_of_Ros%C3%A9.svg",
        "taylor swift" to "https://upload.wikimedia.org/wikipedia/commons/e/e6/Taylor_Swift_signature.svg",
        "billie eilish" to "https://upload.wikimedia.org/wikipedia/commons/4/4d/Billie_Eilish_signature.svg",
        "ariana grande" to "https://upload.wikimedia.org/wikipedia/commons/c/cb/Ariana_Signature.png",
        "selena gomez" to "https://upload.wikimedia.org/wikipedia/commons/d/da/Selena_Gomez_signature.svg",
        "justin bieber" to "https://upload.wikimedia.org/wikipedia/commons/a/ab/Signature_Justin_Bieber.svg",
        "eminem" to "https://upload.wikimedia.org/wikipedia/commons/3/3d/Eminem%27s_signature.svg",
        "shakira" to "https://upload.wikimedia.org/wikipedia/commons/c/c3/Shakira_signature%2C_Billboard_Open_Letter_2016.png",
        "katy perry" to "https://upload.wikimedia.org/wikipedia/commons/3/31/Katy_Perry%27s_signature.svg",
        "dua lipa" to "https://upload.wikimedia.org/wikipedia/commons/a/ad/Dua_Lipa_%28n%C3%ABnshkrim%29.svg",
        "michael jackson" to "https://upload.wikimedia.org/wikipedia/commons/0/04/Michael_Jackson_signature.svg",
        "lady gaga" to "https://upload.wikimedia.org/wikipedia/commons/c/c3/Lady_Gaga_signature.svg",
        "olivia rodrigo" to "https://upload.wikimedia.org/wikipedia/commons/b/b4/Olivia_Rodrigo_signature.svg",
        "ed sheeran" to "https://upload.wikimedia.org/wikipedia/commons/e/ed/Ed_Sheeran_sig.svg",
        "bruno mars" to "https://upload.wikimedia.org/wikipedia/commons/f/f3/Bruno_Mars_sig.svg",
        "rihanna" to "https://upload.wikimedia.org/wikipedia/commons/d/d5/Rihanna-signature.svg",
        "beyoncé" to "https://upload.wikimedia.org/wikipedia/commons/8/8b/Beyonc%C3%A9_signature.svg",
        "beyonce" to "https://upload.wikimedia.org/wikipedia/commons/8/8b/Beyonc%C3%A9_signature.svg",
        "adele" to "https://upload.wikimedia.org/wikipedia/commons/1/14/Adele_signature.svg",
        "coldplay" to "https://upload.wikimedia.org/wikipedia/commons/5/54/Coldplay_signatures_%28vector%29.svg",
        "kendrick lamar" to "https://upload.wikimedia.org/wikipedia/commons/e/e1/Kendrick_Lamar_Signature.svg",
        "kanye west" to "https://upload.wikimedia.org/wikipedia/commons/c/c5/Kanyewestsignature.svg",
        "ye" to "https://upload.wikimedia.org/wikipedia/commons/c/c5/Kanyewestsignature.svg",

        // Indian Singing Legends (Curated from verified IndianAutographs & Archives)
        "kishore kumar" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214086a.jpg",
        "mohammed rafi" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214085a.jpg",
        "mohd rafi" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214085a.jpg",
        "mukesh" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214110a.jpg",
        "asha bhosle" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214077a.jpg",
        "sonu nigam" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214092a.jpg",
        "udit narayan" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214098a.jpg",
        "jagjit singh" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214550a.jpg",
        "manna dey" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214080a.jpg",
        "shaan" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214122a.jpg",
        "pankaj udhas" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214556a.jpg",
        "hariharan" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214104a.jpg",
        "anuradha paudwal" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214107a.jpg",
        "kavita krishnamurthy" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214109-a.jpg",
        "kavita krishnamurti" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214109-a.jpg",
        "hemant kumar" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214081a.jpg",
        "talat mahmood" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214082a.jpg",
        "anup jalota" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214501a.jpg",
        "bhimsen joshi" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214150a.jpg",
        "pandit jasraj" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214255a.jpg",
        "usha mangeshkar" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214078a.jpg",
        "meena mangeshkar" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214079a.jpg",
        "mahendra kapoor" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214089a.jpg",
        "amit kumar" to "https://indianautographs.com/wp-content/uploads/prodimages/m_214088a.jpg",
    )

    const val BITCHORD_SIGNATURE_API = "https://proud-cascade-1654.de.deplexo.com/"

    /**
     * Resolves the signature graphic URL for [artistName].
     * Returns null if signatures are disabled, or if no signature could be found.
     */
    suspend fun getSignature(artistName: String): String? = withContext(Dispatchers.IO) {
        if (!AppSettings.artistSignaturesEnabled.value) return@withContext null
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = cleanName.lowercase()
        val cached = signatureCache[cacheKey]
        if (cached != null) {
            return@withContext if (cached == NO_SIGNATURE_SENTINEL) null else cached
        }

        // Layer 1: BitChord / User-Configured Signature API endpoint
        val customUrl = AppSettings.customSignatureApiUrl.value.trim().ifBlank { BITCHORD_SIGNATURE_API }
        if (customUrl.isNotBlank()) {
            val resolvedCustom = resolveFromCustomApi(customUrl, cleanName)
            if (resolvedCustom != null) {
                Log.d(TAG, "BitChord/Custom API signature hit for '$artistName': $resolvedCustom")
                signatureCache[cacheKey] = resolvedCustom
                return@withContext resolvedCustom
            }
        }

        // Layer 2: Fast curated dictionary
        val curated = CURATED_SIGNATURES[cacheKey]
        if (curated != null) {
            Log.d(TAG, "Curated signature hit for '$artistName': $curated")
            signatureCache[cacheKey] = curated
            return@withContext curated
        }

        // Layer 3: Wikidata SPARQL query (Property P109) - high quality transparent SVG/PNG
        val wikiSignature = queryWikidataSignature(cleanName)
        if (wikiSignature != null) {
            Log.d(TAG, "Wikidata signature for '$artistName': $wikiSignature")
            signatureCache[cacheKey] = wikiSignature
            return@withContext wikiSignature
        }

        signatureCache[cacheKey] = NO_SIGNATURE_SENTINEL
        null
    }

    private fun queryIndianAutographs(artistName: String): String? {
        return runCatching {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val searchUrl = "https://indianautographs.com/?s=$encoded"
            val req = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            Http.client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val linkMatcher = java.util.regex.Pattern.compile("""href=["'](https://indianautographs\.com/singers/[^"']+)["']""").matcher(body)
                val targetLinks = mutableListOf<String>()
                while (linkMatcher.find()) {
                    val l = linkMatcher.group(1) ?: continue
                    if (!l.endsWith("/singers/") && !targetLinks.contains(l)) {
                        targetLinks.add(l)
                    }
                }
                val bestLink = targetLinks.firstOrNull { link ->
                    artistName.lowercase().split(" ").any { part -> part.length > 2 && link.contains(part) }
                } ?: targetLinks.firstOrNull() ?: return null

                val detailReq = Request.Builder()
                    .url(bestLink)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
                Http.client.newCall(detailReq).execute().use { detailResp ->
                    if (!detailResp.isSuccessful) return null
                    val detailBody = detailResp.body?.string().orEmpty()
                    val imgMatcher = java.util.regex.Pattern.compile("""https://indianautographs\.com/wp-content/uploads/prodimages/m_[^"'\s]+a\.(?:jpg|jpeg|png)""").matcher(detailBody)
                    if (imgMatcher.find()) {
                        return imgMatcher.group()
                    }
                    null
                }
            }
        }.onFailure {
            Log.d(TAG, "IndianAutographs query error for $artistName: ${it.message}")
        }.getOrNull()
    }

    private fun resolveFromCustomApi(baseUrl: String, artistName: String): String? {
        return runCatching {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val targetUrl = when {
                baseUrl.contains("{artist}") -> baseUrl.replace("{artist}", encoded)
                baseUrl.contains("/v1/signatures") -> {
                    val separator = if (baseUrl.contains("?")) "&" else "?"
                    "$baseUrl${separator}artist=$encoded"
                }
                baseUrl.contains("proud-cascade") || baseUrl.contains("deplexo") -> {
                    "${baseUrl.trimEnd('/')}/v1/signatures?artist=$encoded"
                }
                else -> {
                    val separator = if (baseUrl.contains("?")) "&" else "?"
                    "$baseUrl${separator}artist=$encoded"
                }
            }

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "DhvaniMusic/1.0 (Artist Signatures)")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                val bodyString = response.body?.string()?.trim().orEmpty()

                if (contentType.contains("json") || bodyString.startsWith("{")) {
                    val json = JSONObject(bodyString)

                    // 1. BitChord / Deplexo API signatures array
                    val signaturesArray = json.optJSONArray("signatures")
                    if (signaturesArray != null && signaturesArray.length() > 0) {
                        var fallbackUrl: String? = null
                        for (i in 0 until signaturesArray.length()) {
                            val sigObj = signaturesArray.optJSONObject(i) ?: continue
                            val asset = sigObj.optJSONObject("asset") ?: continue
                            val format = asset.optString("format").lowercase()
                            val rawUrl = asset.optString("url")
                            if (rawUrl.isNotBlank() && (rawUrl.startsWith("http://") || rawUrl.startsWith("https://"))) {
                                // Strip tracking query params so clean URL and extension are preserved
                                val cleanUrl = rawUrl.substringBefore("?").replace("http://", "https://")
                                if (format == "svg") {
                                    return cleanUrl
                                } else if (fallbackUrl == null && format == "png") {
                                    fallbackUrl = cleanUrl
                                } else if (fallbackUrl == null) {
                                    fallbackUrl = cleanUrl
                                }
                            }
                        }
                        if (fallbackUrl != null) return fallbackUrl
                    }

                    // 2. Direct JSON signature fields
                    val urlCandidate = json.optString("url")
                        .ifBlank { json.optString("signature") }
                        .ifBlank { json.optString("signatureUrl") }
                        .ifBlank { json.optString("svg") }
                    if (urlCandidate.startsWith("http://") || urlCandidate.startsWith("https://")) {
                        return urlCandidate.replace("http://", "https://")
                    }
                } else if (bodyString.startsWith("http://") || bodyString.startsWith("https://")) {
                    return bodyString.replace("http://", "https://")
                } else if (contentType.contains("image") || contentType.contains("svg")) {
                    return targetUrl
                }
                null
            }
        }.getOrNull()
    }

    private fun queryWikidataSignature(artistName: String): String? {
        return runCatching {
            // Escape special chars for SPARQL string literal
            val safeName = artistName.replace("\\", "\\\\").replace("\"", "\\\"")
            val sparql = """
                SELECT ?signature WHERE {
                  { ?article schema:about ?item ; schema:isPartOf <https://en.wikipedia.org/> ; schema:name "$safeName"@en . }
                  UNION
                  { ?article schema:about ?item ; schema:isPartOf <https://en.wikipedia.org/> ; schema:name "$safeName (singer)"@en . }
                  UNION
                  { ?article schema:about ?item ; schema:isPartOf <https://en.wikipedia.org/> ; schema:name "$safeName (musician)"@en . }
                  ?item wdt:P109 ?signature .
                } LIMIT 1
            """.trimIndent()

            val url = Uri.parse(WIKIDATA_SPARQL_ENDPOINT).buildUpon()
                .appendQueryParameter("query", sparql)
                .appendQueryParameter("format", "json")
                .build()
                .toString()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "DhvaniMusic/1.0 (https://github.com/Kanaiya-rgb/Dhvani-Music; dhvani@music.app)")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val bindings = json.optJSONObject("results")?.optJSONArray("bindings")
                if (bindings != null && bindings.length() > 0) {
                    val signatureObj = bindings.getJSONObject(0).optJSONObject("signature")
                    val rawUrl = signatureObj?.optString("value")
                    if (!rawUrl.isNullOrBlank()) {
                        return rawUrl.replace("http://", "https://")
                    }
                }
                null
            }
        }.onFailure {
            Log.d(TAG, "Wikidata signature query error for $artistName: ${it.message}")
        }.getOrNull()
    }

    /** Clears the in-memory cache (e.g. after changing custom API URL). */
    fun clearCache() {
        signatureCache.clear()
    }
}
