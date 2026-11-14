package com.music.dhvani.data.sources

import android.content.Context
import android.content.SharedPreferences
import com.music.dhvani.data.Http
import com.music.dhvani.data.TrackLog
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Manages dynamic, remote retrieval of streaming addons & sources from GitHub Gist or Raw JSON URL.
 *
 * Developers can add, update, or retire streaming addons/sources remotely in the Gist/JSON file,
 * and Dhvani Music will automatically sync them over the air without requiring an APK update.
 */
object RemoteSourcesManager {
    private const val TAG = "RemoteSourcesManager"
    private const val PREFS_NAME = "flux_remote_sources"
    private const val KEY_CACHED_JSON = "cached_sources_json"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time_ms"
    private const val KEY_MANAGED_REMOTE_IDS = "managed_remote_source_ids"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val syncMutex = Mutex()
    private val MIN_SYNC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)

    val isSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow(0L)

    private var prefs: SharedPreferences? = null

    fun isManagedRemote(config: SourceConfig): Boolean {
        if (config.isManagedRemote) return true
        if (config.kind == SourceKind.MODULE) return true
        if (config.id.startsWith("builtin_addon_")) return true
        val managed = prefs?.getStringSet(KEY_MANAGED_REMOTE_IDS, emptySet()) ?: emptySet()
        if (config.id in managed) return true
        val cleanUrl = config.baseUrl.trimEnd('/')
        if (SourceRegistry.DEFAULT_COMMUNITY_ADDONS.any {
            it.id == config.id || (it.baseUrl.isNotBlank() && it.baseUrl.trimEnd('/').equals(cleanUrl, ignoreCase = true))
        }) return true
        if (SourceRegistry.COMMUNITY_ADDON_PRESETS.any {
            it.url.isNotBlank() && it.url.trimEnd('/').equals(cleanUrl, ignoreCase = true)
        }) return true
        return false
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        lastSyncTime.value = prefs?.getLong(KEY_LAST_SYNC_TIME, 0L) ?: 0L

        // Track default managed remote IDs on first boot
        if (prefs?.contains(KEY_MANAGED_REMOTE_IDS) != true) {
            val initialManaged = SourceRegistry.DEFAULT_COMMUNITY_ADDONS.map { it.id }.toSet()
            prefs?.edit()?.putStringSet(KEY_MANAGED_REMOTE_IDS, initialManaged)?.apply()
        }

        // Apply previously cached remote config immediately on startup
        val cached = prefs?.getString(KEY_CACHED_JSON, null)
        if (!cached.isNullOrBlank()) {
            runCatching { parseAndApply(cached) }
        }
    }

    /**
     * Resolves the target Raw URL from whatever URL was provided (GitHub Gist, Gist Raw, or GitHub Repo Raw).
     */
    fun resolveRawUrl(inputUrl: String): String {
        val trimmed = inputUrl.trim()
        if (trimmed.isBlank()) return AppSettings.DEFAULT_SOURCES_REMOTE_URL

        // If user entered a GitHub Gist web URL: https://gist.github.com/user/gist_id
        if (trimmed.contains("gist.github.com/") && !trimmed.contains("/raw")) {
            val gistId = trimmed.substringAfterLast('/').substringBefore('#')
            val user = trimmed.substringBefore("/$gistId").substringAfterLast('/')
            return "https://gist.githubusercontent.com/$user/$gistId/raw"
        }

        // If user entered a specific commit raw URL: strip the commit SHA so it always fetches latest HEAD
        if (trimmed.contains("gist.githubusercontent.com") && trimmed.contains("/raw/")) {
            val regex = Regex("""(gist\.githubusercontent\.com/[^/]+/[^/]+/raw)/[a-f0-9]{40}/(.*)""")
            return trimmed.replace(regex, "$1/$2")
        }

        return trimmed
    }

    private fun fetchHttp(url: String): String? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DhvaniMusic/2.8 (Android)")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            Http.client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    /**
     * Fetches the latest sources and presets from the remote GitHub Gist or Raw URL.
     */
    suspend fun syncRemoteSources(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastSyncTime.value < MIN_SYNC_INTERVAL_MS && lastSyncTime.value > 0L) {
                return@withLock Result.success(SourceRegistry.configs.value.size)
            }

            val rawTargetUrl = resolveRawUrl(
                AppSettings.sourcesRemoteUrl.value.ifBlank { AppSettings.DEFAULT_SOURCES_REMOTE_URL },
            )
            val targetUrl = if (rawTargetUrl.contains("?")) "$rawTargetUrl&_t=$now" else "$rawTargetUrl?_t=$now" 

            TrackLog.d(TAG, "Fetching remote sources from: $targetUrl")
            isSyncing.value = true

            try {
                var body = fetchHttp(targetUrl)
                if (body.isNullOrBlank() && targetUrl != AppSettings.FALLBACK_SOURCES_REMOTE_URL) {
                    TrackLog.d(TAG, "Primary remote sources failed, trying fallback: ${AppSettings.FALLBACK_SOURCES_REMOTE_URL}")
                    body = fetchHttp(AppSettings.FALLBACK_SOURCES_REMOTE_URL)
                }

                if (body.isNullOrBlank()) {
                    TrackLog.w(TAG, "Remote sources unavailable from both primary and fallback")
                    return@withLock Result.failure(IllegalStateException("Empty or unavailable remote sources response"))
                }

                val appliedCount = parseAndApply(body)
                lastSyncTime.value = now
                prefs?.edit()
                    ?.putString(KEY_CACHED_JSON, body)
                    ?.putLong(KEY_LAST_SYNC_TIME, now)
                    ?.apply()

                TrackLog.d(TAG, "Successfully synced $appliedCount remote sources from Gist")
                Result.success(appliedCount)
            } catch (e: Exception) {
                TrackLog.w(TAG, "Remote sources sync error: ${e.message}")
                Result.failure(e)
            } finally {
                isSyncing.value = false
            }
        }
    }

    private fun parseAndApply(body: String): Int {
        val root = json.parseToJsonElement(body)

        val sourcesList = mutableListOf<SourceConfig>()
        val presetsList = mutableListOf<CommunityAddonPreset>()
        val retiredSet = mutableSetOf<String>()

        when (root) {
            is JsonObject -> {
                // If it's a GitHub Gist API format: root["files"][filename]["content"]
                val filesObj = root["files"] as? JsonObject
                if (filesObj != null) {
                    val matchingFile = filesObj["sources.json"]
                        ?: filesObj.values.firstOrNull {
                            it.jsonObject["filename"]?.jsonPrimitive?.contentOrNull?.endsWith(".json") == true
                        }
                        ?: filesObj.values.firstOrNull()

                    val targetContent = matchingFile?.let {
                        it.jsonObject["content"]?.jsonPrimitive?.contentOrNull
                    }
                    if (!targetContent.isNullOrBlank()) {
                        return parseAndApply(targetContent)
                    }
                }

                // 1. Parse retired IDs
                val retiredArray = root["retired_ids"] as? JsonArray
                retiredArray?.forEach { el ->
                    el.jsonPrimitive.contentOrNull?.let { retiredSet.add(it) }
                }

                // 2. Parse sources
                val sourcesArray = (root["sources"] ?: root["addons"]) as? JsonArray
                sourcesArray?.forEach { el ->
                    (el as? JsonObject)?.let { parseSourceConfig(it)?.let(sourcesList::add) }
                }

                // 3. Parse presets
                val presetsArray = root["presets"] as? JsonArray
                presetsArray?.forEach { el ->
                    (el as? JsonObject)?.let { parsePreset(it)?.let(presetsList::add) }
                }
            }
            is JsonArray -> {
                // Direct array of presets/sources
                root.forEach { el ->
                    (el as? JsonObject)?.let {
                        parseSourceConfig(it)?.let(sourcesList::add)
                        parsePreset(it)?.let(presetsList::add)
                    }
                }
            }
            else -> {}
        }

        if (presetsList.isEmpty() && sourcesList.isNotEmpty()) {
            sourcesList.forEach { s ->
                presetsList.add(
                    CommunityAddonPreset(
                        name = s.displayName,
                        url = s.baseUrl,
                        tag = "24-bit Hi-Res",
                        description = s.label.ifBlank { "Streaming Addon" },
                    ),
                )
            }
        }

        val oldManagedIds = prefs?.getStringSet(KEY_MANAGED_REMOTE_IDS, emptySet()) ?: emptySet()
        val newManagedIds = sourcesList.map { it.id }.toSet()

        // If developer removed an addon from sources list in Gist, automatically retire it from user's app!
        val removedFromRemote = oldManagedIds - newManagedIds
        val fullRetiredSet = (retiredSet + removedFromRemote).toSet()

        prefs?.edit()?.putStringSet(KEY_MANAGED_REMOTE_IDS, newManagedIds)?.apply()

        SourceRegistry.applyRemoteSources(sourcesList, presetsList, fullRetiredSet)
        return sourcesList.size
    }

    private fun parseSourceConfig(obj: JsonObject): SourceConfig? {
        val url = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: obj["baseUrl"]?.jsonPrimitive?.contentOrNull
            ?: return null
        if (url.isBlank()) return null

        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: "addon_${Math.abs(url.hashCode())}"
        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: obj["label"]?.jsonPrimitive?.contentOrNull
            ?: SourceRegistry.friendlyAddonName(url).ifBlank { "Remote Addon" }
        val enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true

        val kind = when {
            url.contains("163", ignoreCase = true) || url.contains("netease", ignoreCase = true) || id.contains("netease", ignoreCase = true) || id.contains("163", ignoreCase = true) -> SourceKind.NETEASE
            url.contains("kuwo", ignoreCase = true) || id.contains("kuwo", ignoreCase = true) || url.contains("oiapi.net/api/Kuwo", ignoreCase = true) -> SourceKind.KUWO
            url.contains("oiapi.net", ignoreCase = true) -> SourceKind.KUWO
            url.endsWith("manifest.json", ignoreCase = true) || SourceRegistry.isAddonUrl(url) -> SourceKind.CUSTOM_MODULE
            id == "tidal_lossless" && url.contains("api.tidal.com") -> SourceKind.TIDAL
            else -> SourceKind.CUSTOM_MODULE
        }

        return SourceConfig(
            id = id,
            kind = kind,
            label = name,
            baseUrl = url,
            enabled = enabled,
            isManagedRemote = true,
        )
    }

    private fun parsePreset(obj: JsonObject): CommunityAddonPreset? {
        val url = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: obj["baseUrl"]?.jsonPrimitive?.contentOrNull
            ?: return null
        if (url.isBlank()) return null

        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: obj["label"]?.jsonPrimitive?.contentOrNull
            ?: SourceRegistry.friendlyAddonName(url).ifBlank { "Remote Addon" }
        val tag = obj["tag"]?.jsonPrimitive?.contentOrNull ?: "Lossless FLAC"
        val desc = obj["description"]?.jsonPrimitive?.contentOrNull
            ?: obj["desc"]?.jsonPrimitive?.contentOrNull
            ?: "Studio Master Lossless Audio"

        return CommunityAddonPreset(
            name = name,
            url = url,
            tag = tag,
            description = desc,
        )
    }
}

