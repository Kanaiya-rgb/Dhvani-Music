package com.music.dhvani.data.sources

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.music.dhvani.BuildConfig
import com.music.dhvani.data.TrackLog
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.util.UUID

/**
 * One configured source: which protocol, which index.
 *
 * Stored in encrypted prefs — [baseUrl] is a module index the user called
 * "for my private use", not something to leave sitting in plain-text
 * SharedPreferences on a device someone else might get into.
 */
@Serializable
data class SourceConfig(
    val id: String = UUID.randomUUID().toString(),
    val kind: SourceKind,
    /** What the user called it. Blank falls back to the server's host, or the kind's own label. */
    val label: String = "",
    val baseUrl: String = "",
    val enabled: Boolean = true,
) {
    /** What the sources screen and the player show. Never blank. */
    val displayName: String
        get() = when {
            kind == SourceKind.JIOSAAVN && (label.isBlank() || label.equals("JioSaavn", ignoreCase = true)) -> "High Quality AAC"
            kind == SourceKind.YOUTUBE && (label.isBlank() || label.equals("YouTube Music", ignoreCase = true) || label.equals("YouTube", ignoreCase = true)) -> "Standard Opus"
            else -> label.ifBlank {
                baseUrl.takeIf { it.isNotBlank() }
                    ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                    ?: kind.label
            }
        }

    /** Whether this has enough filled in to be worth contacting at all. */
    val isComplete: Boolean
        get() = !kind.needsServer || baseUrl.isNotBlank()
}

/**
 * The user's sources, always tried in a fixed order: the module source
 * first, YouTube Music second.
 *
 * [SourceKind.YOUTUBE] is seeded on first run and cannot be deleted, only
 * disabled — it needs no configuration, so a "remove" would delete something
 * the user could not then re-create by typing anything in, it would just be a
 * switch that hides itself. The module source is entirely optional: with none
 * configured, YouTube is all there is.
 */
object SourceRegistry {

    private const val TAG = "DhvaniMusic"

    private lateinit var prefs: SharedPreferences

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Every configured source, enabled or not. */
    val configs = MutableStateFlow<List<SourceConfig>>(emptyList())

    /**
     * Built instances, keyed by config id, rebuilt whenever [configs] changes.
     *
     * Held rather than constructed per call so that a source with any warmed
     * state — a module whose index has already been fetched — keeps it across
     * tracks instead of re-probing on every resolve.
     */
    private var instances: Map<String, MusicSource> = emptyMap()

