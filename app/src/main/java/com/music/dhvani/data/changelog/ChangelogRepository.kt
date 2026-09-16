package com.music.dhvani.data.changelog

import android.util.Log
import com.music.dhvani.BuildConfig
import com.music.dhvani.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ReleaseChangelog(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val summary: String,
    val items: List<String>,
)

object ChangelogRepository {

    private const val TAG = "ChangelogRepository"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Kanaiya-rgb/Dhvani-Music/releases?per_page=15"
    private const val CDN_VERSION_URL = "https://raw.githubusercontent.com/Kanaiya-rgb/Dhvani-Music/main/version.json"

    val FALLBACK_RELEASES = listOf(
        ReleaseChangelog(
            version = "v2.1.2",
            date = "16 Sep 2026",
            isLatest = true,
            summary = "Super-Smooth Home Feed & Settings UX Polish",
            items = listOf(
                "🚀 Home Scroll Performance: Eliminated BoxWithConstraints subcompositions and scroll-tick recompositions for butter-smooth 60/120fps scrolling.",
                "📱 Settings Subscreen Auto-Top: About and all sub-screens now open directly at the very top instead of retaining main scroll offset.",
                "👨‍💻 Meet the Developer: Added personal Creator & Developer card with social links in About Dhvani Music.",
                "🌐 Live Changelog Engine: Real-time synchronization with GitHub releases directly from Settings.",
            ),
        ),
        ReleaseChangelog(
            version = "v2.1.1",
            date = "16 Sep 2026",
            isLatest = false,
            summary = "Status Bar Icon Crash Hotfix",
            items = listOf(
                "🐛 Critical Hotfix: Resolved a fatal RemoteServiceException crash caused by an empty vector drawable when Status bar playback icon is turned off.",
                "✨ Valid Transparent Vector: Restored seamless status bar icon hiding without system errors.",
                "⚡ Rock-solid stability on Android 12, 13, 14, and 15 devices.",
            ),
        ),
        ReleaseChangelog(
            version = "v2.1.0",
            date = "16 Sep 2026",
            isLatest = false,
            summary = "Listen Together Revamp, Smart Queue Preload & Instant Update Alerts",
            items = listOf(
                "🌐 Listen Together Redesign: Completely revamped with futuristic aesthetic UI, glowing connection pills, and live waveform visualizers.",
                "⚡ Real-Time Instant Update Alerts: Automatic background update check upon Wi-Fi or Mobile Data connection.",
                "🚀 Smart Queue Preload: Instant prefetch for next 2 songs (synced lyrics, audio chunks, artwork) for 0ms delay skips.",
                "💬 Apple Music Style Lyrics Icon: Modern quotation marks quote bubble glyph for lyrics.",
                "🏝️ Floating Dynamic Island Permission Alert: Interactive preview guidance for Display Over Other Apps permission.",
            ),
        ),
        ReleaseChangelog(
            version = "v2.0.7",
            date = "13 Sep 2026",
            isLatest = false,
            summary = "Status Bar Playback Icon & 32-bit Compatibility",
            items = listOf(
                "🔔 Status Bar Playback Icon: Show Dhvani logo in Android status bar during playback with toggle in Appearance Settings.",
                "📱 Full 32-bit / Android Go Compatibility: armeabi-v7a support for entry-level devices.",
                "🛡️ Notification Permission Handling: POST_NOTIFICATIONS runtime request for seamless background playback.",
                "🎛️ Playback & Audio Fixes: Stream recovery and audio device transition enhancements.",
            ),
        ),
        ReleaseChangelog(
            version = "v2.0.6",
            date = "12 Sep 2026",
            isLatest = false,
            summary = "Download Network Controls, CDN Update Notifications & Storage Fixes",
            items = listOf(
                "🌐 Song Download Network Policy: Download songs over Mobile Data & Wi-Fi (default), Only Wi-Fi, or Mobile Data Only.",
                "🔔 High-Reliability Update Notifications: Fast CDN-backed update detection engine with heads-up notifications.",
                "📊 Dynamic Offline Playback Statistics: Fixed download page stats to calculate live song counts and accurate cumulative playback duration.",
                "🇮🇳 Multilingual Support: Complete Hindi & English localization across download preferences, storage, and settings.",
                "✨ 12+ Kinetic Text-Effects Lyrics Engine: Full shader rendering with 12 distinctive visual animations.",
            ),
        ),
        ReleaseChangelog(
            version = "v2.0.5",
            date = "12 Sep 2026",
            isLatest = false,
            summary = "Text-Effects Showcase Engine & UI Navigation Polish",
            items = listOf(
                "✨ 12+ Unique Text-Effects: Neon Electric, Digital Glitch, Ocean Wave, Cosmic Aurora, Volcanic Ember, Liquid Chrome, Retro CRT Terminal, Dancing Wave, and more.",
                "🎯 Persistent Line-Sync Stylization: Active lyric lines ignite in distinctive signature shaders even for songs without word timestamps.",
                "🧹 Settings Navigation Overhaul: Eliminated duplicate back buttons on Appearance and sub-settings.",
                "⚡ Playback Smoothness: Optimized Compose canvas draw scopes and shaders for flawless 60/120fps motion.",
            ),
        ),
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val _releases = MutableStateFlow(FALLBACK_RELEASES)
    val releases = _releases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var hasFetchedOnce = false

    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (_isLoading.value) return@withContext
        if (!force && hasFetchedOnce && _releases.value.isNotEmpty()) return@withContext

        _isLoading.value = true
        try {
            val fetched = fetchReleasesFromGitHub()
            if (!fetched.isNullOrEmpty()) {
                _releases.value = fetched
                hasFetchedOnce = true
                Log.d(TAG, "Successfully loaded ${fetched.size} releases from GitHub API")
                return@withContext
            }

            // Fallback 1: Try raw CDN version.json if GitHub API rate-limited
            val cdnRelease = fetchLatestFromCdn()
            if (cdnRelease != null) {
                val currentList = FALLBACK_RELEASES.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.version.equals(cdnRelease.version, ignoreCase = true) }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = cdnRelease
                } else {
                    currentList.add(0, cdnRelease)
                }
                // Ensure only first is marked isLatest
                val updated = currentList.mapIndexed { idx, rel ->
                    rel.copy(isLatest = idx == 0)
                }
                _releases.value = updated
                hasFetchedOnce = true
                Log.d(TAG, "Loaded latest release from CDN version.json fallback")
                return@withContext
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching live changelog", e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun fetchReleasesFromGitHub(): List<ReleaseChangelog>? = runCatching {
        val request = Request.Builder()
            .url(GITHUB_RELEASES_URL)
            .header("User-Agent", "Dhvani-Music/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/vnd.github+json")
            .build()

        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub releases API returned HTTP ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val array = json.parseToJsonElement(body).jsonArray

            val list = mutableListOf<ReleaseChangelog>()
            for (element in array) {
                val obj = element.jsonObject
                val isDraft = obj["draft"]?.jsonPrimitive?.booleanOrNull ?: false
                if (isDraft) continue

                val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
                val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val releaseBody = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty()

                val formattedDate = formatReleaseDate(publishedAt)
                val summary = parseReleaseSummary(name, tag, releaseBody)
                val items = parseReleaseItems(releaseBody)

                list.add(
                    ReleaseChangelog(
                        version = tag,
                        date = formattedDate,
                        isLatest = false,
                        summary = summary,
                        items = items,
                    )
                )
            }

            if (list.isNotEmpty()) {
                list[0] = list[0].copy(isLatest = true)
                list
            } else {
                null
            }
        }
    }.getOrNull()

    private fun fetchLatestFromCdn(): ReleaseChangelog? = runCatching {
        val bustUrl = "$CDN_VERSION_URL?_t=${System.currentTimeMillis()}"
        val req = Request.Builder()
            .url(bustUrl)
            .header("User-Agent", "Dhvani-Music/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache")
            .build()
        Http.client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val text = res.body?.string() ?: return null
            val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
            val ver = obj["version"]?.jsonPrimitive?.contentOrNull?.trim() ?: return null
            val tag = if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
            val notes = obj["notes"]?.jsonPrimitive?.contentOrNull.orEmpty()

            val items = if (notes.isNotBlank()) {
                notes.split("; ", "\n").map { it.trim().removePrefix("- ").removePrefix("* ").trim() }.filter { it.isNotBlank() }
            } else {
                listOf("Performance improvements and bug fixes.")
            }

            ReleaseChangelog(
                version = tag,
                date = "Latest Update",
                isLatest = true,
                summary = notes.substringBefore(":").ifBlank { "Latest Release" },
                items = items,
            )
        }
    }.getOrNull()

    private fun formatReleaseDate(iso: String): String {
        if (iso.isBlank()) return "Recent"
        return runCatching {
            val instant = Instant.parse(iso)
            val zdt = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
            formatter.format(zdt)
        }.getOrElse {
            if (iso.length >= 10) iso.substring(0, 10) else iso
        }
    }

    private fun parseReleaseSummary(name: String, tag: String, body: String): String {
        val cleaned = name.removePrefix("Dhvani Music")
            .removePrefix(tag)
            .trim()
            .removePrefix("-")
            .removePrefix("—")
            .trim()

        if (cleaned.isNotBlank()) {
            return cleaned
        }

        val firstHeader = body.lines().firstOrNull { it.trim().startsWith("#") }
            ?.replace(Regex("""^#+\s*"""), "")
            ?.trim()

        if (!firstHeader.isNullOrBlank()) {
            return firstHeader
        }

        return "Release $tag"
    }

    private fun parseReleaseItems(body: String): List<String> {
        val lines = body.lines()
        val bullets = mutableListOf<String>()

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                val clean = trimmed.drop(2).trim()
                    .replace(Regex("""^\*+\s*"""), "")
                    .replace("**", "")
                    .replace("`", "")
                    .trim()

                if (clean.isNotBlank() &&
                    !clean.startsWith("Download", ignoreCase = true) &&
                    !clean.startsWith("###", ignoreCase = true)
                ) {
                    bullets.add(clean)
                }
            }
        }

        if (bullets.isEmpty()) {
            return lines.map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }
                .map { it.replace("**", "").replace("`", "").trim() }
                .filter { !it.startsWith("Download", ignoreCase = true) }
                .take(6)
        }

        return bullets
    }
}