    fun init(context: Context) {
        prefs = runCatching {
            EncryptedSharedPreferences.create(
                context,
                "flux_sources",
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            // Same degradation as AuthStore: a handful of OEM builds cannot
            // init the keystore, and refusing to run at all is worse than
            // storing this the way every other setting in the app is stored.
            TrackLog.w(TAG, "EncryptedSharedPreferences unavailable for sources: ${it.message}")
            context.getSharedPreferences("flux_sources_plain", Context.MODE_PRIVATE)
        }

        val stored = prefs.getString(KEY_SOURCES, null)?.let(::decodeStored) ?: emptyList()
        val removedIds = prefs.getStringSet(KEY_REMOVED_SOURCES, emptySet()) ?: emptySet()

        // Purge retired/non-functional sources (e.g. Tido/Tidal addon that only verified connection without streaming)
        val sanitized = stored.filterNot {
            it.baseUrl.contains("tido", ignoreCase = true) ||
                it.baseUrl.contains("tidal", ignoreCase = true) ||
                it.label.contains("tido", ignoreCase = true) ||
                it.label.contains("tidal hi-res", ignoreCase = true)
        }.map { cfg ->
            // Upgrade generic "Custom Module" label to friendly manifest names
            if (cfg.label == "Custom Module" || cfg.label.isBlank()) {
                val upgraded = friendlyAddonName(cfg.baseUrl)
                if (upgraded.isNotBlank()) cfg.copy(label = upgraded) else cfg
            } else cfg
        }

        // Seed default community addons (e.g. BluBit Station Hi-Fi, Eljohnzc Mono, Eljohnzc PH)
        // only if user hasn't explicitly removed them or already configured them
        val missingCommunityAddons = DEFAULT_COMMUNITY_ADDONS.filter { defaultAddon ->
            defaultAddon.id !in removedIds &&
                sanitized.none { existing ->
                    existing.baseUrl.trimEnd('/').equals(defaultAddon.baseUrl.trimEnd('/'), ignoreCase = true) ||
                        (defaultAddon.baseUrl.contains("bluebit") && existing.baseUrl.contains("bluebit")) ||
                        (defaultAddon.baseUrl.contains("eljohnzc-mono") && existing.baseUrl.contains("eljohnzc-mono")) ||
                        (defaultAddon.baseUrl.contains("4433c7caff7fd070309e") && existing.baseUrl.contains("4433c7caff7fd070309e"))
                }
        }

        // Built-ins (Monochrome, JioSaavn, YouTube) with stable IDs
        val missingBuiltins = BUILT_IN_KINDS
            .filter { kind -> sanitized.none { it.kind == kind } }
            .map {
                val stableId = when (it) {
                    SourceKind.MONOCHROME -> "monochrome"
                    SourceKind.JIOSAAVN -> "jiosaavn"
                    SourceKind.YOUTUBE -> "youtube"
                    else -> UUID.randomUUID().toString()
                }
                SourceConfig(id = stableId, kind = it, enabled = true)
            }

        // Put Monochrome (bit-exact built-in) and active working addons at the top
        val allSources = (sanitized + missingBuiltins + missingCommunityAddons).distinctBy { it.id }
        val seeded = allSources.sortedBy { cfg ->
            when (cfg.kind) {
                SourceKind.MONOCHROME -> 0
                SourceKind.CUSTOM_MODULE, SourceKind.MODULE -> 1
                SourceKind.JIOSAAVN -> 2
                SourceKind.YOUTUBE -> 3
            }
        }

        // If a module index URL was baked in at build time, ensure it is the
        // one stored — add the module source if missing, or silently update its
        // URL if it changed. The toggle’s enabled state is always preserved so
        // the user’s on/off choice survives an app update.
        val envUrl = BuildConfig.MODULE_INDEX_URL.trim()
        val withModule = if (envUrl.isNotEmpty()) {
            val existingModule = seeded.firstOrNull { it.kind == SourceKind.MODULE }
            if (existingModule == null) {
                seeded + SourceConfig(
                    kind = SourceKind.MODULE,
                    label = ENV_MODULE_LABEL,
                    baseUrl = envUrl,
                    enabled = true,
                )
            } else if (existingModule.baseUrl != envUrl || existingModule.label.isBlank()) {
                // The label is filled in as well as the URL, so the env-managed
                // module is named rather than showing the bare index host —
                // which is what [SourceConfig.displayName] falls back to.
                seeded.map {
                    if (it.kind == SourceKind.MODULE) {
                        it.copy(baseUrl = envUrl, label = it.label.ifBlank { ENV_MODULE_LABEL })
                    } else {
                        it
                    }
                }
            } else {
                seeded
            }
        } else {
            // No env URL: keep whatever the user had stored, but ensure there
            // is no leftover env-managed module config lying around from a
            // previous build that did have one.
            seeded
        }

        // YouTube is not switchable — see [setEnabled] — so a config persisted
        // as disabled by an earlier build would strand the app with no source
        // it is allowed to turn back on.
        val after = withModule.map {
            if (it.kind == SourceKind.YOUTUBE && !it.enabled) it.copy(enabled = true) else it
        }

        publish(after, persist = after != stored)
    }

    /**
     * Decodes a stored source list one entry at a time rather than as a
     * single list, so one entry naming a kind this build no longer has —
     * left over from before a kind was retired — doesn't take every other
     * entry down with it. A strict `List<SourceConfig>` decode fails whole:
     * one bad enum value and the user's real, working module config is
     * silently gone along with it.
     */
    private fun decodeStored(raw: String): List<SourceConfig> {
        val elements = runCatching { json.parseToJsonElement(raw).jsonArray }
            .getOrElse { return emptyList() }
        return elements.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(SourceConfig.serializer(), element) }
                .onFailure { TrackLog.w(TAG, "dropping unreadable stored source: ${it.message}") }
                .getOrNull()
        }
    }

    /** The enabled sources in their configured user order. */
    fun active(): List<MusicSource> =
        configs.value
            .filter { it.enabled && it.isComplete }
            .mapNotNull { instances[it.id] }

    fun instance(configId: String): MusicSource? = instances[configId]
        ?: instances.values.firstOrNull { it.configId == configId }
        ?: active().firstOrNull { it.configId == configId || it.kind.name.equals(configId, ignoreCase = true) }
        ?: if (configId.contains("monochrome", ignoreCase = true)) active().firstOrNull { it.kind == SourceKind.MONOCHROME }
        else if (configId.equals("JIOSAAVN", ignoreCase = true)) active().firstOrNull { it.kind == SourceKind.JIOSAAVN }
        else null

    fun config(configId: String): SourceConfig? = configs.value.firstOrNull { it.id == configId }

    // ── Editing ─────────────────────────────────────────────────────────

    fun reorder(newOrder: List<SourceConfig>) = publish(newOrder)

    fun add(config: SourceConfig) {
        if (::prefs.isInitialized) {
            val removed = (prefs.getStringSet(KEY_REMOVED_SOURCES, emptySet()) ?: emptySet()) - config.id
            prefs.edit().putStringSet(KEY_REMOVED_SOURCES, removed).apply()
        }
        publish(configs.value + config)
    }

    fun update(config: SourceConfig) =
        publish(configs.value.map { if (it.id == config.id) config else it })

    fun remove(configId: String) {
        val target = config(configId) ?: return
        if (target.kind in BUILT_IN_KINDS) return
        if (::prefs.isInitialized) {
            val removed = (prefs.getStringSet(KEY_REMOVED_SOURCES, emptySet()) ?: emptySet()) + configId
            prefs.edit().putStringSet(KEY_REMOVED_SOURCES, removed).apply()
        }
        publish(configs.value.filterNot { it.id == configId })
    }

    /**
     * Turns one source on or off.
     *
     * YouTube is not switchable and silently ignores a request to disable it.
     * It is the only source that can supply a home feed, radio or related
     * tracks, and nothing else holds the full catalogue — switching it off
     * doesn't even stop it being played, because a YouTube-queued track whose
     * substitutes all miss still falls back to it. A switch that cannot honour
     * its own off position is worse than no switch, so it isn't offered one:
     * see [SourcesScreen][com.music.dhvani.ui.screens.SourcesScreen].
     */
    fun setEnabled(configId: String, enabled: Boolean) {
        if (!enabled && config(configId)?.kind == SourceKind.YOUTUBE) return
        publish(configs.value.map { if (it.id == configId) it.copy(enabled = enabled) else it })
    }

    /** Toggle the MODULE source on or off by its config id. */
    fun setModuleEnabled(enabled: Boolean) {
        val module = configs.value.firstOrNull { it.kind == SourceKind.MODULE } ?: return
        setEnabled(module.id, enabled)
    }

    /** The user's primary custom module index, if they have set one. */
    fun customModule(): SourceConfig? =
        configs.value.firstOrNull { it.kind == SourceKind.CUSTOM_MODULE }

    fun friendlyAddonName(url: String): String = when {
        url.contains("bluebit", ignoreCase = true) -> "BluBit Station Hi-Fi"
        url.contains("eljohnzc-mono", ignoreCase = true) -> "Eljohnzc Addon Monochrome"
        url.contains("4433c7caff7fd070309e", ignoreCase = true) ||
            url.contains("eljohnzc-addon", ignoreCase = true) -> "Eljohnzc Addon 🇵🇭"
        url.contains("losslessbridge", ignoreCase = true) -> "Lossless Bridge"
        url.contains("qobuz", ignoreCase = true) -> "Qobuz Addon"
        else -> ""
    }

    fun isAddonUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.endsWith("manifest.json", ignoreCase = true) ||
            trimmed.contains("/manifest.json", ignoreCase = true) ||
            trimmed.contains("addon", ignoreCase = true) ||
            trimmed.contains("workers.dev", ignoreCase = true) ||
            trimmed.contains("deplexo", ignoreCase = true) ||
            trimmed.contains("bitchord", ignoreCase = true) ||
            trimmed.contains("velixir", ignoreCase = true)
    }

    fun setCustomModule(url: String, label: String = "") {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val computedLabel = label.ifBlank {
            friendlyAddonName(trimmed).ifBlank {
                if (isAddonUrl(trimmed)) "Custom Addon" else "Custom Module"
            }
        }
        val existing = configs.value.firstOrNull { it.baseUrl.trimEnd('/').equals(trimmed.trimEnd('/'), ignoreCase = true) }
        if (existing != null) {
            update(existing.copy(baseUrl = trimmed, label = computedLabel, enabled = true))
        } else {
            add(
                SourceConfig(
                    kind = SourceKind.CUSTOM_MODULE,
                    label = computedLabel,
                    baseUrl = trimmed,
                    enabled = true,
                ),
            )
        }
    }

    private fun publish(next: List<SourceConfig>, persist: Boolean = true) {
        configs.value = next
        // Rebuilt against the previous map so that an untouched source keeps
        // the instance it already had, rather than being replaced by an
        // identical-but-cold one every time an unrelated row is toggled.
        val previous = instances
        instances = next.associate { config ->
            val existing = previous[config.id]?.takeIf { it.configuredBy(config) }
            config.id to (existing ?: build(config))
        }
        if (persist && ::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_SOURCES, json.encodeToString(ListSerializer(SourceConfig.serializer()), next))
                .apply()
        }
    }

    /**
     * Health-checks a config that hasn't been saved — what the editor's Test
     * button asks.
     *
     * Built fresh and thrown away rather than routed through [instances],
     * which hold the *stored* config: testing one of those would report on the
     * old address, which is precisely the state the user is in the middle of
     * correcting.
     */
    suspend fun probeCandidate(config: SourceConfig): SourceHealth {
        val candidate = build(config)
        return candidate.health()
    }

    private fun build(config: SourceConfig): MusicSource = when (config.kind) {
        SourceKind.CUSTOM_MODULE, SourceKind.MODULE -> ModuleSource(config)
        SourceKind.MONOCHROME -> MonochromeSource(config)
        SourceKind.JIOSAAVN -> JioSaavnSource(config)
        SourceKind.YOUTUBE -> YouTubeSource(config)
    }

    /**
     * Whether an already-built instance still matches its stored config —
     * false after an edit that changes where it points, which is exactly when
     * the warm instance must be thrown away.
     */
    private fun MusicSource.configuredBy(config: SourceConfig): Boolean =
        this is ConfigBacked && this.config == config

    /** Implemented by sources that carry their [SourceConfig], so [publish] can tell a real edit from a no-op. */
    internal interface ConfigBacked {
        val config: SourceConfig
    }

    // ── Track identity ──────────────────────────────────────────────────

    /**
     * A source-backed track's id, as it travels through the queue.
     *
     * Packed into the existing [Song.videoId][com.music.dhvani.data.model.Song.videoId]
     * rather than added beside it: that field is the app's media id everywhere —
     * the queue, the notification, the history, the like state — and a second
     * identity field would have to be threaded through every one of them, with
     * each place that forgot silently falling back to treating the track as
     * YouTube's.
     */
    fun trackKey(configId: String, trackId: String) = "$PREFIX$configId$SEPARATOR$trackId"

    /** The `(configId, trackId)` inside a [trackKey], or null if this is an ordinary YouTube id. */
    fun parseTrackKey(key: String): Pair<String, String>? {
        if (!key.startsWith(PREFIX)) return null
        val body = key.removePrefix(PREFIX)
        val cut = body.indexOf(SEPARATOR)
        if (cut <= 0) return null
        return body.substring(0, cut) to body.substring(cut + SEPARATOR.length)
    }

    /** The playback URI for a source-backed track; [PlaybackService] resolves it at open time. */
    fun trackUri(configId: String, trackId: String): String =
        Uri.Builder()
            .scheme("flux")
            .authority("source")
            .appendQueryParameter("s", configId)
            .appendQueryParameter("t", trackId)
            .build()
            .toString()

    val DEFAULT_COMMUNITY_ADDONS = listOf(
        SourceConfig(
            id = "builtin_addon_eljohnzc_mono",
            kind = SourceKind.CUSTOM_MODULE,
            label = "Eljohnzc Addon Monochrome",
            baseUrl = "https://eljohnzc-mono.de.deplexo.com/manifest.json",
            enabled = true,
        ),
        SourceConfig(
            id = "builtin_addon_eljohnzc_ph",
            kind = SourceKind.CUSTOM_MODULE,
            label = "Eljohnzc Addon 🇵🇭",
            baseUrl = "https://eljohnzc-addon.de.deplexo.com/4433c7caff7fd070309e/manifest.json",
            enabled = true,
        ),
        SourceConfig(
            id = "builtin_addon_bluebit",
            kind = SourceKind.CUSTOM_MODULE,
            label = "BluBit Station Hi-Fi",
            baseUrl = "https://bluebit.jucepe0929.workers.dev/",
            enabled = false,
        ),
    )

    val COMMUNITY_ADDON_PRESETS = listOf(
        CommunityAddonPreset(
            name = "Eljohnzc Addon Monochrome",
            url = "https://eljohnzc-mono.de.deplexo.com/manifest.json",
            tag = "Lossless FLAC",
            description = "16-bit 44.1kHz Lossless FLAC stream",
        ),
        CommunityAddonPreset(
            name = "Eljohnzc Addon 🇵🇭",
            url = "https://eljohnzc-addon.de.deplexo.com/4433c7caff7fd070309e/manifest.json",
            tag = "Lossless & Atmos",
            description = "Tidal Lossless and Dolby Atmos",
        ),
        CommunityAddonPreset(
            name = "BluBit Station Hi-Fi",
            url = "https://bluebit.jucepe0929.workers.dev/",
            tag = "24-bit Hi-Res",
            description = "Tidal & Deezer Lossless FLAC via Cloudflare Workers",
        ),
        CommunityAddonPreset(
            name = "Lossless Bridge",
            url = "https://losslessbridge.de.deplexo.com/",
            tag = "Hi-Res FLAC",
            description = "Lossless bridge audio server",
        ),
    )

    private val BUILT_IN_KINDS = listOf(SourceKind.MONOCHROME, SourceKind.JIOSAAVN, SourceKind.YOUTUBE)

    /** What the build-time module index is called on screen, in place of its host. */
    private const val ENV_MODULE_LABEL = "Ricky's Addon"

    private const val KEY_SOURCES = "sources"
    private const val KEY_REMOVED_SOURCES = "removed_source_ids"
    private const val PREFIX = "src:"
    private const val SEPARATOR = "::"
}

data class CommunityAddonPreset(
    val name: String,
    val url: String,
    val tag: String,
    val description: String,
)
